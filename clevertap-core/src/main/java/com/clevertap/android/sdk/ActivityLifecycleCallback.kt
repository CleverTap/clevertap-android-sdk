package com.clevertap.android.sdk

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Class for handling activity lifecycle events
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
public class ActivityLifecycleCallback {

    public companion object {

        /**
         * `true` once [register] has attached the SDK's lifecycle callbacks to the [Application];
         * further [register] calls are then ignored. Set by the SDK — read it, don't write it.
         */
        @JvmField
        public var registered: Boolean = false

        private var cleverTapId: String? = null

        private val lifecycleCallbacks: Application.ActivityLifecycleCallbacks =
            object : Application.ActivityLifecycleCallbacks {

                override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                    CleverTapAPI.onActivityCreated(activity, cleverTapId)
                }

                override fun onActivityDestroyed(activity: Activity) {
                    // No-op: CleverTap only tracks created, resumed and paused.
                }

                override fun onActivityPaused(activity: Activity) {
                    CleverTapAPI.onActivityPaused()
                }

                override fun onActivityResumed(activity: Activity) {
                    CleverTapAPI.onActivityResumed(activity, cleverTapId)
                }

                override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
                    // No-op: CleverTap only tracks created, resumed and paused.
                }

                override fun onActivityStarted(activity: Activity) {
                    // No-op: CleverTap only tracks created, resumed and paused.
                }

                override fun onActivityStopped(activity: Activity) {
                    // No-op: CleverTap only tracks created, resumed and paused.
                }
            }

        /**
         * Enables lifecycle callbacks for Android devices
         *
         * @param application App's Application object
         */
        @JvmStatic
        public fun register(application: Application?) {
            register(application, null)
        }

        /**
         * Enables lifecycle callbacks for Android devices
         *
         * @param application App's Application object
         * @param cleverTapID Custom CleverTap ID
         */
        @JvmStatic
        public fun register(application: Application?, cleverTapID: String?) {
            if (application == null) {
                Logger.i("Application instance is null/system API is too old")
                return
            }

            if (registered) {
                Logger.v("Lifecycle callbacks have already been registered")
                return
            }

            cleverTapId = cleverTapID
            registered = true

            application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            Logger.i("Activity Lifecycle Callback successfully registered")
        }
    }
}
