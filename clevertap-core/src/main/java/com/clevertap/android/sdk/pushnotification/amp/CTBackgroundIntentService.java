package com.clevertap.android.sdk.pushnotification.amp;

import android.app.IntentService;
import android.content.Intent;
import com.clevertap.android.sdk.CleverTapAPI;


/**
 * Background Intent Service to sync up for new notifications
 */
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
