package com.clevertap.android.sdk.response;

import android.content.Context;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.product_config.CTProductConfigController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProductConfigResponse extends CleverTapResponseDecorator {

    private final String accountId;

    private final CoreMetaData coreMetaData;

    private final Logger logger;

    private CTProductConfigController controller;

    public ProductConfigResponse(
            CleverTapInstanceConfig config,
            CoreMetaData coreMetaData
    ) {
        this.accountId = config.getAccountId();
        this.logger = config.getLogger();
        this.coreMetaData = coreMetaData;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        logger.verbose(accountId, "Processing Product Config response...");

        if (response == null) {
            logger.verbose(accountId, Constants.LOG_TAG_PRODUCT_CONFIG
                    + "Can't parse Product Config Response, JSON response object is null");
            onProductConfigFailed();
            return;
        }

        if (!response.has(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY)) {
            logger.verbose(accountId,
                    Constants.LOG_TAG_PRODUCT_CONFIG + "JSON object doesn't contain the Product Config key");
            onProductConfigFailed();
            return;
        }
        try {
            logger.verbose(accountId,
                    Constants.LOG_TAG_PRODUCT_CONFIG + "Processing Product Config response");
            parseProductConfigs(response.getJSONObject(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            onProductConfigFailed();
            logger.verbose(accountId,
                    Constants.LOG_TAG_PRODUCT_CONFIG + "Failed to parse Product Config response", t);
        }
    }

    private void onProductConfigFailed() {
        if (coreMetaData.isProductConfigRequested()) {
            if (controller != null) {
                controller.onFetchFailed();
            }
            coreMetaData.setProductConfigRequested(false);
        }
    }

    private void parseProductConfigs(JSONObject responseKV) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && controller != null) {
            controller.onFetchSuccess(responseKV);
        } else {
            onProductConfigFailed();
        }
    }

    public void setController(CTProductConfigController controller) {
        this.controller = controller;
    }
}
