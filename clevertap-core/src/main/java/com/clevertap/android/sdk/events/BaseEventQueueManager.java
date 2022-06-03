package com.clevertap.android.sdk.events;

import android.content.Context;
import java.util.concurrent.Future;
import org.json.JSONObject;

public abstract class BaseEventQueueManager {

    public abstract Future<?> queueEvent(final Context context, final JSONObject event, final int eventType);

    public abstract void addToQueue(final Context context, final JSONObject event, final int eventType);

    public abstract void flush();

    public abstract void flushQueueAsync(final Context context, final EventGroup eventGroup);

    public abstract void pushBasicProfile(JSONObject baseProfile, boolean removeFromSharedPrefs);

    public abstract void pushInitialEventsAsync();

    public abstract void flushQueueSync(final Context context, final EventGroup eventGroup);

    public abstract void scheduleQueueFlush(final Context context);
}
