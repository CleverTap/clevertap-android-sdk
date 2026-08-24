package com.clevertap.android.sdk.response

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager
import com.clevertap.android.sdk.inapp.store.preference.NdStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single owner of the Display Units / Native Display (ND) channel (SDK-6055).
 *
 * Handles the whole channel in one pass, in order, so there is no cross-processor ordering dependency
 * (the meta must land before the content gate reads ceilings):
 * 1. ND fcap **meta** — `ndmc`/`ndmp` ceilings, `adUnit_stale` GC, `adUnit_notifs_ss` rule bundle, and
 *    CG-suppression acks from `adUnit_notifs_applaunched` stubs. Ingested on **every** response,
 *    including a user switch (ceilings/rules are per-account and should stay current).
 * 2. ND **content** — `adUnit_notifs` + non-stub `adUnit_notifs_applaunched`, merged, frequency-cap
 *    gated, written to the cache once, and delivered via the callback. **Skipped on a user switch**
 *    (matches the legacy behavior of not surfacing display units to the just-switched-in user).
 *
 * The ND dependencies are nullable: the send-test / push-preview path (see the content-only secondary
 * constructor) carries a single display unit and no ND meta, so meta ingestion is skipped there.
 */
internal class DisplayUnitResponse(
    private val config: CleverTapInstanceConfig,
    private val callbackManager: BaseCallbackManager,
    private val controllerManager: ControllerManager,
    private val storeRegistry: StoreRegistry?,
    private val ndTriggerManager: TriggerManager?,
    private val ndEvaluationManager: NdEvaluationManager?,
) : CleverTapResponseDecorator() {

    /** Content-only constructor for the send-test / push-preview path (no ND fcap meta). */
    constructor(
        config: CleverTapInstanceConfig,
        callbackManager: BaseCallbackManager,
        controllerManager: ControllerManager,
    ) : this(config, callbackManager, controllerManager, null, null, null)

    private val logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        processResponse(jsonBody, stringBody, context, isUserSwitching = false)
    }

    @WorkerThread
    fun processResponse(
        response: JSONObject?,
        stringBody: String?,
        context: Context,
        isUserSwitching: Boolean,
    ) {
        if (config.isAnalyticsOnly) {
            logger.verbose(
                config.accountId,
                "CleverTap instance is configured to analytics only, not processing Display Unit response",
            )
            return
        }
        if (response == null) {
            logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Can't parse Display Unit Response, JSON response object is null",
            )
            return
        }

        // 1. ND fcap meta — always, even on a user switch.
        ingestNdMeta(response, context)

        // 2. ND content — skipped on a user switch.
        if (!isUserSwitching) {
            deliverContent(response)
        }
    }

    // ---- ND fcap meta ----------------------------------------------------------------------------

    private fun ingestNdMeta(response: JSONObject, context: Context) {
        // Content-only (send-test/preview) path has no ND stores wired — skip meta entirely.
        val stores = storeRegistry ?: return
        val evalManager = ndEvaluationManager ?: return
        try {
            val ndFCManager = controllerManager.ndFCManager

            // Account-level ceilings. Per contract §5.1 `ndmc` is emitted on every V2 response; `ndmp`
            // only when the account has a daily cap (absent => uncapped daily, hence Int.MAX_VALUE).
            if (response.has(Constants.ND_MAX_PER_SESSION_KEY) && ndFCManager != null) {
                val perSession = response.optInt(Constants.ND_MAX_PER_SESSION_KEY, 1)
                val perDay = if (response.has(Constants.ND_MAX_PER_DAY_KEY)) {
                    response.optInt(Constants.ND_MAX_PER_DAY_KEY, Int.MAX_VALUE)
                } else {
                    Int.MAX_VALUE
                }
                ndFCManager.updateLimits(context, perDay, perSession)
            }

            // Dead-target GC.
            response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_STALE_KEY)?.let { staleIds ->
                clearStaleNdCache(stores, staleIds)
                ndFCManager?.processResponse(context, staleIds)
            }

            // Advanced-rule metadata bundle (rules only) for local evaluation. Full replace, including
            // an empty array: the bundle is emitted only on App-Launched / ND-meta-fetch and is always
            // the complete current set (contract §5.2), so [] legitimately means "clear".
            if (response.has(Constants.DISPLAY_UNIT_NOTIFS_SS_KEY)) {
                val ssArray = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_SS_KEY)
                val ndStore: NdStore? = stores.ndStore
                if (ndStore != null && ssArray != null) {
                    val meta = Utils.toJSONObjectList(ssArray)
                    ndStore.storeServerSideNdMetaData(meta)
                    logger.verbose(
                        config.accountId,
                        "${Constants.FEATURE_DISPLAY_UNIT}Stored ${meta.size} ND SS metadata entries",
                    )
                }
            }

            // Ack CG-suppressed App-Launched stubs (the real content is delivered below). App-Launched
            // path only — regular-event CG is server-decided.
            ackCgSuppressedStubs(response, evalManager)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}Failed to process ND meta", t)
        }
    }

    private fun ackCgSuppressedStubs(response: JSONObject, evalManager: NdEvaluationManager) {
        val appLaunched = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY) ?: return
        for (i in 0 until appLaunched.length()) {
            val entry = appLaunched.optJSONObject(i)
            if (entry != null && entry.optBoolean(Constants.INAPP_SUPPRESSED, false)) {
                evalManager.recordCgSuppressed(entry)
            }
        }
    }

    /**
     * Wipes the CS/SS cap state (impression timestamps + trigger counts) for stale ND target ids.
     * The legacy per-target counters are purged separately by `NdFCManager.processResponse`.
     */
    private fun clearStaleNdCache(stores: StoreRegistry, staleIds: JSONArray) {
        val impressionStore = stores.ndImpressionStore
        for (i in 0 until staleIds.length()) {
            val staleId = staleIds.optString(i)
            if (staleId.isNullOrEmpty()) {
                continue
            }
            impressionStore?.clear(staleId)
            ndTriggerManager?.removeTriggers(staleId)
        }
    }

    // ---- ND content ------------------------------------------------------------------------------

    private fun deliverContent(response: JSONObject) {
        val notifs = response.optJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY)
        val appLaunched = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY)
        val hasNotifs = notifs != null && notifs.length() > 0
        val hasAppLaunched = appLaunched != null && appLaunched.length() > 0
        if (!hasNotifs && !hasAppLaunched) {
            return
        }
        try {
            parseDisplayUnits(notifs, appLaunched)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}Failed to parse content", t)
        }
    }

    /**
     * Parses Display Units from both `adUnit_notifs` and (non-CG-suppressed) `adUnit_notifs_applaunched`,
     * merges them, applies the ND frequency-cap gate, and writes the cache once —
     * `CTDisplayUnitController.updateDisplayUnits` replaces (not merges) the cache, so two writes for
     * one response would wipe each other. CG stubs
     * (`suppressed:true`) are skipped here; they are acked in [ackCgSuppressedStubs].
     */
    private fun parseDisplayUnits(notifs: JSONArray?, appLaunched: JSONArray?) {
        val parsed = ArrayList<CleverTapDisplayUnit>()
        notifs?.let { parsed.addAll(parseDisplayUnitsFromJson(it, skipSuppressed = false)) }
        appLaunched?.let { parsed.addAll(parseDisplayUnitsFromJson(it, skipSuppressed = true)) }
        if (parsed.isEmpty()) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}No valid Display Units to process")
            return
        }

        val cache = controllerManager.orCreateDisplayUnitCache
        if (cache == null) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}No display-unit cache available")
            return
        }

        val displayUnits = ArrayList(
            NdFcapGate.filter(parsed, controllerManager.ndFCManager, logger, config.accountId),
        )
        cache.updateDisplayUnits(displayUnits)
        if (displayUnits.isNotEmpty()) {
            callbackManager.notifyDisplayUnitsLoaded(displayUnits)
        }
    }

    private fun parseDisplayUnitsFromJson(messages: JSONArray, skipSuppressed: Boolean): List<CleverTapDisplayUnit> {
        val list = ArrayList<CleverTapDisplayUnit>()
        for (i in 0 until messages.length()) {
            try {
                val json = messages.getJSONObject(i)
                if (skipSuppressed && json.optBoolean(Constants.INAPP_SUPPRESSED, false)) {
                    continue
                }
                val unit = CleverTapDisplayUnit.toDisplayUnit(json)
                if (unit.error.isNullOrEmpty()) {
                    list.add(unit)
                } else {
                    logger.verbose(
                        config.accountId,
                        "${Constants.FEATURE_DISPLAY_UNIT}Failed to convert JsonArray item at index:$i to Display Unit",
                    )
                }
            } catch (e: Exception) {
                logger.verbose(
                    config.accountId,
                    "${Constants.FEATURE_DISPLAY_UNIT}Failed to parse Display Unit at index $i: ${e.localizedMessage}",
                )
            }
        }
        return list
    }
}
