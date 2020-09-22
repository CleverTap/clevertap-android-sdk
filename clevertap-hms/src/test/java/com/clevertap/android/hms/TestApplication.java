package com.clevertap.android.hms;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static TestApplication getApplication() {
        return (TestApplication) ApplicationProvider.getApplicationContext();
    }
}