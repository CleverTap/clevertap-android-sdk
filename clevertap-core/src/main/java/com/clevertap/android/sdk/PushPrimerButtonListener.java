package com.clevertap.android.sdk;

import com.clevertap.android.sdk.inapp.CTInAppNotification;

public interface PushPrimerButtonListener {

    void onPositiveButtonClick(CTInAppNotification ctInAppNotification);

    void onNegativeButtonClick(CTInAppNotification ctInAppNotification);
}
