package com.clevertap.android.sdk.interfaces

import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * Generic Interface to parse different types of notification messages
 * @param T Type of notification message
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface INotificationParser<T : Any> {

    /**
     * Parses notification message to Bundle
     * @param message notification message received from cloud messaging provider like firebase.
     * @return [Bundle] object
     */
    public fun toBundle(message: T): Bundle?
}
