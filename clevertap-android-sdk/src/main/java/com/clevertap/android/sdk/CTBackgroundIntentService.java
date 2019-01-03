package com.clevertap.android.sdk;

import android.app.IntentService;
import android.content.Intent;


public class CTBackgroundIntentService extends IntentService {

    public final static String MAIN_ACTION = "com.clevertap.BG_EVENT";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public CTBackgroundIntentService() {
        super("CTBackgroundIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        CleverTapAPI.runBackgroundIntentService(getApplicationContext());
    }
}
