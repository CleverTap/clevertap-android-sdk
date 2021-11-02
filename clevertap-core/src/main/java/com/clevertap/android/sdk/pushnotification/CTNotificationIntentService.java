package com.clevertap.android.sdk.pushnotification;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;

/**
 * Android 12 prevents components that start other activities inside services or broadcast receivers when notification is opened.
 * If this requirement is not met, the system will prevent the activity from starting.
 *
 * As a part of these Android OS changes, CleverTap will be deprecating the CTNotificationIntentService class from v4.3.0
 *
 * CTNotificationIntentService was used to handle Push notifications with action buttons. The service used to open the deeplink
 * and close the notification tray.
 *
 * From Android 12, closing of the notification tray is now handled by the OS when it opens an Activity.
 * If the action buttons are redirecting the user to an existing activity inside the app, then
 * the Notification Clicked event will be triggered with the event properties mentioning which action button was clicked.
 * If the deep link is to a third-party application or a URL, then the Notification Clicked event will not be tracked.
 */
@Deprecated(since = "4.3.0")
public class CTNotificationIntentService extends IntentService {

    public final static String MAIN_ACTION = "com.clevertap.PUSH_EVENT";

    public final static String TYPE_BUTTON_CLICK = "com.clevertap.ACTION_BUTTON_CLICK";

    public CTNotificationIntentService() {
        super("CTNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        String type = extras.getString("ct_type");
        if (TYPE_BUTTON_CLICK.equals(type)) {
            Logger.v("CTNotificationIntentService handling " + TYPE_BUTTON_CLICK);
            handleActionButtonClick(extras);
        } else {
            Logger.v("CTNotificationIntentService: unhandled intent " + intent.getAction());
        }
    }

    @SuppressLint("MissingPermission")
    private void handleActionButtonClick(Bundle extras) {
        try {
            boolean autoCancel = extras.getBoolean("autoCancel", false);
            int notificationId = extras.getInt("notificationId", -1);
            String dl = extras.getString("dl");

            Context context = getApplicationContext();
            Intent launchIntent;
            if (dl != null) {
                launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
                Utils.setPackageNameFromResolveInfoList(context, launchIntent);
            } else {
                launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            }

            if (launchIntent == null) {
                Logger.v("CTNotificationService: create launch intent.");
                return;
            }

            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            launchIntent.putExtras(extras);
            launchIntent.removeExtra("dl");

            if (autoCancel && notificationId > -1) {
                NotificationManager notificationManager =
                        (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(notificationId);
                }

            }
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)); // close the notification drawer
            startActivity(launchIntent);
        } catch (Throwable t) {
            Logger.v("CTNotificationService: unable to process action button click:  " + t.getLocalizedMessage());
        }
    }
}
