package com.clevertap.android.sdk.usereventlogs

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
    private val testDeviceId = UserEventLogTestData.TestDeviceIds.SAMPLE_DEVICE_ID
    private val testEventName = UserEventLogTestData.EventNames.TEST_EVENT
    private val testEventName2 = UserEventLogTestData.EventNames.TEST_EVENT_2
    private val testEventNameNormalized = UserEventLogTestData.EventNames.eventNameToNormalizedMap[testEventName]!!
    private val testEventNameNormalized2 = UserEventLogTestData.EventNames.eventNameToNormalizedMap[testEventName2]!!
    private val setOfActualAndNormalizedEventNamePair = UserEventLogTestData.EventNames.setOfActualAndNormalizedEventNamePair


    companion object {
        private const val TEST_EVENT_NAME_2 = "TEST_EVENT_2"
        private const val TEST_EVENT_NAME_2_NORMALIZED = "TEST_EVENT_2"
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
    fun `test insertEvent when below memory threshold`() {

        // When
        val result = userEventLogDAO.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // Then
        assertTrue(result > 0)
    }

    @Test
    fun `test insertEvent when above memory threshold`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.belowMemThreshold() } returns false
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // Then
        assertEquals(-2L, result) // DB_OUT_OF_MEMORY_ERROR
    }

    @Test
    fun `test insertEvent when db error occurs`() {
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
        val result = dao.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // Then
        assertEquals(-1L, result) // DB_UPDATE_ERROR

        verify {
            dbHelper.deleteDatabase()
        }

    }

    @Test
    fun `test updateEventByDeviceIdAndNormalizedEventName success`() {
        // Given
        val insertResult = userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)
        assertTrue(insertResult > 0)

        // When
        val updateResult1 = userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)
        val updateResult2 = userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertTrue(updateResult1)
        assertTrue(updateResult2)

        // Verify count increased
        val count = userEventLogDAO.readEventCountByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)
        assertEquals(3, count)
    }

    @Test
    fun `test updateEventByDeviceIdAndNormalizedEventName when db error occurs`() {
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
        val result = dao.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test updateEventByDeviceIdAndNormalizedEventName success with timestamp verification`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        // When
        val result = userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertTrue(result)

        // Verify event details
        val eventLog = userEventLogDAO.readEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)
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
    fun `test upsertEventsByDeviceIdAndNormalizedEventName with new and existing events`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.upsertEventsByDeviceIdAndNormalizedEventName(testDeviceId, setOfActualAndNormalizedEventNamePair)

        // Then
        assertTrue(result)
        assertEquals(2, userEventLogDAO.readEventCountByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized))
        assertEquals(1, userEventLogDAO.readEventCountByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized2))
    }

    @Test
    fun `test upsertEventsByDeviceIdAndNormalizedEventName when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.writableDatabase.beginTransaction() } throws SQLiteException()

        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.upsertEventsByDeviceIdAndNormalizedEventName(testDeviceId, setOfActualAndNormalizedEventNamePair)

        // Then
        assertFalse(result)

        verify {
            dbHelper.writableDatabase.beginTransaction()
            dbHelper.writableDatabase.endTransaction() // verify that endTransaction is called even when error occurs
        }
    }

    @Test
    fun `test readEventByDeviceIdAndNormalizedEventName returns null when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertNull(result)
    }

    @Test
    fun `test readEventByDeviceIdAndNormalizedEventName returns correct event log after insert`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertNotNull(result)
        with(requireNotNull(result)) {
            assertEquals(testEventName, eventName)
            assertEquals(testEventNameNormalized, normalizedEventName)
            assertEquals(testDeviceId, deviceID)
            assertEquals(1, countOfEvents)
            assertEquals(MOCK_TIME, firstTs)
            assertEquals(MOCK_TIME, lastTs)
        }
        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test readEventByDeviceIdAndNormalizedEventName after multiple updates`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertNotNull(result)
        with(requireNotNull(result)) {
            assertEquals(testEventName, eventName)
            assertEquals(testEventNameNormalized, normalizedEventName)
            assertEquals(testDeviceId, deviceID)
            assertEquals(2, countOfEvents)
            assertEquals(MOCK_TIME, firstTs) // First timestamp should remain same
            assertEquals(updateTime, lastTs) // Last timestamp should be updated
        }
        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test readEventByDeviceIdAndNormalizedEventName when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertNull(result)
    }

    @Test
    fun `test readEventCountByDeviceIdAndNormalizedEventName returns minus one when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventCountByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `test readEventCountByDeviceIdAndNormalizedEventName when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventCountByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `test readEventCountByDeviceIdAndNormalizedEventName returns correct count after insert`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventCountByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(1, result)
    }

    @Test
    fun `test readEventCountByDeviceIdAndNormalizedEventName returns correct count after multiple updates`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)
        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)
        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventCountByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(3, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceIdAndNormalizedEventName returns minus one when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventFirstTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceIdAndNormalizedEventName when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventFirstTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceIdAndNormalizedEventName returns correct timestamp after insert`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventFirstTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(MOCK_TIME, result)
    }

    @Test
    fun `test readEventFirstTsByDeviceIdAndNormalizedEventName returns same timestamp after updates`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)
        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventFirstTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(MOCK_TIME, result) // First timestamp should remain same after updates

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test readEventLastTsByDeviceIdAndNormalizedEventName returns minus one when event does not exist`() {
        // When
        val result = userEventLogDAO.readEventLastTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventLastTsByDeviceIdAndNormalizedEventName when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.readEventLastTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `test readEventLastTsByDeviceIdAndNormalizedEventName returns correct timestamp after insert`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventLastTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(MOCK_TIME, result)
    }

    @Test
    fun `test readEventLastTsByDeviceIdAndNormalizedEventName returns updated timestamp after updates`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // Mock different time for update
        val updateTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns updateTime

        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // When
        val result = userEventLogDAO.readEventLastTsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertEquals(updateTime, result) // Last timestamp should be updated

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventName returns false when event does not exist`() {
        // When
        val result = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventName when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.eventExistsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventName returns true after insert`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventName returns true for specific deviceID only`() {
        // Given
        val otherDeviceId = "other_device_id"
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // When
        val resultForTestDevice = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)
        val resultForOtherDevice = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventName(otherDeviceId, testEventNameNormalized)

        // Then
        assertTrue(resultForTestDevice)
        assertFalse(resultForOtherDevice)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventNameAndCount returns false when event does not exist`() {
        // When
        val result = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventNameAndCount(testDeviceId, testEventNameNormalized, 1)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventNameAndCount when db error occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.readableDatabase } throws SQLiteException()
        val dao = UserEventLogDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.eventExistsByDeviceIdAndNormalizedEventNameAndCount(testDeviceId, testEventNameNormalized, 1)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventNameAndCount returns true for matching count`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId, testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventNameAndCount(testDeviceId, testEventNameNormalized, 1)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventNameAndCount returns false for non-matching count`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventNameAndCount(testDeviceId, testEventNameNormalized, 2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test eventExistsByDeviceIdAndNormalizedEventNameAndCount verifies count after updates`() {
        // Given
        userEventLogDAO.insertEvent(testDeviceId, testEventName, testEventNameNormalized)
        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // When
        val resultForCount1 = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventNameAndCount(testDeviceId, testEventNameNormalized, 1)
        val resultForCount2 = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventNameAndCount(testDeviceId, testEventNameNormalized, 2)

        // Then
        assertFalse(resultForCount1)
        assertTrue(resultForCount2)
    }

    @Test fun `test eventExistsByDeviceIdAndNormalizedEventNameAndCount returns true for specific deviceID and count`(){
        // Given
        val otherDeviceId = "other_device_id"
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)
        userEventLogDAO.insertEvent(otherDeviceId, testEventName, testEventNameNormalized)
        userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, testEventNameNormalized)

        // When
        val resultForTestDevice = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventNameAndCount(testDeviceId, testEventNameNormalized, 2)
        val resultForOtherDevice = userEventLogDAO.eventExistsByDeviceIdAndNormalizedEventNameAndCount(otherDeviceId, testEventNameNormalized, 1)

        // Then
        assertTrue(resultForTestDevice)
        assertTrue(resultForOtherDevice)
    }

    @Test
    fun `test allEventsByDeviceID returns empty list when no events exist`() {
        // When
        val result = userEventLogDAO.allEventsByDeviceID(testDeviceId)

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
        val result = dao.allEventsByDeviceID(testDeviceId)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test allEventsByDeviceID returns correct list after inserts`() {
        // Given
            setOfActualAndNormalizedEventNamePair.forEach {
                userEventLogDAO.insertEvent(testDeviceId, it.first,it.second)
            }

        // When
        val result = userEventLogDAO.allEventsByDeviceID(testDeviceId)

        // Then
        assertEquals(2, result.size)

        assertTrue(result.all {
            setOfActualAndNormalizedEventNamePair.contains(Pair(it.eventName,it.normalizedEventName)) && it.deviceID == testDeviceId
        })
    }

    @Test
    fun `test allEventsByDeviceID returns events for specific deviceID only`() {
        // Given
        val otherDeviceId = "other_device_id"
        listOf(testDeviceId, otherDeviceId).forEach { deviceId ->
            userEventLogDAO.insertEvent(deviceId,testEventName, testEventNameNormalized)
        }

        // When
        val results = listOf(testDeviceId, otherDeviceId)
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
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // Mock different time for second event
        val laterTime = MOCK_TIME + 1000
        every { Utils.getNowInMillis() } returns laterTime

        userEventLogDAO.insertEvent(testDeviceId, testEventName2, testEventNameNormalized2)

        // When
        val result = userEventLogDAO.allEventsByDeviceID(testDeviceId)

        // Then
        assertEquals(2, result.size)
        assertEquals(testEventNameNormalized, result[0].normalizedEventName)  // Earlier event first
        assertEquals(testEventNameNormalized2, result[1].normalizedEventName) // Later event second
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
        val deviceIds = listOf(testDeviceId, "other_device_id")

        deviceIds.forEach { deviceId ->
            setOfActualAndNormalizedEventNamePair.forEach { pair ->
                userEventLogDAO.insertEvent(deviceId, pair.first, pair.second)
            }
        }

        // When
        val result = userEventLogDAO.allEvents()

        // Then
        assertEquals(4, result.size)
        result.all {
            deviceIds.contains(it.deviceID) && setOfActualAndNormalizedEventNamePair.contains(
                Pair(
                    it.eventName,
                    it.normalizedEventName
                )
            )
        }
    }

    @Test
    fun `test allEvents returns events ordered by lastTs`() {
        // Given

        setOfActualAndNormalizedEventNamePair.forEachIndexed { index, pair ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEvent(testDeviceId,pair.first, pair.second)
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
        data class EventData(val deviceId: String, val name: String, val normalizedName: String, val count: Int)

        val testData = listOf(
            EventData(testDeviceId, testEventName, testEventNameNormalized, 2),
            EventData("other_device_id", testEventName2, testEventNameNormalized2, 1)
        )

        testData.forEach { (deviceId, eventName, normalizedName, updateCount) ->
            userEventLogDAO.insertEvent(deviceId, eventName, normalizedName)
            repeat(updateCount - 1) {
                userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(deviceId, normalizedName)
            }
        }

        // When
        val result = userEventLogDAO.allEvents()
            .groupBy { it.deviceID }
            .mapValues { (_, events) -> events.associateBy { it.eventName } }

        // Then
        testData.forEach { (deviceId, eventName, normalizedName, expectedCount) ->
            val event = result[deviceId]?.get(eventName)
            assertNotNull(event)
            with(requireNotNull(event)) {
                assertEquals(deviceId, this.deviceID)
                assertEquals(eventName, this.eventName)
                assertEquals(normalizedName, this.normalizedEventName)
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
        val setOfActualAndNormalizedEventNamePair = events.map {
            Pair(it, Utils.getNormalizedName(it))
        }.toSet()
        setOfActualAndNormalizedEventNamePair.forEach { pair ->
            userEventLogDAO.insertEvent(testDeviceId, pair.first,pair.second)
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
        val setOfActualAndNormalizedEventNamePair = events.map {
            Pair(it, Utils.getNormalizedName(it))
        }.toSet()

        setOfActualAndNormalizedEventNamePair.forEachIndexed { index, pair ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEvent(testDeviceId, pair.first,pair.second)
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
            .map { it.normalizedEventName }
            .let { normalizedEventNames ->
                // First 6 events should be deleted (10 - 4 = 6)
                (1..6).forEach {
                    assertFalse(normalizedEventNames.contains("event_$it"))
                }
                // Last 4 events should remain
                (7..10).forEach {
                    assertTrue(normalizedEventNames.contains("event_$it"))
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
        val setOfActualAndNormalizedEventNamePair = events.map {
            Pair(it, Utils.getNormalizedName(it))
        }.toSet()

        setOfActualAndNormalizedEventNamePair.forEachIndexed { index, pair ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEvent(testDeviceId, pair.first,pair.second)
        }

        val threshold = 5
        val numberOfRowsToCleanup = 2

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(threshold, numberOfRowsToCleanup)

        // Then
        assertTrue(result)

        userEventLogDAO.allEvents()
            .map { it.normalizedEventName }
            .let { normalizedEventNames ->
                assertEquals(3, normalizedEventNames.size) // All events should remain
                assertTrue(normalizedEventNames.containsAll(events))
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
        val setOfActualAndNormalizedEventNamePair = events.map {
            Pair(it, Utils.getNormalizedName(it))
        }.toSet()

        setOfActualAndNormalizedEventNamePair.forEachIndexed { index, pair ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEvent(testDeviceId, pair.first, pair.second)
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
        userEventLogDAO.insertEvent(testDeviceId,testEventName, testEventNameNormalized)

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(1, 0)

        // Then
        assertTrue(result)
        userEventLogDAO.allEvents().let { events ->
            assertEquals(1, events.size)
            assertEquals(testEventNameNormalized, events[0].normalizedEventName)
        }
    }

    @Test
    fun `test cleanUpExtraEvents with threshold 1 and numberOfRowsToCleanup 0 when multiple events exist`() {
        // Given
        val events = (1..3).map { "event_$it" }
        val setOfActualAndNormalizedEventNamePair = events.map {
            Pair(it, Utils.getNormalizedName(it))
        }.toSet()

        setOfActualAndNormalizedEventNamePair.forEachIndexed { index, pair ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEvent(testDeviceId, pair.first, pair.second)
        }

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(1, 0)

        // Then
        assertTrue(result)
        userEventLogDAO.allEvents().let { remainingEvents ->
            assertEquals(1, remainingEvents.size)
            assertEquals("event_3", remainingEvents[0].normalizedEventName) // Should keep the most recent event
            assertEquals(MOCK_TIME + 2000L, remainingEvents[0].lastTs)
        }

        verify {
            Utils.getNowInMillis()
        }
    }

    @Test
    fun `test cleanUpExtraEvents with threshold 1 and numberOfRowsToCleanup 0 with multiple users`() {
        // Given
        val devices = listOf(testDeviceId, "other_device_id")

        devices.forEachIndexed { index, deviceId ->
            val mockTime = MOCK_TIME + (index * 1000L)
            every { Utils.getNowInMillis() } returns mockTime
            userEventLogDAO.insertEvent(deviceId, testEventName, testEventNameNormalized)
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
            val eventName = "EV e n t_$index"
            userEventLogDAO.insertEvent(testDeviceId, eventName,Utils.getNormalizedName(eventName))

            // Add some updates to earlier events to mix up lastTs
            if (index > 1) {
                every { Utils.getNowInMillis() } returns mockTime + 100
                userEventLogDAO.updateEventByDeviceIdAndNormalizedEventName(testDeviceId, "event_1")
            }
        }

        // When
        val result = userEventLogDAO.cleanUpExtraEvents(1, 0)

        // Then
        assertTrue(result)
        userEventLogDAO.allEvents().let { remainingEvents ->
            assertEquals(1, remainingEvents.size)
            // Should keep event_1 as it has the latest lastTs due to updates
            assertEquals("event_1", remainingEvents[0].normalizedEventName)
        }

        verify {
            Utils.getNowInMillis()
        }
    }

}