package com.clevertap.android.sdk.pushnotification;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Utils;
import java.util.Random;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LaunchPendingIntentFactory {

    public static PendingIntent getLaunchPendingIntent(@NonNull Bundle extras, @NonNull Context context) {
        Intent launchIntent;
        PendingIntent pIntent;
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            pIntent = getActivityIntent(extras, context);
        } else {
            launchIntent = new Intent(context, CTPushNotificationReceiver.class);
            // Take all the properties from the notif and add it to the intent
            launchIntent.putExtras(extras);
            launchIntent.removeExtra(Constants.WZRK_ACTIONS);

            int flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                flagsLaunchPendingIntent |= PendingIntent.FLAG_IMMUTABLE;
            }
            pIntent = PendingIntent.getBroadcast(context, new Random().nextInt(),
                    launchIntent, flagsLaunchPendingIntent);
        }
        return pIntent;
    }

    public static PendingIntent getActivityIntent(@NonNull Bundle extras, @NonNull Context context){
        Intent launchIntent;
        if (extras.containsKey(Constants.DEEP_LINK_KEY) && extras.getString(Constants.DEEP_LINK_KEY)!=null) {
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

        int flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            flagsLaunchPendingIntent |= PendingIntent.FLAG_IMMUTABLE;
        }

        Bundle optionsBundle = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            optionsBundle = ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle();
        }

        return PendingIntent.getActivity(context, new Random().nextInt(), launchIntent,
                flagsLaunchPendingIntent, optionsBundle);


    }
}
