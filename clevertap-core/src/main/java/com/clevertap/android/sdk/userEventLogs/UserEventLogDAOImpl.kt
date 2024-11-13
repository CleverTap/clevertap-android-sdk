package com.clevertap.android.sdk.userEventLogs

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
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
        } catch (e: Exception) {
            logger.verbose("Error adding row to table $tableName Recreating DB")
            db.deleteDatabase()
            DB_UPDATE_ERROR
        }
    }

    @WorkerThread
    override fun updateEventByDeviceID(deviceID: String, eventName: String): Boolean {
        val tableName = table.tableName
        val now = Utils.getNowInMillis()

        return try {
            val query = """
            UPDATE $tableName 
            SET 
                ${Column.COUNT} = ${Column.COUNT} + 1,
                ${Column.LAST_TS} = ?
            WHERE ${Column.DEVICE_ID} = ? 
            AND ${Column.EVENT_NAME} = ?;
        """.trimIndent()

            logger.verbose("Updating event $eventName with deviceID = $deviceID in $tableName")
            db.writableDatabase.execSQL(query, arrayOf(now, deviceID, eventName))
            true
        } catch (e: Exception) {
            logger.verbose("Could not update event in database $tableName.", e)
            false
        }
    }

    @WorkerThread
    override fun upsertEventsByDeviceID(deviceID: String, eventNameList: Set<String>): Boolean {
        val tableName = table.tableName
        logger.verbose("UserEventLog: upSert EventLog for bulk events")
        return try {
            db.writableDatabase.beginTransaction()
            eventNameList.forEach {
                if (eventExistsByDeviceID(deviceID, it)) {
                    logger.verbose("UserEventLog: Updating EventLog for event $it")
                    updateEventByDeviceID(deviceID, it)
                } else {
                    logger.verbose("UserEventLog: Inserting EventLog for event $it")
                    insertEventByDeviceID(deviceID, it)
                }
            }
            db.writableDatabase.setTransactionSuccessful()
            db.writableDatabase.endTransaction()
            true
        } catch (e: Exception) {
            logger.verbose("Failed to perform bulk upsert on table $tableName", e)
            try {
                db.writableDatabase.endTransaction()
            } catch (e: Exception) {
                logger.verbose("Failed to end transaction on table $tableName", e)
            }
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
    override fun readEventCountByDeviceID(deviceID: String, eventName: String): Int =
        readEventColumnByDeviceID(
            deviceID,
            eventName,
            Column.COUNT,
            defaultValueExtractor = { -1 },
            valueExtractor = { cursor, columnName ->
                cursor.getInt(cursor.getColumnIndexOrThrow(columnName))
            }
        )


    @WorkerThread
    override fun readEventFirstTsByDeviceID(deviceID: String, eventName: String): Long =
        readEventColumnByDeviceID(
            deviceID,
            eventName,
            Column.FIRST_TS,
            defaultValueExtractor = { -1L },
            valueExtractor = { cursor, columnName ->
                cursor.getLong(cursor.getColumnIndexOrThrow(columnName))
            }
        )

    @WorkerThread
    override fun readEventLastTsByDeviceID(deviceID: String, eventName: String): Long =
        readEventColumnByDeviceID(
            deviceID,
            eventName,
            Column.LAST_TS,
            defaultValueExtractor = { -1L },
            valueExtractor = { cursor, columnName ->
                cursor.getLong(cursor.getColumnIndexOrThrow(columnName))
            }
        )

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
            db.readableDatabase.rawQuery(query, selectionArgs)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(cursor.getColumnIndexOrThrow("eventExists")) == 1
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
    override fun eventExistsByDeviceIDAndCount(deviceID: String, eventName: String, count: Int): Boolean {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.EVENT_NAME} = ? AND ${Column.COUNT} = ?"
        val selectionArgs = arrayOf(deviceID, eventName, count.toString())
        val query = """
            SELECT EXISTS(
                SELECT 1 
                FROM $tName 
                WHERE $selection
                ) AS eventExists;
        """.trimIndent()
        return try {
            db.readableDatabase.rawQuery(query, selectionArgs)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(cursor.getColumnIndexOrThrow("eventExists")) == 1
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
            false
        }
    }

    // Create index on deviceID,lastTs column if this method is frequently used
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

    // Create index on lastTs column if this method is frequently used
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
    override fun cleanUpExtraEvents(threshold: Int, numberOfRowsToCleanup: Int): Boolean {
        if (threshold <= 0) {
            logger.verbose("Invalid threshold value: $threshold. Threshold should be greater than 0")
            return false
        }
        if (numberOfRowsToCleanup < 0) {
            logger.verbose("Invalid numberOfRowsToCleanup value: $numberOfRowsToCleanup. Should be greater than or equal to 0")
            return false
        }
        if (numberOfRowsToCleanup >= threshold) {
            logger.verbose("Invalid numberOfRowsToCleanup value: $numberOfRowsToCleanup. Should be less than threshold: $threshold")
            return false
        }

        val tName = table.tableName
        val numberOfRowsToKeep = threshold - numberOfRowsToCleanup

        return try {
            // SQL query to delete only the least recently used rows, using a subquery with LIMIT
            // When above threshold is reached, delete in such a way that (threshold - numberOfRowsToCleanup) rows exists after cleanup
            val query = """
            DELETE FROM $tName
            WHERE (${Column.EVENT_NAME}, ${Column.DEVICE_ID}) IN (
                SELECT ${Column.EVENT_NAME}, ${Column.DEVICE_ID}
                FROM $tName
                ORDER BY ${Column.LAST_TS} ASC 
                LIMIT (
                SELECT CASE 
                    WHEN COUNT(*) > ? THEN COUNT(*) - ?
                    ELSE 0
                END 
                FROM $tName
                )
            );
        """.trimIndent()

            // Execute the delete query with the threshold as an argument
            db.writableDatabase.execSQL(query, arrayOf(threshold,numberOfRowsToKeep))
            logger.verbose("If row count is above $threshold then only keep $numberOfRowsToKeep rows in $tName")
            true
        } catch (e: Exception) {
            logger.verbose("Error cleaning up extra events in $tName.", e)
            false
        }
    }

    @WorkerThread
    private fun <T> readEventColumnByDeviceID(
        deviceID: String,
        eventName: String,
        column: String,
        defaultValueExtractor: () -> T,
        valueExtractor: (cursor: Cursor, columnName: String) -> T
    ): T {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.EVENT_NAME} = ?"
        val selectionArgs = arrayOf(deviceID, eventName)
        val projection = arrayOf(column)

        return try {
            db.readableDatabase.query(
                tName, projection, selection, selectionArgs, null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    valueExtractor(cursor, column)
                } else {
                    defaultValueExtractor()
                }
            } ?: defaultValueExtractor()
        } catch (e: Exception) {
            logger.verbose("Could not fetch $column from database $tName.", e)
            defaultValueExtractor()
        }
    }

}
