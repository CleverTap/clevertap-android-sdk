package com.clevertap.android.sdk;

import android.content.Context;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import org.json.JSONObject;
@RestrictTo(Scope.LIBRARY_GROUP)
public abstract class BaseDatabaseManager {

    public abstract void clearQueues(final Context context);

    abstract void queueEventToDB(final Context context, final JSONObject event, final int type);

    abstract void queuePushNotificationViewedEventToDB(final Context context, final JSONObject event);

    abstract QueueCursor updateCursorForDBObject(JSONObject dbObject, QueueCursor cursor);
    public abstract DBAdapter loadDBAdapter(Context context);

    abstract QueueCursor getQueueCursor(final Context context, DBAdapter.Table table, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getQueuedDBEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getPushNotificationViewedQueuedEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getQueuedEvents(final Context context, final int batchSize, final QueueCursor previousCursor,
            final EventGroup eventGroup);

}