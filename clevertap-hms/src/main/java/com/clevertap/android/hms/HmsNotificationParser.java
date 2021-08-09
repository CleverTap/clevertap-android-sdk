package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.interfaces.INotificationParser;
import com.huawei.hms.push.RemoteMessage;

/**
 * Implementation of {@link INotificationParser}<{@link RemoteMessage}>
 */
public class HmsNotificationParser implements INotificationParser<RemoteMessage> {

    @Override
    public Bundle toBundle(@NonNull final RemoteMessage message) {
        try {
            Bundle extras = Utils.stringToBundle(message.getData());
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Found Valid Notification Message ");
            return extras;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Invalid Notification Message ", e);
        }
        return null;
    }
}