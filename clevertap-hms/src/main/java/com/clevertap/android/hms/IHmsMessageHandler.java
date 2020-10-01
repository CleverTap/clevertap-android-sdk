package com.clevertap.android.hms;

import android.content.Context;
import com.huawei.hms.push.RemoteMessage;

/**
 * interface to handle the xiaomi notification message receiver callbacks
 */
public interface IHmsMessageHandler {

    boolean createNotification(Context context, RemoteMessage remoteMessage);

    boolean onNewToken(Context context, String token);
}