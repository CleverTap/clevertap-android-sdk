package com.clevertap.android.sdk

@Suppress("unused")
public open class Application : android.app.Application() {

    override fun onCreate() {
        ActivityLifecycleCallback.register(this)
        super.onCreate()
    }
}
