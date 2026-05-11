package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
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

    @WorkerThread
    override fun upsertMessages(inboxMessages: List<CTMessageDAO>) {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }

        // SQLite UPSERT — INSERT writes index_state for fresh rows; the
        // ON CONFLICT clause intentionally omits index_state so an existing
        // row's state survives upsert. The cross-device delete sweep relies
        // on this: an /a1 redelivery (or any subsequent upsert) must never
        // downgrade an INDEXED row back to PENDING_INDEXING. The FETCH path
        // promotes survivors via a separate markIndexed() call.
        val sql = """
            INSERT INTO ${INBOX_MESSAGES.tableName} (
                ${Column.ID},
                ${Column.DATA},
                ${Column.WZRKPARAMS},
                ${Column.CAMPAIGN},
                ${Column.TAGS},
                ${Column.IS_READ},
                ${Column.EXPIRES},
                ${Column.CREATED_AT},
                ${Column.USER_ID},
                ${Column.SOURCE},
                ${Column.INDEX_STATE}
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(${Column.USER_ID}, ${Column.ID}) DO UPDATE SET
                ${Column.DATA} = excluded.${Column.DATA},
                ${Column.WZRKPARAMS} = excluded.${Column.WZRKPARAMS},
                ${Column.CAMPAIGN} = excluded.${Column.CAMPAIGN},
                ${Column.TAGS} = excluded.${Column.TAGS},
                ${Column.IS_READ} = excluded.${Column.IS_READ},
                ${Column.EXPIRES} = excluded.${Column.EXPIRES},
                ${Column.CREATED_AT} = excluded.${Column.CREATED_AT},
                ${Column.SOURCE} = excluded.${Column.SOURCE}
        """.trimIndent()

        val db = dbHelper.writableDatabase
        for (messageDAO in inboxMessages) {
            try {
                val encryptedData = dbEncryptionHandler.wrapDbData(messageDAO.jsonData.toString())
                val args = arrayOf<Any?>(
                    messageDAO.id,
                    encryptedData,
                    messageDAO.wzrkParams.toString(),
                    messageDAO.campaignId,
                    messageDAO.tags,
                    messageDAO.isRead(),
                    messageDAO.expires,
                    messageDAO.date,
                    messageDAO.userId,
                    (messageDAO.source ?: InboxMessageSource.V1).name,
                    messageDAO.indexState ?: InboxIndexState.PENDING_INDEXING
                )
                db.execSQL(sql, args)
            } catch (e: SQLiteException) {
                logger.verbose("Error adding data to table ${INBOX_MESSAGES.tableName}", e)
            }
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
