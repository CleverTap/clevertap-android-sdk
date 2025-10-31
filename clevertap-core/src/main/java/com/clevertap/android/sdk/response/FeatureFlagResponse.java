package com.clevertap.android.sdk.response;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FeatureFlagResponse {

    private final String accountId;
    private final ILogger logger;

    public FeatureFlagResponse(String accountId, ILogger logger) {
        this.accountId = accountId;
        this.logger = logger;
    }

    public void processResponse(final JSONObject response, CTFeatureFlagsController controller) {
        logger.verbose(accountId, "Processing Feature Flags response...");

        if (response == null) {
            logger.verbose(accountId,
                    Constants.FEATURE_FLAG_UNIT + "Can't parse Feature Flags Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY)) {
            logger.verbose(accountId,
                    Constants.FEATURE_FLAG_UNIT + "JSON object doesn't contain the Feature Flags key");
            // process product config response
            return;
        }
        try {
            logger.verbose(accountId,
                    Constants.FEATURE_FLAG_UNIT + "Processing Feature Flags response");
            parseFeatureFlags(controller, response.getJSONObject(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            logger.verbose(accountId, Constants.FEATURE_FLAG_UNIT + "Failed to parse response", t);
        }
    }

    private void parseFeatureFlags(CTFeatureFlagsController controller, JSONObject responseKV) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && controller != null) {
            controller.updateFeatureFlags(responseKV);
        } else {
            logger.verbose(accountId,
                    Constants.FEATURE_FLAG_UNIT + "Can't parse feature flags, CTFeatureFlagsController is null");
        }
    }
}
