package com.clevertap.android.sdk.inapp

import android.icu.text.SimpleDateFormat
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.matchers.EventAdapter
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONObject
import java.util.Locale

object EvaluationManager {

    private val suppressedClientSideInApps: MutableList<Map<String, Any?>> = ArrayList()

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
    @JvmStatic
    fun evaluateOnAppLaunchedClientSide() {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        evaluateClientSide(event)
    }

    @JvmStatic
    fun evaluateOnAppLaunchedServerSide(appLaunchedNotifs: List<JSONObject>) {
        // BE returns applaunch_notifs [0, 1, 2]
        // record trigger counts
        // evaluate limits [2]
        // show first based on priority (2)

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

        val eligibleInApps = evaluate(event, ArrayList())// TODO replace with actual implementation
        val sortedInApps = sortByPriority(eligibleInApps)
        if (sortedInApps.isNotEmpty()) {
            val inApp = sortedInApps[0]
            if (shouldSuppress(inApp)) {
                suppress(inApp)
                return
            }
            updateTTL(inApp)
        }
        // TODO handle supressed inapps -> DONE
        // TODO calculate TTL field and put it in the json based on ttlOffset parameter -> DONE
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

    private fun shouldSuppress(inApp: JSONObject): Boolean {
        return inApp[Constants.INAPP_SUPPRESSED] as? Boolean ?: false
    }

    private fun suppress(inApp: JSONObject) {
        val ti = inApp[Constants.INAPP_ID_IN_PAYLOAD] as? String
        val wzrkId = ti?.let { generateWzrkId(ti) }
        val pivot = inApp["wzrk_pivot"] as? String ?: "wzrk_default"
        val cgId = inApp["wzrk_cgId"] as? Int

        suppressedClientSideInApps.add(
            mapOf(
                Constants.NOTIFICATION_ID_TAG to wzrkId,
                Constants.INAPP_WZRK_PIVOT to pivot,
                Constants.INAPP_WZRK_CGID to cgId
            )
        )
    }

    private fun generateWzrkId(ti: String): String {
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val date = dateFormatter.format(Clock.SYSTEM.newDate())
        return "${ti}_$date"
    }

    private fun updateTTL(inApp: JSONObject) {
        val offset = inApp[Constants.WZRK_TIME_TO_LIVE_OFFSET] as? Long
        if (offset != null) {
            val now = Clock.SYSTEM.currentTimeSeconds()
            val ttl = now + offset
            inApp.put(Constants.WZRK_TIME_TO_LIVE, ttl)
        } else {
            // Remove TTL since it cannot be calculated based on the TTL offset
            // The default TTL will be set in CTInAppNotification
            inApp.remove(Constants.WZRK_TIME_TO_LIVE)
        }
    }

}
