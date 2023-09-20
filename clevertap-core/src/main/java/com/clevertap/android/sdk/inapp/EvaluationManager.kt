package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.matchers.EventAdapter
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONObject

class EvaluationManager {

    fun evaluateOnEvent(eventName: String, eventProperties: Map<String, Any>) {
        if (eventName != "App Launched") {
            val event = EventAdapter(eventName, eventProperties)
            evaluateServerSide(event)
            evaluateClientSide(event)
        }
    }

    fun evaluateOnChargedEvent(
        eventName: String,
        details: Map<String, Any>,
        items: List<Map<String, Any>>
    ) {
        val event = EventAdapter(eventName, details, items)
        evaluateServerSide(event)
        evaluateClientSide(event)
    }

    // onBatchSent with App Launched event in batch
    fun evaluateOnAppLaunchedClientSide() {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        evaluateClientSide(event)
    }

    fun evaluateOnAppLaunchedServerSide(appLaunchedNotifs: List<JSONObject>) {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        evaluate(event, appLaunchedNotifs)
        // TODO handle supressed inapps
        // TODO eligibleInapps.sort().first().display();
    }

    private fun evaluateServerSide(event: EventAdapter) {
        // evaluate(event, getServerSideNotifsFromStore())
        // TODO add to meta inapp_evals : eligibleInapps.addToMeta();
    }

    private fun evaluateClientSide(event: EventAdapter) {
        // evaluate(event, getClientSideNotifsFromStore())
        // TODO handle supressed inapps
        // TODO calculate TTL field and put it in the json based on ttlOffset parameter
        // TODO eligibleInapps.sort().first().display();
    }

    private fun evaluate(event: EventAdapter, inappNotifs: List<JSONObject>): List<JSONObject> {
        // TODO: whenTriggers
        // TODO: record trigger
        // TODO: whenLimits
        return listOf() // returns eligible inapps
    }

    /**
     * Sorts list of InApp objects with priority(100 highest - 1 lowest) and if equal priority
     * then then the one created earliest
     */
    fun sortByPriority(inApps: List<JSONObject>): List<JSONObject> {
        val priority: (JSONObject) -> Int = { inApp ->
            inApp.opt("priority") as? Int ?: 1
        }

        val ti: (JSONObject) -> String = { inApp ->
            inApp.opt(Constants.INAPP_ID_IN_PAYLOAD) as? String
                ?: (Clock.SYSTEM.newDate().time / 1000).toString()
        }
        // Sort by priority descending and then by timestamp ascending
        return inApps.sortedWith(compareByDescending<JSONObject> { priority(it) }.thenBy { ti(it) })

    }

}
