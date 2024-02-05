package com.clevertap.android.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

/**
 * Class for handling activity lifecycle events
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ActivityLifecycleCallback {

    public static boolean registered = false;
    private static String cleverTapId = null;
    private static final Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            if (cleverTapId != null) {
                CleverTapAPI.onActivityCreated(activity, cleverTapId);
            } else {
                CleverTapAPI.onActivityCreated(activity);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
            CleverTapAPI.onActivityPaused();
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (cleverTapId != null) {
                CleverTapAPI.onActivityResumed(activity, cleverTapId);
            } else {
                CleverTapAPI.onActivityResumed(activity);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }
    };

    /**
     * Enables lifecycle callbacks for Android devices
     *
     * @param application App's Application object
     * @param cleverTapID Custom CleverTap ID
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
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

    /**
     * Enables lifecycle callbacks for Android devices
     *
     * @param application App's Application object
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void register(android.app.Application application) {
        register(application, null);
    }
}
