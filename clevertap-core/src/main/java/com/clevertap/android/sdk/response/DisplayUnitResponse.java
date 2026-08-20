package com.clevertap.android.sdk.response;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.NdFCManager;
import com.clevertap.android.sdk.displayunits.DisplayUnitCache;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DisplayUnitResponse extends CleverTapResponseDecorator {

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final ControllerManager controllerManager;

    private final Logger logger;

    public DisplayUnitResponse(
            CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager,
            ControllerManager controllerManager
    ) {
        this.config = config;
        logger = this.config.getLogger();
        this.callbackManager = callbackManager;
        this.controllerManager = controllerManager;
    }

    //Logic for the processing of Display Unit response

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {

        logger.verbose(config.getAccountId(), "Processing Display Unit items...");

        if (config.isAnalyticsOnly()) {
            logger.verbose(config.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing Display Unit response");
            // process feature flag response
            return;
        }

        // Adding response null check because this will get processed first in case of analytics
        if (response == null) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT
                    + "Can't parse Display Unit Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY)) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "JSON object doesn't contain the Display Units key");
            return;
        }
        try {
            logger
                    .verbose(config.getAccountId(),
                            Constants.FEATURE_DISPLAY_UNIT + "Processing Display Unit response");
            parseDisplayUnits(response.getJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to parse response", t);
        }
    }

    /**
     * Parses the Display Units from the JSON response, populates the cache and
     * notifies the callback only when at least one valid unit was received.
     *
     * A null or empty array is a no-op: the cache is not touched and the callback
     * is not fired. This preserves the legacy pre-8.x contract and matches iOS
     * parity — iOS guards on displayUnitJSON.count > 0 before doing anything.
     *
     * @param messages - Json array of Display Unit items
     */
    private void parseDisplayUnits(JSONArray messages) {
        if (messages == null || messages.length() == 0) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Can't parse Display Units, jsonArray is null or empty");
            return;
        }

        DisplayUnitCache cache = controllerManager.getOrCreateDisplayUnitCache();
        if (cache == null) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "No display-unit cache available");
            return;
        }

        ArrayList<CleverTapDisplayUnit> displayUnits = applyNdFrequencyCaps(parseDisplayUnitsFromJson(messages));
        cache.updateDisplayUnits(displayUnits);
        if (!displayUnits.isEmpty()) {
            callbackManager.notifyDisplayUnitsLoaded(displayUnits);
        }
    }

    /**
     * Native Display frequency caps (SDK-6055): drops units whose counter caps are maxed out before
     * they are handed to the host (the ND analog of in-app's canShow gate). Only units that carry an
     * fcap marker ({@code efc}/{@code tlc}/{@code tdc}/{@code mdc}/{@code excludeGlobalFCaps}) are
     * gated; unmarked units pass through unchanged so existing (non-fcap) display units are never
     * affected. Advanced {@code frequencyLimits}/{@code occurrenceLimits} were already applied during
     * evaluation (the server only ships content for {@code adUnit_eval}-voted campaigns), so the
     * delivery-time re-check here covers the counter caps only.
     */
    @NonNull
    private ArrayList<CleverTapDisplayUnit> applyNdFrequencyCaps(@NonNull ArrayList<CleverTapDisplayUnit> units) {
        final NdFCManager ndFCManager = controllerManager.getNdFCManager();
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
                    json.optInt(Constants.KEY_TLC, -1),  // -1 = uncapped
                    json.optInt(Constants.KEY_TDC, -1),  // -1 = uncapped
                    json.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, -1),
                    false /* advanced whenLimits already applied at evaluation time */);
            if (canShow) {
                allowed.add(unit);
            } else {
                logger.verbose(config.getAccountId(),
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

    /**
     * Converts a JSON array of display units into model objects, filtering out
     * malformed entries.
     */
    @NonNull
    private ArrayList<CleverTapDisplayUnit> parseDisplayUnitsFromJson(@NonNull JSONArray messages) {
        final ArrayList<CleverTapDisplayUnit> list = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            try {
                CleverTapDisplayUnit unit = CleverTapDisplayUnit.toDisplayUnit(messages.getJSONObject(i));
                if (TextUtils.isEmpty(unit.getError())) {
                    list.add(unit);
                } else {
                    logger.verbose(config.getAccountId(),
                            Constants.FEATURE_DISPLAY_UNIT + "Failed to convert JsonArray item at index:" + i
                                    + " to Display Unit");
                }
            } catch (JSONException e) {
                logger.verbose(config.getAccountId(),
                        Constants.FEATURE_DISPLAY_UNIT + "Failed to parse Display Unit at index " + i
                                + ": " + e.getLocalizedMessage());
            }
        }
        return list;
    }
}
