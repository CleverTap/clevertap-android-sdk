package com.clevertap.android.sdk.pushnotification.fcm;

import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;

@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IFcmSdkHandler {

    PushType getPushType();

    boolean isAvailable();

    boolean isSupported();

    void requestToken();

}