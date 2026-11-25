package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONObject

internal open class GeofenceResponse(
    private val config: CleverTapInstanceConfig,
    private val callbackManager: BaseCallbackManager
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        val response = jsonBody
        logger.verbose(config.accountId, "Processing GeoFences response...")

        if (config.isAnalyticsOnly) {
            logger.verbose(
                config.accountId,
                "CleverTap instance is configured to analytics only, not processing geofence response"
            )
            return
        }

        if (response == null) {
            logger.verbose(
                config.accountId,
                Constants.LOG_TAG_GEOFENCES + "Can't parse Geofences Response, JSON response object is null"
            )
            return
        }

        if (!response.has(Constants.GEOFENCES_JSON_RESPONSE_KEY)) {
            logger.verbose(
                config.accountId,
                Constants.LOG_TAG_GEOFENCES + "JSON object doesn't contain the Geofences key"
            )
            return
        }
        try {
            if (callbackManager.geofenceCallback != null) {
                val jsonObject = JSONObject()
                jsonObject.put("geofences", response.getJSONArray(Constants.GEOFENCES_JSON_RESPONSE_KEY))

                logger
                    .verbose(
                        config.accountId,
                        Constants.LOG_TAG_GEOFENCES + "Processing Geofences response"
                    )
                callbackManager.geofenceCallback!!.handleGeoFences(jsonObject)
            } else {
                logger.debug(
                    config.accountId,
                    Constants.LOG_TAG_GEOFENCES + "Geofence SDK has not been initialized to handle the response"
                )
            }
        } catch (t: Throwable) {
            logger
                .verbose(
                    config.accountId,
                    Constants.LOG_TAG_GEOFENCES + "Failed to handle Geofences response", t
                )
        }
    }
}
