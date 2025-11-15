package com.clevertap.android.sdk.response;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.GeofenceCallback;
import com.clevertap.android.sdk.ILogger;
import org.json.JSONObject;

public class GeofenceResponse {

    private final String accountId;

    private final ILogger logger;

    public GeofenceResponse(
            String accountId,
            ILogger logger
    ) {
        this.accountId = accountId;
        this.logger = logger;
    }

    public void processResponse(final JSONObject response, GeofenceCallback callback) {
        logger.verbose(accountId, "Processing GeoFences response...");

        if (response == null) {
            logger.verbose(accountId,
                    Constants.LOG_TAG_GEOFENCES + "Can't parse Geofences Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.GEOFENCES_JSON_RESPONSE_KEY)) {
            logger.verbose(accountId,
                    Constants.LOG_TAG_GEOFENCES + "JSON object doesn't contain the Geofences key");
            return;
        }
        try {
            if (callback != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("geofences", response.getJSONArray(Constants.GEOFENCES_JSON_RESPONSE_KEY));

                logger.verbose(accountId,
                        Constants.LOG_TAG_GEOFENCES + "Processing Geofences response");
                callback.handleGeoFences(jsonObject);
            } else {
                logger.debug(accountId,
                        Constants.LOG_TAG_GEOFENCES + "Geofence SDK has not been initialized to handle the response");
            }
        } catch (Throwable t) {
            logger.verbose(accountId,
                    Constants.LOG_TAG_GEOFENCES + "Failed to handle Geofences response", t);
        }
    }
}
