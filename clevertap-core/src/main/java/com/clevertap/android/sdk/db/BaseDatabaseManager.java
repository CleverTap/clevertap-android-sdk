package com.clevertap.android.sdk.db;

import android.content.Context;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.events.EventGroup;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY_GROUP)
public abstract class BaseDatabaseManager {

    public abstract void clearQueues(final Context context);

    public abstract QueueCursor getQueuedEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor,
            final EventGroup eventGroup);

    public abstract void queueEventToDB(final Context context, final JSONObject event, final int type);

    abstract QueueCursor updateCursorForDBObject(JSONObject dbObject, QueueCursor cursor);

    public abstract DBAdapter loadDBAdapter(Context context);

    abstract QueueCursor getQueueCursor(final Context context, DBAdapter.Table table, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getQueuedDBEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor);

    abstract QueueCursor getPushNotificationViewedQueuedEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor);

    public abstract void queuePushNotificationViewedEventToDB(final Context context, final JSONObject event);

}