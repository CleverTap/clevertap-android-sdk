package com.clevertap.android.sdk.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.utils.Clock

/**
 * A row of the displayed in-app table: the full-screen in-app that is on screen, kept so it can be
 * shown again after the app process is killed.
 *
 * @property inAppId the unique identifier for the in-app campaign
 * @property inAppData the encrypted JSON of the in-app notification
 * @property timeToLive the in-app's time to live, in seconds since the epoch
 */
internal data class DisplayedInAppData(
    val inAppId: String,
    val inAppData: String,
    val timeToLive: Long
)

internal interface DisplayedInAppDAO {
    /**
     * Save the displayed in-app. Only one is kept: any other row is removed in the same transaction.
     *
     * @return true if it was saved, false otherwise (the old row is then kept)
     */
    @WorkerThread
    fun save(displayedInApp: DisplayedInAppData): Boolean

    /**
     * Fetch the displayed in-app by its ID
     *
     * @return the row, or null if not found or it could not be read
     */
    @WorkerThread
    fun fetch(inAppId: String): DisplayedInAppData?

    /**
     * Remove the displayed in-app by its ID
     *
     * @return true if the record was deleted, false otherwise
     */
    @WorkerThread
    fun remove(inAppId: String): Boolean

    /**
     * Remove every row from the table
     *
     * @return true if the operation was successful, false otherwise
     */
    @WorkerThread
    fun clearAll(): Boolean
}

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
