package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.network.NetworkManager;
import org.json.JSONObject;

public class BaseResponse extends CleverTapResponseDecorator {

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final LocalDataStore localDataStore;

    private final NetworkManager networkManager;

    public BaseResponse(Context context, CleverTapInstanceConfig config,
            DeviceInfo deviceInfo, NetworkManager networkManager, LocalDataStore localDataStore,
            CleverTapResponse cleverTapResponse) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        this.networkManager = networkManager;
        this.localDataStore = localDataStore;
    }

    @Override
    public void processResponse(final JSONObject jsonBody, final String responseStr, final Context context) {

        if (responseStr == null) {
            Logger.verbose(config.getAccountId(), "Problem processing queue response, response is null");
            return;
        }
        try {
            Logger.verbose(config.getAccountId(), "Trying to process response: " + responseStr);

            JSONObject response = new JSONObject(responseStr);
            // in app
            cleverTapResponse.processResponse(response, responseStr, context);

            try {
                localDataStore.syncWithUpstream(context, response);
            } catch (Throwable t) {
                Logger.verbose(config.getAccountId(), "Failed to sync local cache with upstream", t);
            }

        } catch (Throwable t) {
            networkManager.incrementResponseFailureCount();
            Logger.verbose(config.getAccountId(), "Problem process send queue response", t);
        }
    }
}
