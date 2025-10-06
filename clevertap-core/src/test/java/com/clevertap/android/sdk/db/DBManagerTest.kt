package com.clevertap.android.sdk.db

import TestCryptHandler
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.network.IJRepo
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.spyk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DBManagerTest : BaseTestCase() {

    private lateinit var dbManager: DBManager
    private lateinit var dbManagerSpy: DBManager
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var lockManager: CTLockManager
    private lateinit var dbAdapter: DBAdapter
    private lateinit var dbEncryptionHandler: DBEncryptionHandler

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, "accountId", "accountToken")
        lockManager = CTLockManager()
        dbEncryptionHandler = DBEncryptionHandler(TestCryptHandler(), TestLogger(), EncryptionLevel.NONE)
        dbManager = DBManager(
            accountId = instanceConfig.accountId,
            logger = instanceConfig.logger,
            databaseName = DBAdapter.getDatabaseName(instanceConfig),
            ctLockManager = lockManager,
            ijRepo = IJRepo(config = instanceConfig),
            dbEncryptionHandler = dbEncryptionHandler
        )
        dbManagerSpy = spyk(dbManager)
        dbAdapter = DBAdapter(
            context = appCtx,
            databaseName = DBAdapter.getDatabaseName(instanceConfig),
            accountId = instanceConfig.accountId,
            logger = instanceConfig.logger,
            dbEncryptionHandler = dbEncryptionHandler
        )
    }

    @After
    fun deleteDb() {
        dbManager.loadDBAdapter(appCtx).deleteDB()
        dbManagerSpy.loadDBAdapter(appCtx).deleteDB()
        dbAdapter.deleteDB()
    }

    @Test
    @Ignore("DBAdapter should be refactored to not depend on System.currentTimeMillis(). Until then testing cleaning of stale events is not possible")
    fun test_loadDBAdapter_when_CalledWithContext_should_ReturnDBAdapterInstanceWithCleanedUpTables() {
        //assertion : some tables of dbadapter already have some entries

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            println(
                "before call, entries for table: ${table.tableName} = ${
                    dbAdapter.fetchEvents(
                        table,
                        Int.MAX_VALUE
                    )
                }"
            )
        }

        //test
        val dAdp = dbManager.loadDBAdapter(appCtx)

        //validate
        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            val entries = dAdp.fetchEvents(table, Int.MAX_VALUE)
            println("after call, entries for table: ${table.tableName} = $entries")
            assertNull(entries)
        }
    }

    @Test
    fun test_clearQueues_when_called_should_ClearEventsAndProfileEventsTable() {
        //assertion : some tables of dbadapter already have some entries
        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS).forEach { table ->
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            println(
                "before call, entries for table: ${table.tableName} = ${
                    dbAdapter.fetchEvents(
                        table,
                        Int.MAX_VALUE
                    )
                }"
            )
        }

        //test
        dbManager.clearQueues(appCtx)

        //validate
        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS).forEach { table ->
            val entries = dbAdapter.fetchEvents(table, Int.MAX_VALUE)
            println("after call, entries for table: ${table.tableName} = $entries")
            assertNotNull(entries.data)
            assertTrue(entries.isEmpty)
            assertFalse(entries.hasMore)
        }
    }

    @Test
    fun test_queueEventToDB_when_called_should_storeDataInEventOrProfileEventTable() {
        val json = JSONObject().also { it.put("name", "a1") }

        //test
        dbManager.queueEventToDB(appCtx, json, Constants.PROFILE_EVENT)

        //validate
        dbAdapter.fetchEvents(Table.PROFILE_EVENTS, 50).let {
            println(" entries : $it")
            assertNotNull(it)
            assertEquals(1, it.data.length())
            assertEquals("a1", it.data.getJSONObject(0).getString("name"))
            assertFalse(it.hasMore)
        }

        //test
        dbManager.queueEventToDB(appCtx, json, Constants.PROFILE_EVENT + 1)

        //validate
        dbAdapter.fetchEvents(Table.EVENTS, 50).let {
            println(" entries : $it")
            assertNotNull(it)
            assertEquals(1, it.data.length())
            assertEquals("a1", it.data.getJSONObject(0).getString("name"))
            assertFalse(it.hasMore)
        }
    }

    @Test
    fun test_queuePushNotificationViewedEventToDB_when_Called_should_StoreDataInPushNotificationViewedTable() {
        val json = JSONObject().also { it.put("name", "a1") }

        //test
        dbManager.queuePushNotificationViewedEventToDB(appCtx, json)

        //validate
        dbAdapter.fetchEvents(Table.PUSH_NOTIFICATION_VIEWED, 50).let {
            println(" entries : $it")
            assertNotNull(it)
            assertEquals(1, it.data.length())
            assertEquals("a1", it.data.getJSONObject(0).getString("name"))

        }
    }

    private fun List<JSONObject>.toJSONArray(): JSONArray {
        val arr = JSONArray()
        this.forEach { arr.put(it) }
        return arr
    }

    // ============= New Test Cases for Combined Queue Implementation =============

    @Test
    fun test_getQueuedEvents_when_EventsAndProfileLessThan50_should_ReturnCombinedBatch() {
        // Setup: Add 20 events and 15 profile events (total 35 < 50)
        val events = (1..20).map { JSONObject().put("name", "event$it").put("type", "event") }
        val profileEvents = (1..15).map { JSONObject().put("name", "profile$it").put("type", "profile") }
        
        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(35, queueData.data?.length()) // Should return all 35 events
        assertFalse(queueData.hasMore)

        // Verify we got both event types
        val resultEvents = mutableListOf<JSONObject>()
        for (i in 0 until queueData.data!!.length()) {
            resultEvents.add(queueData.data!!.getJSONObject(i))
        }
        
        val eventCount = resultEvents.count { it.getString("type") == "event" }
        val profileCount = resultEvents.count { it.getString("type") == "profile" }
        
        assertEquals(20, eventCount, "Should have all 20 events")
        assertEquals(15, profileCount, "Should have all 15 profile events")
        
        // Verify IDs are properly tracked
        assertEquals(20, queueData.eventIds.size, "Should track 20 event IDs")
        assertEquals(15, queueData.profileEventIds.size, "Should track 15 profile event IDs")
    }

    @Test
    fun test_getQueuedEvents_when_ProfileEventsMoreThan50_should_PrioritizeProfileOverEvents() {
        // Setup: Add 30 events and 60 profile events
        val events = (1..30).map { JSONObject().put("name", "event$it").put("type", "event") }
        val profileEvents = (1..60).map { JSONObject().put("name", "profile$it").put("type", "profile") }
        
        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(50, queueData.data.length()) // Should return exactly 50 events
        assertTrue(queueData.hasMore)
        
        // Verify all 50 are from profile events table (priority)
        val resultEvents = mutableListOf<JSONObject>()
        for (i in 0 until queueData.data!!.length()) {
            resultEvents.add(queueData.data!!.getJSONObject(i))
        }
        
        val profileCount = resultEvents.count { it.getString("type") == "profile" }
        assertEquals(50, profileCount, "All 50 should be from profile events table due to priority")
        
        // Verify IDs
        assertEquals(0, queueData.eventIds.size, "Should have no event IDs")
        assertEquals(50, queueData.profileEventIds.size, "Should track 50 profile event IDs")
    }

    @Test
    fun test_getQueuedEvents_when_Events40AndProfile40_should_Return40ProfileEventsAnd10Events() {
        // Setup: Add 40 events and 40 profile events
        val events = (1..40).map { JSONObject().put("name", "event$it").put("type", "event").put("index", it) }
        val profileEvents = (1..40).map { JSONObject().put("name", "profile$it").put("type", "profile").put("index", it) }
        
        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(50, queueData.data.length()) // Should return exactly 50 events
        assertTrue(queueData.hasMore)
        
        // Verify we got 40 profile events and 10 events
        val resultEvents = mutableListOf<JSONObject>()
        for (i in 0 until queueData.data.length()) {
            resultEvents.add(queueData.data.getJSONObject(i))
        }
        
        val eventCount = resultEvents.count { it.getString("type") == "event" }
        val profileCount = resultEvents.count { it.getString("type") == "profile" }
        
        assertEquals(10, eventCount, "Should have 10 events to fill up to 50")
        assertEquals(40, profileCount, "Should have all 40 profile events")
        
        // Verify IDs are properly tracked
        assertEquals(10, queueData.eventIds.size, "Should track 10 event IDs")
        assertEquals(40, queueData.profileEventIds.size, "Should track 40 profile event IDs")
        
        // Verify order - profile events should come first due to priority
        val firstFortyAreProfiles = (0..39).all { 
            queueData.data.getJSONObject(it).getString("type") == "profile"
        }
        assertTrue(firstFortyAreProfiles, "First 40 items should be profile events")
        
        val lastTenAreEvents = (40..49).all { 
            queueData.data.getJSONObject(it).getString("type") == "event"
        }
        assertTrue(lastTenAreEvents, "Last 10 items should be events")
    }

    @Test
    fun test_getQueuedEvents_when_NoEvents_should_ReturnEmpty() {
        // Test - no events in database
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertTrue(queueData.isEmpty)
        assertNotNull(queueData.data)
        assertFalse(queueData.hasMore)
        assertEquals(JSONArray().toString(), queueData.data.toString())
        assertEquals(0, queueData.eventIds.size)
        assertEquals(0, queueData.profileEventIds.size)
    }

    @Test
    fun test_getQueuedEvents_when_OnlyProfileEvents_should_ReturnProfileEvents() {
        // Setup: Add only profile events, no regular events
        val profileEvents = (1..30).map { JSONObject().put("name", "profile$it").put("type", "profile") }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(30, queueData.data.length()) // Should return all 30 profile events
        assertFalse(queueData.hasMore)
        
        // Verify all are profile events
        val resultEvents = mutableListOf<JSONObject>()
        for (i in 0 until queueData.data.length()) {
            resultEvents.add(queueData.data.getJSONObject(i))
        }
        
        val profileCount = resultEvents.count { it.getString("type") == "profile" }
        assertEquals(30, profileCount, "All should be profile events")
        
        // Verify IDs
        assertEquals(0, queueData.eventIds.size, "Should have no event IDs")
        assertEquals(30, queueData.profileEventIds.size, "Should track 30 profile event IDs")
    }

    @Test
    fun test_cleanupSentEvents_when_CalledWithEventIds_should_RemoveEventsFromDatabase() {
        // Setup: Add events to both tables
        val events = (1..10).map { JSONObject().put("name", "event$it") }
        val profileEvents = (1..10).map { JSONObject().put("name", "profile$it") }
        
        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }
        
        // Get the events with IDs
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)
        assertNotNull(queueData)
        assertEquals(20, queueData.data.length())
        assertFalse(queueData.hasMore)
        
        // Test cleanup
        val success = dbManager.cleanupSentEvents(
            appCtx,
            queueData.eventIds,
            queueData.profileEventIds
        )
        
        // Validate
        assertTrue(success, "Cleanup should be successful")
        
        // Check that events are removed
        val remainingEvents = dbAdapter.fetchEvents(Table.EVENTS, Int.MAX_VALUE)
        assertNotNull(remainingEvents.data, "Events table should be empty after cleanup")
        assertTrue(remainingEvents.isEmpty, "Events table should be empty after cleanup")

        val remainingProfileEvents = dbAdapter.fetchEvents(Table.PROFILE_EVENTS, Int.MAX_VALUE)
        assertNotNull(remainingProfileEvents.data, "Profile events table should be empty after cleanup")
        assertTrue(remainingProfileEvents.isEmpty, "Events table should be empty after cleanup")
    }

    @Test
    fun test_cleanupSentEvents_when_CalledWithEmptyIds_should_ReturnTrue() {
        // Test cleanup with empty lists
        val success = dbManager.cleanupSentEvents(
            appCtx,
            emptyList(),
            emptyList()
        )
        
        // Validate
        assertTrue(success, "Cleanup with empty lists should return true")
    }

    @Test
    fun test_cleanupSentEvents_when_CalledWithPartialIds_should_RemoveOnlySpecifiedEvents() {
        // Setup: Add 20 events to each table
        val events = (1..20).map { JSONObject().put("name", "event$it") }
        val profileEvents = (1..20).map { JSONObject().put("name", "profile$it") }
        
        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }
        
        // Get first batch
        val firstBatch = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)
        assertNotNull(firstBatch)
        assertEquals(40, firstBatch.data.length()) // 20 + 20
        assertFalse(firstBatch.hasMore)
        
        // Clean up first batch
        dbManager.cleanupSentEvents(
            appCtx,
            firstBatch.eventIds,
            firstBatch.profileEventIds
        )
        
        // Validate - all should be cleaned since we fetched all 40
        val remainingEvents = dbAdapter.fetchEvents(Table.EVENTS, Int.MAX_VALUE)
        assertNotNull(remainingEvents.data, "All events should be removed")
        assertFalse(remainingEvents.hasEvents, "All events should be removed")

        val remainingProfileEvents = dbAdapter.fetchEvents(Table.PROFILE_EVENTS, Int.MAX_VALUE)
        assertNotNull(remainingProfileEvents.data, "All profile events should be removed")
        assertFalse(remainingProfileEvents.hasEvents, "All events should be removed")
    }

    @Test
    fun test_getQueuedEvents_for_PushNotificationViewed_should_UseCorrectTable() {
        // Setup: Add events to push notification viewed table
        val pushEvents = (1..5).map { 
            JSONObject().put("name", "push$it").put("type", "push_viewed") 
        }
        pushEvents.forEach { dbAdapter.storeObject(it, Table.PUSH_NOTIFICATION_VIEWED) }
        
        // Also add regular events to ensure they're not mixed
        val regularEvents = (1..10).map { JSONObject().put("name", "event$it") }
        regularEvents.forEach { dbAdapter.storeObject(it, Table.EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(
            appCtx, 
            50, 
            EventGroup.PUSH_NOTIFICATION_VIEWED
        )

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(5, queueData.data.length()) // Should only get push notification events
        assertFalse(queueData.hasMore)
        
        // Verify all are push notification events
        for (i in 0 until queueData.data.length()) {
            val event = queueData.data.getJSONObject(i)
            assertEquals("push_viewed", event.getString("type"))
        }
        
        // IDs should be tracked in eventIds for consistency
        assertEquals(5, queueData.eventIds.size)
        assertEquals(0, queueData.profileEventIds.size)
    }

    @Test
    fun test_getQueuedEvents_when_BothTablesHaveEvents_should_PrioritizeProfileEvents() {
        // Setup: Add 25 events and 30 profile events (total 55, but limit is 50)
        val events = (1..25).map { JSONObject().put("name", "event$it").put("type", "event") }
        val profileEvents = (1..30).map { JSONObject().put("name", "profile$it").put("type", "profile") }
        
        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(50, queueData.data.length()) // Should return exactly 50
        assertTrue(queueData.hasMore)
        
        // Verify we get 30 profile events (all of them) and 20 regular events
        val resultEvents = mutableListOf<JSONObject>()
        for (i in 0 until queueData.data.length()) {
            resultEvents.add(queueData.data.getJSONObject(i))
        }
        
        val profileCount = resultEvents.count { it.getString("type") == "profile" }
        val eventCount = resultEvents.count { it.getString("type") == "event" }
        
        assertEquals(30, profileCount, "Should have all 30 profile events")
        assertEquals(20, eventCount, "Should have 20 regular events to fill up to 50")
        
        // Verify IDs
        assertEquals(20, queueData.eventIds.size)
        assertEquals(30, queueData.profileEventIds.size)
        
        // Verify order - first 30 should be profile events
        val firstThirtyAreProfiles = (0..29).all { 
            queueData.data.getJSONObject(it).getString("type") == "profile"
        }
        assertTrue(firstThirtyAreProfiles, "First 30 items should be profile events due to priority")
    }

    @Test
    fun test_getQueuedEvents_when_ProfileEventsExactly50_AndEventsExactly20_should_FetchEvents() {
        // Setup: Add some events and exactly 50 profile events
        val events = (1..20).map { JSONObject().put("name", "event$it").put("type", "event") }
        val profileEvents = (1..50).map { JSONObject().put("name", "profile$it").put("type", "profile") }
        
        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(50, queueData.data.length()) // Should return exactly 50
        assertTrue(queueData.hasMore) // ensures that we will continue loop to fetch continued events to send
        
        // Verify all are profile events (no regular events)
        val resultEvents = mutableListOf<JSONObject>()
        for (i in 0 until queueData.data.length()) {
            resultEvents.add(queueData.data.getJSONObject(i))
        }
        
        val profileCount = resultEvents.count { it.getString("type") == "profile" }
        assertEquals(50, profileCount, "All 50 should be profile events")
        
        // Verify IDs
        assertEquals(0, queueData.eventIds.size, "Should not fetch regular events")
        assertEquals(50, queueData.profileEventIds.size)
    }

    @Test
    fun test_getQueuedEvents_when_ProfileEventsExactly20_AndEventsExactly50_should_FetchEvents() {
        // Setup: Add some events and exactly 50 profile events
        val events = (1..50).map { JSONObject().put("name", "event$it").put("type", "event") }
        val profileEvents = (1..20).map { JSONObject().put("name", "profile$it").put("type", "profile") }

        events.forEach { dbAdapter.storeObject(it, Table.EVENTS) }
        profileEvents.forEach { dbAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        // Test
        val queueData = dbManager.getQueuedEvents(appCtx, 50, EventGroup.REGULAR)

        // Validate
        assertNotNull(queueData)
        assertNotNull(queueData.data)
        assertEquals(20, queueData.profileEventIds.size)
        assertEquals(30, queueData.eventIds.size)
        assertEquals(50, queueData.data.length()) // Should return exactly 50
        assertTrue(queueData.hasMore) // ensures that we will continue loop to fetch continued events to send
    }
}
