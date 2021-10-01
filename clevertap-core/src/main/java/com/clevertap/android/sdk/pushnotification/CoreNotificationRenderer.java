package com.clevertap.android.sdk.pushnotification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CoreNotificationRenderer implements INotificationRenderer {

    private String notifMessage;

    private String notifTitle;

    private int smallIcon;

    @Override
    public Object getCollapseKey(final Bundle extras) {
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
    public NotificationCompat.Builder renderNotification(final Bundle extras, final Context context,
            final Builder nb, final CleverTapInstanceConfig config, final int notificationId) {
        String icoPath = extras.getString(Constants.NOTIF_ICON);// uncommon
        Intent launchIntent = new Intent(context, CTPushNotificationReceiver.class);// uncommon 1

        PendingIntent pIntent;

        // Take all the properties from the notif and add it to the intent
        launchIntent.putExtras(extras);// u1
        launchIntent.removeExtra(Constants.WZRK_ACTIONS);//u1
        pIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);//u1

        // uncommon - START
        NotificationCompat.Style style;
        String bigPictureUrl = extras.getString(Constants.WZRK_BIG_PICTURE);
        if (bigPictureUrl != null && bigPictureUrl.startsWith("http")) {
            try {
                Bitmap bpMap = Utils.getNotificationBitmap(bigPictureUrl, false, context);

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
                                "Falling back to big text notification, couldn't fetch big picture",
                                t);
            }
        } else {
            style = new NotificationCompat.BigTextStyle()
                    .bigText(notifMessage);
        }

        boolean requiresChannelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        if (requiresChannelId && extras.containsKey(Constants.WZRK_SUBTITLE)) {
            nb.setSubText(extras.getString(Constants.WZRK_SUBTITLE));
        }

        if (extras.containsKey(Constants.WZRK_COLOR)) {
            int color = Color.parseColor(extras.getString(Constants.WZRK_COLOR));
            nb.setColor(color);
            nb.setColorized(true);
        }// uncommon

        // uncommon
        nb.setContentTitle(notifTitle)
                .setContentText(notifMessage)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setStyle(style)
                .setSmallIcon(smallIcon);

        // uncommon
        nb.setLargeIcon(Utils.getNotificationBitmap(icoPath, true, context));//uncommon

        // Uncommon - START
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
                    String ico = action.optString(Constants.NOTIF_ICON);
                    String id = action.optString("id");
                    boolean autoCancel = action.optBoolean("ac", true);
                    if (label.isEmpty() || id.isEmpty()) {
                        config.getLogger().debug(config.getAccountId(),
                                "not adding push notification action: action label or id missing");
                        continue;
                    }
                    int icon = 0;
                    if (!ico.isEmpty()) {
                        try {
                            icon = context.getResources().getIdentifier(ico, "drawable", context.getPackageName());
                        } catch (Throwable t) {
                            config.getLogger().debug(config.getAccountId(),
                                    "unable to add notification action icon: " + t.getLocalizedMessage());
                        }
                    }

                    boolean sendToCTIntentService = (autoCancel && isCTIntentServiceAvailable);

                    Intent actionLaunchIntent;
                    if (sendToCTIntentService) {
                        actionLaunchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                        actionLaunchIntent.setPackage(context.getPackageName());
                        actionLaunchIntent.putExtra("ct_type", CTNotificationIntentService.TYPE_BUTTON_CLICK);
                        if (!dl.isEmpty()) {
                            actionLaunchIntent.putExtra("dl", dl);
                        }
                    } else {
                        if (!dl.isEmpty()) {
                            actionLaunchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
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
                    if (sendToCTIntentService) {
                        actionIntent = PendingIntent.getService(context, requestCode,
                                actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    } else {
                        actionIntent = PendingIntent.getActivity(context, requestCode,
                                actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }
                    nb.addAction(icon, label, actionIntent);

                } catch (Throwable t) {
                    config.getLogger()
                            .debug(config.getAccountId(),
                                    "error adding notification action : " + t.getLocalizedMessage());
                }
            }
        }// Uncommon - END

        return nb;

    }

    @Override
    public void setSmallIcon(final int smallIcon, final Context context) {
        this.smallIcon = smallIcon;
    }

}
