package com.clevertap.android.sdk.db.dao

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.db.QueueData
import com.clevertap.android.sdk.db.Table
import org.json.JSONObject

internal interface EventDAO {
    @WorkerThread
    fun storeEvent(event: JSONObject, table: Table): Long
    
    @WorkerThread
    fun fetchEvents(table: Table, limit: Int): QueueData

    @WorkerThread
    fun fetchCombinedEvents(batchSize: Int): QueueData
    
    @WorkerThread
    fun cleanupEventsFromLastId(lastId: String, table: Table)
    
    @WorkerThread
    fun cleanupStaleEvents(table: Table)
    
    @WorkerThread
    fun removeAllEvents(table: Table)
}
