package com.clevertap.android.sdk.pushnotification;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.interfaces.AudibleNotification;

import com.clevertap.android.sdk.network.DownloadedBitmap;
import com.clevertap.android.sdk.network.DownloadedBitmap.Status;
import com.clevertap.android.sdk.network.DownloadedBitmapFactory;
import com.clevertap.android.sdk.utils.Clock;

import org.json.JSONArray;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CoreNotificationRenderer implements INotificationRenderer, AudibleNotification {

    private String notifMessage;

    private String notifTitle;

    private int smallIcon;

    @Override
    public @Nullable Object getCollapseKey(final Bundle extras) {
        return extras.get(Constants.WZRK_COLLAPSE);
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

    @RequiresApi(34)
    private Uri getNotificationGifUri(String gif, Context context, CleverTapInstanceConfig config) {
        return Utils.saveNotificationGif(gif, context, config, Clock.SYSTEM);
    }

    private DownloadedBitmap getNotificationImageBitmap(String bigPictureUrl, Context context, CleverTapInstanceConfig config) {
        if (bigPictureUrl == null || !bigPictureUrl.startsWith("http")) {
            return DownloadedBitmapFactory.INSTANCE.nullBitmapWithStatus(Status.NO_IMAGE);
        }
        DownloadedBitmap downloadedBitmap = DownloadedBitmapFactory.INSTANCE.nullBitmapWithStatus(Status.INIT_ERROR);
        try {
            downloadedBitmap = Utils.getNotificationBitmapWithTimeout(bigPictureUrl,
                    false, context, config, Constants.PN_IMAGE_DOWNLOAD_TIMEOUT_IN_MILLIS);
            if (downloadedBitmap.getBitmap() != null) {
                long pift = downloadedBitmap.getDownloadTime();
                config.getLogger()
                        .verbose("Fetched big picture in " + pift + " millis");
            }
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(),
                    "Falling back to big text notification, couldn't fetch big picture", t);
        }
        return downloadedBitmap;
    }

    private void addContentDescriptionIfNeeded(NotificationCompat.Style style,
                                               Bundle extras, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && style instanceof NotificationCompat.BigPictureStyle) {
            String altText = extras.getString(Constants.WZRK_BIG_PICTURE_ALT_TEXT_KEY,
                    context.getString(R.string.ct_notification_big_picture_alt_text));
            ((NotificationCompat.BigPictureStyle) style).setContentDescription(altText);
        }
    }

    private NotificationCompat.Style generateStyle(final Bundle extras, final Context context, final CleverTapInstanceConfig config) {

        String bigPictureUrl = extras.getString(Constants.WZRK_BIG_PICTURE);
        String gifUrl = extras.getString(Constants.WZRK_GIF);
        String summaryText = extras.getString(Constants.WZRK_MSG_SUMMARY, notifMessage);

        NotificationCompat.Style style;

        try {
            // 1. Try GIF if on Android 14+ and valid URL
            if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE && gifUrl != null && gifUrl.startsWith("http")) {
                Uri gifUri = getNotificationGifUri(gifUrl, context, config);
                if (gifUri != null) {
                    style = new NotificationCompat.BigPictureStyle()
                            .setSummaryText(summaryText)
                            .bigPicture(Icon.createWithContentUri(gifUri));
                    addContentDescriptionIfNeeded(style, extras, context);
                    extras.putString(Constants.WZRK_BPDS, Status.GIF_SUCCESS.getStatusValue());
                    return style;
                }
            }
        } catch (Exception e) {
            config.getLogger().verbose(config.getAccountId(), "Failed to load GIF, falling back to static big-picture", e);
        }

        // 2. Try image fallback
        DownloadedBitmap downloadedBitmap = getNotificationImageBitmap(bigPictureUrl, context, config);
        extras.putString(Constants.WZRK_BPDS, downloadedBitmap.getStatus().getStatusValue());

        try {
            Bitmap bitmap = downloadedBitmap.getBitmap();
            if (bitmap != null) {
                style = new NotificationCompat.BigPictureStyle()
                        .setSummaryText(summaryText)
                        .bigPicture(bitmap);
                addContentDescriptionIfNeeded(style, extras, context);
                return style;
            }
        } catch (Exception e) {
            config.getLogger().verbose(config.getAccountId(), "Failed to load Big Picture, falling back to text notification", e);
        }

        // Fallback to message only
        style = new NotificationCompat.BigTextStyle().bigText(notifMessage);
        return style;
    }

    @Override
    public Builder renderNotification(final Bundle extras, final Context context,
                                      final Builder nb, final CleverTapInstanceConfig config, final int notificationId) {
        NotificationCompat.Style style = generateStyle(extras, context, config);
        addActions(extras, context, nb, config, notificationId);
        return finalizeBuilder(nb, extras, context, config, style);
    }

    public void addActions(Bundle extras, Context context, Builder nb, CleverTapInstanceConfig config, int notificationId) {
        JSONArray actions;
        String actionsString = extras.getString(Constants.WZRK_ACTIONS);
        if (actionsString != null) {
            try {
                actions = new JSONArray(actionsString);
                setActionButtons(context, extras, notificationId, nb, actions);
            } catch (Throwable t) {
                config.getLogger()
                        .debug(config.getAccountId(),
                                "error parsing notification actions: " + t.getLocalizedMessage());
            }
        }
    }

    @SuppressLint("NotificationTrampoline")
    private Builder finalizeBuilder(Builder nb, Bundle extras, Context context,
                                    CleverTapInstanceConfig config, NotificationCompat.Style style) {

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

        String icoPath = extras.getString(Constants.NOTIF_ICON);// uncommon
        boolean showIcon = !"true".equalsIgnoreCase(extras.getString(Constants.NOTIF_HIDE_APP_LARGE_ICON));
        if (showIcon) {
            // uncommon
            nb.setLargeIcon(Utils.getNotificationBitmapWithTimeout(icoPath, true, context,
                    config, Constants.PN_LARGE_ICON_DOWNLOAD_TIMEOUT_IN_MILLIS).getBitmap());//uncommon
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

    @Override
    public Builder setSound(final Context context, final Bundle extras, final Builder nb,
            CleverTapInstanceConfig config
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
