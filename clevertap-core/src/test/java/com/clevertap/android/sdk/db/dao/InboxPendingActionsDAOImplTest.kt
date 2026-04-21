package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InboxPendingActionsDAOImplTest : BaseTestCase() {

    private lateinit var dao: InboxPendingActionsDAO
    private lateinit var dbHelper: DatabaseHelper

    private val userA = "user-a"
    private val userB = "user-b"

    override fun setUp() {
        super.setUp()
        val config = CleverTapInstanceConfig.createInstance(appCtx, "acc-1", "token")
        dbHelper = DatabaseHelper(appCtx, config.accountId, "test_pending_actions_db", config.logger)
        dao = InboxPendingActionsDAOImpl(dbHelper, config.logger, TestClock())
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun `addPendingDelete stores the id and getPendingDeletes returns it`() {
        assertTrue(dao.addPendingDelete("m1", userA))

        assertEquals(setOf("m1"), dao.getPendingDeletes(userA))
    }

    @Test
    fun `duplicate addPendingDelete is a no-op under CONFLICT_IGNORE`() {
        dao.addPendingDelete("m1", userA)
        dao.addPendingDelete("m1", userA)

        assertEquals(setOf("m1"), dao.getPendingDeletes(userA))
    }

    @Test
    fun `removePendingDelete for an existing row clears it`() {
        dao.addPendingDelete("m1", userA)

        assertTrue(dao.removePendingDelete("m1", userA))
        assertTrue(dao.getPendingDeletes(userA).isEmpty())
    }

    @Test
    fun `removePendingDelete for a missing row is still truthy`() {
        assertTrue(dao.removePendingDelete("missing", userA))
    }

    @Test
    fun `pending deletes are isolated per user`() {
        dao.addPendingDelete("m1", userA)

        assertEquals(setOf("m1"), dao.getPendingDeletes(userA))
        assertTrue(dao.getPendingDeletes(userB).isEmpty())
    }

    @Test
    fun `addPendingRead and getPendingReads use the reads table`() {
        dao.addPendingDelete("d1", userA)
        dao.addPendingRead("r1", userA)

        assertEquals(setOf("d1"), dao.getPendingDeletes(userA))
        assertEquals(setOf("r1"), dao.getPendingReads(userA))
    }

    @Test
    fun `addPendingDeletes batch inserts every id atomically`() {
        assertTrue(dao.addPendingDeletes(listOf("m1", "m2", "m3"), userA))

        assertEquals(setOf("m1", "m2", "m3"), dao.getPendingDeletes(userA))
    }

    @Test
    fun `addPendingDeletes with an empty list is a no-op and returns true`() {
        assertTrue(dao.addPendingDeletes(emptyList(), userA))

        assertTrue(dao.getPendingDeletes(userA).isEmpty())
    }

    @Test
    fun `removePendingDeletes deletes only the supplied ids for the supplied user`() {
        dao.addPendingDeletes(listOf("m1", "m2", "m3"), userA)
        dao.addPendingDelete("m2", userB)

        dao.removePendingDeletes(listOf("m1", "m2"), userA)

        assertEquals(setOf("m3"), dao.getPendingDeletes(userA))
        assertEquals(setOf("m2"), dao.getPendingDeletes(userB))
    }

    @Test
    fun `batch methods for reads are symmetric to deletes`() {
        dao.addPendingReads(listOf("r1", "r2"), userA)
        dao.removePendingReads(listOf("r1"), userA)

        assertEquals(setOf("r2"), dao.getPendingReads(userA))
    }
}
