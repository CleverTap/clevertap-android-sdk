package com.clevertap.android.sdk.pushnotification;

public interface CTPushProviderListener {

    void onNewToken(String token, PushConstants.PushType pushType);

}