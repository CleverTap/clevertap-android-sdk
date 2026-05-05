package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.fetch.CallResult
import com.clevertap.android.sdk.network.fetch.InboxDeleteCall
import com.clevertap.android.sdk.network.fetch.NetworkScope
import com.clevertap.android.sdk.utils.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Launches V2 inbox-delete calls on the shared [NetworkScope] and keeps the
 * `inbox_pending_deletes` table in sync. All-or-nothing: on 2xx, every
 * pending row clears in one transaction; on any other outcome, every row
 * stays for the next retry.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InboxDeleteCoordinator(
    private val networkScope: NetworkScope,
    private val ctApi: CtApi,
    private val queueHeaderBuilder: QueueHeaderBuilder,
    // Supplier so the DB adapter is loaded on the network dispatcher, not at
    // factory-construction time — `DBManager.loadDBAdapter` is @WorkerThread.
    private val dbAdapterProvider: () -> DBAdapter,
    private val coreMetaData: CoreMetaData,
    private val packageName: String,
    private val logger: Logger,
    private val clock: Clock = Clock.SYSTEM,
    private val httpDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Fires one HTTP call for every supplied message. Used on the inline
     * delete path where the caller already has the full [CTInboxMessage]
     * (with `wzrk_*` params) in hand.
     */
    fun syncDelete(messages: List<CTInboxMessage>, userId: String) {
        if (messages.isEmpty()) return
        logger.verbose("InboxV2", "syncDelete launching for n=${messages.size} message(s)")
        networkScope.coroutineScope.launch {
            runDelete(messages, userId)
        }
    }

    /**
     * Drains any leftover `inbox_pending_deletes` rows for [userId]. Called
     * at inbox-init time so a delete that failed in the previous session
     * (no network / server 5xx) eventually lands.
     *
     * Local messages are gone by now, so the retry payload carries only the
     * `_id` — no `wzrk_*` attribution. Acceptable graceful degradation: the
     * server still records the delete; attribution is best-effort.
     */
    fun retryPending(userId: String) {
        networkScope.coroutineScope.launch {
            val ids = dbAdapterProvider().getPendingDeletes(userId)
            if (ids.isEmpty()) return@launch
            logger.verbose("InboxV2", "retryPending: ${ids.size} pending delete row(s) for user")
            val idOnly = ids.map { id -> CTInboxMessage(JSONObject().put("id", id)) }
            runDelete(idOnly, userId)
        }
    }

    private suspend fun runDelete(messages: List<CTInboxMessage>, userId: String) {
        val call = InboxDeleteCall(
            ctApi = ctApi,
            queueHeaderBuilder = queueHeaderBuilder,
            messages = messages,
            coreMetaData = coreMetaData,
            packageName = packageName,
            logger = logger,
            clock = clock,
            dispatcher = httpDispatcher
        )
        when (call.execute()) {
            is CallResult.Success -> {
                val ids = messages.map { it.messageId }
                dbAdapterProvider().removePendingDeletes(ids, userId)
                logger.verbose("InboxV2", "syncDelete confirmed for n=${ids.size} — pending rows cleared")
            }
            else -> {
                logger.verbose("InboxV2", "delete batch (n=${messages.size}) did not confirm; will retry")
            }
        }
    }
}
