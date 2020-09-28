package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.Utils.stringToBundle;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.huawei.hms.push.RemoteMessage;

class HmsMessageHandlerImpl implements IHmsMessageHandler {

    @Override
    public boolean createNotification(Context context, final RemoteMessage remoteMessage) {
        Logger.i(LOG_TAG, HMS_LOG_TAG + "onMessageReceived is called");
        boolean isSuccess = false;

        if (remoteMessage != null) {
            try {
                String ctData = remoteMessage.getData();
                Bundle extras = stringToBundle(ctData);
                NotificationInfo info = CleverTapAPI.getNotificationInfo(extras);
                CleverTapAPI cleverTapAPI = CleverTapAPI
                        .getGlobalInstance(context, getAccountIdFromNotificationBundle(extras));
                if (info.fromCleverTap) {
                    CleverTapAPI.createNotification(context, extras);
                    isSuccess = true;
                    if (cleverTapAPI != null) {
                        cleverTapAPI.config().log(LOG_TAG, HMS_LOG_TAG + "onMessageReceived: ");
                    } else {
                        Logger.d(LOG_TAG, HMS_LOG_TAG + "onMessageReceived: ");
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                Logger.d(LOG_TAG, HMS_LOG_TAG + "Error creatig notification ", e);
            }
        } else {
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Received message entity is null!");
        }
        return isSuccess;
    }

    @Override
    public boolean onNewToken(Context context, final String token) {
        boolean isSuccess = false;
        try {
            CleverTapAPI.tokenRefresh(context, token, HPS);
            Logger.d(LOG_TAG, HMS_LOG_TAG + "onNewToken: " + token);
            isSuccess = true;
        } catch (Throwable throwable) {
            // do nothing
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Error onNewToken: " + token, throwable);
        }
        return isSuccess;
    }
}