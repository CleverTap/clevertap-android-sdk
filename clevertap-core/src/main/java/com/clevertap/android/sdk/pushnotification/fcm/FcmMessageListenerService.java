package com.clevertap.android.sdk.pushnotification.fcm;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Clevertap's Implementation for Firebase Message service
 */
public class FcmMessageListenerService extends FirebaseMessagingService {

    private IFcmMessageHandler mHandler = new FcmMessageHandlerImpl();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        mHandler.onMessageReceived(getApplicationContext(), message);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        mHandler.onNewToken(getApplicationContext(), token);
    }
}
