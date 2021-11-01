package com.clevertap.android.sdk.pushnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;

/**
 * Android 12 prevents components that start other activities inside services or broadcast receivers when notification is opened.
 * If this requirement is not met, the system will prevent the activity from starting.
 *
 * As a part of these Android OS changes, CleverTap will be deprecating the CTPushNotificationReceiver class from v4.3.0
 *
 * CTPushNotificationReceiver was used to handle Push notifications with deep links pointing inside the app or third-party applications/URLs.
 * The receiver would first raise the Notification Clicked event and then open the Activity as mentioned in the deep link.
 *
 * Android 12 restricts usage of notification trampolines, meaning that notification must start the activity directly on the notification tap.
 * Tracking of Notification Clicked events now happens in the {@link CleverTapAPI} }instance registered by {@link com.clevertap.android.sdk.ActivityLifecycleCallback}.
 * The push payload is attached as an extra to the notification intent and processed when the notification is tapped.
 * If the deep link is to a third-party application or a URL, then the Notification Clicked event will NO LONGER be tracked.
 */
@Deprecated(since = "4.3.0")
public class CTPushNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            Intent launchIntent;

            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }

            if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
                launchIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(intent.getStringExtra(Constants.DEEP_LINK_KEY)));
                Utils.setPackageNameFromResolveInfoList(context, launchIntent);
            } else {
                launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (launchIntent == null) {
                    return;
                }
            }

            CleverTapAPI.handleNotificationClicked(context, extras);

            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            launchIntent.putExtras(extras);

            //to prevent calling of pushNotificationClickedEvent(extras) in ActivityLifecycleCallback
            launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);

            context.startActivity(launchIntent);

            Logger.d("CTPushNotificationReceiver: handled notification: " + extras.toString());
        } catch (Throwable t) {
            Logger.v("CTPushNotificationReceiver: error handling notification", t);
        }
    }
}
