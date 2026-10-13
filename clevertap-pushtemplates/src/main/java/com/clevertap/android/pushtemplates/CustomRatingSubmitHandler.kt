package com.clevertap.android.pushtemplates

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants

/**
 * Everything that happens when the pt_custom_rating submit button is tapped: the event, the
 * notification's final state, and the destination.
 *
 * Called from [PTRatingSubmitActivity] rather than from [PushTemplateReceiver] on purpose. Since
 * Android 12 a broadcast receiver may not start an activity in response to a notification tap, so
 * the submit button points at an invisible activity — the route Google documents for "record
 * something, then open a screen" — and that activity is allowed to open the destination.
 */
internal object CustomRatingSubmitHandler {

    /**
     * Notifications whose rating has already been submitted. The notification is either dismissed or
     * re-rendered without its buttons on submit, so a second tap is hard to reach; this closes the
     * gap left by a PendingIntent that outlives the view it was attached to (R-29).
     *
     * Bounded because it is only ever a guard — the oldest entry is dropped once the cap is hit.
     */
    private const val SUBMITTED_HISTORY_SIZE = 16
    private val submittedNotificationIds = linkedSetOf<Int>()

    fun submit(context: Context, extras: Bundle) {
        val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)
        val selectedPosition = extras.getInt(PTConstants.PT_RATING_SELECTED_POSITION, 0)

        if (selectedPosition < 1) {
            PTLog.verbose("Custom rating submitted with no selection, ignoring")
            return
        }
        if (!markSubmitted(notificationId)) {
            PTLog.verbose("Custom rating $notificationId was already submitted, ignoring the repeat tap")
            return
        }

        val config = resolveConfig(extras)
        val data = TemplateDataFactory.createTemplateData(
            TemplateType.CUSTOM_RATING,
            extras,
            Utils.isDarkMode(context),
            context.getString(R.string.pt_big_image_alt)
        ) { Utils.getNotificationIds(context) } as? CustomRatingTemplateData

        val destination = resolveDestination(extras, data, selectedPosition)

        extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + selectedPosition)
        extras.putString(Constants.DEEP_LINK_KEY, destination)

        Utils.raiseCleverTapEvent(
            context, config, PTConstants.PT_RATING_EVENT_NAME,
            Utils.getCustomRatingEventProperties(
                extras,
                selectedPosition,
                data?.ratingCount ?: 0,
                data?.ratingStyle?.toString()
            )
        )

        // Submitting always clears the notification. A thank-you drawn in the tray would sit behind
        // the destination the moment it opens, so the user would never actually see it.
        notificationManager(context)?.cancel(notificationId)

        launchDestination(context, extras, destination)
    }

    /**
     * The tapped position's pt_dl{n} override when it has one, then the submit button's own
     * pt_rating_cta_dl, then pt_default_dl.
     */
    private fun resolveDestination(
        extras: Bundle,
        data: CustomRatingTemplateData?,
        selectedPosition: Int
    ): String {
        val override = data?.positions?.getOrNull(selectedPosition - 1)?.deepLink
            ?: Utils.getDeepLinkListFromExtras(extras)?.getOrNull(selectedPosition - 1)
        return override?.takeIf { it.isNotBlank() }
            ?: extras.getString(PTConstants.PT_RATING_CTA_DL)?.takeIf { it.isNotBlank() }
            // A payload with no submit button carries no pt_rating_cta_dl either, so the campaign's
            // own default destination is the last stop before the launcher activity.
            ?: extras.getString(PTConstants.PT_DEFAULT_DL).orEmpty()
    }

    /**
     * Opens the destination, or the launcher activity when the campaign carries no deep link.
     *
     * Safe from here and only from here: [context] is the invisible activity, and an activity may
     * start another activity after a notification tap.
     */
    private fun launchDestination(context: Context, extras: Bundle, destination: String) {
        val launchIntent = if (destination.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(destination)).also {
                com.clevertap.android.sdk.Utils.setPackageNameFromResolveInfoList(context, it)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }
        if (launchIntent == null) {
            PTLog.verbose("No launch intent available for the custom rating destination")
            return
        }

        launchIntent.putExtras(extras)
        launchIntent.putExtra(Constants.DEEP_LINK_KEY, destination)
        launchIntent.removeExtra(Constants.WZRK_ACTIONS)
        // Internal selection markers must not leak into the activity the app receives.
        launchIntent.removeExtra(PTConstants.PT_RATING_SUBMIT)
        launchIntent.removeExtra(PTConstants.PT_RATING_SELECTED_POSITION)
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
        launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(launchIntent)
    }

    /**
     * The instance the notification was rendered for. It travels in the submit intent; without it
     * the event goes to the default instance, which is the same fallback the other templates use.
     */
    private fun resolveConfig(extras: Bundle): CleverTapInstanceConfig? =
        extras.getParcelable("config")

    private fun notificationManager(context: Context): NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    /** @return false when this notification's rating has already been submitted. */
    @Synchronized
    private fun markSubmitted(notificationId: Int): Boolean {
        if (!submittedNotificationIds.add(notificationId)) return false
        if (submittedNotificationIds.size > SUBMITTED_HISTORY_SIZE) {
            submittedNotificationIds.iterator().run {
                next()
                remove()
            }
        }
        return true
    }
}
