package com.clevertap.android.sdk

import java.util.HashMap

public fun interface InAppNotificationButtonListener {

    /**
     * Callback to return a Key Value payload associated with inApp widget click.
     */
    public fun onInAppButtonClick(payload: HashMap<String, String>?)
}
