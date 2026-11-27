package com.clevertap.android.sdk.pushnotification;

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

/**
 * Renders a CleverTap push notification. Pass an implementation to
 * {@code CleverTapAPI.renderPushNotification}.
 * <p>
 * The SDK calls the methods below while it builds the notification, holding its push rendering
 * lock, on a background thread: a CleverTap executor thread for
 * {@code CleverTapAPI.renderPushNotification}, or the thread that delivered the push (for example
 * the FCM service thread) when the SDK renders a received push itself.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameters as platform types, so customer
 * renderers keep compiling with the override signatures they used before, and
 * {@link #setActionButtons} stays a plain Java default method. Do not convert this file to Kotlin.
 */
public interface INotificationRenderer {

    /**
     * Returns the collapse key, used to derive the notification id when the payload sets none. A
     * {@link Number} is used as is; a {@link String} is parsed as an integer, or its hash code is used.
     *
     * @param extras the push payload
     * @return the collapse key, or {@code null} to let the SDK pick a random id
     */
    @Nullable
    Object getCollapseKey(final Bundle extras);

    /**
     * Returns the notification message. An empty or {@code null} message makes the SDK treat the push
     * as silent and not render it (except for Live Updates rendered by a non-core renderer).
     *
     * @param extras the push payload
     * @return the message, or {@code null} if there is none
     */
    @Nullable
    String getMessage(Bundle extras);

    /**
     * Returns the notification title.
     *
     * @param extras  the push payload
     * @param context a context, for example to read the app name as a fallback
     * @return the title, or {@code null} if there is none
     */
    @Nullable
    String getTitle(Bundle extras, final Context context);

    /**
     * Applies the content of the push to the notification builder. The SDK has already created
     * {@code nb} for the notification channel and set its priority (and badge and sound where
     * applicable); it posts the returned builder's notification under {@code notificationId}.
     *
     * @param extras         the push payload
     * @param context        a context
     * @param nb             the builder prepared by the SDK
     * @param config         the configuration of the CleverTap instance rendering the push
     * @param notificationId the id the notification will be posted with
     * @return the builder to post, or {@code null} to not show a notification
     */
    @Nullable
    NotificationCompat.Builder renderNotification(final Bundle extras, final Context context,
            final Builder nb, final CleverTapInstanceConfig config, final int notificationId);

    /**
     * Receives the small icon the SDK resolved: the manifest's CleverTap notification icon, or the app
     * icon. Implementations keep it to use in {@link #renderNotification}.
     *
     * @param smallIcon the drawable resource id
     * @param context   a context
     */
    void setSmallIcon(int smallIcon, final Context context);

    /**
     * Returns the key under which each action button object in the payload stores its icon name.
     *
     * @return the icon key, for example {@code ico}
     */
    String getActionButtonIconKey();

    /**
     * Adds the action buttons described by {@code actions} to {@code nb}. Each action is a JSON object
     * with a label ({@code l}), id ({@code id}), optional deep link ({@code dl}), optional icon (key
     * {@link #getActionButtonIconKey()}) and auto-cancel flag ({@code ac}, default {@code true});
     * actions without a label or id are skipped. Clicks are routed through
     * {@code CTNotificationIntentService} when it is available and needed, otherwise straight to the
     * deep link or the app's launch activity.
     *
     * @param context        a context; must not be {@code null}
     * @param extras         the push payload, copied into each action's intent
     * @param notificationId the id of the notification the buttons belong to
     * @param nb             the builder to add the actions to
     * @param actions        the action button JSON objects, or {@code null} for none
     * @return {@code nb}
     */
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
                    // Send to CTIntentService in case (OS >= S) and notif is for Push templates with
                    // pt_dismiss_on_click true (this includes the remind action)
                    if (!sendToCTIntentService && PushNotificationHandler.isForPushTemplates(extras)
                            && dismissOnClick != null && dismissOnClick.equalsIgnoreCase("true")
                            && autoCancel && isCTIntentServiceAvailable) {
                        sendToCTIntentService = true;
                    }

                    Intent actionLaunchIntent;
                    if (sendToCTIntentService) {
                        actionLaunchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                        actionLaunchIntent.setPackage(context.getPackageName());
                        actionLaunchIntent.putExtra(Constants.KEY_CT_TYPE,
                                CTNotificationIntentService.TYPE_BUTTON_CLICK);
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

                    int flagsActionLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE;

                    PendingIntent actionIntent;
                    int requestCode = new Random().nextInt();
                    if (sendToCTIntentService) {
                        actionIntent = PendingIntent.getService(context, requestCode,
                                actionLaunchIntent, flagsActionLaunchPendingIntent);
                    } else {
                        actionIntent = PendingIntent.getActivity(context, requestCode,
                                actionLaunchIntent, flagsActionLaunchPendingIntent, null);
                    }
                    nb.addAction(icon, label, actionIntent);

                } catch (Throwable t) {
                    Logger.d("error adding notification action : " + t.getLocalizedMessage());
                }
            }
        }

        return nb;
    }
}
