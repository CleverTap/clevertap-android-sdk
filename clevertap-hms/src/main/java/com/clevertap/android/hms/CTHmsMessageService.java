package com.clevertap.android.hms;

import android.os.Bundle;
import android.util.Log;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import org.json.JSONException;

import static com.clevertap.android.sdk.Utils.stringToBundle;

public class CTHmsMessageService extends HmsMessageService {
    private static final String TAG = "CTHmsMessageService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        CleverTapAPI.tokenRefresh(getApplicationContext(), token, PushConstants.PushType.HPS);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.i(TAG, "onMessageReceived is called");
        if (remoteMessage == null) {
            Log.e(TAG, "Received message entity is null!");
            return;
        }

        try {
            String ctData = remoteMessage.getData();
            Bundle extras = stringToBundle(ctData);
            CleverTapAPI.createNotification(getApplicationContext(), extras);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}