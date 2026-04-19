package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo

/**
 * Pure dual-filter logic for Inbox V2 responses.
 *
 * Zero dependencies — no DB, no lock, no logger. Callers hand in every
 * input they need; outputs are fresh lists the caller then persists.
 *
 * Splitting the math out of the controller lets unit tests exercise it
 * without mocks and guarantees the same predicates run consistently in
 * both the pre-write and post-read passes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object InboxV2Merger {

    /**
     * Pass 1 — filter the V2 response in-memory before any DB write.
     *
     * Skips messages we'd immediately delete anyway (pending-delete, expired,
     * unsupported media) and honours local pending-read state over server
     * `isRead`.
     */
    fun preWriteFilter(
        incoming: List<CTMessageDAO>,
        pendingDeletes: Set<String>,
        pendingReads: Set<String>,
        videoSupported: Boolean,
        nowSec: Long
    ): List<CTMessageDAO> = incoming.mapNotNull { dao ->
        when {
            dao.id in pendingDeletes -> null
            !videoSupported && dao.containsVideoOrAudio() -> null
            isExpired(dao, nowSec) -> null
            else -> dao.also {
                if (it.id in pendingReads) it.setRead(1)
            }
        }
    }

    /**
     * Pass 2 — runs after re-reading the full DB. Produces the batch delete
     * list plus the updated in-memory list (with pending-reads applied).
     */
    fun postReadCleanup(
        full: List<CTMessageDAO>,
        pendingDeletes: Set<String>,
        pendingReads: Set<String>,
        videoSupported: Boolean,
        nowSec: Long
    ): CleanupResult {
        val toDelete = ArrayList<String>()
        val kept = ArrayList<CTMessageDAO>(full.size)

        for (dao in full) {
            when {
                dao.id in pendingDeletes -> toDelete.add(dao.id)
                isExpired(dao, nowSec) -> toDelete.add(dao.id)
                !videoSupported && dao.containsVideoOrAudio() -> toDelete.add(dao.id)
                else -> {
                    if (dao.id in pendingReads) dao.isRead = 1
                    kept.add(dao)
                }
            }
        }
        return CleanupResult(toDelete, kept)
    }

    private fun isExpired(dao: CTMessageDAO, nowSec: Long): Boolean {
        val expires = dao.expires
        return expires in 1..<nowSec
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal data class CleanupResult(
    val toDelete: List<String>,
    val finalList: List<CTMessageDAO>
)
