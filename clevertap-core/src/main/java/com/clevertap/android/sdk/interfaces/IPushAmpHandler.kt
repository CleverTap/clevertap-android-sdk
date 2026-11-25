package com.clevertap.android.sdk.interfaces

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * Generic Interface to handle Pull Notifications for different types of notification messages, received from
 * respective services or receivers(ex. FirebaseMessagingService).
 *
 * Implement this interface if you want to support push amp for different types of notification messages.
 * @param T Type of notification message
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IPushAmpHandler<T : Any> {

    /**
     * Processes notification message for Pull Notifications
     * @param context application context
     * @param message notification message received from cloud messaging provider like firebase.
     */
    fun processPushAmp(context: Context?, message: T)
}
