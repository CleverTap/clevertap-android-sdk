package com.clevertap.android.hms;

public interface IHmsSdkHandler {

    String onNewToken();

    String appId();

    boolean isSupported();

}