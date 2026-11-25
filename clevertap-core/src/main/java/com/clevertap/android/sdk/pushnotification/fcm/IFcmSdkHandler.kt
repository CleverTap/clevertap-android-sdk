package com.clevertap.android.sdk.pushnotification.fcm

import com.clevertap.android.sdk.pushnotification.PushType

/**
 * Bridge interface to communicate with Firebase SDK
 */
internal interface IFcmSdkHandler {

    /**
     * @return pushType of FCM
     */
    val pushType: PushType

    /**
     * @return true if FCM credentials are properly available
     */
    val isAvailable: Boolean

    /**
     * @return true if Firebase messaging is supported
     */
    val isSupported: Boolean

    /**
     * Call this method to request token from Firebase SDK
     */
    fun requestToken()

}
