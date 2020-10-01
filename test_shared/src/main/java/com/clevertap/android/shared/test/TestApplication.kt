package com.clevertap.android.shared.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.Application
import com.clevertap.android.sdk.CleverTapAPI

class TestApplication : Application() {

    override fun onCreate() {
        CleverTapAPI.setUIEditorConnectionEnabled(true)
        CleverTapAPI.setDebugLevel(3)
        ActivityLifecycleCallback.register(this)
        super.onCreate()
    }

    companion object {

        val application: TestApplication
            get() = ApplicationProvider.getApplicationContext<Context>() as TestApplication
    }
}