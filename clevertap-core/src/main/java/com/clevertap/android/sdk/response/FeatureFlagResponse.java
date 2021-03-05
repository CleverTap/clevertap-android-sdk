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

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;

    private final ControllerManager mControllerManager;

    public FeatureFlagResponse(CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config, ControllerManager controllerManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mLogger = mConfig.getLogger();
        mControllerManager = controllerManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
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
            // process product config response
            mCleverTapResponse.processResponse(response, stringBody, context);
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

        if (kvArray != null && mControllerManager.getCTFeatureFlagsController() != null) {
            mControllerManager.getCTFeatureFlagsController().updateFeatureFlags(responseKV);
        }
    }
}
