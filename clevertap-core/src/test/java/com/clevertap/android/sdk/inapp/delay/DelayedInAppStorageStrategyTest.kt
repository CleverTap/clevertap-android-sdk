package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DelayedInAppStorageStrategyTest {

    private lateinit var mockLogger: ILogger
    private lateinit var mockDelayedLegacyInAppStore: DelayedLegacyInAppStore
    private val accountId = "test_account_id"

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        mockDelayedLegacyInAppStore = mockk(relaxed = true)
    }

    // ==================== PREPARE FOR SCHEDULING TESTS ====================

    @Test
    fun `prepareForScheduling returns false when delayedLegacyInAppStore is null`() {
        // Arrange
        val strategy = DelayedInAppStorageStrategy(accountId, mockLogger, null)
        val inApps = listOf(createInApp("inapp1"))

        // Act
        val result = strategy.prepareForScheduling(inApps)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `prepareForScheduling returns true when store saves successfully`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)
        val inApps = listOf(createInApp("inapp1"), createInApp("inapp2"))

        every { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(inApps) } returns true

        // Act
        val result = strategy.prepareForScheduling(inApps)

        // Assert
        assertTrue(result)
        verify { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(inApps) }
    }

    @Test
    fun `prepareForScheduling returns false when store save fails`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)
        val inApps = listOf(createInApp("inapp1"))

        every { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(inApps) } returns false

        // Act
        val result = strategy.prepareForScheduling(inApps)

        // Assert
        assertFalse(result)
        verify { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(inApps) }
    }

    @Test
    fun `prepareForScheduling passes correct list to store`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)
        val inApp1 = createInApp("inapp1")
        val inApp2 = createInApp("inapp2")
        val inApp3 = createInApp("inapp3")
        val inApps = listOf(inApp1, inApp2, inApp3)

        val capturedList = slot<List<JSONObject>>()
        every { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(capture(capturedList)) } returns true

        // Act
        strategy.prepareForScheduling(inApps)

        // Assert
        assertEquals(3, capturedList.captured.size)
        assertEquals("inapp1", capturedList.captured[0].getString("id"))
        assertEquals("inapp2", capturedList.captured[1].getString("id"))
        assertEquals("inapp3", capturedList.captured[2].getString("id"))
    }

    @Test
    fun `prepareForScheduling handles empty list`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)
        val inApps = emptyList<JSONObject>()

        every { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(inApps) } returns true

        // Act
        val result = strategy.prepareForScheduling(inApps)

        // Assert
        assertTrue(result)
        verify { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(inApps) }
    }

    // ==================== RETRIEVE AFTER TIMER TESTS ====================

    @Test
    fun `retrieveAfterTimer returns null when delayedLegacyInAppStore is null`() {
        // Arrange
        val strategy = DelayedInAppStorageStrategy(accountId, mockLogger, null)

        // Act
        val result = strategy.retrieveAfterTimer("inapp1")

        // Assert
        assertNull(result)
    }

    @Test
    fun `retrieveAfterTimer returns JSONObject when store finds in-app`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)
        val expectedInApp = createInApp("inapp1")

        every { mockDelayedLegacyInAppStore.getDelayedInApp("inapp1") } returns expectedInApp

        // Act
        val result = strategy.retrieveAfterTimer("inapp1")

        // Assert
        assertNotNull(result)
        assertEquals("inapp1", result?.getString("id"))
        verify { mockDelayedLegacyInAppStore.getDelayedInApp("inapp1") }
    }

    @Test
    fun `retrieveAfterTimer returns null when store does not find in-app`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)

        every { mockDelayedLegacyInAppStore.getDelayedInApp("nonexistent") } returns null

        // Act
        val result = strategy.retrieveAfterTimer("nonexistent")

        // Assert
        assertNull(result)
        verify { mockDelayedLegacyInAppStore.getDelayedInApp("nonexistent") }
    }

    // ==================== CLEAR TESTS ====================

    @Test
    fun `clear does not throw when delayedLegacyInAppStore is null`() {
        // Arrange
        val strategy = DelayedInAppStorageStrategy(accountId, mockLogger, null)

        // Act & Assert - should not throw
        strategy.clear("inapp1")
    }

    @Test
    fun `clear calls removeDelayedInApp on store`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)

        every { mockDelayedLegacyInAppStore.removeDelayedInApp("inapp1") } returns true

        // Act
        strategy.clear("inapp1")

        // Assert
        verify { mockDelayedLegacyInAppStore.removeDelayedInApp("inapp1") }
    }

    // ==================== CLEAR ALL TESTS ====================

    @Test
    fun `clearAll does not throw when delayedLegacyInAppStore is null`() {
        // Arrange
        val strategy = DelayedInAppStorageStrategy(accountId, mockLogger, null)

        // Act & Assert - should not throw
        strategy.clearAll()
    }

    @Test
    fun `clearAll calls removeAllDelayedInApps on store`() {
        // Arrange
        val strategy =
            DelayedInAppStorageStrategy(accountId, mockLogger, mockDelayedLegacyInAppStore)

        every { mockDelayedLegacyInAppStore.removeAllDelayedInApps() } returns true

        // Act
        strategy.clearAll()

        // Assert
        verify { mockDelayedLegacyInAppStore.removeAllDelayedInApps() }
    }

    // ==================== STORE ASSIGNMENT TESTS ====================

    @Test
    fun `delayedLegacyInAppStore can be updated after construction`() {
        // Arrange
        val strategy = DelayedInAppStorageStrategy(accountId, mockLogger, null)
        val inApps = listOf(createInApp("inapp1"))

        // Initially should return false
        val resultBefore = strategy.prepareForScheduling(inApps)
        assertFalse(resultBefore)

        // Act - assign store
        strategy.delayedLegacyInAppStore = mockDelayedLegacyInAppStore
        every { mockDelayedLegacyInAppStore.saveDelayedInAppsBatch(inApps) } returns true

        // Assert - should now work
        val resultAfter = strategy.prepareForScheduling(inApps)
        assertTrue(resultAfter)
    }

    // ==================== HELPER METHODS ====================

    private fun createInApp(id: String): JSONObject {
        return JSONObject().put("id", id)
    }
}