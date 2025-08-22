package com.clevertap.android.sdk.db

import android.content.Context
import com.clevertap.android.sdk.events.EventGroup
import org.json.JSONObject

internal interface BaseDatabaseManager {

    fun loadDBAdapter(context: Context): DBAdapter

    fun clearQueues(context: Context)

    /**
     * Gets queued events from the database
     * @param context Android context
     * @param batchSize Number of events to fetch (typically 50)
     * @param eventGroup Type of events to fetch
     * @return QueueData containing events and their IDs for cleanup
     */
    fun getQueuedEvents(
        context: Context,
        batchSize: Int,
        eventGroup: EventGroup
    ): QueueData

    fun queueEventToDB(context: Context, event: JSONObject, type: Int)

    fun queuePushNotificationViewedEventToDB(context: Context, event: JSONObject)

    fun getPushNotificationViewedQueuedEvents(context: Context, batchSize: Int): QueueData

    /**
     * Cleans up successfully sent events from the database
     * @param context Android context
     * @param eventIds List of event IDs to clean up from events table
     * @param profileEventIds List of event IDs to clean up from profileEvents table
     * @return true if cleanup successful, false otherwise
     */
    fun cleanupSentEvents(
        context: Context,
        eventIds: List<String>,
        profileEventIds: List<String>
    ) : Boolean

    fun cleanupPushNotificationEvents(
        context: Context,
        ids: List<String>
    ) : Boolean
}
