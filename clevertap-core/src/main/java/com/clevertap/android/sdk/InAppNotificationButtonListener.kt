package com.clevertap.android.sdk

import java.util.HashMap

fun interface InAppNotificationButtonListener {

    /**
     * Callback to return a Key Value payload associated with inApp widget click.
     */
    fun onInAppButtonClick(payload: HashMap<String, String>?)
}
