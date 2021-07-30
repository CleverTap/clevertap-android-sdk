package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler;
import com.huawei.hms.push.RemoteMessage;

/**
 * Implementation of {@link IHmsMessageHandler}
 */
public class HmsMessageHandlerImpl implements IHmsMessageHandler {

    private final IHmsNotificationParser mParser;

    public HmsMessageHandlerImpl() {
        this(new HmsNotificationParser());
    }

    HmsMessageHandlerImpl(final IHmsNotificationParser parser) {
        mParser = parser;
    }

    @Override
    public boolean createNotification(Context context, final RemoteMessage remoteMessage) {
        boolean isSuccess = false;
        Bundle messageBundle = mParser.toBundle(remoteMessage);
        if (messageBundle != null) {
            try {
                isSuccess = PushNotificationHandler
                        .getPushNotificationHandler().onMessageReceived(context, messageBundle, HPS.toString());
            } catch (Throwable e) {
                e.printStackTrace();
                Logger.d(LOG_TAG, HMS_LOG_TAG + "Error Creating Notification", e);
            }
        }
        return isSuccess;
    }

    @Override
    public boolean onNewToken(Context context, final String token) {
        boolean isSuccess = false;
        try {
            PushNotificationHandler.getPushNotificationHandler().onNewToken(context, token, PushType.HPS
                    .getType());
            Logger.d(LOG_TAG, HMS_LOG_TAG + "onNewToken: " + token);
            isSuccess = true;
        } catch (Throwable throwable) {
            // do nothing
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Error onNewToken: " + token, throwable);
        }
        return isSuccess;
    }
}