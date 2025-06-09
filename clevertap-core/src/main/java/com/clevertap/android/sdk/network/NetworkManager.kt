package com.clevertap.android.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.copyFrom
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.QueueData
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.evaluation.EventType.Companion.fromBoolean
import com.clevertap.android.sdk.network.EndpointId.Companion.fromEventGroup
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.api.DefineTemplatesRequestBody
import com.clevertap.android.sdk.network.api.EncryptedSendQueueRequestBody
import com.clevertap.android.sdk.network.api.EncryptionSuccess
import com.clevertap.android.sdk.network.api.SendQueueRequestBody
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.response.ARPResponse
import com.clevertap.android.sdk.response.CleverTapResponse
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.toJsonOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import com.clevertap.android.sdk.isNotNullAndBlank
import com.clevertap.android.sdk.network.api.ContentFetchRequestBody
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_DOMAIN_NAME
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_ENCRYPTION_ENABLED
import com.clevertap.android.sdk.network.api.EncryptionFailure

internal class NetworkManager constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo,
    private val coreMetaData: CoreMetaData,
    private val controllerManager: ControllerManager,
    private val databaseManager: BaseDatabaseManager,
    private val callbackManager: BaseCallbackManager,
    private val ctApiWrapper: CtApiWrapper,
    private val encryptionManager: NetworkEncryptionManager,
    private val arpResponse: ARPResponse,
    private val networkRepo: NetworkRepo,
    private val queueHeaderBuilder: QueueHeaderBuilder,
    val cleverTapResponses: MutableList<CleverTapResponse>,
    private val logger: ILogger = config.logger
) {

    private var responseFailureCount = 0

    private var networkRetryCount = 0

    private var minDelayFrequency = 0

    private val mNetworkHeadersListeners: MutableList<NetworkHeadersListener> = ArrayList()

    fun addNetworkHeadersListener(listener: NetworkHeadersListener) {
        mNetworkHeadersListeners.add(listener)
    }

    fun removeNetworkHeadersListener(listener: NetworkHeadersListener) {
        mNetworkHeadersListeners.remove(listener)
    }

    /**
     * Flushes the events queue from the local database to CleverTap servers.
     *
     * @param context    The Context object.
     * @param eventGroup The EventGroup indicating the type of events to be flushed.
     * @param caller     The optional caller identifier.
     */
    fun flushDBQueue(context: Context, eventGroup: EventGroup, caller: String?) {
        config.logger
            .verbose(
                config.accountId,
                "Somebody has invoked me to send the queue to CleverTap servers"
            )

        var previousCursor: QueueData? = null
        var loadMore = true

        while (loadMore) {
            // Retrieve queued events from the local database in batch size of 50

            val cursor: QueueData = databaseManager.getQueuedEvents(context, 50, previousCursor, eventGroup)

            if (cursor.isEmpty) {
                // No events in the queue, log and break
                config.logger.verbose(config.accountId, "No events in the queue, failing")

                if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
                    // Notify listener for push impression sent to the server
                    if (previousCursor?.data != null) {
                        try {
                            notifyListenersForPushImpressionSentToServer(previousCursor.data!!)
                        } catch (e: Exception) {
                            config.logger.verbose(
                                config.accountId,
                                "met with exception while notifying listeners for PushImpressionSentToServer event"
                            )
                        }
                    }
                }
                break
            }

            previousCursor = cursor
            val queue = cursor.data

            if (queue == null || queue.length() <= 0) {
                // No events in the queue, log and break
                config.logger.verbose(config.accountId, "No events in the queue, failing")
                break
            }

            // Send the events queue to CleverTap servers
            loadMore = sendQueue(context, eventGroup, queue, caller)
            if (!loadMore) {
                // network error
                controllerManager.invokeCallbacksForNetworkError()
                controllerManager.invokeBatchListener(queue, false)
            } else {
                // response was successfully received
                controllerManager.invokeBatchListener(queue, true)
            }
        }
    }

    fun getDelayFrequency(): Int {
        minDelayFrequency = networkRepo.getMinDelayFrequency(minDelayFrequency, networkRetryCount)
        logger.debug(
            config.accountId,
            "Setting delay frequency to $minDelayFrequency"
        )
        return minDelayFrequency
    }

    @WorkerThread
    fun initHandshake(eventGroup: EventGroup, handshakeSuccessCallback: Runnable) {
        // Always set this to 0 so that the handshake is not performed during a HTTP failure
        responseFailureCount = 0
        performHandshakeForDomain(eventGroup, handshakeSuccessCallback)
    }

    @WorkerThread
    fun needsHandshakeForDomain(eventGroup: EventGroup): Boolean {
        val needsHandshake = ctApiWrapper.needsHandshake(
            eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED
        )
        val needHandshakeDueToFailure = responseFailureCount > 5

        if (needHandshakeDueToFailure) {
            setDomain(null)
        }
        return needsHandshake || needHandshakeDueToFailure
    }

    @get:WorkerThread
    val currentRequestTimestamp: Int
        get() = ctApiWrapper.ctApi.currentRequestTimestampSeconds

    @WorkerThread
    fun getDomain(eventGroup: EventGroup): String? {
        return ctApiWrapper.ctApi.getActualDomain(eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED)
    }

    private fun hasDomainChanged(newDomain: String): Boolean {
        val oldDomain = networkRepo.getDomain()
        return newDomain != oldDomain
    }

    /**
     * Use QueueHeaderBuilder for header construction
     */
    private fun getQueueHeader(caller: String?): JSONObject? {
        return queueHeaderBuilder.buildHeader(caller)
    }

    @WorkerThread
    fun performHandshakeForDomain(
        eventGroup: EventGroup,
        handshakeSuccessCallback: Runnable
    ) {
        try {
            ctApiWrapper.ctApi.performHandshakeForDomain(eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED)
                .use { response ->
                    if (response.isSuccess()) {
                        logger.verbose(config.accountId, "Received success from handshake :)")

                        if (shouldMuteSdk(response)) {
                            return
                        }

                        saveDomainChanges(response)
                        logger.verbose(config.accountId, "We are not muted")
                        // We have a new domain, run the callback
                        handshakeSuccessCallback.run()
                    } else {
                        logger.verbose(
                            config.accountId,
                            "Invalid HTTP status code received for handshake - " + response.code
                        )
                    }
                }
        } catch (e: Exception) {
            logger.verbose(config.accountId, "Failed to perform handshake!", e)
        }
    }

    /**
     * Processes the incoming response headers for a change in domain and/or mute.
     *
     * @return True to continue sending requests, false otherwise.
     */
    @WorkerThread
    private fun saveDomainChanges(response: Response) {

        val domainName = response.getHeaderValue(CtApi.HEADER_DOMAIN_NAME)
        Logger.v("Getting domain from header - $domainName")
        if (domainName.isNullOrBlank()) {
            return
        }

        val spikyDomainName = response.getHeaderValue(CtApi.SPIKY_HEADER_DOMAIN_NAME)
        Logger.v("Getting spiky domain from header - $spikyDomainName")

        setMuted(false)
        setDomain(domainName)
        Logger.v("Setting spiky domain from header as -$spikyDomainName")
        if (spikyDomainName == null) {
            setSpikyDomain(domainName)
        } else {
            setSpikyDomain(spikyDomainName)
        }
    }

    private fun shouldMuteSdk(response: Response): Boolean {
        response.getHeaderValue(CtApi.HEADER_MUTE)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { muteCommand ->
                // muteCommand is guaranteed to be non-null and non-empty here
                if (muteCommand == "true") {
                    setMuted(true)
                    return true
                } else {
                    setMuted(false)
                }
            }
        return false
    }

    /**
     * Sends the queue to the CleverTap server.
     *
     * @param context    The Context object.
     * @param eventGroup The EventGroup representing the type of event queue.
     * @param queue      The JSON array containing the event queue.
     * @param caller     The optional caller identifier.
     * @return True if the queue was sent successfully, false otherwise.
     */
    fun sendQueue(
        context: Context,
        eventGroup: EventGroup,
        queue: JSONArray?,
        caller: String?
    ): Boolean {
        if (queue == null || queue.length() <= 0) {
            // Empty queue, no need to send
            return false
        }

        if (deviceInfo.deviceID == null) {
            logger.debug(config.accountId, "CleverTap Id not finalized, unable to send queue")
            return false
        }

        val endpointId: EndpointId = fromEventGroup(eventGroup)
        val queueHeader: JSONObject? = getQueueHeader(caller)
        applyQueueHeaderListeners(queueHeader, endpointId, queue.optJSONObject(0).has("profile"))

        val requestBody = SendQueueRequestBody(queueHeader, queue)
        logger.debug(config.accountId, "Send queue contains " + queue.length() + " items: " + requestBody)
        try {
            val headersDoneListener = {
                notifyHeaderListeners(
                    requestBody,
                    endpointId
                )
            }
            return networkCall(eventGroup, requestBody, headersDoneListener)
        } catch (e: Exception) {
            networkRetryCount++
            responseFailureCount++
            logger.debug(
                config.accountId,
                "An exception occurred while sending the queue, will retry: ",
                e
            )
            if (callbackManager.failureFlushListener != null) {
                callbackManager.failureFlushListener.failureFlush(context)
            }
            return false
        }
    }

    private fun networkCall(
        eventGroup: EventGroup,
        requestBody: SendQueueRequestBody,
        notifyNetworkHeaderListeners: () -> Unit
    ): Boolean = callApiForEventGroup(eventGroup, requestBody).use { response ->
        networkRetryCount = 0
        return when (eventGroup) {
            EventGroup.VARIABLES -> {
                handleVariablesResponse(response = response)
            }

            EventGroup.REGULAR -> {
                handleSendQueueResponse(
                    response = response,
                    isFullResponse = doesBodyContainAppLaunchedOrFetchEvents(requestBody),
                    notifyNetworkHeaderListeners = notifyNetworkHeaderListeners
                ).also { isProcessed ->
                    responseFailureCount = if (isProcessed) 0 else responseFailureCount + 1
                }
            }

            EventGroup.PUSH_NOTIFICATION_VIEWED -> {
                handlePushImpressionsResponse(response = response).also { isProcessed ->
                    responseFailureCount = if (isProcessed) 0 else responseFailureCount + 1
                }
            }
        }
    }


    /**
     * Handles the response from content fetch requests
     * Processes through normal ResponseDecorator route
     */
    private fun handleContentFetchResponse(response: Response): Boolean {
        if (response.isSuccess()) {
            val bodyString = response.readBody()
            val bodyJson = bodyString.toJsonOrNull()

            logger.info(config.accountId, "Content fetch response received successfully")

            // Process through normal response decorators
            for (processor: CleverTapResponse in cleverTapResponses) {
                processor.processResponse(bodyJson, bodyString, this.context)
            }
            return true
        } else {
            when (response.code) {
                439 -> {
                    logger.info(
                        config.accountId, "Content fetch request was rate limited (429). Consider reducing request frequency."
                    )
                }

                else -> logger.info(config.accountId, "Content fetch request failed with response code: ${response.code}")
            }
            return false
        }
    }

    private fun notifyHeaderListeners(
        requestBody: SendQueueRequestBody,
        endpointId: EndpointId
    ) {
        if (requestBody.queueHeader != null) {
            for (listener: NetworkHeadersListener in mNetworkHeadersListeners) {
                val isProfile: Boolean =
                    requestBody.queue.optJSONObject(0).has("profile")
                listener.onSentHeaders(
                    allHeaders = requestBody.queueHeader,
                    endpointId = endpointId,
                    eventType = fromBoolean(isProfile)
                )
            }
        }
    }

    @WorkerThread
    fun defineTemplates(templates: Collection<CustomTemplate>): Boolean {
        val header = getQueueHeader(null) ?: return false

        val body = DefineTemplatesRequestBody(header, templates)
        logger.debug(config.accountId, "Will define templates: $body")

        try {
            ctApiWrapper.ctApi.defineTemplates(body).use { response ->
                if (response.isSuccess()) {
                    handleTemplateResponseSuccess(response)
                    return true
                } else {
                    handleVarsOrTemplatesResponseError(response, "CustomTemplates")
                    return false
                }
            }
        } catch (e: Exception) {
            logger.debug(config.accountId, "An exception occurred while defining templates.", e)
            return false
        }
    }

    @WorkerThread
    fun sendContentFetchRequest(content: JSONArray): Boolean {
        val header = getQueueHeader(null) ?: return false

        val body = ContentFetchRequestBody(header, content)
        logger.debug(config.accountId, "Fetching Content: $body")

        try {
            ctApiWrapper.ctApi.sendContentFetch(body).use { response ->
                handleContentFetchResponse(response)
                return true
            }
        } catch (e: Exception) {
            logger.debug(config.accountId, "An exception occurred while fetching content.", e)
            return false
        }
    }

    private fun applyQueueHeaderListeners(
        queueHeader: JSONObject?,
        endpointId: EndpointId,
        isProfile: Boolean
    ) {
        if (queueHeader != null) {
            for (listener in mNetworkHeadersListeners) {
                val headersToAttach = listener.onAttachHeaders(endpointId, fromBoolean(isProfile))
                if (headersToAttach != null) {
                    queueHeader.copyFrom(headersToAttach)
                }
            }
        }
    }

    @WorkerThread
    private fun callApiForEventGroup(
        eventGroup: EventGroup,
        body: SendQueueRequestBody
    ): Response {
        return when (eventGroup) {
            EventGroup.VARIABLES -> {
                ctApiWrapper.ctApi.defineVars(body)
            }
            EventGroup.REGULAR -> {
                sendQueueApi(body)
            }
            EventGroup.PUSH_NOTIFICATION_VIEWED -> {
                sendImpressionsApi(body)
            }
        }
    }

    private fun sendQueueApi(body: SendQueueRequestBody): Response {
        if (config.isEncryptionInTransitEnabled && coreMetaData.isRelaxNetwork.not()) {
            val encryptionResult = encryptionManager.encryptResponse(body.toString())
            val sessionEncryptionKey = encryptionManager.sessionEncryptionKey()

            if (encryptionResult is EncryptionSuccess) {
                val bodyEnc = EncryptedSendQueueRequestBody(
                    encryptedPayload = encryptionResult.data,
                    key = sessionEncryptionKey,
                    iv = encryptionResult.iv
                ).toJsonString()
                logger.verbose("Encrypted Request = $bodyEnc")
                return ctApiWrapper.ctApi.sendQueue(
                    body = bodyEnc,
                    isEncrypted = true
                )
            } else {
                logger.verbose("Normal Request cause encryption failed = $body")
            }
        }
        return ctApiWrapper.ctApi.sendQueue(body = body.toString())
    }

    private fun sendImpressionsApi(body: SendQueueRequestBody): Response {
        return ctApiWrapper.ctApi.sendImpressions(body = body.toString())
    }

    private fun handleVariablesResponse(response: Response): Boolean {
        if (response.isSuccess()) {
            val bodyString = response.readBody()
            val bodyJson = bodyString.toJsonOrNull()

            logger.verbose(config.accountId, "Processing variables response : $bodyJson")

            arpResponse.processResponse(bodyJson, bodyString, this.context)
            return true
        } else {
            handleVarsOrTemplatesResponseError(response, "Variables")
            return false
        }
    }

    private fun handleVarsOrTemplatesResponseError(response: Response, logTag: String) {
        when (response.code) {
            400 -> {
                val errorStreamJson = response.readBody().toJsonOrNull()
                if (errorStreamJson != null && !TextUtils.isEmpty(errorStreamJson.optString("error"))) {
                    val errorMessage = errorStreamJson.optString("error")
                    logger.info(logTag, "Error while syncing: $errorMessage")
                } else {
                    logger.info(logTag, "Error while syncing.")
                }
                return
            }

            401 -> {
                logger.info(
                    logTag, "Unauthorized access from a non-test profile. "
                            + "Please mark this profile as a test profile from the CleverTap dashboard."
                )
                return
            }

            else -> logger.info(logTag, "Response code " + response.code + " while syncing.")
        }
    }

    private fun handleTemplateResponseSuccess(response: Response) {
        logger.info(config.accountId, "Custom templates defined successfully.")
        val body = response.readBody().toJsonOrNull()
        if (body != null) {
            val warnings = body.optString("error")
            if (!TextUtils.isEmpty(warnings)) {
                logger.info(config.accountId, "Custom templates warnings: $warnings")
            }
        }
    }

    @WorkerThread
    private fun handlePushImpressionsResponse(response: Response): Boolean {
        if (!response.isSuccess()) {
            logger.info("Received error response code: " + response.code)
            return false
        }

        if (abortDueToDomainChange(response) || shouldMuteSdk(response)) {
            return false
        }
        saveDomainChanges(response)

        logger.debug(config.accountId, "Push Impressions sent successfully")
        networkRepo.setLastRequestTs(currentRequestTimestamp)
        setFirstRequestTimestampIfNeeded(currentRequestTimestamp)

        logger.verbose(config.accountId, "Processing response : ${response.readBody().toJsonOrNull()}")
        return true
    }

    @WorkerThread
    private fun handleSendQueueResponse(
        response: Response,
        isFullResponse: Boolean,
        notifyNetworkHeaderListeners: () -> Unit
    ): Boolean {
        if (!response.isSuccess()) {
            handleSendQueueResponseError(response)
            return false
        }

        if (abortDueToDomainChange(response) || shouldMuteSdk(response)) {
            return false
        }

        saveDomainChanges(response)
        notifyNetworkHeaderListeners()

        logger.debug(config.accountId, "Queue sent successfully")
        networkRepo.setLastRequestTs(currentRequestTimestamp)
        setFirstRequestTimestampIfNeeded(currentRequestTimestamp)

        var bodyString: String? = response.readBody()
        var bodyJson: JSONObject? = bodyString.toJsonOrNull()

        logger.verbose(config.accountId, "Processing response : $bodyJson")
        if (bodyString.isNullOrBlank() || bodyJson == null) {
            // no-op: there is nothing to handle, considering success as per legacy contract.
            return true
        }

        val isEncryptedResponse = response.getHeaderValue(HEADER_ENCRYPTION_ENABLED).toBoolean()
        if (isEncryptedResponse) {
            when (val decryptResponse = encryptionManager.decryptResponse(bodyString = bodyString)) {
                is EncryptionFailure -> {
                    logger.verbose(config.accountId, "Failed to decrypt response")
                    return false
                }
                is EncryptionSuccess -> {
                    bodyString = decryptResponse.data
                    bodyJson = bodyString.toJsonOrNull()
                    logger.verbose("Decrypted response = $bodyString")
                }
            }
        }

        for (processor: CleverTapResponse in cleverTapResponses) {
            processor.isFullResponse = isFullResponse
            processor.processResponse(bodyJson, bodyString, context)
        }

        return true
    }

    fun abortDueToDomainChange(response: Response): Boolean {
        val newDomain: String? = response.getHeaderValue(HEADER_DOMAIN_NAME)

        if (newDomain.isNotNullAndBlank() && hasDomainChanged(newDomain)) {
            setDomain(newDomain)
            logger.debug(
                config.accountId,
                "The domain has changed to $newDomain. The request will be retried shortly."
            )
            return true
        }
        return false
    }

    private fun handleSendQueueResponseError(response: Response) {
        logger.info("Received error response code: " + response.code)
        when (response.code) {
            419 -> {
                logger.verbose("There is decryption failure on backend, disabling encrypted requests.")
                coreMetaData.isRelaxNetwork = true
            }
            402 -> {
                logger.verbose("Encryption in transit feature on not enabled for your account, please contact Clevertap support.")
                coreMetaData.isRelaxNetwork = true
            }
            else -> {
                // no-op
            }
        }
    }

    private fun doesBodyContainAppLaunchedOrFetchEvents(body: SendQueueRequestBody): Boolean {
        // check if there is app launched/wzrk_fetch event
        for (index in 0..<body.queue.length()) {
            try {
                val event = body.queue.getJSONObject(index)
                val eventType = event.getString("type")
                if ("event" == eventType) {
                    val evtName = event.getString("evtName")
                    if (Constants.APP_LAUNCHED_EVENT == evtName || Constants.WZRK_FETCH == evtName) {
                        return true
                    }
                }
            } catch (jsonException: JSONException) {
                //skip
            }
        }
        return false
    }

    @Throws(JSONException::class)
    private fun notifyListenersForPushImpressionSentToServer(queue: JSONArray) {
        /* verify whether there is a listener assigned to the push ID for monitoring the 'push impression'
                event.
                */

        for (i in 0..<queue.length()) {
            try {
                val notif = queue.getJSONObject(i).optJSONObject("evtData")
                if (notif != null) {
                    val pushId = notif.optString(Constants.WZRK_PUSH_ID)
                    val pushAccountId = notif.optString(Constants.WZRK_ACCT_ID_KEY)

                    notifyListenerForPushImpressionSentToServer(
                        PushNotificationUtil.buildPushNotificationRenderedListenerKey
                            (
                            pushAccountId,
                            pushId
                        )
                    )
                }
            } catch (e: JSONException) {
                logger.verbose(
                    config.accountId,
                    "Encountered an exception while parsing the push notification viewed event queue"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        logger.verbose(
            config.accountId,
            "push notification viewed event sent successfully"
        )
    }

    private fun notifyListenerForPushImpressionSentToServer(listenerKey: String) {
        val notificationRenderedListener = CleverTapAPI.getNotificationRenderedListener(listenerKey)

        if (notificationRenderedListener != null) {
            logger.verbose(
                config.accountId,
                "notifying listener $listenerKey, that push impression sent successfully"
            )
            notificationRenderedListener.onNotificationRendered(true)
        }
    }

    @WorkerThread
    private fun setDomain(domainName: String?) {
        logger.verbose(config.accountId, "Setting domain to $domainName")
        networkRepo.setDomain(domainName)
        ctApiWrapper.ctApi.cachedDomain = domainName

        if (callbackManager.scDomainListener != null) {
            if (domainName != null) {
                callbackManager.scDomainListener.onSCDomainAvailable(Utils.getSCDomain(domainName))
            } else {
                callbackManager.scDomainListener.onSCDomainUnavailable()
            }
        }
    }

    private fun setFirstRequestTimestampIfNeeded(ts: Int) {
        if (networkRepo.getFirstRequestTs() > 0) {
            return
        }
        networkRepo.setFirstRequestTs(ts)
    }

    @WorkerThread
    private fun setSpikyDomain(spikyDomainName: String) {
        logger.verbose(config.accountId, "Setting spiky domain to $spikyDomainName")
        networkRepo.setSpikyDomain(spikyDomainName)
        ctApiWrapper.ctApi.cachedSpikyDomain = spikyDomainName
    }

    @WorkerThread
    private fun setMuted(mute: Boolean) {
        if (mute) {
            networkRepo.setMuted(true)
            networkRepo.setDomain(null)

            // Clear all the queues
            val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>()
            task.execute("CommsManager#setMuted") {
                databaseManager.clearQueues(context)
            }
        } else {
            networkRepo.setMuted(false)
        }
    }

    companion object {
        @JvmStatic
        fun isNetworkOnline(context: Context): Boolean {
            try {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                        ?: return true // lets be optimistic, if we are truly offline we handle the exception
                @SuppressLint("MissingPermission") val netInfo = cm.activeNetworkInfo
                return netInfo != null && netInfo.isConnected
            } catch (ignore: Exception) {
                // lets be optimistic, if we are truly offline we handle the exception
                return true
            }
        }
    }
}
