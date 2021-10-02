package com.clevertap.android.sdk.pushnotification;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler;
import com.clevertap.android.sdk.interfaces.NotificationHandler;

public class CTNotificationIntentService extends IntentService {

    public final static String MAIN_ACTION = "com.clevertap.PUSH_EVENT";

    public final static String TYPE_BUTTON_CLICK = "com.clevertap.ACTION_BUTTON_CLICK";

    private ActionButtonClickHandler mActionButtonClickHandler;

    public CTNotificationIntentService() {
        super("CTNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        NotificationHandler notificationHandler = CleverTapAPI.getNotificationHandler();
        if (PushNotificationHandler.isForPushTemplates(extras) && notificationHandler != null) {
            mActionButtonClickHandler = (ActionButtonClickHandler) notificationHandler;
        } else {
            mActionButtonClickHandler = (ActionButtonClickHandler) PushNotificationHandler
                    .getPushNotificationHandler();
        }

        String type = mActionButtonClickHandler.getType(extras);
        if (TYPE_BUTTON_CLICK.equals(type)) {
            Logger.v("CTNotificationIntentService handling " + TYPE_BUTTON_CLICK);
            handleActionButtonClick(extras);
        } else {
            Logger.v("CTNotificationIntentService: unhandled intent " + intent.getAction());
        }
    }

    private void handleActionButtonClick(Bundle extras) {
        try {
            boolean autoCancel = extras.getBoolean("autoCancel", false);
            int notificationId = extras.getInt("notificationId", -1);
            String dl = extras.getString("dl");

            Context context = getApplicationContext();

            boolean isActionButtonClickHandled = mActionButtonClickHandler
                    .onActionButtonClick(context, extras, notificationId);
            if (isActionButtonClickHandled) {
                return;
            }

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
