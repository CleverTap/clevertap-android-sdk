package com.clevertap.android.hms

/**
 * Bridge interface to communicate with Huawei SDK
 */
internal interface IHmsSdkHandler {

    /**
     * @return App ID of Huawei Project
     */
    fun appId(): String?

    /*
     * Tells whether Huawei Credentials are valid
     */
    val isAvailable: Boolean

    /*
     * Tells whether Huawei push is supported
     */
    val isSupported: Boolean

    /**
     * @return Xiaomi Message token
     */
    fun onNewToken(): String?
}
