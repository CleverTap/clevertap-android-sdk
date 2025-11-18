package com.clevertap.android.sdk.db.dao

import TestCryptHandler
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class EventDAOImplTest : BaseTestCase() {

    private lateinit var eventDAO: EventDAO
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dbEncryptionHandler: DBEncryptionHandler
    private lateinit var testClock: TestClock

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbEncryptionHandler = DBEncryptionHandler(TestCryptHandler(), TestLogger(), EncryptionLevel.NONE)
        testClock = TestClock()
        dbHelper = DatabaseHelper(
            context = appCtx,
            accountId = instanceConfig.accountId,
            dbName = "test_db",
            logger = instanceConfig.logger
        )
        eventDAO = EventDAOImpl(
            dbHelper = dbHelper,
            logger = instanceConfig.logger,
            clock = testClock,
            dbEncryptionHandler = dbEncryptionHandler
        )
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun test_storeEvent_when_called_should_storeTheObjectInGivenTable() {
        val testTables = arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED)
        
        testTables.forEach { table ->
            println("table:$table")
            
            val result1 = eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            val result2 = eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            val result3 = eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            val result4 = eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}4") }, table)
            
            assertEquals(1, result1)
            assertEquals(2, result2)
            assertEquals(3, result3)
            assertEquals(4, result4)
            
            val fetchedEvents = eventDAO.fetchEvents(table, 50)
            assertNotNull(fetchedEvents)
            assertEquals(4, fetchedEvents.data.length())
            assertTrue(fetchedEvents.data[0] is JSONObject)
            assertEquals("${table.tableName}1", (fetchedEvents.data[0] as JSONObject).getString("name"))
            assertEquals("${table.tableName}2", (fetchedEvents.data[1] as JSONObject).getString("name"))
        }
    }

    @Test
    fun test_fetchEvents_when_Called_should_ReturnAListOfEntriesAsJsonObject() {
        val testTables = arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED)
        
        testTables.forEach { table ->
            println("table:$table")

            // Store events
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}4") }, table)

            // Test with limit
            eventDAO.fetchEvents(table, 2).let {
                println("jsonObject = $it")
                assertEquals(2, it.data.length())
                assertTrue(it.data[0] is JSONObject)
                assertEquals("${table.tableName}1", (it.data[0] as JSONObject).getString("name"))
                assertEquals("${table.tableName}2", (it.data[1] as JSONObject).getString("name"))
            }
        }
    }

    @Test
    fun test_removeAllEvents_when_called_should_RemoveAllEntries() {
        val testTables = arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED)
        
        testTables.forEach { table ->
            println("table:$table")
            
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}4") }, table)
            println("jsonObject = ${eventDAO.fetchEvents(table, 50)}")

            eventDAO.removeAllEvents(table)

            eventDAO.fetchEvents(table, 50).let {
                println("jsonObject = $it")
                assertTrue(it.isEmpty)
                assertFalse(it.hasMore)
                assertEquals(0, it.data.length())
                assertEquals(0, it.eventIds.size)
                assertEquals(0, it.profileEventIds.size)
            }
        }
    }

    @Test
    fun test_cleanupEventsFromLastId_when_called_should_removeAllEntriesWithIdLesserThanPassedId() {
        val testTables = arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED)
        
        testTables.forEach { table ->
            println("table:$table")

            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            eventDAO.storeEvent(JSONObject().also { it.put("name", "${table.tableName}4") }, table)
            println("jsonObject = ${eventDAO.fetchEvents(table, 50)}")

            // Remove ids 1 & 2, and keep ids 3 & 4
            eventDAO.cleanupEventsFromLastId("2", table)

            println("after")
            eventDAO.fetchEvents(table, 50).let {
                println("jsonObject = $it")
                assertEquals(2, it.data.length())
                assertTrue(it.data[0] is JSONObject)
                assertEquals("${table.tableName}3", (it.data[0] as JSONObject).getString("name"))
                assertEquals("${table.tableName}4", (it.data[1] as JSONObject).getString("name"))
            }
        }
    }

    @Test
    fun test_fetchEvents_when_noEvents_should_returnNull() {
        val result = eventDAO.fetchEvents(Table.EVENTS, 10)
        assertTrue(result.isEmpty)
        assertFalse(result.hasMore)
        assertEquals(0, result.data.length())
        assertEquals(0, result.eventIds.size)
        assertEquals(0, result.profileEventIds.size)
    }

    @Test
    fun test_fetchEvents_when_limitLessThanAvailable_should_returnCorrectCount() {
        val table = Table.EVENTS
        eventDAO.storeEvent(JSONObject().also { it.put("name", "event1") }, table)
        eventDAO.storeEvent(JSONObject().also { it.put("name", "event2") }, table)
        eventDAO.storeEvent(JSONObject().also { it.put("name", "event3") }, table)

        val result = eventDAO.fetchEvents(table, 2)
        assertNotNull(result)
        assertEquals(2, result.data.length())
        assertEquals("event1", (result.data[0] as JSONObject).getString("name"))
        assertEquals("event2", (result.data[1] as JSONObject).getString("name"))
    }

    @Test
    fun test_cleanupStaleEvents_when_eventsLessThan5DaysOld_should_keepAllEvents() {
        val table = Table.EVENTS
        val currentTime = System.currentTimeMillis()

        // Set current time
        testClock.setCurrentTime(currentTime)

        // Store event 3 days old
        testClock.setCurrentTime(currentTime - (3L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "3_days_old") }, table)

        // Store event 1 day old
        testClock.setCurrentTime(currentTime - (1L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "1_day_old") }, table)

        // Store event 4 days old
        testClock.setCurrentTime(currentTime - (4L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "4_days_old") }, table)

        // Store current event
        testClock.setCurrentTime(currentTime)
        eventDAO.storeEvent(JSONObject().also { it.put("name", "current_event") }, table)

        // Call cleanup at current time
        eventDAO.cleanupStaleEvents(table)

        // Verify all 4 events are still present (all are less than 5 days old)
        val result = eventDAO.fetchEvents(table, 50)
        assertEquals(4, result.data.length())

        // Verify events are in chronological order (oldest first due to CREATED_AT ASC)
        assertEquals("4_days_old", (result.data[0] as JSONObject).getString("name"))
        assertEquals("3_days_old", (result.data[1] as JSONObject).getString("name"))
        assertEquals("1_day_old", (result.data[2] as JSONObject).getString("name"))
        assertEquals("current_event", (result.data[3] as JSONObject).getString("name"))
    }

    @Test
    fun test_cleanupStaleEvents_when_eventsExactly5DaysOld_should_removeThoseEvents() {
        val table = Table.EVENTS
        val currentTime = System.currentTimeMillis()

        // Set current time
        testClock.setCurrentTime(currentTime)

        // Store event exactly 5 days old (should be removed)
        testClock.setCurrentTime(currentTime - (5L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "exactly_5_days_old") }, table)

        // Store event 4 days old (should be kept)
        testClock.setCurrentTime(currentTime - (4L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "4_days_old") }, table)

        // Store event 2 days old (should be kept)
        testClock.setCurrentTime(currentTime - (2L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "2_days_old") }, table)

        // Reset to current time and call cleanup
        testClock.setCurrentTime(currentTime)
        eventDAO.cleanupStaleEvents(table)

        // Verify only events less than 5 days old remain
        val result = eventDAO.fetchEvents(table, 50)
        assertEquals(2, result.data.length())
        assertEquals("4_days_old", (result.data[0] as JSONObject).getString("name"))
        assertEquals("2_days_old", (result.data[1] as JSONObject).getString("name"))
    }

    @Test
    fun test_cleanupStaleEvents_when_eventsMoreThan5DaysOld_should_removeOldEvents() {
        val table = Table.EVENTS
        val currentTime = System.currentTimeMillis()

        // Set current time
        testClock.setCurrentTime(currentTime)

        // Store events older than 5 days (should be removed)
        testClock.setCurrentTime(currentTime - (6L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "6_days_old") }, table)

        testClock.setCurrentTime(currentTime - (10L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "10_days_old") }, table)

        testClock.setCurrentTime(currentTime - (7L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "7_days_old") }, table)

        // Store events less than 5 days old (should be kept)
        testClock.setCurrentTime(currentTime - (3L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "3_days_old") }, table)

        testClock.setCurrentTime(currentTime - (1L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "1_day_old") }, table)

        // Reset to current time and call cleanup
        testClock.setCurrentTime(currentTime)
        eventDAO.cleanupStaleEvents(table)

        // Verify only events less than 5 days old remain
        val result = eventDAO.fetchEvents(table, 50)
        assertEquals(2, result.data.length())
        assertEquals("3_days_old", (result.data[0] as JSONObject).getString("name"))
        assertEquals("1_day_old", (result.data[1] as JSONObject).getString("name"))
    }

    @Test
    fun test_cleanupStaleEvents_when_allEventsAreOld_should_removeAllEvents() {
        val table = Table.EVENTS
        val currentTime = System.currentTimeMillis()

        // Store only old events (all older than 5 days)
        testClock.setCurrentTime(currentTime - (6L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "6_days_old") }, table)

        testClock.setCurrentTime(currentTime - (8L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "8_days_old") }, table)

        testClock.setCurrentTime(currentTime - (15L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "15_days_old") }, table)

        // Reset to current time and call cleanup
        testClock.setCurrentTime(currentTime)
        eventDAO.cleanupStaleEvents(table)

        // Verify all events are removed
        val result = eventDAO.fetchEvents(table, 50)
        assertTrue(result.isEmpty)
        assertEquals(0, result.data.length())
    }

    @Test
    fun test_cleanupStaleEvents_when_multipleTablesHaveStaleEvents_should_cleanupOnlySpecifiedTable() {
        val currentTime = System.currentTimeMillis()

        // Store old events in EVENTS table
        testClock.setCurrentTime(currentTime - (6L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "old_event") }, Table.EVENTS)

        // Store old events in PROFILE_EVENTS table
        eventDAO.storeEvent(JSONObject().also { it.put("name", "old_profile_event") }, Table.PROFILE_EVENTS)

        // Store recent events in both tables
        testClock.setCurrentTime(currentTime - (1L * 24 * 60 * 60 * 1000))
        eventDAO.storeEvent(JSONObject().also { it.put("name", "recent_event") }, Table.EVENTS)
        eventDAO.storeEvent(JSONObject().also { it.put("name", "recent_profile_event") }, Table.PROFILE_EVENTS)

        // Reset to current time and cleanup only EVENTS table
        testClock.setCurrentTime(currentTime)
        eventDAO.cleanupStaleEvents(Table.EVENTS)

        // Verify EVENTS table is cleaned up
        val eventsResult = eventDAO.fetchEvents(Table.EVENTS, 50)
        assertEquals(1, eventsResult.data.length())
        assertEquals("recent_event", (eventsResult.data[0] as JSONObject).getString("name"))

        // Verify PROFILE_EVENTS table is NOT cleaned up (still has old event)
        val profileResult = eventDAO.fetchEvents(Table.PROFILE_EVENTS, 50)
        assertEquals(2, profileResult.data.length())
        assertEquals("old_profile_event", (profileResult.data[0] as JSONObject).getString("name"))
        assertEquals("recent_profile_event", (profileResult.data[1] as JSONObject).getString("name"))
    }
}
