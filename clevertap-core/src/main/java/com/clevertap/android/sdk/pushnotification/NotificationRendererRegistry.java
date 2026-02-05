package com.clevertap.android.sdk.pushnotification;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.CleverTapAPI;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for custom notification renderers.
 * Allows apps to register custom INotificationRenderer implementations
 * that can be used based on keys in the notification payload.
 */
public class NotificationRendererRegistry {

    private static final Map<String, INotificationRenderer> rendererMap = new HashMap<>();
    private static final Object lock = new Object();

    /**
     * Register a custom notification renderer with a key.
     * The renderer will be used when a notification contains the specified key.
     *
     * @param key The key to identify when to use this renderer (e.g., "custom_style", "premium_notif")
     * @param renderer The custom renderer implementation
     */
    public static void registerRenderer(@NonNull String key, @NonNull INotificationRenderer renderer) {
        synchronized (lock) {
            rendererMap.put(key, renderer);
        }
    }

    /**
     * Unregister a custom notification renderer.
     *
     * @param key The key of the renderer to unregister
     */
    public static void unregisterRenderer(@NonNull String key) {
        synchronized (lock) {
            rendererMap.remove(key);
        }
    }

    /**
     * Get a renderer for the given notification extras.
     * Checks if the notification contains a "wzrk_custom_renderer" key
     * and returns the registered renderer if found.
     *
     * @param extras The notification extras bundle
     * @return The custom renderer if found, null otherwise
     */
    @Nullable
    static INotificationRenderer getRendererForNotification(@Nullable Bundle extras) {
        if (extras == null) {
            return null;
        }

        synchronized (lock) {
            // Check for custom renderer key in the notification payload
            String customRendererKey = extras.getString("wzrk_custom_renderer");
            if (customRendererKey != null && !customRendererKey.isEmpty()) {
                return rendererMap.get(customRendererKey);
            }

            // Check alternative key for backward compatibility
            customRendererKey = extras.getString("ct_custom_renderer");
            if (customRendererKey != null && !customRendererKey.isEmpty()) {
                return rendererMap.get(customRendererKey);
            }
        }

        return null;
    }

    /**
     * Clear all registered renderers.
     * Useful for testing or cleanup.
     */
    public static void clearAll() {
        synchronized (lock) {
            rendererMap.clear();
        }
    }

    /**
     * Check if a renderer is registered for the given key.
     *
     * @param key The key to check
     * @return true if a renderer is registered for this key
     */
    public static boolean hasRenderer(@NonNull String key) {
        synchronized (lock) {
            return rendererMap.containsKey(key);
        }
    }
}
