package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.network.NetworkManager;
import org.json.JSONObject;

public class MetadataResponse extends CleverTapResponseDecorator {


    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final DeviceInfo deviceInfo;

    private final Logger logger;

    private final NetworkManager networkManager;

    public MetadataResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config,
            DeviceInfo deviceInfo,
            NetworkManager networkManager) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        logger = this.config.getLogger();
        this.deviceInfo = deviceInfo;
        this.networkManager = networkManager;
    }


    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        // Always look for a GUID in the response, and if present, then perform a force update
        try {
            if (response.has("g")) {
                final String deviceID = response.getString("g");
                deviceInfo.forceUpdateDeviceId(deviceID);
                logger.verbose(config.getAccountId(), "Got a new device ID: " + deviceID);
            }
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), "Failed to update device ID!", t);
        }

        // Handle i
        try {
            if (response.has("_i")) {
                final long i = response.getLong("_i");
                networkManager.setI(context, i);
            }
        } catch (Throwable t) {
            // Ignore
        }

        // Handle j
        try {
            if (response.has("_j")) {
                final long j = response.getLong("_j");
                networkManager.setJ(context, j);
            }
        } catch (Throwable t) {
            // Ignore
        }

        // process ARP response
        cleverTapResponse.processResponse(response, stringBody, context);
    }
}
