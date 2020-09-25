package com.clevertap.android.xps;

import androidx.annotation.RestrictTo;

@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IMiSdkHandler {

    String appId();

    String appKey();

    boolean isAvailable();

    String onNewToken();
}