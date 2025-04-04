package com.clevertap.android.sdk.pushnotification

import android.app.PendingIntent

// Data class for action button information
data class ActionButton(
    val label: String,
    val icon: Int,
    val pendingIntent: PendingIntent
)

