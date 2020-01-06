package com.clevertap.android.sdk;

import android.app.IntentService;
import android.content.Intent;


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
        PushAmpDiagnosticUtil.raiseEvent(getApplicationContext(), Constants.CT_PUSH_AMP_SERVICE_START);
        CleverTapAPI.runBackgroundIntentService(getApplicationContext());
    }
}