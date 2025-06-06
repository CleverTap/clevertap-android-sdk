package com.clevertap.android.geofence

import android.app.Application
import androidx.test.core.app.ApplicationProvider

class TestApplication : Application() {

    companion object {
        val application: TestApplication
            get() = ApplicationProvider.getApplicationContext<Application>() as TestApplication
    }
}
