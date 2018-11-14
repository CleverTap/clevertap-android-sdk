package com.clevertap.android.sdk;

import android.os.Bundle;
import com.google.android.gms.gcm.GcmListenerService;

@Deprecated
public class GcmMessageListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle extras){
        try {
            NotificationInfo info = CleverTapAPI.getNotificationInfo(extras);
            if (info.fromCleverTap) {
                Logger.d("GcmMessageListenerService received notification from CleverTap: " + extras.toString());
                CleverTapAPI.createNotification(getApplicationContext(), extras);
            }
        } catch (Throwable t) {
            Logger.d("Error handling GCM message", t);
        }
    }
}
