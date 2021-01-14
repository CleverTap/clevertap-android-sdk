package com.clevertap.android.sdk;

import android.os.Handler;
import android.os.Looper;

class MainLooperHandler {
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Returns the generic handler object which is used to post
     * runnables. The returned value will never be null.
     *
     * @return The generic handler
     * @see Handler
     */
    Handler getMainLooperHandler() {
        return mHandler;
    }
}