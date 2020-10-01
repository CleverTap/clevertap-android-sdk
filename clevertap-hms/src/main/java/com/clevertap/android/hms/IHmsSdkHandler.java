package com.clevertap.android.hms;

public interface IHmsSdkHandler {

    /**
     * @return App ID of Huawei Project
     */
    String appId();

    /*
     * Tells whether Huawei Credentials are valid
     */
    boolean isAvailable();

    /*
     * Tells whether Huawei push is supported
     */
    boolean isSupported();

    /**
     * @return Xiaomi Message token
     */
    String onNewToken();

}