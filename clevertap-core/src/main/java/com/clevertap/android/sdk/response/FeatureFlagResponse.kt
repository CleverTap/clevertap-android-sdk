package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import org.json.JSONException
import org.json.JSONObject

internal open class FeatureFlagResponse(
    private val config: CleverTapInstanceConfig,
    private val controllerManager: ControllerManager
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        val response = jsonBody
        logger.verbose(config.accountId, "Processing Feature Flags response...")

        if (config.isAnalyticsOnly) {
            logger.verbose(
                config.accountId,
                "CleverTap instance is configured to analytics only, not processing Feature Flags response"
            )
            // process product config response
            return
        }

        if (response == null) {
            logger.verbose(
                config.accountId,
                Constants.FEATURE_FLAG_UNIT + "Can't parse Feature Flags Response, JSON response object is null"
            )
            return
        }

        if (!response.has(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY)) {
            logger.verbose(
                config.accountId,
                Constants.FEATURE_FLAG_UNIT + "JSON object doesn't contain the Feature Flags key"
            )
            // process product config response
            return
        }
        try {
            logger
                .verbose(
                    config.accountId,
                    Constants.FEATURE_FLAG_UNIT + "Processing Feature Flags response"
                )
            parseFeatureFlags(response.getJSONObject(Constants.FEATURE_FLAG_JSON_RESPONSE_KEY))
        } catch (t: Throwable) {
            logger.verbose(config.accountId, Constants.FEATURE_FLAG_UNIT + "Failed to parse response", t)
        }
    }

    @Throws(JSONException::class)
    private fun parseFeatureFlags(responseKV: JSONObject) {
        val kvArray = responseKV.getJSONArray(Constants.KEY_KV)

        if (kvArray != null && controllerManager.ctFeatureFlagsController != null) {
            controllerManager.ctFeatureFlagsController!!.updateFeatureFlags(responseKV)
        } else {
            config.logger.verbose(
                config.accountId,
                Constants.FEATURE_FLAG_UNIT + "Can't parse feature flags, CTFeatureFlagsController is null"
            )
        }
    }
}
