package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InActionStorageStrategyTest {

    private lateinit var mockLogger: ILogger
    private lateinit var strategy: InActionStorageStrategy
    private val accountId = "test_account_id"

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        strategy = InActionStorageStrategy(mockLogger, accountId)
    }

    // ==================== PREPARE FOR SCHEDULING TESTS ====================

    @Test
    fun `prepareForScheduling always returns true`() {
        // Arrange
        val inApps = listOf(createInApp("inapp1"))

        // Act
        val result = strategy.prepareForScheduling(inApps)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `prepareForScheduling returns true for empty list`() {
        // Arrange
        val inApps = emptyList<JSONObject>()

        // Act
        val result = strategy.prepareForScheduling(inApps)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `prepareForScheduling caches in-apps correctly`() {
        // Arrange
        val inApp1 = createInApp("inapp1")
        val inApp2 = createInApp("inapp2")
        val inApps = listOf(inApp1, inApp2)

        // Act
        strategy.prepareForScheduling(inApps)

        // Assert
        val retrieved1 = strategy.retrieveAfterTimer("inapp1")
        val retrieved2 = strategy.retrieveAfterTimer("inapp2")
        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals("inapp1", retrieved1?.getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals("inapp2", retrieved2?.getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `prepareForScheduling skips in-apps with blank id`() {
        // Arrange
        val inAppWithBlankId = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "")
        val inAppWithValidId = createInApp("valid_id")
        val inApps = listOf(inAppWithBlankId, inAppWithValidId)

        // Act
        strategy.prepareForScheduling(inApps)

        // Assert
        assertNull(strategy.retrieveAfterTimer(""))
        assertNotNull(strategy.retrieveAfterTimer("valid_id"))
    }

    @Test
    fun `prepareForScheduling skips in-apps without id field`() {
        // Arrange
        val inAppWithoutId = JSONObject().put("someOtherField", "value")
        val inAppWithValidId = createInApp("valid_id")
        val inApps = listOf(inAppWithoutId, inAppWithValidId)

        // Act
        strategy.prepareForScheduling(inApps)

        // Assert
        assertNotNull(strategy.retrieveAfterTimer("valid_id"))
    }

    @Test
    fun `prepareForScheduling overwrites existing in-app with same id`() {
        // Arrange
        val inAppOriginal = createInApp("same_id").put("version", 1)
        val inAppUpdated = createInApp("same_id").put("version", 2)

        // Act
        strategy.prepareForScheduling(listOf(inAppOriginal))
        strategy.prepareForScheduling(listOf(inAppUpdated))

        // Assert
        val retrieved = strategy.retrieveAfterTimer("same_id")
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.getInt("version"))
    }

    // ==================== RETRIEVE AFTER TIMER TESTS ====================

    @Test
    fun `retrieveAfterTimer returns cached in-app`() {
        // Arrange
        val inApp = createInApp("test_id")
        strategy.prepareForScheduling(listOf(inApp))

        // Act
        val result = strategy.retrieveAfterTimer("test_id")

        // Assert
        assertNotNull(result)
        assertEquals("test_id", result?.getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `retrieveAfterTimer returns null for non-existent id`() {
        // Arrange - don't prepare anything

        // Act
        val result = strategy.retrieveAfterTimer("non_existent")

        // Assert
        assertNull(result)
    }

    @Test
    fun `retrieveAfterTimer returns full JSONObject with all fields`() {
        // Arrange
        val inApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "full_inapp")
            .put("title", "Test Title")
            .put("message", "Test Message")
            .put("priority", 100)
        strategy.prepareForScheduling(listOf(inApp))

        // Act
        val result = strategy.retrieveAfterTimer("full_inapp")

        // Assert
        assertNotNull(result)
        assertEquals("full_inapp", result?.getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals("Test Title", result?.getString("title"))
        assertEquals("Test Message", result?.getString("message"))
        assertEquals(100, result?.getInt("priority"))
    }

    @Test
    fun `retrieveAfterTimer does not remove in-app from cache`() {
        // Arrange
        val inApp = createInApp("persistent_id")
        strategy.prepareForScheduling(listOf(inApp))

        // Act - retrieve multiple times
        val result1 = strategy.retrieveAfterTimer("persistent_id")
        val result2 = strategy.retrieveAfterTimer("persistent_id")
        val result3 = strategy.retrieveAfterTimer("persistent_id")

        // Assert - all should return the same data
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
    }

    // ==================== CLEAR TESTS ====================

    @Test
    fun `clear removes specific in-app from cache`() {
        // Arrange
        val inApp1 = createInApp("inapp1")
        val inApp2 = createInApp("inapp2")
        strategy.prepareForScheduling(listOf(inApp1, inApp2))

        // Act
        strategy.clear("inapp1")

        // Assert
        assertNull(strategy.retrieveAfterTimer("inapp1"))
        assertNotNull(strategy.retrieveAfterTimer("inapp2"))
    }

    @Test
    fun `clear does not throw for non-existent id`() {
        // Act & Assert - should not throw
        strategy.clear("non_existent")
    }

    @Test
    fun `clear does not throw for empty id`() {
        // Act & Assert - should not throw
        strategy.clear("")
    }

    @Test
    fun `clear only removes specified in-app`() {
        // Arrange
        val inApps = (1..5).map { createInApp("inapp$it") }
        strategy.prepareForScheduling(inApps)

        // Act
        strategy.clear("inapp3")

        // Assert
        assertNotNull(strategy.retrieveAfterTimer("inapp1"))
        assertNotNull(strategy.retrieveAfterTimer("inapp2"))
        assertNull(strategy.retrieveAfterTimer("inapp3"))
        assertNotNull(strategy.retrieveAfterTimer("inapp4"))
        assertNotNull(strategy.retrieveAfterTimer("inapp5"))
    }

    @Test
    fun `clear can be called multiple times for same id`() {
        // Arrange
        val inApp = createInApp("inapp1")
        strategy.prepareForScheduling(listOf(inApp))

        // Act & Assert - should not throw
        strategy.clear("inapp1")
        strategy.clear("inapp1")
        strategy.clear("inapp1")

        assertNull(strategy.retrieveAfterTimer("inapp1"))
    }

    // ==================== CLEAR ALL TESTS ====================

    @Test
    fun `clearAll removes all in-apps from cache`() {
        // Arrange
        val inApps = (1..10).map { createInApp("inapp$it") }
        strategy.prepareForScheduling(inApps)

        // Act
        strategy.clearAll()

        // Assert
        (1..10).forEach { i ->
            assertNull(strategy.retrieveAfterTimer("inapp$i"))
        }
    }

    @Test
    fun `clearAll does not throw on empty cache`() {
        // Act & Assert - should not throw
        strategy.clearAll()
    }

    @Test
    fun `clearAll can be called multiple times`() {
        // Arrange
        val inApp = createInApp("inapp1")
        strategy.prepareForScheduling(listOf(inApp))

        // Act & Assert - should not throw
        strategy.clearAll()
        strategy.clearAll()
        strategy.clearAll()
    }

    @Test
    fun `clearAll allows new in-apps to be added after clearing`() {
        // Arrange
        val inApp1 = createInApp("inapp1")
        strategy.prepareForScheduling(listOf(inApp1))
        strategy.clearAll()

        // Act
        val inApp2 = createInApp("inapp2")
        strategy.prepareForScheduling(listOf(inApp2))

        // Assert
        assertNull(strategy.retrieveAfterTimer("inapp1"))
        assertNotNull(strategy.retrieveAfterTimer("inapp2"))
    }

    // ==================== LIFECYCLE SIMULATION TESTS ====================

    @Test
    fun `full lifecycle prepare retrieve clear works correctly`() {
        // Arrange
        val inApp = createInApp("lifecycle_test")

        // Act & Assert - simulate full lifecycle
        // Step 1: Prepare
        val prepareResult = strategy.prepareForScheduling(listOf(inApp))
        assertTrue(prepareResult)

        // Step 2: Retrieve (timer completed)
        val retrieved = strategy.retrieveAfterTimer("lifecycle_test")
        assertNotNull(retrieved)
        assertEquals("lifecycle_test", retrieved?.getString(Constants.INAPP_ID_IN_PAYLOAD))

        // Step 3: Clear (cleanup after processing)
        strategy.clear("lifecycle_test")
        assertNull(strategy.retrieveAfterTimer("lifecycle_test"))
    }

    @Test
    fun `multiple in-apps with independent lifecycles`() {
        // Arrange
        val inApp1 = createInApp("inapp1")
        val inApp2 = createInApp("inapp2")
        val inApp3 = createInApp("inapp3")

        // Act
        strategy.prepareForScheduling(listOf(inApp1, inApp2, inApp3))

        // Complete inapp1 lifecycle
        assertNotNull(strategy.retrieveAfterTimer("inapp1"))
        strategy.clear("inapp1")

        // Complete inapp2 lifecycle
        assertNotNull(strategy.retrieveAfterTimer("inapp2"))
        strategy.clear("inapp2")

        // Assert - inapp3 should still be available
        assertNull(strategy.retrieveAfterTimer("inapp1"))
        assertNull(strategy.retrieveAfterTimer("inapp2"))
        assertNotNull(strategy.retrieveAfterTimer("inapp3"))
    }

    // ==================== EDGE CASES TESTS ====================

    @Test
    fun `handles special characters in id`() {
        // Arrange
        val specialId = "inapp-123_test.special@id"
        val inApp = createInApp(specialId)

        // Act
        strategy.prepareForScheduling(listOf(inApp))

        // Assert
        assertNotNull(strategy.retrieveAfterTimer(specialId))
    }

    @Test
    fun `handles very long id`() {
        // Arrange
        val longId = "a".repeat(1000)
        val inApp = createInApp(longId)

        // Act
        strategy.prepareForScheduling(listOf(inApp))

        // Assert
        assertNotNull(strategy.retrieveAfterTimer(longId))
    }

    @Test
    fun `handles numeric id`() {
        // Arrange
        val numericId = "1234567890"
        val inApp = createInApp(numericId)

        // Act
        strategy.prepareForScheduling(listOf(inApp))

        // Assert
        assertNotNull(strategy.retrieveAfterTimer(numericId))
    }


    @Test
    fun `cache persists across multiple prepareForScheduling calls`() {
        // Arrange & Act
        strategy.prepareForScheduling(listOf(createInApp("batch1_inapp1")))
        strategy.prepareForScheduling(listOf(createInApp("batch2_inapp1")))
        strategy.prepareForScheduling(listOf(createInApp("batch3_inapp1")))

        // Assert - all should be available
        assertNotNull(strategy.retrieveAfterTimer("batch1_inapp1"))
        assertNotNull(strategy.retrieveAfterTimer("batch2_inapp1"))
        assertNotNull(strategy.retrieveAfterTimer("batch3_inapp1"))
    }

    // ==================== HELPER METHODS ====================

    private fun createInApp(id: String): JSONObject {
        return JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, id)
    }
}