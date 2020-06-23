package com.clevertap.android.sdk;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FcmMessageListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message){
        try {
            if (message.getData().size() > 0) {
                Bundle extras = new Bundle();
                for (Map.Entry<String, String> entry : message.getData().entrySet()) {
                    extras.putString(entry.getKey(), entry.getValue());
                }

                NotificationInfo info = CleverTapAPI.getNotificationInfo(extras);

                if (info.fromCleverTap) {
                    Logger.d("FcmMessageListenerService received notification from CleverTap: " + extras.toString());
                    CleverTapAPI.createNotification(getApplicationContext(), extras);
                }
            }
        } catch (Throwable t) {
            Logger.d("Error parsing FCM message", t);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Logger.d("New token received from FCM - "+token);
        CleverTapAPI.pushNewToken(getApplicationContext(),token);
    }
}
