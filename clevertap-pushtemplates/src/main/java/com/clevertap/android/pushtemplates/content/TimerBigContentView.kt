package com.clevertap.android.pushtemplates.content

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.CountdownThread
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils


class TimerBigContentView(
    context: Context,
    timer_end_as_long: Int?,
    private var timer_end_as_int: Int?,
    renderer: TemplateRenderer,
    private var notificationId: Int,
    private var nb: NotificationCompat.Builder
) :
    TimerSmallContentView(context, timer_end_as_long, renderer, R.layout.timer) {

    companion object {
        /*Adds a cachedRemoteView logic to call/update the progress bar only once if
        notifications are raised multiple times*/
        private var cachedRemoteViews: RemoteViews? = null
    }

    init {
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewBigImage(renderer.pt_big_img)
            if (cachedRemoteViews == null) {
                setCustomContentViewProgressBar()
            }
    }

    private fun setCustomContentViewProgressBar() {
        Log.wtf("test","setCustomContentViewProgressBar() is called!")
        //add empty check before
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        cachedRemoteViews = remoteView
        nb.setSmallIcon(renderer.smallIcon)
            val thread = CountdownThread(
                timer_end_as_int!!,
                { tickCount ->
                    remoteView.setProgressBar(
                        R.id.progress_horizontal, timer_end_as_int!!, tickCount,
                        false
                    )
                    notificationManager.notify(notificationId, nb.build())
                },
                {
                    remoteView.setProgressBar(
                        R.id.progress_horizontal, timer_end_as_int!!, 100,
                        false
                    )
                    cachedRemoteViews = null
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