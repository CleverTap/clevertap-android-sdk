package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

public class DBManager {

    QueueCursor updateCursorForDBObject(JSONObject dbObject, QueueCursor cursor) {
        //TODO
        return null;
    }

    DBAdapter loadDBAdapter(Context context) {
        //TODO
        return null;
    }

    QueueCursor getQueueCursor(final Context context, DBAdapter.Table table, final int batchSize,
            final QueueCursor previousCursor) {
        //TODO
        return null;
    }

    QueueCursor getQueuedDBEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor) {
        //TODO
        return null;
    }

    QueueCursor getPushNotificationViewedQueuedEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor) {
        //TODO
        return null;
    }

    void flushDBQueue(final Context context, final EventGroup eventGroup) {
        //TODO
    }
}
