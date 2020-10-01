package com.clevertap.android.xps;

import androidx.annotation.RestrictTo;

/**
 * bridge interface to communicate with the Xiaomi SDK to check for availability, tokens etc.
 */
@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IMiSdkHandler {

    /**
     * @return App ID of Xiaomi Project
     */
    String appId();

    /**
     * @return App key of Xiaomi Project
     */
    String appKey();

    /*
     * Tells whether Xiaomi Credentials are valid
     */
    boolean isAvailable();

    /**
     * @return Xiaomi Message token
     */
    String onNewToken();
}