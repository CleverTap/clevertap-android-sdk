package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

abstract class BaseDatabaseManager {

    abstract void flushDBQueue(final Context context, final EventGroup eventGroup);

    abstract void queueEventToDB(final Context context, final JSONObject event, final int type);

    abstract void queuePushNotificationViewedEventToDB(final Context context, final JSONObject event);

    abstract QueueCursor updateCursorForDBObject(JSONObject dbObject, QueueCursor cursor);
    abstract DBAdapter loadDBAdapter(Context context);

    abstract QueueCursor getQueueCursor(final Context context, DBAdapter.Table table, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getQueuedDBEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getPushNotificationViewedQueuedEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getQueuedEvents(final Context context, final int batchSize, final QueueCursor previousCursor,
            final EventGroup eventGroup);

}