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
        val lastId = events.keys().next() as String
        val eventArray = events.getJSONArray(lastId)
        assertTrue(eventArray.length() > 0)
        
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
