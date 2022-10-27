package com.clevertap.android.sdk.inapp;

import android.content.Context;
import android.os.Bundle;
import java.util.HashMap;

public interface InAppListener {

    void inAppNotificationDidClick(CTInAppNotification inAppNotification, Bundle formData,
            HashMap<String, String> keyValueMap);

    void inAppNotificationDidDismiss(Context context, CTInAppNotification inAppNotification, Bundle formData);

    void inAppNotificationDidShow(CTInAppNotification inAppNotification, Bundle formData);
}