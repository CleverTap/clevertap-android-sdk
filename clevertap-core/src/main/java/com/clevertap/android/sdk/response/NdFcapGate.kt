package com.clevertap.android.sdk.response

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.NdFCManager
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit

/**
 * Native Display frequency-cap gate (SDK-6055). Filters display units through [NdFCManager.canShow]
 * before they are handed to the host — the ND analog of in-app's pre-display cap gate.
 *
 * Only units carrying an fcap marker (`efc`/`tlc`/`tdc`/`mdc`/`excludeGlobalFCaps`) are gated; unmarked
 * units pass through unchanged so existing non-fcap display units are never affected. Advanced
 * `frequencyLimits`/`occurrenceLimits` were already applied during evaluation (the server only ships
 * content for `adUnit_eval`-voted campaigns), so the delivery-time re-check here covers counter caps only.
 */
internal object NdFcapGate {

    private const val UNCAPPED = -1

    fun filter(
        units: List<CleverTapDisplayUnit>,
        ndFCManager: NdFCManager?,
        logger: Logger,
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
            val canShow = ndFCManager.canShow(
                unit.unitID,
                excludeFromCaps,
                json.optInt(Constants.KEY_TLC, UNCAPPED),
                json.optInt(Constants.KEY_TDC, UNCAPPED),
                json.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, UNCAPPED),
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
