package com.clevertap.android.sdk;

import androidx.test.core.app.ApplicationProvider;

public class TestApplication extends Application {
    public ActivityLifecycleCallbacks callback;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        super.registerActivityLifecycleCallbacks(callback);
        this.callback = callback;
    }

    public static TestApplication getApplication() {
        return (TestApplication) ApplicationProvider.getApplicationContext();
    }
}
