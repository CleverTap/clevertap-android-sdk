package com.clevertap.android.sdk.db;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.events.EventGroup;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public class DBManager extends BaseDatabaseManager {

    private DBAdapter dbAdapter;

    private final CTLockManager ctLockManager;

    private final CleverTapInstanceConfig config;

    public DBManager(CleverTapInstanceConfig config,
            CTLockManager ctLockManager) {
        this.config = config;
        this.ctLockManager = ctLockManager;
    }

    @WorkerThread
    @Override
    public synchronized DBAdapter loadDBAdapter(final Context context) {
        if (dbAdapter == null) {
            dbAdapter = new DBAdapter(context, config);
            dbAdapter.cleanupStaleEvents(Table.EVENTS);
            dbAdapter.cleanupStaleEvents(Table.PROFILE_EVENTS);
            dbAdapter.cleanupStaleEvents(Table.PUSH_NOTIFICATION_VIEWED);
            dbAdapter.cleanUpPushNotifications();
        }
        return dbAdapter;
    }

    /**
     * Only call async
     */
    @Override
    public void clearQueues(final Context context) {
        synchronized (ctLockManager.getEventLock()) {

            DBAdapter adapter = loadDBAdapter(context);
            Table tableName = Table.EVENTS;

            adapter.removeEvents(tableName);
            tableName = Table.PROFILE_EVENTS;
            adapter.removeEvents(tableName);

            clearUserContext(context);
        }
    }

    //Session
    private void clearIJ(Context context) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        StorageHelper.persist(editor);
    }

    //Session
    private void clearLastRequestTimestamp(Context context) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_LAST_TS), 0);
    }

    //Session
    private void clearUserContext(final Context context) {
        clearIJ(context);
        clearFirstRequestTimestampIfNeeded(context);
        clearLastRequestTimestamp(context);
    }

    //Session
    private void clearFirstRequestTimestampIfNeeded(Context context) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_FIRST_TS), 0);
    }

    // helper extracts the cursor data from the db object

    @Override
    QueueCursor getPushNotificationViewedQueuedEvents(final Context context, final int batchSize,
            final QueueCursor previousCursor) {
        return getQueueCursor(context, Table.PUSH_NOTIFICATION_VIEWED, batchSize, previousCursor);
    }

    @Override
    QueueCursor getQueueCursor(final Context context, final Table table, final int batchSize,
            final QueueCursor previousCursor) {
        synchronized (ctLockManager.getEventLock()) {
            DBAdapter adapter = loadDBAdapter(context);
            Table tableName = (previousCursor != null) ? previousCursor.getTableName() : table;

            // if previousCursor that means the batch represented by the previous cursor was processed so remove those from the db
            if (previousCursor != null) {
                adapter.cleanupEventsFromLastId(previousCursor.getLastId(), previousCursor.getTableName());
            }

            // grab the new batch
            QueueCursor newCursor = new QueueCursor();
            newCursor.setTableName(tableName);
            JSONObject queuedDBEvents = adapter.fetchEvents(tableName, batchSize);
            newCursor = updateCursorForDBObject(queuedDBEvents, newCursor);

            return newCursor;
        }
    }

    // note : only works with  Queue Cursor of Events table. for other cursors, will override its data with Event table's data
    @Override
    QueueCursor getQueuedDBEvents(final Context context, final int batchSize, final QueueCursor previousCursor) {

        synchronized (ctLockManager.getEventLock()) {
            QueueCursor newCursor = getQueueCursor(context, Table.EVENTS, batchSize, previousCursor);

            if (newCursor.isEmpty() && newCursor.getTableName().equals(Table.EVENTS)) {
                newCursor = getQueueCursor(context, Table.PROFILE_EVENTS, batchSize, null);
            }

            return newCursor.isEmpty() ? null : newCursor;
        }
    }

    @SuppressWarnings("SameParameterValue")
    public QueueCursor getQueuedEvents(final Context context, final int batchSize, final QueueCursor previousCursor,
            final EventGroup eventGroup) {
        if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
            config.getLogger().verbose(config.getAccountId(), "Returning Queued Notification Viewed events");
            return getPushNotificationViewedQueuedEvents(context, batchSize, previousCursor);
        } else {
            config.getLogger().verbose(config.getAccountId(), "Returning Queued events");
            return getQueuedDBEvents(context, batchSize, previousCursor);
        }
    }

    //Event
    @WorkerThread
    @Override
    public void queueEventToDB(final Context context, final JSONObject event, final int type) {
        Table table = (type == Constants.PROFILE_EVENT) ? Table.PROFILE_EVENTS
                : Table.EVENTS;
        queueEventInternal(context, event, table);
    }

    @WorkerThread
    @Override
    public void queuePushNotificationViewedEventToDB(final Context context, final JSONObject event) {
        queueEventInternal(context, event, Table.PUSH_NOTIFICATION_VIEWED);
    }

    /* calling this function Will set items from jsonObject to queue cursor.
     * if jsonObject is null, will return the queue cursor as it is
     * else
     *  - it will try taking the first key and set it at as cursor's Last Id
     *  - it will try taking the value of first key, cast it as jsonArray and set it as cursor's data
     *
     * therefore, the json must be of format : {string : jsonArray }
     * */
    @Override
    QueueCursor updateCursorForDBObject(final JSONObject dbObject, final QueueCursor cursor) {
        if (dbObject == null) {
            return cursor;
        }

        Iterator<String> keys = dbObject.keys();
        if (keys.hasNext()) {
            String key = keys.next();
            cursor.setLastId(key);
            try {
                cursor.setData(dbObject.getJSONArray(key));
            } catch (JSONException e) {
                cursor.setLastId(null);
                cursor.setData(null);
            }
        }

        return cursor;
    }

    @WorkerThread
    private void queueEventInternal(final Context context, final JSONObject event, Table table) {
        synchronized (ctLockManager.getEventLock()) {
            DBAdapter adapter = loadDBAdapter(context);
            long returnCode = adapter.storeObject(event, table);

            if (returnCode > 0) {
                config.getLogger().debug(config.getAccountId(), "Queued event: " + event.toString());
                config.getLogger()
                        .verbose(config.getAccountId(),
                                "Queued event to DB table " + table + ": " + event.toString());
            }
        }
    }
}
