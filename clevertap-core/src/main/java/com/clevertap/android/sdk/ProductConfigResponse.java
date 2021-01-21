package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ProductConfigResponse extends CleverTapResponseDecorator {


    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;

    ProductConfigResponse(CleverTapResponse cleverTapResponse) {
        mCleverTapResponse = cleverTapResponse;
        CoreState coreState = getCoreState();
        mConfig = coreState.getConfig();
        mLogger = mConfig.getLogger();

    }

    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {
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
        if (getCoreState().getCoreMetaData().isProductConfigRequested()) {
            if (getCoreState().getCtProductConfigController() != null) {
                getCoreState().getCtProductConfigController().onFetchFailed();
            }
            getCoreState().getCoreMetaData().setProductConfigRequested(false);
        }
    }

    private void parseProductConfigs(JSONObject responseKV) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && getCoreState().getCtProductConfigController() != null) {
            getCoreState().getCtProductConfigController().onFetchSuccess(responseKV);
        } else {
            onProductConfigFailed();
        }
    }
}
