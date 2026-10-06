package com.clevertap.android.sdk.response

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.NdFCManager
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit

/**
 * Native Display frequency-cap gate. Filters display units through [NdFCManager.canShow]
 * before they are handed to the host — the ND analog of in-app's pre-display cap gate.
 *
 * Only units carrying an fcap marker (`efc`/`tlc`/`tdc`/`mdc`/`excludeGlobalFCaps`) are gated; unmarked
 * units (e.g. journeys, which the server leaves undecorated) pass through unchanged.
 *
 * The gate enforces **session caps only** — the account `ndmc` and per-target `mdc`. On regular events the
 * server already enforced the global daily ceiling and the per-target `tdc`/`tlc`, and the App-Launched
 * path trims the global budget separately (`trimToGlobalCap`); re-applying any of those here would
 * double-cap. Advanced `frequencyLimits`/`occurrenceLimits` were already applied during evaluation (the
 * server only ships content for `adUnit_eval`-voted campaigns), so they are not re-checked here either.
 */
internal object NdFcapGate {

    private const val UNCAPPED = -1

    fun filter(
        units: List<CleverTapDisplayUnit>,
        ndFCManager: NdFCManager?,
        logger: ILogger,
        accountId: String?,
    ): List<CleverTapDisplayUnit> {
        if (ndFCManager == null) {
            return units
        }
        return units.filter { unit ->
            val json = unit.jsonObject
            if (json == null || !NdFCManager.isFcapManaged(json)) {
                return@filter true // not fcap-managed -> deliver as before
            }
            val excludeFromCaps = json.optInt(Constants.KEY_EFC, UNCAPPED) == 1 ||
                json.optInt(Constants.KEY_EXCLUDE_GLOBAL_CAPS, UNCAPPED) == 1
            // Gate on the stable campaign id (ti), NOT unitID (= wzrk_id, ti_yyyyMMdd, rotates daily) —
            // must match how didShow records and how the evaluator keys whenLimits.
            val campaignId = json.optString(Constants.INAPP_ID_IN_PAYLOAD)
            val canShow = ndFCManager.canShow(
                campaignId,
                excludeFromCaps,
                json.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, UNCAPPED), // mdc = per-target session cap
                false, // advanced whenLimits already applied at evaluation time
            )
            if (!canShow) {
                logger.verbose(
                    accountId,
                    "${Constants.FEATURE_DISPLAY_UNIT}ND unit ${unit.unitID} suppressed by frequency caps",
                )
            }
            canShow
        }
    }
}
