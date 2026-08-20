package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
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
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONObject

/**
 * Local evaluation of Native Display (ND) advanced-rule campaigns (SDK-6055, Phase 4).
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
    private val triggersMatcher: TriggersMatcher,
    private val ndTriggersManager: TriggerManager,
    private val ndLimitsMatcher: LimitsMatcher,
    private val storeRegistry: StoreRegistry
) : NetworkHeadersListener {

    companion object {
        private val TAG = NdEvaluationManager::class.java.simpleName
    }

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
        if (metadata.isEmpty()) return

        var updated = false
        for (event in events) {
            for (inApp in metadata) {
                if (!triggersMatcher.matchEvent(getWhenTriggers(inApp), event)) {
                    continue
                }
                val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)
                if (campaignId.isEmpty()) continue

                ndTriggersManager.increment(campaignId)

                if (ndLimitsMatcher.matchWhenLimits(getWhenLimits(inApp), campaignId)) {
                    val ti = campaignId.toLongOrNull() ?: continue
                    // Append without a contains() guard: a re-vote while a prior send is in flight must
                    // not be dropped (onSentHeaders removes only what was sent). Server dedups (§6.2).
                    evaluatedNdCampaignIds.add(ti)
                    updated = true
                    Logger.v(TAG, "ND campaign $ti eligible -> adUnit_eval")
                }
            }
        }
        if (updated) {
            saveEvaluatedNdIds()
        }
    }

    @VisibleForTesting
    internal fun getWhenTriggers(triggerJson: JSONObject): List<TriggerAdapter> {
        val whenTriggers = triggerJson.optJSONArray(Constants.INAPP_WHEN_TRIGGERS).orEmptyArray()
        return (0 until whenTriggers.length()).mapNotNull {
            (whenTriggers[it] as? JSONObject)?.let { obj -> TriggerAdapter(obj) }
        }
    }

    @VisibleForTesting
    internal fun getWhenLimits(limitJSON: JSONObject): List<LimitAdapter> {
        val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()
        val occurrenceLimits = limitJSON.optJSONArray(Constants.INAPP_OCCURRENCE_LIMITS).orEmptyArray()
        return (frequencyLimits.toList<JSONObject>() + occurrenceLimits.toList()).mapNotNull {
            if (it.isNotNullAndEmpty()) LimitAdapter(it) else null
        }
    }

    /**
     * Records a CG-suppressed App-Launched ND stub as an `adUnit_suppressed` ack. The server ships
     * stubs (`suppressed:true` + `wzrk_cgId`) inline in `adUnit_notifs_applaunched`; the SDK acks at
     * its would-have-been-surfaced moment (App-Launched path only — regular events need no ack) so the
     * CG event fires at render time rather than server-delivery time. Bare shape mirrors in-app's
     * `inapps_suppressed`.
     */
    fun recordCgSuppressed(stub: JSONObject) {
        val wzrkId = stub.optString(Constants.NOTIFICATION_ID_TAG)
        if (wzrkId.isEmpty()) {
            // Per contract §5.4 the CG stub always ships wzrk_id (unlike in-app payloads which carry
            // only ti). Log if one ever doesn't, rather than dropping the ack silently.
            Logger.v(TAG, "Dropping ND CG ack: stub missing wzrk_id (ti=${stub.optString(Constants.INAPP_ID_IN_PAYLOAD)})")
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
        Logger.v(TAG, "Recorded ND CG-suppression ack for $wzrkId")
    }

    override fun onAttachHeaders(endpointId: EndpointId): JSONObject? {
        if (endpointId != ENDPOINT_A1) return null
        val header = JSONObject()
        if (evaluatedNdCampaignIds.isNotEmpty()) {
            header.put(Constants.ND_SS_EVAL_META, JsonUtil.listToJsonArray(evaluatedNdCampaignIds))
        }
        if (suppressedNdCampaigns.isNotEmpty()) {
            header.put(Constants.ND_SUPPRESSED_META, JsonUtil.listToJsonArray(suppressedNdCampaigns))
        }
        return if (header.isNotNullAndEmpty()) header else null
    }

    override fun onSentHeaders(allHeaders: JSONObject, endpointId: EndpointId) {
        if (endpointId != ENDPOINT_A1) return
        removeSentEvaluatedNdIds(allHeaders)
        removeSentSuppressedNd(allHeaders)
    }

    private fun removeSentEvaluatedNdIds(header: JSONObject) {
        var updated = false
        header.optJSONArray(Constants.ND_SS_EVAL_META)?.let {
            for (i in 0 until it.length()) {
                val ti = it.optLong(i)
                if (ti != 0L) {
                    updated = evaluatedNdCampaignIds.remove(ti) || updated
                }
            }
        }
        if (updated) saveEvaluatedNdIds()
    }

    private fun removeSentSuppressedNd(header: JSONObject) {
        var updated = false
        val sent = header.optJSONArray(Constants.ND_SUPPRESSED_META) ?: return
        val sentString = sent.toString()
        val iterator = suppressedNdCampaigns.iterator()
        while (iterator.hasNext()) {
            val id = iterator.next()[Constants.NOTIFICATION_ID_TAG] as? String
            if (id != null && sentString.contains(id)) {
                iterator.remove()
                updated = true
            }
        }
        if (updated) saveSuppressedNdIds()
    }

    @WorkerThread
    fun loadEvaluatedAndSuppressedNdIds() {
        storeRegistry.ndStore?.let { store ->
            // Read via optLong, NOT toList<Long>(): org.json parses int-range numbers as Integer and
            // `element is Long` filters them all out, so `ti`s (epoch-second ids) would be dropped.
            val stored = store.readEvaluatedServerSideNdIds()
            evaluatedNdCampaignIds =
                (0 until stored.length()).map { stored.optLong(it) }.filter { it != 0L }.toMutableList()
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
