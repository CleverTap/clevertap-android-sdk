package com.clevertap.android.pushtemplates.styles

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateRenderer
import org.json.JSONArray

/**
 * Native progress-centric template (`pt_progress`) — the only promotable Push Template.
 *
 * Renders [NotificationCompat.ProgressStyle] (segments / points / tracker icon) and requests
 * promotion ([NotificationCompat.Builder.setRequestPromotedOngoing]) so on Android 16 it becomes a
 * pinned, always-expanded Live Update with a status-bar chip. Unlike the other templates it uses a
 * base style with NO custom RemoteViews — a hard requirement for promotion.
 *
 * Contract: TAN §14. This is the first cut (SDK-6086); fallback-tier fidelity on API < 16 and full
 * action support are follow-ups (SDK-6087 / SDK-6088).
 */
internal class ProgressStyle(private val renderer: TemplateRenderer) {

    fun builderFromStyle(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        nb: NotificationCompat.Builder
    ): NotificationCompat.Builder {

        val ended = "end".equals(extras.getString(PTConstants.PT_LA_EVENT), ignoreCase = true)

        nb.setSmallIcon(renderer.smallIcon)
            .setContentTitle(renderer.getTitle(extras, context)?.let { Html.fromHtml(it) } ?: "")
            .setContentText(renderer.getMessage(extras) ?: "")
            .setOnlyAlertOnce(true)          // updates should not re-alert
            .setOngoing(!ended)              // ongoing while the activity is live
            .setAutoCancel(ended)
            .setColor(parseColor(renderer.smallIconColour))

        val progressStyle = NotificationCompat.ProgressStyle()
            .setStyledByProgress(boolean(extras, PTConstants.PT_STYLED_BY_PROGRESS, def = false))
            .setProgress(extras.getString(PTConstants.PT_PROGRESS)?.toIntOrNull() ?: 0)

        parseSegments(extras.getString(PTConstants.PT_PROGRESS_SEGMENTS)).takeIf { it.isNotEmpty() }
            ?.let { progressStyle.setProgressSegments(it) }
        parsePoints(extras.getString(PTConstants.PT_PROGRESS_POINTS)).takeIf { it.isNotEmpty() }
            ?.let { progressStyle.setProgressPoints(it) }

        icon(context, extras.getString(PTConstants.PT_PROGRESS_TRACKER_ICON))?.let {
            progressStyle.setProgressTrackerIcon(it)
        }
        icon(context, extras.getString(PTConstants.PT_PROGRESS_START_ICON))?.let {
            progressStyle.setProgressStartIcon(it)
        }
        icon(context, extras.getString(PTConstants.PT_PROGRESS_END_ICON))?.let {
            progressStyle.setProgressEndIcon(it)
        }

        nb.setStyle(progressStyle)

        applyChip(context, extras, nb)

        // Request promotion (Android 16 Live Update). Compat no-ops on older OS.
        if (!ended && !"false".equals(extras.getString(PTConstants.PT_PROMOTE), ignoreCase = true)) {
            nb.setRequestPromotedOngoing(true)
        }

        return nb
    }

    private fun applyChip(context: Context, extras: Bundle, nb: NotificationCompat.Builder) {
        when (extras.getString(PTConstants.PT_CHIP_TYPE)?.lowercase()) {
            "text" -> extras.getString(PTConstants.PT_CHIP_TEXT)?.takeIf { it.isNotEmpty() }
                ?.let { nb.setShortCriticalText(it) }

            "timer", "countdown" -> {
                val whenMs = extras.getString(PTConstants.PT_WHEN)?.toLongOrNull()
                if (whenMs != null) {
                    nb.setWhen(whenMs).setUsesChronometer(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        nb.setChronometerCountDown(
                            boolean(extras, PTConstants.PT_COUNTDOWN, def = false)
                        )
                    }
                }
            }
        }
    }

    private fun parseSegments(json: String?): List<NotificationCompat.ProgressStyle.Segment> {
        val out = ArrayList<NotificationCompat.ProgressStyle.Segment>()
        if (json.isNullOrEmpty()) return out
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val seg = NotificationCompat.ProgressStyle.Segment(o.optInt("length", 1))
                o.optString("color").takeIf { it.isNotEmpty() }?.let { seg.setColor(Color.parseColor(it)) }
                out.add(seg)
            }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to parse segments", t)
        }
        return out
    }

    private fun parsePoints(json: String?): List<NotificationCompat.ProgressStyle.Point> {
        val out = ArrayList<NotificationCompat.ProgressStyle.Point>()
        if (json.isNullOrEmpty()) return out
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val pt = NotificationCompat.ProgressStyle.Point(o.optInt("position", 0))
                o.optString("color").takeIf { it.isNotEmpty() }?.let { pt.setColor(Color.parseColor(it)) }
                out.add(pt)
            }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to parse points", t)
        }
        return out
    }

    private fun icon(context: Context, url: String?): IconCompat? {
        if (url.isNullOrEmpty()) return null
        return try {
            renderer.templateMediaManager.getNotificationBitmap(url, false, context)
                ?.let { IconCompat.createWithBitmap(it) }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to load icon $url", t)
            null
        }
    }

    private fun boolean(extras: Bundle, key: String, def: Boolean): Boolean =
        extras.getString(key)?.let { "true".equals(it, ignoreCase = true) } ?: def

    private fun parseColor(hex: String?): Int = try {
        if (hex.isNullOrEmpty()) Color.parseColor(PTConstants.PT_COLOUR_GREY) else Color.parseColor(hex)
    } catch (t: Throwable) {
        Color.parseColor(PTConstants.PT_COLOUR_GREY)
    }
}
