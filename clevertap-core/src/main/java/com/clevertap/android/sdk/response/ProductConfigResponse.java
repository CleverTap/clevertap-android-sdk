package com.clevertap.android.sdk.response;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.product_config.CTProductConfigController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProductConfigResponse {

    private final String accountId;

    private final ILogger logger;

    public ProductConfigResponse(
            String accountId,
            ILogger logger
    ) {
        this.accountId = accountId;
        this.logger = logger;
    }

    public void processResponse(
            final JSONObject response,
            CTProductConfigController controller,
            CoreMetaData coreMetaData
    ) {
        logger.verbose(accountId, "Processing Product Config response...");

        if (response == null) {
            logger.verbose(accountId, Constants.LOG_TAG_PRODUCT_CONFIG
                    + "Can't parse Product Config Response, JSON response object is null");
            onProductConfigFailed(controller, coreMetaData);
            return;
        }

        if (!response.has(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY)) {
            logger.verbose(accountId, Constants.LOG_TAG_PRODUCT_CONFIG + "JSON object doesn't contain the Product Config key");
            onProductConfigFailed(controller, coreMetaData);
            return;
        }
        try {
            logger.verbose(accountId,
                    Constants.LOG_TAG_PRODUCT_CONFIG + "Processing Product Config response");
            JSONObject resp = response.getJSONObject(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY);
            parseProductConfigs(resp, controller, coreMetaData);
        } catch (Throwable t) {
            onProductConfigFailed(controller, coreMetaData);
            logger.verbose(accountId,
                    Constants.LOG_TAG_PRODUCT_CONFIG + "Failed to parse Product Config response", t);
        }
    }

    private void parseProductConfigs(
            JSONObject responseKV,
            CTProductConfigController controller,
            CoreMetaData coreMetaData
    ) throws JSONException {
        JSONArray kvArray = responseKV.getJSONArray(Constants.KEY_KV);

        if (kvArray != null && controller != null) {
            controller.onFetchSuccess(responseKV);
        } else {
            onProductConfigFailed(controller, coreMetaData);
        }
    }

    private void onProductConfigFailed(
            CTProductConfigController controller,
            CoreMetaData coreMetaData
    ) {
        if (coreMetaData.isProductConfigRequested()) {
            controller.onFetchFailed();
        }
        coreMetaData.setProductConfigRequested(false);
    }
}
