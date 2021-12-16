package com.clevertap.android.sdk.interfaces;

import android.os.Bundle;
import androidx.annotation.NonNull;

/**
 * Generic Interface to parse different types of notification messages
 * @param <T> Type of notification message
 */
public interface INotificationParser<T> {

    /**
     * Parses notification message to Bundle
     * @param message notification message received from cloud messaging provider like firebase,xiaomi,huawei etc.
     * @return {@link Bundle} object
     */
    Bundle toBundle(@NonNull T message);
}
