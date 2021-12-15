package com.clevertap.android.sdk.pushnotification.fcm;

import static com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.interfaces.INotificationParser;
import com.clevertap.android.sdk.interfaces.IPushAmpHandler;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler;
import com.google.firebase.messaging.RemoteMessage;

/**
 * implementation of {@link IFcmMessageHandler}
 */
public class CTFcmMessageHandler implements IFcmMessageHandler, IPushAmpHandler<RemoteMessage> {

    private final INotificationParser<RemoteMessage> mParser;

    public CTFcmMessageHandler() {
        this(new FcmNotificationParser());
    }

    CTFcmMessageHandler(final INotificationParser<RemoteMessage> parser) {
        mParser = parser;
    }

    @Override
    public boolean onMessageReceived(final Context context, final RemoteMessage message) {
        boolean isSuccess = false;

        Bundle messageBundle = mParser.toBundle(message);
        if (messageBundle != null) {
            isSuccess = PushNotificationHandler.getPushNotificationHandler()
                    .onMessageReceived(context, messageBundle, PushType.FCM.toString());
        }

        return isSuccess;
    }

    @Override
    public boolean onNewToken(final Context applicationContext, final String token) {
        boolean isSuccess = false;
        try {
            PushNotificationHandler.getPushNotificationHandler().onNewToken(applicationContext, token, PushType.FCM
                    .getType());

            Logger.d(LOG_TAG, FCM_LOG_TAG + "New token received from FCM - " + token);
            isSuccess = true;
        } catch (Throwable t) {
            // do nothing
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Error onNewToken", t);
        }
        return isSuccess;
    }


    @Override
    public void processPushAmp(final Context context, @NonNull final RemoteMessage message) {
        Bundle messageBundle = mParser.toBundle(message);
        if (messageBundle != null) {
            CleverTapAPI.processPushNotification(context, messageBundle);
        }
    }
}