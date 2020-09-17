package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.BaseCTApiListener;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface CTApiPushListener extends BaseCTApiListener {

    void onNewToken(String freshToken, PushConstants.PushType pushType);

    void pushDeviceTokenEvent(String token, boolean register, PushConstants.PushType pushType);
}