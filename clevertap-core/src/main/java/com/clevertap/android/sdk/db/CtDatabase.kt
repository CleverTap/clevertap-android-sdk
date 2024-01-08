package com.clevertap.android.sdk.db

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.Table.EVENTS
import com.clevertap.android.sdk.db.Table.INBOX_MESSAGES
import com.clevertap.android.sdk.db.Table.PROFILE_EVENTS
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATIONS
import com.clevertap.android.sdk.db.Table.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.db.Table.UNINSTALL_TS
import com.clevertap.android.sdk.db.Table.USER_PROFILES
import java.io.File
import kotlin.math.max

class DatabaseHelper internal constructor(context: Context, dbName: String?, private val logger: Logger) :
    SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {

    companion object {

        private const val DATABASE_VERSION = 3
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
                // For DB Version 2, just adding Push Notifications, Uninstall TS and Inbox Messages tables and related indices
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
            }

            2 -> {
                // For DB Version 3, just adding Push Notification Viewed table and index
                executeStatement(db, DROP_TABLE_PUSH_NOTIFICATION_VIEWED)
                executeStatement(db, CREATE_NOTIFICATION_VIEWED_TABLE)
                executeStatement(db, NOTIFICATION_VIEWED_INDEX)
            }
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

private val CREATE_USER_PROFILES_TABLE = """
    CREATE TABLE ${USER_PROFILES.tableName} (
        ${Column.ID} STRING UNIQUE PRIMARY KEY,
        ${Column.DATA} STRING NOT NULL
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
