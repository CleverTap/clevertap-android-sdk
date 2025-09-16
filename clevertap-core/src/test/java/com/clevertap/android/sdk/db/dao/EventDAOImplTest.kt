package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.shared.test.BaseTestCase
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

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbHelper = DatabaseHelper(appCtx, instanceConfig.accountId, "test_db", instanceConfig.logger)
        eventDAO = EventDAOImpl(dbHelper, instanceConfig.logger)
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
}
