package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

open class TimerSmallContentView(
    context: Context,
    timer_end: Int?,
    renderer: TemplateRenderer,
    layoutId: Int = R.layout.timer_collapsed
) :
    ContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewCollapsedBackgroundColour(renderer.pt_bg)
        setCustomContentViewChronometerBackgroundColour(renderer.pt_bg)
        setCustomContentViewTitleColour(renderer.pt_title_clr)
        setCustomContentViewChronometerTitleColour(
            renderer.pt_chrono_title_clr,
            renderer.pt_title_clr
        )
        setCustomContentViewMessageColour(renderer.pt_msg_clr)
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

    internal fun setCustomContentViewChronometerBackgroundColour(pt_bg: String?) {
        if (pt_bg != null && pt_bg.isNotEmpty()) {
            remoteView.setInt(
                R.id.chronometer,
                "setBackgroundColor",
                Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE)
            )
        }
    }

    internal fun setCustomContentViewChronometerTitleColour(
        pt_chrono_title_clr: String?,
        pt_title_clr: String?
    ) {
        if (pt_chrono_title_clr != null && pt_chrono_title_clr.isNotEmpty()) {
            remoteView.setTextColor(
                R.id.chronometer,
                Utils.getColour(pt_chrono_title_clr, PTConstants.PT_COLOUR_BLACK)
            )
        } else {
            if (pt_title_clr != null && pt_title_clr.isNotEmpty()) {
                remoteView.setTextColor(
                    R.id.chronometer,
                    Utils.getColour(pt_title_clr, PTConstants.PT_COLOUR_BLACK)
                )
            }
        }
    }
}