package com.clevertap.android.sdk.interfaces;

import android.content.Context;
import android.os.Bundle;

public interface NotificationHandler {

    /**
     * @param applicationContext - application context
     * @param message            - notification message from cloud messaging owners
     */
    boolean onMessageReceived(final Context applicationContext, Bundle message, final String pushType);

    /**
     * @param applicationContext - application context
     * @param token              - token received from cloud messaging owners
     */
    boolean onNewToken(Context applicationContext, String token, final String pushType);
}
