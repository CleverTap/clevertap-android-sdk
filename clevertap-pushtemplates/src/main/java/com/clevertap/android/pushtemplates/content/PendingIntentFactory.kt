package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTConstants.KEY_CLICKED_STAR
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTPushNotificationReceiver
import com.clevertap.android.pushtemplates.PushTemplateReceiver
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory
import java.util.Random

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

    var launchIntent: Intent? = null

    @JvmStatic
    fun setPendingIntent(
        context: Context, notificationId: Int, extras: Bundle, launchIntent: Intent?
    ): PendingIntent {
        val dl = extras[Constants.DEEP_LINK_KEY]
        extras.putInt(PTConstants.PT_NOTIF_ID, notificationId)
        if (dl != null) {
            extras.putBoolean(PTConstants.DEFAULT_DL, true)
        }

        if (launchIntent == null) {
            /**
             * To support Android 12 trampoline restriction return activity pending intent
             */
            return LaunchPendingIntentFactory.getActivityIntent(extras, context)
        } else {
            launchIntent.putExtras(extras)
            launchIntent.removeExtra(Constants.WZRK_ACTIONS)
            launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
            launchIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            var flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
                flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_MUTABLE
            }
            return PendingIntent.getBroadcast(
                context, System.currentTimeMillis().toInt(),
                launchIntent, flagsLaunchPendingIntent
            )
        }
    }

    @JvmStatic
    fun setDismissIntent(context: Context, extras: Bundle, intent: Intent): PendingIntent {
        intent.putExtras(extras)
        intent.putExtra(PTConstants.PT_DISMISS_INTENT, true)

        var flagsLaunchPendingIntent = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
            flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(),
            intent, flagsLaunchPendingIntent
        )
    }

    @JvmStatic
    fun getPendingIntent(
        context: Context, notificationId: Int, extras: Bundle,
        isLauncher: Boolean, identifier: Int, renderer: TemplateRenderer?
    ): PendingIntent? {

        launchIntent = null // reset to null or else last value will get retain
        if (isLauncher && VERSION.SDK_INT < VERSION_CODES.S) {
            launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
        } else if (!isLauncher) {
            launchIntent = Intent(context, PushTemplateReceiver::class.java)
        }

        var flagsLaunchPendingIntent = 0
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_IMMUTABLE
        }

        when (identifier) {
            BASIC_CONTENT_PENDING_INTENT, AUTO_CAROUSEL_CONTENT_PENDING_INTENT,
            MANUAL_CAROUSEL_CONTENT_PENDING_INTENT, ZERO_BEZEL_CONTENT_PENDING_INTENT,
            TIMER_CONTENT_PENDING_INTENT, PRODUCT_DISPLAY_CONTENT_PENDING_INTENT,
            INPUT_BOX_CONTENT_PENDING_INTENT -> {
                return if (renderer?.deepLinkList != null && renderer.deepLinkList!!.size > 0) {
                    extras.putString(Constants.DEEP_LINK_KEY, renderer.deepLinkList!![0])
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent
                    )
                } else {
                    if (extras[Constants.DEEP_LINK_KEY] == null) {
                        extras.putString(Constants.DEEP_LINK_KEY, null)
                    }
                    setPendingIntent(context, notificationId, extras, launchIntent)
                }
            }

            MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT -> {
                launchIntent!!.putExtra(PTConstants.PT_RIGHT_SWIPE, true)// fix
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)// fix
                launchIntent!!.putExtras(extras)

                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent
                )
            }

            MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT -> {
                launchIntent!!.putExtra(PTConstants.PT_RIGHT_SWIPE, false)// fix
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)// fix
                launchIntent!!.putExtras(extras)

                return setPendingIntent(
                    context, notificationId, extras, launchIntent
                )
            }

            MANUAL_CAROUSEL_DISMISS_PENDING_INTENT -> {
                val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
                return setDismissIntent(context, extras, dismissIntent)
            }

            RATING_CONTENT_PENDING_INTENT -> {
                extras.putString(
                    Constants.DEEP_LINK_KEY,
                    renderer?.pt_rating_default_dl
                )
                return if (VERSION.SDK_INT < VERSION_CODES.S) {
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent
                    )
                } else {
                    LaunchPendingIntentFactory.getActivityIntent(extras, context)
                }
            }

            RATING_CLICK1_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra("click1", true)
                launchIntent!!.putExtra(KEY_CLICKED_STAR, 1)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra("config", renderer?.config)
                return PendingIntent.getBroadcast(
                    context,
                    RATING_CLICK1_PENDING_INTENT,
                    launchIntent!!,
                    flagsLaunchPendingIntent
                )
            }

            RATING_CLICK2_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra("click2", true)
                launchIntent!!.putExtra(KEY_CLICKED_STAR, 2)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra("config", renderer?.config)
                return PendingIntent.getBroadcast(
                    context,
                    RATING_CLICK2_PENDING_INTENT,
                    launchIntent!!,
                    flagsLaunchPendingIntent
                )
            }

            RATING_CLICK3_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra("click3", true)
                launchIntent!!.putExtra(KEY_CLICKED_STAR, 3)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra("config", renderer?.config)
                return PendingIntent.getBroadcast(
                    context,
                    RATING_CLICK3_PENDING_INTENT,
                    launchIntent!!,
                    flagsLaunchPendingIntent
                )
            }

            RATING_CLICK4_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra("click4", true)
                launchIntent!!.putExtra(KEY_CLICKED_STAR, 4)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra("config", renderer?.config)
                return PendingIntent.getBroadcast(
                    context,
                    RATING_CLICK4_PENDING_INTENT,
                    launchIntent!!,
                    flagsLaunchPendingIntent
                )
            }

            RATING_CLICK5_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra("click5", true)
                launchIntent!!.putExtra(KEY_CLICKED_STAR, 5)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra("config", renderer?.config)
                return PendingIntent.getBroadcast(
                    context,
                    RATING_CLICK5_PENDING_INTENT,
                    launchIntent!!,
                    flagsLaunchPendingIntent
                )
            }

            FIVE_ICON_CONTENT_PENDING_INTENT -> {
                extras.putString(Constants.DEEP_LINK_KEY, null)
                return setPendingIntent(context, notificationId, extras, launchIntent)
            }

            FIVE_ICON_CLOSE_PENDING_INTENT -> {
                val reqCode = Random().nextInt()
                launchIntent!!.putExtra("close", true)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtras(extras)
                return PendingIntent.getBroadcast(context, reqCode, launchIntent!!, flagsLaunchPendingIntent)
            }

            PRODUCT_DISPLAY_DL1_PENDING_INTENT -> {
                val requestCode = Random().nextInt()
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra(PTConstants.PT_CURRENT_POSITION, 0)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW_DL, renderer?.deepLinkList!![0])
                return PendingIntent.getBroadcast(context, requestCode, launchIntent!!, flagsLaunchPendingIntent)
            }

            PRODUCT_DISPLAY_DL2_PENDING_INTENT -> {
                val requestCode = Random().nextInt()
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra(PTConstants.PT_CURRENT_POSITION, 1)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW_DL, renderer?.deepLinkList!![1])
                return PendingIntent.getBroadcast(context, requestCode, launchIntent!!, flagsLaunchPendingIntent)
            }

            PRODUCT_DISPLAY_DL3_PENDING_INTENT -> {
                val requestCode = Random().nextInt()
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra(PTConstants.PT_CURRENT_POSITION, 2)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW_DL, renderer?.deepLinkList!![2])
                return PendingIntent.getBroadcast(context, requestCode, launchIntent!!, flagsLaunchPendingIntent)
            }

            PRODUCT_DISPLAY_CONTENT_SMALL1_PENDING_INTENT -> {
                extras.putString(
                    Constants.DEEP_LINK_KEY,
                    renderer?.deepLinkList!![0]
                )
                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent
                )
            }

            PRODUCT_DISPLAY_CONTENT_SMALL2_PENDING_INTENT -> {
                extras.putString(
                    Constants.DEEP_LINK_KEY,
                    renderer?.deepLinkList!![1]
                )
                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent
                )
            }

            PRODUCT_DISPLAY_CONTENT_SMALL3_PENDING_INTENT -> {
                extras.putString(
                    Constants.DEEP_LINK_KEY,
                    renderer?.deepLinkList!![2]
                )
                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent
                )
            }

            PRODUCT_DISPLAY_DISMISS_PENDING_INTENT -> {
                val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
                return setDismissIntent(context, extras, dismissIntent)
            }

            PRODUCT_DISPLAY_BUY_NOW_PENDING_INTENT -> {
                launchIntent!!.putExtra(PTConstants.PT_IMAGE_1, true)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW_DL, renderer?.deepLinkList!![0])
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW, true)
                launchIntent!!.putExtra("config", renderer?.config)
                launchIntent!!.putExtras(extras)
                return PendingIntent.getBroadcast(
                    context,
                    Random().nextInt(),
                    launchIntent!!,
                    flagsLaunchPendingIntent
                )
            }

            INPUT_BOX_REPLY_PENDING_INTENT -> {
                if (renderer?.deepLinkList!!.size > 0) {
                    extras.putString(Constants.DEEP_LINK_KEY, renderer?.deepLinkList!![0])
                }
                launchIntent!!.putExtra(PTConstants.PT_INPUT_FEEDBACK, renderer?.pt_input_feedback)
                launchIntent!!.putExtra(PTConstants.PT_INPUT_AUTO_OPEN, renderer?.pt_input_auto_open)
                launchIntent!!.putExtra("config", renderer?.config)

                return if (renderer?.deepLinkList != null) {
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent
                    )
                } else {
                    extras.putString(Constants.DEEP_LINK_KEY, null)
                    setPendingIntent(context, notificationId, extras, launchIntent)
                }
            }
            else -> throw IllegalArgumentException("invalid pendingIntentType")
        }
    }

    @JvmStatic
    fun getCtaLaunchPendingIntent(context: Context, extras: Bundle, dl: String, notificationId: Int): PendingIntent {
        var clazz: Class<*>? = null
        try {
            clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
        } catch (ex: ClassNotFoundException) {
            PTLog.debug("No Intent Service found")
        }

        val isCTIntentServiceAvailable = Utils.isServiceAvailable(context, clazz)

        return if (VERSION.SDK_INT < VERSION_CODES.S && isCTIntentServiceAvailable) {
            extras.putBoolean("autoCancel", true)
            extras.putInt(Constants.PT_NOTIF_ID, notificationId)
            launchIntent = Intent(CTNotificationIntentService.MAIN_ACTION)
            launchIntent!!.putExtras(extras)
            launchIntent!!.putExtra("dl", dl)
            launchIntent!!.setPackage(context.packageName)
            launchIntent!!.putExtra(Constants.KEY_CT_TYPE, CTNotificationIntentService.TYPE_BUTTON_CLICK)

            var flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_IMMUTABLE
            }
            PendingIntent.getService(
                context,
                System.currentTimeMillis().toInt(),
                launchIntent!!,
                flagsLaunchPendingIntent
            )
        } else {
            extras.putString(Constants.DEEP_LINK_KEY, dl)
            LaunchPendingIntentFactory.getActivityIntent(extras, context)
        }
    }
}