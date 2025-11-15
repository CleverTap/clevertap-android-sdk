package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.network.IJRepo;
import org.json.JSONObject;

public class MetadataResponse {

    private final String accountId;
    private final ILogger logger;

    public MetadataResponse(
            String accountId,
            ILogger logger
    ) {
        this.accountId = accountId;
        this.logger = logger;
    }

    public void processResponse(final JSONObject response, final Context context, final IJRepo ijRepo, final DeviceInfo deviceInfo) {
        // Always look for a GUID in the response, and if present, then perform a force update
        try {
            if (response.has("g")) {
                final String deviceID = response.getString("g");
                deviceInfo.forceUpdateDeviceId(deviceID);
                logger.verbose(accountId, "Got a new device ID: " + deviceID);
            }
        } catch (Throwable t) {
            logger.verbose(accountId, "Failed to update device ID!", t);
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
