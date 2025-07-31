package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table.UNINSTALL_TS

internal class UninstallTimestampDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: Logger
) : UninstallTimestampDAO {

    @WorkerThread
    override fun storeUninstallTimestamp() {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }
        
        val tableName = UNINSTALL_TS.tableName
        val cv = ContentValues().apply {
            put(Column.CREATED_AT, System.currentTimeMillis())
        }
        
        try {
            dbHelper.writableDatabase.insert(tableName, null, cv)
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName. Recreating DB", e)
            dbHelper.deleteDatabase()
        }
    }

    @WorkerThread
    override fun getLastUninstallTimestamp(): Long {
        val tName = UNINSTALL_TS.tableName
        var timestamp: Long = 0
        
        try {
            dbHelper.readableDatabase.query(
                tName, null, null, null, null, null, 
                "${Column.CREATED_AT} DESC", "1"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Column.CREATED_AT))
                }
            }
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
        }
        return timestamp
    }
}
