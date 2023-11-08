package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.data.InAppBase
import com.clevertap.android.sdk.inapp.data.InAppClientSide
import com.clevertap.android.sdk.inapp.data.InAppServerSide
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.isNotNullAndEmpty
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.EndpointId.ENDPOINT_A1
import com.clevertap.android.sdk.network.NetworkHeadersListener
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class EvaluationManager constructor(
    private val triggersMatcher: TriggersMatcher,
    private val triggersManager: TriggerManager,
    private val limitsMatcher: LimitsMatcher,
    private val storeRegistry: StoreRegistry,
) : NetworkHeadersListener {

    private val evaluatedServerSideInAppIds: MutableList<String> = ArrayList()
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
    fun evaluateOnAppLaunchedClientSide(): JSONArray {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
        return evaluateClientSide(event)
    }

    fun evaluateOnAppLaunchedServerSide(appLaunchedNotifs: List<InAppServerSide>): JSONArray {
        // BE returns applaunch_notifs [0, 1, 2]
        // record trigger counts
        // evaluate limits [2]
        // show first based on priority (2)
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())

        val eligibleInApps = evaluate(
            event,
            appLaunchedNotifs
        )
        sortByPriority(eligibleInApps).forEach {
            if (!it.shouldSuppress) {
                return JSONArray((it as InAppServerSide).toJsonObject())
            } else {
                suppress(it)
            }
        }
        return JSONArray()
    }

    fun matchWhenLimitsBeforeDisplay(listOfLimitAdapter: List<LimitAdapter>, campaignId: String): Boolean {
        return limitsMatcher.matchWhenLimits(listOfLimitAdapter, campaignId)
    }

    private fun evaluateServerSide(event: EventAdapter) {
        val eligibleInApps = evaluate(event, getInApps<InAppServerSide>())

        for (inApp in eligibleInApps) {
            evaluatedServerSideInAppIds.add(inApp.campaignId)
        }
    }

    private fun evaluateClientSide(event: EventAdapter): JSONArray {
        val eligibleInApps = evaluate(event, getInApps<InAppClientSide>())

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
                Constants.INAPP_WZRK_PIVOT to inApp.wzrk_pivot,
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

    private inline fun <reified T> getInApps(): List<InAppBase> {
        val inAppStore = storeRegistry.inAppStore ?: return emptyList()

        return when (T::class) {
            InAppClientSide::class -> InAppClientSide.getListFromJsonArray(inAppStore.readClientSideInApps())
            InAppServerSide::class -> InAppServerSide.getListFromJsonArray(inAppStore.readServerSideInApps())
            else -> emptyList()
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

    override fun onAttachHeaders(endpointId: EndpointId): JSONObject? {
        val header = JSONObject()
        if (endpointId == ENDPOINT_A1) {
            if (evaluatedServerSideInAppIds.isNotEmpty()) {
                header.put(Constants.INAPP_SS_EVAL_META, JsonUtil.listToJsonArray(evaluatedServerSideInAppIds))
            }
            if (suppressedClientSideInApps.isNotEmpty()) {
                header.put(Constants.INAPP_SUPPRESSED_META, JsonUtil.listToJsonArray(suppressedClientSideInApps))
            }
        }
        if (header.isNotNullAndEmpty())
            return header

        return null
    }

    override fun onSentHeaders(allHeaders: JSONObject, endpointId: EndpointId) {
        if (endpointId == ENDPOINT_A1) {
            removeSentEvaluatedServerSideInAppIds(allHeaders)
            removeSentSuppressedClientSideInApps(allHeaders)
        }
    }
}
