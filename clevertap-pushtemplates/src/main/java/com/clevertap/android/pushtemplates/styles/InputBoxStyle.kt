package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.content.INPUT_BOX_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.INPUT_BOX_REPLY_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PendingIntentFactory

class InputBoxStyle(private var renderer: TemplateRenderer) : Style(renderer) {

    override fun setNotificationBuilderBasics(
        notificationBuilder: NotificationCompat.Builder,
        contentViewSmall: RemoteViews?,
        contentViewBig: RemoteViews?,
        pt_title: String?,
        pIntent: PendingIntent?,
        dIntent: PendingIntent?
    ): NotificationCompat.Builder {
        return super.setNotificationBuilderBasics(
            notificationBuilder, contentViewSmall,
            contentViewBig, pt_title, pIntent, dIntent
        ).setContentText(renderer.pt_msg)
    }

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return null
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return null
    }

    override fun builderFromStyle(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        nb: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        var inputBoxNotificationBuilder = super.builderFromStyle(context, extras, notificationId, nb)
        inputBoxNotificationBuilder = setStandardViewBigImageStyle(
            renderer.pt_big_img, extras,
            context, inputBoxNotificationBuilder
        )
        if (renderer.pt_input_label != null && renderer.pt_input_label!!.isNotEmpty()) {
            //Initialise RemoteInput
            val remoteInput = RemoteInput.Builder(PTConstants.PT_INPUT_KEY)
                .setLabel(renderer.pt_input_label)
                .build()

            val replyIntent: PendingIntent
            replyIntent = PendingIntentFactory.getPendingIntent(
                context, notificationId, extras, false,
                INPUT_BOX_REPLY_PENDING_INTENT, renderer
            )!!
            //Notification Action with RemoteInput instance added.
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.sym_action_chat, renderer.pt_input_label, replyIntent
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()

            //Notification.Action instance added to Notification Builder.
            inputBoxNotificationBuilder.addAction(replyAction)
        }
        if (renderer.pt_dismiss_on_click != null && renderer.pt_dismiss_on_click!!.isNotEmpty()) {
            extras.putString(PTConstants.PT_DISMISS_ON_CLICK, renderer.pt_dismiss_on_click)
        }
        renderer.setActionButtons(context, extras, notificationId, inputBoxNotificationBuilder, renderer.actions)
        return inputBoxNotificationBuilder
    }

    private fun setStandardViewBigImageStyle(
        pt_big_img: String?,
        extras: Bundle,
        context: Context,
        notificationBuilder: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        var bigPictureStyle: NotificationCompat.Style
        if (pt_big_img != null && pt_big_img.startsWith("http")) {
            try {
                val bpMap = Utils.getNotificationBitmap(pt_big_img, false, context)
                    ?: throw Exception("Failed to fetch big picture!")
                bigPictureStyle = if (extras.containsKey(PTConstants.PT_MSG_SUMMARY)) {
                    val summaryText = renderer.pt_msg_summary
                    NotificationCompat.BigPictureStyle()
                        .setSummaryText(summaryText)
                        .bigPicture(bpMap)
                } else {
                    NotificationCompat.BigPictureStyle()
                        .setSummaryText(renderer.pt_msg)
                        .bigPicture(bpMap)
                }
            } catch (t: Throwable) {
                bigPictureStyle = NotificationCompat.BigTextStyle()
                    .bigText(renderer.pt_msg)
                PTLog.verbose(
                    "Falling back to big text notification, couldn't fetch big picture",
                    t
                )
            }
        } else {
            bigPictureStyle = NotificationCompat.BigTextStyle()
                .bigText(renderer.pt_msg)
        }
        notificationBuilder.setStyle(bigPictureStyle)
        return notificationBuilder
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            INPUT_BOX_CONTENT_PENDING_INTENT, renderer
        )
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return null
    }
}