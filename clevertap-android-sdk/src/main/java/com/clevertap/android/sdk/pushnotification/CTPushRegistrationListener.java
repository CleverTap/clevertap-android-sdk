package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CTPushRegistrationListener {
    void onComplete(String token);
}