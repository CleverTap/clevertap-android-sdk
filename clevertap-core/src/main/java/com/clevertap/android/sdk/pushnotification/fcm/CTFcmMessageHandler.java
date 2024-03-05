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
 * implementation of {@link IFcmMessageHandler} and {@link IPushAmpHandler} for Firebase notification message
 */
public class CTFcmMessageHandler implements IFcmMessageHandler, IPushAmpHandler<RemoteMessage> {

    private final INotificationParser<RemoteMessage> mParser;

    public CTFcmMessageHandler() {
        this(new FcmNotificationParser());
    }

    CTFcmMessageHandler(final INotificationParser<RemoteMessage> parser) {
        mParser = parser;
    }

    /**
     * {@inheritDoc}
     * <br><br>
     * Use this method if you have custom implementation of messaging service and wants to create push-template
     * notification/non push-template notification using CleverTap
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: Starting from v5.1.0, this method runs on the caller's thread. Make sure to call it
     * in onMessageReceive() of messaging service.
     * </p>
     */
    @Override
    public boolean createNotification(final Context context, final RemoteMessage message) {
        /**
         * Convert firebase message to bundle and pass to PushNotificationHandler for further processing
         */
        boolean isSuccess = false;

        Bundle messageBundle = mParser.toBundle(message);
        if (messageBundle != null) {
            /**
             * Analytics: If FCM alters original priority of a notification
             */
            messageBundle = new FcmNotificationBundleManipulation(messageBundle).addPriority(message).build();

            isSuccess = PushNotificationHandler.getPushNotificationHandler()
                    .onMessageReceived(context, messageBundle, PushType.FCM.toString());
        }

        return isSuccess;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     * <br><br>
     * Use this method if you are rendering notification by your own and wants to support your custom rendered
     * notification for Pull Notifications
     */
    @Override
    public void processPushAmp(final Context context, @NonNull final RemoteMessage message) {
        Bundle messageBundle = mParser.toBundle(message);
        if (messageBundle != null) {
            CleverTapAPI.processPushNotification(context, messageBundle);
        }
    }
}