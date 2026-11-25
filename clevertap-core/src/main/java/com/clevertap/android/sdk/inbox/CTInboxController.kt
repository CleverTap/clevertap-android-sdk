package com.clevertap.android.sdk.inbox

import androidx.annotation.AnyThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.db.dao.PendingDelete
import com.clevertap.android.sdk.db.dao.PendingRead
import com.clevertap.android.sdk.response.InboxV2DeliverySource
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONException
import java.util.Collections
import java.util.concurrent.Callable

/**
 * Controller class which handles Users and Messages for the Notification Inbox
 */
internal open class CTInboxController @VisibleForTesting internal constructor(
    private val config: CleverTapInstanceConfig,
    guid: String?,
    private val dbAdapter: DBAdapter,
    private val ctLockManager: CTLockManager,
    private val callbackManager: BaseCallbackManager,
    private val videoSupported: Boolean,
    private val inboxDeleteCoordinator: InboxDeleteCoordinator?,
    private val clock: Clock
) {

    private var messagesList: ArrayList<CTMessageDAO>

    private val messagesLock = Any()

    @get:AnyThread
    // Java let a null guid through to dbAdapter.getMessages(), which rejects it; fail at the same point.
    val userId: String = guid!!

    init {
        this.messagesList = this.dbAdapter.getMessages(this.userId)
    }

    // always call async
    @WorkerThread
    constructor(
        config: CleverTapInstanceConfig, guid: String?, adapter: DBAdapter,
        ctLockManager: CTLockManager,
        callbackManager: BaseCallbackManager,
        videoSupported: Boolean,
        inboxDeleteCoordinator: InboxDeleteCoordinator?
    ) : this(
        config, guid, adapter, ctLockManager, callbackManager, videoSupported,
        inboxDeleteCoordinator, Clock.SYSTEM
    )

    fun count(): Int {
        return messages.size
    }

    @AnyThread
    fun deleteInboxMessage(message: CTInboxMessage?) {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("deleteInboxMessage", Callable<Void?> {
            val message = message!!
            // Look up the DAO directly. Must happen BEFORE
            // the local delete wipes the DAO from cache.
            val dao = findMessageById(message.messageId)
            val isV2 = dao != null && dao.source == InboxMessageSource.V2

            if (isV2) {
                val now = clock.currentTimeSeconds()
                dbAdapter.addPendingDelete(
                    message.messageId,
                    userId,
                    dao!!.wzrkParams,
                    resolvePendingActionExpiry(dao, now)
                )
            }
            synchronized(ctLockManager.inboxControllerLock) {
                val update = _deleteMessageWithId(message.messageId)
                if (update) {
                    callbackManager._notifyInboxMessagesDidUpdate()
                }
            }
            if (isV2 && inboxDeleteCoordinator != null) {
                inboxDeleteCoordinator.syncDelete(Collections.singletonList(message), userId)
            }
            null
        })
    }

    @AnyThread
    fun deleteInboxMessagesForIDs(messageIDs: ArrayList<String>?) {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("deleteInboxMessagesForIDs", Callable<Void?> {
            val messageIDs = messageIDs!!
            // Partition ids into V1 and V2 BEFORE the local delete wipes
            // the cache.
            val now = clock.currentTimeSeconds()
            val idSet: Set<String> = HashSet(messageIDs)
            val v2Messages: MutableList<CTInboxMessage> = ArrayList()
            val pendingRows: MutableList<PendingDelete> = ArrayList()
            synchronized(messagesLock) {
                for (d in messagesList) {
                    if (!idSet.contains(d.id)) {
                        continue
                    }
                    if (d.source != InboxMessageSource.V2) {
                        continue
                    }
                    v2Messages.add(CTInboxMessage(d.toJSON()))
                    pendingRows.add(
                        PendingDelete(
                            d.id,
                            d.wzrkParams,
                            resolvePendingActionExpiry(d, now)
                        )
                    )
                }
            }

            if (pendingRows.isNotEmpty()) {
                dbAdapter.addPendingDeletes(pendingRows, userId)
            }
            synchronized(ctLockManager.inboxControllerLock) {
                val update = _deleteMessagesForIds(messageIDs)
                if (update) {
                    callbackManager._notifyInboxMessagesDidUpdate()
                }
            }
            if (inboxDeleteCoordinator != null && v2Messages.isNotEmpty()) {
                inboxDeleteCoordinator.syncDelete(v2Messages, userId)
            }
            null
        })
    }

    @AnyThread
    private fun getInboxMessageForId(messageId: String?): CTInboxMessage? {
        val dao = findMessageById(messageId)
        return if (dao == null) null else CTInboxMessage(dao.toJSON())
    }

    @AnyThread
    fun getMessageForId(messageId: String?): CTMessageDAO? {
        return findMessageById(messageId)
    }

    @get:AnyThread
    val messages: ArrayList<CTMessageDAO>
        get() {
            synchronized(messagesLock) {
                trimMessages()
                return messagesList
            }
        }

    @get:AnyThread
    val unreadMessages: ArrayList<CTMessageDAO>
        get() {
            val unread = ArrayList<CTMessageDAO>()
            synchronized(messagesLock) {
                val messages = messages
                for (message in messages) {
                    if (message.isRead == 0) {
                        unread.add(message)
                    }
                }
            }
            return unread
        }

    @AnyThread
    fun markReadInboxMessage(message: CTInboxMessage?) {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("markReadInboxMessage", Callable<Void?> {
            val message = message!!
            synchronized(ctLockManager.inboxControllerLock) {
                val read = _markReadForMessageWithId(message.messageId)
                if (read) {
                    callbackManager._notifyInboxMessagesDidUpdate()
                }
            }
            null
        })
    }

    @AnyThread
    fun markReadInboxMessagesForIDs(messageIDs: ArrayList<String>?) {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("markReadInboxMessagesForIDs", Callable<Void?> {
            val messageIDs = messageIDs!!
            synchronized(ctLockManager.inboxControllerLock) {
                val read = _markReadForMessagesWithIds(messageIDs)
                if (read) {
                    callbackManager._notifyInboxMessagesDidUpdate()
                }
            }
            null
        })
    }

    @AnyThread
    fun unreadCount(): Int {
        return unreadMessages.size
    }

    // always call async
    @WorkerThread
    fun updateMessages(inboxMessages: JSONArray): Boolean {
        Logger.v("CTInboxController:updateMessages() called")

        var haveUpdates = false
        val newMessages = ArrayList<CTMessageDAO>()

        for (i in 0 until inboxMessages.length()) {
            try {
                val messageDAO = CTMessageDAO.initWithJSON(
                    inboxMessages.getJSONObject(i), this.userId, InboxMessageSource.V1
                ) ?: continue

                if (!videoSupported && messageDAO.containsVideoOrAudio()) {
                    Logger.d(
                        "Dropping inbox message containing video/audio as app does not support video. For more information checkout CleverTap documentation."
                    )
                    continue
                }

                newMessages.add(messageDAO)

                Logger.v("Inbox Message for message id - " + messageDAO.id + " added")
            } catch (e: JSONException) {
                Logger.d("Unable to update notification inbox messages - " + e.localizedMessage)
            }
        }

        if (newMessages.size > 0) {
            this.dbAdapter.upsertMessages(newMessages)
            haveUpdates = true
            Logger.v("New Notification Inbox messages added")
            synchronized(messagesLock) {
                this.messagesList = this.dbAdapter.getMessages(this.userId)
                trimMessages()
            }
        }
        return haveUpdates
    }

    /**
     * Applies an already-parsed V2 message list to the store.
     * Caller must hold `inboxControllerLock`; firing the UI callback
     * is the caller's responsibility (mirrors the V1 [updateMessages]
     * contract). The dual-filter math lives in [InboxV2Merger] as pure
     * functions; this method only sequences DB reads/writes around it.
     *
     * When `source` is [InboxV2DeliverySource.FETCH] the method
     * additionally runs the cross-device delete sweep:
     * 1. Promotes `PENDING_INDEXING` rows whose ids appear in the fetch
     *    response to `INDEXED` via a targeted UPDATE (never downgrades).
     * 2. Identifies V2 rows that are `INDEXED` (or stale
     *    `PENDING_INDEXING` older than [INDEXING_GRACE_SECONDS])
     *    but absent from this response — these are treated as cross-device
     *    deletes and removed from the DB before the post-read cleanup re-read.
     *
     * When `source` is [InboxV2DeliverySource.A1] the sweep is
     * skipped: an `/a1` payload is not a complete inbox snapshot, so
     * "absent from response" cannot be used as a delete signal.
     */
    @WorkerThread
    fun processV2Response(incoming: List<CTMessageDAO>, source: InboxV2DeliverySource): Boolean {
        val nowSec = clock.currentTimeSeconds()
        var updated = false

        // ── FETCH-only: promote PENDING_INDEXING rows that appeared in this
        // response to INDEXED, then sweep V2 rows absent from the authoritative
        // fetch snapshot (cross-device delete signal).
        if (source == InboxV2DeliverySource.FETCH) {
            val incomingIds: MutableSet<String> = HashSet(incoming.size)
            for (dao in incoming) {
                incomingIds.add(dao.id)
            }
            if (incomingIds.isNotEmpty()) {
                config.logger.verbose(
                    config.accountId,
                    "InboxV2: markIndexed " + incomingIds.size + " msg(s)"
                )
                dbAdapter.markIndexed(ArrayList(incomingIds), userId)
            }

            // Sweep: INDEXED V2 rows (and stale PENDING_INDEXING older than
            // the grace cutoff) that are absent from this fetch response are
            // treated as cross-device deletes. Deleted from DB here so the
            // postReadCleanup re-read below won't see them.
            val graceCutoff = nowSec - INDEXING_GRACE_SECONDS
            val sweepable = dbAdapter.findSweepableV2Ids(userId, graceCutoff) as MutableSet<String>
            sweepable.removeAll(incomingIds)
            if (sweepable.isNotEmpty()) {
                config.logger.verbose(
                    config.accountId,
                    "InboxV2: cross-device sweep — removing " + sweepable.size + " msg(s): " + sweepable
                )
                dbAdapter.deleteMessagesForIDs(ArrayList(sweepable), userId)
                updated = true
            } else {
                config.logger.verbose(
                    config.accountId,
                    "InboxV2: cross-device sweep — nothing to remove"
                )
            }
        }

        // T6.2: drop AWAITING_CONFIRM pending-deletes whose TTL has elapsed.
        val removedDeletes = dbAdapter.removeExpiredAwaitingConfirm(userId, nowSec)
        if (removedDeletes > 0) {
            config.logger.verbose(
                config.accountId,
                "InboxV2: removed $removedDeletes expired AWAITING_CONFIRM row(s)"
            )
        }
        // T7.2: drop pending-reads whose TTL has elapsed (server may never echo
        // back e.g. if the message was deleted, retargeted, or expired).
        val removedReads = dbAdapter.removeExpiredPendingReads(userId, nowSec)
        if (removedReads > 0) {
            config.logger.verbose(
                config.accountId,
                "InboxV2: removed $removedReads expired pending-read row(s)"
            )
        }
        val pendingDeletes = dbAdapter.getPendingDeleteIds(userId)
        val pendingReads = dbAdapter.getPendingReads(userId)

        // Server-caught-up: any incoming message with isRead=1 that was still
        // pending locally confirms the read — drop the pending row. Must run
        // BEFORE preWriteFilter, which mutates the incoming DAOs in-place via
        // the pending-read override and would make every pending id look
        // server-confirmed.
        if (pendingReads.isNotEmpty()) {
            val confirmedReads: MutableList<String> = ArrayList()
            for (dao in incoming) {
                if (dao.isRead == 1 && pendingReads.contains(dao.id)) {
                    confirmedReads.add(dao.id)
                }
            }
            if (confirmedReads.isNotEmpty()) {
                dbAdapter.removePendingReads(confirmedReads, userId)
            }
        }

        val toUpsert = InboxV2Merger.preWriteFilter(
            incoming, pendingDeletes, pendingReads, videoSupported, nowSec
        )
        if (toUpsert.isNotEmpty()) {
            // FETCH is the authoritative source — new rows from this response are
            // server-confirmed, so write them as INDEXED directly. ON CONFLICT
            // preserves index_state for existing rows, so this only affects INSERTs.
            if (source == InboxV2DeliverySource.FETCH) {
                for (dao in toUpsert) {
                    dao.indexState = InboxIndexState.INDEXED
                }
            }
            dbAdapter.upsertMessages(toUpsert)
        }
        updated = updated || toUpsert.isNotEmpty()

        synchronized(messagesLock) {
            val full = dbAdapter.getMessages(userId)
            val cleanup = InboxV2Merger.postReadCleanup(
                full, pendingDeletes, pendingReads, videoSupported, nowSec
            )

            if (cleanup.toDelete.isNotEmpty()) {
                dbAdapter.deleteMessagesForIDs(cleanup.toDelete, userId)
                updated = true
            }
            this.messagesList = ArrayList(cleanup.finalList)
        }
        return updated
    }

    @AnyThread
    fun _deleteMessageWithId(messageId: String?): Boolean {
        val messageDAO = findMessageById(messageId) ?: return false
        synchronized(messagesLock) {
            this.messagesList.remove(messageDAO)
        }

        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("RunDeleteMessage", Callable<Void?> {
            dbAdapter.deleteMessageForId(messageId, userId)
            null
        })
        return true
    }

    @AnyThread
    fun _deleteMessagesForIds(messageIDs: ArrayList<String>): Boolean {
        val messageDAOList = ArrayList<CTMessageDAO>()
        for (messageID in messageIDs) {
            val messageDAO = findMessageById(messageID) ?: continue
            messageDAOList.add(messageDAO)
        }
        if (messageDAOList.isEmpty()) {
            return false
        }

        synchronized(messagesLock) {
            this.messagesList.removeAll(messageDAOList)
        }

        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("RunDeleteMessagesForIDs", Callable<Void?> {
            dbAdapter.deleteMessagesForIDs(messageIDs, userId)
            null
        })
        return true
    }

    @AnyThread
    fun _markReadForMessageWithId(messageId: String?): Boolean {
        val messageDAO = findMessageById(messageId) ?: return false

        val isV2 = messageDAO.source == InboxMessageSource.V2
        val pendingReadExpiresAt = if (isV2) {
            resolvePendingActionExpiry(messageDAO, clock.currentTimeSeconds())
        } else {
            0L
        }

        synchronized(messagesLock) {
            messageDAO.isRead = 1
        }
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.addOnSuccessListener { callbackManager._notifyInboxMessagesDidUpdate() } //  //OR callbackManager.getInboxListener().inboxMessagesDidUpdate();
        task.addOnFailureListener { e -> Logger.d("Failed to update message read state for id:$messageId", e) }

        task.execute("RunMarkMessageRead", Callable<Void?> {
            dbAdapter.markReadMessageForId(messageId, userId)
            if (isV2) {
                dbAdapter.addPendingRead(messageId!!, userId, pendingReadExpiresAt)
            }
            null
        })
        return true
    }

    @AnyThread
    fun _markReadForMessagesWithIds(messageIDs: ArrayList<String>): Boolean {
        // One pass over `messages` under a single lock
        val idSet: Set<String> = HashSet(messageIDs)
        val pendingReadRows: MutableList<PendingRead> = ArrayList()
        var atleastOneMessageIsValid = false

        synchronized(messagesLock) {
            val now = clock.currentTimeSeconds()
            for (dao in messagesList) {
                if (!idSet.contains(dao.id)) {
                    continue
                }
                atleastOneMessageIsValid = true
                dao.isRead = 1
                if (dao.source == InboxMessageSource.V2) {
                    pendingReadRows.add(PendingRead(dao.id, resolvePendingActionExpiry(dao, now)))
                }
            }
        }

        if (!atleastOneMessageIsValid) {
            return false
        }

        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.addOnSuccessListener { callbackManager._notifyInboxMessagesDidUpdate() }
        task.addOnFailureListener { e -> Logger.d("Failed to update message read state for ids:$messageIDs", e) }

        task.execute("RunMarkMessagesReadForIDs", Callable<Void?> {
            dbAdapter.markReadMessagesForIds(messageIDs, userId)
            if (pendingReadRows.isNotEmpty()) {
                dbAdapter.addPendingReads(pendingReadRows, userId)
            }
            null
        })
        return true
    }

    /**
     * Single entry point for callers that need to branch on V1 vs V2 without
     * reading source off the public [CTInboxMessage]. Looks up the DAO
     * in the in-memory list under `messagesLock`.
     *
     * @return `true` only when the message exists in cache and is V2.
     *         Unknown ids and V1 messages both return `false` — both
     *         are treated as "do not perform V2-specific behavior".
     */
    @AnyThread
    fun isV2Message(id: String?): Boolean {
        if (id == null) {
            return false
        }
        synchronized(messagesLock) {
            for (message in messagesList) {
                if (id == message.id) {
                    return message.source == InboxMessageSource.V2
                }
            }
        }
        return false
    }

    @AnyThread
    private fun findMessageById(id: String?): CTMessageDAO? {
        synchronized(messagesLock) {
            for (message in messagesList) {
                if (message.id == id) {
                    return message
                }
            }
        }
        Logger.v("Inbox Message for message id - $id not found")
        return null
    }

    @AnyThread
    private fun trimMessages() {
        Logger.v("CTInboxController:trimMessages() called")
        val toDelete = ArrayList<CTMessageDAO>()
        synchronized(messagesLock) {
            for (message in this.messagesList) {
                if (!videoSupported && message.containsVideoOrAudio()) {
                    Logger.d(
                        "Removing inbox message containing video/audio as app does not support video. For more information checkout CleverTap documentation."
                    )
                    toDelete.add(message)
                    continue
                }
                val expires = message.expires
                val expired = expires > 0 && clock.currentTimeSeconds() > expires
                if (expired) {
                    Logger.v("Inbox Message: " + message.id + " is expired - removing")
                    toDelete.add(message)
                }
            }

            if (toDelete.size <= 0) {
                return
            }

            for (message in toDelete) {
                _deleteMessageWithId(message.id)
            }
        }
    }

    companion object {

        private const val PENDING_DELETE_DEFAULT_TTL_SECONDS = 24L * 60L * 60L

        // BE's documented indexing window is 1–2 h. 3× as a conservative buffer trades
        // a small flicker risk (if BE indexing slips beyond 6 h) for faster cross-device
        // delete reflection on the firing device. Tunable in one place.
        @VisibleForTesting
        const val INDEXING_GRACE_SECONDS = 6L * 60L * 60L

        private fun resolvePendingActionExpiry(dao: CTMessageDAO?, nowSeconds: Long): Long {
            val ttl = dao?.expires ?: 0L
            if (ttl == 0L) {
                return 0L // infinite TTL — never expires
            }
            return if (ttl > 0L) ttl else nowSeconds + PENDING_DELETE_DEFAULT_TTL_SECONDS
        }
    }
}
