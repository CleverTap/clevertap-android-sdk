package com.clevertap.android.hms;

import android.content.Context;
import com.huawei.hms.push.RemoteMessage;

/**
 * interface to handle the huawei notification message service callbacks
 */
public interface IHmsMessageHandler {

    /**
     * @param context       - application context
     * @param remoteMessage - Huawei Remote Message
     * @return true if everything is fine & notification is rendered successfully
     */
    boolean createNotification(Context context, RemoteMessage remoteMessage);

    /**
     * @param context - application context
     * @param token   - fcm token received from Huawei SDK
     * @return true if the token is sent to Clevertap's server
     */

    boolean onNewToken(Context context, String token);
}