package com.clevertap.android.sdk.db

import android.content.Context
import com.clevertap.android.sdk.events.EventGroup
import org.json.JSONObject

internal interface BaseDatabaseManager {

    fun loadDBAdapter(context: Context): DBAdapter

    fun clearQueues(context: Context)

    fun getQueuedEvents(
        context: Context,
        batchSize: Int,
        previousQueue: QueueData?,
        eventGroup: EventGroup
    ): QueueData

    fun getQueuedDBEvents(context: Context, batchSize: Int, previousQueue: QueueData?): QueueData

    fun getQueue(context: Context, table: Table, batchSize: Int, previousQueue: QueueData?): QueueData

    fun queueEventToDB(context: Context, event: JSONObject, type: Int)

    fun queuePushNotificationViewedEventToDB(context: Context, event: JSONObject)

    fun getPushNotificationViewedQueuedEvents(context: Context, batchSize: Int, previousQueue: QueueData?): QueueData
}
