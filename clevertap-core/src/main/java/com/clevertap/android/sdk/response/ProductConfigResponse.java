package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProductConfigResponse extends CleverTapResponseDecorator {

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final CoreMetaData mCoreMetaData;

    private final Logger mLogger;

    private final ControllerManager mControllerManager;

    public ProductConfigResponse(CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config,
            CoreMetaData coreMetaData, ControllerManager controllerManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mLogger = mConfig.getLogger();
        mCoreMetaData = coreMetaData;
        mControllerManager = controllerManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        mLogger.verbose(mConfig.getAccountId(), "Processing Product Config response...");

        if (mConfig.isAnalyticsOnly()) {
            mLogger.verbose(mConfig.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing Product Config response");
            // process geofence response
            mCleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            mLogger.verbose(mConfig.getAccountId(), Constants.LOG_TAG_PRODUCT_CONFIG
                    + "Can't parse Product Config Response, JSON response object is null");
            onProductConfigFailed();
            return;
        }

        if (!response.has(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY)) {
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.LOG_TAG_PRODUCT_CONFIG + "JSON object doesn't contain the Product Config key");
            onProductConfigFailed();
            return;
        }
        try {
            mLogger
                    .verbose(mConfig.getAccountId(),
                            Constants.LOG_TAG_PRODUCT_CONFIG + "Processing Product Config response");
            parseProductConfigs(response.getJSONObject(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            onProductConfigFailed();
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.LOG_TAG_PRODUCT_CONFIG + "Failed to parse Product Config response", t);
        }

        // process geofence response
        mCleverTapResponse.processResponse(response, stringBody, context);

    }

    private void onProductConfigFailed() {
        if (mCoreMetaData.isProductConfigRequested()) {
            if (mControllerManager.getCTProductConfigController() != null) {
                mControllerManager.getCTProductConfigController().onFetchFailed();
            }
            mCoreMetaData.setProductConfigRequested(false);
        }
    }

    private void parseProductConfigs(JSONObject responseKV) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && mControllerManager.getCTProductConfigController() != null) {
            mControllerManager.getCTProductConfigController().onFetchSuccess(responseKV);
        } else {
            onProductConfigFailed();
        }
    }
}
