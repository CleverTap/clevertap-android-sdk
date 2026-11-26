package com.clevertap.android.sdk.interfaces

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapInstanceConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface AudibleNotification {
    public fun setSound(
        context: Context,
        extras: Bundle,
        nb: NotificationCompat.Builder,
        config: CleverTapInstanceConfig
    ): NotificationCompat.Builder
}
