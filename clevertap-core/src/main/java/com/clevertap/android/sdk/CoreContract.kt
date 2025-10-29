package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.api.SendQueueRequestBody
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.utils.Clock
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
     * Collects headers from features to attach to request
     */
    fun getHeadersToAttach(endpointId: EndpointId): JSONObject?

    /**
     * Notifies SC Domain listener about domain availability
     */
    fun notifySCDomainAvailable(domain: String)

    /**
     * Notifies SC Domain listener about domain unavailability
     */
    fun notifySCDomainUnavailable()

    // ============ CORE DEPENDENCIES ACCESS ============

    fun context(): Context
    fun config(): CleverTapInstanceConfig
    fun deviceInfo(): DeviceInfo
    fun coreMetaData(): CoreMetaData
    fun database(): BaseDatabaseManager
    fun logger(): ILogger
    fun analytics(): AnalyticsManager

    fun clock(): Clock
}