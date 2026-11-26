package com.clevertap.android.sdk.interfaces

import androidx.annotation.RestrictTo

/**
 * Kept public + RestrictTo (not internal): it appears in public static members of CleverTapAPI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface NotificationRenderedListener {

    public fun onNotificationRendered(isRendered: Boolean)
}
