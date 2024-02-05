package com.clevertap.android.sdk.pushnotification;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.Utils;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public interface INotificationRenderer {

    @Nullable Object getCollapseKey(final Bundle extras);

    @Nullable String getMessage(Bundle extras);

    @Nullable String getTitle(Bundle extras, final Context context);

    @Nullable NotificationCompat.Builder renderNotification(final Bundle extras, final Context context,
            final Builder nb, final CleverTapInstanceConfig config, final int notificationId);

    void setSmallIcon(int smallIcon, final Context context);

    String getActionButtonIconKey();

    default NotificationCompat.Builder setActionButtons(
            Context context,
            Bundle extras,
            int notificationId,
            NotificationCompat.Builder nb, JSONArray actions
    ) {

        String intentServiceName = ManifestInfo.getInstance(context).getIntentServiceName();
        Class clazz = null;
        if (intentServiceName != null) {
            try {
                clazz = Class.forName(intentServiceName);
            } catch (ClassNotFoundException e) {
                try {
                    clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService");
                } catch (ClassNotFoundException ex) {
                    Logger.d("No Intent Service found");
                }
            }
        } else {
            try {
                clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService");
            } catch (ClassNotFoundException ex) {
                Logger.d("No Intent Service found");
            }
        }

        boolean isCTIntentServiceAvailable = Utils.isServiceAvailable(context, clazz);

        if (actions != null && actions.length() > 0) {
            for (int i = 0; i < actions.length(); i++) {
                try {
                    JSONObject action = actions.getJSONObject(i);
                    String label = action.optString("l");
                    String dl = action.optString("dl");
                    String ico = action.optString(getActionButtonIconKey());
                    String id = action.optString("id");
                    boolean autoCancel = action.optBoolean("ac", true);
                    if (label.isEmpty() || id.isEmpty()) {
                        Logger.d("not adding push notification action: action label or id missing");
                        continue;
                    }
                    int icon = 0;
                    if (!ico.isEmpty()) {
                        try {
                            icon = context.getResources().getIdentifier(ico, "drawable", context.getPackageName());
                        } catch (Throwable t) {
                           Logger.d("unable to add notification action icon: " + t.getLocalizedMessage());
                        }
                    }

                    boolean sendToCTIntentService = (VERSION.SDK_INT < VERSION_CODES.S && autoCancel
                            && isCTIntentServiceAvailable);

                    String dismissOnClick = extras.getString("pt_dismiss_on_click");
                    /**
                     * Send to CTIntentService in case (OS >= S) and notif is for Push templates with remind action
                     */
                    if (!sendToCTIntentService && PushNotificationHandler.isForPushTemplates(extras)
                            && id.contains("remind") && dismissOnClick!=null &&
                            dismissOnClick.equalsIgnoreCase("true") && autoCancel &&
                            isCTIntentServiceAvailable) {
                        sendToCTIntentService = true;
                    }

                    /**
                     * Send to CTIntentService in case (OS >= S) and notif is for Push templates with pt_dismiss_on_click
                     * true
                     */
                    if (!sendToCTIntentService && PushNotificationHandler.isForPushTemplates(extras)
                            && dismissOnClick!=null && dismissOnClick.equalsIgnoreCase("true")
                            && autoCancel && isCTIntentServiceAvailable) {
                        sendToCTIntentService = true;
                    }

                    Intent actionLaunchIntent;
                    if (sendToCTIntentService) {
                        actionLaunchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                        actionLaunchIntent.setPackage(context.getPackageName());
                        actionLaunchIntent.putExtra(Constants.KEY_CT_TYPE, CTNotificationIntentService.TYPE_BUTTON_CLICK);
                        if (!dl.isEmpty()) {
                            actionLaunchIntent.putExtra("dl", dl);
                        }
                    } else {
                        if (!dl.isEmpty()) {
                            actionLaunchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
                            Utils.setPackageNameFromResolveInfoList(context, actionLaunchIntent);
                        } else {
                            actionLaunchIntent = context.getPackageManager()
                                    .getLaunchIntentForPackage(context.getPackageName());
                        }
                    }

                    if (actionLaunchIntent != null) {
                        actionLaunchIntent.putExtras(extras);
                        actionLaunchIntent.removeExtra(Constants.WZRK_ACTIONS);
                        actionLaunchIntent.putExtra("actionId", id);
                        actionLaunchIntent.putExtra("autoCancel", autoCancel);
                        actionLaunchIntent.putExtra("wzrk_c2a", id);
                        actionLaunchIntent.putExtra("notificationId", notificationId);

                        actionLaunchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    }

                    PendingIntent actionIntent;
                    int requestCode = new Random().nextInt();
                    int flagsActionLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (VERSION.SDK_INT >= VERSION_CODES.M) {
                        flagsActionLaunchPendingIntent |= PendingIntent.FLAG_IMMUTABLE;
                    }
                    if (sendToCTIntentService) {
                        actionIntent = PendingIntent.getService(context, requestCode,
                                actionLaunchIntent, flagsActionLaunchPendingIntent);
                    } else {
                        Bundle optionsBundle = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            optionsBundle = ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle();
                        }
                        actionIntent = PendingIntent.getActivity(context, requestCode,
                                actionLaunchIntent, flagsActionLaunchPendingIntent, optionsBundle);
                    }
                    nb.addAction(icon, label, actionIntent);

                } catch (Throwable t) {
                    Logger.d("error adding notification action : " + t.getLocalizedMessage());
                }
            }
        }// Uncommon - END

        return nb;
    }
}
