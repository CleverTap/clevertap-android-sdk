package com.clevertap.android.sdk.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_OUT_OF_MEMORY_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.DB_UPDATE_ERROR
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG

/**
 * Data class representing a delayed legacy in-app entry for batch operations
 *
 * @property inAppId the unique identifier for the in-app campaign
 * @property delay the delay in seconds before showing
 * @property inAppData the JSON data for the in-app notification
 */
internal data class DelayedLegacyInAppData(
    val inAppId: String,
    val delay: Int,
    val inAppData: String
)

internal interface DelayedLegacyInAppDAO {

    /**
     * Insert a delayed legacy in-app notification
     *
     * @param inAppId the unique identifier for the in-app campaign
     * @param delay the delay in seconds before showing
     * @param inAppData the JSON data for the in-app notification
     * @return the row ID of the newly inserted record, or error code if failed
     */
    @WorkerThread
    fun insert(singleDelayedInApp: DelayedLegacyInAppData): Long

    /**
     * Insert multiple delayed legacy in-app notifications in a batch transaction
     *
     * @param delayedInApps list of DelayedLegacyInAppData to insert
     * @return true if all insertions were successful, false otherwise
     */
    @WorkerThread
    fun insertBatch(delayedInApps: List<DelayedLegacyInAppData>): Boolean

    /**
     * Remove a delayed legacy in-app notification by its ID
     *
     * @param inAppId the unique identifier for the in-app campaign
     * @return true if the record was deleted, false otherwise
     */
    @WorkerThread
    fun remove(inAppId: String): Boolean

    /**
     * Fetch a single delayed legacy in-app notification by its ID
     *
     * @param inAppId the unique identifier for the in-app campaign
     * @return String containing the in-app data, or null if not found
     */
    @WorkerThread
    fun fetchSingleInApp(inAppId: String): String?

    /**
     * Remove all delayed legacy in-app notifications from the table
     *
     * @return true if the operation was successful, false otherwise
     */
    @WorkerThread
    fun clearAll(): Boolean
}

internal class DelayedLegacyInAppDAOImpl(
    private val db: DatabaseHelper,
    private val logger: Logger,
    private val table: Table
) : DelayedLegacyInAppDAO {

    @WorkerThread
    override fun insert(singleDelayedInApp: DelayedLegacyInAppData): Long {
        val inAppId = singleDelayedInApp.inAppId
        val delay = singleDelayedInApp.delay
        val inAppData = singleDelayedInApp.inAppData

        if (!db.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return DB_OUT_OF_MEMORY_ERROR
        }

        val tableName = table.tableName
        logger.verbose("Inserting delayed legacy in-app: $inAppId with delay: ${delay}s in $tableName")

        val now = Utils.getNowInMillis()
        val values = ContentValues().apply {
            put(Column.INAPP_ID, inAppId)
            put(Column.DELAY, delay)
            put(Column.DATA, inAppData)
            put(Column.CREATED_AT, now)
        }

        return try {
            db.writableDatabase.insertWithOnConflict(
                tableName,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        } catch (e: SQLiteException) {
            logger.verbose("Error adding delayed legacy in-app to table $tableName. Recreating DB. Exception: $e")
            db.deleteDatabase()
            DB_UPDATE_ERROR
        }
    }

    @WorkerThread
    override fun insertBatch(delayedInApps: List<DelayedLegacyInAppData>): Boolean {
        if (delayedInApps.isEmpty()) {
            logger.verbose("DelayedLegacyInAppDAO: Empty batch insert list")
            return true
        }

        if (!db.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return false
        }

        val tableName = table.tableName
        logger.verbose("DelayedLegacyInAppDAO: Batch insert for ${delayedInApps.size} delayed legacy in-apps")

        return try {
            db.writableDatabase.beginTransaction()
            val now = Utils.getNowInMillis()

            delayedInApps.forEach { delayedInApp ->
                logger.verbose("DelayedLegacyInAppDAO: Batch inserting ${delayedInApp.inAppId}")

                val values = ContentValues().apply {
                    put(Column.INAPP_ID, delayedInApp.inAppId)
                    put(Column.DELAY, delayedInApp.delay)
                    put(Column.DATA, delayedInApp.inAppData)
                    put(Column.CREATED_AT, now)
                }

                db.writableDatabase.insertWithOnConflict(
                    tableName,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }

            db.writableDatabase.setTransactionSuccessful()
            db.writableDatabase.endTransaction()
            logger.verbose("DelayedLegacyInAppDAO: Batch insert completed successfully for ${delayedInApps.size} items")
            true
        } catch (e: Exception) {
            logger.verbose("Failed to perform batch insert on table $tableName", e)
            try {
                db.writableDatabase.endTransaction()
            } catch (e: Exception) {
                logger.verbose("Failed to end transaction on table $tableName", e)
            }
            false
        }
    }

    @WorkerThread
    override fun remove(inAppId: String): Boolean {
        val tableName = table.tableName

        return try {
            logger.verbose("Removing delayed legacy in-app: $inAppId from $tableName")
            val rowsDeleted = db.writableDatabase.delete(
                tableName,
                "${Column.INAPP_ID} = ?",
                arrayOf(inAppId)
            )
            rowsDeleted > 0
        } catch (e: SQLiteException) {
            logger.verbose("Could not remove delayed legacy in-app from database $tableName.", e)
            false
        }
    }

    @WorkerThread
    override fun fetchSingleInApp(inAppId: String): String? {
        val tableName = table.tableName
        val selection = "${Column.INAPP_ID} = ?"
        val selectionArgs = arrayOf(inAppId)
        val projection = arrayOf(Column.DATA)

        return try {
            db.readableDatabase.query(
                tableName, projection, selection, selectionArgs, null, null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA))
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logger.verbose("Could not fetch delayed legacy in-app from database $tableName.", e)
            null
        }
    }

    @WorkerThread
    override fun clearAll(): Boolean {
        val tableName = table.tableName

        return try {
            logger.verbose("Clearing all delayed legacy in-apps from $tableName")
            db.writableDatabase.delete(tableName, null, null)
            logger.verbose("Successfully cleared all delayed legacy in-apps from $tableName")
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error clearing all delayed legacy in-apps from table $tableName.", e)
            false
        }
    }
}