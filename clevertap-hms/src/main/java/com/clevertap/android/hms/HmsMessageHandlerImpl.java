package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.huawei.hms.push.RemoteMessage;

/**
 * Implementation of {@link IHmsMessageHandler}
 */
class HmsMessageHandlerImpl implements IHmsMessageHandler {

    private IHmsNotificationParser mParser;

    HmsMessageHandlerImpl(final IHmsNotificationParser parser) {
        mParser = parser;
    }

    @Override
    public boolean createNotification(Context context, final RemoteMessage remoteMessage) {
        boolean isSuccess = false;
        Bundle messageBundle = mParser.toBundle(remoteMessage);
        if (messageBundle != null) {
            try {
                createNotificationWithMessageBundle(context, messageBundle);
                isSuccess = true;
            } catch (Throwable e) {
                e.printStackTrace();
                Logger.d(LOG_TAG, HMS_LOG_TAG + "Error Creating Notification", e);
            }
        }
        return isSuccess;
    }

    void createNotificationWithMessageBundle(final Context context, final Bundle messageBundle) {

        CleverTapAPI cleverTapAPI = CleverTapAPI
                .getGlobalInstance(context, getAccountIdFromNotificationBundle(messageBundle));
        CleverTapAPI.createNotification(context, messageBundle);
        if (cleverTapAPI != null) {
            cleverTapAPI.config().log(LOG_TAG, HMS_LOG_TAG + "Creating Notification");
        } else {
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Creating Notification");
        }
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