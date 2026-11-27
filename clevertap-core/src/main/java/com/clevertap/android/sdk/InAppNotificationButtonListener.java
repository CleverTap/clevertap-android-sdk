package com.clevertap.android.sdk;

import java.util.HashMap;

/**
 * Receives the custom key-value pairs of an in-app button whose action is "key-value". Register it
 * with {@link CleverTapAPI#setInAppNotificationButtonListener(InAppNotificationButtonListener)}.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameter as a platform type, so wrapper SDKs
 * and apps can override it with a nullable or non-null type. Do not convert this file to Kotlin.
 */
public interface InAppNotificationButtonListener {

    /**
     * Callback to return a Key Value payload associated with inApp widget click.
     * <p>
     * Called synchronously on the thread that handles the click: the main thread for native in-app
     * templates, but for HTML in-apps (JavaScript bridge) and custom code templates it is the thread
     * that triggered the action, so don't assume the main thread.
     *
     * @param payload the key-value pairs configured on the clicked button
     */
    void onInAppButtonClick(HashMap<String, String> payload);
}
