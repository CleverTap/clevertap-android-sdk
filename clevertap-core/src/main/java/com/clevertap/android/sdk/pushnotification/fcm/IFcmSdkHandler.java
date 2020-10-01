package com.clevertap.android.sdk.pushnotification.fcm;

import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;

/**
 * Bridge interface to communicate with Firebase SDK
 */
@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IFcmSdkHandler {

    /**
     * @return pushType of FCM
     */
    PushType getPushType();

    /**
     * @return true if FCM credentials are properly available
     */
    boolean isAvailable();

    /**
     * @return true if Firebase messaging is supported
     */
    boolean isSupported();

    /**
     * Call this method to request token from Firebase SDK
     */
    void requestToken();

}