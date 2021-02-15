package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import org.json.JSONObject;

public class GeofenceResponse extends CleverTapResponseDecorator {

    private final BaseCallbackManager mCallbackManager;

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;

    public GeofenceResponse(final CleverTapResponse cleverTapResponse,
            CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mLogger = mConfig.getLogger();
        mCallbackManager = callbackManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        mLogger.verbose(mConfig.getAccountId(), "Processing GeoFences response...");

        if (mConfig.isAnalyticsOnly()) {
            mLogger.verbose(mConfig.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing geofence response");

            // process further response
            mCleverTapResponse.processResponse(response, stringBody, context);
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

        // process further response
        mCleverTapResponse.processResponse(response, stringBody, context);

    }
}
