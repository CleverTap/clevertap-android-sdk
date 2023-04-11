package com.clevertap.android.pushtemplates.content

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.CountdownThread
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.sdk.Logger


class TimerBigContentView(
    context: Context,
    private var timer_end: Int?,
    private var timer_end1: Int?,
    renderer: TemplateRenderer,
    private var notificationId: Int,
    private var nb: NotificationCompat.Builder
) :
    TimerSmallContentView(context, timer_end, renderer, R.layout.timer) {

    init {
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewBigImage(renderer.pt_big_img)
        setCustomContentViewProgressBar()
    }

    private fun setCustomContentViewProgressBar() {
        //add empty check before
        remoteView.setViewVisibility(R.id.progress_horizontal,View.VISIBLE)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nb.setSmallIcon(renderer.smallIcon)
        val thread = CountdownThread(
            timer_end1!!,
            { tickCount ->
                remoteView.setProgressBar(R.id.progress_horizontal,timer_end1!!,tickCount,
                    false)
//                if (notificationManager.getActiveNotifications(). { it.id == notificationId }) {
//                    // The notification has not been shown yet, so show it
//                    notificationManager.notify(notificationId, nb.build())
//                }
                notificationManager.notify(notificationId, nb.build())
            },
            {
                remoteView.setProgressBar(R.id.progress_horizontal,timer_end1!!,100,
                    false)
            })
        thread.start()
    }

    private fun setCustomContentViewMessageSummary(pt_msg_summary: String?) {
        if (pt_msg_summary != null && pt_msg_summary.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.msg,
                    Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary))
            }
        }
    }

    private fun setCustomContentViewBigImage(pt_big_img: String?) {
        if (pt_big_img != null && pt_big_img.isNotEmpty()) {
            Utils.loadImageURLIntoRemoteView(R.id.big_image, pt_big_img, remoteView)
            if (Utils.getFallback()) {
                remoteView.setViewVisibility(R.id.big_image, View.GONE)
            }
        } else {
            remoteView.setViewVisibility(R.id.big_image, View.GONE)
        }
    }
}