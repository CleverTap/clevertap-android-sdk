package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import org.json.JSONObject;

public class GeofenceResponse extends CleverTapResponseDecorator {

    private final BaseCallbackManager callbackManager;

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final Logger logger;

    public GeofenceResponse(final CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        logger = this.config.getLogger();
        this.callbackManager = callbackManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        logger.verbose(config.getAccountId(), "Processing GeoFences response...");

        if (config.isAnalyticsOnly()) {
            logger.verbose(config.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing geofence response");

            // process further response
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            logger.verbose(config.getAccountId(),
                    Constants.LOG_TAG_GEOFENCES + "Can't parse Geofences Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.GEOFENCES_JSON_RESPONSE_KEY)) {
            logger.verbose(config.getAccountId(),
                    Constants.LOG_TAG_GEOFENCES + "JSON object doesn't contain the Geofences key");
            // process further response
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }
        try {
            if (callbackManager.getGeofenceCallback() != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("geofences", response.getJSONArray(Constants.GEOFENCES_JSON_RESPONSE_KEY));

                logger
                        .verbose(config.getAccountId(),
                                Constants.LOG_TAG_GEOFENCES + "Processing Geofences response");
                callbackManager.getGeofenceCallback().handleGeoFences(jsonObject);
            } else {
                logger.debug(config.getAccountId(),
                        Constants.LOG_TAG_GEOFENCES + "Geofence SDK has not been initialized to handle the response");
            }
        } catch (Throwable t) {
            logger
                    .verbose(config.getAccountId(),
                            Constants.LOG_TAG_GEOFENCES + "Failed to handle Geofences response", t);
        }

        // process further response
        cleverTapResponse.processResponse(response, stringBody, context);

    }
}
