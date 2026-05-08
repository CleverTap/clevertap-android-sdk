package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InboxPendingActionsDAOImplTest : BaseTestCase() {

    private lateinit var dao: InboxPendingActionsDAO
    private lateinit var dbHelper: DatabaseHelper

    private val userA = "user-a"
    private val userB = "user-b"
    private val futureTtl = 1_000_000L

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
    fun `addPendingDelete stores the id and getPendingDeleteIds returns it`() {
        assertTrue(dao.addPendingDelete("m1", userA, null, futureTtl))

        assertEquals(setOf("m1"), dao.getPendingDeleteIds(userA))
    }

    @Test
    fun `addPendingDelete persists wzrkParams JSON and expiresAt`() {
        val params = JSONObject().put("wzrk_id", "camp-1").put("wzrk_pivot", "default")
        assertTrue(dao.addPendingDelete("m1", userA, params, futureTtl))

        val rows = dao.getPendingDeletes(userA)
        assertEquals(1, rows.size)
        assertEquals("m1", rows[0].messageId)
        assertEquals(futureTtl, rows[0].expiresAt)
        val stored = assertNotNull(rows[0].wzrkParams)
        assertEquals("camp-1", stored.getString("wzrk_id"))
        assertEquals("default", stored.getString("wzrk_pivot"))
    }

    @Test
    fun `addPendingDelete tolerates null wzrkParams`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)

        val rows = dao.getPendingDeletes(userA)
        assertEquals(1, rows.size)
        assertNull(rows[0].wzrkParams)
    }

    @Test
    fun `duplicate addPendingDelete is a no-op under CONFLICT_IGNORE`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)
        dao.addPendingDelete("m1", userA, JSONObject().put("wzrk_id", "different"), futureTtl)

        assertEquals(setOf("m1"), dao.getPendingDeleteIds(userA))
    }

    @Test
    fun `removePendingDelete for an existing row clears it`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)

        assertTrue(dao.removePendingDelete("m1", userA))
        assertTrue(dao.getPendingDeleteIds(userA).isEmpty())
    }

    @Test
    fun `removePendingDelete for a missing row is still truthy`() {
        assertTrue(dao.removePendingDelete("missing", userA))
    }

    @Test
    fun `pending deletes are isolated per user`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)

        assertEquals(setOf("m1"), dao.getPendingDeleteIds(userA))
        assertTrue(dao.getPendingDeleteIds(userB).isEmpty())
    }

    @Test
    fun `addPendingRead and getPendingReads use the reads table`() {
        dao.addPendingDelete("d1", userA, null, futureTtl)
        dao.addPendingRead("r1", userA, futureTtl)

        assertEquals(setOf("d1"), dao.getPendingDeleteIds(userA))
        assertEquals(setOf("r1"), dao.getPendingReads(userA))
    }

    @Test
    fun `addPendingDeletes batch inserts every row atomically with wzrkParams and expiresAt`() {
        val rows = listOf(
            PendingDelete("m1", JSONObject().put("wzrk_id", "c1"), futureTtl),
            PendingDelete("m2", JSONObject().put("wzrk_id", "c2"), futureTtl + 5L),
            PendingDelete("m3", null, futureTtl + 10L)
        )
        assertTrue(dao.addPendingDeletes(rows, userA))

        val out = dao.getPendingDeletes(userA).associateBy { it.messageId }
        assertEquals(setOf("m1", "m2", "m3"), out.keys)
        assertEquals("c1", out["m1"]?.wzrkParams?.getString("wzrk_id"))
        assertEquals(futureTtl, out["m1"]?.expiresAt)
        assertEquals("c2", out["m2"]?.wzrkParams?.getString("wzrk_id"))
        assertEquals(futureTtl + 5L, out["m2"]?.expiresAt)
        assertNull(out["m3"]?.wzrkParams)
        assertEquals(futureTtl + 10L, out["m3"]?.expiresAt)
    }

    @Test
    fun `addPendingDeletes with an empty list is a no-op and returns true`() {
        assertTrue(dao.addPendingDeletes(emptyList(), userA))

        assertTrue(dao.getPendingDeleteIds(userA).isEmpty())
    }

    @Test
    fun `removePendingDeletes deletes only the supplied ids for the supplied user`() {
        dao.addPendingDeletes(
            listOf(
                PendingDelete("m1", null, futureTtl),
                PendingDelete("m2", null, futureTtl),
                PendingDelete("m3", null, futureTtl)
            ),
            userA
        )
        dao.addPendingDelete("m2", userB, null, futureTtl)

        dao.removePendingDeletes(listOf("m1", "m2"), userA)

        assertEquals(setOf("m3"), dao.getPendingDeleteIds(userA))
        assertEquals(setOf("m2"), dao.getPendingDeleteIds(userB))
    }

    @Test
    fun `new pending row defaults to PENDING_SEND so getPendingDeletes returns it`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)

        assertEquals(listOf("m1"), dao.getPendingDeletes(userA).map { it.messageId })
    }

    @Test
    fun `markPendingDeletesAwaitingConfirm flips state for matching rows only`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)
        dao.addPendingDelete("m2", userA, null, futureTtl)

        assertTrue(dao.markPendingDeletesAwaitingConfirm(listOf("m1"), userA))

        // both still in id set (merger filter unchanged)
        assertEquals(setOf("m1", "m2"), dao.getPendingDeleteIds(userA))
        // only m2 still PENDING_SEND, so retry list excludes m1
        assertEquals(listOf("m2"), dao.getPendingDeletes(userA).map { it.messageId })
    }

    @Test
    fun `markPendingDeletesAwaitingConfirm scoped by userId`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)
        dao.addPendingDelete("m1", userB, null, futureTtl)

        dao.markPendingDeletesAwaitingConfirm(listOf("m1"), userA)

        assertTrue(dao.getPendingDeletes(userA).isEmpty())
        assertEquals(listOf("m1"), dao.getPendingDeletes(userB).map { it.messageId })
    }

    @Test
    fun `markPendingDeletesAwaitingConfirm with empty list is a no-op`() {
        dao.addPendingDelete("m1", userA, null, futureTtl)

        assertTrue(dao.markPendingDeletesAwaitingConfirm(emptyList(), userA))
        assertEquals(listOf("m1"), dao.getPendingDeletes(userA).map { it.messageId })
    }

    @Test
    fun `removeExpiredAwaitingConfirm drops only AWAITING_CONFIRM rows whose expires is past now`() {
        dao.addPendingDelete("send_past", userA, null, 100L)
        dao.addPendingDelete("await_past", userA, null, 100L)
        dao.addPendingDelete("await_future", userA, null, 9_999L)
        dao.markPendingDeletesAwaitingConfirm(listOf("await_past", "await_future"), userA)

        val removed = dao.removeExpiredAwaitingConfirm(userA, nowSeconds = 1_000L)

        assertEquals(1, removed)
        assertEquals(setOf("send_past", "await_future"), dao.getPendingDeleteIds(userA))
    }

    @Test
    fun `removeExpiredAwaitingConfirm scoped by userId`() {
        dao.addPendingDelete("m1", userA, null, 100L)
        dao.addPendingDelete("m1", userB, null, 100L)
        dao.markPendingDeletesAwaitingConfirm(listOf("m1"), userA)
        dao.markPendingDeletesAwaitingConfirm(listOf("m1"), userB)

        dao.removeExpiredAwaitingConfirm(userA, nowSeconds = 1_000L)

        assertTrue(dao.getPendingDeleteIds(userA).isEmpty())
        assertEquals(setOf("m1"), dao.getPendingDeleteIds(userB))
    }

    @Test
    fun `batch methods for reads are symmetric to deletes`() {
        dao.addPendingReads(
            listOf(PendingRead("r1", futureTtl), PendingRead("r2", futureTtl)),
            userA
        )
        dao.removePendingReads(listOf("r1"), userA)

        assertEquals(setOf("r2"), dao.getPendingReads(userA))
    }

    @Test
    fun `addPendingRead persists expiresAt in the reads table`() {
        dao.addPendingRead("r1", userA, 1_700_000_000L)

        // expiresAt is asserted via the sweep; row remains when nowSeconds is below.
        val notRemoved = dao.removeExpiredPendingReads(userA, nowSeconds = 1_500_000_000L)
        assertEquals(0, notRemoved)
        assertEquals(setOf("r1"), dao.getPendingReads(userA))

        val removed = dao.removeExpiredPendingReads(userA, nowSeconds = 1_900_000_000L)
        assertEquals(1, removed)
        assertTrue(dao.getPendingReads(userA).isEmpty())
    }

    @Test
    fun `removeExpiredPendingReads drops only rows whose expires is past now`() {
        dao.addPendingRead("past", userA, 100L)
        dao.addPendingRead("future", userA, 9_999L)

        val removed = dao.removeExpiredPendingReads(userA, nowSeconds = 1_000L)

        assertEquals(1, removed)
        assertEquals(setOf("future"), dao.getPendingReads(userA))
    }

    @Test
    fun `removeExpiredPendingReads scoped by userId`() {
        dao.addPendingRead("r1", userA, 100L)
        dao.addPendingRead("r1", userB, 100L)

        dao.removeExpiredPendingReads(userA, nowSeconds = 1_000L)

        assertTrue(dao.getPendingReads(userA).isEmpty())
        assertEquals(setOf("r1"), dao.getPendingReads(userB))
    }
}
