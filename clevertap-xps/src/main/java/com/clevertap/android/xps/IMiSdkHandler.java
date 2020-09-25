package com.clevertap.android.xps;

import androidx.annotation.RestrictTo;

@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IMiSdkHandler {

    String onNewToken();

    String appKey();

    String appId();

    boolean isAvailable();
}