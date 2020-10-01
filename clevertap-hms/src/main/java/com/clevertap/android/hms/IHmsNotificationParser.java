package com.clevertap.android.hms;

import android.os.Bundle;
import com.huawei.hms.push.RemoteMessage;

/**
 * Impl converts the RemoteMessage to bundle
 */
public interface IHmsNotificationParser {

    /**
     * @param message - Huawei message
     * @return bundle with the message content, in case of invalid message returns null
     */
    Bundle toBundle(RemoteMessage message);
}