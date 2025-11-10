package com.clevertap.android.sdk.db

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Table.EVENTS
import com.clevertap.android.sdk.db.Table.PROFILE_EVENTS
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup
import org.json.JSONObject

internal class DBManager constructor(
    private val accountId: String,
    private val logger: ILogger,
    private val databaseName: String,
    private val ctLockManager: CTLockManager,
    private val dbEncryptionHandler: DBEncryptionHandler,
) : BaseDatabaseManager {

    private companion object {
        private const val USER_EVENT_LOG_ROWS_PER_USER = 2_048 + 256 // events + profile props
        private const val USER_EVENT_LOG_ROWS_THRESHOLD = 5 * USER_EVENT_LOG_ROWS_PER_USER
    }

    private var dbAdapter: DBAdapter? = null

    @WorkerThread
    @Synchronized
    override fun loadDBAdapter(context: Context): DBAdapter {
        var dbAdapter = this.dbAdapter
        if (dbAdapter == null) {
            dbAdapter = DBAdapter(
                context = context,
                databaseName = databaseName,
                accountId = accountId,
                logger = logger,
                dbEncryptionHandler = dbEncryptionHandler
            )
            this.dbAdapter = dbAdapter
            dbAdapter.cleanupStaleEvents(EVENTS)
            dbAdapter.cleanupStaleEvents(PROFILE_EVENTS)
            dbAdapter.cleanupStaleEvents(PUSH_NOTIFICATION_VIEWED)
            dbAdapter.cleanUpPushNotifications()
            dbAdapter.userEventLogDAO()
                .cleanUpExtraEvents(USER_EVENT_LOG_ROWS_THRESHOLD, USER_EVENT_LOG_ROWS_PER_USER)
        }
        return dbAdapter
    }

    /**
     * Deletes all the events from the event and profile event queues.
     *
     * @param context The Android context.
     */
    @WorkerThread
    override fun clearQueues(context: Context) {
        val adapter = loadDBAdapter(context)
        var tableName = EVENTS
        adapter.removeEvents(tableName)
        tableName = PROFILE_EVENTS
        adapter.removeEvents(tableName)
    }

    /**
     * Main entry point for getting queued events
     * Now returns a combined batch of events and profile events with cleanup info
     */
    override fun getQueuedEvents(
        context: Context,
        batchSize: Int,
        eventGroup: EventGroup
    ): QueueData {
        return when (eventGroup) {
            EventGroup.PUSH_NOTIFICATION_VIEWED -> {
                logger.verbose(accountId, "Returning Queued Notification Viewed events")
                getPushNotificationViewedQueuedEvents(context, batchSize)
            }
            else -> {
                logger.verbose(accountId, "Returning combined queued events")
                getCombinedQueuedEvents(context, batchSize)
            }
        }
    }

    /**
     * Fetches a combined batch of events from both events and profileEvents tables
     * Returns QueueData with events data and ids, also if there are more events to fetch
     */
    override fun getCombinedQueuedEvents(context: Context, batchSize: Int): QueueData {
        synchronized(ctLockManager.eventLock) {
            val adapter = loadDBAdapter(context)

            // Fetch combined batch of events with cleanup info
            return adapter.fetchCombinedEvents(batchSize)
        }
    }

    /**
     * Cleans up successfully sent events
     * Should be called from NetworkManager after successful transmission
     *
     * @param context Android context
     * @param eventIds List of event IDs to clean up from events table
     * @param profileEventIds List of event IDs to clean up from profileEvents table
     */
    override fun cleanupSentEvents(
        context: Context,
        eventIds: List<String>,
        profileEventIds: List<String>
    ): Boolean {
        synchronized(ctLockManager.eventLock) {
            // Return true if nothing to clean up
            if (eventIds.isEmpty() && profileEventIds.isEmpty()) {
                return true
            }

            try {
                val adapter = loadDBAdapter(context)

                // Clean up events from events table
                if (eventIds.isNotEmpty()) {
                    //adapter.cleanupEventsByIds(EVENTS, eventIds)
                    adapter.cleanupEventsFromLastId(eventIds[eventIds.size-1], EVENTS)
                    logger.verbose(
                        accountId,
                        "Cleaned ${eventIds.size} events from events table"
                    )
                }

                // Clean up events from profileEvents table
                if (profileEventIds.isNotEmpty()) {
                    //adapter.cleanupEventsByIds(PROFILE_EVENTS, profileEventIds)
                    adapter.cleanupEventsFromLastId(profileEventIds[profileEventIds.size-1], PROFILE_EVENTS)
                    logger.verbose(
                        accountId,
                        "Cleaned ${profileEventIds.size} events from profileEvents table"
                    )
                }

                return true

            } catch (e: Exception) {
                logger.verbose(
                    accountId,
                    "Error during cleanup of sent events",
                    e
                )
                return false
            }
        }
    }

    override fun cleanupPushNotificationEvents(context: Context, ids: List<String>) : Boolean {
        synchronized(ctLockManager.eventLock) {
            // Return true if nothing to clean up
            if (ids.isEmpty()) {
                return true
            }

            try {
                val adapter = loadDBAdapter(context)
                // Clean up events from profileEvents table
                if (ids.isNotEmpty()) {
                    //adapter.cleanupEventsByIds(PUSH_NOTIFICATION_VIEWED, ids)
                    adapter.cleanupEventsFromLastId(ids[ids.size - 1], PUSH_NOTIFICATION_VIEWED)
                    logger.verbose(
                        accountId,
                        "Cleaned ${ids.size} events from Push impressions table"
                    )
                }
                return true
            } catch (e: Exception) {
                logger.verbose(
                    accountId,
                    "Error during cleanup of notification sent events",
                    e
                )
                return false
            }
        }
    }

    /**
     * Handles push notification viewed events separately
     * These remain in their own queue
     */
    override fun getPushNotificationViewedQueuedEvents(
        context: Context,
        batchSize: Int
    ): QueueData {
        synchronized(ctLockManager.eventLock) {
            val adapter = loadDBAdapter(context)

            // Use the optimized fetchEvents method that returns QueueData
            return adapter.fetchEvents(PUSH_NOTIFICATION_VIEWED, batchSize)
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

    @WorkerThread
    private fun queueEventForTable(context: Context, event: JSONObject, table: Table) {
        synchronized(ctLockManager.eventLock) {
            val adapter = loadDBAdapter(context)
            val returnCode = adapter.storeObject(event, table)
            if (returnCode > 0) {
                logger.debug(accountId, "Queued event: $event")
                logger.verbose(accountId, "Queued event to DB table $table: $event")
            }
        }
    }
}
