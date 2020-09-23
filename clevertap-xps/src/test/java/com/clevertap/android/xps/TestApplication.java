package com.clevertap.android.xps;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

public class TestApplication extends Application {

    public static TestApplication getApplication() {
        return (TestApplication) ApplicationProvider.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}