package com.clevertap.android.sdk.pushnotification.fcm;

import static com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.interfaces.INotificationParser;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Implementation of {@link INotificationParser}<{@link RemoteMessage}>
 */
class FcmNotificationParser implements INotificationParser<RemoteMessage> {

    @Override
    public Bundle toBundle(@NonNull final RemoteMessage message) {
        try {
            Bundle extras = new Bundle();
            for (Map.Entry<String, String> entry : message.getData().entrySet()) {
                extras.putString(entry.getKey(), entry.getValue());
            }
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Found Valid Notification Message ");
            return extras;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Invalid Notification Message ", e);
        }
        return null;
    }
}
