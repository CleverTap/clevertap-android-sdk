package com.clevertap.android.sdk.interfaces;

import android.content.Context;
import androidx.annotation.NonNull;

/**
 * Generic Interface to handle Pull Notifications for different types of notification messages, received from
 * respective services or receivers(ex. FirebaseMessagingService).
 * <br><br>
 * Implement this interface if you want to support push amp for different types of notification messages.
 * @param <T> Type of notification message
 */
public interface IPushAmpHandler<T> {

    /**
     *  Processes notification message for Pull Notifications
     * @param context application context
     * @param message notification message received from cloud messaging provider like firebase,xiaomi,huawei etc.
     */
    void processPushAmp(final Context context, @NonNull final T message);
}
