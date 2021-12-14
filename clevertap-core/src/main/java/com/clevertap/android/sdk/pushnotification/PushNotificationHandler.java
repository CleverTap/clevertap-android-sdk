package com.clevertap.android.sdk.pushnotification;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.XPS;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler;
import com.clevertap.android.sdk.interfaces.NotificationHandler;

public class PushNotificationHandler implements ActionButtonClickHandler {

    private static class SingletonNotificationHandler {

        private final static PushNotificationHandler INSTANCE = new PushNotificationHandler();
    }

    public static NotificationHandler getPushNotificationHandler() {
        return SingletonNotificationHandler.INSTANCE;
    }

    public static boolean isForPushTemplates(Bundle extras) {
        if (extras == null) {
            return false;
        }
        String pt_id = extras.getString("pt_id");
        return !(("0").equals(pt_id) || pt_id == null || pt_id.isEmpty());
    }

    private PushNotificationHandler() {
        // NO-OP
    }

    @Override
    public boolean onActionButtonClick(final Context context, final Bundle extras, final int notificationId) {
        return false;
    }

    @Override
    public synchronized boolean onMessageReceived(final Context applicationContext, final Bundle message,
            final String pushType) {
        CleverTapAPI cleverTapAPI = CleverTapAPI
                .getGlobalInstance(applicationContext, getAccountIdFromNotificationBundle(message));
        NotificationInfo info = CleverTapAPI.getNotificationInfo(message);

        if (info.fromCleverTap) {
            if (cleverTapAPI != null) {
                cleverTapAPI.getCoreState().getConfig().log(LOG_TAG,
                        pushType + "received notification from CleverTap: " + message.toString());
                if (isForPushTemplates(message) && CleverTapAPI.getNotificationHandler() != null) {
                    // render push template
                    CleverTapAPI.getNotificationHandler().onMessageReceived(applicationContext, message, pushType);
                } else {
                    // render core push
                    cleverTapAPI.renderPushNotification(new CoreNotificationRenderer(), applicationContext, message);
                    //CleverTapAPI.createNotification(applicationContext, message);
                }
            } else {
                Logger.d(LOG_TAG, pushType + "received notification from CleverTap: " + message.toString());
                Logger.d(LOG_TAG, pushType + " not renderning since cleverTapAPI is null");
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onNewToken(final Context applicationContext, final String token, final String pushType) {
        if (pushType.equals(FCM.getType())) {
            CleverTapAPI.tokenRefresh(applicationContext, token, FCM);
        } else if (pushType.equals(HPS.getType())) {
            CleverTapAPI.tokenRefresh(applicationContext, token, HPS);
        } else if (pushType.equals(XPS.getType())) {
            CleverTapAPI.tokenRefresh(applicationContext, token, XPS);
        }
        return true;
    }
}
