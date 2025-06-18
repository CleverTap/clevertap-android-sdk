package com.clevertap.demo.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Activity tracker to keep track of the current foreground activity
 */
object ActivityTracker : Application.ActivityLifecycleCallbacks {
    
    private var currentActivity: WeakReference<Activity>? = null
    
    val currentForegroundActivity: Activity?
        get() = currentActivity?.get()
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = WeakReference(activity)
    }
    
    override fun onActivityStarted(activity: Activity) {
        // Activity is becoming visible
    }
    
    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
    }
    
    override fun onActivityPaused(activity: Activity) {
        // Activity is no longer in the foreground
    }
    
    override fun onActivityStopped(activity: Activity) {
        // Activity is no longer visible
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Activity state is being saved
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity?.get() == activity) {
            currentActivity = null
        }
    }
}
