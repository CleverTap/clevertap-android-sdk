package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.View
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.GradientDirection
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TimerLayout
import com.clevertap.android.pushtemplates.TimerTemplateData
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal open class TimerSmallContentView(
    context: Context,
    timer_end: Long?,
    renderer: TemplateRenderer,
    data: TimerTemplateData,
    layoutId: Int = R.layout.timer_collapsed
) :
    ActionButtonsContentView(context, renderer, layoutId) {

    init {
        setCustomContentViewBasicKeys(data.baseContent.textData.subtitle, data.baseContent.colorData.metaColor)
        setCustomContentViewTitle(data.baseContent.textData.title)
        setCustomContentViewMessage(data.baseContent.textData.message)
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_small)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomContentViewChronometerTitleColour(
            data.chronometerTitleColor,
            data.baseContent.colorData.titleColor
        )
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        remoteView.setViewVisibility(R.id.large_icon, View.GONE)

        setupChronometerBackground(
            data.chronometerStyle,
            data.chronometerBgColor,
            data.chronometerBorderColor,
            data.chronometerGradientColor1,
            data.chronometerGradientColor2,
            data.chronometerGradientDirection,
            data.chronometerBorderRadius,
            data.chronometerBorderWidth
        )

        // Add a 3 second buffer to prevent negative timer values
        remoteView.setChronometer(
            R.id.chronometer,
            SystemClock.elapsedRealtime() + timer_end!! + 3 * PTConstants.ONE_SECOND_LONG,
            null,
            true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            remoteView.setChronometerCountDown(R.id.chronometer, true)
        }
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)

        if (data.timerLayout == TimerLayout.SEGMENTED) {
            remoteView.setViewVisibility(R.id.chronometer_frame, View.GONE)
            remoteView.setViewVisibility(R.id.segmented_timer_layout, View.VISIBLE)

            val totalSeconds = (timer_end!! / 1000).toInt()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            remoteView.setTextViewText(R.id.hrs_value, String.format("%02d", hours))
            remoteView.setTextViewText(R.id.mins_value, String.format("%02d", minutes))
            remoteView.setTextViewText(R.id.secs_value, String.format("%02d", seconds))
        }

    }

    private fun setupChronometerBackground(
        style: ButtonStyle,
        bgColorStr: String?,
        borderColorStr: String?,
        gradientColor1: String?,
        gradientColor2: String?,
        gradientDirection: GradientDirection,
        cornerRadius: Float,
        borderWidth: Float?
    ) {
        val bitmap = if (style == ButtonStyle.GRADIENT) {
            val color1 = gradientColor1?.let { Utils.getColourOrNull(it) } ?: return
            val color2 = gradientColor2?.let { Utils.getColourOrNull(it) } ?: return
            val borderColor = borderColorStr?.let { Utils.getColourOrNull(it) }
            NotificationBitmapUtils.createGradientBitmap(
                color1, color2, gradientDirection,
                CHRONO_BITMAP_WIDTH, CHRONO_BITMAP_HEIGHT, cornerRadius, borderColor, borderWidth
            )
        } else {
            val bgColor = bgColorStr?.let { Utils.getColourOrNull(it) } ?: return
            val borderColor = borderColorStr?.let { Utils.getColourOrNull(it) }
            NotificationBitmapUtils.createSolidBitmap(
                bgColor, borderColor,
                CHRONO_BITMAP_WIDTH, CHRONO_BITMAP_HEIGHT, cornerRadius, borderWidth
            )
        }
        remoteView.setImageViewBitmap(R.id.chronometer_bg, bitmap)
        remoteView.setViewVisibility(R.id.chronometer_bg, View.VISIBLE)
    }

    private fun setCustomContentViewChronometerTitleColour(
        pt_chrono_title_clr: String?,
        pt_title_clr: String?
    ) {
        if (pt_chrono_title_clr.isNotNullAndEmpty()) {
            setCustomTextColour(pt_chrono_title_clr, R.id.chronometer)
        } else if (pt_title_clr.isNotNullAndEmpty()) {
            setCustomTextColour(pt_title_clr, R.id.chronometer)
        }
    }

    companion object {
        private const val CHRONO_BITMAP_WIDTH = 100
        private const val CHRONO_BITMAP_HEIGHT = 24
    }
}