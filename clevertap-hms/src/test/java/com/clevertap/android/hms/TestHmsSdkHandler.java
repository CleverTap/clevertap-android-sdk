package com.clevertap.android.hms;

public class TestHmsSdkHandler implements IHmsSdkHandler {
    private boolean isAvailable;
    private boolean isSupported;

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @Override
    public String onNewToken() {
        return HmsTestConstants.HMS_TOKEN;
    }

    @Override
    public String appId() {
        return isAvailable ? HmsTestConstants.HMS_APP_ID : HmsTestConstants
                .EMPTY_STRING;
    }

    @Override
    public boolean isSupported() {
        return isSupported;
    }

    public void setSupported(boolean supported) {
        isSupported = supported;
    }
}