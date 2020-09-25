package com.clevertap.demo;

import android.app.Application;
import com.clevertap.android.sdk.ActivityLifecycleCallback;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        ActivityLifecycleCallback.register(this);
        super.onCreate();
    }
}
