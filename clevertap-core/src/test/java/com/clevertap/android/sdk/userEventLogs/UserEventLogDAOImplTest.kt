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

}