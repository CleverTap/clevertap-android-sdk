package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import com.clevertap.android.pushtemplates.*
import com.clevertap.android.pushtemplates.PTConstants.KEY_CLICKED_STAR
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory
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
const val PRODUCT_DISPLAY_CONTENT_PENDING_INTENT = 20
const val PRODUCT_DISPLAY_DL1_PENDING_INTENT = 21
const val PRODUCT_DISPLAY_DL2_PENDING_INTENT = 22
const val PRODUCT_DISPLAY_DL3_PENDING_INTENT = 23
const val PRODUCT_DISPLAY_DISMISS_PENDING_INTENT = 28
const val ZERO_BEZEL_CONTENT_PENDING_INTENT = 29
const val TIMER_CONTENT_PENDING_INTENT = 30
const val INPUT_BOX_CONTENT_PENDING_INTENT = 31
const val INPUT_BOX_REPLY_PENDING_INTENT = 32
const val VERTICAL_IMAGE_CONTENT_PENDING_INTENT = 33
const val VERTICAL_IMAGE_BUTTON_PENDING_INTENT = 34
const val CUSTOM_RATING_CONTENT_PENDING_INTENT = 35

internal object PendingIntentFactory {

    var launchIntent: Intent? = null

    @JvmStatic
    fun setPendingIntent(
        context: Context, notificationId: Int, extras: Bundle, launchIntent: Intent?, requestCode : Int
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
            flagsLaunchPendingIntent = flagsLaunchPendingIntent or
                    if (launchIntent.hasExtra(PTConstants.PT_INPUT_FEEDBACK)) {
                        //  PendingIntents attached to actions with remote inputs must be mutable
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        PendingIntent.FLAG_IMMUTABLE
                    }
            return PendingIntent.getBroadcast(
                context, requestCode,
                launchIntent, flagsLaunchPendingIntent
            )
        }
    }

    @JvmStatic
    fun setDismissIntent(context: Context, extras: Bundle, intent: Intent): PendingIntent {
        intent.putExtras(extras)
        intent.putExtra(PTConstants.PT_DISMISS_INTENT, true)

        var flagsLaunchPendingIntent = PendingIntent.FLAG_CANCEL_CURRENT
        flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context, Random().nextInt(),
            intent, flagsLaunchPendingIntent
        )
    }

    /**
     * Improved version that uses specific parameters instead of entire TemplateData
     */
    @JvmStatic
    @JvmOverloads
    fun getPendingIntent(
        context: Context,
        notificationId: Int,
        extras: Bundle,
        isLauncher: Boolean,
        identifier: Int,
        deepLink: String? = null,
        inputFeedback: String? = null,
        inputAutoOpen: String? = null,
        config: CleverTapInstanceConfig? = null
    ): PendingIntent? {

        launchIntent = null // reset to null or else last value will get retain
        if (isLauncher && VERSION.SDK_INT < VERSION_CODES.S) {
            launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
        } else if (!isLauncher) {
            launchIntent = Intent(context, PushTemplateReceiver::class.java)
        }

        var flagsLaunchPendingIntent = 0
        flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_IMMUTABLE

        val requestCode = Random().nextInt()
        when (identifier) {
            BASIC_CONTENT_PENDING_INTENT, AUTO_CAROUSEL_CONTENT_PENDING_INTENT,
            MANUAL_CAROUSEL_CONTENT_PENDING_INTENT, ZERO_BEZEL_CONTENT_PENDING_INTENT,
            TIMER_CONTENT_PENDING_INTENT, PRODUCT_DISPLAY_CONTENT_PENDING_INTENT,
            INPUT_BOX_CONTENT_PENDING_INTENT, VERTICAL_IMAGE_CONTENT_PENDING_INTENT,
            CUSTOM_RATING_CONTENT_PENDING_INTENT -> {
                return if (deepLink != null) {
                    extras.putString(Constants.DEEP_LINK_KEY, deepLink)
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent,
                        requestCode
                    )
                } else {
                    if (extras[Constants.DEEP_LINK_KEY] == null) {
                        extras.putString(Constants.DEEP_LINK_KEY, null)
                    }
                    setPendingIntent(context, notificationId, extras, launchIntent, requestCode)
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
                    launchIntent,
                    requestCode
                )
            }

            MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT -> {
                launchIntent!!.putExtra(PTConstants.PT_RIGHT_SWIPE, false)// fix
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)// fix
                launchIntent!!.putExtras(extras)

                return setPendingIntent(
                    context, notificationId, extras, launchIntent, requestCode
                )
            }

            MANUAL_CAROUSEL_DISMISS_PENDING_INTENT -> {
                val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
                return setDismissIntent(context, extras, dismissIntent)
            }

            RATING_CONTENT_PENDING_INTENT, VERTICAL_IMAGE_BUTTON_PENDING_INTENT -> {
                extras.putString(Constants.DEEP_LINK_KEY, deepLink)
                return if (VERSION.SDK_INT < VERSION_CODES.S) {
                    setPendingIntent(
                        context,
                        notificationId,
                        extras,
                        launchIntent,
                        requestCode
                    )
                } else {
                    LaunchPendingIntentFactory.getActivityIntent(extras, context)
                }
            }

            RATING_CLICK1_PENDING_INTENT, RATING_CLICK2_PENDING_INTENT, RATING_CLICK3_PENDING_INTENT,
            RATING_CLICK4_PENDING_INTENT, RATING_CLICK5_PENDING_INTENT -> {
                val clickedStar = getRatingStarNumber(identifier)
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra("click$clickedStar", true)
                launchIntent!!.putExtra(KEY_CLICKED_STAR, clickedStar)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra("config", config)
                return PendingIntent.getBroadcast(
                    context,
                    extras.getIntArray(PTConstants.KEY_REQUEST_CODES)?.get(clickedStar - 1)!!,
                    launchIntent!!,
                    flagsLaunchPendingIntent
                )
            }

            FIVE_ICON_CONTENT_PENDING_INTENT -> {
                extras.putString(Constants.DEEP_LINK_KEY, null)
                return setPendingIntent(context, notificationId, extras, launchIntent, requestCode)
            }

            PRODUCT_DISPLAY_DL1_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra(PTConstants.PT_CURRENT_POSITION, 0)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW_DL, deepLink)
                return PendingIntent.getBroadcast(context, requestCode, launchIntent!!, flagsLaunchPendingIntent)
            }

            PRODUCT_DISPLAY_DL2_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra(PTConstants.PT_CURRENT_POSITION, 1)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW_DL, deepLink)
                return PendingIntent.getBroadcast(context, requestCode, launchIntent!!, flagsLaunchPendingIntent)
            }

            PRODUCT_DISPLAY_DL3_PENDING_INTENT -> {
                launchIntent!!.putExtras(extras)
                launchIntent!!.putExtra(PTConstants.PT_CURRENT_POSITION, 2)
                launchIntent!!.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
                launchIntent!!.putExtra(PTConstants.PT_BUY_NOW_DL, deepLink)
                return PendingIntent.getBroadcast(context, requestCode, launchIntent!!, flagsLaunchPendingIntent)
            }

            PRODUCT_DISPLAY_DISMISS_PENDING_INTENT -> {
                val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
                return setDismissIntent(context, extras, dismissIntent)
            }

            INPUT_BOX_REPLY_PENDING_INTENT -> {
                extras.putString(Constants.DEEP_LINK_KEY, deepLink)
                launchIntent!!.putExtra(PTConstants.PT_INPUT_FEEDBACK, inputFeedback)
                launchIntent!!.putExtra(PTConstants.PT_INPUT_AUTO_OPEN, inputAutoOpen)
                launchIntent!!.putExtra("config", config)

                return setPendingIntent(
                    context,
                    notificationId,
                    extras,
                    launchIntent,
                    requestCode
                )
            }
            else -> throw IllegalArgumentException("invalid pendingIntentType")
        }
    }

    /**
     * Broadcast fired when a pt_custom_rating position is tapped.
     *
     * Deliberately a broadcast and not an activity intent: tapping a position only re-renders the
     * notification with that position selected, and launching an activity from here would hit the
     * Android 12+ notification trampoline ban (FR-AND-03).
     *
     * The request code is derived from the notification id and position so a second tap on the same
     * position updates the existing PendingIntent instead of leaking a new one.
     */
    @JvmStatic
    fun getCustomRatingPositionIntent(
        context: Context,
        notificationId: Int,
        extras: Bundle,
        position: Int,
        config: CleverTapInstanceConfig?
    ): PendingIntent {
        val intent = Intent(context, PushTemplateReceiver::class.java).apply {
            putExtras(extras)
            putExtra(PTConstants.PT_RATING_SELECTED_POSITION, position)
            putExtra(PTConstants.PT_NOTIF_ID, notificationId)
            putExtra("config", config)
        }
        return PendingIntent.getBroadcast(
            context,
            customRatingRequestCode(notificationId, position),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Broadcast fired when the pt_custom_rating submit button is tapped. The handler raises the event
     * and launches the destination from there, so the activity is started outside the receiver's
     * broadcast dispatch (FR-AND-04).
     *
     * [selectedPosition] is 0 before the user has picked anything, which the handler treats as a
     * no-op.
     */
    @JvmStatic
    fun getCustomRatingSubmitIntent(
        context: Context,
        notificationId: Int,
        extras: Bundle,
        selectedPosition: Int,
        config: CleverTapInstanceConfig?
    ): PendingIntent {
        val intent = Intent(context, PushTemplateReceiver::class.java).apply {
            putExtras(extras)
            putExtra(PTConstants.PT_RATING_SUBMIT, true)
            putExtra(PTConstants.PT_RATING_SELECTED_POSITION, selectedPosition)
            putExtra(PTConstants.PT_NOTIF_ID, notificationId)
            putExtra("config", config)
            // Action buttons are not offered on this template and the blob is large enough to matter
            // against the Binder transaction limit once icon urls are already in the bundle.
            removeExtra(Constants.WZRK_ACTIONS)
        }
        return PendingIntent.getBroadcast(
            context,
            customRatingRequestCode(notificationId, SUBMIT_REQUEST_CODE_SLOT),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val SUBMIT_REQUEST_CODE_SLOT = 0

    /** Stable per (notification, slot) so re-renders reuse rather than accumulate PendingIntents. */
    private fun customRatingRequestCode(notificationId: Int, slot: Int): Int =
        notificationId * 31 + slot

    @JvmStatic
    fun getCtaLaunchPendingIntent(context: Context, extras: Bundle, dl: String, notificationId: Int): PendingIntent {
        var clazz: Class<*>? = null
        try {
            clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
        } catch (_: ClassNotFoundException) {
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
            flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_IMMUTABLE
            PendingIntent.getService(
                context,
                Random().nextInt(),
                launchIntent!!,
                flagsLaunchPendingIntent
            )
        } else {
            extras.putString(Constants.DEEP_LINK_KEY, dl)
            LaunchPendingIntentFactory.getActivityIntent(extras, context)
        }
    }

    /**
     * This function returns the number(1 to 5) of the concerned star for rating PT based on the identifier.
     */
    @JvmStatic
    private fun getRatingStarNumber(identifier: Int): Int {
        return when (identifier) {
            RATING_CLICK1_PENDING_INTENT -> 1
            RATING_CLICK2_PENDING_INTENT -> 2
            RATING_CLICK3_PENDING_INTENT -> 3
            RATING_CLICK4_PENDING_INTENT -> 4
            else -> 5
        }
    }
}