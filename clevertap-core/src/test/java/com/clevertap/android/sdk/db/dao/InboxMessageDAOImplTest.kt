package com.clevertap.android.sdk.db.dao

import TestCryptHandler
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.sdk.inbox.InboxIndexState
import com.clevertap.android.sdk.inbox.InboxMessageSource
import com.clevertap.android.shared.test.BaseTestCase
import android.content.ContentValues
import io.mockk.mockk
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class InboxMessageDAOImplTest : BaseTestCase() {

    private lateinit var inboxMessageDAO: InboxMessageDAO
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var dbEncryptionHandler: DBEncryptionHandler
    private lateinit var dbHelper: DatabaseHelper

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbEncryptionHandler = DBEncryptionHandler(TestCryptHandler(), TestLogger(), EncryptionLevel.NONE)
        dbHelper = DatabaseHelper(
            context = appCtx,
            accountId = instanceConfig.accountId,
            dbName = "test_db",
            logger = instanceConfig.logger
        )
        inboxMessageDAO = InboxMessageDAOImpl(
            dbHelper = dbHelper,
            logger = instanceConfig.logger,
            dbEncryptionHandler = dbEncryptionHandler
        )
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun test_deleteMessage_when_MessageIDAndUserIDIsPassed_should_DeleteMessageIfExists() {
        val msgId = "msg_1234"
        val userID = "user_11"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))
        var msgList = inboxMessageDAO.getMessages(userID)
        assertEquals(1, msgList.size)

        val result = inboxMessageDAO.deleteMessage(msgId, userID)
        assertEquals(true, result)
        msgList = inboxMessageDAO.getMessages(userID)
        assertEquals(0, msgList.size)
    }

    @Test
    fun test_deleteMessages_withEmptyIds() {
        val userId = "user_11"
        val msgIds = emptyList<String>()

        val op = inboxMessageDAO.deleteMessages(messageIds = msgIds, userId = userId)
        assertTrue(op)
    }

    @Test
    fun test_deleteMessages_when_MessageIDAndUserIdIsPassed_should_DeleteMessagesIfExists() {
        val userId = "user_11"
        val msgIds = listOf("msg_1", "msg_2", "msg_3")

        // When all msgIds are present in the db all should be deleted
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, false),
                getCtMsgDao(msgIds[1], userId, false),
                getCtMsgDao(msgIds[2], userId, false)
            )
        )
        var msgList = inboxMessageDAO.getMessages(userId)
        assertEquals(3, msgList.size)

        val result = inboxMessageDAO.deleteMessages(msgIds, userId)
        assertTrue(result)
        msgList = inboxMessageDAO.getMessages(userId)
        assertEquals(0, msgList.size)

        // When some msgId from msgIds is not found, it is skipped and remaining are deleted
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, false),
                getCtMsgDao(msgIds[1], userId, false),
                getCtMsgDao(msgIds[2], userId, false)
            )
        )
        msgList = inboxMessageDAO.getMessages(userId)
        assertEquals(3, msgList.size)

        val updatedMsgIds = msgIds.toMutableList().apply {
            removeAt(lastIndex)
            add("msg_4")
        }
        val result2 = inboxMessageDAO.deleteMessages(updatedMsgIds, userId)
        msgList = inboxMessageDAO.getMessages(userId)
        assertTrue(result2)
        assertEquals(1, msgList.size)
    }

    @Test
    fun test_getMessages_when_FunctionIsCalledWithCorrectUserID_should_ReturnAllAssociatedMessages() {
        // Case: incorrect user id
        var msgList = inboxMessageDAO.getMessages("unavailableUser")
        assertEquals(0, msgList.size)

        // Case: correct user id
        val msgId = "msg_1234"
        val userID = "user_11"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        msgList = inboxMessageDAO.getMessages(userID)
        assertEquals(1, msgList.size)
        assertEquals(msgId, msgList[0].id)
        assertEquals(userID, msgList[0].userId)
    }

    @Test
    fun test_markMessageAsRead_when_CorrectUserIdAndMessageIsPassed_should_SetMessageIdAsRead() {
        val msgId = "msg_1234"
        var userID = "user_11"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        inboxMessageDAO.markMessageAsRead(msgId, userID)
        var msg = inboxMessageDAO.getMessages(userID)[0]
        assertTrue(msg.isRead == 1)

        userID = "user_12"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        inboxMessageDAO.markMessageAsRead("msgId", userID) // Wrong message ID
        msg = inboxMessageDAO.getMessages(userID)[0]
        assertFalse(msg.isRead == 1)
    }

    @Test
    fun test_markMessagesAsRead_when_CorrectUserIdAndMessagesArePassed_should_SetMessageIdsAsRead() {
        val userId = "user_11"
        val msgIds = listOf("msg_1", "msg_2", "msg_3")

        // When all msgIds are present in the db all should be marked as read
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, read = false),
                getCtMsgDao(msgIds[1], userId, read = false),
                getCtMsgDao(msgIds[2], userId, read = false)
            )
        )
        val result = inboxMessageDAO.markMessagesAsRead(msgIds, userId)
        var msgList = inboxMessageDAO.getMessages(userId)
        assertTrue(result)
        for (msg in msgList)
            assertTrue(msg.isRead == 1)

        // When some msgId from msgIds is not found, it is skipped and remaining are marked as read
        val userId2 = "user_12"
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId2, read = false),
                getCtMsgDao(msgIds[1], userId2, read = false),
                getCtMsgDao(msgIds[2], userId2, read = false)
            )
        )
        val updatedMsgIds = msgIds.toMutableList().apply {
            removeAt(lastIndex)
            add("msg_4")
        }
        val result2 = inboxMessageDAO.markMessagesAsRead(updatedMsgIds, userId2)
        msgList = inboxMessageDAO.getMessages(userId2)
        assertTrue(result2)
        for (msg in msgList) {
            if (msg.id in updatedMsgIds)
                assertTrue(msg.isRead == 1)
            else
                assertFalse(msg.isRead == 1)
        }
    }

    @Test
    fun test_upsertMessages_when_Called_should_InsertORUpsertAMessage() {
        // When a message is not present it will insert the message
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = false, campaignId = "cp1234")))
        assertNotNull(inboxMessageDAO.getMessages("user_11")[0])

        // When a message is present it will update the message
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = true, campaignId = "cp4321")))
        val messages = inboxMessageDAO.getMessages("user_11")
        assertEquals(1, messages.size)
        assertEquals(true, messages[0].isRead == 1)
        assertEquals("cp4321", messages[0].campaignId)
    }

    @Test
    fun `upsert and read preserves V2 source`() {
        val dao = getCtMsgDao("m1", "user_11", source = InboxMessageSource.V2)
        inboxMessageDAO.upsertMessages(listOf(dao))
        val loaded = inboxMessageDAO.getMessages("user_11").single()
        assertEquals(InboxMessageSource.V2, loaded.source)
    }

    @Test
    fun `upsert and read preserves V1 source`() {
        val dao = getCtMsgDao("m1", "user_11", source = InboxMessageSource.V1)
        inboxMessageDAO.upsertMessages(listOf(dao))
        val loaded = inboxMessageDAO.getMessages("user_11").single()
        assertEquals(InboxMessageSource.V1, loaded.source)
    }

    @Test
    fun `upsert and read defaults indexState to PENDING_INDEXING`() {
        val dao = getCtMsgDao("m1", "user_11", source = InboxMessageSource.V2)
        inboxMessageDAO.upsertMessages(listOf(dao))
        val loaded = inboxMessageDAO.getMessages("user_11").single()
        assertEquals(InboxIndexState.PENDING_INDEXING, loaded.indexState)
    }

    @Test
    fun `upsert preserves indexState on UPDATE — never downgrades INDEXED`() {
        // Insert a row, flip it to INDEXED via the dedicated DAO call,
        // then upsert the same id again with a DAO carrying the default
        // (PENDING_INDEXING). The existing INDEXED state must survive.
        val userId = "user_11"
        val dao = getCtMsgDao("m1", userId, source = InboxMessageSource.V2)
        inboxMessageDAO.upsertMessages(listOf(dao))
        inboxMessageDAO.markIndexed(listOf("m1"), userId)
        assertEquals(InboxIndexState.INDEXED, inboxMessageDAO.getMessages(userId).single().indexState)

        // Re-upsert with the default indexState — must NOT downgrade.
        val redelivery = getCtMsgDao("m1", userId, source = InboxMessageSource.V2)
        assertEquals(InboxIndexState.PENDING_INDEXING, redelivery.indexState)
        inboxMessageDAO.upsertMessages(listOf(redelivery))
        assertEquals(InboxIndexState.INDEXED, inboxMessageDAO.getMessages(userId).single().indexState)
    }

    @Test
    fun `markIndexed flips supplied PENDING_INDEXING rows to INDEXED`() {
        val userId = "user_11"
        inboxMessageDAO.upsertMessages(
            listOf(
                getCtMsgDao("m1", userId, source = InboxMessageSource.V2),
                getCtMsgDao("m2", userId, source = InboxMessageSource.V2),
                getCtMsgDao("m3", userId, source = InboxMessageSource.V2)
            )
        )

        val ok = inboxMessageDAO.markIndexed(listOf("m1", "m3"), userId)
        assertTrue(ok)

        val byId = inboxMessageDAO.getMessages(userId).associateBy { it.id }
        assertEquals(InboxIndexState.INDEXED, byId.getValue("m1").indexState)
        assertEquals(InboxIndexState.PENDING_INDEXING, byId.getValue("m2").indexState)
        assertEquals(InboxIndexState.INDEXED, byId.getValue("m3").indexState)
    }

    @Test
    fun `markIndexed with empty ids is a no-op and returns true`() {
        val userId = "user_11"
        inboxMessageDAO.upsertMessages(
            listOf(getCtMsgDao("m1", userId, source = InboxMessageSource.V2))
        )

        val ok = inboxMessageDAO.markIndexed(emptyList(), userId)
        assertTrue(ok)
        assertEquals(
            InboxIndexState.PENDING_INDEXING,
            inboxMessageDAO.getMessages(userId).single().indexState
        )
    }

    @Test
    fun `markIndexed is scoped to the supplied userId`() {
        inboxMessageDAO.upsertMessages(
            listOf(
                getCtMsgDao("m1", "user_11", source = InboxMessageSource.V2),
                getCtMsgDao("m1", "user_12", source = InboxMessageSource.V2)
            )
        )

        inboxMessageDAO.markIndexed(listOf("m1"), "user_11")

        assertEquals(
            InboxIndexState.INDEXED,
            inboxMessageDAO.getMessages("user_11").single().indexState
        )
        assertEquals(
            InboxIndexState.PENDING_INDEXING,
            inboxMessageDAO.getMessages("user_12").single().indexState
        )
    }

    @Test
    fun `unrecognised indexState value falls back to PENDING_INDEXING`() {
        // Insert a row directly with an invalid index_state value to simulate
        // schema skew or a future enum constant the current SDK doesn't know.
        val cv = ContentValues().apply {
            put(Column.ID, "m1")
            put(Column.DATA, dbEncryptionHandler.wrapDbData(JSONObject().toString()))
            put(Column.WZRKPARAMS, JSONObject().toString())
            put(Column.CAMPAIGN, "cp1")
            put(Column.TAGS, "")
            put(Column.IS_READ, 0)
            put(Column.EXPIRES, Long.MAX_VALUE)
            put(Column.CREATED_AT, 1L)
            put(Column.USER_ID, "user_11")
            put(Column.SOURCE, "V2")
            put(Column.INDEX_STATE, "ALIEN")
        }
        dbHelper.writableDatabase.insert(Table.INBOX_MESSAGES.tableName, null, cv)

        val loaded = inboxMessageDAO.getMessages("user_11").single()
        assertEquals(InboxIndexState.PENDING_INDEXING, loaded.indexState)
    }

    @Test
    fun `unrecognised source value falls back to V1`() {
        // Insert a row directly via SQL using an invalid source value so we
        // simulate either a schema-skew or a future enum constant the current
        // SDK doesn't know about. Read should not throw; loaded source is V1.
        val cv = ContentValues().apply {
            put(Column.ID, "m1")
            put(Column.DATA, dbEncryptionHandler.wrapDbData(JSONObject().toString()))
            put(Column.WZRKPARAMS, JSONObject().toString())
            put(Column.CAMPAIGN, "cp1")
            put(Column.TAGS, "")
            put(Column.IS_READ, 0)
            put(Column.EXPIRES, Long.MAX_VALUE)
            put(Column.CREATED_AT, 1L)
            put(Column.USER_ID, "user_11")
            put(Column.SOURCE, "ALIEN")
        }
        dbHelper.writableDatabase.insert(Table.INBOX_MESSAGES.tableName, null, cv)

        val loaded = inboxMessageDAO.getMessages("user_11").single()
        assertEquals(InboxMessageSource.V1, loaded.source)
    }

    private fun getCtMsgDao(
        id: String = "1",
        userId: String = "1",
        read: Boolean = false,
        jsonData: JSONObject = JSONObject(),
        date: Long = System.currentTimeMillis(),
        expires: Long = (System.currentTimeMillis() * 10),
        tags: List<String> = listOf(),
        campaignId: String = "campaignID",
        wzrkParams: JSONObject = JSONObject(),
        source: InboxMessageSource = InboxMessageSource.V1
    ): CTMessageDAO {
        return CTMessageDAO().also {
            it.id = id
            it.jsonData = jsonData
            it.isRead = if (read) 1 else 0
            it.date = date
            it.expires = expires
            it.userId = userId
            it.tags = tags.joinToString(",")
            it.campaignId = campaignId
            it.wzrkParams = wzrkParams
            it.source = source
        }
    }

    // ── findSweepableV2Ids ───────────────────────────────────────────────────

    @Test
    fun `findSweepableV2Ids returns empty when table has no V2 rows`() {
        inboxMessageDAO.upsertMessages(
            listOf(getCtMsgDao("m1", "user_11", source = InboxMessageSource.V1))
        )
        val result = inboxMessageDAO.findSweepableV2Ids("user_11", Long.MAX_VALUE)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findSweepableV2Ids returns INDEXED V2 ids regardless of created_at`() {
        val userId = "user_11"
        // Insert as PENDING_INDEXING, then flip to INDEXED
        inboxMessageDAO.upsertMessages(
            listOf(getCtMsgDao("m1", userId, source = InboxMessageSource.V2))
        )
        inboxMessageDAO.markIndexed(listOf("m1"), userId)

        // cutoff far in the future — INDEXED rows are always eligible
        val result = inboxMessageDAO.findSweepableV2Ids(userId, Long.MAX_VALUE)
        assertEquals(setOf("m1"), result)
    }

    @Test
    fun `findSweepableV2Ids returns stale PENDING_INDEXING V2 ids (created_at less than cutoff)`() {
        val userId = "user_11"
        // Use a fixed old timestamp so the row is definitely before any cutoff
        val oldDate = 1_000L // epoch seconds
        inboxMessageDAO.upsertMessages(
            listOf(getCtMsgDao("m1", userId, date = oldDate, source = InboxMessageSource.V2))
        )

        // cutoff = oldDate + 1 → row qualifies as stale
        val result = inboxMessageDAO.findSweepableV2Ids(userId, oldDate + 1)
        assertEquals(setOf("m1"), result)
    }

    @Test
    fun `findSweepableV2Ids excludes fresh PENDING_INDEXING V2 ids (created_at not less than cutoff)`() {
        val userId = "user_11"
        val recentDate = 9_000_000L
        inboxMessageDAO.upsertMessages(
            listOf(getCtMsgDao("m1", userId, date = recentDate, source = InboxMessageSource.V2))
        )

        // cutoff = recentDate → NOT less than cutoff, so row must be excluded
        val result = inboxMessageDAO.findSweepableV2Ids(userId, recentDate)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findSweepableV2Ids excludes V1 messages`() {
        val userId = "user_11"
        inboxMessageDAO.upsertMessages(
            listOf(getCtMsgDao("m_v1", userId, source = InboxMessageSource.V1))
        )
        // Even if we somehow flip it to INDEXED the source filter keeps it out
        inboxMessageDAO.markIndexed(listOf("m_v1"), userId)

        val result = inboxMessageDAO.findSweepableV2Ids(userId, Long.MAX_VALUE)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findSweepableV2Ids is scoped to the supplied userId`() {
        inboxMessageDAO.upsertMessages(
            listOf(
                getCtMsgDao("m1", "user_11", source = InboxMessageSource.V2),
                getCtMsgDao("m2", "user_12", source = InboxMessageSource.V2)
            )
        )
        inboxMessageDAO.markIndexed(listOf("m1"), "user_11")
        inboxMessageDAO.markIndexed(listOf("m2"), "user_12")

        assertEquals(setOf("m1"), inboxMessageDAO.findSweepableV2Ids("user_11", Long.MAX_VALUE))
        assertEquals(setOf("m2"), inboxMessageDAO.findSweepableV2Ids("user_12", Long.MAX_VALUE))
    }

    @Test
    fun `findSweepableV2Ids returns both INDEXED and stale PENDING_INDEXING together`() {
        val userId = "user_11"
        val oldDate = 100L

        inboxMessageDAO.upsertMessages(
            listOf(
                getCtMsgDao("indexed", userId, source = InboxMessageSource.V2),
                getCtMsgDao("stale_pending", userId, date = oldDate, source = InboxMessageSource.V2),
                getCtMsgDao("fresh_pending", userId, date = 9_000_000L, source = InboxMessageSource.V2)
            )
        )
        inboxMessageDAO.markIndexed(listOf("indexed"), userId)
        // stale_pending and fresh_pending remain PENDING_INDEXING

        val result = inboxMessageDAO.findSweepableV2Ids(userId, staleCutoffSeconds = oldDate + 1)
        assertEquals(setOf("indexed", "stale_pending"), result)
        assertFalse("fresh_pending" in result)
    }
}
