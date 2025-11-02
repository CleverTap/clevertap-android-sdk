package com.clevertap.android.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.network.EndpointId.Companion.fromEventGroup
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.api.DefineTemplatesRequestBody
import com.clevertap.android.sdk.network.api.EncryptedSendQueueRequestBody
import com.clevertap.android.sdk.network.api.EncryptionSuccess
import com.clevertap.android.sdk.network.api.SendQueueRequestBody
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.toJsonOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * NetworkManager - Handles all HTTP network operations for CleverTap SDK
 *
 * Dependencies:
 * - coreContract: Interface to core system for response routing
 * - ctApiWrapper: HTTP API client wrapper
 * - encryptionManager: Handles request/response encryption
 * - networkRepo: Persistent storage for network state
 * - queueHeaderBuilder: Builds request headers
 *
 * All other dependencies (context, config, deviceInfo, etc.) are accessed through coreContract
 */
internal class NetworkManager constructor(
    private val ctApiWrapper: CtApiWrapper,
    private val encryptionManager: NetworkEncryptionManager,
    private val networkRepo: NetworkRepo,
) {

    companion object {
        private const val BATCH_SIZE = 50

        @JvmStatic
        fun isNetworkOnline(context: Context): Boolean {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return true // optimistic: if truly offline, we handle the exception
                @SuppressLint("MissingPermission") val netInfo = cm.activeNetworkInfo
                return netInfo != null && netInfo.isConnected
            } catch (ignore: Exception) {
                return true // optimistic
            }
        }
    }

    lateinit var coreContract: CoreContract
    private val logger: ILogger
        get() = coreContract.logger()

    private val accountId: String
        get() = coreContract.config().accountId

    private var responseFailureCount = 0
    private var networkRetryCount = 0
    private var minDelayFrequency = 0


    /**
     * Flushes the events queue from the local database to CleverTap servers.
     *
     * @param context    The Context object.
     * @param eventGroup The EventGroup indicating the type of events to be flushed.
     * @param caller     The optional caller identifier.
     * @param isUserSwitchFlush True when user is switching.
     */
    fun flushDBQueue(
        context: Context,
        eventGroup: EventGroup,
        caller: String?,
        isUserSwitchFlush: Boolean
    ) {
        logger.verbose(accountId, "Starting queue flush to CleverTap servers")

        var continueProcessing = true
        var totalEventsSent = 0

        while (continueProcessing) {
            // Retrieve combined batch of events
            val queueData = coreContract.database().getQueuedEvents(
                context = context,
                batchSize = BATCH_SIZE,
                eventGroup = eventGroup
            )

            if (queueData.isEmpty) {
                logger.verbose(accountId, "No more events in queue")
                break
            }

            val queue = queueData.data
            val batchSize = queue.length()
            logger.verbose(
                accountId,
                "Processing batch of $batchSize events (${queueData.eventIds.size} from events, ${queueData.profileEventIds.size} from profile)"
            )

            // Send the combined batch to CleverTap servers
            val networkCallSuccess = sendQueue(
                context = context,
                eventGroup = eventGroup,
                queue = queue,
                caller = caller,
                isUserSwitchFlush = isUserSwitchFlush
            )

            if (networkCallSuccess.not()) {
                logger.verbose(accountId, "Failed to send batch - will retry later")
                coreContract.onNetworkError()
                coreContract.onNetworkSuccess(queue, false)
                break
            }

            // Notify success
            coreContract.onNetworkSuccess(queue, true)
            totalEventsSent += batchSize

            // Cleanup events from database
            if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
                coreContract.database().cleanupPushNotificationEvents(
                    context = context,
                    ids = queueData.eventIds
                )
            } else {
                coreContract.database().cleanupSentEvents(
                    context = context,
                    eventIds = queueData.eventIds,
                    profileEventIds = queueData.profileEventIds
                )
            }

            // Continue if we got a full batch (might be more events)
            continueProcessing = queueData.hasMore
        }

        logger.verbose(accountId, "Queue flush completed. Total events sent: $totalEventsSent")
    }

    fun getDelayFrequency(): Int {
        minDelayFrequency = networkRepo.getMinDelayFrequency(minDelayFrequency, networkRetryCount)
        logger.debug(accountId, "Setting delay frequency to $minDelayFrequency")
        return minDelayFrequency
    }

    @WorkerThread
    fun initHandshake(eventGroup: EventGroup, handshakeSuccessCallback: Runnable) {
        // Always set this to 0 so that handshake is not performed during HTTP failure
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

    @WorkerThread
    fun performHandshakeForDomain(
        eventGroup: EventGroup,
        handshakeSuccessCallback: Runnable
    ) {
        try {
            ctApiWrapper.ctApi.performHandshakeForDomain(
                eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED
            ).use { response ->
                if (response.isSuccess()) {
                    logger.verbose(accountId, "Received success from handshake :)")

                    if (shouldMuteSdk(response)) {
                        return
                    }

                    saveDomainChanges(response)
                    logger.verbose(accountId, "We are not muted")
                    // We have a new domain, run the callback
                    handshakeSuccessCallback.run()
                } else {
                    logger.verbose(
                        accountId,
                        "Invalid HTTP status code received for handshake - ${response.code}"
                    )
                }
            }
        } catch (e: Exception) {
            logger.verbose(accountId, "Failed to perform handshake!", e)
        }
    }

    /**
     * Processes the incoming response headers for a change in domain and/or mute.
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
        caller: String?,
        isUserSwitchFlush: Boolean = false
    ): Boolean {
        if (queue == null || queue.length() <= 0) {
            return false
        }

        if (coreContract.deviceInfo().deviceID == null) {
            logger.debug(accountId, "CleverTap Id not finalized, unable to send queue")
            return false
        }

        val endpointId: EndpointId = fromEventGroup(eventGroup)
        val queueHeader: JSONObject? = coreContract.networkHeaderForQueue(EndpointId.ENDPOINT_A1, caller)

        val requestBody = SendQueueRequestBody(queueHeader, queue)
        logger.debug(accountId, "Send queue contains ${queue.length()} items: $requestBody")

        try {
            return networkCall(eventGroup, requestBody, endpointId, isUserSwitchFlush)
        } catch (e: Exception) {
            networkRetryCount++
            responseFailureCount++
            logger.debug(accountId, "An exception occurred while sending the queue, will retry: ", e)
            coreContract.onFlushFailure(context)
            return false
        }
    }

    private fun networkCall(
        eventGroup: EventGroup,
        requestBody: SendQueueRequestBody,
        endpointId: EndpointId,
        isUserSwitchFlush: Boolean
    ): Boolean = callApiForEventGroup(eventGroup, requestBody).use { response ->
        networkRetryCount = 0

        // Route response based on event group
        return when (eventGroup) {
            EventGroup.VARIABLES -> {
                handleVariablesNetworkCall(response)
            }
            EventGroup.REGULAR -> {
                handleRegularNetworkCall(
                    response = response,
                    requestBody = requestBody,
                    endpointId = endpointId,
                    isUserSwitchFlush = isUserSwitchFlush
                )
            }
            EventGroup.PUSH_NOTIFICATION_VIEWED -> {
                handlePushImpressionsNetworkCall(
                    response = response,
                    queue = requestBody.queue
                )
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
        val config = coreContract.config()
        val coreMetaData = coreContract.coreMetaData()

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

    // ============ RESPONSE HANDLING - Delegates to CoreContract ============

    private fun handleVariablesNetworkCall(response: Response): Boolean {
        if (!response.isSuccess()) {
            handleVarsOrTemplatesResponseError(response, "Variables")
            responseFailureCount++
            return false
        }

        if (abortDueToDomainChange(response) || shouldMuteSdk(response)) {
            return false
        }

        saveDomainChanges(response)

        // Delegate to CoreContract for processing
        coreContract.handleVariablesResponse(response)

        responseFailureCount = 0
        return true
    }

    private fun handlePushImpressionsNetworkCall(
        response: Response,
        queue: JSONArray
    ): Boolean {
        if (!response.isSuccess()) {
            logger.info("Received error response code: ${response.code}")
            responseFailureCount++
            return false
        }

        if (abortDueToDomainChange(response) || shouldMuteSdk(response)) {
            return false
        }

        saveDomainChanges(response)
        logger.debug(accountId, "Push Impressions sent successfully")
        networkRepo.setLastRequestTs(currentRequestTimestamp)
        setFirstRequestTimestampIfNeeded(currentRequestTimestamp)

        // Delegate to CoreContract for processing
        coreContract.handlePushImpressionsResponse(response, queue)

        responseFailureCount = 0
        return true
    }

    private fun handleRegularNetworkCall(
        response: Response,
        requestBody: SendQueueRequestBody,
        endpointId: EndpointId,
        isUserSwitchFlush: Boolean
    ): Boolean {
        if (!response.isSuccess()) {
            handleSendQueueResponseError(response)
            responseFailureCount++
            return false
        }

        if (abortDueToDomainChange(response) || shouldMuteSdk(response)) {
            return false
        }

        saveDomainChanges(response)

        // Notify headers sent
        if (requestBody.queueHeader != null) {
            coreContract.notifyHeadersSent(requestBody.queueHeader, endpointId)
        }

        logger.debug(accountId, "Queue sent successfully")
        networkRepo.setLastRequestTs(currentRequestTimestamp)
        setFirstRequestTimestampIfNeeded(currentRequestTimestamp)

        val isFullResponse = doesBodyContainAppLaunchedOrFetchEvents(requestBody)

        // Delegate to CoreContract for full response processing
        coreContract.handleSendQueueResponse(
            response = response,
            isFullResponse = isFullResponse,
            requestBody = requestBody,
            endpointId = endpointId,
            isUserSwitchFlush = isUserSwitchFlush
        )

        responseFailureCount = 0
        return true
    }

    // ============ TEMPLATE DEFINITION ============

    @WorkerThread
    fun defineTemplates(templates: Collection<CustomTemplate>): Boolean {
        val header = coreContract.networkHeaderForQueue(EndpointId.TEMPLATES, null) ?: return false

        val body = DefineTemplatesRequestBody(header, templates)
        logger.debug(accountId, "Will define templates: $body")

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
            logger.debug(accountId, "An exception occurred while defining templates.", e)
            return false
        }
    }

    private fun handleTemplateResponseSuccess(response: Response) {
        logger.info(accountId, "Custom templates defined successfully.")
        val body = response.readBody().toJsonOrNull()
        if (body != null) {
            val warnings = body.optString("error")
            if (!TextUtils.isEmpty(warnings)) {
                logger.info(accountId, "Custom templates warnings: $warnings")
            }
        }
    }

    // ============ ERROR HANDLING ============

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
            }
            401 -> {
                logger.info(
                    logTag, "Unauthorized access from a non-test profile. " +
                            "Please mark this profile as a test profile from the CleverTap dashboard."
                )
            }
            else -> logger.info(logTag, "Response code ${response.code} while syncing.")
        }
    }

    fun abortDueToDomainChange(response: Response): Boolean {
        val newDomain: String? = response.getHeaderValue(CtApi.HEADER_DOMAIN_NAME)

        if (!newDomain.isNullOrBlank() && hasDomainChanged(newDomain)) {
            setDomain(newDomain)
            logger.debug(
                accountId,
                "The domain has changed to $newDomain. The request will be retried shortly."
            )
            return true
        }
        return false
    }

    private fun handleSendQueueResponseError(response: Response) {
        val coreMetaData = coreContract.coreMetaData()
        logger.info("Received error response code: ${response.code}")

        when (response.code) {
            419 -> {
                logger.verbose("There is decryption failure on backend, disabling encrypted requests.")
                coreMetaData.isRelaxNetwork = true
            }
            402 -> {
                logger.verbose("Encryption in transit feature not enabled for your account, please contact Clevertap support.")
                coreMetaData.isRelaxNetwork = true
            }
        }
    }

    private fun doesBodyContainAppLaunchedOrFetchEvents(body: SendQueueRequestBody): Boolean {
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

    // ============ DOMAIN MANAGEMENT ============

    @WorkerThread
    private fun setDomain(domainName: String?) {
        logger.verbose(accountId, "Setting domain to $domainName")
        networkRepo.setDomain(domainName)
        ctApiWrapper.ctApi.cachedDomain = domainName

        // Notify CoreContract about domain changes
        if (domainName != null) {
            coreContract.notifySCDomainAvailable(Utils.getSCDomain(domainName))
        } else {
            coreContract.notifySCDomainUnavailable()
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
        logger.verbose(accountId, "Setting spiky domain to $spikyDomainName")
        networkRepo.setSpikyDomain(spikyDomainName)
        ctApiWrapper.ctApi.cachedSpikyDomain = spikyDomainName
    }

    @WorkerThread
    private fun setMuted(mute: Boolean) {
        if (mute) {
            networkRepo.setMuted(true)
            networkRepo.setDomain(null)

            val config = coreContract.config()
            val context = coreContract.context()
            val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>()
            task.execute("CommsManager#setMuted") {
                coreContract.database().clearQueues(context)
            }
        } else {
            networkRepo.setMuted(false)
        }
    }
}