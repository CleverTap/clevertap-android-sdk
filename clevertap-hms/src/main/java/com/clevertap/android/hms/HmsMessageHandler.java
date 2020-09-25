package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.LOG_TAG;
import static com.clevertap.android.sdk.Utils.stringToBundle;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.clevertap.android.sdk.CleverTapAPI;
import com.huawei.hms.push.RemoteMessage;

class HmsMessageHandler implements IHmsMessageHandler {

    @Override
    public boolean createNotification(Context context, final RemoteMessage remoteMessage) {
        Log.i(LOG_TAG, "onMessageReceived is called");
        boolean isSuccess = false;

        if (remoteMessage != null) {
            try {
                String ctData = remoteMessage.getData();
                Bundle extras = stringToBundle(ctData);
                CleverTapAPI.createNotification(context, extras);
                isSuccess = true;
            } catch (Throwable e) {
                e.printStackTrace();
                isSuccess = false;
            }
        } else {
            Log.e(LOG_TAG, "Received message entity is null!");
        }
        return isSuccess;
    }

    @Override
    public boolean onNewToken(Context context, final String token) {
        boolean isSuccess;
        try {
            CleverTapAPI.tokenRefresh(context, token, HPS);
            isSuccess = true;
        } catch (Throwable throwable) {
            isSuccess = false;
        }
        return isSuccess;
    }
}