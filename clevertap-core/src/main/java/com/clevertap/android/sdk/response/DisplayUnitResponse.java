package com.clevertap.android.sdk.response;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
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

        final JSONArray notifs = response.optJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY);
        final JSONArray appLaunched = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY);
        final boolean hasNotifs = notifs != null && notifs.length() > 0;
        final boolean hasAppLaunched = appLaunched != null && appLaunched.length() > 0;
        if (!hasNotifs && !hasAppLaunched) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "JSON object doesn't contain the Display Units key");
            return;
        }
        try {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Processing Display Unit response");
            parseDisplayUnits(notifs, appLaunched);
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to parse response", t);
        }
    }

    /**
     * Parses Display Units from both {@code adUnit_notifs} and (non-CG-suppressed)
     * {@code adUnit_notifs_applaunched}, merges them, applies the ND frequency-cap gate, and writes
     * the cache once. Both keys are handled in a single pass because
     * {@link com.clevertap.android.sdk.displayunits.CTDisplayUnitController#updateDisplayUnits}
     * replaces (not merges) the cache — two separate writes for one response would wipe each other.
     * CG stubs ({@code suppressed:true}) are skipped here; they are acked by {@link AdUnitResponse}.
     *
     * A response with no valid units is a no-op (cache untouched, callback not fired), preserving the
     * legacy pre-8.x contract / iOS parity.
     */
    private void parseDisplayUnits(JSONArray notifs, JSONArray appLaunched) {
        final ArrayList<CleverTapDisplayUnit> parsed = new ArrayList<>();
        if (notifs != null) {
            parsed.addAll(parseDisplayUnitsFromJson(notifs, false));
        }
        if (appLaunched != null) {
            parsed.addAll(parseDisplayUnitsFromJson(appLaunched, true));
        }
        if (parsed.isEmpty()) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "No valid Display Units to process");
            return;
        }

        DisplayUnitCache cache = controllerManager.getOrCreateDisplayUnitCache();
        if (cache == null) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "No display-unit cache available");
            return;
        }

        ArrayList<CleverTapDisplayUnit> displayUnits = NdFcapGate.filter(
                parsed, controllerManager.getNdFCManager(), logger, config.getAccountId());
        cache.updateDisplayUnits(displayUnits);
        if (!displayUnits.isEmpty()) {
            callbackManager.notifyDisplayUnitsLoaded(displayUnits);
        }
    }

    /**
     * Converts a JSON array of display units into model objects, filtering out malformed entries.
     * When {@code skipSuppressed} is true, CG-suppressed stubs ({@code suppressed:true}, no content)
     * are skipped — those are acked by {@link AdUnitResponse}, not rendered.
     */
    @NonNull
    private ArrayList<CleverTapDisplayUnit> parseDisplayUnitsFromJson(@NonNull JSONArray messages, boolean skipSuppressed) {
        final ArrayList<CleverTapDisplayUnit> list = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            try {
                JSONObject json = messages.getJSONObject(i);
                if (skipSuppressed && json.optBoolean(Constants.INAPP_SUPPRESSED, false)) {
                    continue;
                }
                CleverTapDisplayUnit unit = CleverTapDisplayUnit.toDisplayUnit(json);
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
