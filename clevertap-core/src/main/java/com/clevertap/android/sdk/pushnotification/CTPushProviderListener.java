package com.clevertap.android.sdk.pushnotification;

import com.clevertap.android.sdk.BaseCTApiListener;

public interface CTPushProviderListener extends BaseCTApiListener {

    void onNewToken(String token, PushConstants.PushType pushType);

}