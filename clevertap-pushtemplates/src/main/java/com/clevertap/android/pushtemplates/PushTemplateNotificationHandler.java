package com.clevertap.android.pushtemplates;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler;
import com.clevertap.android.sdk.pushnotification.INotificationRenderer;
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil;
import java.util.Objects;

public class PushTemplateNotificationHandler implements ActionButtonClickHandler {

    @Override
    public boolean onActionButtonClick(final Context context, final Bundle extras, final int notificationId) {
        String actionID = extras.getString(PTConstants.PT_ACTION_ID);
        String dismissOnClick = extras.getString(PTConstants.PT_DISMISS_ON_CLICK);
        CleverTapInstanceConfig config = extras.getParcelable("config");

        if (dismissOnClick != null && dismissOnClick.equalsIgnoreCase("true")) {
            /**
             * For input box remind CTA,pt_dismiss_on_click must be true to raise event
             */
            if (actionID != null && actionID.contains("remind")) {
                Utils.raiseCleverTapEvent(context, config, extras);
            }
            Utils.cancelNotification(context, notificationId);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMessageReceived(final Context applicationContext, final Bundle message, final String pushType) {
        try {
            PTLog.debug("Inside Push Templates");
            // initial setup
            INotificationRenderer templateRenderer = new TemplateRenderer(applicationContext, message);
            CleverTapAPI cleverTapAPI = CleverTapAPI
                    .getGlobalInstance(applicationContext,
                            PushNotificationUtil.getAccountIdFromNotificationBundle(message));
            Objects.requireNonNull(cleverTapAPI)
                    .renderPushNotification(templateRenderer, applicationContext, message);

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