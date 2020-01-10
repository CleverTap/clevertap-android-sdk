package com.clevertap.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver for INSTALL_REFERRAL intents.
 * Deprecation warning because Google Play install referrer via intent will be deprecated in March 2020
 */
@Deprecated
public final class InstallReferrerBroadcastReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
            CleverTapAPI.handleInstallReferrer(context,intent);
    }

}
