package com.clevertap.android.sdk.pushnotification;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.RingtoneManager;
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
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.interfaces.AudibleNotification;
import org.json.JSONArray;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CoreNotificationRenderer implements INotificationRenderer, AudibleNotification {

    private String notifMessage;

    private String notifTitle;

    private int smallIcon;

    @Override
    public @Nullable Object getCollapseKey(final Bundle extras) {
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
        String icoPath = extras.getString(Constants.NOTIF_ICON);// uncommon

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

        boolean requiresChannelId = VERSION.SDK_INT >= VERSION_CODES.O;
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
                .setContentIntent(LaunchPendingIntentFactory.getLaunchPendingIntent(extras, context))
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

        setActionButtons(context,extras,notificationId,nb,actions);

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

    @Override
    public Builder setSound(final Context context, final Bundle extras, final Builder nb,CleverTapInstanceConfig config
            ) {
        try {
            if (extras.containsKey(Constants.WZRK_SOUND)) {
                Uri soundUri = null;

                Object o = extras.get(Constants.WZRK_SOUND);

                if ((o instanceof Boolean && (Boolean) o)) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                } else if (o instanceof String) {
                    String s = (String) o;
                    if (s.equals("true")) {
                        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    } else if (!s.isEmpty()) {
                        if (s.contains(".mp3") || s.contains(".ogg") || s.contains(".wav")) {
                            s = s.substring(0, (s.length() - 4));
                        }
                        soundUri = Uri
                                .parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName()
                                        + "/raw/" + s);

                    }
                }

                if (soundUri != null) {
                    nb.setSound(soundUri);
                }
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Could not process sound parameter", t);
        }

        return nb;
    }
}
