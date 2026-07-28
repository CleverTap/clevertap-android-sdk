package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table.INBOX_MESSAGES
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.sdk.inbox.InboxIndexState
import com.clevertap.android.sdk.inbox.InboxMessageSource
import org.json.JSONObject

internal class InboxMessageDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: ILogger,
    private val dbEncryptionHandler: DBEncryptionHandler
) : InboxMessageDAO {

    @WorkerThread
    override fun getMessages(userId: String): ArrayList<CTMessageDAO> {
        val tName = INBOX_MESSAGES.tableName
        val messageDAOArrayList = ArrayList<CTMessageDAO>()
        
        try {
            dbHelper.readableDatabase.query(
                tName, null, "${Column.USER_ID} = ?", arrayOf(userId), 
                null, null, "${Column.CREATED_AT} DESC"
            )?.use { cursor ->
                // find indices
                val idColumnIndex = cursor.getColumnIndexOrThrow(Column.ID)
                val dataColumnIndex = cursor.getColumnIndexOrThrow(Column.DATA)
                val wzrkParamsColumnIndex = cursor.getColumnIndexOrThrow(Column.WZRKPARAMS)
                val createdAtColumnIndex = cursor.getColumnIndexOrThrow(Column.CREATED_AT)
                val expiresColumnIndex = cursor.getColumnIndexOrThrow(Column.EXPIRES)
                val isReadColumnIndex = cursor.getColumnIndexOrThrow(Column.IS_READ)
                val userIdColumnIndex = cursor.getColumnIndexOrThrow(Column.USER_ID)
                val tagsColumnIndex = cursor.getColumnIndexOrThrow(Column.TAGS)
                val campaignColumnIndex = cursor.getColumnIndexOrThrow(Column.CAMPAIGN)
                val sourceColumnIndex = cursor.getColumnIndex(Column.SOURCE)
                val indexStateColumnIndex = cursor.getColumnIndex(Column.INDEX_STATE)

                while (cursor.moveToNext()) {
                    val decryptedData = dbEncryptionHandler.unwrapDbData(cursor.getString(dataColumnIndex))
                    if (decryptedData == null) {
                        logger.debug("There was some problem in loading inbox message from DB")
                        continue
                    }

                    val ctMessageDAO = CTMessageDAO().apply {
                        this.id = cursor.getString(idColumnIndex)
                        this.jsonData = JSONObject(decryptedData)
                        this.wzrkParams = JSONObject(cursor.getString(wzrkParamsColumnIndex))
                        this.date = cursor.getLong(createdAtColumnIndex)
                        this.expires = cursor.getLong(expiresColumnIndex)
                        this.isRead = cursor.getInt(isReadColumnIndex)
                        this.userId =
                            cursor.getString(userIdColumnIndex) // This seems redundant if you are already filtering by userId
                        this.tags = cursor.getString(tagsColumnIndex)
                        this.campaignId = cursor.getString(campaignColumnIndex)
                        this.source = readSource(cursor, sourceColumnIndex)
                        this.indexState = readIndexState(cursor, indexStateColumnIndex)
                    }
                    messageDAOArrayList.add(ctMessageDAO)
                }
            }
        } catch (e: Exception) {
            logger.verbose("Error retrieving records from $tName", e)
        }
        return messageDAOArrayList
    }

    /**
     * Saves a list of inbox messages. A message already in the table is updated; a new one
     * is inserted.
     *
     * ### Why UPDATE-then-INSERT instead of SQLite UPSERT
     *
     * The obvious way to write this is SQLite UPSERT:
     * `INSERT ... ON CONFLICT(messageUser, _id) DO UPDATE SET ...`. We cannot use it.
     * UPSERT was added in SQLite 3.24.0, and Android does not ship its own SQLite — it uses
     * the one built into the phone's operating system:
     *
     * | API level | Android | SQLite   | UPSERT works? |
     * |-----------|---------|----------|---------------|
     * | 23        | 6.0     | 3.8.10.2 | no            |
     * | 26        | 8.0     | 3.18.2   | no            |
     * | 28        | 9       | 3.22.0   | no            |
     * | 29        | 10      | 3.22.0   | no            |
     * | 30        | 11      | 3.28.0   | yes           |
     *
     * Our minSdk is 23, so on Android 6.0 through 10 the phone's SQLite is too old and the
     * statement fails to compile at all — `near "ON": syntax error`. Watch out for Android
     * 10: it kept the same SQLite as Android 9, so the cut-off is Android 11, not 10.
     *
     * So instead we UPDATE the row for this `(messageUser, _id)` pair, and INSERT only when
     * the UPDATE found nothing to change. Both are plain SQLite that works everywhere.
     *
     * ### Why the read flag and index_state survive correctly
     *
     * `index_state` marks whether the server has confirmed a message. A message can be
     * delivered again later, and when that happens its `index_state` must not be reset —
     * the cross-device delete sweep would otherwise mistake a valid message for a deleted
     * one. See [findSweepableV2Ids] and [markIndexed].
     *
     * This is why we cannot simply use `insertWithOnConflict(CONFLICT_REPLACE)` either:
     * REPLACE deletes the old row and inserts a fresh one, wiping `index_state`.
     *
     * Our version handles it by construction: `index_state` is not in the UPDATE's column
     * list at all, so an existing row's value cannot be touched. Only the INSERT sets it.
     *
     * Example — the table holds `(_id = m1, messageUser = userA, index_state = INDEXED)`
     * and message `m1` arrives again for `userA`:
     * - the UPDATE refreshes the text, read flag and expiry, and leaves `index_state` alone
     * - so it stays `INDEXED`, which is correct
     *
     * ### Why one transaction
     *
     * The whole list is written inside a single transaction. That gives two things: the
     * UPDATE and INSERT for one message cannot be split apart by another process writing
     * at the same time, and the database commits once for the batch instead of once per
     * message.
     */
    @WorkerThread
    override fun upsertMessages(inboxMessages: List<CTMessageDAO>) {
        if (inboxMessages.isEmpty()) {
            return
        }

        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }

        val db = dbHelper.writableDatabase

        // The outer catch keeps the long-standing contract that this method never throws.
        // Before this class used a transaction, every write went through execSQL inside a
        // try/catch, so callers were never given an exception. beginTransaction() and
        // endTransaction() can both fail (for example when the disk is full or the database
        // is locked), and CryptMigrator.migrateInboxData calls us during SDK start-up
        // without any try/catch of its own — so a throw here would break initialisation.
        try {
            db.beginTransaction()
            try {
                inboxMessages.forEach { messageDAO -> writeMessage(db, messageDAO) }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: SQLiteException) {
            logger.verbose("Error writing inbox messages to ${INBOX_MESSAGES.tableName}", e)
        }
    }

    /**
     * Saves one message: update the row for this `(messageUser, _id)` pair, or insert it if
     * there is no such row yet.
     *
     * If saving this one message fails we log it and carry on to the next message, rather
     * than giving up on the whole list. In SQLite a single failed statement does not cancel
     * the surrounding transaction, so the other messages still get saved. That matches how
     * this worked before: one bad message costs you that message, not the whole batch.
     */
    @WorkerThread
    private fun writeMessage(db: SQLiteDatabase, messageDAO: CTMessageDAO) {
        val tName = INBOX_MESSAGES.tableName

        // The columns a repeat delivery is allowed to overwrite.
        //
        // index_state is deliberately missing: leaving it out of the UPDATE is what stops an
        // already-confirmed message being reset back to unconfirmed. _id and messageUser are
        // missing because they identify the row — they go in the WHERE clause below, and are
        // only written when we insert a brand-new row.
        val mutableColumns = ContentValues().apply {
            put(Column.DATA, dbEncryptionHandler.wrapDbData(messageDAO.jsonData.toString()))
            put(Column.WZRKPARAMS, messageDAO.wzrkParams.toString())
            put(Column.CAMPAIGN, messageDAO.campaignId)
            put(Column.TAGS, messageDAO.tags)
            put(Column.IS_READ, messageDAO.isRead())
            put(Column.EXPIRES, messageDAO.expires)
            put(Column.CREATED_AT, messageDAO.date)
            put(Column.SOURCE, (messageDAO.source ?: InboxMessageSource.V1).name)
        }

        try {
            val updated = db.update(
                tName,
                mutableColumns,
                "${Column.USER_ID} = ? AND ${Column.ID} = ?",
                arrayOf(messageDAO.userId, messageDAO.id)
            )
            // updated > 0 means a row already existed and we just refreshed it, so there is
            // nothing left to do.
            if (updated > 0) {
                return
            }

            // We get here only when the UPDATE matched no rows, i.e. this message is new to
            // this user. Add the two identifying columns, plus index_state — the insert is
            // the only place index_state is ever written.
            val insertColumns = ContentValues(mutableColumns).apply {
                put(Column.ID, messageDAO.id)
                put(Column.USER_ID, messageDAO.userId)
                put(Column.INDEX_STATE, messageDAO.indexState ?: InboxIndexState.PENDING_INDEXING)
            }
            db.insert(tName, null, insertColumns)
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tName", e)
        }
    }

    @WorkerThread
    override fun deleteMessage(messageId: String, userId: String): Boolean {
        val tName = INBOX_MESSAGES.tableName
        return try {
            dbHelper.writableDatabase.delete(
                tName, 
                "${Column.ID} = ? AND ${Column.USER_ID} = ?", 
                arrayOf(messageId, userId)
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale records from $tName", e)
            false
        }
    }

    @WorkerThread
    override fun deleteMessages(messageIds: List<String>, userId: String): Boolean {
        if (messageIds.isEmpty()) {
            // Or just return true if there's nothing to delete
            logger.verbose("messageIds list is empty, nothing to delete.")
            return true
        }
        val tName = INBOX_MESSAGES.tableName
        val idsTemplateGroup = getTemplateMarkersList(messageIds.size)
        val whereArgs = messageIds.toMutableList().apply { add(userId) }

        return try {
            dbHelper.writableDatabase.delete(
                tName, 
                "${Column.ID} IN ($idsTemplateGroup) AND ${Column.USER_ID} = ?", 
                whereArgs.toTypedArray()
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale records from $tName", e)
            false
        }
    }

    @WorkerThread
    override fun markMessageAsRead(messageId: String, userId: String): Boolean {
        val tName = INBOX_MESSAGES.tableName
        val cv = ContentValues().apply {
            put(Column.IS_READ, 1)
        }
        
        return try {
            dbHelper.writableDatabase.update(
                tName, cv, 
                "${Column.ID} = ? AND ${Column.USER_ID} = ?", 
                arrayOf(messageId, userId)
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error updating record in $tName", e)
            false
        }
    }

    @WorkerThread
    override fun markMessagesAsRead(messageIds: List<String>, userId: String): Boolean {
        val tName = INBOX_MESSAGES.tableName
        val idsTemplateGroup = getTemplateMarkersList(messageIds.size)
        val whereArgs = messageIds.toMutableList().apply { add(userId) }
        val cv = ContentValues().apply {
            put(Column.IS_READ, 1)
        }

        return try {
            dbHelper.writableDatabase.update(
                tName, cv,
                "${Column.ID} IN ($idsTemplateGroup) AND ${Column.USER_ID} = ?",
                whereArgs.toTypedArray()
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error updating records in $tName", e)
            false
        }
    }

    @WorkerThread
    override fun markIndexed(messageIds: List<String>, userId: String): Boolean {
        if (messageIds.isEmpty()) return true
        val tName = INBOX_MESSAGES.tableName
        val idsTemplateGroup = getTemplateMarkersList(messageIds.size)
        val whereArgs = messageIds.toMutableList().apply { add(userId) }
        val cv = ContentValues().apply {
            put(Column.INDEX_STATE, InboxIndexState.INDEXED)
        }

        return try {
            dbHelper.writableDatabase.update(
                tName, cv,
                "${Column.ID} IN ($idsTemplateGroup) AND ${Column.USER_ID} = ?",
                whereArgs.toTypedArray()
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error marking inbox rows indexed in $tName", e)
            false
        }
    }

    @WorkerThread
    override fun findSweepableV2Ids(userId: String, staleCutoffSeconds: Long): Set<String> {
        val tName = INBOX_MESSAGES.tableName
        val result = mutableSetOf<String>()
        try {
            // Select V2 rows that are either:
            //  (a) INDEXED — fetch backend has confirmed them; absence is a delete signal.
            //  (b) PENDING_INDEXING but older than the grace cutoff — indexing window
            //      has demonstrably elapsed; absence is treated as a delete signal too.
            val selection = "${Column.USER_ID} = ?" +
                    " AND ${Column.SOURCE} = ?" +
                    " AND ${Column.EXPIRES} != 0" +
                    " AND (${Column.INDEX_STATE} = ?" +
                    " OR (${Column.INDEX_STATE} = ? AND ${Column.CREATED_AT} < ?))"
            val selectionArgs = arrayOf(
                userId,
                InboxMessageSource.V2.name,
                InboxIndexState.INDEXED,
                InboxIndexState.PENDING_INDEXING,
                staleCutoffSeconds.toString()
            )
            dbHelper.readableDatabase.query(
                tName,
                arrayOf(Column.ID),
                selection,
                selectionArgs,
                null, null, null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Column.ID)
                while (cursor.moveToNext()) {
                    cursor.getString(idIndex)?.let { result.add(it) }
                }
            }
        } catch (e: Exception) {
            logger.verbose("Error querying sweepable V2 ids from $tName", e)
        }
        return result
    }

    private fun readSource(cursor: android.database.Cursor, columnIndex: Int): InboxMessageSource {
        if (columnIndex < 0) return InboxMessageSource.V1
        val raw = cursor.getString(columnIndex) ?: return InboxMessageSource.V1
        return try {
            InboxMessageSource.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            InboxMessageSource.V1
        }
    }

    private fun readIndexState(cursor: android.database.Cursor, columnIndex: Int): String {
        if (columnIndex < 0) return InboxIndexState.PENDING_INDEXING
        val raw = cursor.getString(columnIndex) ?: return InboxIndexState.PENDING_INDEXING
        return when (raw) {
            InboxIndexState.PENDING_INDEXING, InboxIndexState.INDEXED -> raw
            else -> InboxIndexState.PENDING_INDEXING
        }
    }

    private fun getTemplateMarkersList(count: Int): String {
        return buildString {
            if (count > 0) {
                append("?")
                repeat(count - 1) {
                    append(", ?")
                }
            }
        }
    }
}
