package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class DisplayUnitResponse extends CleverTapResponseDecorator {

    private final Object displayUnitControllerLock = new Object();

    private final BaseCallbackManager callbackManager;

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final ControllerManager controllerManager;

    private final Logger logger;

    public DisplayUnitResponse(CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager, ControllerManager controllerManager) {
        this.cleverTapResponse = cleverTapResponse;
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
            cleverTapResponse.processResponse(response, stringBody, context);
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
            // process feature flag response
            cleverTapResponse.processResponse(response, stringBody, context);
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

        // process feature flag response
        cleverTapResponse.processResponse(response, stringBody, context);
    }

    /**
     * Parses the Display Units using the JSON response
     *
     * @param messages - Json array of Display Unit items
     */
    private void parseDisplayUnits(JSONArray messages) {
        if (messages == null || messages.length() == 0) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Can't parse Display Units, jsonArray is either empty or null");
            return;
        }

        synchronized (displayUnitControllerLock) {// lock to avoid multiple instance creation for controller
            if (controllerManager.getCTDisplayUnitController() == null) {
                controllerManager.setCTDisplayUnitController(new CTDisplayUnitController());
            }
        }
        ArrayList<CleverTapDisplayUnit> displayUnits = controllerManager.getCTDisplayUnitController()
                .updateDisplayUnits(messages);

        callbackManager.notifyDisplayUnitsLoaded(displayUnits);
    }
}
