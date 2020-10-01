package com.clevertap.android.xps;

import androidx.annotation.RestrictTo;

/**
 * Bridge interface to communicate with the Xiaomi SDK
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

    /**
     * @return true if Xiaomi credentials are properly available
     */
    boolean isAvailable();

    /**
     * @return Xiaomi Message token
     */
    String onNewToken();
}