package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface INotificationBundleManipulation<T> {

    fun addPriority(message: T): INotificationBundleManipulation<T>

    // Other methods for manipulating different attributes...

    fun build(): Bundle
}
