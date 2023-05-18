package com.clevertap.android.sdk.pushsdk;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.interfaces.INotificationParser;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Implementation of {@link INotificationParser}<{@link RemoteMessage}>
 */
class FcmNotificationParser {

    private static final String TAG = "FcmNotificationParser";

    public static @NonNull Bundle toBundle(@NonNull final RemoteMessage message) {
        Bundle extras = new Bundle();
        try {
            for (Map.Entry<String, String> entry : message.getData().entrySet()) {
                extras.putString(entry.getKey(), entry.getValue());
            }
            Logger.d(TAG, "Found Valid Notification Message ");
            return extras;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.d(TAG, "Invalid Notification Message ", e);
        }
        return extras;
    }
}
