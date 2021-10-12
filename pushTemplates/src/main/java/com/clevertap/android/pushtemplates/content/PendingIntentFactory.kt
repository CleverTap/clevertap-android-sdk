package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTPushNotificationReceiver
import com.clevertap.android.pushtemplates.PushTemplateReceiver
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import java.util.*

const val BASIC_CONTENT_PENDING_INTENT = 1
const val AUTO_CAROUSEL_CONTENT_PENDING_INTENT = 2
const val MANUAL_CAROUSEL_CONTENT_PENDING_INTENT = 3
const val MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT = 4
const val MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT = 5
const val MANUAL_CAROUSEL_DISMISS_PENDING_INTENT = 6
const val RATING_CONTENT_PENDING_INTENT = 7
const val RATING_CLICK1_PENDING_INTENT = 8
const val RATING_CLICK2_PENDING_INTENT = 9
const val RATING_CLICK3_PENDING_INTENT = 10
const val RATING_CLICK4_PENDING_INTENT = 11
const val RATING_CLICK5_PENDING_INTENT = 12
const val FIVE_ICON_CONTENT_PENDING_INTENT = 13
const val FIVE_ICON_CTA1_PENDING_INTENT = 14
const val FIVE_ICON_CTA2_PENDING_INTENT = 15
const val FIVE_ICON_CTA3_PENDING_INTENT = 16
const val FIVE_ICON_CTA4_PENDING_INTENT = 17
const val FIVE_ICON_CTA5_PENDING_INTENT = 18
const val FIVE_ICON_CLOSE_PENDING_INTENT = 19
const val PRODUCT_DISPLAY_CONTENT_PENDING_INTENT = 20
const val PRODUCT_DISPLAY_DL1_PENDING_INTENT = 21
const val PRODUCT_DISPLAY_DL2_PENDING_INTENT = 22
const val PRODUCT_DISPLAY_DL3_PENDING_INTENT = 23
const val PRODUCT_DISPLAY_CONTENT_SMALL1_PENDING_INTENT = 24
const val PRODUCT_DISPLAY_CONTENT_SMALL2_PENDING_INTENT = 25
const val PRODUCT_DISPLAY_CONTENT_SMALL3_PENDING_INTENT = 26
const val PRODUCT_DISPLAY_BUY_NOW_PENDING_INTENT = 27
const val PRODUCT_DISPLAY_DISMISS_PENDING_INTENT = 28
const val ZERO_BEZEL_CONTENT_PENDING_INTENT = 29
const val TIMER_CONTENT_PENDING_INTENT = 30
const val INPUT_BOX_CONTENT_PENDING_INTENT = 31
const val INPUT_BOX_REPLY_PENDING_INTENT = 32

internal object PendingIntentFactory {

    lateinit var launchIntent: Intent

    @JvmStatic
    fun setPendingIntent(
        context: Context, notificationId: Int, extras: Bundle, launchIntent: Intent,
        dl: String?
    ): PendingIntent {
        launchIntent.putExtras(extras)
        launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
        if (dl != null) {
            launchIntent.putExtra(PTConstants.DEFAULT_DL, true)
            launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl)
        }
        launchIntent.removeExtra(Constants.WZRK_ACTIONS)
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
        launchIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(),
            launchIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

    }

    @JvmStatic
    fun setDismissIntent(context: Context, extras: Bundle, intent: Intent): PendingIntent {
        intent.putExtras(extras)
        intent.putExtra(PTConstants.PT_DISMISS_INTENT, true)
        return PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(),
            intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    @JvmStatic
    fun getPendingIntent(
        context: Context, notificationId: Int, extras: Bundle,
        isLauncher: Boolean, identifier: Int, renderer: TemplateRenderer
    ): PendingIntent? {

        if (isLauncher) {
            launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
        } else if (!isLauncher) {
            launchIntent = Intent(context, PushTemplateReceiver::class.java)
        }


        when (identifier) {
            BASIC_CONTENT_PENDING_INTENT, AUTO_CAROUSEL_CONTENT_PENDING_INTENT,
            MANUAL_CAROUSEL_CONTENT_PENDING_INTENT, ZERO_BEZEL_CONTENT_PENDING_INTENT,
            TIMER_CONTENT_PENDING_INTENT,PRODUCT_DISPLAY_CONTENT_PENDING_INTENT -> {
                return if (renderer.deepLinkList != null && renderer.deepLinkList!!.size > 0) {
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent,
                        renderer.deepLinkList!![0]
                    )
                } else {
                    setPendingIntent(context, notificationId, extras, launchIntent, null)
                }
            }

            MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT -> {
                launchIntent.putExtra(PTConstants.PT_RIGHT_SWIPE, true)
                launchIntent.putExtra(PTConstants.PT_MANUAL_CAROUSEL_FROM, 0)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)

                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent,
                    renderer.deepLinkList!![0]
                )
            }

            MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT -> {
                launchIntent.putExtra(PTConstants.PT_RIGHT_SWIPE, false)
                launchIntent.putExtra(PTConstants.PT_MANUAL_CAROUSEL_FROM, 0)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)

                return setPendingIntent(
                    context, notificationId, extras, launchIntent, renderer.deepLinkList!![0]
                )
            }

            MANUAL_CAROUSEL_DISMISS_PENDING_INTENT -> {
                val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
                return setDismissIntent(context, extras, dismissIntent)
            }

            RATING_CONTENT_PENDING_INTENT -> {
                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent,
                    renderer.pt_rating_default_dl
                )
            }

            RATING_CLICK1_PENDING_INTENT -> {
                launchIntent.putExtra("click1", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra("config", renderer.config)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, Random().nextInt(), launchIntent, 0)
            }

            RATING_CLICK2_PENDING_INTENT -> {
                launchIntent.putExtra("click2", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra("config", renderer.config)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, Random().nextInt(), launchIntent, 0)
            }

            RATING_CLICK3_PENDING_INTENT -> {
                launchIntent.putExtra("click3", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra("config", renderer.config)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, Random().nextInt(), launchIntent, 0)
            }

            RATING_CLICK4_PENDING_INTENT -> {
                launchIntent.putExtra("click4", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra("config", renderer.config)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, Random().nextInt(), launchIntent, 0)
            }

            RATING_CLICK5_PENDING_INTENT -> {
                launchIntent.putExtra("click5", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra("config", renderer.config)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, Random().nextInt(), launchIntent, 0)
            }


            FIVE_ICON_CTA1_PENDING_INTENT -> {
                val reqCode = Random().nextInt()
                launchIntent.putExtra("cta1", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, reqCode, launchIntent, 0)
            }

            FIVE_ICON_CTA2_PENDING_INTENT -> {
                val reqCode = Random().nextInt()
                launchIntent.putExtra("cta2", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, reqCode, launchIntent, 0)
            }

            FIVE_ICON_CTA3_PENDING_INTENT -> {
                val reqCode = Random().nextInt()
                launchIntent.putExtra("cta3", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, reqCode, launchIntent, 0)
            }

            FIVE_ICON_CTA4_PENDING_INTENT -> {
                val reqCode = Random().nextInt()
                launchIntent.putExtra("cta4", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, reqCode, launchIntent, 0)
            }

            FIVE_ICON_CTA5_PENDING_INTENT -> {
                val reqCode = Random().nextInt()
                launchIntent.putExtra("cta5", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, reqCode, launchIntent, 0)
            }

            FIVE_ICON_CONTENT_PENDING_INTENT -> {
                return setPendingIntent(context, notificationId, extras, launchIntent, null)
            }

            FIVE_ICON_CLOSE_PENDING_INTENT -> {
                val reqCode = Random().nextInt()
                launchIntent.putExtra("close", true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, reqCode, launchIntent, 0)
            }

            PRODUCT_DISPLAY_DL1_PENDING_INTENT -> {
                val requestCode = Random().nextInt()
                launchIntent.putExtra(PTConstants.PT_CURRENT_POSITION, 0)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra(PTConstants.PT_BUY_NOW_DL, renderer.deepLinkList!![0])
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, requestCode, launchIntent, 0)
            }

            PRODUCT_DISPLAY_DL2_PENDING_INTENT -> {
                val requestCode = Random().nextInt()
                launchIntent.putExtra(PTConstants.PT_CURRENT_POSITION, 1)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra(PTConstants.PT_BUY_NOW_DL, renderer.deepLinkList!![1])
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, requestCode, launchIntent, 0)
            }

            PRODUCT_DISPLAY_DL3_PENDING_INTENT -> {
                val requestCode = Random().nextInt()
                launchIntent.putExtra(PTConstants.PT_CURRENT_POSITION, 2)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra(PTConstants.PT_BUY_NOW_DL, renderer.deepLinkList!![2])
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, requestCode, launchIntent, 0)
            }

            PRODUCT_DISPLAY_CONTENT_SMALL1_PENDING_INTENT -> {
                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent,
                    renderer.deepLinkList!![0]
                )
            }

            PRODUCT_DISPLAY_CONTENT_SMALL2_PENDING_INTENT -> {
                    return setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent,
                        renderer.deepLinkList!![1]
                    )
            }

            PRODUCT_DISPLAY_CONTENT_SMALL3_PENDING_INTENT -> {
                    return setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent,
                        renderer.deepLinkList!![2]
                    )
            }

            PRODUCT_DISPLAY_DISMISS_PENDING_INTENT -> {
                val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
                return setDismissIntent(context, extras, dismissIntent)
            }

            PRODUCT_DISPLAY_BUY_NOW_PENDING_INTENT -> {
                launchIntent.putExtra(PTConstants.PT_IMAGE_1, true)
                launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent.putExtra(PTConstants.PT_BUY_NOW_DL, renderer.deepLinkList!![0])
                launchIntent.putExtra(PTConstants.PT_BUY_NOW, true)
                launchIntent.putExtra("config", renderer.config)
                launchIntent.putExtras(extras)
                return PendingIntent.getBroadcast(context, Random().nextInt(), launchIntent, 0)
            }

            INPUT_BOX_CONTENT_PENDING_INTENT -> {

                return if (renderer.deepLinkList != null && renderer.deepLinkList!!.size > 0) {
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent,
                        renderer.deepLinkList!![0]
                    )
                } else {
                    setPendingIntent(context, notificationId, extras, launchIntent, null)
                }
            }

            INPUT_BOX_REPLY_PENDING_INTENT -> {
                launchIntent.putExtra(PTConstants.PT_INPUT_FEEDBACK, renderer.pt_input_feedback)
                launchIntent.putExtra(PTConstants.PT_INPUT_AUTO_OPEN, renderer.pt_input_auto_open)
                launchIntent.putExtra("config", renderer.config)

                return if (renderer.deepLinkList != null) {
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent,
                        renderer.deepLinkList!![0]
                    )
                } else {
                    setPendingIntent(context, notificationId, extras, launchIntent, null)
                }
            }
            else -> throw IllegalArgumentException("invalid pendingIntentType")
        }
    }
}