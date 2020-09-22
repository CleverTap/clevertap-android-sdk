package com.clevertap.android.shared.test;

import androidx.test.core.app.ApplicationProvider;

import com.clevertap.android.sdk.ActivityLifecycleCallback;
import com.clevertap.android.sdk.Application;
import com.clevertap.android.sdk.CleverTapAPI;

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