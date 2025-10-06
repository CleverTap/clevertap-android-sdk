package com.clevertap.android.sdk.db

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.dao.*
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.sdk.usereventlogs.UserEventLogDAO
import com.clevertap.android.sdk.usereventlogs.UserEventLogDAOImpl
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONObject

/**
 * Refactored DBAdapter following Single Responsibility Principle
 * Each table now has its own dedicated DAO for better maintainability
 */
internal class DBAdapter constructor(
    context: Context,
    databaseName: String,
    private val accountId: String,
    private val logger: ILogger,
    private val dbEncryptionHandler: DBEncryptionHandler,
    private val clock: Clock = Clock.SYSTEM
) {

    companion object {
        internal const val DB_UPDATE_ERROR = -1L
        internal const val DB_OUT_OF_MEMORY_ERROR = -2L
        internal const val NOT_ENOUGH_SPACE_LOG =
            "There is not enough space left on the device to store data, data discarded"
        private const val DATABASE_NAME = "clevertap"

        fun getDatabaseName(config: CleverTapInstanceConfig): String {
            return if (config.isDefaultInstance) DATABASE_NAME else DATABASE_NAME + "_" + config.accountId
        }
    }

    private val dbHelper: DatabaseHelper = DatabaseHelper(
        context = context,
        accountId = accountId,
        dbName = databaseName,
        logger = logger
    )

    // DAO instances - lazy initialization for better performance
    private val eventDAO: EventDAO by lazy { EventDAOImpl(dbHelper, logger, dbEncryptionHandler, clock) }
    private val inboxMessageDAO: InboxMessageDAO by lazy { InboxMessageDAOImpl(dbHelper, logger, dbEncryptionHandler) }
    private val userProfileDAO: UserProfileDAO by lazy { UserProfileDAOImpl(dbHelper, logger, dbEncryptionHandler) }
    private val pushNotificationDAO: PushNotificationDAO by lazy { PushNotificationDAOImpl(dbHelper, logger, clock) }
    private val uninstallTimestampDAO: UninstallTimestampDAO by lazy { UninstallTimestampDAOImpl(dbHelper, logger) }

    @Volatile
    private var userEventLogDao: UserEventLogDAO? = null

    // =====================================================
    // EVENT-RELATED OPERATIONS
    // =====================================================

    @WorkerThread
    @Synchronized
    fun storeObject(obj: JSONObject, table: Table): Long = eventDAO.storeEvent(obj, table)

    @WorkerThread
    @Synchronized
    fun fetchEvents(table: Table, limit: Int): QueueData = eventDAO.fetchEvents(table, limit)

    @WorkerThread
    @Synchronized
    fun fetchCombinedEvents(batchSize: Int): QueueData = eventDAO.fetchCombinedEvents(batchSize)

    @WorkerThread
    @Synchronized
    fun cleanupEventsFromLastId(lastId: String, table: Table) = eventDAO.cleanupEventsFromLastId(lastId, table)

    @Synchronized
    fun cleanupStaleEvents(table: Table) = eventDAO.cleanupStaleEvents(table)

    @Synchronized
    fun removeEvents(table: Table) = eventDAO.removeAllEvents(table)

    // =====================================================
    // INBOX MESSAGE OPERATIONS
    // =====================================================

    @WorkerThread
    @Synchronized
    fun getMessages(userId: String): ArrayList<CTMessageDAO> = inboxMessageDAO.getMessages(userId)

    @WorkerThread
    @Synchronized
    fun upsertMessages(inboxMessages: List<CTMessageDAO>) = inboxMessageDAO.upsertMessages(inboxMessages)

    @WorkerThread
    @Synchronized
    fun deleteMessageForId(messageId: String?, userId: String?): Boolean {
        return if (messageId != null && userId != null) {
            inboxMessageDAO.deleteMessage(messageId, userId)
        } else false
    }

    @WorkerThread
    @Synchronized
    fun deleteMessagesForIDs(messageIDs: List<String?>?, userId: String?): Boolean {
        return if (messageIDs != null && userId != null) {
            val validIds = messageIDs.filterNotNull()
            if (validIds.isNotEmpty()) {
                inboxMessageDAO.deleteMessages(validIds, userId)
            } else false
        } else false
    }

    @WorkerThread
    @Synchronized
    fun markReadMessageForId(messageId: String?, userId: String?): Boolean {
        return if (messageId != null && userId != null) {
            inboxMessageDAO.markMessageAsRead(messageId, userId)
        } else false
    }

    @WorkerThread
    @Synchronized
    fun markReadMessagesForIds(messageIDs: List<String?>?, userId: String?): Boolean {
        return if (messageIDs != null && userId != null) {
            val validIds = messageIDs.filterNotNull()
            if (validIds.isNotEmpty()) {
                inboxMessageDAO.markMessagesAsRead(validIds, userId)
            } else false
        } else false
    }

    // =====================================================
    // USER PROFILE OPERATIONS
    // =====================================================

    @WorkerThread
    @Synchronized
    fun storeUserProfile(id: String?, deviceId: String?, obj: JSONObject): Long {
        return if (id != null && deviceId != null) {
            userProfileDAO.storeUserProfile(id, deviceId, obj)
        } else DB_UPDATE_ERROR
    }

    @Synchronized
    fun fetchUserProfilesByAccountId(accountId: String?): Map<String, JSONObject> {
        return if (accountId != null) {
            userProfileDAO.fetchUserProfilesByAccountId(accountId)
        } else emptyMap()
    }

    @Synchronized
    fun fetchUserProfileByAccountIdAndDeviceID(accountId: String?, deviceId: String?): JSONObject? {
        return if (accountId != null && deviceId != null) {
            userProfileDAO.fetchUserProfile(accountId, deviceId)
        } else null
    }

    // =====================================================
    // PUSH NOTIFICATION OPERATIONS
    // =====================================================

    @Synchronized
    fun storePushNotificationId(id: String, ttlInSeconds: Long) {
        pushNotificationDAO.storePushNotificationId(id, ttlInSeconds)
    }

    @Synchronized
    fun fetchPushNotificationIds(): Array<String> = pushNotificationDAO.fetchPushNotificationIds()

    @Synchronized
    fun doesPushNotificationIdExist(id: String): Boolean = pushNotificationDAO.doesPushNotificationIdExist(id)

    @WorkerThread
    @Synchronized
    fun updatePushNotificationIds(ids: Array<String>) = pushNotificationDAO.updatePushNotificationIds(ids)

    @Synchronized
    fun cleanUpPushNotifications() = pushNotificationDAO.cleanUpPushNotifications()

    // =====================================================
    // UNINSTALL TIMESTAMP OPERATIONS
    // =====================================================

    @Synchronized
    fun storeUninstallTimestamp() = uninstallTimestampDAO.storeUninstallTimestamp()

    @Synchronized
    fun getLastUninstallTimestamp(): Long = uninstallTimestampDAO.getLastUninstallTimestamp()

    // =====================================================
    // USER EVENT LOG OPERATIONS
    // =====================================================

    @WorkerThread
    fun userEventLogDAO(): UserEventLogDAO {
        return userEventLogDao ?: synchronized(this) {
            userEventLogDao ?: UserEventLogDAOImpl(
                dbHelper, logger, Table.USER_EVENT_LOGS_TABLE
            ).also { userEventLogDao = it }
        }
    }

    // =====================================================
    // UTILITY METHODS
    // =====================================================

    @VisibleForTesting
    internal fun deleteDB() {
        dbHelper.deleteDatabase()
    }
}
