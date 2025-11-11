package com.clevertap.android.sdk.response;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.features.DisplayUnitContract;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class DisplayUnitResponse {

    private final String accountId;

    private final ILogger logger;
    public DisplayUnitResponse(
            String accountId,
            ILogger logger
    ) {
        this.accountId = accountId;
        this.logger = logger;
    }

    //Logic for the processing of Display Unit response

    public void processResponse(final JSONObject response, DisplayUnitContract displayUnitContract) {

        logger.verbose(accountId, "Processing Display Unit items...");

        // Adding response null check because this will get processed first in case of analytics
        if (response == null) {
            logger.verbose(accountId, Constants.FEATURE_DISPLAY_UNIT
                    + "Can't parse Display Unit Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY)) {
            logger.verbose(accountId,
                    Constants.FEATURE_DISPLAY_UNIT + "JSON object doesn't contain the Display Units key");
            return;
        }
        try {
            logger.verbose(accountId, Constants.FEATURE_DISPLAY_UNIT + "Processing Display Unit response");
            parseDisplayUnits(response.getJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY), displayUnitContract);
        } catch (Throwable t) {
            logger.verbose(accountId, Constants.FEATURE_DISPLAY_UNIT + "Failed to parse response", t);
        }
    }

    /**
     * Parses the Display Units using the JSON response
     *
     * @param messages - Json array of Display Unit items
     */
    private void parseDisplayUnits(JSONArray messages, DisplayUnitContract displayUnitContract) {
        if (messages == null || messages.length() == 0) {
            logger.verbose(accountId,
                    Constants.FEATURE_DISPLAY_UNIT + "Can't parse Display Units, jsonArray is either empty or null");
            return;
        }

        CTDisplayUnitController controller = displayUnitContract.provideDisplayUnitController();
        ArrayList<CleverTapDisplayUnit> displayUnits = controller.updateDisplayUnits(messages);

        displayUnitContract.notifyDisplayUnitsLoaded(displayUnits);
    }
}
