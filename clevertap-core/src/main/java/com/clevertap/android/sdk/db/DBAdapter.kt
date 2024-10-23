package com.clevertap.android.sdk.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.db.Table.INBOX_MESSAGES
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATIONS
import com.clevertap.android.sdk.db.Table.UNINSTALL_TS
import com.clevertap.android.sdk.db.Table.USER_EVENT_LOGS_TABLE
import com.clevertap.android.sdk.db.Table.USER_PROFILES
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.sdk.userEventLogs.UserEventLogDAO
import com.clevertap.android.sdk.userEventLogs.UserEventLogDAOImpl
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

//TODO: Introduce Clock or a time provider instead of depending on the static currentTimeMillis()
internal class DBAdapter(context: Context, config: CleverTapInstanceConfig) {

    companion object {

        private const val DATA_EXPIRATION = 1000L * 60 * 60 * 24 * 5

        //Notification Inbox Messages Table fields

        internal const val DB_UPDATE_ERROR = -1L

        internal const val DB_OUT_OF_MEMORY_ERROR = -2L

        @Suppress("unused")
        private const val DB_UNDEFINED_CODE = -3L

        private const val DATABASE_NAME = "clevertap"

        internal const val NOT_ENOUGH_SPACE_LOG =
            "There is not enough space left on the device to store data, data discarded"
    }

    @Volatile
    private var userEventLogDao: UserEventLogDAO? = null
    private val logger = config.logger

    private val dbHelper: DatabaseHelper = DatabaseHelper(context, config, getDatabaseName(config), logger)

    private var rtlDirtyFlag = true

    /**
     * Deletes the inbox message for given messageId
     *
     * @param messageId String messageId
     * @return boolean value based on success of operation
     */
    @WorkerThread
    @Synchronized
    fun deleteMessageForId(messageId: String?, userId: String?): Boolean {
        if (messageId == null || userId == null) {
            return false
        }
        val tName = INBOX_MESSAGES.tableName
        return try {
            dbHelper.writableDatabase.delete(
                tName, Column.ID + " = ? AND " + Column.USER_ID + " = ?", arrayOf(messageId, userId)
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale records from $tName", e)
            false
        }
    }

    /**
     * Deletes multiple inbox messages for given list of messageIDs
     *
     * @param messageIDs ArrayList of type String
     * @param userId     String userId
     * @return boolean value depending on success of operation
     */
    @WorkerThread
    @Synchronized
    fun deleteMessagesForIDs(messageIDs: List<String?>?, userId: String?): Boolean {
        if (messageIDs == null || userId == null) {
            return false
        }
        val tName = INBOX_MESSAGES.tableName
        val idsTemplateGroup = getTemplateMarkersList(messageIDs.size)
        val whereArgs = messageIDs.toMutableList()
        //Append userID as last element of arguments
        whereArgs.add(userId)

        return try {
            dbHelper.writableDatabase.delete(
                tName, "${Column.ID} IN ($idsTemplateGroup) AND ${Column.USER_ID} = ?", whereArgs.toTypedArray()
            )

            true
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale records from $tName", e)
            false
        }
    }

    @Synchronized
    fun doesPushNotificationIdExist(id: String): Boolean {
        return id == fetchPushNotificationId(id)
    }

    @Synchronized
    fun fetchPushNotificationIds(): Array<String?> {
        if (!rtlDirtyFlag) {
            return emptyArray()
        }
        val tName = PUSH_NOTIFICATIONS.tableName
        val pushIds: MutableList<String?> = ArrayList()

        try {
            dbHelper.readableDatabase.query(tName, null, "${Column.IS_READ} = 0", null, null, null, null)
                ?.use { cursor ->
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
        return pushIds.toTypedArray<String?>()
    }

    /**
     * Retrieves all user profiles based on the accountId.
     * Returns an emptyMap if no corresponding profile is found
     *
     * @param accountId String userId
     * @return Map representing the fetched profile, keys of this map will be the deviceIDs and the values will be the corresponding profiles
     */
    @Synchronized
    fun fetchUserProfilesByAccountId(accountId: String?): Map<String,JSONObject> {
        if (accountId == null) {
            return emptyMap()
        }

        val profiles = mutableMapOf<String, JSONObject>()
        val tName = USER_PROFILES.tableName

        try {
            dbHelper.readableDatabase.query(tName, null, "${Column.ID} = ?", arrayOf(accountId), null, null, null)
                ?.use { cursor ->
                    val dataIndex = cursor.getColumnIndex(Column.DATA)
                    val deviceId = cursor.getColumnIndex(Column.DEVICE_ID)
                    if (dataIndex >= 0) {
                        while (cursor.moveToNext()) {
                            val profileString = cursor.getString(dataIndex)
                            val deviceIdString = cursor.getString(deviceId)
                            profileString?.let {
                                try {
                                    val jsonObject = JSONObject(it)
                                    profiles.put(deviceIdString, jsonObject)
                                } catch (e: JSONException) {
                                    logger.verbose("Error parsing JSON for profile", e)
                                }
                            }
                        }
                    }
                }
        } catch (e: SQLiteException) {
            logger.verbose("Could not fetch records out of database $tName.", e)
        }

        return profiles
    }

    /**
     * Retrieves a user profile based on the accountId and deviceId
     * Returns null if no profile is found
     *
     * @param accountId String userId
     * @param deviceId String deviceId
     * @return JSONObject representing the fetched profile
     */
    @Synchronized
    fun fetchUserProfileByAccountIdAndDeviceID(accountId: String?, deviceId: String?): JSONObject? {
        if (accountId == null || deviceId == null) {
            return null
        }
        val tName = USER_PROFILES.tableName
        var profileString: String? = null
        try {
            dbHelper.readableDatabase.query(
                tName,
                null,
                "${Column.ID} = ? AND ${Column.DEVICE_ID} = ?",
                arrayOf(accountId, deviceId),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex(Column.DATA)
                    if (dataIndex >= 0) {
                        profileString = cursor.getString(dataIndex)
                    }
                }
            }
        } catch (e: SQLiteException) {
            logger.verbose("Could not fetch records out of database $tName.", e)
        }
        return profileString?.let {
            try {
                JSONObject(it)
            } catch (e: JSONException) {
                null
            }
        }
    }

    @Synchronized
    fun getLastUninstallTimestamp(): Long {
        val tName = UNINSTALL_TS.tableName
        var timestamp: Long = 0
        try {
            dbHelper.readableDatabase.query(tName, null, null, null, null, null, "${Column.CREATED_AT} DESC", "1")
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Column.CREATED_AT))
                    }
                }
        } catch (e: Exception) { // SQLiteException | IllegalArgumentException
            logger.verbose("Could not fetch records out of database $tName.", e)
        }
        return timestamp
    }

    /**
     * Retrieves list of inbox messages based on given userId
     *
     * @param userId String userid
     * @return ArrayList of [CTMessageDAO]
     */
    @WorkerThread
    @Synchronized
    fun getMessages(userId: String): ArrayList<CTMessageDAO> {
        val tName = INBOX_MESSAGES.tableName
        val messageDAOArrayList = ArrayList<CTMessageDAO>()
        try {
            dbHelper.readableDatabase.query(
                tName, null, "${Column.USER_ID} = ?", arrayOf(userId), null, null, "${Column.CREATED_AT} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val ctMessageDAO = CTMessageDAO()
                    ctMessageDAO.id = cursor.getString(cursor.getColumnIndexOrThrow(Column.ID))
                    ctMessageDAO.jsonData = JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA)))
                    ctMessageDAO.wzrkParams =
                        JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(Column.WZRKPARAMS)))
                    ctMessageDAO.date = cursor.getLong(cursor.getColumnIndexOrThrow(Column.CREATED_AT))
                    ctMessageDAO.expires = cursor.getLong(cursor.getColumnIndexOrThrow(Column.EXPIRES))
                    ctMessageDAO.isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Column.IS_READ))
                    ctMessageDAO.userId = cursor.getString(cursor.getColumnIndexOrThrow(Column.USER_ID))
                    ctMessageDAO.tags = cursor.getString(cursor.getColumnIndexOrThrow(Column.TAGS))
                    ctMessageDAO.campaignId = cursor.getString(cursor.getColumnIndexOrThrow(Column.CAMPAIGN))
                    messageDAOArrayList.add(ctMessageDAO)
                }
            }
        } catch (e: Exception) { //SQLiteException | IllegalArgumentException | JSONException
            logger.verbose("Error retrieving records from $tName", e)
        }
        return messageDAOArrayList
    }

    /**
     * Marks inbox message as read for given messageId
     *
     * @param messageId String messageId
     * @return boolean value depending on success of operation
     */
    @WorkerThread
    @Synchronized
    fun markReadMessageForId(messageId: String?, userId: String?): Boolean {
        if (messageId == null || userId == null) {
            return false
        }
        val tName = INBOX_MESSAGES.tableName
        val cv = ContentValues()
        cv.put(Column.IS_READ, 1)
        return try {
            dbHelper.writableDatabase.update(
                INBOX_MESSAGES.tableName, cv, "${Column.ID} = ? AND ${Column.USER_ID} = ?", arrayOf(messageId, userId)
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale records from $tName", e)
            false
        }
    }

    /**
     * Marks multiple inbox messages as read for given list of messageIDs
     *
     * @param messageIDs ArrayList of type String
     * @param userId     String userId
     * @return boolean value depending on success of operation
     */
    @WorkerThread
    @Synchronized
    fun markReadMessagesForIds(messageIDs: List<String?>?, userId: String?): Boolean {
        if (messageIDs == null || userId == null) {
            return false
        }
        val tName = INBOX_MESSAGES.tableName
        val idsTemplateGroup = getTemplateMarkersList(messageIDs.size)
        val whereArgs = messageIDs.toMutableList()

        //Append userID as last element of array to be used by query builder
        whereArgs.add(userId)
        val cv = ContentValues()
        cv.put(Column.IS_READ, 1)
        return try {
            dbHelper.writableDatabase.update(
                INBOX_MESSAGES.tableName,
                cv,
                "${Column.ID} IN ($idsTemplateGroup) AND ${Column.USER_ID} = ?",
                whereArgs.toTypedArray()
            )
            true
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale records from $tName", e)
            false
        }
    }

    /**
     * removes all the user profiles with given account id from the db.
     *
     * @param id the accountId for which the profiles are to be removed
     */
    @Synchronized
    fun removeUserProfilesForAccountId(id: String?) {
        if (id == null) {
            return
        }
        val tableName = USER_PROFILES.tableName
        try {
            dbHelper.writableDatabase.delete(tableName, "${Column.ID} = ?", arrayOf(id))
        } catch (e: SQLiteException) {
            logger.verbose("Error removing user profile from $tableName Recreating DB")
            deleteDB()
        }
    }

    /**
     * Adds a String timestamp representing uninstall flag to the DB.
     */
    @Synchronized
    fun storeUninstallTimestamp() {
        if (!belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }
        val tableName = UNINSTALL_TS.tableName
        val cv = ContentValues()
        cv.put(Column.CREATED_AT, System.currentTimeMillis())
        try {
            dbHelper.writableDatabase.insert(tableName, null, cv)
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName Recreating DB")
            deleteDB()
        }
    }

    /**
     * Adds a JSON string representing the profile to the DB.
     *
     * @param id the accountId for this profile
     * @param deviceId the deviceId for this profile
     * @param obj the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     */
    @WorkerThread
    @Synchronized
    fun storeUserProfile(id: String?, deviceId: String?, obj: JSONObject): Long {
        if (id == null || deviceId == null) {
            return DB_UPDATE_ERROR
        }
        if (!belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return DB_OUT_OF_MEMORY_ERROR
        }
        val tableName = USER_PROFILES.tableName

        logger.verbose("Inserting or updating userProfile for accountID = $id + deviceID = $deviceId")
        val cv = ContentValues()
        cv.put(Column.DATA, obj.toString())
        cv.put(Column.ID, id)
        cv.put(Column.DEVICE_ID, deviceId)

        return try {
            dbHelper.writableDatabase.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName Recreating DB")
            deleteDB()
            DB_UPDATE_ERROR
        }
    }

    /**
     * Stores a list of inbox messages
     *
     * @param inboxMessages ArrayList of type [CTMessageDAO]
     */
    @WorkerThread
    @Synchronized
    fun upsertMessages(inboxMessages: List<CTMessageDAO>) {
        if (!belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }

        for (messageDAO in inboxMessages) {
            val cv = ContentValues()
            cv.put(Column.ID, messageDAO.id)
            cv.put(Column.DATA, messageDAO.jsonData.toString())
            cv.put(Column.WZRKPARAMS, messageDAO.wzrkParams.toString())
            cv.put(Column.CAMPAIGN, messageDAO.campaignId)
            cv.put(Column.TAGS, messageDAO.tags)
            cv.put(Column.IS_READ, messageDAO.isRead())
            cv.put(Column.EXPIRES, messageDAO.expires)
            cv.put(Column.CREATED_AT, messageDAO.date)
            cv.put(Column.USER_ID, messageDAO.userId)
            try {
                dbHelper.writableDatabase.insertWithOnConflict(
                    INBOX_MESSAGES.tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE
                )
            } catch (e: SQLiteException) {
                logger.verbose("Error adding data to table " + INBOX_MESSAGES.tableName)
            }
        }
    }

    @Synchronized
    fun cleanUpPushNotifications() {
        //In Push_Notifications, KEY_CREATED_AT is stored as a future epoch, i.e. currentTimeMillis() + ttl,
        //so comparing to the current time for removal is correct
        cleanInternal(PUSH_NOTIFICATIONS, 0)
    }

    /**
     * Removes sent events with an _id <= last_id from table
     *
     * @param lastId the last id to delete
     * @param table  the table to remove events
     */
    @WorkerThread
    @Synchronized
    fun cleanupEventsFromLastId(lastId: String, table: Table) {
        val tName = table.tableName
        try {
            dbHelper.writableDatabase.delete(tName, "${Column.ID} <= ?", arrayOf(lastId))
        } catch (e: SQLiteException) {
            logger.verbose("Error removing sent data from table $tName Recreating DB")
            deleteDB()
        }
    }

    @Synchronized
    fun storePushNotificationId(id: String?, ttl: Long) {
        if (id == null) {
            return
        }
        if (!belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }
        val tableName = PUSH_NOTIFICATIONS.tableName
        val createdAtTime = if (ttl > 0) {
            ttl
        } else {
            System.currentTimeMillis() + Constants.DEFAULT_PUSH_TTL
        }
        val cv = ContentValues()
        cv.put(Column.DATA, id)
        cv.put(Column.CREATED_AT, createdAtTime)
        cv.put(Column.IS_READ, 0)
        try {
            dbHelper.writableDatabase.insert(tableName, null, cv)
            rtlDirtyFlag = true
            logger.verbose("Stored PN - $id with TTL - $createdAtTime")
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName Recreating DB")
            deleteDB()
        }
    }

    /**
     * Removes stale events.
     *
     * @param table the table to remove events
     */
    @Synchronized
    fun cleanupStaleEvents(table: Table) {
        cleanInternal(table, DATA_EXPIRATION)
    }

    /**
     * Returns a JSONObject keyed with the lastId retrieved and a value of a JSONArray of the retrieved JSONObject
     * events
     *
     * @param table the table to read from
     * @return JSONObject containing the max row ID and a JSONArray of the JSONObject events or null
     */
    @Synchronized
    fun fetchEvents(table: Table, limit: Int): JSONObject? {
        val tName = table.tableName
        var lastId: String? = null
        val events = JSONArray()
        try {
            dbHelper.readableDatabase.query(
                tName, null, null, null, null, null, "${Column.CREATED_AT} ASC", limit.toString()
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.isLast) {
                        lastId = cursor.getString(cursor.getColumnIndexOrThrow(Column.ID))
                    }
                    try {
                        val j = JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA)))
                        events.put(j)
                    } catch (e: JSONException) {
                        // Ignore
                    }
                }
            }
        } catch (e: Exception) { // SQLiteException | IllegalArgumentException
            logger.verbose("Could not fetch records out of database $tName.", e)
            lastId = null
        }

        return lastId?.let {
            try {
                val ret = JSONObject()
                ret.put(it, events)
                ret
            } catch (e: JSONException) {
                null
            }
        }
    }

    @WorkerThread
    @Synchronized
    fun updatePushNotificationIds(ids: Array<String?>) {
        if (ids.isEmpty()) {
            return
        }
        if (!belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return
        }
        val tableName = PUSH_NOTIFICATIONS.tableName
        val cv = ContentValues()
        cv.put(Column.IS_READ, 1)
        val idsTemplateGroup = getTemplateMarkersList(ids.size)
        try {
            dbHelper.writableDatabase.update(tableName, cv, "${Column.DATA} IN ($idsTemplateGroup)", ids)
            rtlDirtyFlag = false
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName Recreating DB")
            deleteDB()
        }
    }

    /**
     * Adds a JSON string to the DB.
     *
     * @param obj   the JSON to record
     * @param table the table to insert into
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     */
    @WorkerThread
    @Synchronized
    fun storeObject(obj: JSONObject, table: Table): Long {
        if (!belowMemThreshold()) {
            logger.verbose(NOT_ENOUGH_SPACE_LOG)
            return DB_OUT_OF_MEMORY_ERROR
        }
        val tableName = table.tableName
        val cv = ContentValues()
        cv.put(Column.DATA, obj.toString())
        cv.put(Column.CREATED_AT, System.currentTimeMillis())

        return try {
            dbHelper.writableDatabase.insert(tableName, null, cv)
            val sql = "SELECT COUNT(*) FROM $tableName"
            val statement = dbHelper.writableDatabase.compileStatement(sql)
            statement.simpleQueryForLong()
        } catch (e: SQLiteException) {
            logger.verbose("Error adding data to table $tableName Recreating DB")
            deleteDB()
            DB_UPDATE_ERROR
        }
    }

    /**
     * Removes all events from table
     *
     * @param table the table to remove events
     */
    @Synchronized
    fun removeEvents(table: Table) {
        val tName = table.tableName
        try {
            dbHelper.writableDatabase.delete(tName, null, null)
        } catch (e: SQLiteException) {
            logger.verbose("Error removing all events from table $tName Recreating DB")
            deleteDB()
        }
    }

    /**
     * -----------------------------
     * -----------DAO---------------
     * -----------------------------
     */
    @WorkerThread
    fun userEventLogDAO(): UserEventLogDAO {
        return userEventLogDao ?: synchronized(this) {
            userEventLogDao ?: UserEventLogDAOImpl(
                dbHelper,
                logger,
                USER_EVENT_LOGS_TABLE
            ).also { userEventLogDao = it }

        }
    }

    @WorkerThread
    private fun belowMemThreshold(): Boolean {
        return dbHelper.belowMemThreshold()
    }

    private fun cleanInternal(table: Table, expiration: Long) {
        val time = (System.currentTimeMillis() - expiration) / 1000
        val tName = table.tableName
        try {
            dbHelper.writableDatabase.delete(tName, "${Column.CREATED_AT} <= $time", null)
        } catch (e: SQLiteException) {
            logger.verbose("Error removing stale event records from $tName. Recreating DB.", e)
            deleteDB()
        }
    }

    @VisibleForTesting
    internal fun deleteDB() {
        dbHelper.deleteDatabase()
    }

    private fun fetchPushNotificationId(id: String): String {
        val tName = PUSH_NOTIFICATIONS.tableName
        var pushId = "" // TODO: fix dupe failing
        try {
            dbHelper.readableDatabase.query(tName, null, "${Column.DATA} =?", arrayOf(id), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        pushId = cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA))
                    }
                    logger.verbose("Fetching PID for check - $pushId")
                }
        } catch (e: Exception) { // SQLiteException | IllegalArgumentException
            logger.verbose("Could not fetch records out of database $tName.", e)
        }
        return pushId
    }

    private fun getDatabaseName(config: CleverTapInstanceConfig): String {
        return if (config.isDefaultInstance) DATABASE_NAME else DATABASE_NAME + "_" + config.accountId
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
