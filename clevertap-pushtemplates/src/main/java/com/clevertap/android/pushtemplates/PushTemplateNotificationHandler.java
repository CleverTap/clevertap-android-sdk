package com.clevertap.android.pushtemplates;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler;
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil;
import com.clevertap.android.sdk.validation.ManifestValidator;

import java.util.Objects;

public class PushTemplateNotificationHandler implements ActionButtonClickHandler {

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
            TemplateRenderer templateRenderer = new TemplateRenderer(applicationContext, message);
            if (shouldRenderTimerTemplateUsingFGS(applicationContext, templateRenderer.getTemplateType(), templateRenderer.getTimerShowTerminal(), templateRenderer.getTimerUseFGS())) {
                PTLog.debug("Starting service for Timer Template");
                Intent serviceIntent = new Intent(applicationContext, TimerTemplateService.class);
                serviceIntent.putExtras(message);
                ContextCompat.startForegroundService(applicationContext, serviceIntent);
            } else {
                CleverTapAPI cleverTapAPI = CleverTapAPI.getGlobalInstance(applicationContext, PushNotificationUtil.getAccountIdFromNotificationBundle(message));
                Objects.requireNonNull(cleverTapAPI).renderPushNotificationOnCallerThread(templateRenderer, applicationContext, message);
            }

        } catch (Throwable throwable) {
            PTLog.verbose("Error parsing FCM payload", throwable);
        }
        return true;
    }

    public boolean shouldRenderTimerTemplateUsingFGS(Context applicationContext, TemplateType templateType, boolean showTerminalNotification, boolean useFGS) {
        return templateType == TemplateType.TIMER
                && showTerminalNotification
                && useFGS
                && ManifestValidator.isComponentPresentInManifest(applicationContext, "com.clevertap.android.pushtemplates.TimerTemplateService", ManifestValidator.ComponentType.SERVICE)
                && areRevampedTimerTemplatePermissionsGranted(applicationContext);
    }

    public boolean areRevampedTimerTemplatePermissionsGranted(Context context) {
        boolean foregroundServicePermission = ContextCompat.checkSelfPermission(
                context,
                "android.permission.FOREGROUND_SERVICE"
        ) == PackageManager.PERMISSION_GRANTED;

        // FOREGROUND_SERVICE_REMOTE_MESSAGING is only required on API 34 and above
        boolean foregroundServiceRemoteMessagingPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || ContextCompat.checkSelfPermission(
                context,
                "android.permission.FOREGROUND_SERVICE_REMOTE_MESSAGING"
        ) == PackageManager.PERMISSION_GRANTED;

        return foregroundServicePermission && foregroundServiceRemoteMessagingPermission;
    }

    @Override
    public boolean onNewToken(final Context applicationContext, final String token, final String pushType) {
        return true;
    }
}