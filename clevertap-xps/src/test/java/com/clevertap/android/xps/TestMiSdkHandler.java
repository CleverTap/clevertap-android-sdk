package com.clevertap.android.xps;

public class TestMiSdkHandler implements IMiSdkHandler {
    private boolean isAvailable;

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @Override
    public String onNewToken() {
        return XpsTestConstants.MI_TOKEN;
    }

    @Override
    public String appKey() {
        return isAvailable ? XpsTestConstants.MI_APP_KEY : XpsTestConstants
                .EMPTY_STRING;
    }

    @Override
    public String appId() {
        return isAvailable ? XpsTestConstants.MI_APP_ID : XpsTestConstants
                .EMPTY_STRING;
    }
}