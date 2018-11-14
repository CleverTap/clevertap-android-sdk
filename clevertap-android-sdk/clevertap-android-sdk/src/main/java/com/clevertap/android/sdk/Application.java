package com.clevertap.android.sdk;

@SuppressWarnings({"unused"})
public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        ActivityLifecycleCallback.register(this);
        super.onCreate();
    }
}
