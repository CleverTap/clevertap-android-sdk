package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.clevertap.android.pushtemplates.*
import com.clevertap.android.pushtemplates.content.INPUT_BOX_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.INPUT_BOX_REPLY_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService

class InputBoxStyle(private var renderer: TemplateRenderer): Style(renderer) {

    override fun setNotificationBuilderBasics(
        notificationBuilder: NotificationCompat.Builder,
        contentViewSmall: RemoteViews?,
        contentViewBig: RemoteViews?,
        pt_title: String?,
        pIntent: PendingIntent?,
        dIntent: PendingIntent?
    ): NotificationCompat.Builder {
      return  super.setNotificationBuilderBasics(notificationBuilder, contentViewSmall,
          contentViewBig, pt_title, pIntent, dIntent).setContentText(renderer.pt_msg)
    }

    override fun makeSmallContentView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return null
    }

    override fun makeBigContentView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return null
    }

    override fun builderFromStyle(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        nb: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        var inputBoxNotificationBuilder = super.builderFromStyle(context, extras, notificationId, nb)
        inputBoxNotificationBuilder = setStandardViewBigImageStyle(renderer.pt_big_img, extras,
            context, inputBoxNotificationBuilder)
        if (renderer.pt_input_label != null && renderer.pt_input_label!!.isNotEmpty()) {
            //Initialise RemoteInput
            val remoteInput = RemoteInput.Builder(PTConstants.PT_INPUT_KEY)
                .setLabel(renderer.pt_input_label)
                .build()

            //Notification Action with RemoteInput instance added.
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.sym_action_chat, renderer.pt_input_label, PendingIntentFactory.
                getPendingIntent(context,notificationId,extras,false,
                    INPUT_BOX_REPLY_PENDING_INTENT,renderer))
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()

            //Notification.Action instance added to Notification Builder.
            inputBoxNotificationBuilder.addAction(replyAction)
        }
        if (renderer.pt_dismiss_on_click != null && renderer.pt_dismiss_on_click!!.isNotEmpty()){
            extras.putString(PTConstants.PT_DISMISS_ON_CLICK, renderer.pt_dismiss_on_click)
        }
        setActionButtons(context, extras, notificationId, inputBoxNotificationBuilder)
        return inputBoxNotificationBuilder
    }

    private fun setActionButtons(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        nb: NotificationCompat.Builder
    ) {
        var clazz: Class<*>? = null
        try {
            clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
        } catch (ex: ClassNotFoundException) {
            PTLog.debug("No Intent Service found")
        }
        val isPTIntentServiceAvailable = com.clevertap.android.sdk.Utils.isServiceAvailable(context, clazz)
        if (renderer.actions != null && renderer.actions!!.length() > 0) {
            for (i in 0 until renderer.actions!!.length()) {
                try {
                    val action = renderer.actions!!.getJSONObject(i)
                    val label = action.optString("l")
                    val dl = action.optString("dl")
                    val ico = action.optString(PTConstants.PT_NOTIF_ICON)
                    val id = action.optString("id")
                    val autoCancel = action.optBoolean("ac", true)
                    if (label.isEmpty() || id.isEmpty()) {
                        PTLog.debug("not adding push notification action: action label or id missing")
                        continue
                    }
                    var icon = 0
                    if (ico.isNotEmpty()) {
                        try {
                            icon = context.resources.getIdentifier(
                                ico,
                                "drawable",
                                context.packageName
                            )
                        } catch (t: Throwable) {
                            PTLog.debug("unable to add notification action icon: " + t.localizedMessage)
                        }
                    }
                    val sendToPTIntentService = autoCancel && isPTIntentServiceAvailable
                    var actionLaunchIntent: Intent?
                    if (sendToPTIntentService) {
                        actionLaunchIntent = Intent(CTNotificationIntentService.MAIN_ACTION)
                        actionLaunchIntent.setPackage(context.packageName)
                        actionLaunchIntent.putExtra(
                            PTConstants.PT_TYPE,
                            CTNotificationIntentService.TYPE_BUTTON_CLICK
                        )
                        if (dl.isNotEmpty()) {
                            actionLaunchIntent.putExtra("dl", dl)
                        }
                    } else {
                        actionLaunchIntent = if (dl.isNotEmpty()) {
                            Intent(Intent.ACTION_VIEW, Uri.parse(dl))
                        } else {
                            context.packageManager.getLaunchIntentForPackage(context.packageName)
                        }
                    }
                    if (actionLaunchIntent != null) {
                        actionLaunchIntent.putExtras(extras)
                        actionLaunchIntent.removeExtra(Constants.WZRK_ACTIONS)
                        actionLaunchIntent.putExtra(PTConstants.PT_ACTION_ID, id)
                        actionLaunchIntent.putExtra("autoCancel", autoCancel)
                        actionLaunchIntent.putExtra("wzrk_c2a", id)
                        actionLaunchIntent.putExtra("notificationId", notificationId)
                        actionLaunchIntent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    var actionIntent: PendingIntent? = null
                    val requestCode = System.currentTimeMillis().toInt() + i
                    actionIntent = if (sendToPTIntentService) {
                        PendingIntent.getService(
                            context, requestCode,
                            actionLaunchIntent!!, PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    } else {
                        PendingIntent.getActivity(
                            context, requestCode,
                            actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    }
                    nb.addAction(icon, label, actionIntent)
                } catch (t: Throwable) {
                    PTLog.debug("error adding notification action : " + t.localizedMessage)
                }
            }
        }
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
        return PendingIntentFactory.getPendingIntent(context,notificationId,extras,true,
            INPUT_BOX_CONTENT_PENDING_INTENT,renderer
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