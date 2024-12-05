package com.clevertap.android.sdk.usereventlogs

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

    // Replace multiple params with single POJO param if param length increases
    @WorkerThread
    override fun insertEvent(
        deviceID: String,
        eventName: String,
        normalizedEventName: String
    ): Long {
        if (!db.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return DB_OUT_OF_MEMORY_ERROR
        }
        val tableName = table.tableName
        logger.verbose("Inserting event $eventName with deviceID = $deviceID in $tableName")
        val now = Utils.getNowInMillis()
        val values = ContentValues().apply {
            put(Column.EVENT_NAME, eventName)
            put(Column.NORMALIZED_EVENT_NAME, normalizedEventName)
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
    override fun updateEventByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Boolean {
        val tableName = table.tableName
        val now = Utils.getNowInMillis()

        return try {
            val query = """
            UPDATE $tableName 
            SET 
                ${Column.COUNT} = ${Column.COUNT} + 1,
                ${Column.LAST_TS} = ?
            WHERE ${Column.DEVICE_ID} = ? 
            AND ${Column.NORMALIZED_EVENT_NAME} = ?;
        """.trimIndent()

            logger.verbose("Updating event $normalizedEventName with deviceID = $deviceID in $tableName")
            db.writableDatabase.execSQL(query, arrayOf(now, deviceID, normalizedEventName))
            true
        } catch (e: Exception) {
            logger.verbose("Could not update event in database $tableName.", e)
            false
        }
    }

    @WorkerThread
    override fun upsertEventsByDeviceIdAndNormalizedEventName(
        deviceID: String,
        setOfActualAndNormalizedEventNamePair: Set<Pair<String, String>>
    ): Boolean {
        val tableName = table.tableName
        logger.verbose("UserEventLog: upSert EventLog for bulk events")
        return try {
            db.writableDatabase.beginTransaction()
            setOfActualAndNormalizedEventNamePair.forEach {
                if (eventExistsByDeviceIdAndNormalizedEventName(deviceID, it.second)) {
                    logger.verbose("UserEventLog: Updating EventLog for event $it")
                    updateEventByDeviceIdAndNormalizedEventName(deviceID, it.second)
                } else {
                    logger.verbose("UserEventLog: Inserting EventLog for event $it")
                    insertEvent(deviceID, it.first, it.second)
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
    override fun readEventByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): UserEventLog? {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.NORMALIZED_EVENT_NAME} = ?"
        val selectionArgs = arrayOf(deviceID, normalizedEventName)
        return try {
            db.readableDatabase.query(
                tName, null, selection, selectionArgs, null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val eventLog = UserEventLog(
                        eventName = cursor.getString(cursor.getColumnIndexOrThrow(Column.EVENT_NAME)),
                        normalizedEventName = cursor.getString(cursor.getColumnIndexOrThrow(Column.NORMALIZED_EVENT_NAME)),
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
    override fun readEventCountByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Int =
        readEventColumnByDeviceIdAndNormalizedEventName(
            deviceID,
            normalizedEventName,
            Column.COUNT,
            defaultValueExtractor = { -1 },
            valueExtractor = { cursor, columnName ->
                cursor.getInt(cursor.getColumnIndexOrThrow(columnName))
            }
        )


    @WorkerThread
    override fun readEventFirstTsByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Long =
        readEventColumnByDeviceIdAndNormalizedEventName(
            deviceID,
            normalizedEventName,
            Column.FIRST_TS,
            defaultValueExtractor = { -1L },
            valueExtractor = { cursor, columnName ->
                cursor.getLong(cursor.getColumnIndexOrThrow(columnName))
            }
        )

    @WorkerThread
    override fun readEventLastTsByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Long =
        readEventColumnByDeviceIdAndNormalizedEventName(
            deviceID,
            normalizedEventName,
            Column.LAST_TS,
            defaultValueExtractor = { -1L },
            valueExtractor = { cursor, columnName ->
                cursor.getLong(cursor.getColumnIndexOrThrow(columnName))
            }
        )

    @WorkerThread
    override fun eventExistsByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Boolean {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.NORMALIZED_EVENT_NAME} = ?"
        val selectionArgs = arrayOf(deviceID, normalizedEventName)
        val resultColumn = "eventExists"

        val query = """
            SELECT EXISTS(
                SELECT 1 
                FROM $tName 
                WHERE $selection
            ) AS $resultColumn;
        """.trimIndent()

        return try {
            db.readableDatabase.rawQuery(query, selectionArgs)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(cursor.getColumnIndexOrThrow(resultColumn)) == 1
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
    override fun eventExistsByDeviceIdAndNormalizedEventNameAndCount(deviceID: String, normalizedEventName: String, count: Int): Boolean {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.NORMALIZED_EVENT_NAME} = ? AND ${Column.COUNT} = ?"
        val selectionArgs = arrayOf(deviceID, normalizedEventName, count.toString())
        val resultColumn = "eventExists"

        val query = """
            SELECT EXISTS(
                SELECT 1 
                FROM $tName 
                WHERE $selection
                ) AS $resultColumn;
        """.trimIndent()
        return try {
            db.readableDatabase.rawQuery(query, selectionArgs)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(cursor.getColumnIndexOrThrow(resultColumn)) == 1
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
                        normalizedEventName = cursor.getString(cursor.getColumnIndexOrThrow(Column.NORMALIZED_EVENT_NAME)),
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
                        normalizedEventName = cursor.getString(cursor.getColumnIndexOrThrow(Column.NORMALIZED_EVENT_NAME)),
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
            WHERE (${Column.NORMALIZED_EVENT_NAME}, ${Column.DEVICE_ID}) IN (
                SELECT ${Column.NORMALIZED_EVENT_NAME}, ${Column.DEVICE_ID}
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
    private fun <T> readEventColumnByDeviceIdAndNormalizedEventName(
        deviceID: String,
        normalizedEventName: String,
        column: String,
        defaultValueExtractor: () -> T,
        valueExtractor: (cursor: Cursor, columnName: String) -> T
    ): T {
        val tName = table.tableName
        val selection = "${Column.DEVICE_ID} = ? AND ${Column.NORMALIZED_EVENT_NAME} = ?"
        val selectionArgs = arrayOf(deviceID, normalizedEventName)
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
