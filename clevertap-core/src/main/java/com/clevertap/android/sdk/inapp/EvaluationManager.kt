package com.clevertap.android.sdk.inapp

import android.icu.text.SimpleDateFormat
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.matchers.EventAdapter
import com.clevertap.android.sdk.inapp.matchers.LimitsMatcher
import com.clevertap.android.sdk.inapp.matchers.TriggersMatcher
import com.clevertap.android.sdk.network.BatchListener
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.toList
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class EvaluationManager constructor(
    private val inappController: InAppController,
    private val triggersMatcher: TriggersMatcher,
    private val triggersManager: TriggerManager,
    private val impressionStore: ImpressionStore,
    private val impressionManager: ImpressionManager,
    private val limitsMatcher: LimitsMatcher,
    private val inAppStore: InAppStore,
): BatchListener {
    val evaluatedServerSideInAppIds: MutableList<String>
        get() = evaluatedServerSideInAppIds
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
    private fun evaluateOnAppLaunchedClientSide() {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        evaluateClientSide(event)
    }

    fun evaluateOnAppLaunchedServerSide(appLaunchedNotifs: List<JSONObject>) {
        // BE returns applaunch_notifs [0, 1, 2]
        // record trigger counts
        // evaluate limits [2]
        // show first based on priority (2)
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        val eligibleInApps = evaluate(event, appLaunchedNotifs)
        val sortedInApps = sortByPriority(eligibleInApps)

        val inAppNotificationsToQueue: MutableList<JSONObject> = mutableListOf()
        for (inApp in sortedInApps) {
            if (!shouldSuppress(inApp)) {
                inAppNotificationsToQueue.add(inApp)
                break
            }

            suppress(inApp)
        }

        inappController.addInAppNotificationsToQueue(JSONArray(inAppNotificationsToQueue))
        // TODO handle supressed inapps - DONE
        // TODO eligibleInapps.sort().first().display(); - DONE
    }

    private fun evaluateServerSide(event: EventAdapter) {
        val eligibleInApps = evaluate(event, inAppStore.readServerSideInApps().toList())
        // TODO add to meta inapp_evals : eligibleInapps.addToMeta();
        for (inApp in eligibleInApps) {
            val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)
            if (campaignId.isNotEmpty()) {
                evaluatedServerSideInAppIds.add(campaignId)
            }
        }
    }

    private fun evaluateClientSide(event: EventAdapter) {
        val eligibleInApps = evaluate(event, inAppStore.readClientSideInApps().toList()) // TODO replace with actual implementation -DONE
        val sortedInApps = sortByPriority(eligibleInApps)
        if (sortedInApps.isNotEmpty()) {
            val inApp = sortedInApps[0]
            if (shouldSuppress(inApp)) {
                suppress(inApp)
                return
            }

            updateTTL(inApp)
            inappController.addInAppNotificationsToQueue(JSONArray(inApp))
        }
        // TODO handle supressed inapps -> DONE
        // TODO calculate TTL field and put it in the json based on ttlOffset parameter -> DONE
        // TODO eligibleInapps.sort().first().display(); - DONE
    }

    private fun evaluate(event: EventAdapter, inappNotifs: List<JSONObject>): List<JSONObject> {
        // TODO: whenTriggers - DONE
        // TODO: record trigger - DONE
        // TODO: whenLimits - DONE
        val eligibleInApps: MutableList<JSONObject> = mutableListOf()
        for (inApp in inappNotifs) {
            val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)
            val whenTriggers = inApp.optJSONArray(Constants.INAPP_WHEN_TRIGGERS).orEmptyArray()
            val matchesTrigger = triggersMatcher.matchEvent(whenTriggers, event.eventName, event.eventProperties)
            if (matchesTrigger) {
                triggersManager.increment(campaignId)
                val frequencyLimits = inApp.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()
                val occurrenceLimits = inApp.optJSONArray(Constants.INAPP_OCCURRENCE_LIMITS).orEmptyArray()

                val whenLimits: MutableList<JSONObject> = mutableListOf()
                whenLimits.addAll(frequencyLimits.toList())
                whenLimits.addAll(occurrenceLimits.toList())

                val matchesLimits = limitsMatcher.matchWhenLimits(whenLimits, campaignId)
                if (matchesLimits) {
                    eligibleInApps.add(inApp)
                }
            }
        }
        return eligibleInApps //// returns eligible inapps
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

    fun onAppLaunchedWithSuccess() {
        evaluateOnAppLaunchedClientSide()
    }

    override fun onBatchSent(batch: JSONArray, success: Boolean) {
        if (success) {
            val header = batch[0] as JSONObject
            removeSentEvaluatedServerSideInAppIds(header)
            removeSentSuppressedClientSideInApps(header)
        }
    }

    private fun removeSentEvaluatedServerSideInAppIds(header: JSONObject) {
        val inAppsEval = header.optJSONArray(Constants.INAPP_SS_EVAL_META)
        inAppsEval?.let {
            for (i in 0 until it.length()) {
                val inAppId = it.optString(i)
                evaluatedServerSideInAppIds.remove(inAppId)
            }
        }
    }

    private fun removeSentSuppressedClientSideInApps(header: JSONObject) {
        val inAppsEval = header.optJSONArray(Constants.INAPP_SUPPRESSED_META)
        inAppsEval?.let {
            val iterator = suppressedClientSideInApps.iterator()
            while (iterator.hasNext()) {
                val suppressedInApp = iterator.next()
                val inAppId = suppressedInApp[Constants.NOTIFICATION_ID_TAG] as? String
                if (inAppId != null && inAppsEval.toString().contains(inAppId)) {
                    iterator.remove()
                }
            }
        }
    }
}
