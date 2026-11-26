package com.clevertap.android.sdk.pushnotification

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.interfaces.AudibleNotification
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray

internal open class CoreNotificationRenderer : INotificationRenderer, AudibleNotification {

    private var notifMessage: String? = null

    private var notifTitle: String? = null

    private var smallIconRes = 0

    @Suppress("DEPRECATION")
    override fun getCollapseKey(extras: Bundle): Any? = extras.get(Constants.WZRK_COLLAPSE)

    override fun getMessage(extras: Bundle): String? =
        extras.getString(Constants.NOTIF_MSG).also { notifMessage = it }

    override fun getTitle(extras: Bundle, context: Context): String? {
        val title = extras.getString(Constants.NOTIF_TITLE, "")
        return (if (title.isEmpty()) context.applicationInfo.name else title).also { notifTitle = it }
    }

    @RequiresApi(34)
    private fun notificationGifUri(gif: String, context: Context, config: CleverTapInstanceConfig): Uri? =
        Utils.getNotificationGifURI(gif, context, config, Clock.SYSTEM)

    private fun downloadBigPicture(
        bigPictureUrl: String?,
        context: Context,
        config: CleverTapInstanceConfig
    ): DownloadedBitmap {
        if (bigPictureUrl == null || !bigPictureUrl.startsWith("http")) {
            return DownloadedBitmapFactory.nullBitmapWithStatus(Status.NO_IMAGE)
        }
        var downloadedBitmap = DownloadedBitmapFactory.nullBitmapWithStatus(Status.INIT_ERROR)
        try {
            downloadedBitmap = Utils.getNotificationBitmapWithTimeout(
                bigPictureUrl,
                false, context, config, Constants.PN_IMAGE_DOWNLOAD_TIMEOUT_IN_MILLIS
            )
            if (downloadedBitmap.bitmap != null) {
                config.logger.verbose("Fetched big picture in ${downloadedBitmap.downloadTime} millis")
            }
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                "Falling back to big text notification, couldn't fetch big picture", t
            )
        }
        return downloadedBitmap
    }

    private fun addContentDescriptionIfNeeded(
        style: NotificationCompat.Style,
        extras: Bundle, context: Context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && style is NotificationCompat.BigPictureStyle) {
            val altText = extras.getString(
                Constants.WZRK_BIG_PICTURE_ALT_TEXT_KEY,
                context.getString(R.string.ct_notification_big_picture_alt_text)
            )
            style.setContentDescription(altText)
        }
    }

    private fun generateStyle(
        extras: Bundle,
        context: Context,
        config: CleverTapInstanceConfig
    ): NotificationCompat.Style {

        val bigPictureUrl = extras.getString(Constants.WZRK_BIG_PICTURE)
        val gifUrl = extras.getString(Constants.WZRK_GIF)
        val summaryText = extras.getString(Constants.WZRK_MSG_SUMMARY, notifMessage)

        try {
            // 1. Try GIF if on Android 14+ and valid URL
            if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE && gifUrl != null && gifUrl.startsWith("http")) {
                val gifUri = notificationGifUri(gifUrl, context, config)
                if (gifUri != null) {
                    val style = NotificationCompat.BigPictureStyle()
                        .setSummaryText(summaryText)
                        .bigPicture(Icon.createWithContentUri(gifUri))
                    addContentDescriptionIfNeeded(style, extras, context)
                    extras.putString(Constants.WZRK_BPDS, Status.GIF_SUCCESS.statusValue)
                    return style
                }
            }
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Failed to load GIF, falling back to static big-picture", e)
        }

        // 2. Try image fallback
        val downloadedBitmap = downloadBigPicture(bigPictureUrl, context, config)
        extras.putString(Constants.WZRK_BPDS, downloadedBitmap.status.statusValue)
        if (downloadedBitmap.failureReason != null) {
            extras.putString(Constants.WZRK_BPDS_REASON, downloadedBitmap.failureReason)
        }

        try {
            val bitmap = downloadedBitmap.bitmap
            if (bitmap != null) {
                val style = NotificationCompat.BigPictureStyle()
                    .setSummaryText(summaryText)
                    .bigPicture(bitmap)
                addContentDescriptionIfNeeded(style, extras, context)
                return style
            }
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Failed to load Big Picture, falling back to text notification", e)
        }

        // Fallback to message only
        return NotificationCompat.BigTextStyle().bigText(notifMessage)
    }

    override fun renderNotification(
        extras: Bundle, context: Context,
        nb: Builder, config: CleverTapInstanceConfig, notificationId: Int
    ): Builder? {
        val style = generateStyle(extras, context, config)
        addActions(extras, context, nb, config, notificationId)
        return finalizeBuilder(nb, extras, context, config, style)
    }

    open fun addActions(
        extras: Bundle,
        context: Context,
        nb: Builder,
        config: CleverTapInstanceConfig,
        notificationId: Int
    ) {
        val actionsString = extras.getString(Constants.WZRK_ACTIONS) ?: return
        try {
            setActionButtons(context, extras, notificationId, nb, JSONArray(actionsString))
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "error parsing notification actions: ${t.localizedMessage}")
        }
    }

    @SuppressLint("NotificationTrampoline")
    private fun finalizeBuilder(
        nb: Builder, extras: Bundle, context: Context,
        config: CleverTapInstanceConfig, style: NotificationCompat.Style
    ): Builder {

        val requiresChannelId = VERSION.SDK_INT >= VERSION_CODES.O
        if (requiresChannelId && extras.containsKey(Constants.WZRK_SUBTITLE)) {
            nb.setSubText(extras.getString(Constants.WZRK_SUBTITLE))
        }

        if (extras.containsKey(Constants.WZRK_COLOR)) {
            val color = Color.parseColor(extras.getString(Constants.WZRK_COLOR))
            nb.setColor(color)
            nb.setColorized(true)
        } // uncommon

        extras.getString(Constants.WZRK_DISMISS)?.let { dismissValue ->
            try {
                nb.setTimeoutAfter(dismissValue.toLong() * 1000L)
            } catch (e: NumberFormatException) {
                config.logger.verbose(config.accountId, "Failed to parse timeout dismiss value", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val isSticky = "true".equals(extras.getString(Constants.WZRK_STICKY), ignoreCase = true)
            nb.setOngoing(isSticky)
        }

        // uncommon
        nb.setContentTitle(notifTitle)
            .setContentText(notifMessage)
            .setContentIntent(LaunchPendingIntentFactory.getLaunchPendingIntent(extras, context))
            .setAutoCancel(true)
            .setStyle(style)
            .setSmallIcon(smallIconRes)

        val icoPath = extras.getString(Constants.NOTIF_ICON) // uncommon
        val showIcon = !"true".equals(extras.getString(Constants.NOTIF_HIDE_APP_LARGE_ICON), ignoreCase = true)
        if (showIcon) {
            // uncommon
            nb.setLargeIcon(
                Utils.getNotificationBitmapWithTimeout(
                    icoPath, true, context,
                    config, Constants.PN_LARGE_ICON_DOWNLOAD_TIMEOUT_IN_MILLIS
                ).bitmap
            ) // uncommon
        }
        return nb
    }

    override fun setSmallIcon(smallIcon: Int, context: Context) {
        smallIconRes = smallIcon
    }

    override fun getActionButtonIconKey(): String = Constants.NOTIF_ICON

    override fun setSound(
        context: Context, extras: Bundle, nb: Builder,
        config: CleverTapInstanceConfig
    ): Builder {
        try {
            if (extras.containsKey(Constants.WZRK_SOUND)) {
                @Suppress("DEPRECATION")
                val o = extras.get(Constants.WZRK_SOUND)
                val soundUri = when {
                    o == true || o == "true" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    o is String && o.isNotEmpty() -> {
                        val sound = if (o.contains(".mp3") || o.contains(".ogg") || o.contains(".wav")) {
                            o.substring(0, o.length - 4)
                        } else {
                            o
                        }
                        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/$sound")
                    }

                    else -> null
                }

                soundUri?.let { nb.setSound(it) }
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "Could not process sound parameter", t)
        }

        return nb
    }
}
