package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle

interface INotificationBundleManipulation<T> {

    fun addPriority(message: T): INotificationBundleManipulation<T>

    // Other methods for manipulating different attributes...

    fun build(): Bundle
}