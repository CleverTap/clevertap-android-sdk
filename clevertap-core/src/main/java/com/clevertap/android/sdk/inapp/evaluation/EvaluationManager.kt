package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.isNotNullAndEmpty
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.EndpointId.ENDPOINT_A1
import com.clevertap.android.sdk.network.NetworkHeadersListener
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.toList
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

@RestrictTo(LIBRARY)
class EvaluationManager constructor(
    private val triggersMatcher: TriggersMatcher,
    private val triggersManager: TriggerManager,
    private val limitsMatcher: LimitsMatcher,
    private val storeRegistry: StoreRegistry,
) : NetworkHeadersListener {

    @VisibleForTesting
    internal val evaluatedServerSideCampaignIds: MutableList<Long> = ArrayList()

    @VisibleForTesting
    internal val suppressedClientSideInApps: MutableList<Map<String, Any?>> = ArrayList()

    fun evaluateOnEvent(eventName: String, eventProperties: Map<String, Any>, userLocation: Location?): JSONArray {
        val event = EventAdapter(eventName, eventProperties, userLocation = userLocation)
        evaluateServerSide(event)
        return evaluateClientSide(event)
    }

    fun evaluateOnChargedEvent(
        details: Map<String, Any>,
        items: List<Map<String, Any>>,
        userLocation: Location?
    ): JSONArray {
        val event = EventAdapter(Constants.CHARGED_EVENT, details, items, userLocation = userLocation)
        evaluateServerSide(event)
        return evaluateClientSide(event)
    }

    // onBatchSent with App Launched event in batch
    fun evaluateOnAppLaunchedClientSide(eventProperties: Map<String, Any>, userLocation: Location?): JSONArray {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, eventProperties, userLocation = userLocation)
        return evaluateClientSide(event)
    }

    fun evaluateOnAppLaunchedServerSide(
        appLaunchedNotifs: List<JSONObject>,
        eventProperties: Map<String, Any>,
        userLocation: Location?
    ): JSONArray {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, eventProperties, userLocation = userLocation)

        val eligibleInApps = evaluate(event, appLaunchedNotifs)

        sortByPriority(eligibleInApps).forEach { inApp ->
            if (!shouldSuppress(inApp)) {
                return JSONArray().also { it.put(inApp) }
            } else {
                suppress(inApp)
            }
        }
        return JSONArray()
    }

    fun matchWhenLimitsBeforeDisplay(listOfLimitAdapter: List<LimitAdapter>, campaignId: String): Boolean {
        return limitsMatcher.matchWhenLimits(listOfLimitAdapter, campaignId)
    }

    @VisibleForTesting
    internal fun evaluateServerSide(event: EventAdapter) {
        storeRegistry.inAppStore?.let { store ->
            val eligibleInApps = evaluate(event, store.readServerSideInAppsMetaData().toList())

            eligibleInApps.forEach { inApp ->
                val campaignId = inApp.optLong(Constants.INAPP_ID_IN_PAYLOAD)

                if (campaignId != 0L) {
                    evaluatedServerSideCampaignIds.add(campaignId)
                }
            }
        }
    }

    @VisibleForTesting
    internal fun evaluateClientSide(event: EventAdapter): JSONArray {
        storeRegistry.inAppStore?.let { store ->
            val eligibleInApps = evaluate(event, store.readClientSideInApps().toList())

            sortByPriority(eligibleInApps).forEach { inApp ->
                if (!shouldSuppress(inApp)) {
                    updateTTL(inApp)
                    return JSONArray().also { it.put(inApp) }
                } else {
                    suppress(inApp)
                }
            }
        }.run { return JSONArray() }
    }

    @VisibleForTesting
    internal fun evaluate(
        event: EventAdapter,
        inappNotifs: List<JSONObject>,
        clearResource: (url: String) -> Unit = {}
    ): List<JSONObject> {
        val eligibleInApps: MutableList<JSONObject> = mutableListOf()

        for (inApp in inappNotifs) {
            val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)

            val matchesTrigger =
                triggersMatcher.matchEvent(getWhenTriggers(inApp), event)
            if (matchesTrigger) {
                Logger.v("INAPP", "Triggers matched for event ${event.eventName} against inApp $campaignId")
                triggersManager.increment(campaignId)

                val matchesLimits = limitsMatcher.matchWhenLimits(getWhenLimits(inApp), campaignId)
                val discardData = limitsMatcher.shouldDiscard(getWhenLimits(inApp), campaignId)

                if (discardData) {
                    clearResource.invoke("") // todo pass correct url
                }
                if (matchesLimits) {
                    Logger.v("INAPP", "Limits matched for event ${event.eventName} against inApp $campaignId")
                    eligibleInApps.add(inApp)
                } else {
                    Logger.v("INAPP", "Limits did not matched for event ${event.eventName} against inApp $campaignId")
                }
            } else {
                Logger.v("INAPP", "Triggers did not matched for event ${event.eventName} against inApp $campaignId")
            }
        }
        return eligibleInApps
    }

    @VisibleForTesting
    internal fun getWhenTriggers(triggerJson: JSONObject): List<TriggerAdapter> {
        val whenTriggers = triggerJson.optJSONArray(Constants.INAPP_WHEN_TRIGGERS).orEmptyArray()
        return (0 until whenTriggers.length()).mapNotNull {
            val jsonObject = whenTriggers[it] as? JSONObject
            jsonObject?.let { nonNullJsonObject -> TriggerAdapter(nonNullJsonObject) }
        }
    }

    internal fun getWhenLimits(limitJSON: JSONObject): List<LimitAdapter> {
        val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()
        val occurrenceLimits = limitJSON.optJSONArray(Constants.INAPP_OCCURRENCE_LIMITS).orEmptyArray()

        return (frequencyLimits.toList<JSONObject>() + occurrenceLimits.toList()).mapNotNull {
            if (it.isNotNullAndEmpty()) {
                LimitAdapter(it)
            } else null
        }.toMutableList()
    }

    /**
     * Sorts list of InApp objects with priority(100 highest - 1 lowest) and if equal priority
     * then then the one created earliest
     */
    internal fun sortByPriority(inApps: List<JSONObject>): List<JSONObject> {
        val priority: (JSONObject) -> Int = { inApp ->
            inApp.optInt(Constants.INAPP_PRIORITY, 1)
        }

        val ti: (JSONObject) -> String = { inApp ->
            inApp.optString(Constants.INAPP_ID_IN_PAYLOAD, (Clock.SYSTEM.newDate().time / 1000).toString())
        }
        // Sort by priority descending and then by timestamp ascending
        return inApps.sortedWith(compareByDescending<JSONObject> { priority(it) }.thenBy { ti(it) })
    }

    private fun shouldSuppress(inApp: JSONObject): Boolean {
        return inApp.optBoolean(Constants.INAPP_SUPPRESSED)
    }

    @VisibleForTesting
    internal fun suppress(inApp: JSONObject) {
        val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)
        val wzrkId = generateWzrkId(campaignId)
        val wzrkPivot = inApp.optString(Constants.INAPP_WZRK_PIVOT, "wzrk_default")
        val wzrkCgId = inApp.optInt(Constants.INAPP_WZRK_CGID)

        suppressedClientSideInApps.add(
            mapOf(
                Constants.NOTIFICATION_ID_TAG to wzrkId,
                Constants.INAPP_WZRK_PIVOT to wzrkPivot,
                Constants.INAPP_WZRK_CGID to wzrkCgId
            )
        )
    }

    @VisibleForTesting
    internal fun generateWzrkId(ti: String, clock: Clock = Clock.SYSTEM): String {
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val date = dateFormatter.format(clock.newDate())
        return "${ti}_$date"
    }

    @VisibleForTesting
    internal fun updateTTL(inApp: JSONObject, clock: Clock = Clock.SYSTEM) {
        val offset = inApp.opt(Constants.WZRK_TIME_TO_LIVE_OFFSET) as? Long
        if (offset != null) {
            val now = clock.currentTimeSeconds()
            val ttl = now + offset
            inApp.put(Constants.WZRK_TIME_TO_LIVE, ttl)
        } else {
            // return TTL as null since it cannot be calculated due to null offset value
            // The default TTL will be set in the CTInAppNotification
            inApp.remove(Constants.WZRK_TIME_TO_LIVE)
        }
    }

    private fun removeSentEvaluatedServerSideCampaignIds(header: JSONObject) {
        val inAppsEval = header.optJSONArray(Constants.INAPP_SS_EVAL_META)
        inAppsEval?.let {
            for (i in 0 until it.length()) {
                val campaignId = it.optLong(i)

                if (campaignId != 0L) {
                    evaluatedServerSideCampaignIds.remove(campaignId)
                }
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
            if (evaluatedServerSideCampaignIds.isNotEmpty()) {
                header.put(Constants.INAPP_SS_EVAL_META, JsonUtil.listToJsonArray(evaluatedServerSideCampaignIds))
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
            removeSentEvaluatedServerSideCampaignIds(allHeaders)
            removeSentSuppressedClientSideInApps(allHeaders)
        }
    }
}
