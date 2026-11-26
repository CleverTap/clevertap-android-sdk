package com.clevertap.android.sdk.interfaces

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.pushnotification.PushType

public interface NotificationHandler {

    /**
     * @param applicationContext - application context
     * @param message            - notification message from cloud messaging owners
     */
    public fun onMessageReceived(applicationContext: Context?, message: Bundle?, pushType: String?): Boolean

    /**
     * @param applicationContext - application context
     * @param token              - token received from cloud messaging owners
     */
    public fun onNewToken(applicationContext: Context?, token: String?, pushType: PushType?): Boolean
}
