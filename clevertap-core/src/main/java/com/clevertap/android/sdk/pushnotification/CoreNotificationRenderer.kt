package com.clevertap.android.sdk.pushnotification

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.interfaces.AudibleNotification
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory.nullBitmapWithStatus
import org.json.JSONArray

@RestrictTo(RestrictTo.Scope.LIBRARY)
class CoreNotificationRenderer : INotificationRenderer, AudibleNotification {
    private var notifMessage: String? = null

    private var notifTitle: String? = null

    private var smallIcon = 0

    override fun getCollapseKey(extras: Bundle): Any? {
        return extras[Constants.WZRK_COLLAPSE]
    }

    override fun getMessage(extras: Bundle): String? {
        notifMessage = extras.getString(Constants.NOTIF_MSG)
        return notifMessage
    }

    override fun getTitle(extras: Bundle, context: Context): String? {
        val title = extras.getString(Constants.NOTIF_TITLE, "")
        notifTitle = title.ifEmpty { context.applicationInfo.name }
        return notifTitle
    }

    @SuppressLint("NotificationTrampoline")
    override fun renderNotification(
        extras: Bundle, context: Context,
        nb: NotificationCompat.Builder, config: CleverTapInstanceConfig, notificationId: Int
    ): NotificationCompat.Builder? {
        // uncommon - START

        var style: NotificationCompat.Style?
        val bigPictureUrl = extras.getString(Constants.WZRK_BIG_PICTURE)
        if (bigPictureUrl != null && bigPictureUrl.startsWith("http")) {
            var downloadedBitmap = nullBitmapWithStatus(DownloadedBitmap.Status.INIT_ERROR)
            try {
                downloadedBitmap = Utils.getNotificationBitmapWithTimeout(
                    bigPictureUrl,
                    false, context, config, Constants.PN_IMAGE_DOWNLOAD_TIMEOUT_IN_MILLIS
                )

                val bpMap = downloadedBitmap.bitmap
                    ?: throw Exception("Failed to fetch big picture!")

                val pift = downloadedBitmap.downloadTime
                config.logger
                    .verbose("Fetched big picture in $pift millis")

                extras.putString(Constants.WZRK_BPDS, downloadedBitmap.status.statusValue)

                if (extras.containsKey(Constants.WZRK_MSG_SUMMARY)) {
                    val summaryText = extras.getString(Constants.WZRK_MSG_SUMMARY)
                    style = NotificationCompat.BigPictureStyle()
                        .setSummaryText(summaryText)
                        .bigPicture(bpMap)
                } else {
                    style = NotificationCompat.BigPictureStyle()
                        .setSummaryText(notifMessage)
                        .bigPicture(bpMap)
                }
            } catch (t: Throwable) {
                style = NotificationCompat.BigTextStyle()
                    .bigText(notifMessage)
                extras.putString(Constants.WZRK_BPDS, downloadedBitmap.status.statusValue)
                config.logger
                    .verbose(
                        config.accountId,
                        "Falling back to big text notification, couldn't fetch big picture",
                        t
                    )
            }
        } else {
            style = NotificationCompat.BigTextStyle()
                .bigText(notifMessage)
            extras.putString(Constants.WZRK_BPDS, DownloadedBitmap.Status.NO_IMAGE.statusValue)
        }

        val requiresChannelId = Build.VERSION.SDK_INT >= VERSION_CODES.O
        if (requiresChannelId && extras.containsKey(Constants.WZRK_SUBTITLE)) {
            nb.setSubText(extras.getString(Constants.WZRK_SUBTITLE))
        }

        if (extras.containsKey(Constants.WZRK_COLOR)) {
            val color = Color.parseColor(extras.getString(Constants.WZRK_COLOR))
            nb.setColor(color)
            nb.setColorized(true)
        } // uncommon


        // uncommon
        nb.setContentTitle(notifTitle)
            .setContentText(notifMessage)
            .setContentIntent(LaunchPendingIntentFactory.getLaunchPendingIntent(extras, context))
            .setAutoCancel(true)
            .setStyle(style)
            .setSmallIcon(smallIcon)

        val icoPath = extras.getString(Constants.NOTIF_ICON) // uncommon
        val showIcon =
            !"true".equals(extras.getString(Constants.NOTIF_HIDE_APP_LARGE_ICON), ignoreCase = true)
        if (showIcon) {
            // uncommon
            nb.setLargeIcon(
                Utils.getNotificationBitmapWithTimeout(
                    icoPath, true, context,
                    config, Constants.PN_LARGE_ICON_DOWNLOAD_TIMEOUT_IN_MILLIS
                ).bitmap
            ) //uncommon
        }

        // Uncommon - START
        // add actions if any
        var actions: JSONArray? = null
        val actionsString = extras.getString(Constants.WZRK_ACTIONS)
        if (actionsString != null) {
            try {
                actions = JSONArray(actionsString)
            } catch (t: Throwable) {
                config.logger
                    .debug(
                        config.accountId,
                        "error parsing notification actions: " + t.localizedMessage
                    )
            }
        }

        val actionButtons = getActionButtons(context, extras, notificationId, actions)
        attachActionButtons(nb, actionButtons)
        return nb
    }


    override fun setSmallIcon(smallIcon: Int, context: Context) {
        this.smallIcon = smallIcon
    }

    override val actionButtonIconKey: String
        get() = Constants.NOTIF_ICON

    override fun setSound(
        context: Context, extras: Bundle, nb: NotificationCompat.Builder,
        config: CleverTapInstanceConfig
    ): NotificationCompat.Builder? {
        try {
            if (extras.containsKey(Constants.WZRK_SOUND)) {
                var soundUri: Uri? = null

                val o = extras[Constants.WZRK_SOUND]

                if ((o is Boolean && o)) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else if (o is String) {
                    var s = o
                    if (s == "true") {
                        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    } else if (s.isNotEmpty()) {
                        if (s.contains(".mp3") || s.contains(".ogg") || s.contains(".wav")) {
                            s = s.substring(0, (s.length - 4))
                        }
                        soundUri = Uri
                            .parse(
                                (ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName
                                        + "/raw/" + s)
                            )
                    }
                }

                if (soundUri != null) {
                    nb.setSound(soundUri)
                }
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "Could not process sound parameter", t)
        }

        return nb
    }
}
