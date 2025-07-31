package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_OUT_OF_MEMORY_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_UPDATE_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class EventDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: Logger
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
            put(Column.DATA, event.toString())
            put(Column.CREATED_AT, System.currentTimeMillis())
        }

        return try {
            dbHelper.writableDatabase.insert(tableName, null, cv)
            val sql = "SELECT COUNT(*) FROM $tableName"
            val statement = dbHelper.writableDatabase.compileStatement(sql)
            statement.simpleQueryForLong()
        } catch (e: Exception) {
            logger.verbose("Error adding data to table $tableName. Recreating DB", e)
            dbHelper.deleteDatabase()
            DB_UPDATE_ERROR
        }
    }

    @WorkerThread
    override fun fetchEvents(table: Table, limit: Int): JSONObject? {
        val tName = table.tableName
        var lastId: String? = null
        val events = JSONArray()
        
        try {
            dbHelper.readableDatabase.query(
                tName, null, null, null, null, null, 
                "${Column.CREATED_AT} ASC", limit.toString()
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.isLast) {
                        lastId = cursor.getString(cursor.getColumnIndexOrThrow(Column.ID))
                    }
                    try {
                        val j = JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA)))
                        events.put(j)
                    } catch (e: JSONException) {
                        // Ignore malformed JSON
                    }
                }
            }
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
            lastId = null
        }

        return lastId?.let {
            try {
                val ret = JSONObject()
                ret.put(it, events)
                ret
            } catch (e: JSONException) {
                null
            }
        }
    }

    @WorkerThread
    override fun cleanupEventsFromLastId(lastId: String, table: Table) {
        val tName = table.tableName
        try {
            dbHelper.writableDatabase.delete(tName, "${Column.ID} <= ?", arrayOf(lastId))
        } catch (e: Exception) {
            logger.verbose("Error removing sent data from table $tName. Recreating DB", e)
            dbHelper.deleteDatabase()
        }
    }

    @WorkerThread
    override fun cleanupStaleEvents(table: Table) {
        val time = (System.currentTimeMillis() - DATA_EXPIRATION) / 1000
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
}
