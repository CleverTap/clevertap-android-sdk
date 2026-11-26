package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class ProductConfigResponse(
    private val config: CleverTapInstanceConfig,
    private val coreMetaData: CoreMetaData,
    private val controllerManager: ControllerManager
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        logger.verbose(config.accountId, "Processing Product Config response...")

        if (config.isAnalyticsOnly) {
            logger.verbose(
                config.accountId,
                "CleverTap instance is configured to analytics only, not processing Product Config response"
            )
            return
        }

        if (jsonBody == null) {
            logger.verbose(
                config.accountId,
                "${Constants.LOG_TAG_PRODUCT_CONFIG}Can't parse Product Config Response, JSON response object is null"
            )
            onProductConfigFailed()
            return
        }

        if (!jsonBody.has(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY)) {
            logger.verbose(
                config.accountId,
                "${Constants.LOG_TAG_PRODUCT_CONFIG}JSON object doesn't contain the Product Config key"
            )
            onProductConfigFailed()
            return
        }
        try {
            logger.verbose(config.accountId, "${Constants.LOG_TAG_PRODUCT_CONFIG}Processing Product Config response")
            parseProductConfigs(jsonBody.getJSONObject(Constants.REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY))
        } catch (t: Throwable) {
            onProductConfigFailed()
            logger.verbose(
                config.accountId,
                "${Constants.LOG_TAG_PRODUCT_CONFIG}Failed to parse Product Config response",
                t
            )
        }
    }

    private fun onProductConfigFailed() {
        if (coreMetaData.isProductConfigRequested) {
            controllerManager.ctProductConfigController?.onFetchFailed()
            coreMetaData.isProductConfigRequested = false
        }
    }

    private fun parseProductConfigs(responseKV: JSONObject) {
        // getJSONArray throws when the kv array is missing
        responseKV.getJSONArray(Constants.KEY_KV)

        val productConfigController = controllerManager.ctProductConfigController
        if (productConfigController != null) {
            productConfigController.onFetchSuccess(responseKV)
        } else {
            onProductConfigFailed()
        }
    }
}
