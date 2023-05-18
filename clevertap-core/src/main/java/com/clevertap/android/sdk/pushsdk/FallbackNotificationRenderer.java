package com.clevertap.android.sdk.pushsdk;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService;
import com.clevertap.android.sdk.pushnotification.INotificationRenderer;
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory;
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Implementation of {@link INotificationRenderer} responsible for rendering basic notification elements.
 * Basic notification elements rendering will improve execution time to render.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class FallbackNotificationRenderer implements INotificationRenderer {

    private String notifMessage;

    private String notifTitle;

    private int smallIcon;

    @Override
    public @Nullable
    Object getCollapseKey(final Bundle extras) {
        Object collapse_key = extras.get(Constants.WZRK_COLLAPSE);
        return collapse_key;
    }

    @Override
    public String getMessage(final Bundle extras) {
        notifMessage = extras.getString(Constants.NOTIF_MSG);
        return notifMessage;
    }

    @Override
    public String getTitle(final Bundle extras, final Context context) {
        String title = extras.getString(Constants.NOTIF_TITLE, "");
        notifTitle = title.isEmpty() ? context.getApplicationInfo().name : title;
        return notifTitle;
    }

    @Override
    public Builder renderNotification(final Bundle extras, final Context context,
            final Builder nb, final CleverTapInstanceConfig config, final int notificationId) {
        String icoPath = extras.getString(Constants.NOTIF_ICON);

        NotificationCompat.Style style;
        String bigPictureUrl = extras.getString(Constants.WZRK_BIG_PICTURE);
        if (bigPictureUrl != null && bigPictureUrl.startsWith("http")) {
            try {
                Bitmap bpMap = Utils.getNotificationBitmapWithTimeoutAndSize(bigPictureUrl, false, context, config,
                        3000, 2097152);//2MB

                if (bpMap == null) {
                    throw new Exception("Failed to fetch big picture!");
                }

                if (extras.containsKey(Constants.WZRK_MSG_SUMMARY)) {
                    String summaryText = extras.getString(Constants.WZRK_MSG_SUMMARY);
                    style = new NotificationCompat.BigPictureStyle()
                            .setSummaryText(summaryText)
                            .bigPicture(bpMap);
                } else {
                    style = new NotificationCompat.BigPictureStyle()
                            .setSummaryText(notifMessage)
                            .bigPicture(bpMap);
                }
            } catch (Throwable t) {
                style = new NotificationCompat.BigTextStyle()
                        .bigText(notifMessage);
                config.getLogger()
                        .verbose(config.getAccountId(),
                                "FallbackNotificationRenderer :: Falling back to big text notification, couldn't fetch big picture",
                                t);
            }
        } else {
            style = new NotificationCompat.BigTextStyle()
                    .bigText(notifMessage);
        }

        boolean requiresChannelId = VERSION.SDK_INT >= VERSION_CODES.O;
        if (requiresChannelId && extras.containsKey(Constants.WZRK_SUBTITLE)) {
            nb.setSubText(extras.getString(Constants.WZRK_SUBTITLE));
        }

        if (extras.containsKey(Constants.WZRK_COLOR)) {
            int color = Color.parseColor(extras.getString(Constants.WZRK_COLOR));
            nb.setColor(color);
            nb.setColorized(true);
        }

        nb.setContentTitle(notifTitle)
                .setContentText(notifMessage)
                .setContentIntent(LaunchPendingIntentFactory.getLaunchPendingIntent(extras, context))
                .setAutoCancel(true)
                .setStyle(style)
                .setSmallIcon(smallIcon);

        nb.setLargeIcon(Utils.getNotificationBitmap(icoPath, true, context));//uncommon

        // add actions if any
        JSONArray actions = null;
        String actionsString = extras.getString(Constants.WZRK_ACTIONS);
        if (actionsString != null) {
            try {
                actions = new JSONArray(actionsString);
            } catch (Throwable t) {
                config.getLogger()
                        .debug(config.getAccountId(),
                                "error parsing notification actions: " + t.getLocalizedMessage());
            }
        }

        setActionButtons(context, extras, notificationId, nb, actions);

        return nb;

    }

    @Override
    public Builder setActionButtons(final Context context, final Bundle extras, final int notificationId,
            final Builder nb,
            final JSONArray actions) {
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
                    int requestCode = ((int) System.currentTimeMillis()) + i;
                    int flagsActionLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (VERSION.SDK_INT >= VERSION_CODES.M) {
                        flagsActionLaunchPendingIntent |= PendingIntent.FLAG_IMMUTABLE;
                    }
                    if (sendToCTIntentService) {
                        actionIntent = PendingIntent.getService(context, requestCode,
                                actionLaunchIntent, flagsActionLaunchPendingIntent);
                    } else {
                        actionIntent = PendingIntent.getActivity(context, requestCode,
                                actionLaunchIntent, flagsActionLaunchPendingIntent);
                    }
                    nb.addAction(icon, label, actionIntent);

                } catch (Throwable t) {
                    Logger.d("error adding notification action : " + t.getLocalizedMessage());
                }
            }
        }

        return nb;
    }

    @Override
    public void setSmallIcon(final int smallIcon, final Context context) {
        this.smallIcon = smallIcon;
    }

    @Override
    public String getActionButtonIconKey() {
        return Constants.NOTIF_ICON;
    }
}
