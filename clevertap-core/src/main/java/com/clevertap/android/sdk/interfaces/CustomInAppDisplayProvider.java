package com.clevertap.android.sdk.interfaces;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.inapp.CTInAppAction;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton;

//
// CustomInAppDisplayProvider
// --------------------------
// Activities that implement this interface can take responsibility for
// displaying individual CTInAppNotifications.
//
// InAppController looks for this interface on the current activity whenever it
// needs to show an in-app notification. If present, it calls canDisplay(); if
// that returns true it then calls display().
//
// display() MUST report its outcome via the Callbacks object so the SDK keeps
// queue state and analytics accurate.
//
public interface CustomInAppDisplayProvider {

    // Returns true if this provider can handle the notification in this host.
    boolean canDisplay(
            @NonNull CTInAppNotification      notification,
            @NonNull CleverTapInstanceConfig  config,
            @NonNull Activity                 host);

    // Renders the notification (only after canDisplay() == true).
    // Implementation MUST report its outcome via Callbacks.
    void display(
            @NonNull CTInAppNotification      notification,
            @NonNull CleverTapInstanceConfig  config,
            @NonNull Activity                 host,
            @NonNull Callbacks                callbacks);

    // Bridge back into InAppController for metrics & queue management.
    interface Callbacks {
        Bundle onActionTriggered(
                @NonNull CTInAppNotification  notification,
                @NonNull CTInAppAction        action,
                @NonNull String               callToAction,
                Bundle                        additionalData,
                Context                       activityContext);

        void onButtonClicked(
                @NonNull CTInAppNotification      notification,
                CTInAppNotificationButton         button,
                Context                           activityContext);

        void onDismissed(
                @NonNull CTInAppNotification  notification,
                Bundle                        formData);

        void onShown(
                @NonNull CTInAppNotification  notification,
                Bundle                        formData);
    }
}
