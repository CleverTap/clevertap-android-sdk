package com.clevertap.android.sdk.db.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBAdapter.Companion.NOT_ENOUGH_SPACE_LOG
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATIONS
import com.clevertap.android.sdk.utils.Clock

internal class PushNotificationDAOImpl(
    private val dbHelper: DatabaseHelper,
    private val logger: ILogger,
    private val clock: Clock = Clock.SYSTEM
) : PushNotificationDAO {

    @Volatile
    private var rtlDirtyFlag = true

    @WorkerThread
    override fun storePushNotificationId(id: String, ttl: Long) {
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }
        
        val tableName = PUSH_NOTIFICATIONS.tableName
        val createdAtTime = if (ttl > 0) ttl else clock.currentTimeMillis() + Constants.DEFAULT_PUSH_TTL
        
        val cv = ContentValues().apply {
            put(Column.DATA, id)
            put(Column.CREATED_AT, createdAtTime)
            put(Column.IS_READ, 0)
        }
        
        try {
            dbHelper.writableDatabase.insert(tableName, null, cv)
            rtlDirtyFlag = true
            logger.verbose("Stored PN - $id with TTL - $createdAtTime")
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName. Recreating DB", e)
            dbHelper.deleteDatabase()
        }
    }

    @WorkerThread
    override fun fetchPushNotificationIds(): Array<String?> {
        if (!rtlDirtyFlag) {
            return emptyArray()
        }
        
        val tName = PUSH_NOTIFICATIONS.tableName
        val pushIds: MutableList<String?> = ArrayList()

        try {
            dbHelper.readableDatabase.query(
                tName, null, "${Column.IS_READ} = 0", 
                null, null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val dataIndex = cursor.getColumnIndex(Column.DATA)
                    if (dataIndex >= 0) {
                        val data = cursor.getString(dataIndex)
                        logger.verbose("Fetching PID - $data")
                        pushIds.add(data)
                    }
                }
            }
        } catch (e: SQLiteException) {
            logger.verbose("Could not fetch records out of database $tName.", e)
        }
        return pushIds.toTypedArray()
    }

    @WorkerThread
    override fun doesPushNotificationIdExist(id: String): Boolean {
        return id == fetchPushNotificationId(id)
    }

    @WorkerThread
    override fun updatePushNotificationIds(ids: Array<String?>) {
        if (ids.isEmpty()) return
        
        if (!dbHelper.belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }
        
        val tableName = PUSH_NOTIFICATIONS.tableName
        val cv = ContentValues().apply {
            put(Column.IS_READ, 1)
        }
        val idsTemplateGroup = getTemplateMarkersList(ids.size)
        
        try {
            dbHelper.writableDatabase.update(
                tableName, cv, 
                "${Column.DATA} IN ($idsTemplateGroup)", ids
            )
            rtlDirtyFlag = false
        } catch (e: SQLiteException) {
            logger.verbose("Error updating data in table $tableName. Recreating DB", e)
            dbHelper.deleteDatabase()
        }
    }

    @WorkerThread
    override fun cleanUpPushNotifications() {
        // Push notifications store future epoch (currentTime + TTL)
        val time = clock.currentTimeMillis()
        val tName = PUSH_NOTIFICATIONS.tableName
        
        try {
            dbHelper.writableDatabase.delete(tName, "${Column.CREATED_AT} <= $time", null)
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale push notification records from $tName. Recreating DB.", e)
            dbHelper.deleteDatabase()
        }
    }

    private fun fetchPushNotificationId(id: String): String {
        val tName = PUSH_NOTIFICATIONS.tableName
        var pushId = ""
        
        try {
            dbHelper.readableDatabase.query(
                tName, null, "${Column.DATA} = ?", arrayOf(id), 
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    pushId = cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA))
                }
                logger.verbose("Fetching PID for check - $pushId")
            }
        } catch (e: Exception) {
            logger.verbose("Could not fetch records out of database $tName.", e)
        }
        return pushId
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
