package com.clevertap.android.sdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.pushnotification.PushType;

import java.util.List;

/**
 * Class for handling activity lifecycle events
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ActivityLifecycleCallback {

    public static boolean registered = false;
    private static String cleverTapId = null;
    private static List<PushType> pushTypes = null;
    private static final Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
            CleverTapAPI.onActivityCreated(activity, cleverTapId, pushTypes);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            CleverTapAPI.onActivityPaused();
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            if (cleverTapId != null) {
                CleverTapAPI.onActivityResumed(activity, cleverTapId, pushTypes);
            } else {
                CleverTapAPI.onActivityResumed(activity);
            }
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }
    };

    /**
     * Enables lifecycle callbacks for Android devices
     *
     * @param application App's Application object
     */
    public static void register(android.app.Application application) {
        register(application, null);
    }

    /**
     * Enables lifecycle callbacks for Android devices
     *
     * @param application App's Application object
     * @param cleverTapID Custom CleverTap ID
     */
    public static void register(android.app.Application application, final String cleverTapID) {
        if (application == null) {
            Logger.i("Application instance is null/system API is too old");
            return;
        }

        if (registered) {
            Logger.v("Lifecycle callbacks have already been registered");
            return;
        }

        cleverTapId = cleverTapID;
        registered = true;

        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        Logger.i("Activity Lifecycle Callback successfully registered");
    }

    public static void register(
            android.app.Application application,
            final String cleverTapID,
            final List<PushType> pushTypes
    ) {
        if (application == null) {
            Logger.i("Application instance is null/system API is too old");
            return;
        }

        if (registered) {
            Logger.v("Lifecycle callbacks have already been registered");
            return;
        }

        ActivityLifecycleCallback.cleverTapId = cleverTapID;
        ActivityLifecycleCallback.pushTypes = pushTypes;
        ActivityLifecycleCallback.registered = true;

        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        Logger.i("Activity Lifecycle Callback successfully registered");
    }
}
