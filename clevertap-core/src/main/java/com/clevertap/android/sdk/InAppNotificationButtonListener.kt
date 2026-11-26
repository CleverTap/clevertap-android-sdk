package com.clevertap.android.sdk

import java.util.HashMap

/**
 * Receives the custom key-value pairs of an in-app button whose action is "key-value". Register it
 * with [CleverTapAPI.setInAppNotificationButtonListener].
 */
public fun interface InAppNotificationButtonListener {

    /**
     * Callback to return a Key Value payload associated with inApp widget click.
     *
     * Called synchronously on the thread that handles the click: the main thread for native in-app
     * templates, but for HTML in-apps (JavaScript bridge) and custom code templates it is the thread
     * that triggered the action, so don't assume the main thread.
     *
     * @param payload the key-value pairs configured on the clicked button
     */
    public fun onInAppButtonClick(payload: HashMap<String, String>?)
}
