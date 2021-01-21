package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

class GeofenceResponse extends CleverTapResponse {

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;
    private final CallbackManager mCallbackManager;

    GeofenceResponse() {
        CoreState coreState = getCoreState();
        mConfig = coreState.getConfig();
        mLogger = mConfig.getLogger();
        mCallbackManager = coreState.getCallbackManager();
    }

    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {
        mLogger.verbose(mConfig.getAccountId(), "Processing GeoFences response...");

        if (mConfig.isAnalyticsOnly()) {
            mLogger.verbose(mConfig.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing geofence response");
            return;
        }

        if (response == null) {
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.LOG_TAG_GEOFENCES + "Can't parse Geofences Response, JSON response object is null");
            return;
        }

        if (!response.has(Constants.GEOFENCES_JSON_RESPONSE_KEY)) {
            mLogger.verbose(mConfig.getAccountId(),
                    Constants.LOG_TAG_GEOFENCES + "JSON object doesn't contain the Geofences key");
            return;
        }
        try {
            if (mCallbackManager.getGeofenceCallback() != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("geofences", response.getJSONArray(Constants.GEOFENCES_JSON_RESPONSE_KEY));

                mLogger
                        .verbose(mConfig.getAccountId(),
                                Constants.LOG_TAG_GEOFENCES + "Processing Geofences response");
                mCallbackManager.getGeofenceCallback().handleGeoFences(jsonObject);
            } else {
                mLogger.debug(mConfig.getAccountId(),
                        Constants.LOG_TAG_GEOFENCES + "Geofence SDK has not been initialized to handle the response");
            }
        } catch (Throwable t) {
            mLogger
                    .verbose(mConfig.getAccountId(),
                            Constants.LOG_TAG_GEOFENCES + "Failed to handle Geofences response", t);
        }


    }
}
