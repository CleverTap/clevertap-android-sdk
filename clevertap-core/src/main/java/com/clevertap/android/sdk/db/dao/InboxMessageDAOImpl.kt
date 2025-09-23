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

                while (cursor.moveToNext()) {
                    val ctMessageDAO = CTMessageDAO().apply {
                        this.id = cursor.getString(idColumnIndex)
                        val decryptedData = dbEncryptionHandler.unwrapDbData(cursor.getString(dataColumnIndex))
                        this.jsonData = JSONObject(decryptedData)
                        this.wzrkParams = JSONObject(cursor.getString(wzrkParamsColumnIndex))
                        this.date = cursor.getLong(createdAtColumnIndex)
                        this.expires = cursor.getLong(expiresColumnIndex)
                        this.isRead = cursor.getInt(isReadColumnIndex)
                        this.userId = cursor.getString(userIdColumnIndex) // This seems redundant if you are already filtering by userId
                        this.tags = cursor.getString(tagsColumnIndex)
                        this.campaignId = cursor.getString(campaignColumnIndex)
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

        for (messageDAO in inboxMessages) {
            val cv = ContentValues().apply {
                put(Column.ID, messageDAO.id)
                val encryptedData = dbEncryptionHandler.wrapDbData(messageDAO.jsonData.toString())
                put(Column.DATA, encryptedData)
                put(Column.WZRKPARAMS, messageDAO.wzrkParams.toString())
                put(Column.CAMPAIGN, messageDAO.campaignId)
                put(Column.TAGS, messageDAO.tags)
                put(Column.IS_READ, messageDAO.isRead())
                put(Column.EXPIRES, messageDAO.expires)
                put(Column.CREATED_AT, messageDAO.date)
                put(Column.USER_ID, messageDAO.userId)
            }
            
            try {
                dbHelper.writableDatabase.insertWithOnConflict(
                    INBOX_MESSAGES.tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE
                )
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
