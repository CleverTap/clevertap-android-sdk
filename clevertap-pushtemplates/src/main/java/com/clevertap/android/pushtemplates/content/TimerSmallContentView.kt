package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
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

    private fun setCustomContentViewChronometerBackgroundColour(pt_bg: String?) {
        pt_bg?.takeIf { it.isNotEmpty() }?.let {
            Utils.getColourOrNull(it)?.let { color ->
                remoteView.setInt(
                    R.id.chronometer,
                    "setBackgroundColor",
                    color
                )
            }
        }
    }

    private fun setCustomContentViewChronometerTitleColour(
        pt_chrono_title_clr: String?,
        pt_title_clr: String?
    ) {
        pt_chrono_title_clr?.takeIf { it.isNotEmpty() }?.let {
            Utils.getColourOrNull(it)?.let { color ->
                remoteView.setTextColor(
                    R.id.chronometer,
                    color
                )
            }
        } ?: run {
            pt_title_clr?.takeIf { it.isNotEmpty() }?.let {
                Utils.getColourOrNull(it)?.let { color ->
                    remoteView.setTextColor(
                        R.id.chronometer,
                        color
                    )
                }
            }
        }
    }
}