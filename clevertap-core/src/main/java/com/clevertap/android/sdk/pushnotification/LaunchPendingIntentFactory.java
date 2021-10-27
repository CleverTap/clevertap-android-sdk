package com.clevertap.android.sdk.pushnotification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Utils;

class LaunchPendingIntentFactory {

    static PendingIntent getLaunchPendingIntent(@NonNull Bundle extras, @NonNull Context context) {
        Intent launchIntent;
        PendingIntent pIntent;
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
                launchIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(extras.getString(Constants.DEEP_LINK_KEY)));
                Utils.setPackageNameFromResolveInfoList(context, launchIntent);
            } else {
                launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (launchIntent == null) {
                    return null;
                }
            }

            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // Take all the properties from the notif and add it to the intent
            launchIntent.putExtras(extras);
            launchIntent.removeExtra(Constants.WZRK_ACTIONS);

            int flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT
                    | PendingIntent.FLAG_IMMUTABLE;

            pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), launchIntent,
                    flagsLaunchPendingIntent);
        } else {
            launchIntent = new Intent(context, CTPushNotificationReceiver.class);
            // Take all the properties from the notif and add it to the intent
            launchIntent.putExtras(extras);
            launchIntent.removeExtra(Constants.WZRK_ACTIONS);

            int flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                flagsLaunchPendingIntent |= PendingIntent.FLAG_IMMUTABLE;
            }
            pIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                    launchIntent, flagsLaunchPendingIntent);
        }
        return pIntent;
    }
}
