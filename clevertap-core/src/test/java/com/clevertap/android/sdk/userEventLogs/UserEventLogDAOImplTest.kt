package com.clevertap.android.sdk.userEventLogs

import android.content.Context
import android.database.sqlite.SQLiteException
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.shared.test.TestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class UserEventLogDAOImplTest {

    private lateinit var userEventLogDAO: UserEventLogDAOImpl
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var logger: Logger
    private lateinit var table: Table
    private lateinit var context: Context
    private lateinit var config: CleverTapInstanceConfig

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    companion object {
        private const val TEST_DEVICE_ID = "test_device_id"
        private const val TEST_EVENT_NAME = "test_event"
        private const val TEST_EVENT_NAME_2 = "test_event_2"
        private const val TEST_DB_NAME = "test_clevertap.db"
        private const val MOCK_TIME = 1234567890L
    }

    @Before
    fun setUp() {
        context = TestApplication.application
        logger = mockk(relaxed = true)
        config = CleverTapInstanceConfig.createInstance(context, accID, accToken, accRegion)
        table = Table.USER_EVENT_LOGS_TABLE

        databaseHelper = DatabaseHelper(context, config, TEST_DB_NAME, logger)
        userEventLogDAO = UserEventLogDAOImpl(databaseHelper, logger, table)

        mockkStatic(Utils::class)
        every { Utils.getNowInMillis() } returns MOCK_TIME
    }

    @After
    fun tearDown() {
        databaseHelper.close()
        context.getDatabasePath(TEST_DB_NAME).delete()
        unmockkStatic(Utils::class)
    }

    @Test
    fun `test insertEventByDeviceID when below memory threshold`() {

        // When
        val result = userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertTrue(result > 0)
    }

    @Test
    fun `test insertEventByDeviceID when above memory threshold`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.belowMemThreshold() } returns false
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-2L, result) // DB_OUT_OF_MEMORY_ERROR
    }

    @Test
    fun `test insertEventByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.belowMemThreshold() } returns true
        every { dbHelper.writableDatabase.insertWithOnConflict(
            any(),
            isNull(),
            any(),
            any()
        ) } throws SQLiteException()

        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-1L, result) // DB_UPDATE_ERROR

        verify {
            dbHelper.deleteDatabase()
        }

    }

    @Test
    fun `test updateEventByDeviceID success`() {
        // Given
        val insertResult = userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        assertTrue(insertResult > 0)

        // When
        val updateResult1 = userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        val updateResult2 = userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertTrue(updateResult1)
        assertTrue(updateResult2)

        // Verify count increased
        val count = userEventLogDAO.readEventCountByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        assertEquals(3, count)
    }

    @Test
    fun `test updateEventByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every {
            dbHelper.writableDatabase.execSQL(
                any(),
                any()
            )
        } throws SQLiteException()

        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test updateEventByDeviceID success with timestamp verification`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        // When
        val result = userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertTrue(result)

        // Verify event details
        val eventLog = userEventLogDAO.readEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        assertNotNull(eventLog)
        with(requireNotNull(eventLog)) {
            assertEquals(2, countOfEvents)
            assertEquals(MOCK_TIME, firstTs)
            assertEquals(updateTime, lastTs)
        }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test upsertEventsByDeviceID with new and existing events`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        val eventNames = setOf(TEST_EVENT_NAME, TEST_EVENT_NAME_2)

        // When
        val result = userEventLogDAO.upsertEventsByDeviceID(TEST_DEVICE_ID, eventNames)

        // Then
        assertTrue(result)
        assertEquals(2, userEventLogDAO.readEventCountByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME))
        assertEquals(1, userEventLogDAO.readEventCountByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME_2))
    }

    @Test
    fun `test upsertEventsByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.writableDatabase.beginTransaction() } throws SQLiteException()

        val dao = UserEventLogDAOImpl(dbHelper, logger, table)
        val eventNames = setOf(TEST_EVENT_NAME, TEST_EVENT_NAME_2)

        // When
        val result = dao.upsertEventsByDeviceID(TEST_DEVICE_ID, eventNames)

        // Then
        assertFalse(result)

        verify {
            dbHelper.writableDatabase.beginTransaction()
            dbHelper.writableDatabase.endTransaction() // verify that endTransaction is called even when error occurs
        }
    }

    @Test
    fun `test readEventByDeviceID returns null when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertNull(result)
    }

    @Test
    fun `test readEventByDeviceID returns correct event log after insert`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertNotNull(result)
        with(requireNotNull(result)) {
            assertEquals(TEST_EVENT_NAME, eventName)
            assertEquals(TEST_DEVICE_ID, deviceID)
            assertEquals(1, countOfEvents)
            assertEquals(MOCK_TIME, firstTs)
            assertEquals(MOCK_TIME, lastTs)
        }
        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test readEventByDeviceID after multiple updates`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertNotNull(result)
        with(requireNotNull(result)) {
            assertEquals(TEST_EVENT_NAME, eventName)
            assertEquals(TEST_DEVICE_ID, deviceID)
            assertEquals(2, countOfEvents)
            assertEquals(MOCK_TIME, firstTs) // First timestamp should remain same
            assertEquals(updateTime, lastTs) // Last timestamp should be updated
        }
        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test readEventByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertNull(result)
    }

    @Test
    fun `test readEventCountByDeviceID returns minus one when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventCountByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `test readEventCountByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventCountByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `test readEventCountByDeviceID returns correct count after insert`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventCountByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(1, result)
    }

    @Test
    fun `test readEventCountByDeviceID returns correct count after multiple updates`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventCountByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(3, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceID returns minus one when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventFirstTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventFirstTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceID returns correct timestamp after insert`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventFirstTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(MOCK_TIME, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceID returns same timestamp after updates`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventFirstTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(MOCK_TIME, result) // First timestamp should remain same after updates

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test readEventLastTsByDeviceID returns minus one when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventLastTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventLastTsByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventLastTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventLastTsByDeviceID returns correct timestamp after insert`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventLastTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(MOCK_TIME, result)
    }

    @Test
    fun `test readEventLastTsByDeviceID returns updated timestamp after updates`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.readEventLastTsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertEquals(updateTime, result) // Last timestamp should be updated

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test eventExistsByDeviceID returns false when event does not exist`() {
        // When
        val result = userEventLogDAO.eventExistsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.eventExistsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceID returns true after insert`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.eventExistsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test eventExistsByDeviceID returns true for specific deviceID only`() {
        // Given
        val otherDeviceId = "other_device_id"
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val resultForTestDevice = userEventLogDAO.eventExistsByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        val resultForOtherDevice = userEventLogDAO.eventExistsByDeviceID(otherDeviceId, TEST_EVENT_NAME)

        // Then
        assertTrue(resultForTestDevice)
        assertFalse(resultForOtherDevice)
    }

    @Test
    fun `test eventExistsByDeviceIDAndCount returns false when event does not exist`() {
        // When
        val result = userEventLogDAO.eventExistsByDeviceIDAndCount(TEST_DEVICE_ID, TEST_EVENT_NAME, 1)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIDAndCount when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.eventExistsByDeviceIDAndCount(TEST_DEVICE_ID, TEST_EVENT_NAME, 1)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIDAndCount returns true for matching count`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.eventExistsByDeviceIDAndCount(TEST_DEVICE_ID, TEST_EVENT_NAME, 1)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test eventExistsByDeviceIDAndCount returns false for non-matching count`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.eventExistsByDeviceIDAndCount(TEST_DEVICE_ID, TEST_EVENT_NAME, 2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIDAndCount verifies count after updates`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val resultForCount1 = userEventLogDAO.eventExistsByDeviceIDAndCount(TEST_DEVICE_ID, TEST_EVENT_NAME, 1)
        val resultForCount2 = userEventLogDAO.eventExistsByDeviceIDAndCount(TEST_DEVICE_ID, TEST_EVENT_NAME, 2)

        // Then
        assertFalse(resultForCount1)
        assertTrue(resultForCount2)
    }

    @Test fun `test eventExistsByDeviceIDAndCount returns true for specific deviceID and count`(){
        // Given
        val otherDeviceId = "other_device_id"
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)
        userEventLogDAO.insertEventByDeviceID(otherDeviceId, TEST_EVENT_NAME)
        userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val resultForTestDevice = userEventLogDAO.eventExistsByDeviceIDAndCount(TEST_DEVICE_ID, TEST_EVENT_NAME, 2)
        val resultForOtherDevice = userEventLogDAO.eventExistsByDeviceIDAndCount(otherDeviceId, TEST_EVENT_NAME, 1)

        // Then
        assertTrue(resultForTestDevice)
        assertTrue(resultForOtherDevice)
    }

    @Test
    fun `test allEventsByDeviceID returns empty list when no events exist`() {
        // When
        val result = userEventLogDAO.allEventsByDeviceID(TEST_DEVICE_ID)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test allEventsByDeviceID when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.allEventsByDeviceID(TEST_DEVICE_ID)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test allEventsByDeviceID returns correct list after inserts`() {
        // Given
        val list = listOf(TEST_EVENT_NAME, TEST_EVENT_NAME_2)
            list.forEach {
                userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, it)
            }

        // When
        val result = userEventLogDAO.allEventsByDeviceID(TEST_DEVICE_ID)

        // Then
        assertEquals(2, result.size)

        assertTrue(result.all {
            list.contains(it.eventName) && it.deviceID == TEST_DEVICE_ID
        })
    }

    @Test
    fun `test allEventsByDeviceID returns events for specific deviceID only`() {
        // Given
        val otherDeviceId = "other_device_id"
        listOf(TEST_DEVICE_ID, otherDeviceId).forEach { deviceId ->
            userEventLogDAO.insertEventByDeviceID(deviceId, TEST_EVENT_NAME)
        }

        // When
        val results = listOf(TEST_DEVICE_ID, otherDeviceId)
            .associateWith { deviceId ->
                userEventLogDAO.allEventsByDeviceID(deviceId)
            }

        // Then
        results.forEach { (deviceId, events) ->
            assertEquals(1, events.size)
            assertTrue(events.all { it.deviceID == deviceId })
        }
    }

    @Test
    fun `test allEventsByDeviceID returns events ordered by lastTs`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // Mock different time for second event
        val laterTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns laterTime

        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME_2)

        // When
        val result = userEventLogDAO.allEventsByDeviceID(TEST_DEVICE_ID)

        // Then
        assertEquals(2, result.size)
        assertEquals(TEST_EVENT_NAME, result[0].eventName)  // Earlier event first
        assertEquals(TEST_EVENT_NAME_2, result[1].eventName) // Later event second
        assertTrue(result[0].lastTs < result[1].lastTs)

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test allEvents returns empty list when no events exist`() {
        // When
        val result = userEventLogDAO.allEvents()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test allEvents when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.allEvents()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test allEvents returns all events from different users`() {
        // Given
        val deviceIds = listOf(TEST_DEVICE_ID, "other_device_id")
        val eventNames = listOf(TEST_EVENT_NAME, TEST_EVENT_NAME_2)

        deviceIds.forEach { deviceId ->
            eventNames.forEach { eventName ->
                userEventLogDAO.insertEventByDeviceID(deviceId, eventName)
            }
        }

        // When
        val result = userEventLogDAO.allEvents()

        // Then
        assertEquals(4, result.size)
        result.all {
            deviceIds.contains(it.deviceID) && eventNames.contains(it.eventName)
        }
    }

    @Test
    fun `test allEvents returns events ordered by lastTs`() {
        // Given
        val events = listOf(TEST_EVENT_NAME, TEST_EVENT_NAME_2)

        events.forEachIndexed { index, eventName ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, eventName)
        }

        // When
        val result = userEventLogDAO.allEvents()

        // Then
        assertEquals(2, result.size)
        result.zipWithNext { a, b ->
            assertTrue(a.lastTs <= b.lastTs)
        }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test allEvents returns correct event details`() {
        // Given
        data class EventData(val deviceId: String, val name: String, val count: Int)

        val testData = listOf(
            EventData(TEST_DEVICE_ID, TEST_EVENT_NAME, 2),
            EventData("other_device_id", TEST_EVENT_NAME_2, 1)
        )

        testData.forEach { (deviceId, eventName, updateCount) ->
            userEventLogDAO.insertEventByDeviceID(deviceId, eventName)
            repeat(updateCount - 1) {
                userEventLogDAO.updateEventByDeviceID(deviceId, eventName)
            }
        }

        // When
        val result = userEventLogDAO.allEvents()
            .groupBy { it.deviceID }
            .mapValues { (_, events) -> events.associateBy { it.eventName } }

        // Then
        testData.forEach { (deviceId, eventName, expectedCount) ->
            val event = result[deviceId]?.get(eventName)
            assertNotNull(event)
            with(requireNotNull(event)) {
                assertEquals(deviceId, this.deviceID)
                assertEquals(eventName, this.eventName)
                assertEquals(expectedCount, this.countOfEvents)
                assertEquals(MOCK_TIME, this.firstTs)
                assertEquals(MOCK_TIME, this.lastTs)
            }
        }
    }

    @Test
    fun `test cleanUpExtraEvents with zero threshold`() {
        // When
        val result = userEventLogDAO.cleanUpExtraEvents(0, 2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test cleanUpExtraEvents with negative threshold`() {
        // When
        val result = userEventLogDAO.cleanUpExtraEvents(-5, 2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test cleanUpExtraEvents with negative numberOfRowsToCleanup`() {
        // When
        val result = userEventLogDAO.cleanUpExtraEvents(5, -2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test cleanUpExtraEvents with zero numberOfRowsToCleanup`() {
        // When
        val result = userEventLogDAO.cleanUpExtraEvents(5, 0)

        // Then
        assertTrue(result)  // Should pass as 0 is valid now
    }

    @Test
    fun `test cleanUpExtraEvents with numberOfRowsToCleanup equal to threshold`() {
        // When
        val result = userEventLogDAO.cleanUpExtraEvents(5, 5)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test cleanUpExtraEvents with numberOfRowsToCleanup greater than threshold`() {
        // When
        val result = userEventLogDAO.cleanUpExtraEvents(5, 6)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test cleanUpExtraEvents validation ensures database is not modified with invalid params`() {
        // Given
        val events = (1..5).map { "event_$it" }
        events.forEach { eventName ->
            userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, eventName)
        }

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(5, 6)

        // Then
        assertFalse(result)
        assertEquals(5, userEventLogDAO.allEvents().size) // Verify no events were deleted
    }

    @Test
    fun `test cleanUpExtraEvents when no events exist`() {
        // When
        val result = userEventLogDAO.cleanUpExtraEvents(5, 2)

        // Then
        assertTrue(result)
        assertTrue(userEventLogDAO.allEvents().isEmpty())
    }

    @Test
    fun `test cleanUpExtraEvents when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.writableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.cleanUpExtraEvents(5, 2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test cleanUpExtraEvents deletes correct number of events when above threshold`() {
        // Given
        val events = (1..10).map { "event_$it" }

        events.forEachIndexed { index, eventName ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, eventName)
        }

        val threshold = 6
        val numberOfRowsToCleanup = 2

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(threshold, numberOfRowsToCleanup)

        // Then
        assertTrue(result)

        val remainingEvents = userEventLogDAO.allEvents()
        assertEquals(threshold - numberOfRowsToCleanup, remainingEvents.size) // Should have 4 events remaining

        // Verify oldest events were deleted and newest remain
        remainingEvents
            .map { it.eventName }
            .let { eventNames ->
                // First 6 events should be deleted (10 - 4 = 6)
                (1..6).forEach {
                    assertFalse(eventNames.contains("event_$it"))
                }
                // Last 4 events should remain
                (7..10).forEach {
                    assertTrue(eventNames.contains("event_$it"))
                }
            }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test cleanUpExtraEvents maintains events when below threshold`() {
        // Given
        val events = (1..3).map { "event_$it" }

        events.forEachIndexed { index, eventName ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, eventName)
        }

        val threshold = 5
        val numberOfRowsToCleanup = 2

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(threshold, numberOfRowsToCleanup)

        // Then
        assertTrue(result)

        userEventLogDAO.allEvents()
            .map { it.eventName }
            .let { eventNames ->
                assertEquals(3, eventNames.size) // All events should remain
                assertTrue(eventNames.containsAll(events))
            }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test cleanUpExtraEvents maintains correct order after cleanup`() {
        // Given
        val eventCount = 10
        val events = (1..eventCount).map { "event_$it" }

        events.forEachIndexed { index, eventName ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, eventName)
        }

        val threshold = 6
        val numberOfRowsToCleanup = 2

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(threshold, numberOfRowsToCleanup)

        // Then
        assertTrue(result)

        userEventLogDAO.allEvents().let { remainingEvents ->
            assertEquals(4, remainingEvents.size) // threshold - numberOfRowsToCleanup
            remainingEvents.zipWithNext { a, b ->
                assertTrue(a.lastTs <= b.lastTs)
            }
        }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test cleanUpExtraEvents with threshold 1 and numberOfRowsToCleanup 0 when single event exists`() {
        // Given
        userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, TEST_EVENT_NAME)

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(1, 0)

        // Then
        assertTrue(result)
        userEventLogDAO.allEvents().let { events ->
            assertEquals(1, events.size)
            assertEquals(TEST_EVENT_NAME, events[0].eventName)
        }
    }

    @Test
    fun `test cleanUpExtraEvents with threshold 1 and numberOfRowsToCleanup 0 when multiple events exist`() {
        // Given
        val events = (1..3).map { "event_$it" }

        events.forEachIndexed { index, eventName ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, eventName)
        }

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(1, 0)

        // Then
        assertTrue(result)
        userEventLogDAO.allEvents().let { remainingEvents ->
            assertEquals(1, remainingEvents.size)
            assertEquals("event_3", remainingEvents[0].eventName) // Should keep the most recent event
            assertEquals(MOCK_TIME + 2000L, remainingEvents[0].lastTs)
        }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test cleanUpExtraEvents with threshold 1 and numberOfRowsToCleanup 0 with multiple users`() {
        // Given
        val devices = listOf(TEST_DEVICE_ID, "other_device_id")

        devices.forEachIndexed { index, deviceId ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEventByDeviceID(deviceId, TEST_EVENT_NAME)
        }

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(1, 0)

        // Then
        assertTrue(result)
        userEventLogDAO.allEvents().let { remainingEvents ->
            assertEquals(1, remainingEvents.size)
            // Should keep the last inserted event
            assertEquals("other_device_id", remainingEvents[0].deviceID)
        }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test cleanUpExtraEvents with threshold 1 and numberOfRowsToCleanup 0 maintains order after cleanup`() {
        // Given
        (1..5).forEach { index ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEventByDeviceID(TEST_DEVICE_ID, "event_$index")

            // Add some updates to earlier events to mix up lastTs
            if (index > 1) {
                every { Utils.getNowInMillis() } returns mockTime + 100
                userEventLogDAO.updateEventByDeviceID(TEST_DEVICE_ID, "event_1")
            }
        }

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(1, 0)

        // Then
        assertTrue(result)
        userEventLogDAO.allEvents().let { remainingEvents ->
            assertEquals(1, remainingEvents.size)
            // Should keep event_1 as it has the latest lastTs due to updates
            assertEquals("event_1", remainingEvents[0].eventName)
        }

        verify {
            Utils.getNowInMillis()
        }
    }

}