package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.View
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TimerTemplateData
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.isNotNullAndEmpty
import androidx.annotation.RestrictTo


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class TimerSmallContentView(
    context: Context,
    timerEnd: Long?,
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


        val timerEndMillis = timerEnd ?: 0L

        // Add a buffer to prevent negative timer values
        remoteView.setChronometer(
            R.id.chronometer,
            SystemClock.elapsedRealtime() + timerEndMillis + TIMER_BUFFER_SECONDS * PTConstants.ONE_SECOND_LONG,
            null,
            true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            remoteView.setChronometerCountDown(R.id.chronometer, true)
        }
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)

    }

    private fun setupChronometerBackground(
        style: ButtonStyle,
        bgColorStr: String?,
        borderColorStr: String?,
        gradientColor1: String?,
        gradientColor2: String?,
        gradientDirection: Double,
        cornerRadius: Float,
        borderWidth: Float?
    ) {
        val borderColor = colorOrNull(borderColorStr)
        val bitmap = when (style) {
            ButtonStyle.GRADIENT_LINEAR -> {
                val color1 = colorOrNull(gradientColor1) ?: return
                val color2 = colorOrNull(gradientColor2) ?: return
                NotificationBitmapUtils.createLinearGradientBitmap(
                    color1, color2, gradientDirection,
                    CHRONO_BITMAP_WIDTH, CHRONO_BITMAP_HEIGHT, cornerRadius, borderColor, borderWidth
                )
            }
            ButtonStyle.GRADIENT_RADIAL -> {
                val color1 = colorOrNull(gradientColor1) ?: return
                val color2 = colorOrNull(gradientColor2) ?: return
                NotificationBitmapUtils.createRadialBitmap(
                    color1, color2,
                    CHRONO_BITMAP_WIDTH, CHRONO_BITMAP_HEIGHT, cornerRadius, borderColor, borderWidth
                )
            }
            ButtonStyle.SOLID -> {
                val bgColor = colorOrNull(bgColorStr) ?: return
                NotificationBitmapUtils.createSolidBitmap(
                    bgColor, borderColor,
                    CHRONO_BITMAP_WIDTH, CHRONO_BITMAP_HEIGHT, cornerRadius, borderWidth
                )
            }
        }
        remoteView.setImageViewBitmap(R.id.chronometer_bg, bitmap)
        remoteView.setViewVisibility(R.id.chronometer_bg, View.VISIBLE)
    }

    /** The parsed [colour], or `null` when it is missing or invalid. */
    private fun colorOrNull(colour: String?): Int? = colour?.let { Utils.getColourOrNull(it) }

    private fun setCustomContentViewChronometerTitleColour(
        chronometerTitleColour: String?,
        titleColour: String?
    ) {
        if (chronometerTitleColour.isNotNullAndEmpty()) {
            setCustomTextColour(chronometerTitleColour, R.id.chronometer)
        } else if (titleColour.isNotNullAndEmpty()) {
            setCustomTextColour(titleColour, R.id.chronometer)
        }
    }

    companion object {
        /** Buffer added to the chronometer base to prevent negative timer values. */
        private const val TIMER_BUFFER_SECONDS = 3

        private const val CHRONO_BITMAP_WIDTH = 100
        private const val CHRONO_BITMAP_HEIGHT = 50
    }
}
