package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TimerTemplateData
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
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.chronometer)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomContentViewChronometerTitleColour(
            data.chronometerTitleColor,
            data.baseContent.colorData.titleColor
        )
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)

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
}