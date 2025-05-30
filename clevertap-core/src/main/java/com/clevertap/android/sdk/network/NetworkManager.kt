package com.clevertap.android.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.areAppNotificationsEnabled
import com.clevertap.android.sdk.copyFrom
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.QueueData
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.evaluation.EventType.Companion.fromBoolean
import com.clevertap.android.sdk.login.IdentityRepoFactory
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
import com.clevertap.android.sdk.response.ConsoleResponse
import com.clevertap.android.sdk.response.DisplayUnitResponse
import com.clevertap.android.sdk.response.FeatureFlagResponse
import com.clevertap.android.sdk.response.FetchVariablesResponse
import com.clevertap.android.sdk.response.GeofenceResponse
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.response.InboxResponse
import com.clevertap.android.sdk.response.MetadataResponse
import com.clevertap.android.sdk.response.ProductConfigResponse
import com.clevertap.android.sdk.response.PushAmpResponse
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.toJsonOrNull
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.SecureRandom
import com.clevertap.android.sdk.isNotNullAndBlank
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_DOMAIN_NAME
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_ENCRYPTION_ENABLED
import com.clevertap.android.sdk.network.api.EncryptionFailure

internal class NetworkManager(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo,
    private val coreMetaData: CoreMetaData,
    private val validationResultStack: ValidationResultStack,
    private val controllerManager: ControllerManager,
    private val databaseManager: BaseDatabaseManager,
    private val callbackManager: BaseCallbackManager,
    ctLockManager: CTLockManager,
    private val validator: Validator,
    inAppResponse: InAppResponse,
    private val ctApiWrapper: CtApiWrapper,
    private val encryptionManager: NetworkEncryptionManager,
    private val ijRepo: IJRepo,
    private val arpRepo: ArpRepo,
    val cleverTapResponses: MutableList<CleverTapResponse> = mutableListOf(
        inAppResponse,
        MetadataResponse(config, deviceInfo, ijRepo),
        ARPResponse(config, validator, controllerManager, arpRepo),
        ConsoleResponse(config),
        InboxResponse(
            config, ctLockManager,
            callbackManager,
            controllerManager
        ),
        PushAmpResponse(
            context,
            config,
            databaseManager,
            callbackManager,
            controllerManager
        ),
        FetchVariablesResponse(config, controllerManager, callbackManager),
        DisplayUnitResponse(config, callbackManager, controllerManager),
        FeatureFlagResponse(config, controllerManager),
        ProductConfigResponse(config, coreMetaData, controllerManager),
        GeofenceResponse(config, callbackManager)
    ),
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

    val delayFrequency: Int
        //gives delay frequency based on region
        get() {
            logger.debug(config.accountId, "Network retry #$networkRetryCount")

            //Retry with delay as 1s for first 10 retries
            if (networkRetryCount < 10) {
                logger.debug(
                    config.accountId,
                    "Failure count is $networkRetryCount. Setting delay frequency to 1s"
                )
                minDelayFrequency =
                    Constants.PUSH_DELAY_MS //reset minimum delay to 1s
                return minDelayFrequency
            }

            if (config.accountRegion == null) {
                //Retry with delay as 1s if region is null in case of eu1
                logger.debug(config.accountId, "Setting delay frequency to 1s")
                return Constants.PUSH_DELAY_MS
            } else {
                //Retry with delay as minimum delay frequency and add random number of seconds to scatter traffic
                val randomGen = SecureRandom()
                val randomDelay = (randomGen.nextInt(10) + 1) * 1000
                minDelayFrequency += randomDelay
                if (minDelayFrequency < Constants.MAX_DELAY_FREQUENCY) {
                    logger.debug(
                        config.accountId,
                        "Setting delay frequency to $minDelayFrequency"
                    )
                    return minDelayFrequency
                } else {
                    minDelayFrequency = Constants.PUSH_DELAY_MS
                }
                logger.debug(
                    config.accountId,
                    "Setting delay frequency to $minDelayFrequency"
                )
                return minDelayFrequency
            }
        }

    @WorkerThread
    fun initHandshake(eventGroup: EventGroup, handshakeSuccessCallback: Runnable) {
        // Always set this to 0 so that the handshake is not performed during a HTTP failure
        responseFailureCount = 0
        performHandshakeForDomain(context, eventGroup, handshakeSuccessCallback)
    }

    @WorkerThread
    fun needsHandshakeForDomain(eventGroup: EventGroup): Boolean {
        val needsHandshake = ctApiWrapper.needsHandshake(
            eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED
        )
        val needHandshakeDueToFailure = responseFailureCount > 5

        if (needHandshakeDueToFailure) {
            setDomain(context, null)
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

    private val firstRequestTimestamp: Int
        get() = StorageHelper.getIntFromPrefs(
            context,
            config,
            Constants.KEY_FIRST_TS,
            0
        )

    private var lastRequestTimestamp: Int
        get() = StorageHelper.getIntFromPrefs(
            context,
            config,
            Constants.KEY_LAST_TS,
            0
        )
        set(ts) {
            StorageHelper.putInt(
                context,
                StorageHelper.storageKeyWithSuffix(
                    config.accountId,
                    Constants.KEY_LAST_TS
                ),
                ts
            )
        }

    private fun hasDomainChanged(newDomain: String): Boolean {
        val oldDomain = StorageHelper.getStringFromPrefs(context, config, Constants.KEY_DOMAIN_NAME, null)
        return newDomain != oldDomain
    }

    /**
     * Constructs a header [JSONObject] to be included as a first element of a sendQueue request
     *
     * @param context The Context object.
     * @param caller  The optional caller identifier.
     */
    private fun getQueueHeader(context: Context, caller: String?): JSONObject? {
        try {
            // Construct the header JSON object
            val header = JSONObject()

            // Add caller if available
            if (caller != null) {
                header.put(Constants.D_SRC, caller)
            }

            // Add device ID
            val deviceId = deviceInfo.deviceID
            if (deviceId != null && deviceId != "") {
                header.put("g", deviceId)
            } else {
                logger.verbose(
                    config.accountId,
                    "CRITICAL: Couldn't finalise on a device ID! Using error device ID instead!"
                )
            }

            // Add type as "meta"
            header.put("type", "meta")

            // Add app fields
            val appFields = deviceInfo.appLaunchedFields
            if (coreMetaData.isWebInterfaceInitializedExternally) {
                appFields.put("wv_init", true)
            }
            header.put("af", appFields)

            // Add _i and _j if available
            val i = ijRepo.getI(context)
            if (i > 0) {
                header.put("_i", i)
            }

            val j = ijRepo.getJ(context)
            if (j > 0) {
                header.put("_j", j)
            }

            val accountId = config.accountId
            val token = config.accountToken

            if (accountId == null || token == null) {
                logger.debug(
                    config.accountId,
                    "Account ID/token not found, unable to configure queue request"
                )
                return null
            }

            // Add account ID, token, and timestamps
            header.put("id", accountId)
            header.put("tk", token)
            header.put("l_ts", lastRequestTimestamp)
            header.put("f_ts", firstRequestTimestamp)

            // Add ct_pi (identities)
            header.put(
                "ct_pi", IdentityRepoFactory
                    .getRepo(this.context, config, validationResultStack).identitySet.toString()
            )

            // Add ddnd (Do Not Disturb)
            header.put(
                "ddnd",
                !(this.context.areAppNotificationsEnabled()
                        && (controllerManager.pushProviders == null
                        || controllerManager.pushProviders.isNotificationSupported))
            )

            // Add bk (Background Ping) if required
            if (coreMetaData.isBgPing) {
                header.put("bk", 1)
                coreMetaData.isBgPing = false
            }
            // Add rtl (Rendered Target List)
            header.put(
                "rtl", CTJsonConverter.getRenderedTargetList(
                    databaseManager.loadDBAdapter(
                        this.context
                    )
                )
            )

            // Add rct and ait (Referrer Click Time and App Install Time) if not sent before
            if (!coreMetaData.isInstallReferrerDataSent) {
                header.put("rct", coreMetaData.referrerClickTime)
                header.put("ait", coreMetaData.appInstallTime)
            }
            // Add frs (First Request in Session) and update first request flag
            header.put("frs", coreMetaData.isFirstRequestInSession)

            // Add debug flag to show errors and events on the integration-debugger
            if (CleverTapAPI.getDebugLevel() == 3) {
                header.put("debug", true)
            }

            coreMetaData.isFirstRequestInSession = false

            //Add ARP (Additional Request Parameters)
            try {
                val arp = arpRepo.getARP(context)
                if (arp != null && arp.length() > 0) {
                    header.put("arp", arp)
                }
            } catch (e: JSONException) {
                logger.verbose(config.accountId, "Failed to attach ARP", e)
            }

            // Add ref (Referrer Information)
            val ref = JSONObject()
            try {
                val utmSource = coreMetaData.source
                if (utmSource != null) {
                    ref.put("us", utmSource)
                }

                val utmMedium = coreMetaData.medium
                if (utmMedium != null) {
                    ref.put("um", utmMedium)
                }

                val utmCampaign = coreMetaData.campaign
                if (utmCampaign != null) {
                    ref.put("uc", utmCampaign)
                }

                if (ref.length() > 0) {
                    header.put("ref", ref)
                }
            } catch (e: JSONException) {
                logger.verbose(config.accountId, "Failed to attach ref", e)
            }

            // Add wzrk_ref (CleverTap-specific Parameters)
            val wzrkParams = coreMetaData.wzrkParams
            if (wzrkParams != null && wzrkParams.length() > 0) {
                header.put("wzrk_ref", wzrkParams)
            }

            // Attach InAppFC to header if available
            if (controllerManager.inAppFCManager != null) {
                Logger.v("Attaching InAppFC to Header")
                header.put("imp", controllerManager.inAppFCManager.shownTodayCount)
                header.put("tlc", controllerManager.inAppFCManager.getInAppsCount(context))
            } else {
                logger.verbose(
                    config.accountId,
                    "controllerManager.getInAppFCManager() is NULL, not Attaching InAppFC to Header"
                )
            }

            return header
        } catch (e: JSONException) {
            logger.verbose(config.accountId, "CommsManager: Failed to attach header", e)
            return null
        }
    }

    @WorkerThread
    private fun performHandshakeForDomain(
        context: Context,
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

        setMuted(context, false)
        setDomain(context, domainName)
        Logger.v("Setting spiky domain from header as -$spikyDomainName")
        if (spikyDomainName == null) {
            setSpikyDomain(context, domainName)
        } else {
            setSpikyDomain(context, spikyDomainName)
        }
    }

    private fun shouldMuteSdk(response: Response): Boolean {
        response.getHeaderValue(CtApi.HEADER_MUTE)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { muteCommand ->
                // muteCommand is guaranteed to be non-null and non-empty here
                if (muteCommand == "true") {
                    setMuted(context, true)
                    return true
                } else {
                    setMuted(context, false)
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
        val queueHeader: JSONObject? = getQueueHeader(context, caller)
        applyQueueHeaderListeners(queueHeader, endpointId, queue.optJSONObject(0).has("profile"))

        val requestBody = SendQueueRequestBody(queueHeader, queue)
        logger.debug(config.accountId, "Send queue contains " + queue.length() + " items: " + requestBody)
        try {
            callApiForEventGroup(eventGroup, requestBody).use { response ->
                networkRetryCount = 0
                val isProcessed = when (eventGroup) {
                    EventGroup.VARIABLES -> {
                        handleVariablesResponse(response = response)
                    }
                    EventGroup.REGULAR -> {
                        handleSendQueueResponse(
                            eventGroup = eventGroup,
                            response = response,
                            isFullResponse = doesBodyContainAppLaunchedOrFetchEvents(requestBody),
                            notifyNetworkHeaderListeners = {
                                notifyHeaderListeners(
                                    requestBody,
                                    endpointId
                                )
                            }
                        )
                    }
                    EventGroup.PUSH_NOTIFICATION_VIEWED -> {
                        handlePushImpressionsResponse(response = response)
                    }
                }

                if (isProcessed) {
                    responseFailureCount = 0
                } else {
                    responseFailureCount++
                }
                return isProcessed
            }
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
    fun defineTemplates(
        context: Context,
        templates: Collection<CustomTemplate>
    ): Boolean {
        val header = getQueueHeader(context, null) ?: return false

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
    private fun callApiForEventGroup(eventGroup: EventGroup, body: SendQueueRequestBody): Response {
        return when (eventGroup) {
            EventGroup.VARIABLES -> {
                ctApiWrapper.ctApi.defineVars(body)
            }
            EventGroup.REGULAR -> {
                sendQueueApi(eventGroup, body)
            }
            EventGroup.PUSH_NOTIFICATION_VIEWED -> {
                sendImpressionsApi(eventGroup, body)
            }
        }
    }

    private fun sendQueueApi(eventGroup: EventGroup, body: SendQueueRequestBody): Response {
        val response: Response
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
                response = ctApiWrapper.ctApi.sendQueue(
                    isViewedEvent = false,
                    body = bodyEnc,
                    isEncrypted = true
                )
            } else {
                logger.verbose("Normal Request cause encryption failed = $body")
                response = ctApiWrapper.ctApi.sendQueue(
                    isViewedEvent = false,
                    body = body.toString(),
                    isEncrypted = false
                )
            }
        } else {
            response = ctApiWrapper.ctApi.sendQueue(
                isViewedEvent = eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED,
                body = body.toString(),
                isEncrypted = false
            )
        }
        return response
    }

    private fun sendImpressionsApi(eventGroup: EventGroup, body: SendQueueRequestBody): Response {
        val response: Response = ctApiWrapper.ctApi.sendQueue(
                isViewedEvent = eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED,
                body = body.toString(),
                isEncrypted = false
            )
        return response
    }

    private fun handleVariablesResponse(response: Response): Boolean {
        if (response.isSuccess()) {
            val bodyString = response.readBody()
            val bodyJson = bodyString.toJsonOrNull()

            logger.verbose(config.accountId, "Processing variables response : $bodyJson")

            ARPResponse(config, validator, controllerManager, arpRepo)
                .processResponse(bodyJson, bodyString, this.context)
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
        lastRequestTimestamp = currentRequestTimestamp
        setFirstRequestTimestampIfNeeded(currentRequestTimestamp)

        logger.verbose(config.accountId, "Processing response : ${response.readBody().toJsonOrNull()}")
        return true
    }

    @WorkerThread
    private fun handleSendQueueResponse(
        eventGroup: EventGroup,
        response: Response,
        isFullResponse: Boolean,
        notifyNetworkHeaderListeners: () -> Unit
    ): Boolean {
        if (!response.isSuccess()) {
            handleSendQueueResponseError(eventGroup, response)
            return false
        }

        if (abortDueToDomainChange(response) || shouldMuteSdk(response)) {
            return false
        }

        saveDomainChanges(response)
        notifyNetworkHeaderListeners()

        logger.debug(config.accountId, "Queue sent successfully")
        lastRequestTimestamp = currentRequestTimestamp
        setFirstRequestTimestampIfNeeded(currentRequestTimestamp)

        val isEncryptedResponse = response.getHeaderValue(HEADER_ENCRYPTION_ENABLED).toBoolean()
        var bodyString: String? = response.readBody()
        var bodyJson: JSONObject? = bodyString.toJsonOrNull()

        if (isEncryptedResponse && bodyString != null) {

            logger.verbose("Encrypted response = $bodyString")

            val decryptResponse = encryptionManager.decryptResponse(bodyString = bodyString)
            when (decryptResponse) {
                is EncryptionFailure -> {
                    logger.verbose(config.accountId, "Failed to decrypt response")
                    return false // todo lp check if this should be considered as nw failure?
                }
                is EncryptionSuccess -> {
                    // reassign before processing responses
                    bodyString = decryptResponse.data
                    bodyJson = bodyString.toJsonOrNull()
                }
            }

        } else {
            // no-op, we have assigned body already
        }

        logger.verbose(config.accountId, "Processing response : $bodyJson")

        for (processor: CleverTapResponse in cleverTapResponses) {
            processor.isFullResponse = isFullResponse
            processor.processResponse(bodyJson, bodyString, context)
        }

        return true
    }

    fun abortDueToDomainChange(response: Response): Boolean {
        val newDomain: String? = response.getHeaderValue(HEADER_DOMAIN_NAME)

        if (newDomain.isNotNullAndBlank() && hasDomainChanged(newDomain)) {
            setDomain(context, newDomain)
            logger.debug(
                config.accountId,
                "The domain has changed to $newDomain. The request will be retried shortly."
            )
            return true
        }
        return false
    }

    private fun handleSendQueueResponseError(
        eventGroup: EventGroup,
        response: Response
    ) {
        logger.info("Received error response code: " + response.code)
        when (eventGroup) {
            EventGroup.REGULAR -> {
                when (response.code) {
                    419 -> {
                        coreMetaData.isRelaxNetwork = true
                    }
                    else -> {
                        // no-op
                    }
                }
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
    private fun setDomain(
        context: Context,
        domainName: String?
    ) {
        logger.verbose(config.accountId, "Setting domain to $domainName")
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_DOMAIN_NAME),
            domainName
        )
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
        if (firstRequestTimestamp > 0) {
            return
        }
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_FIRST_TS),
            ts
        )
    }

    @WorkerThread
    private fun setSpikyDomain(context: Context, spikyDomainName: String) {
        logger.verbose(config.accountId, "Setting spiky domain to $spikyDomainName")
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.SPIKY_KEY_DOMAIN_NAME),
            spikyDomainName
        )
        ctApiWrapper.ctApi.cachedSpikyDomain = spikyDomainName
    }

    @WorkerThread
    private fun setMuted(context: Context, mute: Boolean) {
        if (mute) {
            val now = (System.currentTimeMillis() / 1000).toInt()
            StorageHelper.putInt(
                context,
                StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_MUTED),
                now
            )
            setDomain(context, null)

            // Clear all the queues
            val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>()
            task.execute("CommsManager#setMuted") {
                databaseManager.clearQueues(context)
            }
        } else {
            StorageHelper.putInt(
                context,
                StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_MUTED),
                0
            )
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
