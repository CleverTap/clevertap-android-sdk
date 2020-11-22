package com.clevertap.android.sdk.login;

import com.clevertap.android.sdk.BaseCTApiListener;

public class NonDefaultProfileHandlerImpl extends BaseProfilerHandler {

    public NonDefaultProfileHandlerImpl(BaseCTApiListener baseCTApiListener) {
        super(baseCTApiListener);
    }

    @Override
    String[] fetchConfig() {
        return mConfig.getProfileKeys();
    }
}