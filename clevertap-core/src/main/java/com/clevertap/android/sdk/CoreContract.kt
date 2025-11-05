package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.features.DataFeature
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.api.SendQueueRequestBody
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationResultStack
import org.json.JSONArray
import org.json.JSONObject

/**
 * CoreContract serves as the central mediator for network responses and feature communication.
 * Implemented by CoreState to delegate responses to appropriate features.
 */
internal interface CoreContract {

    // ============ DATABASE QUEUES ============
    fun didNotFlush()

    // ============ NETWORK RESPONSE ROUTING ============

    /**
     * Handles the response from regular event queue API (/a1)
     * Routes to appropriate features based on response content
     */
    fun handleSendQueueResponse(
        response: Response,
        isFullResponse: Boolean,
        requestBody: SendQueueRequestBody,
        endpointId: EndpointId,
        isUserSwitchFlush: Boolean
    )

    /**
     * Handles the response from content fetch APIs (inbox, product config, display units).
     * Routes the response to the appropriate feature handler (e.g., CTInboxController, ProductConfig).
     * (Currently clevertap only supports this for Native Displays)
     *
     * Note: Called only on success of api call.
     *
     * @param response The network response object containing the fetched content.
     */
    fun handleContentResponseData(
        response: JSONObject,
        isUserSwitchFlush: Boolean
    )

    /**
     * Handles the response from variables API (/vars)
     * Routes to VariablesFeature and ARPResponse
     */
    fun handleVariablesResponse(response: Response)

    /**
     * Handles the response from push impressions API
     * Routes to PushFeature for notification tracking
     */
    fun handlePushImpressionsResponse(response: Response, queue: JSONArray)

    // ============ NETWORK CALLBACKS ============

    /**
     * Notifies features of network error
     */
    fun onNetworkError()

    /**
     * Notifies features of successful network call
     */
    fun onNetworkSuccess(queue: JSONArray, success: Boolean)

    /**
     * Notifies features that network flush failed
     */
    fun onFlushFailure(context: Context)

    // ============ NETWORK HEADER MANAGEMENT ============

    /**
     * Notifies features that headers were sent
     */
    fun notifyHeadersSent(allHeaders: JSONObject, endpointId: EndpointId)

    /**
     * Notifies SC Domain listener about domain availability
     */
    fun notifySCDomainAvailable(domain: String)

    /**
     * Notifies SC Domain listener about domain unavailability
     */
    fun notifySCDomainUnavailable()

    /**
     * Builds the first json item to be sent in queue for clevertap api calls
     */
    fun networkHeaderForQueue(endpointId: EndpointId, caller: String?): JSONObject?

    fun evaluateInAppForEvent(context: Context, event: JSONObject, eventType: Int)

    // ============ CORE DEPENDENCIES ACCESS ============

    fun context(): Context
    fun config(): CleverTapInstanceConfig
    fun deviceInfo(): DeviceInfo
    fun coreMetaData(): CoreMetaData
    fun database(): BaseDatabaseManager
    fun logger(): ILogger
    fun analytics(): AnalyticsManager
    fun clock(): Clock
    fun executors(): CTExecutors
    fun mainLooperHandler(): MainLooperHandler
    fun validationResultStack(): ValidationResultStack
    fun data(): DataFeature // note: eventually break this dependency
}