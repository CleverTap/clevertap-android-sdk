package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.huawei.hms.push.RemoteMessage;

/**
 * Implementation of {@link IHmsNotificationParser}
 */
public class HmsNotificationParser implements IHmsNotificationParser {

    @Override
    public Bundle toBundle(final RemoteMessage message) {
        try {
            Bundle extras = Utils.stringToBundle(message.getData());
            NotificationInfo info = CleverTapAPI.getNotificationInfo(extras);
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Found Valid Notification Message ");
            return info.fromCleverTap ? extras : null;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Invalid Notification Message ", e);
        }
        return null;
    }
}