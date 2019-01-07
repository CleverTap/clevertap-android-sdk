package com.clevertap.android.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

/**
 * Class for handling activity lifecycle events
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ActivityLifecycleCallback {
    static boolean registered = false;

    /**
     * Enables lifecycle callbacks for Android devices
     * @param application App's Application object
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static synchronized void register(android.app.Application application) {
        if (application == null) {
            Logger.i("Application instance is null/system API is too old");
            return;
        }

        if (registered) {
            Logger.v("Lifecycle callbacks have already been registered");
            return;
        }

        registered = true;
        application.registerActivityLifecycleCallbacks(
                new android.app.Application.ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityCreated(Activity activity, Bundle bundle) {
                        CleverTapAPI.onActivityCreated(activity);
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {}

                    @Override
                    public void onActivityResumed(Activity activity) {
                       CleverTapAPI.onActivityResumed(activity);
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        CleverTapAPI.onActivityPaused();
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {}

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

                    @Override
                    public void onActivityDestroyed(Activity activity) {}
                }

        );
        Logger.i("Activity Lifecycle Callback successfully registered");
    }
}
