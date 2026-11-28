package com.clevertap.android.sdk;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Receives the custom key-value pairs of an in-app button whose action is "key-value". Register it
 * with {@link CleverTapAPI#setInAppNotificationButtonListener(InAppNotificationButtonListener)}.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameter
 * carries androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin
 * override must declare the same nullability. Do not convert this file to Kotlin.
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
    void onInAppButtonClick(@NonNull HashMap<String, String> payload);
}
