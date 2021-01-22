package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

class MetadataResponse extends CleverTapResponseDecorator {


    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final DeviceInfo mDeviceInfo;

    private final Logger mLogger;

    private final NetworkManager mNetworkManager;

    MetadataResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config, DeviceInfo deviceInfo,
            NetworkManager networkManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mLogger = mConfig.getLogger();
        mDeviceInfo = deviceInfo;
        mNetworkManager = networkManager;
    }


    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {
        // Always look for a GUID in the response, and if present, then perform a force update
        try {
            if (response.has("g")) {
                final String deviceID = response.getString("g");
                mDeviceInfo.forceUpdateDeviceId(deviceID);
                mLogger.verbose(mConfig.getAccountId(), "Got a new device ID: " + deviceID);
            }
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), "Failed to update device ID!", t);
        }

        // Handle i
        try {
            if (response.has("_i")) {
                final long i = response.getLong("_i");
                mNetworkManager.setI(context, i);
            }
        } catch (Throwable t) {
            // Ignore
        }

        // Handle j
        try {
            if (response.has("_j")) {
                final long j = response.getLong("_j");
                mNetworkManager.setJ(context, j);
            }
        } catch (Throwable t) {
            // Ignore
        }

        // process ARP response
        mCleverTapResponse.processResponse(response, stringBody, context);
    }
}
