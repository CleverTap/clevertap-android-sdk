package com.clevertap.android.pushtemplates;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

public class PTNotificationIntentService extends IntentService {

    public final static String MAIN_ACTION = "com.clevertap.PT_PUSH_EVENT";
    public final static String TYPE_BUTTON_CLICK = "com.clevertap.ACTION_BUTTON_CLICK";

    public PTNotificationIntentService() {
        super("PTNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        String type = extras.getString(PTConstants.PT_TYPE);
        if (TYPE_BUTTON_CLICK.equals(type)) {
            PTLog.verbose("PTNotificationIntentService handling " + TYPE_BUTTON_CLICK);
            handleActionButtonClick(extras);
        } else {
            PTLog.verbose("PTNotificationIntentService: unhandled intent "+intent.getAction());
        }
    }

    private void handleActionButtonClick(Bundle extras) {
        try {
            boolean autoCancel = extras.getBoolean("autoCancel", false);
            int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID, -1);
            String actionID = extras.getString(PTConstants.PT_ACTION_ID);
            String dl = extras.getString("dl");
            String dismissOnClick = extras.getString(PTConstants.PT_DISMISS_ON_CLICK);
            CleverTapInstanceConfig config = extras.getParcelable("config");
            Context context = getApplicationContext();

            if (dismissOnClick!=null)
                if (dismissOnClick.equalsIgnoreCase("true")){
                    if(actionID != null && actionID.contains("remind")){
                        Utils.raiseCleverTapEvent(context, config, extras);
                    }
                    Utils.cancelNotification(context, notificationId);
                    return;
                }
            Intent launchIntent;
            if (dl != null) {
                launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
                Utils.setPackageNameFromResolveInfoList(context,launchIntent);
            } else {
                launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            }

            if (launchIntent == null) {
                PTLog.verbose("PTNotificationIntentService: create launch intent.");
                return;
            }

            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

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
            PTLog.verbose("PTNotificationIntentService: unable to process action button click:  "+ t.getLocalizedMessage());
        }
    }
}
