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
     * Creates notification from Firebase Remote message
     * @param applicationContext - application context
     * @param message            - Firebase Remote message
     * @return true if everything is fine & notification is rendered successfully
     */
    boolean createNotification(final Context applicationContext, RemoteMessage message);

    /**
     * Processes new token from Firebase
     * @param applicationContext - application context
     * @param token              - fcm token received from Firebase SDK
     * @return true if the token is sent to Clevertap's server
     */
    boolean onNewToken(Context applicationContext, String token);

}