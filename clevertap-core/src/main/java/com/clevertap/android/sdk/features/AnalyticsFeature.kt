package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.SessionManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.validation.Validator
import org.json.JSONException
import org.json.JSONObject

/**
 * Analytics and event tracking
 * Manages event queues, analytics, and user sessions
 */
internal data class AnalyticsFeature(
    val analyticsManager: AnalyticsManager,
    val baseEventQueueManager: BaseEventQueueManager,
    val eventMediator: EventMediator,
    val sessionManager: SessionManager,
    val validator: Validator
) : CleverTapFeature {

    lateinit var coreContract: CoreContract

    fun networkFailed() {
        baseEventQueueManager.scheduleQueueFlush(coreContract.context())
    }

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context
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
}
