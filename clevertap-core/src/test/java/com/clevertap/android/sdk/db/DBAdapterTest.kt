package com.clevertap.android.sdk.db

import TestCryptHandler
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.db.dao.PendingDelete
import com.clevertap.android.sdk.db.dao.PendingRead
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.sdk.inbox.InboxIndexState
import com.clevertap.android.sdk.inbox.InboxMessageSource
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.Test
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class DBAdapterTest : BaseTestCase() {

    private lateinit var dbAdapter: DBAdapter
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var dbEncryptionHandler: DBEncryptionHandler

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbEncryptionHandler = DBEncryptionHandler(TestCryptHandler(), TestLogger(), EncryptionLevel.NONE)
        dbAdapter = DBAdapter(
            context = appCtx,
            databaseName = DBAdapter.getDatabaseName(instanceConfig),
            accountId = instanceConfig.accountId,
            logger = instanceConfig.logger,
            dbEncryptionHandler = dbEncryptionHandler
        )
    }

    @After
    fun deleteDB() {
        dbAdapter.deleteDB()
    }

    // =====================================================
    // INTEGRATION TESTS - Testing DBAdapter delegation
    // =====================================================

    @Test
    fun test_dbAdapter_delegation_works_correctly() {
        // Test that DBAdapter correctly delegates to DAOs
        // This is an integration test to ensure the refactoring didn't break existing functionality

        // Test user profile operations
        val result = dbAdapter.storeUserProfile("accountID", "deviceID", JSONObject().apply {
            put("name", "john")
        })
        assertTrue(result > 0)

        val profile = dbAdapter.fetchUserProfileByAccountIdAndDeviceID("accountID", "deviceID")
        assertNotNull(profile)
        assertEquals("john", profile.getString("name"))

        // Test inbox message operations
        val msgId = "msg_1234"
        val userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))

        val messages = dbAdapter.getMessages(userID)
        assertEquals(1, messages.size)
        assertEquals(msgId, messages[0].id)

        // Test push notification operations
        dbAdapter.storePushNotificationId("pushNotif", 0)
        assertTrue(dbAdapter.doesPushNotificationIdExist("pushNotif"))

        // Test event operations
        val eventResult = dbAdapter.storeObject(JSONObject().apply { put("event", "test") }, Table.EVENTS)
        assertTrue(eventResult > 0)

        val events = dbAdapter.fetchEvents(Table.EVENTS, 10)
        assertNotNull(events)
        assertFalse(events.isEmpty)

        // Test uninstall timestamp operations
        assertEquals(0, dbAdapter.getLastUninstallTimestamp())
        dbAdapter.storeUninstallTimestamp()
        assertTrue(dbAdapter.getLastUninstallTimestamp() > 0)
    }

    @Test
    fun test_userEventLogDAO_returns_singleton_instance() {
        // Test that UserEventLogDAO singleton pattern still works
        val dao1 = dbAdapter.userEventLogDAO()
        val dao2 = dbAdapter.userEventLogDAO()

        assertNotNull(dao1)
        assertSame(dao1, dao2) // Verify same instance is returned
    }

    @Test
    fun test_null_parameter_handling() {
        // Test that null parameter handling still works correctly after refactoring

        // User profile operations with null parameters
        assertEquals(-1L, dbAdapter.storeUserProfile(null, "deviceID", JSONObject()))
        assertEquals(-1L, dbAdapter.storeUserProfile("accountID", null, JSONObject()))
        assertNull(dbAdapter.fetchUserProfileByAccountIdAndDeviceID(null, "deviceID"))
        assertNull(dbAdapter.fetchUserProfileByAccountIdAndDeviceID("accountID", null))
        assertEquals(emptyMap<String, JSONObject>(), dbAdapter.fetchUserProfilesByAccountId(null))

        // Inbox message operations with null parameters
        assertFalse(dbAdapter.deleteMessageForId(null, "userId"))
        assertFalse(dbAdapter.deleteMessageForId("msgId", null))
        assertFalse(dbAdapter.deleteMessagesForIDs(null, "userId"))
        assertFalse(dbAdapter.deleteMessagesForIDs(listOf("msgId"), null))
        assertFalse(dbAdapter.markReadMessageForId(null, "userId"))
        assertFalse(dbAdapter.markReadMessageForId("msgId", null))
        assertFalse(dbAdapter.markReadMessagesForIds(null, "userId"))
        assertFalse(dbAdapter.markReadMessagesForIds(listOf("msgId"), null))
    }

    @Test
    fun test_markReadMessageForId_when_CorrectUserIdAndMessageIsPassed_should_SetMessageIdAsRead() {
        val msgId = "msg_1234"
        var userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        dbAdapter.markReadMessageForId(msgId, userID)
        var msg = dbAdapter.getMessages(userID)[0]
        assertTrue(msg.isRead == 1)

        userID = "user_12"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        dbAdapter.markReadMessageForId("msgId", userID)
        msg = dbAdapter.getMessages(userID)[0]
        assertFalse(msg.isRead == 1)
    }

    @Test
    fun test_markReadMessagesForIds_when_CorrectUserIdAndMessagesArePassed_should_SetMessageIdsAsRead() {
        // case 1:when msdIds and userId is null, false should be returned
        var result: Boolean = dbAdapter.markReadMessagesForIds(messageIDs = null, userId = null)
        assertFalse(result)

        var userId = "user_11"
        //case 2: when msgIds is null and userId is non-null, false should be returned
        result = dbAdapter.markReadMessagesForIds(messageIDs = null, userId)
        assertFalse(result)

        val msgIds = mutableListOf<String>()
        //case 3: when msgIds is non-null and userId is null, false should be returned
        result = dbAdapter.markReadMessagesForIds(msgIds, userId = null)
        assertFalse(result)

        userId = "user_11"
        msgIds.add("msg_1")
        msgIds.add("msg_2")
        msgIds.add("msg_3")

        //case 4: when msgIds and userId is non-null, sqlite query is run and true is returned

        //When all msgIds are present in the db all should be marked as read
        dbAdapter.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, read = false),
                getCtMsgDao(msgIds[1], userId, read = false),
                getCtMsgDao(msgIds[2], userId, read = false)
            )
        )
        result = dbAdapter.markReadMessagesForIds(msgIds, userId)
        var msgList = dbAdapter.getMessages(userId)
        assertTrue(result)
        for (msg in msgList)
            assertTrue(msg.isRead == 1)

        // When some msgId from msgIds is not found, it is skipped and remaining are marked as read
        userId = "user_12"
        dbAdapter.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, read = false),
                getCtMsgDao(msgIds[1], userId, read = false),
                getCtMsgDao(msgIds[2], userId, read = false)
            )
        )
        msgIds.removeAt(msgIds.lastIndex)
        msgIds.add("msg_4")
        result = dbAdapter.markReadMessagesForIds(msgIds, userId)
        msgList = dbAdapter.getMessages(userId)
        assertTrue(result)
        for (msg in msgList) {
            if (msg.id in msgIds)
                assertTrue(msg.isRead == 1)
            else
                assertFalse(msg.isRead == 1)
        }
    }

    @Test
    fun test_storeUserProfile() {
        // test
        dbAdapter.storeUserProfile(
            "userID",
            "deviceID",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") }).let { numberOfRows ->
            assertEquals(numberOfRows, 1)
        }

        //validation
        dbAdapter.fetchUserProfileByAccountIdAndDeviceID("userID", "deviceID").let {
            assertNotNull(it)
            assertEquals("john", it.getString("name"))
            assertEquals("daniel", it.getString("father"))
        }
    }

    @Test
    fun test_storeUserProfile_nullAccountID_returnsDB_UPDATE_EROOR() {
        dbAdapter.storeUserProfile(null, "deviceID", JSONObject()).let {
            assertEquals(-1, it)
        }
    }

    @Test
    fun test_storeUserProfile_nullDeviceID_returnsDB_UPDATE_EROOR() {
        dbAdapter.storeUserProfile("accountID", null, JSONObject()).let {
            assertEquals(-1, it)
        }
    }

    @Test
    fun test_storePushNotificationId_when_Called_should_storePushNotificationId() {
        //test
        dbAdapter.storePushNotificationId("pn1", 1)

        //validate
        dbAdapter.fetchPushNotificationIds().let {
            assertEquals(1, it.size)
            assertEquals("pn1", it[0])
        }
    }

    @Test
    fun test_upsertMessages_when_Called_should_InsertORUpsertAMessage() {
        //when a message is not present it will insert the message

        //test
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = false, campaignId = "cp1234")))

        //validate
        assertNotNull(dbAdapter.getMessages("user_11")[0])

        //when a message is not present it will insert the message
        //test
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = true, campaignId = "cp4321")))

        //validate
        assertEquals(1, dbAdapter.getMessages("user_11").size)
        assertEquals(true, dbAdapter.getMessages("user_11")[0].isRead == 1)
        assertEquals("cp4321", dbAdapter.getMessages("user_11")[0].campaignId)
    }

    @Test
    fun test_removeEvents_when_called_should_RemoveAllEntries() {
        //note : will not work with Table.USER_PROFILES, Table.PUSH_NOTIFICATIONS, Table.INBOX_MESSAGES, Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            println("table:$table")
            //assertion
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}4") }, table)
            println("jsonObject = ${dbAdapter.fetchEvents(table, Int.MAX_VALUE)}")

            //test
            dbAdapter.removeEvents(table)

            //validation
            dbAdapter.fetchEvents(table, Int.MAX_VALUE).let {
                println("jsonObject = $it")
                assertNotNull(it)
                assertTrue(it.isEmpty)
                assertFalse(it.hasMore)
                assertFalse(it.hasEvents)
                assertFalse(it.hasProfileEvents)
            }
        }
    }

    @Test
    @Ignore("This could be tested when DBAdapter can be provided with a test clock")
    fun test_cleanUpPushNotifications_when_Called_should_ClearAllStoredPNsThatHaventExpired() {
        dbAdapter.cleanUpPushNotifications()
    }

    @Test
    @Ignore("This could be tested when DBAdapter can be provided with a test clock")
    fun test_cleanStaleEvents_when_Called_should_ClearAllStoredPNsThatHaventExpired() {
        dbAdapter.cleanupStaleEvents(Table.EVENTS)
    }

    @Test
    fun `test delayedLegacyInAppDAO returns singleton instance`() {
        // When
        val dao1 = dbAdapter.delayedLegacyInAppDAO()
        val dao2 = dbAdapter.delayedLegacyInAppDAO()

        // Then
        assertNotNull(dao1)
        assertSame(dao1, dao2) // Verify same instance is returned
    }

    // =====================================================
    // markIndexed / findSweepableV2Ids
    // =====================================================

    @Test
    fun `markIndexed promotes PENDING_INDEXING row to INDEXED`() {
        val userId = "user_sweep"
        val msgId = "sweep_msg_1"
        dbAdapter.upsertMessages(arrayListOf(
            getV2MsgDao(msgId, userId, indexState = InboxIndexState.PENDING_INDEXING, date = 100L)
        ))

        dbAdapter.markIndexed(listOf(msgId), userId)

        // INDEXED row is returned regardless of cutoff
        val sweepable = dbAdapter.findSweepableV2Ids(userId, Long.MAX_VALUE)
        assertTrue(sweepable.contains(msgId))
    }

    @Test
    fun `findSweepableV2Ids returns stale PENDING_INDEXING rows`() {
        val userId = "user_stale"
        val msgId = "stale_msg"
        // date=100 seconds; cutoff=200 → 100 < 200 → stale
        dbAdapter.upsertMessages(arrayListOf(
            getV2MsgDao(msgId, userId, indexState = InboxIndexState.PENDING_INDEXING, date = 100L)
        ))

        val stale = dbAdapter.findSweepableV2Ids(userId, 200L)
        assertTrue(stale.contains(msgId))
    }

    @Test
    fun `findSweepableV2Ids does not return fresh PENDING_INDEXING rows`() {
        val userId = "user_fresh"
        val msgId = "fresh_msg"
        // date=500 seconds; cutoff=200 → 500 > 200 → not stale
        dbAdapter.upsertMessages(arrayListOf(
            getV2MsgDao(msgId, userId, indexState = InboxIndexState.PENDING_INDEXING, date = 500L)
        ))

        val sweepable = dbAdapter.findSweepableV2Ids(userId, 200L)
        assertFalse(sweepable.contains(msgId))
    }

    @Test
    fun `findSweepableV2Ids excludes V1 messages`() {
        val userId = "user_v1"
        val msgId = "v1_msg"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userId)))

        val sweepable = dbAdapter.findSweepableV2Ids(userId, Long.MAX_VALUE)
        assertFalse(sweepable.contains(msgId))
    }

    @Test
    fun `findSweepableV2Ids excludes INDEXED rows with expires=0`() {
        val userId = "user_infinite"
        val msgId = "infinite_ttl_msg"
        dbAdapter.upsertMessages(arrayListOf(
            getV2MsgDao(msgId, userId, indexState = InboxIndexState.INDEXED, expires = 0L)
        ))

        val sweepable = dbAdapter.findSweepableV2Ids(userId, Long.MAX_VALUE)
        assertFalse(sweepable.contains(msgId))
    }

    // =====================================================
    // PENDING DELETE
    // =====================================================

    @Test
    fun `addPendingDelete and getPendingDeleteIds round-trip`() {
        val userId = "user_pd"
        val msgId = "pd_msg_1"
        dbAdapter.addPendingDelete(msgId, userId, null, 0L)

        val ids = dbAdapter.getPendingDeleteIds(userId)
        assertTrue(ids.contains(msgId))
    }

    @Test
    fun `addPendingDelete null guards return false`() {
        assertFalse(dbAdapter.addPendingDelete(null, "user", null, 0L))
        assertFalse(dbAdapter.addPendingDelete("msg", null, null, 0L))
    }

    @Test
    fun `getPendingDeleteIds null userId returns empty set`() {
        assertTrue(dbAdapter.getPendingDeleteIds(null).isEmpty())
    }

    @Test
    fun `getPendingDeletes returns full row including wzrkParams`() {
        val userId = "user_pd2"
        val msgId = "pd_msg_2"
        val params = JSONObject().put("wzrk_key", "val")
        dbAdapter.addPendingDelete(msgId, userId, params, 999L)

        val rows = dbAdapter.getPendingDeletes(userId)
        assertEquals(1, rows.size)
        assertEquals(msgId, rows[0].messageId)
        assertEquals("val", rows[0].wzrkParams?.getString("wzrk_key"))
    }

    @Test
    fun `getPendingDeletes null userId returns empty list`() {
        assertTrue(dbAdapter.getPendingDeletes(null).isEmpty())
    }

    @Test
    fun `removePendingDelete removes the row`() {
        val userId = "user_rpd"
        val msgId = "rpd_msg"
        dbAdapter.addPendingDelete(msgId, userId, null, 0L)
        dbAdapter.removePendingDelete(msgId, userId)

        assertTrue(dbAdapter.getPendingDeleteIds(userId).isEmpty())
    }

    @Test
    fun `removePendingDelete null guards return false`() {
        assertFalse(dbAdapter.removePendingDelete(null, "user"))
        assertFalse(dbAdapter.removePendingDelete("msg", null))
    }

    @Test
    fun `markPendingDeletesAwaitingConfirm transitions state`() {
        val userId = "user_mpdac"
        val msgId = "mpdac_msg"
        dbAdapter.addPendingDelete(msgId, userId, null, 999L)
        val result = dbAdapter.markPendingDeletesAwaitingConfirm(listOf(msgId), userId)
        assertTrue(result)
        // Row is still visible until TTL sweeps it
        assertTrue(dbAdapter.getPendingDeleteIds(userId).contains(msgId))
    }

    @Test
    fun `markPendingDeletesAwaitingConfirm null or empty guards return false`() {
        assertFalse(dbAdapter.markPendingDeletesAwaitingConfirm(null, "user"))
        assertFalse(dbAdapter.markPendingDeletesAwaitingConfirm(emptyList(), "user"))
        assertFalse(dbAdapter.markPendingDeletesAwaitingConfirm(listOf("msg"), null))
    }

    @Test
    fun `removeExpiredAwaitingConfirm removes TTL-elapsed rows`() {
        val userId = "user_reac"
        val msgId = "reac_msg"
        dbAdapter.addPendingDelete(msgId, userId, null, 100L)
        dbAdapter.markPendingDeletesAwaitingConfirm(listOf(msgId), userId)

        val removed = dbAdapter.removeExpiredAwaitingConfirm(userId, 200L) // 200 > 100 → expired
        assertEquals(1, removed)
        assertTrue(dbAdapter.getPendingDeleteIds(userId).isEmpty())
    }

    @Test
    fun `removeExpiredAwaitingConfirm does not remove non-elapsed rows`() {
        val userId = "user_reac2"
        val msgId = "reac_msg2"
        dbAdapter.addPendingDelete(msgId, userId, null, 500L)
        dbAdapter.markPendingDeletesAwaitingConfirm(listOf(msgId), userId)

        val removed = dbAdapter.removeExpiredAwaitingConfirm(userId, 200L) // 200 < 500 → not expired
        assertEquals(0, removed)
        assertTrue(dbAdapter.getPendingDeleteIds(userId).contains(msgId))
    }

    @Test
    fun `removeExpiredAwaitingConfirm null userId returns 0`() {
        assertEquals(0, dbAdapter.removeExpiredAwaitingConfirm(null, 999L))
    }

    @Test
    fun `addPendingDeletes batch and removePendingDeletes batch`() {
        val userId = "user_batch_pd"
        val rows = listOf(
            PendingDelete("batch_1", null, 0L),
            PendingDelete("batch_2", null, 0L)
        )
        dbAdapter.addPendingDeletes(rows, userId)

        val ids = dbAdapter.getPendingDeleteIds(userId)
        assertTrue(ids.containsAll(listOf("batch_1", "batch_2")))

        dbAdapter.removePendingDeletes(listOf("batch_1", "batch_2"), userId)
        assertTrue(dbAdapter.getPendingDeleteIds(userId).isEmpty())
    }

    @Test
    fun `addPendingDeletes and removePendingDeletes null or empty guards return false`() {
        assertFalse(dbAdapter.addPendingDeletes(null, "user"))
        assertFalse(dbAdapter.addPendingDeletes(emptyList(), "user"))
        assertFalse(dbAdapter.addPendingDeletes(listOf(PendingDelete("m", null, 0L)), null))
        assertFalse(dbAdapter.removePendingDeletes(null, "user"))
        assertFalse(dbAdapter.removePendingDeletes(emptyList(), "user"))
        assertFalse(dbAdapter.removePendingDeletes(listOf("m"), null))
    }

    // =====================================================
    // PENDING READ
    // =====================================================

    @Test
    fun `addPendingRead and getPendingReads round-trip`() {
        val userId = "user_pr"
        val msgId = "pr_msg_1"
        dbAdapter.addPendingRead(msgId, userId, 0L)

        val ids = dbAdapter.getPendingReads(userId)
        assertTrue(ids.contains(msgId))
    }

    @Test
    fun `addPendingRead null guards return false`() {
        assertFalse(dbAdapter.addPendingRead(null, "user", 0L))
        assertFalse(dbAdapter.addPendingRead("msg", null, 0L))
    }

    @Test
    fun `getPendingReads null userId returns empty set`() {
        assertTrue(dbAdapter.getPendingReads(null).isEmpty())
    }

    @Test
    fun `removePendingRead removes the row`() {
        val userId = "user_rpr"
        val msgId = "rpr_msg"
        dbAdapter.addPendingRead(msgId, userId, 0L)
        dbAdapter.removePendingRead(msgId, userId)

        assertTrue(dbAdapter.getPendingReads(userId).isEmpty())
    }

    @Test
    fun `removePendingRead null guards return false`() {
        assertFalse(dbAdapter.removePendingRead(null, "user"))
        assertFalse(dbAdapter.removePendingRead("msg", null))
    }

    @Test
    fun `removeExpiredPendingReads removes TTL-elapsed rows`() {
        val userId = "user_repr"
        val msgId = "repr_msg"
        dbAdapter.addPendingRead(msgId, userId, 100L)

        val removed = dbAdapter.removeExpiredPendingReads(userId, 200L) // 200 > 100 → expired
        assertEquals(1, removed)
        assertTrue(dbAdapter.getPendingReads(userId).isEmpty())
    }

    @Test
    fun `removeExpiredPendingReads does not remove non-elapsed rows`() {
        val userId = "user_repr2"
        val msgId = "repr_msg2"
        dbAdapter.addPendingRead(msgId, userId, 500L)

        val removed = dbAdapter.removeExpiredPendingReads(userId, 200L) // 200 < 500 → not expired
        assertEquals(0, removed)
        assertTrue(dbAdapter.getPendingReads(userId).contains(msgId))
    }

    @Test
    fun `removeExpiredPendingReads null userId returns 0`() {
        assertEquals(0, dbAdapter.removeExpiredPendingReads(null, 999L))
    }

    @Test
    fun `addPendingReads batch and removePendingReads batch`() {
        val userId = "user_batch_pr"
        val rows = listOf(
            PendingRead("pr_batch_1", 0L),
            PendingRead("pr_batch_2", 0L)
        )
        dbAdapter.addPendingReads(rows, userId)

        val ids = dbAdapter.getPendingReads(userId)
        assertTrue(ids.containsAll(listOf("pr_batch_1", "pr_batch_2")))

        dbAdapter.removePendingReads(listOf("pr_batch_1", "pr_batch_2"), userId)
        assertTrue(dbAdapter.getPendingReads(userId).isEmpty())
    }

    @Test
    fun `addPendingReads and removePendingReads null or empty guards return false`() {
        assertFalse(dbAdapter.addPendingReads(null, "user"))
        assertFalse(dbAdapter.addPendingReads(emptyList(), "user"))
        assertFalse(dbAdapter.addPendingReads(listOf(PendingRead("m", 0L)), null))
        assertFalse(dbAdapter.removePendingReads(null, "user"))
        assertFalse(dbAdapter.removePendingReads(emptyList(), "user"))
        assertFalse(dbAdapter.removePendingReads(listOf("m"), null))
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private fun getCtMsgDao(
        id: String = "1",
        userId: String = "1",
        read: Boolean = false,
        jsonData: JSONObject = JSONObject(),
        date: Long = System.currentTimeMillis(),
        expires: Long = (System.currentTimeMillis() * 10),
        tags: List<String> = listOf(),
        campaignId: String = "campaignID",
        wzrkParams: JSONObject = JSONObject()
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
        }
    }

    private fun getV2MsgDao(
        id: String = "1",
        userId: String = "1",
        indexState: String = InboxIndexState.PENDING_INDEXING,
        date: Long = 1000L,
        expires: Long = 9_999_999L
    ): CTMessageDAO = getCtMsgDao(id = id, userId = userId, date = date, expires = expires).also {
        it.source = InboxMessageSource.V2
        it.indexState = indexState
    }
}
