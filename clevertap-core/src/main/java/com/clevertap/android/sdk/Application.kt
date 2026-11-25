package com.clevertap.android.sdk

@Suppress("unused")
open class Application : android.app.Application() {

    override fun onCreate() {
        ActivityLifecycleCallback.register(this)
        super.onCreate()
    }
}
