package com.clevertap.android.sdk.pushnotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger

/**
 * Receives the delete-intent fired when a Live Update notification is swiped away.
 * Raises the "Live Activity" lifecycle event with state `Dismissed`, keeping Android on par with
 * the iOS Live Activity dismissal callback (which the OS provides via ActivityKit).
 *
 * The SDK attaches this receiver's intent only when the client-supplied factory did not set its own
 * delete intent, so client dismiss handling is never clobbered.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class CTLiveUpdateDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val extras = intent.extras ?: return
            Logger.v(
                "CTLiveUpdateDismissReceiver: live update dismissed for "
                    + extras.getString(Constants.WZRK_LIVE_UPDATE_ID)
            )
            CleverTapAPI.handleLiveUpdateDismissed(context.applicationContext, extras)
        } catch (t: Throwable) {
            Logger.v("CTLiveUpdateDismissReceiver: failed to handle dismissal", t)
        }
    }

    companion object {
        const val TYPE_DISMISS = "com.clevertap.LIVE_ACTIVITY_DISMISS"
    }
}
