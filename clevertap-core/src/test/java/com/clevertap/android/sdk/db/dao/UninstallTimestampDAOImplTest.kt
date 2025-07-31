package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class UninstallTimestampDAOImplTest : BaseTestCase() {

    private lateinit var uninstallTimestampDAO: UninstallTimestampDAO
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var testClock: TestClock

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbHelper = DatabaseHelper(appCtx, instanceConfig.accountId, "test_db", instanceConfig.logger)
        testClock = TestClock()
        uninstallTimestampDAO = UninstallTimestampDAOImpl(dbHelper, instanceConfig.logger, testClock)
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun test_getLastUninstallTimestamp_when_FunctionIsCalled_should_ReturnTheLastUninstallTime() {
        // When no uninstall time is stored, should return 0
        assertEquals(0, uninstallTimestampDAO.getLastUninstallTimestamp())

        // Set a specific time on the test clock
        val expectedTime = 1609459200000L // January 1, 2021 00:00:00 UTC
        testClock.setCurrentTime(expectedTime)
        
        // Store timestamp using the test clock
        uninstallTimestampDAO.storeUninstallTimestamp()
        
        // Validation: the exact timestamp should be returned
        assertEquals(expectedTime, uninstallTimestampDAO.getLastUninstallTimestamp())
    }

    @Test
    fun test_getLastUninstallTimestamp_when_noTimestampStored_should_returnZero() {
        val result = uninstallTimestampDAO.getLastUninstallTimestamp()
        assertEquals(0, result)
    }

    @Test
    fun test_storeUninstallTimestamp_and_getLastUninstallTimestamp_should_work() {
        val expectedTime = 1609459200000L // January 1, 2021 00:00:00 UTC
        testClock.setCurrentTime(expectedTime)
        
        uninstallTimestampDAO.storeUninstallTimestamp()
        val result = uninstallTimestampDAO.getLastUninstallTimestamp()

        // Validation - exact timestamp should be returned
        assertEquals(expectedTime, result)
    }

    @Test
    fun test_getLastUninstallTimestamp_when_multipleTimestamps_should_returnLatest() {
        val firstTime = 1609459200000L // January 1, 2021 00:00:00 UTC
        val secondTime = 1609545600000L // January 2, 2021 00:00:00 UTC
        
        // Store first timestamp
        testClock.setCurrentTime(firstTime)
        uninstallTimestampDAO.storeUninstallTimestamp()
        val firstTimestamp = uninstallTimestampDAO.getLastUninstallTimestamp()
        assertEquals(firstTime, firstTimestamp)
        
        // Store second timestamp with later time
        testClock.setCurrentTime(secondTime)
        uninstallTimestampDAO.storeUninstallTimestamp()
        val secondTimestamp = uninstallTimestampDAO.getLastUninstallTimestamp()
        
        // Should return the latest timestamp
        assertEquals(secondTime, secondTimestamp)
        assertTrue(secondTimestamp > firstTimestamp)
    }
}
