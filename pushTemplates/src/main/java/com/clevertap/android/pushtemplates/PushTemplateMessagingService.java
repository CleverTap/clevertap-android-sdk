package com.clevertap.android.pushtemplates;

import android.content.Context;
import android.os.Bundle;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.interfaces.NotificationHandler;
import com.clevertap.android.sdk.pushnotification.INotificationRenderer;
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil;

import java.util.Objects;

public class PushTemplateMessagingService implements NotificationHandler {

    @Override
    public boolean onMessageReceived(final Context applicationContext, final Bundle message, final String pushType) {
        try {
            PTLog.debug("Inside Push Templates");
            //TemplateRenderer.createNotification(applicationContext, message);
            // initial setup
            INotificationRenderer templateRenderer = new TemplateRenderer(applicationContext, message);
            CleverTapAPI cleverTapAPI = CleverTapAPI
                    .getGlobalInstance(applicationContext, PushNotificationUtil.getAccountIdFromNotificationBundle(message));
            Objects.requireNonNull(cleverTapAPI).renderPushNotification(templateRenderer,applicationContext,message);

        } catch (Throwable throwable) {
            PTLog.verbose("Error parsing FCM payload", throwable);
        }
        return true;
    }

    @Override
    public boolean onNewToken(final Context applicationContext, final String token, final String pushType) {
        return true;
    }

}