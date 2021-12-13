package com.clevertap.android.pushtemplates;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationReceiver;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.util.concurrent.Callable;

public class PTPushNotificationReceiver extends CTPushNotificationReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        CleverTapAPI cleverTapAPI = CleverTapAPI
                .getGlobalInstance(context, extras.getString(Constants.WZRK_ACCT_ID_KEY));

        if (cleverTapAPI != null) {
            try {
                CleverTapInstanceConfig config = cleverTapAPI.getCoreState().getConfig();
                Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
                task.execute("PTPushNotificationReceiver#cleanUpFiles", new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        try {
                            Utils.deleteImageFromStorage(context, intent);
                            Utils.deleteSilentNotificationChannel(context);
                        } catch (Throwable t) {
                            PTLog.verbose(
                                    "Couldn't clean up images and/or couldn't delete silent notification channel: "
                                            + t.getLocalizedMessage());
                        }
                        return null;
                    }
                });
            } catch (Exception e) {
                PTLog.verbose("Couldn't clean up images and/or couldn't delete silent notification channel: " + e
                        .getLocalizedMessage());
            }

        } else {
            PTLog.verbose("clevertap instance is null, not running PTPushNotificationReceiver#cleanUpFiles");
        }
    }
}
