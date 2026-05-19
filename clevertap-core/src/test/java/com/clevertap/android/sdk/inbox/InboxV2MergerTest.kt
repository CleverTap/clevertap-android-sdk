package com.clevertap.android.sdk.inbox

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InboxV2MergerTest {

    private fun daoFor(
        id: String,
        expires: Long = Long.MAX_VALUE,
        read: Int = 0
    ): CTMessageDAO = object : CTMessageDAO() {
        override fun containsVideoOrAudio(): Boolean = false
    }.apply {
        setId(id)
        setExpires(expires)
        setRead(read)
    }

    private fun videoDaoFor(
        id: String,
        expires: Long = Long.MAX_VALUE
    ): CTMessageDAO = object : CTMessageDAO() {
        override fun containsVideoOrAudio(): Boolean = true
    }.apply {
        setId(id)
        setExpires(expires)
    }

    // ========== preWriteFilter ==========

    @Test
    fun `preWriteFilter passes new messages through unchanged`() {
        val m1 = daoFor("m1")

        val result = InboxV2Merger.preWriteFilter(
            incoming = listOf(m1),
            pendingDeletes = emptySet(),
            pendingReads = emptySet(),
            videoSupported = true,
            nowSec = 100
        )

        assertEquals(listOf(m1), result)
    }

    @Test
    fun `preWriteFilter drops pending-deleted ids`() {
        val result = InboxV2Merger.preWriteFilter(
            incoming = listOf(daoFor("m1")),
            pendingDeletes = setOf("m1"),
            pendingReads = emptySet(),
            videoSupported = true,
            nowSec = 100
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `preWriteFilter drops expired messages`() {
        val result = InboxV2Merger.preWriteFilter(
            incoming = listOf(daoFor("m1", expires = 50)),
            pendingDeletes = emptySet(),
            pendingReads = emptySet(),
            videoSupported = true,
            nowSec = 100
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `preWriteFilter drops video when video unsupported`() {
        val result = InboxV2Merger.preWriteFilter(
            incoming = listOf(videoDaoFor("m1")),
            pendingDeletes = emptySet(),
            pendingReads = emptySet(),
            videoSupported = false,
            nowSec = 100
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `preWriteFilter keeps video when video supported`() {
        val result = InboxV2Merger.preWriteFilter(
            incoming = listOf(videoDaoFor("m1")),
            pendingDeletes = emptySet(),
            pendingReads = emptySet(),
            videoSupported = true,
            nowSec = 100
        )
        assertEquals(1, result.size)
        assertEquals("m1", result.single().id)
    }

    @Test
    fun `preWriteFilter applies pending-read override`() {
        val result = InboxV2Merger.preWriteFilter(
            incoming = listOf(daoFor("m1", read = 0)),
            pendingDeletes = emptySet(),
            pendingReads = setOf("m1"),
            videoSupported = true,
            nowSec = 100
        )
        assertEquals(1, result.single().isRead)
    }

    // ========== postReadCleanup ==========

    @Test
    fun `postReadCleanup splits toDelete and kept correctly`() {
        val survivor = daoFor("m1")
        val pendingDel = daoFor("m2")
        val expired = daoFor("m3", expires = 50)
        val video = videoDaoFor("m4")
        val pendingRead = daoFor("m5", read = 0)

        val result = InboxV2Merger.postReadCleanup(
            full = listOf(survivor, pendingDel, expired, video, pendingRead),
            pendingDeletes = setOf("m2"),
            pendingReads = setOf("m5"),
            videoSupported = false,
            nowSec = 100
        )

        assertEquals(listOf("m2", "m3", "m4"), result.toDelete)
        assertEquals(listOf("m1", "m5"), result.finalList.map { it.id })
        assertEquals(1, result.finalList.first { it.id == "m5" }.isRead)
    }

    @Test
    fun `postReadCleanup on empty list is a no-op`() {
        val result = InboxV2Merger.postReadCleanup(
            full = emptyList(),
            pendingDeletes = emptySet(),
            pendingReads = emptySet(),
            videoSupported = true,
            nowSec = 100
        )
        assertTrue(result.toDelete.isEmpty())
        assertTrue(result.finalList.isEmpty())
    }

    @Test
    fun `postReadCleanup removes row that survived an app-kill after pending-delete`() {
        val stale = daoFor("m1")

        val result = InboxV2Merger.postReadCleanup(
            full = listOf(stale),
            pendingDeletes = setOf("m1"),
            pendingReads = emptySet(),
            videoSupported = true,
            nowSec = 100
        )

        assertEquals(listOf("m1"), result.toDelete)
        assertTrue(result.finalList.isEmpty())
    }
}
