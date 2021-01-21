package com.clevertap.android.sdk;

import android.content.Context;
import java.util.concurrent.Future;
import org.json.JSONObject;

public abstract class BaseQueueManager {

    public abstract Future<?> queueEvent(final Context context, final JSONObject event, final int eventType);

    abstract void pushBasicProfile(JSONObject baseProfile);

    abstract void pushInitialEventsAsync();

    abstract void addToQueue(final Context context, final JSONObject event, final int eventType) ;

    abstract void flush();

    abstract void flushQueueAsync(final Context context, final EventGroup eventGroup);

    public abstract void flushQueueSync(final Context context, final EventGroup eventGroup);

    abstract void scheduleQueueFlush(final Context context);
}
