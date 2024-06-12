package com.clevertap.android.sdk.db

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.COMMAND_ADD
import com.clevertap.android.sdk.Constants.COMMAND_SET
import com.clevertap.android.sdk.Constants.DATE_PREFIX
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.Table.EVENTS
import com.clevertap.android.sdk.db.Table.INBOX_MESSAGES
import com.clevertap.android.sdk.db.Table.PROFILE_EVENTS
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATIONS
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.db.Table.UNINSTALL_TS
import com.clevertap.android.sdk.db.Table.USER_PROFILES
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.math.max

class DatabaseHelper internal constructor(val context: Context, val config: CleverTapInstanceConfig, dbName: String?, private val logger: Logger) :
    SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {

    companion object {

        private const val DATABASE_VERSION = 4
        private const val DB_LIMIT = 20 * 1024 * 1024 //20mb
    }

    private val databaseFile: File

    init {
        databaseFile = context.getDatabasePath(dbName)
    }

    override fun onCreate(db: SQLiteDatabase) {
        logger.verbose("Creating CleverTap DB")
        executeStatement(db, CREATE_EVENTS_TABLE)
        executeStatement(db, CREATE_PROFILE_EVENTS_TABLE)
        executeStatement(db, CREATE_USER_PROFILES_TABLE)
        executeStatement(db, CREATE_INBOX_MESSAGES_TABLE)
        executeStatement(db, CREATE_PUSH_NOTIFICATIONS_TABLE)
        executeStatement(db, CREATE_UNINSTALL_TS_TABLE)
        executeStatement(db, CREATE_NOTIFICATION_VIEWED_TABLE)
        executeStatement(db, EVENTS_TIME_INDEX)
        executeStatement(db, PROFILE_EVENTS_TIME_INDEX)
        executeStatement(db, UNINSTALL_TS_INDEX)
        executeStatement(db, PUSH_NOTIFICATIONS_TIME_INDEX)
        executeStatement(db, INBOX_MESSAGES_COMP_ID_USERID_INDEX)
        executeStatement(db, NOTIFICATION_VIEWED_INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        logger.verbose("Upgrading CleverTap DB to version $newVersion")
        when (oldVersion) {
            1 -> {
                // For DB Version 2, just adding Push Notifications, Uninstall TS and Inbox Messages tables and related indices and migrating userProfiles table
                executeStatement(db, DROP_TABLE_UNINSTALL_TS)
                executeStatement(db, DROP_TABLE_INBOX_MESSAGES)
                executeStatement(db, DROP_TABLE_PUSH_NOTIFICATION_VIEWED)
                executeStatement(db, CREATE_INBOX_MESSAGES_TABLE)
                executeStatement(db, CREATE_PUSH_NOTIFICATIONS_TABLE)
                executeStatement(db, CREATE_UNINSTALL_TS_TABLE)
                executeStatement(db, CREATE_NOTIFICATION_VIEWED_TABLE)
                executeStatement(db, UNINSTALL_TS_INDEX)
                executeStatement(db, PUSH_NOTIFICATIONS_TIME_INDEX)
                executeStatement(db, INBOX_MESSAGES_COMP_ID_USERID_INDEX)
                executeStatement(db, NOTIFICATION_VIEWED_INDEX)
                migrateUserProfilesTable(db)
            }

            2 -> {
                // For DB Version 3, just adding Push Notification Viewed table and index and migrating userProfiles table
                executeStatement(db, DROP_TABLE_PUSH_NOTIFICATION_VIEWED)
                executeStatement(db, CREATE_NOTIFICATION_VIEWED_TABLE)
                executeStatement(db, NOTIFICATION_VIEWED_INDEX)
                migrateUserProfilesTable(db)
            }

            3 -> {
                // For DB Version 4, just migrate userProfiles table
                migrateUserProfilesTable(db)
            }
        }
    }

    private fun getDeviceIdForAccountIdFromPrefs(accountId: String): String {
        val baseKey = Constants.DEVICE_ID_TAG + ":" + accountId
        val fallbackKey = Constants.FALLBACK_ID_TAG + ":" + accountId

        return StorageHelper.getString(context, baseKey, null)
            ?: if (config.isDefaultInstance) StorageHelper.getString(context, baseKey, null) else null
                ?: StorageHelper.getString(context, fallbackKey, "")
    }

    /**
     * This function migrates the userProfiles table to a new schema with a composite primary key of _id + deviceId
     * The older schema only had _id as the primary key
     * While migrating, the deviceId is back-filled and also the data string is corrected in format
     *
     * @param db
     */
    private fun migrateUserProfilesTable(db: SQLiteDatabase) {
        executeStatement(db, CREATE_TEMP_USER_PROFILES_TABLE)

        val deviceId = getDeviceIdForAccountIdFromPrefs(config.accountId)

        // Query to select all data from the old user profiles table
        val selectQuery = "SELECT ${Column.ID}, ${Column.DATA} FROM ${USER_PROFILES.tableName};"
        val cursor = db.rawQuery(selectQuery, null)

        cursor.use {
            if (cursor.moveToFirst()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(Column.ID))
                val dataString = cursor.getString(cursor.getColumnIndexOrThrow(Column.DATA))
                val updatedDataString = migrateDataString(dataString)

                // Insert the modified data into the temporary table
                val insertQuery =
                    """INSERT INTO temp_${USER_PROFILES.tableName} (${Column.ID}, ${Column.DEVICE_ID}, ${Column.DATA})
                                 VALUES ('$id', '$deviceId', '$updatedDataString');"""
                executeStatement(db, insertQuery)
            }
        }

        executeStatement(db, DROP_USER_PROFILES_TABLE)
        executeStatement(db, RENAME_USER_PROFILES_TABLE)
    }

    /**
     * This function migrates the data column for the userProfiles table
     * Removes the "$D_" prefix from date related property values
     * Removes the "$set" and "$add" key from incorrectly stored multi-valued properties
     */
    private fun migrateDataString(dataString: String): String {
        return try {
            val jsonObject = JSONObject(dataString)
            val keys = jsonObject.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                var value = jsonObject.get(key)

                if (value is String && value.startsWith(DATE_PREFIX)) {
                    value = (value.removePrefix(DATE_PREFIX)).toLong()
                    jsonObject.put(key, value)
                }

                if (value is JSONObject) {
                    if (value.has(COMMAND_SET))
                        jsonObject.put(key, value.getJSONArray(COMMAND_SET))
                    else if (value.has(COMMAND_ADD))
                        jsonObject.put(key, value.getJSONArray(COMMAND_ADD))
                }
            }

            jsonObject.toString()
        } catch (e: JSONException) {
            // Return the original string if an error occurs
            logger.verbose("Error while migrating data column for userProfiles table for data = $dataString", e)
            dataString
        }
    }

    @SuppressLint("UsableSpace")
    fun belowMemThreshold(): Boolean {
        return if (databaseFile.exists()) {
            max(databaseFile.usableSpace, DB_LIMIT.toLong()) >= databaseFile.length()
        } else {
            true
        }
    }

    fun deleteDatabase() {
        close()
        if (!databaseFile.delete()) {
            logger.debug("Could not delete database")
        }
    }

    private fun executeStatement(db: SQLiteDatabase, statement: String) {
        val sqLiteStatement = db.compileStatement(statement)
        logger.verbose("Executing - $statement")
        sqLiteStatement.execute()
    }
}

enum class Table(val tableName: String) {
    EVENTS("events"),
    PROFILE_EVENTS("profileEvents"),
    USER_PROFILES("userProfiles"),
    INBOX_MESSAGES("inboxMessages"),
    PUSH_NOTIFICATIONS("pushNotifications"),
    UNINSTALL_TS("uninstallTimestamp"),
    PUSH_NOTIFICATION_VIEWED("notificationViewed")
}

object Column {

    const val ID = "_id"
    const val DATA = "data"
    const val CREATED_AT = "created_at"
    const val IS_READ = "isRead"
    const val EXPIRES = "expires"
    const val TAGS = "tags"
    const val USER_ID = "messageUser"
    const val CAMPAIGN = "campaignId"
    const val WZRKPARAMS = "wzrkParams"
    const val DEVICE_ID = "deviceID"
}

private val CREATE_EVENTS_TABLE = """
    CREATE TABLE ${EVENTS.tableName} (
        ${Column.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
        ${Column.DATA} STRING NOT NULL,
        ${Column.CREATED_AT} INTEGER NOT NULL
    );
"""

private val CREATE_PROFILE_EVENTS_TABLE = """
    CREATE TABLE ${PROFILE_EVENTS.tableName} (
        ${Column.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
        ${Column.DATA} STRING NOT NULL,
        ${Column.CREATED_AT} INTEGER NOT NULL
    );
"""

private val CREATE_INBOX_MESSAGES_TABLE = """ 
    CREATE TABLE ${INBOX_MESSAGES.tableName} (
        ${Column.ID} STRING NOT NULL,
        ${Column.DATA} TEXT NOT NULL,
        ${Column.WZRKPARAMS} TEXT NOT NULL,
        ${Column.CAMPAIGN} STRING NOT NULL,
        ${Column.TAGS} TEXT NOT NULL,
        ${Column.IS_READ} INTEGER NOT NULL DEFAULT 0,
        ${Column.EXPIRES} INTEGER NOT NULL,
        ${Column.CREATED_AT} INTEGER NOT NULL,
        ${Column.USER_ID} STRING NOT NULL
    );
"""

private val INBOX_MESSAGES_COMP_ID_USERID_INDEX = """
    CREATE UNIQUE INDEX IF NOT EXISTS userid_id_idx ON ${INBOX_MESSAGES.tableName} (
        ${Column.USER_ID},
        ${Column.ID}
    );
"""
private val EVENTS_TIME_INDEX = """
    CREATE INDEX IF NOT EXISTS time_idx ON ${EVENTS.tableName} (${Column.CREATED_AT});
"""

private val PROFILE_EVENTS_TIME_INDEX = """
    CREATE INDEX IF NOT EXISTS time_idx ON ${PROFILE_EVENTS.tableName} ( ${Column.CREATED_AT});
"""

private val CREATE_PUSH_NOTIFICATIONS_TABLE = """
    CREATE TABLE ${PUSH_NOTIFICATIONS.tableName} (
        ${Column.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
        ${Column.DATA} STRING NOT NULL,
        ${Column.CREATED_AT} INTEGER NOT NULL,
        ${Column.IS_READ} INTEGER NOT NULL
    );
"""

private val PUSH_NOTIFICATIONS_TIME_INDEX = """
    CREATE INDEX IF NOT EXISTS time_idx ON ${PUSH_NOTIFICATIONS.tableName} (${Column.CREATED_AT});
"""

private val CREATE_UNINSTALL_TS_TABLE = """
    CREATE TABLE ${UNINSTALL_TS.tableName} (
        ${Column.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
        ${Column.CREATED_AT} INTEGER NOT NULL
    );
"""

private val UNINSTALL_TS_INDEX = """
    CREATE INDEX IF NOT EXISTS time_idx ON ${UNINSTALL_TS.tableName} (${Column.CREATED_AT});
"""

private val CREATE_NOTIFICATION_VIEWED_TABLE = """
    CREATE TABLE ${PUSH_NOTIFICATION_VIEWED.tableName} (
        ${Column.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
        ${Column.DATA} STRING NOT NULL,
        ${Column.CREATED_AT} INTEGER NOT NULL
    );
"""

private val NOTIFICATION_VIEWED_INDEX = """
    CREATE INDEX IF NOT EXISTS time_idx ON ${PUSH_NOTIFICATION_VIEWED.tableName} (${Column.CREATED_AT});
"""

private val DROP_TABLE_UNINSTALL_TS = "DROP TABLE IF EXISTS ${UNINSTALL_TS.tableName}"

private val DROP_TABLE_INBOX_MESSAGES = "DROP TABLE IF EXISTS ${INBOX_MESSAGES.tableName}"

private val DROP_TABLE_PUSH_NOTIFICATION_VIEWED = "DROP TABLE IF EXISTS ${PUSH_NOTIFICATION_VIEWED.tableName}"

private val CREATE_USER_PROFILES_TABLE = """
    CREATE TABLE ${USER_PROFILES.tableName} (
        ${Column.DEVICE_ID} STRING NOT NULL,
        ${Column.ID} STRING NOT NULL,
        ${Column.DATA} STRING NOT NULL,
        PRIMARY KEY (${Column.ID}, ${Column.DEVICE_ID})
    );
"""

private val CREATE_TEMP_USER_PROFILES_TABLE = """
    CREATE TABLE temp_${USER_PROFILES.tableName} (
        ${Column.ID} STRING NOT NULL,
        ${Column.DEVICE_ID} STRING NOT NULL,
        ${Column.DATA} STRING NOT NULL,
        PRIMARY KEY (${Column.ID}, ${Column.DEVICE_ID})
    );
"""

private val DROP_USER_PROFILES_TABLE = """
    DROP TABLE ${USER_PROFILES.tableName};
"""

private val RENAME_USER_PROFILES_TABLE = """
    ALTER TABLE temp_${USER_PROFILES.tableName} RENAME TO ${USER_PROFILES.tableName};
"""
