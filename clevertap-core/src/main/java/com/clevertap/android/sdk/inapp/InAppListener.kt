package com.clevertap.android.sdk.inapp

import android.content.Context
import android.os.Bundle

internal interface InAppListener {

    fun inAppNotificationDidClick(
        inAppNotification: CTInAppNotification,
        button: CTInAppNotificationButton,
        activityContext: Context?
    ): Bundle?

    fun inAppNotificationDidDismiss(inAppNotification: CTInAppNotification, formData: Bundle?)
    fun inAppNotificationDidShow(inAppNotification: CTInAppNotification, formData: Bundle?)
    fun inAppNotificationActionTriggered(
        inAppNotification: CTInAppNotification,
        action: CTInAppAction,
        callToAction: String,
        additionalData: Bundle?,
        activityContext: Context?
    ): Bundle?
}
