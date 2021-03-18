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

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final CoreMetaData coreMetaData;

    private final Logger logger;

    private final ControllerManager controllerManager;

    public ProductConfigResponse(CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config,
            CoreMetaData coreMetaData, ControllerManager controllerManager) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        logger = this.config.getLogger();
        this.coreMetaData = coreMetaData;
        this.controllerManager = controllerManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        logger.verbose(config.getAccountId(), "Processing Product Config response...");

        if (config.isAnalyticsOnly()) {
            logger.verbose(config.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing Product Config response");
            // process geofence response
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            logger.verbose(config.getAccountId(), Constants.LOG_TAG_PRODUCT_CONFIG
                    + "Can't parse Product Config Response, JSON response object is null");
            onProductConfigFailed();
            return;
        }

        if (!response.has(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY)) {
            logger.verbose(config.getAccountId(),
                    Constants.LOG_TAG_PRODUCT_CONFIG + "JSON object doesn't contain the Product Config key");
            onProductConfigFailed();
            // process geofence response
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }
        try {
            logger
                    .verbose(config.getAccountId(),
                            Constants.LOG_TAG_PRODUCT_CONFIG + "Processing Product Config response");
            parseProductConfigs(response.getJSONObject(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            onProductConfigFailed();
            logger.verbose(config.getAccountId(),
                    Constants.LOG_TAG_PRODUCT_CONFIG + "Failed to parse Product Config response", t);
        }

        // process geofence response
        cleverTapResponse.processResponse(response, stringBody, context);

    }

    private void onProductConfigFailed() {
        if (coreMetaData.isProductConfigRequested()) {
            if (controllerManager.getCTProductConfigController() != null) {
                controllerManager.getCTProductConfigController().onFetchFailed();
            }
            coreMetaData.setProductConfigRequested(false);
        }
    }

    private void parseProductConfigs(JSONObject responseKV) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && controllerManager.getCTProductConfigController() != null) {
            controllerManager.getCTProductConfigController().onFetchSuccess(responseKV);
        } else {
            onProductConfigFailed();
        }
    }
}
