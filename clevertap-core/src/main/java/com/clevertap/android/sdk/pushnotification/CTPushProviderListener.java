package com.clevertap.android.sdk.pushnotification;

import com.clevertap.android.sdk.BaseCTApiListener;

public interface CTPushProviderListener extends BaseCTApiListener {

    void log(String tag, String message);

    void log(String tag, String message, Throwable throwable);

    void onNewToken(String token, PushConstants.PushType pushType);
}