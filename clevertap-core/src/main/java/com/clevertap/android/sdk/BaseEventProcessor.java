package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

abstract class BaseEventProcessor {
    abstract void processEvent(final Context context, final JSONObject event, final int eventType);

    abstract void processPushNotificationViewedEvent(final Context context, final JSONObject event);
}