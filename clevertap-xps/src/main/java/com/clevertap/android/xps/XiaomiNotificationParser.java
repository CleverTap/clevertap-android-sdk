package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.xps.XpsConstants.XIAOMI_LOG_TAG;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.interfaces.INotificationParser;
import com.xiaomi.mipush.sdk.MiPushMessage;

/**
 * Implementation of {@link INotificationParser}<{@link MiPushMessage}>
 */
public class XiaomiNotificationParser implements INotificationParser<MiPushMessage> {

    @Override
    public Bundle toBundle(@NonNull final MiPushMessage message) {
        try {
            Bundle extras = Utils.stringToBundle(message.getContent());
            Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Found Valid Notification Message ");
            return extras;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Invalid Notification Message ", e);
        }
        return null;
    }
}