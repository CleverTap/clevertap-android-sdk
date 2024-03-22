package com.clevertap.android.sdk.db

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.Table.EVENTS
import com.clevertap.android.sdk.db.Table.PROFILE_EVENTS
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup
import org.json.JSONObject

internal class DBManager(
    private val config: CleverTapInstanceConfig,
    private val ctLockManager: CTLockManager
) : BaseDatabaseManager {

    private var dbAdapter: DBAdapter? = null

    @WorkerThread
    @Synchronized
    override fun loadDBAdapter(context: Context): DBAdapter {
        var dbAdapter = this.dbAdapter
        if (dbAdapter == null) {
            dbAdapter = DBAdapter(context, config)
            this.dbAdapter = dbAdapter
            dbAdapter.cleanupStaleEvents(EVENTS)
            dbAdapter.cleanupStaleEvents(PROFILE_EVENTS)
            dbAdapter.cleanupStaleEvents(PUSH_NOTIFICATION_VIEWED)
            dbAdapter.cleanUpPushNotifications()
        }
        return dbAdapter
    }

    @WorkerThread
    override fun clearQueues(context: Context) {
        synchronized(ctLockManager.eventLock) {
            val adapter = loadDBAdapter(context)
            var tableName = EVENTS
            adapter.removeEvents(tableName)
            tableName = PROFILE_EVENTS
            adapter.removeEvents(tableName)
            clearUserContext(context)
        }
    }

    override fun getQueuedEvents(
        context: Context,
        batchSize: Int,
        previousQueue: QueueData?,
        eventGroup: EventGroup
    ): QueueData {
        return if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
            config.logger.verbose(config.accountId, "Returning Queued Notification Viewed events")
            getPushNotificationViewedQueuedEvents(context, batchSize, previousQueue)
        } else {
            config.logger.verbose(config.accountId, "Returning Queued events")
            getQueuedDBEvents(context, batchSize, previousQueue)
        }
    }

    /**
     * Only works with Queue of Events table. For other queues, it will override its data with Event table's data
     */
    override fun getQueuedDBEvents(context: Context, batchSize: Int, previousQueue: QueueData?): QueueData {
        synchronized(ctLockManager.eventLock) {
            var queue = getQueue(context, EVENTS, batchSize, previousQueue)
            if (queue.isEmpty && queue.table == EVENTS) {
                queue = getQueue(context, PROFILE_EVENTS, batchSize, null)
            }
            return queue
        }
    }

    override fun getQueue(context: Context, table: Table, batchSize: Int, previousQueue: QueueData?): QueueData {
        synchronized(ctLockManager.eventLock) {
            val adapter = loadDBAdapter(context)
            val tableName = previousQueue?.table ?: table

            // Remove the previous batch from the db, if there is such, since it was processed.
            previousQueue?.lastId?.let { lastId ->
                adapter.cleanupEventsFromLastId(lastId, previousQueue.table)
            }

            val dbEvents = adapter.fetchEvents(tableName, batchSize)
            val newQueue = QueueData(tableName)
            newQueue.setDataFromDbObject(dbEvents)
            return newQueue
        }
    }

    //Event
    @WorkerThread
    override fun queueEventToDB(context: Context, event: JSONObject, type: Int) {
        val table = if (type == Constants.PROFILE_EVENT) PROFILE_EVENTS else EVENTS
        queueEventForTable(context, event, table)
    }

    @WorkerThread
    override fun queuePushNotificationViewedEventToDB(context: Context, event: JSONObject) {
        queueEventForTable(context, event, PUSH_NOTIFICATION_VIEWED)
    }

    override fun getPushNotificationViewedQueuedEvents(
        context: Context,
        batchSize: Int,
        previousQueue: QueueData?
    ): QueueData {
        return getQueue(context, PUSH_NOTIFICATION_VIEWED, batchSize, previousQueue)
    }

    //Session
    private fun clearIJ(context: Context) {
        val editor = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ).edit()
        editor.clear()
        StorageHelper.persist(editor)
    }

    //Session
    private fun clearLastRequestTimestamp(context: Context) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_LAST_TS), 0)
    }

    //Session
    private fun clearUserContext(context: Context) {
        clearIJ(context)
        clearFirstRequestTimestampIfNeeded(context)
        clearLastRequestTimestamp(context)
    }

    //Session
    private fun clearFirstRequestTimestampIfNeeded(context: Context) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_FIRST_TS), 0)
    }

    @WorkerThread
    private fun queueEventForTable(context: Context, event: JSONObject, table: Table) {
        synchronized(ctLockManager.eventLock) {
            val adapter = loadDBAdapter(context)
            val returnCode = adapter.storeObject(event, table)
            if (returnCode > 0) {
                config.logger.debug(config.accountId, "Queued event: $event")
                config.logger.verbose(config.accountId, "Queued event to DB table $table: $event")
            }
        }
    }
}
