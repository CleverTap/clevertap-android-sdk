package com.clevertap.android.geofence;

import android.app.Application;
import androidx.test.core.app.ApplicationProvider;


public class TestApplication extends Application {

    public static Application getApplication() {
        return ApplicationProvider.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
