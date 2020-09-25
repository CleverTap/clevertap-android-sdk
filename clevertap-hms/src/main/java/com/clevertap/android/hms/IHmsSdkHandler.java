package com.clevertap.android.hms;

public interface IHmsSdkHandler {

    String appId();

    boolean isSupported();

    String onNewToken();

    boolean isAvailable();

}