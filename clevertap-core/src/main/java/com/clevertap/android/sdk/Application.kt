package com.clevertap.android.sdk

/**
 * Convenience [android.app.Application] that calls [ActivityLifecycleCallback.register] in
 * [onCreate] (before `super.onCreate()`), so the SDK receives activity lifecycle events without any
 * code. Declare it (or a subclass) as `android:name` of the `<application>` tag in the manifest. If
 * you already have your own Application class, call [ActivityLifecycleCallback.register] from its
 * `onCreate` instead.
 */
@Suppress("unused")
public open class Application : android.app.Application() {

    override fun onCreate() {
        ActivityLifecycleCallback.register(this)
        super.onCreate()
    }
}
