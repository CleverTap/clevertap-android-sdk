package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

class DisplayUnitResponse extends CleverTapResponse {

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;

    DisplayUnitResponse(CleverTapResponse cleverTapResponse) {
        mCleverTapResponse = cleverTapResponse;
        CoreState coreState = getCoreState();
        mConfig = coreState.getConfig();
        mLogger = mConfig.getLogger();

    }

    //Logic for the processing of Display Unit response

    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {

        mLogger.verbose(mConfig.getAccountId(), "Processing Display Unit items...");

        if (mConfig.isAnalyticsOnly()) {
            mLogger.verbose(mConfig.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing Display Unit response");
            // process feature flag response
            mCleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            mLogger.verbose(mConfig.getAccountId(), Constants.FEATURE_DISPLAY_UNIT
                    + "Can't parse Display Unit Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY)) {
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "JSON object doesn't contain the Display Units key");
            return;
        }
        try {
            mLogger
                    .verbose(mConfig.getAccountId(),
                            Constants.FEATURE_DISPLAY_UNIT + "Processing Display Unit response");
            parseDisplayUnits(response.getJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to parse response", t);
        }

        // process feature flag response
        mCleverTapResponse.processResponse(response, stringBody, context);
    }

    /**
     * Parses the Display Units using the JSON response
     *
     * @param messages - Json array of Display Unit items
     */
    private void parseDisplayUnits(JSONArray messages) {
        if (messages == null || messages.length() == 0) {
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Can't parse Display Units, jsonArray is either empty or null");
            return;
        }

        synchronized (displayUnitControllerLock) {// lock to avoid multiple instance creation for controller
            if (mCTDisplayUnitController == null) {
                mCTDisplayUnitController = new CTDisplayUnitController();
            }
        }
        ArrayList<CleverTapDisplayUnit> displayUnits = mCTDisplayUnitController.updateDisplayUnits(messages);

        notifyDisplayUnitsLoaded(displayUnits);
    }
}
