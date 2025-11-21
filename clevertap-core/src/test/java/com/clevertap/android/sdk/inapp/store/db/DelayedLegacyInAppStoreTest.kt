package com.clevertap.android.sdk.inapp.store.db

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.DelayedLegacyInAppDAO
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DelayedLegacyInAppStoreTest {

    private lateinit var store: DelayedLegacyInAppStore
    private lateinit var mockDAO: DelayedLegacyInAppDAO
    private lateinit var mockCryptHandler: CryptHandler
    private lateinit var mockLogger: ILogger

    private val accountId = "test_account_id"
    private val testInAppId = "inapp_123"
    private val testDelay = 300

    private fun createTestInApp(id: String = testInAppId, delay: Int = testDelay): JSONObject {
        return JSONObject().apply {
            put(Constants.INAPP_ID_IN_PAYLOAD, id)
            put(INAPP_DELAY_AFTER_TRIGGER, delay)
            put("type", "interstitial")
            put("wzrk_id", "wzrk_$id")
        }
    }

    @Before
    fun setUp() {
        mockDAO = mockk(relaxed = true)
        mockCryptHandler = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        store = DelayedLegacyInAppStore(
            mockDAO,
            mockCryptHandler,
            mockLogger,
            accountId
        )
    }

    // ============================================
    // SAVE DELAYED INAPPS BATCH TESTS
    // ============================================

    @Test
    fun `test saveDelayedInAppsBatch returns true for empty array`() {
        // Given
        val emptyArray = JSONArray()

        // When
        val result = store.saveDelayedInAppsBatch(emptyArray)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test saveDelayedInAppsBatch encrypts and saves single inapp`() {
        // Given
        val inApp = createTestInApp()
        val inAppsArray = JSONArray().apply { put(inApp) }
        val encryptedData = "encrypted_data"

        every { mockCryptHandler.encrypt(any()) } returns encryptedData
        every { mockDAO.insertBatch(any()) } returns true

        // When
        val result = store.saveDelayedInAppsBatch(inAppsArray)

        // Then
        assertTrue(result)
        verify(exactly = 1) { mockCryptHandler.encrypt(inApp.toString()) }
        verify(exactly = 1) {
            mockDAO.insertBatch(match { list ->
                list.size == 1 &&
                        list[0].inAppId == testInAppId &&
                        list[0].delay == testDelay &&
                        list[0].inAppData == encryptedData
            })
        }
    }

    @Test
    fun `test saveDelayedInAppsBatch encrypts and saves multiple inapps`() {
        // Given
        val inApp1 = createTestInApp("inapp_1", 300)
        val inApp2 = createTestInApp("inapp_2", 600)
        val inApp3 = createTestInApp("inapp_3", 900)
        val inAppsArray = JSONArray().apply {
            put(inApp1)
            put(inApp2)
            put(inApp3)
        }

        every { mockCryptHandler.encrypt(any()) } returns "encrypted_data"
        every { mockDAO.insertBatch(any()) } returns true

        // When
        val result = store.saveDelayedInAppsBatch(inAppsArray)

        // Then
        assertTrue(result)
        verify(exactly = 3) { mockCryptHandler.encrypt(any()) }
        verify(exactly = 1) {
            mockDAO.insertBatch(match { list -> list.size == 3 })
        }
    }

    @Test
    fun `test saveDelayedInAppsBatch skips items with encryption failure`() {
        // Given
        val inApp1 = createTestInApp("inapp_1", 300)
        val inApp2 = createTestInApp("inapp_2", 600)
        val inApp3 = createTestInApp("inapp_3", 900)
        val inAppsArray = JSONArray().apply {
            put(inApp1)
            put(inApp2)
            put(inApp3)
        }

        // Encrypt first and third successfully, fail second
        every { mockCryptHandler.encrypt(inApp1.toString()) } returns "encrypted_1"
        every { mockCryptHandler.encrypt(inApp2.toString()) } returns null
        every { mockCryptHandler.encrypt(inApp3.toString()) } returns "encrypted_3"
        every { mockDAO.insertBatch(any()) } returns true

        // When
        val result = store.saveDelayedInAppsBatch(inAppsArray)

        // Then
        assertTrue(result)
        verify(exactly = 3) { mockCryptHandler.encrypt(any()) }
        verify(exactly = 1) {
            mockDAO.insertBatch(match { list ->
                list.size == 2 &&
                        list[0].inAppId == "inapp_1" &&
                        list[1].inAppId == "inapp_3"
            })
        }
    }

    @Test
    fun `test saveDelayedInAppsBatch returns false when all items fail encryption`() {
        // Given
        val inApp1 = createTestInApp("inapp_1", 300)
        val inApp2 = createTestInApp("inapp_2", 600)
        val inAppsArray = JSONArray().apply {
            put(inApp1)
            put(inApp2)
        }

        every { mockCryptHandler.encrypt(any()) } returns null

        // When
        val result = store.saveDelayedInAppsBatch(inAppsArray)

        // Then
        assertFalse(result)
        verify(exactly = 2) { mockCryptHandler.encrypt(any()) }
        verify(exactly = 0) { mockDAO.insertBatch(any()) }
    }

    @Test
    fun `test saveDelayedInAppsBatch returns false when DAO insertBatch fails`() {
        // Given
        val inApp = createTestInApp()
        val inAppsArray = JSONArray().apply { put(inApp) }

        every { mockCryptHandler.encrypt(any()) } returns "encrypted_data"
        every { mockDAO.insertBatch(any()) } returns false

        // When
        val result = store.saveDelayedInAppsBatch(inAppsArray)

        // Then
        assertFalse(result)
        verify(exactly = 1) { mockCryptHandler.encrypt(any()) }
        verify(exactly = 1) { mockDAO.insertBatch(any()) }
    }

    @Test
    fun `test saveDelayedInAppsBatch extracts correct fields from JSON`() {
        // Given
        val inAppJson = JSONObject().apply {
            put(Constants.INAPP_ID_IN_PAYLOAD, "test_id_123")
            put(INAPP_DELAY_AFTER_TRIGGER, 450)
            put("type", "alert")
        }
        val inAppsArray = JSONArray().apply { put(inAppJson) }

        every { mockCryptHandler.encrypt(any()) } returns "encrypted"
        every { mockDAO.insertBatch(any()) } returns true

        // When
        store.saveDelayedInAppsBatch(inAppsArray)

        // Then
        verify {
            mockDAO.insertBatch(match { list ->
                list.size == 1 &&
                        list[0].inAppId == "test_id_123" &&
                        list[0].delay == 450
            })
        }
    }

    @Test
    fun `test saveDelayedInAppsBatch with large batch size`() {
        // Given
        val inAppsArray = JSONArray()
        repeat(100) { index ->
            inAppsArray.put(createTestInApp("inapp_$index", index * 10))
        }

        every { mockCryptHandler.encrypt(any()) } returns "encrypted"
        every { mockDAO.insertBatch(any()) } returns true

        // When
        val result = store.saveDelayedInAppsBatch(inAppsArray)

        // Then
        assertTrue(result)
        verify(exactly = 100) { mockCryptHandler.encrypt(any()) }
        verify(exactly = 1) {
            mockDAO.insertBatch(match { list -> list.size == 100 })
        }
    }


    // ============================================
    // GET DELAYED INAPP TESTS
    // ============================================

    @Test
    fun `test getDelayedInApp returns null when inapp not found in database`() {
        // Given
        every { mockDAO.fetchSingleInApp(testInAppId) } returns null

        // When
        val result = store.getDelayedInApp(testInAppId)

        // Then
        assertNull(result)
        verify(exactly = 1) { mockDAO.fetchSingleInApp(testInAppId) }
        verify(exactly = 0) { mockCryptHandler.decrypt(any()) }
    }

    @Test
    fun `test getDelayedInApp decrypts and returns JSONObject`() {
        // Given
        val inAppJson = createTestInApp()
        val encryptedData = "encrypted_data"
        val decryptedData = inAppJson.toString()

        every { mockDAO.fetchSingleInApp(testInAppId) } returns encryptedData
        every { mockCryptHandler.decrypt(encryptedData) } returns decryptedData

        // When
        val result = store.getDelayedInApp(testInAppId)

        // Then
        assertNotNull(result)
        assertEquals(testInAppId, result?.getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals(testDelay, result?.getInt(INAPP_DELAY_AFTER_TRIGGER))
        verify(exactly = 1) { mockDAO.fetchSingleInApp(testInAppId) }
        verify(exactly = 1) { mockCryptHandler.decrypt(encryptedData) }
    }

    @Test
    fun `test getDelayedInApp returns null when decryption fails`() {
        // Given
        val encryptedData = "encrypted_data"

        every { mockDAO.fetchSingleInApp(testInAppId) } returns encryptedData
        every { mockCryptHandler.decrypt(encryptedData) } returns null

        // When
        val result = store.getDelayedInApp(testInAppId)

        // Then
        assertNull(result)
        verify(exactly = 1) { mockDAO.fetchSingleInApp(testInAppId) }
        verify(exactly = 1) { mockCryptHandler.decrypt(encryptedData) }
    }

    @Test
    fun `test getDelayedInApp returns null when JSON parsing fails`() {
        // Given
        val encryptedData = "encrypted_data"
        val invalidJsonData = "not a valid json"

        every { mockDAO.fetchSingleInApp(testInAppId) } returns encryptedData
        every { mockCryptHandler.decrypt(encryptedData) } returns invalidJsonData

        // When
        val result = store.getDelayedInApp(testInAppId)

        // Then
        assertNull(result)
        verify(exactly = 1) { mockDAO.fetchSingleInApp(testInAppId) }
        verify(exactly = 1) { mockCryptHandler.decrypt(encryptedData) }
    }

    // ============================================
    // REMOVE DELAYED INAPP TESTS
    // ============================================

    @Test
    fun `test removeDelayedInApp returns true when removal succeeds`() {
        // Given
        every { mockDAO.remove(testInAppId) } returns true

        // When
        val result = store.removeDelayedInApp(testInAppId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { mockDAO.remove(testInAppId) }
    }

    @Test
    fun `test removeDelayedInApp returns false when removal fails`() {
        // Given
        every { mockDAO.remove(testInAppId) } returns false

        // When
        val result = store.removeDelayedInApp(testInAppId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { mockDAO.remove(testInAppId) }
    }

    // ============================================
    // REMOVE DELAYED INAPPS BATCH TESTS
    // ============================================

    @Test
    fun `test removeDelayedInAppsBatch returns 0 for empty list`() {
        // Given
        val emptyList = emptyList<String>()

        // When
        val result = store.removeDelayedInAppsBatch(emptyList)

        // Then
        assertEquals(0, result)
        verify(exactly = 0) { mockDAO.remove(any()) }
    }

    @Test
    fun `test removeDelayedInAppsBatch removes single inapp`() {
        // Given
        val inAppIds = listOf("inapp_1")
        every { mockDAO.remove("inapp_1") } returns true

        // When
        val result = store.removeDelayedInAppsBatch(inAppIds)

        // Then
        assertEquals(1, result)
        verify(exactly = 1) { mockDAO.remove("inapp_1") }
    }

    @Test
    fun `test removeDelayedInAppsBatch removes multiple inapps`() {
        // Given
        val inAppIds = listOf("inapp_1", "inapp_2", "inapp_3")
        every { mockDAO.remove(any()) } returns true

        // When
        val result = store.removeDelayedInAppsBatch(inAppIds)

        // Then
        assertEquals(3, result)
        verify(exactly = 1) { mockDAO.remove("inapp_1") }
        verify(exactly = 1) { mockDAO.remove("inapp_2") }
        verify(exactly = 1) { mockDAO.remove("inapp_3") }
    }

    @Test
    fun `test removeDelayedInAppsBatch counts only successful removals`() {
        // Given
        val inAppIds = listOf("inapp_1", "inapp_2", "inapp_3", "inapp_4")
        every { mockDAO.remove("inapp_1") } returns true
        every { mockDAO.remove("inapp_2") } returns false
        every { mockDAO.remove("inapp_3") } returns true
        every { mockDAO.remove("inapp_4") } returns false

        // When
        val result = store.removeDelayedInAppsBatch(inAppIds)

        // Then
        assertEquals(2, result)
        verify(exactly = 4) { mockDAO.remove(any()) }
    }

    // ============================================
    // HAS DELAYED INAPP TESTS
    // ============================================

    @Test
    fun `test hasDelayedInApp returns true when inapp exists`() {
        // Given
        every { mockDAO.fetchSingleInApp(testInAppId) } returns "encrypted_data"

        // When
        val result = store.hasDelayedInApp(testInAppId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { mockDAO.fetchSingleInApp(testInAppId) }
    }

    @Test
    fun `test hasDelayedInApp returns false when inapp does not exist`() {
        // Given
        every { mockDAO.fetchSingleInApp(testInAppId) } returns null

        // When
        val result = store.hasDelayedInApp(testInAppId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { mockDAO.fetchSingleInApp(testInAppId) }
    }
}