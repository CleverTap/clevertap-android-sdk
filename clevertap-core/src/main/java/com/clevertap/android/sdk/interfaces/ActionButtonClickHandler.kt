package com.clevertap.android.sdk.interfaces

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ActionButtonClickHandler : NotificationHandler {

    fun onActionButtonClick(context: Context?, extras: Bundle?, notificationId: Int): Boolean
}
