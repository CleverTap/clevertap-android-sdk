package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.network.BatchListener
import com.clevertap.android.sdk.response.data.InAppBase
import com.clevertap.android.sdk.response.data.InAppClientSide
import com.clevertap.android.sdk.response.data.InAppServerSide
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class EvaluationManager constructor(
    //private val inappController: InAppController,
    private val triggersMatcher: TriggersMatcher,
    private val triggersManager: TriggerManager,
    private val impressionStore: ImpressionStore,
    private val impressionManager: ImpressionManager,
    private val limitsMatcher: LimitsMatcher,
    private val inAppStore: InAppStore,
) : BatchListener {

    val evaluatedServerSideInAppIds: MutableList<String>
        get() = evaluatedServerSideInAppIds
    private val suppressedClientSideInApps: MutableList<Map<String, Any?>> = ArrayList()

    fun evaluateOnEvent(eventName: String, eventProperties: Map<String, Any>): JSONArray {
        val event = EventAdapter(eventName, eventProperties)
        evaluateServerSide(event)
        return evaluateClientSide(event)
    }

    fun evaluateOnChargedEvent(
        details: Map<String, Any>, items: List<Map<String, Any>>
    ): JSONArray {
        val event = EventAdapter(Constants.CHARGED_EVENT, details, items)
        evaluateServerSide(event)
        return evaluateClientSide(event)
    }

    // onBatchSent with App Launched event in batch
    fun evaluateOnAppLaunchedClientSide() {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        evaluateClientSide(event)
    }

    fun evaluateOnAppLaunchedServerSide(appLaunchedNotifs: List<InAppServerSide>) {
        // BE returns applaunch_notifs [0, 1, 2]
        // record trigger counts
        // evaluate limits [2]
        // show first based on priority (2)
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        val eligibleInApps = evaluate(event, appLaunchedNotifs)
        val sortedInApps = sortByPriority(eligibleInApps)

        val inAppNotificationsToQueue: MutableList<JSONObject> = mutableListOf()
        for (inApp in sortedInApps) {
            if (!inApp.shouldSuppress) {
                inAppNotificationsToQueue.add(((inApp as InAppServerSide).toJsonObject())) //TODO check this
                break
            }

            suppress(inApp)
        }

        //inappController.addInAppNotificationsToQueue(JSONArray(inAppNotificationsToQueue))
        // TODO handle supressed inapps - DONE
        // TODO eligibleInapps.sort().first().display(); - DONE
    }

    private fun evaluateServerSide(event: EventAdapter) {
        val eligibleInApps =
            evaluate(event, InAppServerSide.getListFromJsonArray(inAppStore.readServerSideInApps()))
        // TODO add to meta inapp_evals : eligibleInapps.addToMeta();
        for (inApp in eligibleInApps) {
            val campaignId = inApp.campaignId
            if (campaignId.isNotEmpty()) {
                evaluatedServerSideInAppIds.add(campaignId)
            }
        }
    }

    private fun evaluateClientSide(event: EventAdapter): JSONArray {
        val eligibleInApps = evaluate(
            event, InAppClientSide.getListFromJsonArray(inAppStore.readClientSideInApps())
        )
        sortByPriority(eligibleInApps).forEach {
            if (!it.shouldSuppress) {
                //updateTTL(it)
                return JSONArray(it)
            } else {
                suppress(it)
            }
        }
        return JSONArray()
    }

    private fun evaluate(event: EventAdapter, inappNotifs: List<InAppBase>): List<InAppBase> {
        // TODO: whenTriggers - DONE
        // TODO: record trigger - DONE
        // TODO: whenLimits - DONE
        val eligibleInApps: MutableList<InAppBase> = mutableListOf()
        for (inApp in inappNotifs) {
            val campaignId = inApp.campaignId
            val matchesTrigger = triggersMatcher.matchEvent(
                inApp.whenTriggers, event.eventName, event.eventProperties
            )
            if (matchesTrigger) {
                triggersManager.increment(campaignId)

                val matchesLimits = limitsMatcher.matchWhenLimits(inApp.whenLimits, campaignId)
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
    fun sortByPriority(inApps: List<InAppBase>): List<InAppBase> {
        val priority: (InAppBase) -> Int = { inApp ->
            inApp.priority as? Int ?: 1
        }

        val ti: (InAppBase) -> String = { inApp ->
            inApp.campaignId as? String ?: (Clock.SYSTEM.newDate().time / 1000).toString()
        }
        // Sort by priority descending and then by timestamp ascending
        return inApps.sortedWith(compareByDescending<InAppBase> { priority(it) }.thenBy { ti(it) })
    }

    private fun suppress(inApp: InAppBase) {
        val wzrkId = generateWzrkId(inApp.campaignId)
        suppressedClientSideInApps.add(
            mapOf(
                Constants.NOTIFICATION_ID_TAG to wzrkId,
                Constants.INAPP_WZRK_PIVOT to (inApp.wzrk_pivot ?: "wzrk_default"),
                Constants.INAPP_WZRK_CGID to inApp.wzrk_cgId
            )
        )
    }

    private fun generateWzrkId(ti: String): String {
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val date = dateFormatter.format(Clock.SYSTEM.newDate())
        return "${ti}_$date"
    }

    /*private fun updateTTL(inApp: JSONObject) {
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
    }*/

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
