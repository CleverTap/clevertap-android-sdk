package com.clevertap.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class PushAmpDiagnosticUtil {
    private PushAmpDiagnosticUtil() {

    }

    public static void raiseEvent(Context context, String eventName) {
        Intent intent = new Intent(Constants.ACTION_CT_LOCAL_EVENT);
        intent.putExtra(Constants.KEY_CT_LOCAL_EVENT, eventName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}