package com.clevertap.android.sdk.response

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.NdFCManager
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager
import com.clevertap.android.sdk.inapp.store.preference.NdStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single owner of the Display Units / Native Display (ND) channel.
 *
 * Handles the whole channel in one pass, in order, so there is no cross-processor ordering dependency
 * (the meta must land before the content gate reads ceilings):
 * 1. ND fcap **meta** — `ndmc`/`ndmp` ceilings and `adUnit_stale` GC run on **every** response (per-account,
 *    kept current even across a user switch); the per-user `adUnit_notifs_ss` rule bundle is skipped on a
 *    user switch.
 * 2. ND **content** — `adUnit_notifs` + non-stub `adUnit_notifs_applaunched`, merged, frequency-cap gated,
 *    written to the cache once, and delivered via the callback. The App-Launched CG-suppression acks are
 *    raised here too (at the would-have-shown moment — a stub still within its whenLimits), so they share
 *    the eligibility check with real content. **Skipped on a user switch** (matches the legacy behavior of
 *    not surfacing display units — or attributing the outgoing user's CG arm — to the just-switched-in user).
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
    // For the App-Launched trigger match: the `App Launched` event properties + location, mirroring in-app.
    // Nullable because the content-only preview path doesn't evaluate (ndEvaluationManager is null there).
    private val deviceInfo: DeviceInfo?,
    private val coreMetaData: CoreMetaData?,
) : CleverTapResponseDecorator() {

    /** Content-only constructor for the send-test / push-preview path (no ND fcap meta). */
    constructor(
        config: CleverTapInstanceConfig,
        callbackManager: BaseCallbackManager,
        controllerManager: ControllerManager,
    ) : this(config, callbackManager, controllerManager, null, null, null, null, null)

    private val logger = config.logger

    // ND meta (account ceilings + dead-target GC) must stay current even across a user switch, so this
    // processor runs during a switch; the per-user ss-bundle and the content/CG path are skipped internally.
    override fun runsDuringUserSwitch(): Boolean = true

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        processResponse(jsonBody, stringBody, context, ResponseContext.DEFAULT)
    }

    @WorkerThread
    override fun processResponse(
        response: JSONObject?,
        stringBody: String?,
        context: Context,
        responseContext: ResponseContext,
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

        val isUserSwitching = responseContext.isUserSwitching
        val source = responseContext.source

        // 1. ND fcap meta — from /a1 only. A /content re-feed is a partial content response, so it must not
        //    overwrite the ceilings / ss-bundle / stale GC (the ND analog of the in-app store guard for
        //    CONTENT_FETCH). On /a1: account-level ceilings + dead-target GC run on every response incl. a
        //    user switch; the per-user ss-bundle is skipped on a switch so the outgoing user's rules don't
        //    land in the new user's prefs.
        if (source != CTResponseSource.CONTENT_FETCH) {
            ingestNdMeta(response, context, isUserSwitching)
        }

        // 2. ND content — skipped on a user switch. On a /content re-feed the (cap-filtered) units are MERGED
        //    into the cache rather than replacing it, so /content never wipes the /a1 set.
        if (!isUserSwitching) {
            deliverContent(response, source)
        }
    }

    // ---- ND fcap meta ----------------------------------------------------------------------------

    private fun ingestNdMeta(response: JSONObject, context: Context, isUserSwitching: Boolean) {
        // Content-only (send-test/preview) path has no ND stores wired — skip meta entirely.
        val stores = storeRegistry ?: return
        try {
            val ndFCManager = controllerManager.ndFCManager

            // Account-level ceilings. `ndmc` presence marks a cap-aware response; absent `ndmp` => uncapped
            // daily (the Int.MAX_VALUE default expresses that).
            if (response.has(Constants.ND_MAX_PER_SESSION_KEY) && ndFCManager != null) {
                val perSession = response.optInt(Constants.ND_MAX_PER_SESSION_KEY, 1)
                val perDay = response.optInt(Constants.ND_MAX_PER_DAY_KEY, Int.MAX_VALUE)
                ndFCManager.updateLimits(perDay, perSession)
            }

            // Dead-target GC.
            response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_STALE_KEY)?.let { staleIds ->
                clearStaleNdCache(stores, staleIds)
                ndFCManager?.processResponse(staleIds)
            }

            // The ss-bundle and CG acks are per-user; skip them on a user switch so the outgoing user's
            // rules/acks aren't written into the just-switched-in user's stores (mirrors InAppResponse).
            if (isUserSwitching) return

            // Advanced-rule metadata bundle for local evaluation. Full replace, including an empty array
            // (the bundle is always the complete current set, so [] means "clear").
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
            // Note: App-Launched CG-suppression acks are raised in the content pass (deliverContent →
            // appLaunchedWithinWhenLimits), gated on the same whenLimits as real content so the CG arm is
            // counted only when the campaign would actually have shown.
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}Failed to process ND meta", t)
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

    private fun deliverContent(response: JSONObject, source: CTResponseSource) {
        val notifs = response.optJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY)
        val appLaunched = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY)
        val hasNotifs = notifs != null && notifs.length() > 0
        val hasAppLaunched = appLaunched != null && appLaunched.length() > 0
        if (!hasNotifs && !hasAppLaunched) {
            return
        }
        try {
            parseDisplayUnits(notifs, appLaunched, source)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}Failed to parse content", t)
        }
    }

    /**
     * Parses Display Units from both `adUnit_notifs` and (non-CG-suppressed) `adUnit_notifs_applaunched`,
     * merges them, applies the ND frequency-cap gate, and writes the cache once —
     * `CTDisplayUnitController.updateDisplayUnits` replaces (not merges) the cache, so two writes for
     * one response would wipe each other. CG stubs (`suppressed:true`) are not delivered; they are split
     * out and acked within [appLaunchedWithinWhenLimits] once they pass the same whenLimits as real content.
     */
    private fun parseDisplayUnits(notifs: JSONArray?, appLaunched: JSONArray?, source: CTResponseSource) {
        val parsed = ArrayList<CleverTapDisplayUnit>()
        notifs?.let { parsed.addAll(parseDisplayUnitsFromJson(it)) }
        // App-Launched content is filtered by whenLimits here (it carries no adUnit_eval vote). This also acks
        // the CG-suppression stubs as a side effect, so acks happen even when the response is stub-only.
        appLaunched?.let { parsed.addAll(parseDisplayUnitsFromJson(JSONArray(appLaunchedWithinWhenLimits(it)))) }

        // Only touch the cache when the response actually carried deliverable (non-stub) content. A response
        // whose only ND payload is CG-suppression stubs is acked above but must NOT reset the cache — otherwise
        // it would silently wipe units delivered earlier in the session (e.g. an App-Launched re-foreground for
        // a control-group user), with no callback. A response that DID carry real content still resets even if
        // everything was cap-filtered, so getAllDisplayUnits() can't keep serving a now-capped unit.
        val hadDeliverableContent = (notifs != null && notifs.length() > 0) || hasNonSuppressed(appLaunched)
        if (!hadDeliverableContent) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}CG-stub-only response; cache left intact")
            return
        }

        val cache = controllerManager.orCreateDisplayUnitCache
        if (cache == null) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}No display-unit cache available")
            return
        }

        // Frequency-cap gate FIRST — caps are applied to the new content before any content-fetch merge.
        // Never cap-gate the send-test / push-preview path (content-only constructor, no ND stores).
        val gated = ArrayList(
            if (storeRegistry == null) parsed
            else NdFcapGate.filter(parsed, controllerManager.ndFCManager, logger, config.accountId),
        )

        // A /content re-feed carries only the personalized subset of display units. The cache is a REPLACE,
        // so applying the subset as-is would wipe the units delivered by /a1. Merge the (already cap-filtered)
        // subset by unitID into the current set instead; /a1 stays authoritative and keeps REPLACE. Skip
        // entirely if nothing mergeable arrived, so we don't re-publish the unchanged /a1 set.
        if (source == CTResponseSource.CONTENT_FETCH) {
            if (!hasMergeableUnit(gated)) {
                return
            }
            val merged = mergeByUnitId(cache.allDisplayUnits, gated)
            cache.updateDisplayUnits(merged)
            callbackManager.notifyDisplayUnitsLoaded(merged)
            return
        }

        // /a1: authoritative REPLACE. Writing an empty list resets a cache whose content was fully cap-filtered.
        cache.updateDisplayUnits(gated)
        if (gated.isNotEmpty()) {
            callbackManager.notifyDisplayUnitsLoaded(gated)
        } else {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}No Display Units survived; cache cleared")
        }
    }

    /** Whether [appLaunched] carries at least one deliverable (non-`suppressed`) entry. */
    private fun hasNonSuppressed(appLaunched: JSONArray?): Boolean {
        if (appLaunched == null) return false
        for (i in 0 until appLaunched.length()) {
            val entry = appLaunched.optJSONObject(i) ?: continue
            if (!entry.optBoolean(Constants.INAPP_SUPPRESSED, false)) return true
        }
        return false
    }

    /**
     * Merges [incoming] units into [existing] keyed by unitID: an existing unitID is replaced in place
     * (position preserved), a new one is appended; empty unitIDs are skipped (matches the cache). In practice
     * /a1 and /content never return the same unitID, so this is a union/append — the keying just dedupes
     * within a payload and stays correct should that contract ever change.
     */
    private fun mergeByUnitId(
        existing: List<CleverTapDisplayUnit>?,
        incoming: List<CleverTapDisplayUnit>,
    ): ArrayList<CleverTapDisplayUnit> {
        val merged = LinkedHashMap<String, CleverTapDisplayUnit>()
        existing?.forEach { unit -> unit.unitID?.takeIf { it.isNotEmpty() }?.let { merged[it] = unit } }
        incoming.forEach { unit -> unit.unitID?.takeIf { it.isNotEmpty() }?.let { merged[it] = unit } }
        return ArrayList(merged.values)
    }

    /** True if at least one unit carries a usable unitID, i.e. would actually merge into the cache. */
    private fun hasMergeableUnit(units: List<CleverTapDisplayUnit>): Boolean =
        units.any { !it.unitID.isNullOrEmpty() }

    /**
     * Processes the App-Launched batch: splits CG-suppression stubs from deliverable content, applies the
     * advanced `whenLimits` to both, acks the eligible stubs, and returns the content still within cap.
     *
     * CG acks fire at the **would-have-shown** moment — a stub is acked only if it is still within its
     * `whenLimits` (mirrors in-app, which records CG suppression for *eligible* in-apps, not on receipt).
     * There is deliberately **no** global-budget gate on the acks: a CG stub renders nothing, so it neither
     * consumes nor competes for the daily/session budget.
     *
     * Deliverable content additionally goes through [trimToGlobalCap]. Guarded and **fail-closed**: if a
     * malformed rule makes a filter throw, drop that set (it can't be cap-checked, so it must not bypass
     * caps) — but the throws are contained here so regular `adUnit_notifs` content still delivers. The
     * evaluator is absent only on the send-test/preview path, which has no ND caps/acks to apply.
     */
    private fun appLaunchedWithinWhenLimits(appLaunched: JSONArray): List<JSONObject> {
        val content = ArrayList<JSONObject>()
        val cgStubs = ArrayList<JSONObject>()
        for (i in 0 until appLaunched.length()) {
            val entry = appLaunched.optJSONObject(i) ?: continue
            if (entry.optBoolean(Constants.INAPP_SUPPRESSED, false)) cgStubs.add(entry) else content.add(entry)
        }
        val evaluator = ndEvaluationManager ?: return content // preview path: no caps/acks to apply

        // The `App Launched` event the batch is evaluated against — properties + location sourced exactly as
        // in-app does (InAppController.onAppLaunchServerSideInAppsResponse): appLaunchedFields + user location.
        // Guarded: a malformed appLaunchedFields must not abort content delivery.
        val eventProperties: Map<String, Any> = try {
            deviceInfo?.appLaunchedFields?.let { JsonUtil.mapFromJson<Any>(it) } ?: emptyMap()
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}Failed to read App-Launched event props", t)
            emptyMap()
        }
        val userLocation = try { coreMetaData?.locationFromUser } catch (t: Throwable) { null }

        // Ack CG stubs that are still eligible — within their whenTriggers + whenLimits (would have shown).
        // Fully guarded (incl. recordCgSuppressed, which writes SharedPreferences): a bad CG stub must never
        // drop the regular `adUnit_notifs` content parsed alongside it.
        if (cgStubs.isNotEmpty()) {
            try {
                evaluator.retainAppLaunchedWithinLimits(cgStubs, eventProperties, userLocation)
                    .forEach { evaluator.recordCgSuppressed(it) }
            } catch (t: Throwable) {
                logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}ND CG ack failed; skipping acks", t)
            }
        }

        // Deliverable content: whenTriggers + whenLimits + remaining global-budget trim.
        val eligible = try {
            evaluator.retainAppLaunchedWithinLimits(content, eventProperties, userLocation)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "${Constants.FEATURE_DISPLAY_UNIT}ND App-Launched eligibility filter failed; dropping units", t)
            emptyList() // fail-closed: don't let un-cap-checked units through
        }
        return trimToGlobalCap(eligible)
    }

    /**
     * Trims the App-Launched batch to the remaining account-level global budget (`ndmp` daily + `ndmc`
     * session). App-Launched is server-ships-all / SDK-decides, so — unlike regular events, where the
     * server trims using the reported `ndmp` — the SDK enforces the global cap here. Units are consumed in
     * server order (already priority-sorted). Budget-exempt (always kept) = **non-regime** units (which never
     * count) and [NdFCManager.isExcludeFromCaps] units (`efc`/`excludeGlobalFCaps`); everything else consumes
     * a slot. (`efc` is kept as exclude-from-caps but still counts when viewed — see [NdFCManager.didShow].)
     */
    private fun trimToGlobalCap(units: List<JSONObject>): List<JSONObject> {
        val ndFCManager = controllerManager.ndFCManager ?: return units
        var remaining = ndFCManager.globalCapRemaining()
        val kept = ArrayList<JSONObject>(units.size)
        for (unit in units) {
            val exempt = !NdFCManager.inRegime(unit) || NdFCManager.isExcludeFromCaps(unit)
            when {
                exempt -> kept.add(unit)
                remaining > 0 -> { kept.add(unit); remaining-- }
                else -> logger.verbose(
                    config.accountId,
                    "${Constants.FEATURE_DISPLAY_UNIT}App-Launched ND unit dropped: global cap reached",
                )
            }
        }
        return kept
    }

    private fun parseDisplayUnitsFromJson(messages: JSONArray): List<CleverTapDisplayUnit> {
        val list = ArrayList<CleverTapDisplayUnit>()
        for (i in 0 until messages.length()) {
            try {
                val json = messages.getJSONObject(i)
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
