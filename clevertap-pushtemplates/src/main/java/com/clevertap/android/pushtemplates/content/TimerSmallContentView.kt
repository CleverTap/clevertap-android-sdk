package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal open class TimerSmallContentView(
    context: Context,
    timer_end: Int?,
    renderer: TemplateRenderer,
    layoutId: Int = R.layout.timer_collapsed
) :
    ActionButtonsContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomBackgroundColour(renderer.pt_bg, R.id.content_view_small)
        setCustomBackgroundColour(renderer.pt_bg, R.id.chronometer)
        setCustomTextColour(renderer.pt_title_clr, R.id.title)
        setCustomContentViewChronometerTitleColour(
            renderer.pt_chrono_title_clr,
            renderer.pt_title_clr
        )
        setCustomTextColour(renderer.pt_msg_clr, R.id.msg)
        remoteView.setChronometer(
            R.id.chronometer,
            SystemClock.elapsedRealtime() + timer_end!!,
            null,
            true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            remoteView.setChronometerCountDown(R.id.chronometer, true)
        }
        setCustomContentViewSmallIcon()
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