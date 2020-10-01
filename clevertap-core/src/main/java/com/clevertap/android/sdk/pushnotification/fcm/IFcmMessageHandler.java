package com.clevertap.android.sdk.pushnotification.fcm;

import android.content.Context;
import androidx.annotation.RestrictTo;
import com.google.firebase.messaging.RemoteMessage;

/**
 * interface to handle the Firebase notification service receiver callbacks
 */
@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IFcmMessageHandler {

    /**
     * @param applicationContext - application context
     * @param message            - Firebase Remote message
     * @return true if everything is fine & notification is rendered successfully
     */
    boolean onMessageReceived(final Context applicationContext, RemoteMessage message);

    /**
     * @param applicationContext - application context
     * @param token              - fcm token received from Firebase SDK
     * @return true if the token is sent to Clevertap's server
     */

    boolean onNewToken(Context applicationContext, String token);

}