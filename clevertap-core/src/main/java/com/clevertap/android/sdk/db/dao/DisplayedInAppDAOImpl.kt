package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.sdk.utils.Clock

internal class DisplayedInAppDAOImpl(
    private val db: DatabaseHelper,
    private val logger: ILogger,
    private val clock: Clock = Clock.SYSTEM,
) : DisplayedInAppDAO {

    private val tableName = Table.DISPLAYED_INAPP.tableName

    @WorkerThread
    override fun save(displayedInApp: DisplayedInAppData): Boolean {
        if (!db.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return false
        }

        val values = ContentValues().apply {
            put(Column.INAPP_ID, displayedInApp.inAppId)
            put(Column.DATA, displayedInApp.inAppData)
            put(Column.TTL, displayedInApp.timeToLive)
            put(Column.CREATED_AT, clock.currentTimeMillis())
        }

        return try {
            val database = db.writableDatabase
            database.beginTransaction()
            try {
                logger.verbose("Saving displayed in-app ${displayedInApp.inAppId} to $tableName")
                database.delete(tableName, null, null)
                database.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
            true
        } catch (e: SQLiteException) {
            logger.verbose("Could not save displayed in-app to $tableName", e)
            false
        }
    }

    @WorkerThread
    override fun fetch(inAppId: String): DisplayedInAppData? {
        val selection = "${Column.INAPP_ID} = ?"
        val selectionArgs = arrayOf(inAppId)
        val projection = arrayOf(Column.DATA, Column.TTL)

        return try {
            db.readableDatabase.query(
                tableName, projection, selection, selectionArgs, null, null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    DisplayedInAppData(
                        inAppId = inAppId,
                        inAppData = cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA)),
                        timeToLive = cursor.getLong(cursor.getColumnIndexOrThrow(Column.TTL))
                    )
                } else {
                    null
                }
            }
        } catch (e: SQLiteException) {
            logger.verbose("Could not fetch displayed in-app from $tableName", e)
            null
        }
    }

    @WorkerThread
    override fun remove(inAppId: String): Boolean {
        return try {
            logger.verbose("Removing displayed in-app $inAppId from $tableName")
            val rowsDeleted = db.writableDatabase.delete(
                tableName,
                "${Column.INAPP_ID} = ?",
                arrayOf(inAppId)
            )
            rowsDeleted > 0
        } catch (e: SQLiteException) {
            logger.verbose("Could not remove displayed in-app from $tableName", e)
            false
        }
    }

    @WorkerThread
    override fun clearAll(): Boolean {
        return try {
            logger.verbose("Clearing $tableName")
            db.writableDatabase.delete(tableName, null, null)
            true
        } catch (e: SQLiteException) {
            logger.verbose("Could not clear $tableName", e)
            false
        }
    }
}
