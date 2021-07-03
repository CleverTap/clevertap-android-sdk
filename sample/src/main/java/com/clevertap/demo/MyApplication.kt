package com.clevertap.demo

import android.app.NotificationManager
import androidx.multidex.MultiDexApplication
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI

class MyApplication : MultiDexApplication() {

    override fun onCreate() {
        CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.VERBOSE)
        ActivityLifecycleCallback.register(this)
        super.onCreate()
        CleverTapAPI.createNotificationChannel(
            this, "BRTesting", "Offers",
            "All Offers", NotificationManager.IMPORTANCE_MAX, true
        )
    }
}