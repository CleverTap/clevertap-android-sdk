package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.clevertap.android.pushtemplates.InputBoxTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateDataFactory.toBaseContent
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.INPUT_BOX_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.INPUT_BOX_REPLY_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal class InputBoxStyle(private val data: InputBoxTemplateData, private val renderer: TemplateRenderer) : Style(data.toBaseContent(), renderer) {

    private val actionButtonsHandler = ActionButtonsHandler(renderer)

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
        ).setContentText(data.textData.message)
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
        
        // Apply action buttons with special flag for InputBoxStyle
        inputBoxNotificationBuilder = actionButtonsHandler.addActionButtons(
            context, extras, notificationId, inputBoxNotificationBuilder, true
        )

        inputBoxNotificationBuilder = setStandardViewBigImageStyle(
            context, inputBoxNotificationBuilder
        )
        if (data.inputLabel.isNotNullAndEmpty()) {
            //Initialise RemoteInput
            val remoteInput = RemoteInput.Builder(PTConstants.PT_INPUT_KEY)
                .setLabel(data.inputLabel)
                .build()

            val replyIntent: PendingIntent = PendingIntentFactory.getPendingIntent(
                context, notificationId, extras, false,
                INPUT_BOX_REPLY_PENDING_INTENT, data.deepLinkList.getOrNull(0), data.inputFeedback, data.inputAutoOpen, renderer.config
            )!!
            //Notification Action with RemoteInput instance added.
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.sym_action_chat, data.inputLabel, replyIntent
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()

            //Notification.Action instance added to Notification Builder.
            inputBoxNotificationBuilder.addAction(replyAction)
        }
        if (data.dismissOnClick.isNotNullAndEmpty()) {
            extras.putString(PTConstants.PT_DISMISS_ON_CLICK, data.dismissOnClick)
        }
        return inputBoxNotificationBuilder
    }

    private fun setStandardViewBigImageStyle(
        context: Context,
        notificationBuilder: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        var bigPictureStyle: NotificationCompat.Style
        if (data.imageData.url.isNotNullAndEmpty() && data.imageData.url.startsWith("http")) {
            try {
                val bpMap = renderer.templateMediaManager.getNotificationBitmap(data.imageData.url, false, context)
                    ?: throw Exception("Failed to fetch big picture!")

                val summaryText = data.textData.messageSummary ?: data.textData.message
                bigPictureStyle = NotificationCompat.BigPictureStyle()
                    .setSummaryText(summaryText)
                    .bigPicture(bpMap)

                if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
                    bigPictureStyle.setContentDescription(data.imageData.altText)
                }
            } catch (t: Throwable) {
                bigPictureStyle = NotificationCompat.BigTextStyle()
                    .bigText(data.textData.message)
                PTLog.verbose(
                    "Falling back to big text notification, couldn't fetch big picture",
                    t
                )
            }
        } else {
            bigPictureStyle = NotificationCompat.BigTextStyle()
                .bigText(data.textData.message)
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
            INPUT_BOX_CONTENT_PENDING_INTENT, data.deepLinkList.getOrNull(0)
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