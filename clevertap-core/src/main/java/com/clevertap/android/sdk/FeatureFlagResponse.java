package com.clevertap.android.sdk;

import android.content.Context;
import android.telecom.Call;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class FeatureFlagResponse extends CleverTapResponseDecorator {

    private CTFeatureFlagsController mCTFeatureFlagsController;

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;

    FeatureFlagResponse(CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config, ControllerManager controllerManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mLogger = mConfig.getLogger();
        mCTFeatureFlagsController = controllerManager.getCTFeatureFlagsController();
    }

    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {
        mLogger.verbose(mConfig.getAccountId(), "Processing Feature Flags response...");

        if (mConfig.isAnalyticsOnly()) {
            mLogger.verbose(mConfig.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing Feature Flags response");
            // process product config response
            mCleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.FEATURE_FLAG_UNIT + "Can't parse Feature Flags Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY)) {
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.FEATURE_FLAG_UNIT + "JSON object doesn't contain the Feature Flags key");
            return;
        }
        try {
            mLogger
                    .verbose(mConfig.getAccountId(),
                            Constants.FEATURE_FLAG_UNIT + "Processing Feature Flags response");
            parseFeatureFlags(response.getJSONObject(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), Constants.FEATURE_FLAG_UNIT + "Failed to parse response", t);
        }

        // process product config response
        mCleverTapResponse.processResponse(response, stringBody, context);

    }

    private void parseFeatureFlags(JSONObject responseKV) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && mCTFeatureFlagsController != null) {
            mCTFeatureFlagsController.updateFeatureFlags(responseKV);
        }
    }
}
