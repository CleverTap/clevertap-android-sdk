package com.clevertap.android.sdk;

import androidx.test.core.app.ApplicationProvider;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        CleverTapAPI.setUIEditorConnectionEnabled(true);
        CleverTapAPI.setDebugLevel(3);
        ActivityLifecycleCallback.register(this);
        super.onCreate();
    }

    public static TestApplication getApplication() {
        return (TestApplication) ApplicationProvider.getApplicationContext();
    }
}