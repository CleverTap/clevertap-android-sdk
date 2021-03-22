package com.clevertap.android.sdk.task;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

@RestrictTo(Scope.LIBRARY)
public class MainLooperHandler extends Handler {

    public Runnable getPendingRunnable() {
        return pendingRunnable;
    }

    private Runnable pendingRunnable = null;

    public void setPendingRunnable(final Runnable pendingRunnable) {
        this.pendingRunnable = pendingRunnable;
    }
    public MainLooperHandler() {
        super(Looper.getMainLooper());
    }
}