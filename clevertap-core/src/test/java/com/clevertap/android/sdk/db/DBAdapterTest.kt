package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.inbox.CTMessageDAO
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

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbAdapter = DBAdapter(
            context = appCtx,
            databaseName = DBAdapter.getDatabaseName(instanceConfig),
            accountId = instanceConfig.accountId,
            logger = instanceConfig.logger
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
}
