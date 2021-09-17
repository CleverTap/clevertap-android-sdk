package com.clevertap.android.pushtemplates;

import android.content.Context;
import android.content.Intent;

import com.clevertap.android.sdk.pushnotification.CTPushNotificationReceiver;

public class PTPushNotificationReceiver extends CTPushNotificationReceiver {
    private final AsyncHelper asyncHelper = AsyncHelper.getInstance();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);

        asyncHelper.postAsyncSafely("PTPushNotificationReceiver#cleanUpFiles", new Runnable() {
            @Override
            public void run() {
                try {
                    Utils.deleteImageFromStorage(context, intent);
                    Utils.deleteSilentNotificationChannel(context);
                } catch (Throwable t) {
                    PTLog.verbose("Couldn't clean up images and/or couldn't delete silent notification channel: " + t.getLocalizedMessage());
                }
            }
        });
    }
}
