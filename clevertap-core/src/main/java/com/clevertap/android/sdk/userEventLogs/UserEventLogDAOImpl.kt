package com.clevertap.android.sdk.userEventLogs

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_OUT_OF_MEMORY_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_UPDATE_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table

internal class UserEventLogDAOImpl(
    private val db: DatabaseHelper,
    private val logger: Logger,
    private val table: Table
) : UserEventLogDAO {


    @WorkerThread
    override fun insertEventByDeviceID(deviceID: String, eventName: String): Long {
        if (!db.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return DB_OUT_OF_MEMORY_ERROR
        }
        val tableName = table.tableName
        logger.verbose("Inserting event $eventName with deviceID = $deviceID in $tableName")
        val now = Utils.getNowInMillis()
        val values = ContentValues().apply {
            put(Column.EVENT_NAME, eventName)
            put(Column.FIRST_TS, now)
            put(Column.LAST_TS, now)
            put(Column.COUNT, 1)
            put(Column.DEVICE_ID, deviceID)
        }
        return try {
            db.writableDatabase.insertWithOnConflict(
                tableName,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        } catch (e: SQLiteException) {
            logger.verbose("Error adding row to table $tableName Recreating DB")
            db.deleteDatabase()
            DB_UPDATE_ERROR
        }
    }

    @WorkerThread
    override fun updateEventByDeviceID(deviceID: String, eventName: String): Boolean {
        val now = System.currentTimeMillis()
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.EVENT_NAME} = ?"
        val selectionArgs = arrayOf(deviceID, eventName)
        val projection = arrayOf(Column.COUNT)
        return try {
            // Below code can be replace with nested SQL query but couldn't found any difference in the performance
            db.readableDatabase.query(
                tName, projection, selection, selectionArgs, null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val countOfEvents = cursor.getInt(cursor.getColumnIndexOrThrow(Column.COUNT))
                    val values = ContentValues().apply {
                        put(Column.LAST_TS, now)
                        put(Column.COUNT, countOfEvents + 1)
                    }
                    val updatedRow =
                        db.writableDatabase.update(tName, values, selection, selectionArgs)
                    updatedRow > 0
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
            false
        }
    }

    @WorkerThread
    override fun readEventByDeviceID(deviceID: String, eventName: String): UserEventLog? {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.EVENT_NAME} = ?"
        val selectionArgs = arrayOf(deviceID, eventName)
        return try {
            db.readableDatabase.query(
                tName, null, selection, selectionArgs, null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val eventLog = UserEventLog(
                        eventName = cursor.getString(cursor.getColumnIndexOrThrow(Column.EVENT_NAME)),
                        firstTs = cursor.getLong(cursor.getColumnIndexOrThrow(Column.FIRST_TS)),
                        lastTs = cursor.getLong(cursor.getColumnIndexOrThrow(Column.LAST_TS)),
                        countOfEvents = cursor.getInt(cursor.getColumnIndexOrThrow(Column.COUNT)),
                        deviceID = cursor.getString(cursor.getColumnIndexOrThrow(Column.DEVICE_ID))
                    )
                    eventLog
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
            null
        }
    }

    @WorkerThread
    override fun eventExistsByDeviceID(deviceID: String, eventName: String): Boolean {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.EVENT_NAME} = ?"
        val selectionArgs = arrayOf(deviceID, eventName)

        val query = """
            SELECT EXISTS(
                SELECT 1 
                FROM $tName 
                WHERE $selection
            ) AS eventExists;
        """.trimIndent()

        return try {
            db.readableDatabase.rawQuery(query, selectionArgs).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(cursor.getColumnIndexOrThrow("eventExists")) == 1
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
            false
        }
    }

    @WorkerThread
    override fun allEventsByDeviceID(deviceID: String): List<UserEventLog> {
        val tName = table.tableName
        val eventList = mutableListOf<UserEventLog>()
        val selection = "${Column.DEVICE_ID} = ?"
        val selectionArgs = arrayOf(deviceID)
        val orderBy = "${Column.LAST_TS} ASC"

        return try {
            db.readableDatabase.query(
                tName, null, selection, selectionArgs, null, null, orderBy, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val eventLog = UserEventLog(
                        eventName = cursor.getString(cursor.getColumnIndexOrThrow(Column.EVENT_NAME)),
                        firstTs = cursor.getLong(cursor.getColumnIndexOrThrow(Column.FIRST_TS)),
                        lastTs = cursor.getLong(cursor.getColumnIndexOrThrow(Column.LAST_TS)),
                        countOfEvents = cursor.getInt(cursor.getColumnIndexOrThrow(Column.COUNT)),
                        deviceID = cursor.getString(cursor.getColumnIndexOrThrow(Column.DEVICE_ID))
                    )
                    eventList.add(eventLog)
                }
                eventList
            } ?: emptyList()
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
            emptyList()
        }
    }

    @WorkerThread
    override fun allEvents(): List<UserEventLog> {
        val tName = table.tableName
        val eventList = mutableListOf<UserEventLog>()
        val orderBy = "${Column.LAST_TS} ASC"

        return try {
            db.readableDatabase.query(
                tName, null, null, null, null, null, orderBy
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val eventLog = UserEventLog(
                        eventName = cursor.getString(cursor.getColumnIndexOrThrow(Column.EVENT_NAME)),
                        firstTs = cursor.getLong(cursor.getColumnIndexOrThrow(Column.FIRST_TS)),
                        lastTs = cursor.getLong(cursor.getColumnIndexOrThrow(Column.LAST_TS)),
                        countOfEvents = cursor.getInt(cursor.getColumnIndexOrThrow(Column.COUNT)),
                        deviceID = cursor.getString(cursor.getColumnIndexOrThrow(Column.DEVICE_ID))
                    )
                    eventList.add(eventLog)
                }
                eventList
            } ?: emptyList()
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
            emptyList()
        }
    }

    @WorkerThread
    override fun cleanUpExtraEvents(threshold: Int): Boolean {
        val tName = table.tableName
        val rowCountQuery = "SELECT COUNT(*) FROM $tName"
        val deleteQuery = "DELETE FROM $tName WHERE ${Column.DEVICE_ID} IN (" +
                "SELECT ${Column.DEVICE_ID} FROM $tName ORDER BY ${Column.LAST_TS} ASC LIMIT ?)"

        return try {
            db.readableDatabase.rawQuery(rowCountQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val rowCount = cursor.getInt(0)
                    if (rowCount > threshold) {
                        val excessRows = rowCount - threshold
                        db.writableDatabase.execSQL(deleteQuery, arrayOf(excessRows.toString()))
                        logger.verbose("$excessRows least recently used rows deleted from $tName")
                    }
                }
            }
            true
        } catch (e: Exception) {
            logger.verbose("Error cleaning up extra events in $tName.", e)
            false
        }
    }

    //TODO: Create indexes
}
