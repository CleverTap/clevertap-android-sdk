package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.EndpointId.ENDPOINT_A1
import com.clevertap.android.sdk.network.NetworkHeadersListener
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONObject

/**
 * Local evaluation of Native Display (ND) advanced-rule campaigns.
 *
 * ND is SS-only. On each event the SDK evaluates the cached advanced-rule metadata bundle
 * (`adUnit_notifs_ss`) exactly like in-app SS: match `whenTriggers` → increment the ND trigger
 * count → check `whenLimits` (frequency + occurrence) over ND's own impression/trigger stores. Every
 * eligible campaign's `ti` is added to the `adUnit_eval` vote list, which is reported back in the
 * `/a1` request header; the server delivers content only for voted campaigns.
 *
 * The class is the ND [NetworkHeadersListener]: it owns the in-memory `adUnit_eval` /
 * `adUnit_suppressed` lists (persisted via [StoreRegistry.ndStore]) and the attach → send →
 * remove-exactly-sent lifecycle. `adUnit_suppressed` carries App-Launched CG-suppression acks
 * (see [recordCgSuppressed]).
 *
 * Reuses [TriggersMatcher] and [LimitsMatcher]; only the stores differ from in-app.
 */
internal class NdEvaluationManager(
    private val config: CleverTapInstanceConfig,
    private val triggersMatcher: TriggersMatcher,
    private val ndTriggersManager: TriggerManager,
    private val ndLimitsMatcher: LimitsMatcher,
    private val storeRegistry: StoreRegistry
) : NetworkHeadersListener {

    @VisibleForTesting
    internal var evaluatedNdCampaignIds: MutableList<Long> = ArrayList()

    // App-Launched CG-suppression acks (bare {wzrk_id, wzrk_pivot, wzrk_cgId}), via recordCgSuppressed.
    @VisibleForTesting
    internal var suppressedNdCampaigns: MutableList<Map<String, Any?>> = ArrayList()

    fun evaluateOnEvent(
        eventName: String,
        eventProperties: Map<String, Any>,
        userLocation: Location?
    ) {
        evaluate(listOf(EventAdapter(eventName, eventProperties, userLocation = userLocation)))
    }

    fun evaluateOnChargedEvent(
        details: Map<String, Any>,
        items: List<Map<String, Any>>,
        userLocation: Location?
    ) {
        evaluate(listOf(EventAdapter(Constants.CHARGED_EVENT, details, items, userLocation = userLocation)))
    }

    fun evaluateOnUserAttributeChange(
        eventProperties: Map<String, Map<String, Any?>>,
        userLocation: Location?,
        appFields: Map<String, Any>
    ) {
        val events = eventProperties.map { entry ->
            val merged = entry.value.toMutableMap().apply { putAll(appFields) }
            EventAdapter(
                eventName = entry.key + Constants.USER_ATTRIBUTE_CHANGE,
                eventProperties = merged,
                userLocation = userLocation,
                profileAttrName = entry.key
            )
        }
        evaluate(events)
    }

    /**
     * Core loop: for each event, vote every advanced-rule ND campaign whose triggers match and whose
     * whenLimits still pass. Mirrors [EvaluationManager.evaluate] minus display/priority/suppression
     * (the server owns dispatch for ND; the SDK only votes eligibility).
     */
    @WorkerThread
    @VisibleForTesting
    internal fun evaluate(events: List<EventAdapter>) {
        val ndStore = storeRegistry.ndStore ?: return
        val metadata = ndStore.readServerSideNdMetaData()
        if (metadata.isEmpty()) {
            return
        }

        var updated = false
        for (event in events) {
            for (inApp in metadata) {
                if (!triggersMatcher.matchEvent(whenTriggers = EvalRules.whenTriggers(inApp), event = event)) {
                    continue
                }
                val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)
                if (campaignId.isEmpty()) {
                    continue
                }

                ndTriggersManager.increment(campaignId)

                if (ndLimitsMatcher.matchWhenLimits(whenLimits = EvalRules.whenLimits(inApp), campaignId = campaignId)) {
                    val ti = campaignId.toLongOrNull() ?: continue
                    // Append without a contains() guard: a re-vote while a prior send is in flight must
                    // not be dropped (onSentHeaders removes only what was sent). Server dedups.
                    evaluatedNdCampaignIds.add(ti)
                    updated = true
                    config.logger.verbose(config.accountId,"ND campaign $ti eligible -> adUnit_eval")
                }
            }
        }
        if (updated) {
            saveEvaluatedNdIds()
        }
    }

    /**
     * Filters App-Launched content-in-advance by its advanced `whenTriggers` **and** `whenLimits`, returning
     * the units still eligible (order preserved). App-Launched content is never voted, so this is where both
     * its triggers and limits are applied — mirroring in-app's SS App-Launched flow
     * ([EvaluationManager.executeServerSideAppLaunchEvaluationFlow] → `evaluate`): build the `App Launched`
     * event, match `whenTriggers` (so `firstTimeOnly` / property / geo conditions are honored), then — on a
     * match — increment the occurrence counter and check `whenLimits`.
     *
     * Rules are read inline from each payload (App-Launched campaigns are excluded from `adUnit_notifs_ss`).
     * A unit with no inline `whenTriggers` trivially qualifies (the launch itself is the trigger) — ND-only,
     * since in-app campaigns always carry triggers. This returns **all** cap-survivors; the priority
     * arbitration (deliver all ties at the top band) is applied separately by [selectTopPriorityBand].
     *
     * @param eventProperties the `App Launched` event properties (`deviceInfo.appLaunchedFields`), as in-app.
     * @param userLocation the user location for geo-triggers, as in-app.
     */
    @WorkerThread
    fun retainAppLaunchedWithinLimits(
        content: List<JSONObject>,
        eventProperties: Map<String, Any>,
        userLocation: Location?,
    ): List<JSONObject> {
        if (content.isEmpty()) {
            return content
        }

        val event = EventAdapter(
            eventName = Constants.APP_LAUNCHED_EVENT,
            eventProperties = eventProperties,
            userLocation = userLocation,
        )

        return content.filter { unit ->
            val ti = unit.optString(Constants.INAPP_ID_IN_PAYLOAD)
            if (ti.isEmpty()) {
                return@filter true // no id -> can't cap -> pass
            }

            // Match whenTriggers against the App Launched event. Empty triggers trivially qualify; a unit WITH
            // triggers must match (honoring firstTimeOnly / property / geo), exactly like in-app's evaluate().
            val whenTriggers = EvalRules.whenTriggers(unit)
            if (whenTriggers.isNotEmpty() && !triggersMatcher.matchEvent(whenTriggers = whenTriggers, event = event)) {
                config.logger.verbose(config.accountId, "App-Launched ND $ti dropped: whenTriggers did not match")
                return@filter false
            }

            // Qualified -> advance the occurrence counter (needed for onEvery/onExactly), as in-app does on a
            // trigger match, then check whenLimits.
            ndTriggersManager.increment(ti)
            val whenLimits = EvalRules.whenLimits(unit) // inline rules, mirroring in-app's getWhenLimits(inApp)
            if (whenLimits.isEmpty()) {
                true // no whenLimits to enforce
            } else {
                ndLimitsMatcher.matchWhenLimits(whenLimits = whenLimits, campaignId = ti).also { within ->
                    if (!within) {
                        config.logger.verbose(config.accountId, "App-Launched ND $ti suppressed by whenLimits")
                    }
                }
            }
        }
    }

    /**
     * Priority arbitration for the App-Launched batch — "all ties at the top priority band" (mirrors the
     * server's `rollout-native-display-priority` ON behaviour, as used on custom events).
     *
     * Input is the already cap-filtered (whenTriggers + whenLimits) deliverable [content] and CG [stubs].
     * Sort all candidates priority DESC then `ti` ASC (ND ids are epoch timestamps → ties break oldest-first,
     * same as [EvaluationManager.sortByPriority]) and walk bands top-down:
     * - every CG stub in a band we reach is a would-have-shown control-group exposure → added to [cgAcks];
     * - the first band that contains deliverable content wins: **all** its content is delivered (ties), and
     *   the walk stops (the priority lock latches on an actual delivery, so lower bands are dropped);
     * - a band with only stubs does not latch the lock → fall through to the next band (its stubs still ack).
     *
     * So a batch of 4 units at priorities 10/3/2/1 delivers only priority 10; two units tied at 10 deliver both.
     */
    fun selectTopPriorityBand(content: List<JSONObject>, stubs: List<JSONObject>): AppLaunchedSelection {
        if (content.isEmpty() && stubs.isEmpty()) {
            return AppLaunchedSelection(winners = emptyList(), cgAcks = emptyList())
        }

        // Tag each candidate (true = CG stub) and sort by priority DESC, ti ASC.
        val sorted = (content.map { it to false } + stubs.map { it to true })
            .sortedWith(
                compareByDescending<Pair<JSONObject, Boolean>> { it.first.optInt(Constants.INAPP_PRIORITY, 1) }
                    .thenBy { it.first.optString(Constants.INAPP_ID_IN_PAYLOAD) },
            )

        val winners = ArrayList<JSONObject>()
        val cgAcks = ArrayList<JSONObject>()
        var i = 0
        while (i < sorted.size) {
            val bandPriority = sorted[i].first.optInt(Constants.INAPP_PRIORITY, 1)
            val bandContent = ArrayList<JSONObject>()
            while (i < sorted.size && sorted[i].first.optInt(Constants.INAPP_PRIORITY, 1) == bandPriority) {
                val (unit, isStub) = sorted[i]
                if (isStub) {
                    cgAcks.add(unit)
                } else {
                    bandContent.add(unit)
                }
                i++
            }
            if (bandContent.isNotEmpty()) { // actual delivery -> lock latches; drop lower bands
                winners.addAll(bandContent)
                break
            }
            // all-stub band -> acked above, no delivery, fall through to the next band
        }
        return AppLaunchedSelection(winners = winners, cgAcks = cgAcks)
    }

    /** Result of [selectTopPriorityBand]: content to deliver + CG stubs to ack. */
    data class AppLaunchedSelection(val winners: List<JSONObject>, val cgAcks: List<JSONObject>)

    /**
     * Records a CG-suppressed App-Launched stub as an `adUnit_suppressed` ack, so the CG event fires at
     * the would-have-been-surfaced moment. Bare shape mirrors in-app's `inapps_suppressed`.
     */
    fun recordCgSuppressed(stub: JSONObject) {
        val wzrkId = stub.optString(Constants.NOTIFICATION_ID_TAG)
        if (wzrkId.isEmpty()) {
            // CG stubs always ship wzrk_id; log rather than drop silently if one doesn't.
            config.logger.verbose(config.accountId,"Dropping ND CG ack: stub missing wzrk_id (ti=${stub.optString(Constants.INAPP_ID_IN_PAYLOAD)})")
            return
        }
        suppressedNdCampaigns.add(
            mapOf(
                Constants.NOTIFICATION_ID_TAG to wzrkId,
                Constants.INAPP_WZRK_PIVOT to stub.optString(Constants.INAPP_WZRK_PIVOT, "wzrk_default"),
                Constants.INAPP_WZRK_CGID to stub.optInt(Constants.INAPP_WZRK_CGID)
            )
        )
        saveSuppressedNdIds()
        config.logger.verbose(config.accountId,"Recorded ND CG-suppression ack for $wzrkId")
    }

    override fun onAttachHeaders(endpointId: EndpointId): JSONObject? {
        if (endpointId != ENDPOINT_A1) {
            return null
        }
        return HeaderVoteLists.attach(
            evalMetaKey = Constants.ND_SS_EVAL_META,
            suppressedMetaKey = Constants.ND_SUPPRESSED_META,
            evalIds = evaluatedNdCampaignIds,
            suppressed = suppressedNdCampaigns,
        )
    }

    override fun onSentHeaders(allHeaders: JSONObject, endpointId: EndpointId) {
        if (endpointId != ENDPOINT_A1) {
            return
        }
        if (HeaderVoteLists.removeSentEvalIds(sentHeader = allHeaders, evalMetaKey = Constants.ND_SS_EVAL_META, evalIds = evaluatedNdCampaignIds)) {
            saveEvaluatedNdIds()
        }
        if (HeaderVoteLists.removeSentSuppressed(sentHeader = allHeaders, suppressedMetaKey = Constants.ND_SUPPRESSED_META, suppressed = suppressedNdCampaigns)) {
            saveSuppressedNdIds()
        }
    }

    @WorkerThread
    fun loadEvaluatedAndSuppressedNdIds() {
        storeRegistry.ndStore?.let { store ->
            evaluatedNdCampaignIds = HeaderVoteLists.readEvalIds(store.readEvaluatedServerSideNdIds())
            suppressedNdCampaigns = JsonUtil.listFromJsonSafe(store.readSuppressedNdIds())
        }
    }

    @VisibleForTesting
    internal fun saveEvaluatedNdIds() {
        storeRegistry.ndStore?.storeEvaluatedServerSideNdIds(JsonUtil.listToJsonArray(evaluatedNdCampaignIds))
    }

    @VisibleForTesting
    internal fun saveSuppressedNdIds() {
        storeRegistry.ndStore?.storeSuppressedNdIds(JsonUtil.listToJsonArray(suppressedNdCampaigns))
    }
}
