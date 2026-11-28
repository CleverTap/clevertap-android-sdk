package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.inapp.CTInAppNotification;

import java.util.Map;

/**
 * A listener for in-app notifications.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameters carry
 * androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin override
 * must declare the same nullability. Do not convert this file to Kotlin.
 */
public interface InAppNotificationListener {

    /**
     * This is called when an in-app notification is about to be rendered.
     * If you'd like this notification to not be rendered, then return false.
     * <p>
     * Returning true will cause this notification to be rendered immediately.
     *
     * @param extras The extra key/value pairs set in the CleverTap dashboard for this notification
     * @return True to show this notification immediately, false to not show this notification
     */
    boolean beforeShow(@NonNull Map<String, Object> extras);

    /**
     * This is called when an in-app notification is rendered.
     *
     * @param ctInAppNotification The CTInAppNotification object for this notification.
     * {@link CTInAppNotification} object
     */
    void onShow(@NonNull CTInAppNotification ctInAppNotification);

    /**
     * When an in-app notification is dismissed (either by the close button, or a call to action),
     * this method will be called.
     *
     * @param extras       The extra key/value pairs set in the CleverTap dashboard for this notification
     * @param actionExtras The extra key/value pairs from the notification
     *                     (for example, a rating widget might have some properties which can be read here)
     *                     <p>
     *                     Note: This can be null if the notification was dismissed without taking any action
     */
    void onDismissed(@NonNull Map<String, Object> extras, @Nullable Map<String, Object> actionExtras);
}
