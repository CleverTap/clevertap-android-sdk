package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_OUT_OF_MEMORY_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_UPDATE_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.QueueData
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class EventDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: ILogger,
    private val dbEncryptionHandler: DBEncryptionHandler,
    private val clock: Clock = Clock.SYSTEM
) : EventDAO {

    companion object {
        private const val DATA_EXPIRATION = 1000L * 60 * 60 * 24 * 5
    }

    @WorkerThread
    override fun storeEvent(event: JSONObject, table: Table): Long {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return DB_OUT_OF_MEMORY_ERROR
        }
        
        val tableName = table.tableName
        val cv = ContentValues().apply {
            put(Column.DATA, dbEncryptionHandler.wrapDbData(event.toString()))
            put(Column.CREATED_AT, clock.currentTimeMillis())
        }

        return try {
            dbHelper.writableDatabase.insert(tableName, null, cv)
        } catch (e: Exception) {
            logger.verbose("Error adding data to table $tableName. Recreating DB", e)
            dbHelper.deleteDatabase()
            DB_UPDATE_ERROR
        }
    }

    /**
     * Returns a JSONObject keyed with the lastId retrieved and a value of a JSONArray of the retrieved JSONObject
     * events
     *
     * @param table the table to read from
     * @return JSONObject containing the max row ID and a JSONArray of the JSONObject events or null
     */
    @Synchronized
    override fun fetchEvents(table: Table, limit: Int): QueueData {
        val queueData = QueueData()

        val tName = table.tableName
        try {
            dbHelper.readableDatabase.query(
                tName,
                arrayOf(Column.ID, Column.DATA, Column.CREATED_AT),
                null, null, null, null,
                "${Column.CREATED_AT} ASC",
                (limit + 1).toString()
            ).use { cursor ->
                val rowCount = cursor.count
                queueData.hasMore = rowCount > limit
                readEventRows(cursor = cursor, table = table, limit = limit, queueData = queueData)
            }
        } catch (e: Exception) {
            logger.verbose("Could not fetch records from table $tName", e)
        }

        val size = if (table == Table.PROFILE_EVENTS) {
            queueData.profileEventIds.size
        } else {
            queueData.eventIds.size
        }
        logger.verbose("Fetched $size events from $tName")
        return queueData
    }

    /**
     * Reads up to [limit] decryptable rows from [cursor] into [queueData]. Rows that cannot be
     * decrypted are skipped and do not count towards the limit.
     */
    private fun readEventRows(cursor: Cursor, table: Table, limit: Int, queueData: QueueData) {
        val colId = cursor.getColumnIndexOrThrow(Column.ID)
        val colData = cursor.getColumnIndexOrThrow(Column.DATA)

        var pos = 0
        while (cursor.moveToNext()) {
            if (pos == limit) {
                break
            }
            if (addEventRow(cursor = cursor, colId = colId, colData = colData, table = table, queueData = queueData)) {
                pos++
            }
        }
    }

    /**
     * Decrypts the current row of [cursor] and adds it to [queueData].
     *
     * @return false if the row could not be decrypted and was skipped
     */
    private fun addEventRow(cursor: Cursor, colId: Int, colData: Int, table: Table, queueData: QueueData): Boolean {
        val id = cursor.getString(colId)
        val eventData = cursor.getString(colData)

        val decryptedData = dbEncryptionHandler.unwrapDbData(eventData)
        if (decryptedData == null) {
            logger.verbose("Error parsing event data for id: $id from table: ${table.tableName}")
            return false
        }
        val jsonEvent = JSONObject(decryptedData)
        queueData.data.put(jsonEvent)

        if (table == Table.PROFILE_EVENTS) {
            queueData.profileEventIds.add(id)
        } else {
            queueData.eventIds.add(id)
        }
        return true
    }

    /**
     * Fetches a combined batch of events from both events and profileEvents tables
     * Prioritizes profileEvents table first, then fills remaining slots from events
     *
     * @param batchSize The maximum number of events to fetch (typically 50)
     * @return QueueData containing the events and their IDs for cleanup
     */
    @Synchronized
    override fun fetchCombinedEvents(batchSize: Int): QueueData {
        val combinedQueueData = QueueData()

        // First priority: Fetch from profileEvents table using the base fetchEvents method
        val profileData = fetchEvents(Table.PROFILE_EVENTS, batchSize)

        // Add profile events to combined data
        for (i in 0 until profileData.data.length()) {
            combinedQueueData.data.put(profileData.data.getJSONObject(i))
        }
        combinedQueueData.profileEventIds.addAll(profileData.profileEventIds)
        combinedQueueData.hasMore = profileData.hasMore

        // Calculate remaining slots for normal events
        val eventsNeeded = batchSize - combinedQueueData.profileEventIds.size

        // Second priority: Fill remaining slots from events table
        if (eventsNeeded > 0 || combinedQueueData.hasMore.not()) {
            val eventsData = fetchEvents(Table.EVENTS, eventsNeeded)

            // Add events to combined data
            for (i in 0 until eventsData.data.length()) {
                combinedQueueData.data.put(eventsData.data.getJSONObject(i))
            }
            combinedQueueData.eventIds.addAll(eventsData.eventIds)
            combinedQueueData.hasMore = eventsData.hasMore
        }

        logger.verbose("Fetched combined batch: ${combinedQueueData.profileEventIds.size} profile events, ${combinedQueueData.eventIds.size} events")

        return combinedQueueData
    }

    /**
     * Removes sent events with an _id <= last_id from table
     *
     * @param lastId the last id to delete
     * @param table  the table to remove events
     */
    @WorkerThread
    @Synchronized
    override fun cleanupEventsFromLastId(lastId: String, table: Table) {
        val tName = table.tableName
        try {
            dbHelper.writableDatabase.delete(tName, "${Column.ID} <= ?", arrayOf(lastId))
        } catch (e: SQLiteException) {
            logger.verbose("Error removing sent data from table $tName Recreating DB", e)
            dbHelper.deleteDatabase()
        }
    }

    @WorkerThread
    override fun cleanupStaleEvents(table: Table) {
        val time = clock.currentTimeMillis() - DATA_EXPIRATION
        val tName = table.tableName
        try {
            dbHelper.writableDatabase.delete(tName, "${Column.CREATED_AT} <= $time", null)
        } catch (e: Exception) {
            logger.verbose("Error removing stale event records from $tName. Recreating DB.", e)
            dbHelper.deleteDatabase()
        }
    }

    @WorkerThread
    override fun removeAllEvents(table: Table) {
        val tName = table.tableName
        try {
            dbHelper.writableDatabase.delete(tName, null, null)
        } catch (e: Exception) {
            logger.verbose("Error removing all events from table $tName. Recreating DB", e)
            dbHelper.deleteDatabase()
        }
    }

    /**
     * Updates all event rows in the specified table based on current encryption level.
     * - FULL_DATA: Transforms plain text data to encrypted format
     * - NONE/MEDIUM: Transforms encrypted data to plain text format
     * Useful for migrating data when encryption level changes.
     *
     * @param table the table to update events in
     * @return the number of rows successfully updated, or DB_UPDATE_ERROR on failure
     */
    @WorkerThread
    @Synchronized
    override fun updateAllEvents(table: Table): Int {
        val tName = table.tableName

        try {
            // First, read all rows
            val rowsUpdated = dbHelper.readableDatabase.query(
                tName,
                arrayOf(Column.ID, Column.DATA),
                null, null, null, null, null
            ).use { cursor -> reEncryptRows(cursor = cursor, tName = tName) }

            logger.verbose("Successfully migrated $rowsUpdated rows in table $tName")
            return rowsUpdated
        } catch (e: Exception) {
            logger.verbose("Error updating all events in table $tName. Recreating DB", e)
            dbHelper.deleteDatabase()
            return DB_UPDATE_ERROR.toInt()
        }
    }

    /**
     * Re-encrypts every row of [cursor] that is not already in the current encryption format.
     *
     * @return the number of rows updated
     */
    private fun reEncryptRows(cursor: Cursor, tName: String): Int {
        var rowsUpdated = 0
        val colId = cursor.getColumnIndexOrThrow(Column.ID)
        val colData = cursor.getColumnIndexOrThrow(Column.DATA)

        // Process each row
        while (cursor.moveToNext()) {
            if (reEncryptRow(cursor = cursor, colId = colId, colData = colData, tName = tName)) {
                rowsUpdated++
            }
        }
        return rowsUpdated
    }

    /**
     * Re-encrypts the current row of [cursor].
     *
     * @return true if the row was rewritten, false if it was skipped or the update touched no row
     */
    private fun reEncryptRow(cursor: Cursor, colId: Int, colData: Int, tName: String): Boolean {
        val id = cursor.getString(colId)
        val eventData = cursor.getString(colData)

        if (eventData != null && dbEncryptionHandler.isInCorrectEncryptionFormat(eventData)) {
            return false
        }
        // Decrypt the existing data
        val decryptedData = dbEncryptionHandler.unwrapDbData(eventData)
        if (decryptedData == null) {
            logger.verbose("Error decrypting data for id: $id from table: $tName, skipping")
            return false
        }

        // Re-encrypt with current encryption settings
        val reEncryptedData = dbEncryptionHandler.wrapDbData(decryptedData)

        // Update the row with re-encrypted data
        val cv = ContentValues().apply {
            put(Column.DATA, reEncryptedData)
        }

        val updated = dbHelper.writableDatabase.update(
            tName,
            cv,
            "${Column.ID} = ?",
            arrayOf(id)
        )
        return updated > 0
    }
}
