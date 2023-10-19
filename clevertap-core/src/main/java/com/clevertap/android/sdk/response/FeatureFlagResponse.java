package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FeatureFlagResponse extends CleverTapResponseDecorator {

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final ControllerManager controllerManager;

    public FeatureFlagResponse(CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config, ControllerManager controllerManager) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        this.controllerManager = controllerManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        Logger.verbose(config.getAccountId(), "Processing Feature Flags response...");

        if (config.isAnalyticsOnly()) {
            Logger.verbose(config.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing Feature Flags response");
            // process product config response
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            Logger.verbose(config.getAccountId(),
                    Constants.FEATURE_FLAG_UNIT + "Can't parse Feature Flags Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY)) {
            Logger.verbose(config.getAccountId(),
                    Constants.FEATURE_FLAG_UNIT + "JSON object doesn't contain the Feature Flags key");
            // process product config response
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }
        try {
            Logger
                    .verbose(config.getAccountId(),
                            Constants.FEATURE_FLAG_UNIT + "Processing Feature Flags response");
            parseFeatureFlags(response.getJSONObject(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            Logger.verbose(config.getAccountId(), Constants.FEATURE_FLAG_UNIT + "Failed to parse response", t);
        }

        // process product config response
        cleverTapResponse.processResponse(response, stringBody, context);

    }

    private void parseFeatureFlags(JSONObject responseKV) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && controllerManager.getCTFeatureFlagsController() != null) {
            controllerManager.getCTFeatureFlagsController().updateFeatureFlags(responseKV);
        }else {
            Logger.verbose(config.getAccountId(),
                    Constants.FEATURE_FLAG_UNIT + "Can't parse feature flags, CTFeatureFlagsController is null");
        }
    }
}
