package com.clevertap.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver for INSTALL_REFERRAL intents.
 */
public final class InstallReferrerBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        CleverTapAPI.handleInstallReferrerViaReceiver(context, intent);
    }
}