package com.clevertap.android.sdk.db

import android.content.Context
import android.database.sqlite.SQLiteException
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.Utils
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DelayedLegacyInAppDAOImplTest {

    private lateinit var testClock: TestClock
    private lateinit var delayedInAppDAO: DelayedLegacyInAppDAO
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var logger: Logger
    private lateinit var table: Table
    private lateinit var context: Context
    private lateinit var config: CleverTapInstanceConfig

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    private val testInAppId1 = "inapp_12345"
    private val testInAppId2 = "inapp_67890"
    private val testDelay1 = 300 // 5 minutes in seconds
    private val testDelay2 = 600 // 10 minutes in seconds
    private val testInAppData1 = """{"ti":"Test InApp 1","type":"interstitial"}"""
    private val testInAppData2 = """{"ti":"Test InApp 2","type":"alert"}"""

    private val MOCK_TIME = 1609459200000L // 2021-01-01 00:00:00

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        logger = mockk(relaxed = true)
        table = Table.DELAYED_LEGACY_INAPPS
        config = CleverTapInstanceConfig.createInstance(
            context,
            accID,
            accToken,
            accRegion
        )

        databaseHelper = DatabaseHelper(context, config.accountId, "test_ct_delayed_inapps", logger)
        testClock = spyk(TestClock(MOCK_TIME))
        delayedInAppDAO = DelayedLegacyInAppDAOImpl(databaseHelper, logger, table, testClock)
    }

    @After
    fun tearDown() {
        unmockkStatic(Utils::class)
        databaseHelper.close()
        context.deleteDatabase("test_ct_delayed_inapps")
    }

    // ============================================
    // INSERT BATCH TESTS
    // ============================================

    @Test
    fun `test insertBatch returns true for empty list`() {
        // Given
        val emptyList = emptyList<DelayedLegacyInAppData>()

        // When
        val result = delayedInAppDAO.insertBatch(emptyList)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test insertBatch successfully inserts multiple entries`() {
        // Given
        val batchData = listOf(
            DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1),
            DelayedLegacyInAppData(testInAppId2, testDelay2, testInAppData2)
        )

        // When
        val result = delayedInAppDAO.insertBatch(batchData)

        // Then
        assertTrue(result)
        assertNotNull(delayedInAppDAO.fetchSingleInApp(testInAppId1))
        assertNotNull(delayedInAppDAO.fetchSingleInApp(testInAppId2))
    }

    @Test
    fun `test insertBatch all entries have same timestamp`() {
        // Given
        val batchData = listOf(
            DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1),
            DelayedLegacyInAppData(testInAppId2, testDelay2, testInAppData2)
        )

        // When
        delayedInAppDAO.insertBatch(batchData)

        verify { testClock.currentTimeMillis() }
    }

    @Test
    fun `test insertBatch replaces existing entries with same inAppIds`() {
        // Given
        val data1 = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        delayedInAppDAO.insertBatch(listOf(data1))

        val batchData = listOf(
            DelayedLegacyInAppData(testInAppId1, testDelay2, testInAppData2),
            DelayedLegacyInAppData(testInAppId2, testDelay1, testInAppData1)
        )

        // When
        val result = delayedInAppDAO.insertBatch(batchData)

        // Then
        assertTrue(result)
        assertEquals(testInAppData2, delayedInAppDAO.fetchSingleInApp(testInAppId1))
        assertEquals(testInAppData1, delayedInAppDAO.fetchSingleInApp(testInAppId2))
    }

    @Test
    fun `test insertBatch returns false when memory threshold exceeded`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.belowMemThreshold() } returns false
        val dao = DelayedLegacyInAppDAOImpl(dbHelper, logger, table)
        val batchData = listOf(
            DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        )

        // When
        val result = dao.insertBatch(batchData)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test insertBatch returns false and ends transaction when exception occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.belowMemThreshold() } returns true
        every { dbHelper.writableDatabase.beginTransaction() } throws SQLiteException()
        val dao = DelayedLegacyInAppDAOImpl(dbHelper, logger, table)
        val batchData = listOf(
            DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        )

        // When
        val result = dao.insertBatch(batchData)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test insertBatch with large batch size`() {
        // Given
        val largeBatch = (1..100).map { index ->
            DelayedLegacyInAppData(
                "inapp_$index",
                index * 10,
                """{"ti":"InApp $index"}"""
            )
        }

        // When
        val result = delayedInAppDAO.insertBatch(largeBatch)

        // Then
        assertTrue(result)
        // Verify random samples
        assertNotNull(delayedInAppDAO.fetchSingleInApp("inapp_1"))
        assertNotNull(delayedInAppDAO.fetchSingleInApp("inapp_50"))
        assertNotNull(delayedInAppDAO.fetchSingleInApp("inapp_100"))
    }

    // ============================================
    // REMOVE TESTS
    // ============================================

    @Test
    fun `test remove returns false when inAppId does not exist`() {
        // When
        val result = delayedInAppDAO.remove(testInAppId1)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test remove returns true when entry exists`() {
        // Given
        val data = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        delayedInAppDAO.insertBatch(listOf(data))

        // When
        val result = delayedInAppDAO.remove(testInAppId1)

        // Then
        assertTrue(result)
        assertNull(delayedInAppDAO.fetchSingleInApp(testInAppId1))
    }

    @Test
    fun `test remove deletes only specified inAppId`() {
        // Given
        val data1 = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        val data2 = DelayedLegacyInAppData(testInAppId2, testDelay2, testInAppData2)
        delayedInAppDAO.insertBatch(listOf(data1))
        delayedInAppDAO.insertBatch(listOf(data2))

        // When
        val result = delayedInAppDAO.remove(testInAppId1)

        // Then
        assertTrue(result)
        assertNull(delayedInAppDAO.fetchSingleInApp(testInAppId1))
        assertNotNull(delayedInAppDAO.fetchSingleInApp(testInAppId2))
    }

    @Test
    fun `test remove returns false when database exception occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.writableDatabase.delete(any(), any(), any()) } throws SQLiteException()
        val dao = DelayedLegacyInAppDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.remove(testInAppId1)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test remove multiple times on same inAppId`() {
        // Given
        val data = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        delayedInAppDAO.insertBatch(listOf(data))

        // When
        val firstRemove = delayedInAppDAO.remove(testInAppId1)
        val secondRemove = delayedInAppDAO.remove(testInAppId1)

        // Then
        assertTrue(firstRemove)
        assertFalse(secondRemove)
    }

    // ============================================
    // FETCH SINGLE INAPP TESTS
    // ============================================

    @Test
    fun `test fetchSingleInApp returns null when inAppId does not exist`() {
        // When
        val result = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then
        assertNull(result)
    }

    @Test
    fun `test fetchSingleInApp returns correct data after insert`() {
        // Given
        val data = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        delayedInAppDAO.insertBatch(listOf(data))

        // When
        val result = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then
        assertNotNull(result)
        assertEquals(testInAppData1, result)
    }

    @Test
    fun `test fetchSingleInApp returns updated data after replace`() {
        // Given
        val data1 = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        val data2 = DelayedLegacyInAppData(testInAppId1, testDelay2, testInAppData2)
        delayedInAppDAO.insertBatch(listOf(data1))
        delayedInAppDAO.insertBatch(listOf(data2))

        // When
        val result = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then
        assertEquals(testInAppData2, result)
    }

    @Test
    fun `test fetchSingleInApp returns null after remove`() {
        // Given
        val data = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        delayedInAppDAO.insertBatch(listOf(data))
        delayedInAppDAO.remove(testInAppId1)

        // When
        val result = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then
        assertNull(result)
    }

    @Test
    fun `test fetchSingleInApp returns null when database exception occurs`() {
        // Given
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every {
            dbHelper.readableDatabase.query(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws SQLiteException()
        val dao = DelayedLegacyInAppDAOImpl(dbHelper, logger, table)

        // When
        val result = dao.fetchSingleInApp(testInAppId1)

        // Then
        assertNull(result)
    }

    @Test
    fun `test fetchSingleInApp retrieves correct entry when multiple exist`() {
        // Given
        val data1 = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)
        val data2 = DelayedLegacyInAppData(testInAppId2, testDelay2, testInAppData2)
        delayedInAppDAO.insertBatch(listOf(data1))
        delayedInAppDAO.insertBatch(listOf(data2))

        // When
        val result1 = delayedInAppDAO.fetchSingleInApp(testInAppId1)
        val result2 = delayedInAppDAO.fetchSingleInApp(testInAppId2)

        // Then
        assertEquals(testInAppData1, result1)
        assertEquals(testInAppData2, result2)
    }

    @Test
    fun `test fetchSingleInApp with complex JSON data`() {
        // Given
        val complexJsonData = """
            {
                "ti":"Complex InApp",
                "type":"interstitial",
                "bg":"#FFFFFF",
                "html":"<html><body>Test</body></html>",
                "w":100,
                "h":100,
                "buttons":[
                    {"text":"OK","action":"close"},
                    {"text":"Cancel","action":"dismiss"}
                ]
            }
        """.trimIndent()
        val data = DelayedLegacyInAppData(testInAppId1, testDelay1, complexJsonData)
        delayedInAppDAO.insertBatch(listOf(data))

        // When
        val result = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then
        assertNotNull(result)
        assertEquals(complexJsonData, result)
    }

    // ============================================
    // INTEGRATION TESTS
    // ============================================

    @Test
    fun `test complete lifecycle - insert, fetch, remove`() {
        // Given
        val data = DelayedLegacyInAppData(testInAppId1, testDelay1, testInAppData1)

        // When - Insert
        val insertResult = delayedInAppDAO.insertBatch(listOf(data))
        val fetchAfterInsert = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then - Insert successful
        assertTrue(insertResult)
        assertEquals(testInAppData1, fetchAfterInsert)

        // When - Remove
        val removeResult = delayedInAppDAO.remove(testInAppId1)
        val fetchAfterRemove = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then - Remove successful
        assertTrue(removeResult)
        assertNull(fetchAfterRemove)
    }

    @Test
    fun `test batch operations workflow`() {
        // Given
        val batchData = listOf(
            DelayedLegacyInAppData("inapp_1", 100, """{"ti":"InApp 1"}"""),
            DelayedLegacyInAppData("inapp_2", 200, """{"ti":"InApp 2"}"""),
            DelayedLegacyInAppData("inapp_3", 300, """{"ti":"InApp 3"}""")
        )

        // When - Batch insert
        val insertResult = delayedInAppDAO.insertBatch(batchData)

        // Then - All inserted successfully
        assertTrue(insertResult)

        // When - Remove some entries
        val remove1 = delayedInAppDAO.remove("inapp_1")
        val remove3 = delayedInAppDAO.remove("inapp_3")

        // Then - Removals successful, inapp_2 still exists
        assertTrue(remove1)
        assertTrue(remove3)
        assertNull(delayedInAppDAO.fetchSingleInApp("inapp_1"))
        assertNotNull(delayedInAppDAO.fetchSingleInApp("inapp_2"))
        assertNull(delayedInAppDAO.fetchSingleInApp("inapp_3"))
    }

    @Test
    fun `test special characters in inAppId are handled correctly`() {
        // Given
        val specialInAppId = "inapp_test-123_abc@xyz"
        val data = DelayedLegacyInAppData(specialInAppId, testDelay1, testInAppData1)

        // When
        val insertResult = delayedInAppDAO.insertBatch(listOf(data))
        val fetchedData = delayedInAppDAO.fetchSingleInApp(specialInAppId)

        // Then
        assertTrue(insertResult)
        assertEquals(testInAppData1, fetchedData)
    }

    @Test
    fun `test Unicode characters in JSON data are preserved`() {
        // Given
        val unicodeJson = """{"ti":"测试 InApp 🚀","emoji":"✓"}"""
        val data = DelayedLegacyInAppData(testInAppId1, testDelay1, unicodeJson)

        // When
        delayedInAppDAO.insertBatch(listOf(data))
        val fetchedData = delayedInAppDAO.fetchSingleInApp(testInAppId1)

        // Then
        assertEquals(unicodeJson, fetchedData)
    }

    @Test
    fun `test clearAll`() {
        // Given - Both legacy and delayed in-apps exist
        val allInApps = (1..20).map { index ->
            DelayedLegacyInAppData("inapp_$index", index * 30, """{"ti":"InApp $index"}""")
        }
        delayedInAppDAO.insertBatch(allInApps)

        // When - NO_MODE is activated, everything should be cleared
        val clearResult = delayedInAppDAO.clearAll()

        // Then
        assertTrue(clearResult)
        // Verify all are gone
        (1..20).forEach { index ->
            assertNull(delayedInAppDAO.fetchSingleInApp("inapp_$index"))
        }
    }
}