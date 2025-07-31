package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
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

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbHelper = DatabaseHelper(appCtx, instanceConfig.accountId, "test_db", instanceConfig.logger)
        uninstallTimestampDAO = UninstallTimestampDAOImpl(dbHelper, instanceConfig.logger)
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun test_getLastUninstallTimestamp_when_FunctionIsCalled_should_ReturnTheLastUninstallTime() {
        // When no uninstall time is stored, should return 0
        assertEquals(0, uninstallTimestampDAO.getLastUninstallTimestamp())

        // Store current time as uninstall time
        val currentTime = System.currentTimeMillis()
        uninstallTimestampDAO.storeUninstallTimestamp()
        // TODO: This could be tested in a better way when DBAdapter can be provided with a test clock
        // Validation: the last uninstall timestamp is returned (can differ by 1-2 seconds based on processor speed, so taking a range in here of max 2 seconds)
        assertTrue(uninstallTimestampDAO.getLastUninstallTimestamp() in currentTime..(currentTime + 2000))
    }

    @Test
    fun test_getLastUninstallTimestamp_when_noTimestampStored_should_returnZero() {
        val result = uninstallTimestampDAO.getLastUninstallTimestamp()
        assertEquals(0, result)
    }

    @Test
    fun test_storeUninstallTimestamp_and_getLastUninstallTimestamp_should_work() {
        val currentTime = System.currentTimeMillis()
        uninstallTimestampDAO.storeUninstallTimestamp()
        val result = uninstallTimestampDAO.getLastUninstallTimestamp()

        // Validation - allowing for small time differences during test execution
        assertTrue(result >= currentTime)
        assertTrue(result <= currentTime + 2000) // Within 2 seconds
    }

    @Test
    fun test_getLastUninstallTimestamp_when_multipleTimestamps_should_returnLatest() {
        uninstallTimestampDAO.storeUninstallTimestamp()
        val firstTimestamp = uninstallTimestampDAO.getLastUninstallTimestamp()
        
        Thread.sleep(10) // Small delay to ensure different timestamps
        
        uninstallTimestampDAO.storeUninstallTimestamp()
        val secondTimestamp = uninstallTimestampDAO.getLastUninstallTimestamp()

        assertTrue(secondTimestamp >= firstTimestamp)
    }
}
