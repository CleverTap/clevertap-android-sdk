package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.SessionManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventMediator
import org.json.JSONObject

/**
 * Analytics and event tracking
 * Manages event queues, analytics, and user sessions
 */
internal data class AnalyticsFeature(
    val analyticsManager: AnalyticsManager,
    val baseEventQueueManager: BaseEventQueueManager,
    val eventMediator: EventMediator,
    val sessionManager: SessionManager
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
        // no-op
        // this feature does not behave in response to api call for now.
    }
}
