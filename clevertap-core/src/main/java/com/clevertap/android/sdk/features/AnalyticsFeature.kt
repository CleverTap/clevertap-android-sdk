package com.clevertap.android.sdk.features

import android.os.Bundle
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.ProfileValueHandler
import com.clevertap.android.sdk.SessionManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.validation.Validator
import org.json.JSONException
import org.json.JSONObject

/**
 * Analytics and event tracking
 * Manages event queues, analytics, and user sessions
 */
internal class AnalyticsFeature(
    private val networkFeature: NetworkFeature,
    private val validator: Validator,
    private val profileValueHandler: ProfileValueHandler,
    private val loginInfoProvider: LoginInfoProvider
) : CleverTapFeature {

    lateinit var coreContract: CoreContract

    // Lazy-initialized Analytics dependencies (initialized after coreContract is set)
    val eventMediator: EventMediator by lazy {
        EventMediator(
            coreContract.config(),
            coreContract.coreMetaData(),
            coreContract.data().localDataStore,
            profileValueHandler,
            networkFeature.networkRepo
        )
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(
            coreContract.config(),
            coreContract.coreMetaData(),
            validator,
            coreContract.data().localDataStore
        )
    }

    val baseEventQueueManager: BaseEventQueueManager by lazy {
        EventQueueManager(
            eventMediator,
            sessionManager,
            networkFeature.networkManager,
            loginInfoProvider,
            coreContract
        )
    }

    val analyticsManager: AnalyticsManager by lazy {
        AnalyticsManager(
            context = coreContract.context(),
            config = coreContract.config(),
            baseEventQueueManager = baseEventQueueManager,
            validator = validator,
            validationResultStack = coreContract.validationResultStack(),
            coreMetaData = coreContract.coreMetaData(),
            deviceInfo = coreContract.deviceInfo(),
            currentTimeProvider = coreContract.clock(),
            executors = coreContract.executors()
        )
    }

    fun networkFailed() {
        baseEventQueueManager.scheduleQueueFlush(coreContract.context())
    }

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
    ) {
        // Handle "arp" (additional request parameters)
        try {
            if (response.has("arp")) {
                val arp = response.get("arp") as JSONObject
                if (arp.length() > 0) {
                    //Handle Discarded events in ARP
                    try {
                        processDiscardedEventsList(arp)
                    } catch (t: Throwable) {
                        coreContract.logger().verbose("Error handling discarded events response: " + t.localizedMessage)
                    }
                }
            }
        } catch (t: Throwable) {
            coreContract.logger().verbose(coreContract.config().accountId, "Failed to process ARP", t)
        }
    }

    /**
     * Dashboard has a feature where marketers can discard event. We get that list in the ARP response,
     * SDK then checks if the event is in the discarded list before sending it to LC
     */
    private fun processDiscardedEventsList(response: JSONObject) {
        if (!response.has(Constants.DISCARDED_EVENT_JSON_KEY)) {
            coreContract.logger().verbose(coreContract.config().accountId, "ARP doesn't contain the Discarded Events key")
            return
        }
        try {
            val discardedEventsList = ArrayList<String?>()
            val discardedEventsArray = response.getJSONArray(Constants.DISCARDED_EVENT_JSON_KEY)

            if (discardedEventsArray != null) {
                for (i in 0..<discardedEventsArray.length()) {
                    discardedEventsList.add(discardedEventsArray.getString(i))
                }
            }
            coreContract.coreMetaData().setDiscardedEvents(discardedEventsList)
        } catch (e: JSONException) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "Error parsing discarded events list" + e.getLocalizedMessage()
            )
        }
    }

    // ========== PUBLIC API FACADES ==========
    // These methods provide direct delegation from CleverTapAPI to Analytics functionality
    // Signature matches CleverTapAPI public methods for 1:1 mapping

    /**
     * Push an event with optional action data
     */
    fun pushEvent(eventName: String?, eventActions: MutableMap<String?, Any?>?) {
        analyticsManager.pushEvent(eventName, eventActions)
    }

    /**
     * Push a profile update
     */
    fun pushProfile(profile: Map<String?, Any?>?) {
        analyticsManager.pushProfile(profile)
    }

    /**
     * Push a charged event (purchase)
     */
    fun pushChargedEvent(
        chargeDetails: HashMap<String?, Any?>?,
        items: ArrayList<HashMap<String?, Any?>>?
    ) {
        analyticsManager.pushChargedEvent(chargeDetails, items)
    }

    /**
     * Push an error event
     */
    fun pushError(errorMessage: String?, errorCode: Int) {
        analyticsManager.pushError(errorMessage, errorCode)
    }

    /**
     * Push install referrer via URL string
     */
    fun pushInstallReferrer(url: String?) {
        analyticsManager.pushInstallReferrer(url)
    }

    /**
     * Push install referrer via UTM parameters
     */
    fun pushInstallReferrer(source: String?, medium: String?, campaign: String?) {
        analyticsManager.pushInstallReferrer(source, medium, campaign)
    }

    /**
     * Push a deep link
     */
    fun pushDeepLink(uri: android.net.Uri?, shouldRaiseCleverTapEvent: Boolean) {
        analyticsManager.pushDeepLink(uri, shouldRaiseCleverTapEvent)
    }

    /**
     * Add unique values to a multi-value profile property
     */
    fun addMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        analyticsManager.addMultiValuesForKey(key, values)
    }

    /**
     * Remove unique values from a multi-value profile property
     */
    fun removeMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        analyticsManager.removeMultiValuesForKey(key, values)
    }

    /**
     * Set unique values as a multi-value profile property (overwrites existing)
     */
    fun setMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        analyticsManager.setMultiValuesForKey(key, values)
    }

    /**
     * Remove a profile property
     */
    fun removeValueForKey(key: String?) {
        analyticsManager.removeValueForKey(key)
    }

    /**
     * Increment a numeric profile property
     */
    fun incrementValue(key: String?, value: Number?) {
        analyticsManager.incrementValue(key, value)
    }

    /**
     * Decrement a numeric profile property
     */
    fun decrementValue(key: String?, value: Number?) {
        analyticsManager.decrementValue(key, value)
    }

    /**
     * Record a screen view
     */
    fun recordPageEventWithExtras(jsonObject: JSONObject?) {
        analyticsManager.recordPageEventWithExtras(jsonObject)
    }

    /**
     * Flush all queued events
     */
    fun flush() {
        baseEventQueueManager.flush()
    }

    /**
     * Push notification viewed event
     */
    fun pushNotificationViewedEvent(extras: android.os.Bundle?) {
        analyticsManager.pushNotificationViewedEvent(extras)
    }

    /**
     * Push inbox message state event (clicked or viewed)
     */
    fun pushInboxMessageStateEvent(
        clicked: Boolean,
        message: CTInboxMessage,
        data: Bundle?
    ) {
        analyticsManager.pushInboxMessageStateEvent(clicked, message, data)
    }

    /**
     * Raise geofence event (entered or exited)
     */
    fun raiseEventForGeofences(
        eventName: String?,
        geofenceProperties: JSONObject
    ): java.util.concurrent.Future<*>? {
        return analyticsManager.raiseEventForGeofences(eventName, geofenceProperties)
    }

    /**
     * Raise signed call event
     */
    fun raiseEventForSignedCall(
        eventName: String?,
        eventProperties: JSONObject?
    ): java.util.concurrent.Future<*>? {
        return analyticsManager.raiseEventForSignedCall(eventName, eventProperties)
    }

    /**
     * Send fetch event (for variables/inapps)
     */
    fun sendFetchEvent(event: JSONObject?) {
        analyticsManager.sendFetchEvent(event)
    }

    /**
     * Push define vars event (sync variables to server)
     */
    fun pushDefineVarsEvent(defineVarsData: JSONObject?) {
        analyticsManager.pushDefineVarsEvent(defineVarsData)
    }

    /**
     * Get user's last visit timestamp
     */
    fun getUserLastVisitTs(): Long {
        return sessionManager.userLastVisitTs
    }

    /**
     * Generate empty multi-value error
     */
    fun generateEmptyMultiValueError(key: String?) {
        analyticsManager._generateEmptyMultiValueError(key)
    }
}
