package com.clevertap.android.sdk.pushnotification;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

public interface INotificationRenderer {

    @Nullable Object getCollapseKey(final Bundle extras);

    @Nullable String getMessage(Bundle extras);

    @Nullable String getTitle(Bundle extras, final Context context);

    @Nullable NotificationCompat.Builder renderNotification(final Bundle extras, final Context context,
            final Builder nb, final CleverTapInstanceConfig config, final int notificationId);

    void setSmallIcon(int smallIcon, final Context context);
}
