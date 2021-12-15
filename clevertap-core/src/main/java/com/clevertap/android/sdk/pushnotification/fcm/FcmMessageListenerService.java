package com.clevertap.android.sdk.pushnotification.fcm;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Clevertap's Implementation for Firebase Message service
 */
public class FcmMessageListenerService extends FirebaseMessagingService {

    private IFcmMessageHandler mHandler = new CTFcmMessageHandler();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        mHandler.createNotification(getApplicationContext(), message);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        mHandler.onNewToken(getApplicationContext(), token);
    }
}
