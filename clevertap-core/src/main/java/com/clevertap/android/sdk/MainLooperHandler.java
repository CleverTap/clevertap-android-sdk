package com.clevertap.android.sdk;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

//ToDO move this a single Task manager
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