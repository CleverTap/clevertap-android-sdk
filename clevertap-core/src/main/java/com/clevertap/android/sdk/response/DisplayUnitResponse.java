package com.clevertap.android.sdk.response;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.displayunits.DisplayUnitCache;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    @Override
    public boolean runsDuringUserSwitch() {
        return false; // display units are reset for the new user after the switch
    }

    //Logic for the processing of Display Unit response

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        processResponse(response, stringBody, context, ResponseContext.DEFAULT);
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context,
            final ResponseContext responseContext) {

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
            parseDisplayUnits(response.getJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY),
                    responseContext.getSource());
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
    private void parseDisplayUnits(JSONArray messages, CTResponseSource source) {
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

        ArrayList<CleverTapDisplayUnit> parsedUnits = parseDisplayUnitsFromJson(messages);

        // A /content response carries only the personalized subset of display units. The cache's
        // default updateDisplayUnits() is a REPLACE, so applying that subset as-is would wipe the
        // units delivered by /a1. When the source is a content fetch we merge by unitID into the
        // current set instead; /a1 stays authoritative and keeps REPLACE. The merge lives here,
        // before the cache, so it also holds for a host-supplied DisplayUnitCache and needs no
        // public API change (mirrors iOS).
        final ArrayList<CleverTapDisplayUnit> unitsToApply;
        if (source == CTResponseSource.CONTENT_FETCH) {
            // A content fetch that brought no mergeable unit (all empty/duplicate unitIDs) would
            // leave the merged set identical to the current cache — re-publishing the unchanged /a1
            // set to listeners. Skip entirely so we notify only when a unit was actually received.
            if (!hasMergeableUnit(parsedUnits)) {
                return;
            }
            unitsToApply = mergeByUnitId(cache.getAllDisplayUnits(), parsedUnits);
        } else {
            unitsToApply = parsedUnits;
        }

        cache.updateDisplayUnits(unitsToApply);
        if (!unitsToApply.isEmpty()) {
            callbackManager.notifyDisplayUnitsLoaded(unitsToApply);
        }
    }

    /**
     * Merges {@code incoming} units into {@code existing} keyed by unitID: a unit whose unitID is
     * already present replaces it in place (original position preserved), a new unitID is appended.
     * Units with an empty unitID are skipped, matching
     * {@link com.clevertap.android.sdk.displayunits.CTDisplayUnitController#updateDisplayUnits}.
     *
     * Note: the sentinel unitID "0_0" (substituted when a payload has no wzrk_id) collapses several
     * such units to the last one seen — same limitation as the default cache and iOS, an accepted
     * edge for malformed payloads.
     */
    @NonNull
    private ArrayList<CleverTapDisplayUnit> mergeByUnitId(
            @Nullable ArrayList<CleverTapDisplayUnit> existing,
            @NonNull ArrayList<CleverTapDisplayUnit> incoming) {
        final LinkedHashMap<String, CleverTapDisplayUnit> merged = new LinkedHashMap<>();
        if (existing != null) {
            for (CleverTapDisplayUnit unit : existing) {
                if (unit != null && !TextUtils.isEmpty(unit.getUnitID())) {
                    merged.put(unit.getUnitID(), unit);
                }
            }
        }
        for (CleverTapDisplayUnit unit : incoming) {
            if (unit != null && !TextUtils.isEmpty(unit.getUnitID())) {
                merged.put(unit.getUnitID(), unit);
            }
        }
        return new ArrayList<>(merged.values());
    }

    // True if at least one unit carries a usable unitID, i.e. would actually merge into the cache.
    private boolean hasMergeableUnit(@NonNull ArrayList<CleverTapDisplayUnit> units) {
        for (CleverTapDisplayUnit unit : units) {
            if (unit != null && !TextUtils.isEmpty(unit.getUnitID())) {
                return true;
            }
        }
        return false;
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
