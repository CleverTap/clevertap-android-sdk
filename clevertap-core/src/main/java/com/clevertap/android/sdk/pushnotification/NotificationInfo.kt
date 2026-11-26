package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.CleverTapAPI

/**
 * Contains information regarding the notification payload in the FCM intent.
 *
 * Use [CleverTapAPI.getNotificationInfo] (with the notification's [Bundle]) to retrieve this
 * information.
 */
public class NotificationInfo @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    /**
     * Whether or not this notification was sent via CleverTap.
     */
    @JvmField public val fromCleverTap: Boolean,
    /**
     * Whether to parse this notifcation payload or not.
     *
     * True if and only if this notification is from CleverTap, and it should be parsed for information.
     */
    @JvmField public val shouldRender: Boolean
) {

    override fun toString(): String =
        "NotificationInfo{fromCleverTap=$fromCleverTap, shouldRender=$shouldRender}"
}
