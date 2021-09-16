package com.clevertap.android.sdk.pushnotification;

import android.content.Context;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

public interface INotificationRenderer {

    Object getCollapseKey(final Bundle extras);

    String getMessage(Bundle extras);

    String getTitle(Bundle extras, final Context context);

    NotificationCompat.Builder renderNotification(final Bundle extras, final Context context,
            final Builder nb, final CleverTapInstanceConfig config, final int notificationId);

    void setSmallIcon(int smallIcon, final Context context);
}
