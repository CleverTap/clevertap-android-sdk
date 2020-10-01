package com.clevertap.android.sdk.pushnotification.fcm;

import static com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * implementation of {@link IFcmMessageHandler}
 */
public class FcmMessageHandlerImpl implements IFcmMessageHandler {

    @Override
    public boolean onMessageReceived(final Context context, final RemoteMessage message) {
        boolean isSuccess = false;
        try {
            if (message.getData().size() > 0) {
                Bundle extras = new Bundle();
                for (Map.Entry<String, String> entry : message.getData().entrySet()) {
                    extras.putString(entry.getKey(), entry.getValue());
                }
                CleverTapAPI cleverTapAPI = CleverTapAPI
                        .getGlobalInstance(context, getAccountIdFromNotificationBundle(extras));
                NotificationInfo info = CleverTapAPI.getNotificationInfo(extras);

                if (info.fromCleverTap) {
                    if (cleverTapAPI != null) {
                        cleverTapAPI.config().log(LOG_TAG,
                                FCM_LOG_TAG + "received notification from CleverTap: " + extras.toString());
                    } else {
                        Logger.d(LOG_TAG, FCM_LOG_TAG + "received notification from CleverTap: " + extras.toString());
                    }
                    CleverTapAPI.createNotification(context, extras);
                    isSuccess = true;
                }
            }
        } catch (Throwable t) {
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Error parsing FCM message", t);
        }
        return isSuccess;
    }

    @Override
    public boolean onNewToken(final Context applicationContext, final String token) {
        boolean isSuccess = false;
        try {
            CleverTapAPI.tokenRefresh(applicationContext, token, PushConstants.PushType.FCM);
            Logger.d(LOG_TAG, FCM_LOG_TAG + "New token received from FCM - " + token);
            isSuccess = true;
        } catch (Throwable t) {
            // do nothing
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Error onNewToken", t);
        }
        return isSuccess;
    }
}