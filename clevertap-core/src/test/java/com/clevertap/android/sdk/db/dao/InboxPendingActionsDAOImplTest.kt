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
        assertTrue(dao.addPendingDelete("m1", userA, null))

        assertEquals(setOf("m1"), dao.getPendingDeleteIds(userA))
    }

    @Test
    fun `addPendingDelete persists wzrkParams JSON`() {
        val params = JSONObject().put("wzrk_id", "camp-1").put("wzrk_pivot", "default")
        assertTrue(dao.addPendingDelete("m1", userA, params))

        val rows = dao.getPendingDeletes(userA)
        assertEquals(1, rows.size)
        assertEquals("m1", rows[0].messageId)
        val stored = assertNotNull(rows[0].wzrkParams)
        assertEquals("camp-1", stored.getString("wzrk_id"))
        assertEquals("default", stored.getString("wzrk_pivot"))
    }

    @Test
    fun `addPendingDelete tolerates null wzrkParams`() {
        dao.addPendingDelete("m1", userA, null)

        val rows = dao.getPendingDeletes(userA)
        assertEquals(1, rows.size)
        assertNull(rows[0].wzrkParams)
    }

    @Test
    fun `duplicate addPendingDelete is a no-op under CONFLICT_IGNORE`() {
        dao.addPendingDelete("m1", userA, null)
        dao.addPendingDelete("m1", userA, JSONObject().put("wzrk_id", "different"))

        assertEquals(setOf("m1"), dao.getPendingDeleteIds(userA))
    }

    @Test
    fun `removePendingDelete for an existing row clears it`() {
        dao.addPendingDelete("m1", userA, null)

        assertTrue(dao.removePendingDelete("m1", userA))
        assertTrue(dao.getPendingDeleteIds(userA).isEmpty())
    }

    @Test
    fun `removePendingDelete for a missing row is still truthy`() {
        assertTrue(dao.removePendingDelete("missing", userA))
    }

    @Test
    fun `pending deletes are isolated per user`() {
        dao.addPendingDelete("m1", userA, null)

        assertEquals(setOf("m1"), dao.getPendingDeleteIds(userA))
        assertTrue(dao.getPendingDeleteIds(userB).isEmpty())
    }

    @Test
    fun `addPendingRead and getPendingReads use the reads table`() {
        dao.addPendingDelete("d1", userA, null)
        dao.addPendingRead("r1", userA)

        assertEquals(setOf("d1"), dao.getPendingDeleteIds(userA))
        assertEquals(setOf("r1"), dao.getPendingReads(userA))
    }

    @Test
    fun `addPendingDeletes batch inserts every row atomically with wzrkParams`() {
        val rows = listOf(
            PendingDelete("m1", JSONObject().put("wzrk_id", "c1")),
            PendingDelete("m2", JSONObject().put("wzrk_id", "c2")),
            PendingDelete("m3", null)
        )
        assertTrue(dao.addPendingDeletes(rows, userA))

        val out = dao.getPendingDeletes(userA).associateBy { it.messageId }
        assertEquals(setOf("m1", "m2", "m3"), out.keys)
        assertEquals("c1", out["m1"]?.wzrkParams?.getString("wzrk_id"))
        assertEquals("c2", out["m2"]?.wzrkParams?.getString("wzrk_id"))
        assertNull(out["m3"]?.wzrkParams)
    }

    @Test
    fun `addPendingDeletes with an empty list is a no-op and returns true`() {
        assertTrue(dao.addPendingDeletes(emptyList(), userA))

        assertTrue(dao.getPendingDeleteIds(userA).isEmpty())
    }

    @Test
    fun `removePendingDeletes deletes only the supplied ids for the supplied user`() {
        dao.addPendingDeletes(
            listOf(PendingDelete("m1", null), PendingDelete("m2", null), PendingDelete("m3", null)),
            userA
        )
        dao.addPendingDelete("m2", userB, null)

        dao.removePendingDeletes(listOf("m1", "m2"), userA)

        assertEquals(setOf("m3"), dao.getPendingDeleteIds(userA))
        assertEquals(setOf("m2"), dao.getPendingDeleteIds(userB))
    }

    @Test
    fun `batch methods for reads are symmetric to deletes`() {
        dao.addPendingReads(listOf("r1", "r2"), userA)
        dao.removePendingReads(listOf("r1"), userA)

        assertEquals(setOf("r2"), dao.getPendingReads(userA))
    }
}
