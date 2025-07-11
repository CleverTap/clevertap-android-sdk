package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.network.IJRepo;
import org.json.JSONObject;

public class MetadataResponse extends CleverTapResponseDecorator {

    private final CleverTapInstanceConfig config;

    private final DeviceInfo deviceInfo;

    private final Logger logger;
    private final IJRepo ijRepo;

    public MetadataResponse(
            CleverTapInstanceConfig config,
            DeviceInfo deviceInfo,
            IJRepo ijRepo
    ) {
        this.config = config;
        logger = this.config.getLogger();
        this.deviceInfo = deviceInfo;
        this.ijRepo = ijRepo;
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
                ijRepo.setI(context, i);
            }
        } catch (Throwable t) {
            // Ignore
        }

        // Handle j
        try {
            if (response.has("_j")) {
                final long j = response.getLong("_j");
                ijRepo.setJ(context, j);
            }
        } catch (Throwable t) {
            // Ignore
        }
    }
}
