package com.clevertap.android.pushtemplates;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler;
import com.clevertap.android.sdk.pushnotification.INotificationRenderer;
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil;
import com.clevertap.android.sdk.pushnotification.PushType;
import java.util.HashMap;

public class PushTemplateNotificationHandler implements ActionButtonClickHandler {

    public PushTemplateNotificationHandler() {
        // Covers apps that create the instance before setting this handler
        try {
            HashMap<String, CleverTapAPI> instances = CleverTapAPI.getInstances();
            if (instances != null) {
                for (CleverTapAPI instance : instances.values()) {
                    registerSdkVersion(instance);
                }
            }
        } catch (Throwable t) {
            PTLog.verbose("Unable to register Push Templates SDK version", t);
        }
    }

    @Override
    public boolean onActionButtonClick(final Context context, final Bundle extras, final int notificationId) {
        String actionID = extras.getString(PTConstants.PT_ACTION_ID);
        String dismissOnClick = extras.getString(PTConstants.PT_DISMISS_ON_CLICK);
        CleverTapInstanceConfig config = extras.getParcelable("config");

        if (dismissOnClick != null && dismissOnClick.equalsIgnoreCase("true")) {

            // For input box remind CTA,pt_dismiss_on_click must be true to raise event
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
            CleverTapAPI cleverTapAPI = CleverTapAPI
                    .getGlobalInstance(applicationContext,
                            PushNotificationUtil.getAccountIdFromNotificationBundle(message));
            if (cleverTapAPI != null) {
                // Covers instances created after this handler was set
                registerSdkVersion(cleverTapAPI);
                INotificationRenderer templateRenderer = new TemplateRenderer(applicationContext, message, cleverTapAPI.getCoreState().getConfig());
                cleverTapAPI.renderPushNotificationOnCallerThread(templateRenderer, applicationContext, message);
            }
        } catch (Throwable throwable) {
            PTLog.verbose("Error parsing FCM payload", throwable);
        }
        return true;
    }

    @Override
    public boolean onNewToken(final Context applicationContext, final String token, final PushType pushType) {
        return true;
    }

    static void registerSdkVersion(CleverTapAPI instance) {
        if (instance == null) {
            return;
        }
        try {
            instance.setCustomSdkVersion(PTConstants.PT_SDK_NAME, BuildConfig.VERSION_CODE);
        } catch (Throwable t) {
            PTLog.verbose("Unable to register Push Templates SDK version", t);
        }
    }

}