package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import androidx.core.database.sqlite.transaction
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.sdk.utils.Clock

internal class PushPreviewDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: ILogger,
    private val dbEncryptionHandler: DBEncryptionHandler,
    private val clock: Clock,
) : PushPreviewDAO {

    @WorkerThread
    override fun save(previewKey: String, data: String, expiresAtSeconds: Long, collapseKey: String?): Boolean {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return false
        }
        val nowSeconds = clock.currentTimeSeconds()
        val values = ContentValues().apply {
            put(Column.PREVIEW_KEY, previewKey)
            // Encrypted only at FULL_DATA, like events and inbox. Unlike those tables this
            // one is not re-wrapped by CryptMigrator when the encryption level changes:
            // unwrapDbData reads both plain and encrypted text, and a row lives at most
            // 24 hours, so a level change leaves nothing behind that needs migrating.
            put(Column.DATA, dbEncryptionHandler.wrapDbData(data))
            put(Column.EXPIRES, expiresAtSeconds)
            put(Column.CREATED_AT, nowSeconds)
            put(Column.COLLAPSE_KEY, collapseKey)
        }
        return try {
            // One transaction, one fsync. Three auto-committed statements would cost
            // three fsyncs, which is the whole cost of this call on slow flash.
            dbHelper.writableDatabase.transaction {
                // INSERT OR REPLACE: a repeat of the same preview refreshes its expiry
                // instead of adding a second row.
                insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                delete(TABLE, "${Column.EXPIRES} <= $nowSeconds", null)
                trimToCap(this)
            }
            true
        } catch (e: SQLiteException) {
            // Log only. A preview store failure must never take the whole database down.
            logger.verbose("Error saving push preview into $TABLE", e)
            false
        }
    }

    @WorkerThread
    override fun find(previewKey: String): String? {
        val nowSeconds = clock.currentTimeSeconds()
        try {
            dbHelper.readableDatabase.query(
                TABLE,
                arrayOf(Column.DATA),
                "${Column.PREVIEW_KEY} = ? AND ${Column.EXPIRES} > $nowSeconds",
                arrayOf(previewKey),
                null, null, null, "1"
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    return dbEncryptionHandler.unwrapDbData(cursor.getString(0)).also { data ->
                        // A row that exists but cannot be decrypted is a key problem, not a missing push.
                        if (data == null) logger.verbose("Push preview row for $previewKey is unreadable")
                    }
                }
            }
        } catch (e: SQLiteException) {
            logger.verbose("Error reading push preview from $TABLE", e)
        }
        return null
    }

    /**
     * Keeps the newest [MAX_ROWS] rows. Two plain statements on purpose: a SELECT
     * for the id of the oldest row to keep, then a DELETE below it. Both parse on
     * SQLite 3.8.10.2 (Android 6.0) without question.
     *
     * Example: ids 1..53 present, the SELECT returns 4, the DELETE removes 1, 2, 3.
     * With fewer than MAX_ROWS rows the SELECT is empty and nothing is deleted.
     */
    private fun trimToCap(db: SQLiteDatabase) {
        // Android's query() accepts the limit only as "N" or "OFFSET,N". "49,1" becomes
        // SQL "LIMIT 49,1": skip the 49 newest rows, return the 50th.
        val oldestIdToKeep = db.query(
            TABLE,
            arrayOf(Column.ID),
            null, null, null, null,
            "${Column.ID} DESC",
            "${MAX_ROWS - 1},1"
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

        if (oldestIdToKeep != null) {
            db.delete(TABLE, "${Column.ID} < ?", arrayOf(oldestIdToKeep.toString()))
        }
    }

    private companion object {
        val TABLE = Table.PUSH_PREVIEWS.tableName
        const val MAX_ROWS = 50
    }
}
