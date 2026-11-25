package com.clevertap.android.sdk.interfaces;

import android.content.Context;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

public interface AudibleNotification {
    NotificationCompat.Builder setSound(
            Context context,
            Bundle extras,
            NotificationCompat.Builder nb, CleverTapInstanceConfig config
    );

}
