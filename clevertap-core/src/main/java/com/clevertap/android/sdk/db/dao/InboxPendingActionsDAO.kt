package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import androidx.core.database.sqlite.transaction
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONObject

internal object PendingDeleteState {
    const val PENDING_SEND = "PENDING_SEND"
    const val AWAITING_CONFIRM = "AWAITING_CONFIRM"
}

internal data class PendingDelete(
    val messageId: String,
    val wzrkParams: JSONObject?,
    val expiresAt: Long
)

/**
 * Persists "user deleted this locally" / "user read this locally" intents so a
 * later V2 fetch can't resurrect a deleted message or overwrite a locally-read
 * message until the server confirms the action.
 */
internal interface InboxPendingActionsDAO {

    @WorkerThread fun getPendingDeleteIds(userId: String): Set<String>
    @WorkerThread fun getPendingDeletes(userId: String): List<PendingDelete>
    @WorkerThread fun getPendingReads(userId: String): Set<String>

    @WorkerThread fun addPendingDelete(
        messageId: String,
        userId: String,
        wzrkParams: JSONObject?,
        expiresAt: Long
    ): Boolean
    @WorkerThread fun removePendingDelete(messageId: String, userId: String): Boolean
    @WorkerThread fun addPendingRead(messageId: String, userId: String): Boolean
    @WorkerThread fun removePendingRead(messageId: String, userId: String): Boolean

    @WorkerThread fun addPendingDeletes(rows: List<PendingDelete>, userId: String): Boolean
    @WorkerThread fun removePendingDeletes(messageIds: List<String>, userId: String): Boolean
    @WorkerThread fun markPendingDeletesAwaitingConfirm(messageIds: List<String>, userId: String): Boolean
    @WorkerThread fun removeExpiredAwaitingConfirm(userId: String, nowSeconds: Long): Int

    @WorkerThread fun addPendingReads(messageIds: List<String>, userId: String): Boolean
    @WorkerThread fun removePendingReads(messageIds: List<String>, userId: String): Boolean
}

internal class InboxPendingActionsDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: ILogger,
    private val clock: Clock = Clock.SYSTEM
) : InboxPendingActionsDAO {

    @WorkerThread
    override fun getPendingDeleteIds(userId: String): Set<String> {
        val result = LinkedHashSet<String>()
        try {
            dbHelper.readableDatabase.query(
                Table.INBOX_PENDING_DELETES.tableName,
                arrayOf(Column.ID),
                "${Column.USER_ID} = ?",
                arrayOf(userId),
                null, null, null
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Column.ID)
                while (cursor.moveToNext()) result.add(cursor.getString(idIdx))
            }
        } catch (e: Exception) {
            logger.verbose("Error reading from ${Table.INBOX_PENDING_DELETES.tableName}", e)
        }
        return result
    }

    @WorkerThread
    override fun getPendingDeletes(userId: String): List<PendingDelete> {
        val out = ArrayList<PendingDelete>()
        try {
            dbHelper.readableDatabase.query(
                Table.INBOX_PENDING_DELETES.tableName,
                arrayOf(Column.ID, Column.WZRKPARAMS, Column.EXPIRES),
                "${Column.USER_ID} = ? AND ${Column.STATE} = ?",
                arrayOf(userId, PendingDeleteState.PENDING_SEND),
                null, null, null
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Column.ID)
                val paramsIdx = cursor.getColumnIndexOrThrow(Column.WZRKPARAMS)
                val expiresIdx = cursor.getColumnIndexOrThrow(Column.EXPIRES)
                while (cursor.moveToNext()) {
                    val raw = if (cursor.isNull(paramsIdx)) null else cursor.getString(paramsIdx)
                    val params = raw?.let {
                        try {
                            JSONObject(it)
                        } catch (e: Exception) {
                            logger.verbose("Skipping malformed wzrkParams for pending delete row", e)
                            null
                        }
                    }
                    out.add(
                        PendingDelete(
                            cursor.getString(idIdx),
                            params,
                            cursor.getLong(expiresIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.verbose("Error reading from ${Table.INBOX_PENDING_DELETES.tableName}", e)
        }
        return out
    }

    @WorkerThread
    override fun getPendingReads(userId: String): Set<String> =
        readIds(Table.INBOX_PENDING_READS.tableName, userId)

    @WorkerThread
    override fun addPendingDelete(
        messageId: String,
        userId: String,
        wzrkParams: JSONObject?,
        expiresAt: Long
    ): Boolean {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return false
        }
        val cv = ContentValues().apply {
            put(Column.USER_ID, userId)
            put(Column.ID, messageId)
            put(Column.WZRKPARAMS, wzrkParams?.toString())
            put(Column.EXPIRES, expiresAt)
            put(Column.CREATED_AT, clock.currentTimeSeconds())
        }
        return try {
            dbHelper.writableDatabase.insertWithOnConflict(
                Table.INBOX_PENDING_DELETES.tableName, null, cv, SQLiteDatabase.CONFLICT_IGNORE
            ) >= 0
        } catch (e: SQLiteException) {
            logger.verbose("Error inserting into ${Table.INBOX_PENDING_DELETES.tableName}", e)
            false
        }
    }

    @WorkerThread
    override fun removePendingDelete(messageId: String, userId: String): Boolean =
        deleteOne(Table.INBOX_PENDING_DELETES.tableName, messageId, userId)

    @WorkerThread
    override fun addPendingRead(messageId: String, userId: String): Boolean =
        insertOne(Table.INBOX_PENDING_READS.tableName, messageId, userId)

    @WorkerThread
    override fun removePendingRead(messageId: String, userId: String): Boolean =
        deleteOne(Table.INBOX_PENDING_READS.tableName, messageId, userId)

    @WorkerThread
    override fun addPendingDeletes(rows: List<PendingDelete>, userId: String): Boolean {
        if (rows.isEmpty()) return true
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return false
        }
        val db = dbHelper.writableDatabase
        return try {
            db.transaction {
                val now = clock.currentTimeSeconds()
                rows.forEach { row ->
                    val cv = ContentValues().apply {
                        put(Column.USER_ID, userId)
                        put(Column.ID, row.messageId)
                        put(Column.WZRKPARAMS, row.wzrkParams?.toString())
                        put(Column.EXPIRES, row.expiresAt)
                        put(Column.CREATED_AT, now)
                    }
                    insertWithOnConflict(
                        Table.INBOX_PENDING_DELETES.tableName, null, cv, SQLiteDatabase.CONFLICT_IGNORE
                    )
                }
                true
            }
        } catch (e: SQLiteException) {
            logger.verbose("Error batch-inserting into ${Table.INBOX_PENDING_DELETES.tableName}", e)
            false
        }
    }

    @WorkerThread
    override fun removePendingDeletes(messageIds: List<String>, userId: String): Boolean =
        deleteBatch(Table.INBOX_PENDING_DELETES.tableName, messageIds, userId)

    @WorkerThread
    override fun markPendingDeletesAwaitingConfirm(
        messageIds: List<String>,
        userId: String
    ): Boolean {
        if (messageIds.isEmpty()) return true
        val placeholders = buildString {
            append("?")
            repeat(messageIds.size - 1) { append(", ?") }
        }
        val cv = ContentValues().apply { put(Column.STATE, PendingDeleteState.AWAITING_CONFIRM) }
        val args = (messageIds + userId).toTypedArray()
        return try {
            dbHelper.writableDatabase.update(
                Table.INBOX_PENDING_DELETES.tableName,
                cv,
                "${Column.ID} IN ($placeholders) AND ${Column.USER_ID} = ?",
                args
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error transitioning rows to AWAITING_CONFIRM", e)
            false
        }
    }

    @WorkerThread
    override fun removeExpiredAwaitingConfirm(userId: String, nowSeconds: Long): Int =
        try {
            dbHelper.writableDatabase.delete(
                Table.INBOX_PENDING_DELETES.tableName,
                "${Column.USER_ID} = ? AND ${Column.STATE} = ? AND ${Column.EXPIRES} <= ?",
                arrayOf(userId, PendingDeleteState.AWAITING_CONFIRM, nowSeconds.toString())
            )
        } catch (e: SQLiteException) {
            logger.verbose("Error sweeping expired pending-delete rows", e)
            0
        }

    @WorkerThread
    override fun addPendingReads(messageIds: List<String>, userId: String): Boolean =
        insertBatch(Table.INBOX_PENDING_READS.tableName, messageIds, userId)

    @WorkerThread
    override fun removePendingReads(messageIds: List<String>, userId: String): Boolean =
        deleteBatch(Table.INBOX_PENDING_READS.tableName, messageIds, userId)

    private fun readIds(table: String, userId: String): Set<String> {
        val result = LinkedHashSet<String>()
        try {
            dbHelper.readableDatabase.query(
                table,
                arrayOf(Column.ID),
                "${Column.USER_ID} = ?",
                arrayOf(userId),
                null, null, null
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Column.ID)
                while (cursor.moveToNext()) result.add(cursor.getString(idIdx))
            }
        } catch (e: Exception) {
            logger.verbose("Error reading from $table", e)
        }
        return result
    }

    private fun insertOne(table: String, messageId: String, userId: String): Boolean {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return false
        }
        val cv = ContentValues().apply {
            put(Column.USER_ID, userId)
            put(Column.ID, messageId)
            put(Column.CREATED_AT, clock.currentTimeSeconds())
        }
        return try {
            dbHelper.writableDatabase.insertWithOnConflict(
                table, null, cv, SQLiteDatabase.CONFLICT_IGNORE
            ) >= 0
        } catch (e: SQLiteException) {
            logger.verbose("Error inserting into $table", e)
            false
        }
    }

    private fun deleteOne(table: String, messageId: String, userId: String): Boolean =
        try {
            dbHelper.writableDatabase.delete(
                table,
                "${Column.ID} = ? AND ${Column.USER_ID} = ?",
                arrayOf(messageId, userId)
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error deleting from $table", e)
            false
        }

    private fun insertBatch(table: String, messageIds: List<String>, userId: String): Boolean {
        if (messageIds.isEmpty()) return true
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return false
        }
        val db = dbHelper.writableDatabase
        return try {
            db.transaction {
                val now = clock.currentTimeSeconds()
                messageIds.forEach { id ->
                    val cv = ContentValues().apply {
                        put(Column.USER_ID, userId)
                        put(Column.ID, id)
                        put(Column.CREATED_AT, now)
                    }
                    insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
                true
            }
        } catch (e: SQLiteException) {
            logger.verbose("Error batch-inserting into $table", e)
            false
        }
    }

    private fun deleteBatch(table: String, messageIds: List<String>, userId: String): Boolean {
        if (messageIds.isEmpty()) return true
        val placeholders = buildString {
            append("?")
            repeat(messageIds.size - 1) { append(", ?") }
        }
        val args = (messageIds + userId).toTypedArray()
        return try {
            dbHelper.writableDatabase.delete(
                table,
                "${Column.ID} IN ($placeholders) AND ${Column.USER_ID} = ?",
                args
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error batch-deleting from $table", e)
            false
        }
    }
}
