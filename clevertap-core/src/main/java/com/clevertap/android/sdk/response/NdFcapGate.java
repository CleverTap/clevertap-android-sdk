package com.clevertap.android.sdk.response;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.NdFCManager;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Native Display frequency-cap gate (SDK-6055). Filters display units through
 * {@link NdFCManager#canShow} before they are handed to the host — the ND analog of in-app's
 * pre-display cap gate.
 *
 * <p>Only units carrying an fcap marker ({@code efc}/{@code tlc}/{@code tdc}/{@code mdc}/
 * {@code excludeGlobalFCaps}) are gated; unmarked units pass through unchanged so existing non-fcap
 * display units are never affected. Advanced {@code frequencyLimits}/{@code occurrenceLimits} were
 * already applied during evaluation (the server only ships content for {@code adUnit_eval}-voted
 * campaigns), so the delivery-time re-check here covers the counter caps only.
 */
final class NdFcapGate {

    private NdFcapGate() {
    }

    @NonNull
    static ArrayList<CleverTapDisplayUnit> filter(
            @NonNull ArrayList<CleverTapDisplayUnit> units,
            @Nullable NdFCManager ndFCManager,
            @NonNull Logger logger,
            String accountId
    ) {
        if (ndFCManager == null) {
            return units;
        }
        final ArrayList<CleverTapDisplayUnit> allowed = new ArrayList<>(units.size());
        for (CleverTapDisplayUnit unit : units) {
            final JSONObject json = unit.getJsonObject();
            if (json == null || !isFcapManaged(json)) {
                allowed.add(unit); // not fcap-managed -> deliver as before
                continue;
            }
            final boolean excludeFromCaps =
                    json.optInt(Constants.KEY_EFC, -1) == 1
                            || json.optInt(Constants.KEY_EXCLUDE_GLOBAL_CAPS, -1) == 1;
            final boolean canShow = ndFCManager.canShow(
                    unit.getUnitID(),
                    excludeFromCaps,
                    json.optInt(Constants.KEY_TLC, -1),   // -1 = uncapped
                    json.optInt(Constants.KEY_TDC, -1),   // -1 = uncapped
                    json.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, -1),
                    false /* advanced whenLimits already applied at evaluation time */);
            if (canShow) {
                allowed.add(unit);
            } else {
                logger.verbose(accountId,
                        Constants.FEATURE_DISPLAY_UNIT + "ND unit " + unit.getUnitID()
                                + " suppressed by frequency caps");
            }
        }
        return allowed;
    }

    private static boolean isFcapManaged(@NonNull JSONObject json) {
        return json.has(Constants.KEY_EFC)
                || json.has(Constants.KEY_TLC)
                || json.has(Constants.KEY_TDC)
                || json.has(Constants.INAPP_MAX_DISPLAY_COUNT)
                || json.has(Constants.KEY_EXCLUDE_GLOBAL_CAPS);
    }
}
