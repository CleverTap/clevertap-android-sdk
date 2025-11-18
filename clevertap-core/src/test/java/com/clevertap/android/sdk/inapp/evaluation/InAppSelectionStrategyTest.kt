package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InAppSelectionStrategyTest : BaseTestCase() {

    private lateinit var suppressionHandler: (JSONObject) -> Boolean
    private val suppressedInApps = mutableListOf<String>()

    @Before
    override fun setUp() {
        super.setUp()
        suppressedInApps.clear()

        // Setup suppression handler that tracks suppressed in-apps
        suppressionHandler = { inApp ->
            val isSuppressed = inApp.optBoolean(Constants.INAPP_SUPPRESSED, false)
            if (isSuppressed) {
                suppressedInApps.add(inApp.optString(Constants.INAPP_ID_IN_PAYLOAD))
            }
            isSuppressed
        }
    }

    // ==================== IMMEDIATE STRATEGY TESTS ====================

    @Test
    fun `Immediate strategy shouldUpdateTTL returns true`() {
        // Assert
        assertTrue(InAppSelectionStrategy.Immediate.shouldUpdateTTL())
    }

    @Test
    fun `Immediate strategy selects first non-suppressed in-app from sorted list`() {
        // Arrange
        val inApp1 = createInApp("inapp1", priority = 300, suppressed = false)
        val inApp2 = createInApp("inapp2", priority = 200, suppressed = false)
        val inApp3 = createInApp("inapp3", priority = 100, suppressed = false)

        val sortedInApps = listOf(inApp1, inApp2, inApp3)

        // Act
        val result = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(1, result.size)
        assertEquals("inapp1", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `Immediate strategy skips suppressed in-apps and returns first non-suppressed`() {
        // Arrange
        val suppressedInApp1 = createInApp("suppressed1", priority = 300, suppressed = true)
        val suppressedInApp2 = createInApp("suppressed2", priority = 200, suppressed = true)
        val normalInApp = createInApp("normal", priority = 100, suppressed = false)

        val sortedInApps = listOf(suppressedInApp1, suppressedInApp2, normalInApp)

        // Act
        val result = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(1, result.size)
        assertEquals("normal", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals(2, suppressedInApps.size)
        assertTrue(suppressedInApps.contains("suppressed1"))
        assertTrue(suppressedInApps.contains("suppressed2"))
    }

    @Test
    fun `Immediate strategy returns empty list when all in-apps are suppressed`() {
        // Arrange
        val suppressedInApp1 = createInApp("suppressed1", priority = 300, suppressed = true)
        val suppressedInApp2 = createInApp("suppressed2", priority = 200, suppressed = true)

        val sortedInApps = listOf(suppressedInApp1, suppressedInApp2)

        // Act
        val result = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(0, result.size)
        assertEquals(2, suppressedInApps.size)
    }

    @Test
    fun `Immediate strategy returns empty list when input list is empty`() {
        // Arrange
        val sortedInApps = emptyList<JSONObject>()

        // Act
        val result = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `Immediate strategy returns single in-app when only one in-app is not suppressed`() {
        // Arrange
        val inApp = createInApp("single", priority = 100, suppressed = false)
        val sortedInApps = listOf(inApp)

        // Act
        val result = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(1, result.size)
        assertEquals("single", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `Immediate strategy returns single suppressed in-app list when only one suppressed in-app exists`() {
        // Arrange
        val suppressedInApp = createInApp("suppressed", priority = 100, suppressed = true)
        val sortedInApps = listOf(suppressedInApp)

        // Act
        val result = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(0, result.size)
        assertEquals(1, suppressedInApps.size)
        assertTrue(suppressedInApps.contains("suppressed"))
    }

    @Test
    fun `Immediate strategy ignores delay values and treats all as immediate`() {
        // Arrange
        val inAppWithDelay10 = createDelayedInApp("delayed10", delay = 10, priority = 300, suppressed = false)
        val inAppWithDelay5 = createDelayedInApp("delayed5", delay = 5, priority = 200, suppressed = false)
        val inAppImmediate = createInApp("immediate", priority = 100, suppressed = false)

        val sortedInApps = listOf(inAppWithDelay10, inAppWithDelay5, inAppImmediate)

        // Act
        val result = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Should return first (highest priority) regardless of delay
        assertEquals(1, result.size)
        assertEquals("delayed10", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }


    // ==================== DELAYED STRATEGY TESTS ====================

    @Test
    fun `Delayed strategy shouldUpdateTTL returns false`() {
        // Assert
        assertFalse(InAppSelectionStrategy.Delayed.shouldUpdateTTL())
    }

    @Test
    fun `Delayed strategy groups in-apps by delay and returns one per group`() {
        // Arrange
        val inApp10s_high = createDelayedInApp("10s_high", delay = 10, priority = 300, suppressed = false)
        val inApp10s_low = createDelayedInApp("10s_low", delay = 10, priority = 200, suppressed = false)
        val inApp20s = createDelayedInApp("20s", delay = 20, priority = 250, suppressed = false)
        val inApp30s = createDelayedInApp("30s", delay = 30, priority = 150, suppressed = false)

        val sortedInApps = listOf(inApp10s_high, inApp10s_low, inApp20s, inApp30s)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Should return 3 in-apps, one per delay group (10s, 20s, 30s)
        assertEquals(3, result.size)

        val resultIds = result.map { it.getString(Constants.INAPP_ID_IN_PAYLOAD) }
        assertTrue(resultIds.contains("10s_high"))  // Highest priority in 10s group
        assertTrue(resultIds.contains("20s"))
        assertTrue(resultIds.contains("30s"))
        assertFalse(resultIds.contains("10s_low"))  // Lower priority in 10s group, should be skipped
    }

    @Test
    fun `Delayed strategy selects highest priority in-app per delay group`() {
        // Arrange - Three in-apps with same delay, different priorities
        val inApp10s_high = createDelayedInApp("10s_high", delay = 10, priority = 500, suppressed = false)
        val inApp10s_mid = createDelayedInApp("10s_mid", delay = 10, priority = 300, suppressed = false)
        val inApp10s_low = createDelayedInApp("10s_low", delay = 10, priority = 100, suppressed = false)

        val sortedInApps = listOf(inApp10s_high, inApp10s_mid, inApp10s_low)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Should return only the highest priority
        assertEquals(1, result.size)
        assertEquals("10s_high", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `Delayed strategy skips suppressed in-apps within delay groups`() {
        // Arrange
        val inApp10s_suppressed = createDelayedInApp("10s_suppressed", delay = 10, priority = 300, suppressed = true)
        val inApp10s_normal = createDelayedInApp("10s_normal", delay = 10, priority = 200, suppressed = false)
        val inApp20s_suppressed = createDelayedInApp("20s_suppressed", delay = 20, priority = 250, suppressed = true)
        val inApp20s_normal = createDelayedInApp("20s_normal", delay = 20, priority = 100, suppressed = false)

        val sortedInApps = listOf(inApp10s_suppressed, inApp10s_normal, inApp20s_suppressed, inApp20s_normal)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(2, result.size)

        val resultIds = result.map { it.getString(Constants.INAPP_ID_IN_PAYLOAD) }
        assertTrue(resultIds.contains("10s_normal"))
        assertTrue(resultIds.contains("20s_normal"))
        assertFalse(resultIds.contains("10s_suppressed"))
        assertFalse(resultIds.contains("20s_suppressed"))

        assertEquals(2, suppressedInApps.size)
    }

    @Test
    fun `Delayed strategy returns empty list when all in-apps in all delay groups are suppressed`() {
        // Arrange
        val inApp10s_suppressed = createDelayedInApp("10s_suppressed", delay = 10, priority = 300, suppressed = true)
        val inApp20s_suppressed = createDelayedInApp("20s_suppressed", delay = 20, priority = 200, suppressed = true)
        val inApp30s_suppressed = createDelayedInApp("30s_suppressed", delay = 30, priority = 100, suppressed = true)

        val sortedInApps = listOf(inApp10s_suppressed, inApp20s_suppressed, inApp30s_suppressed)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(0, result.size)
        assertEquals(3, suppressedInApps.size)
    }

    @Test
    fun `Delayed strategy handles delay group with all suppressed in-apps while other groups have valid in-apps`() {
        // Arrange
        val inApp10s_suppressed1 = createDelayedInApp("10s_suppressed1", delay = 10, priority = 300, suppressed = true)
        val inApp10s_suppressed2 = createDelayedInApp("10s_suppressed2", delay = 10, priority = 200, suppressed = true)
        val inApp20s_normal = createDelayedInApp("20s_normal", delay = 20, priority = 250, suppressed = false)

        val sortedInApps = listOf(inApp10s_suppressed1, inApp10s_suppressed2, inApp20s_normal)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Should return only from 20s group
        assertEquals(1, result.size)
        assertEquals("20s_normal", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals(2, suppressedInApps.size)
    }

    @Test
    fun `Delayed strategy returns empty list when input list is empty`() {
        // Arrange
        val sortedInApps = emptyList<JSONObject>()

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `Delayed strategy handles single in-app with delay`() {
        // Arrange
        val inApp = createDelayedInApp("single", delay = 15, priority = 100, suppressed = false)
        val sortedInApps = listOf(inApp)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(1, result.size)
        assertEquals("single", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `Delayed strategy treats missing delay field as delay 0 and groups accordingly`() {
        // Arrange
        val inAppNoDelay1 = createInApp("no_delay1", priority = 300, suppressed = false)
        val inAppNoDelay2 = createInApp("no_delay2", priority = 200, suppressed = false)
        val inApp10s = createDelayedInApp("delay10", delay = 10, priority = 250, suppressed = false)

        val sortedInApps = listOf(inAppNoDelay1, inAppNoDelay2, inApp10s)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Should return 2: one from delay=0 group and one from delay=10 group
        assertEquals(2, result.size)

        val resultIds = result.map { it.getString(Constants.INAPP_ID_IN_PAYLOAD) }
        assertTrue(resultIds.contains("no_delay1"))  // Highest priority in delay=0 group
        assertTrue(resultIds.contains("delay10"))
        assertFalse(resultIds.contains("no_delay2"))  // Lower priority in delay=0 group
    }

    @Test
    fun `Delayed strategy handles multiple delay groups with varying counts`() {
        // Arrange - 5s group: 1 in-app, 10s group: 3 in-apps, 15s group: 2 in-apps
        val inApp5s = createDelayedInApp("5s", delay = 5, priority = 400, suppressed = false)

        val inApp10s_high = createDelayedInApp("10s_high", delay = 10, priority = 300, suppressed = false)
        val inApp10s_mid = createDelayedInApp("10s_mid", delay = 10, priority = 200, suppressed = false)
        val inApp10s_low = createDelayedInApp("10s_low", delay = 10, priority = 100, suppressed = false)

        val inApp15s_high = createDelayedInApp("15s_high", delay = 15, priority = 250, suppressed = false)
        val inApp15s_low = createDelayedInApp("15s_low", delay = 15, priority = 150, suppressed = false)

        val sortedInApps = listOf(inApp5s, inApp10s_high, inApp10s_mid, inApp10s_low, inApp15s_high, inApp15s_low)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Should return 3: one from each delay group
        assertEquals(3, result.size)

        val resultIds = result.map { it.getString(Constants.INAPP_ID_IN_PAYLOAD) }
        assertTrue(resultIds.contains("5s"))
        assertTrue(resultIds.contains("10s_high"))
        assertTrue(resultIds.contains("15s_high"))
    }

    @Test
    fun `Delayed strategy maintains order of selected in-apps by delay group order in input`() {
        // Arrange - Input order: 10s, 20s, 30s
        val inApp10s = createDelayedInApp("10s", delay = 10, priority = 300, suppressed = false)
        val inApp20s = createDelayedInApp("20s", delay = 20, priority = 200, suppressed = false)
        val inApp30s = createDelayedInApp("30s", delay = 30, priority = 100, suppressed = false)

        val sortedInApps = listOf(inApp10s, inApp20s, inApp30s)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Order should be maintained based on first occurrence in input
        assertEquals(3, result.size)
        assertEquals("10s", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals("20s", result[1].getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals("30s", result[2].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `Delayed strategy handles negative delay values as separate groups`() {
        // Arrange - Testing edge case with negative delay (should be treated as distinct group)
        val inAppNegative = createDelayedInApp("negative", delay = -1, priority = 300, suppressed = false)
        val inApp0 = createDelayedInApp("zero", delay = 0, priority = 200, suppressed = false)
        val inApp10 = createDelayedInApp("ten", delay = 10, priority = 100, suppressed = false)

        val sortedInApps = listOf(inAppNegative, inApp0, inApp10)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert - Should return 3, one from each distinct delay group
        assertEquals(3, result.size)

        val resultIds = result.map { it.getString(Constants.INAPP_ID_IN_PAYLOAD) }
        assertTrue(resultIds.contains("negative"))
        assertTrue(resultIds.contains("zero"))
        assertTrue(resultIds.contains("ten"))
    }

    @Test
    fun `Delayed strategy handles very large delay values`() {
        // Arrange
        val inAppLargeDelay = createDelayedInApp("large", delay = 1200, priority = 300, suppressed = false)
        val inAppNormalDelay = createDelayedInApp("normal", delay = 10, priority = 200, suppressed = false)

        val sortedInApps = listOf(inAppLargeDelay, inAppNormalDelay)

        // Act
        val result = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(2, result.size)

        val resultIds = result.map { it.getString(Constants.INAPP_ID_IN_PAYLOAD) }
        assertTrue(resultIds.contains("large"))
        assertTrue(resultIds.contains("normal"))
    }

    // ==================== COMPARISON TESTS BETWEEN STRATEGIES ====================

    @Test
    fun `Immediate and Delayed strategies have opposite shouldUpdateTTL values`() {
        // Assert
        assertTrue(InAppSelectionStrategy.Immediate.shouldUpdateTTL())
        assertFalse(InAppSelectionStrategy.Delayed.shouldUpdateTTL())
    }

    @Test
    fun `Immediate strategy returns 1 in-app while Delayed returns multiple from same input`() {
        // Arrange
        val inApp10s_high = createDelayedInApp("10s_high", delay = 10, priority = 300, suppressed = false)
        val inApp10s_low = createDelayedInApp("10s_low", delay = 10, priority = 200, suppressed = false)
        val inApp20s = createDelayedInApp("20s", delay = 20, priority = 250, suppressed = false)

        val sortedInApps = listOf(inApp10s_high, inApp10s_low, inApp20s)

        // Act
        val immediateResult = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)
        suppressedInApps.clear()  // Reset for delayed test
        val delayedResult = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(1, immediateResult.size)
        assertEquals("10s_high", immediateResult[0].getString(Constants.INAPP_ID_IN_PAYLOAD))

        assertEquals(2, delayedResult.size)
        val delayedIds = delayedResult.map { it.getString(Constants.INAPP_ID_IN_PAYLOAD) }
        assertTrue(delayedIds.contains("10s_high"))
        assertTrue(delayedIds.contains("20s"))
    }

    @Test
    fun `Both strategies return empty list for empty input`() {
        // Arrange
        val sortedInApps = emptyList<JSONObject>()

        // Act
        val immediateResult = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)
        val delayedResult = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(0, immediateResult.size)
        assertEquals(0, delayedResult.size)
    }

    @Test
    fun `Both strategies return empty list when all in-apps are suppressed`() {
        // Arrange
        val suppressedInApp1 = createDelayedInApp("suppressed1", delay = 10, priority = 300, suppressed = true)
        val suppressedInApp2 = createDelayedInApp("suppressed2", delay = 20, priority = 200, suppressed = true)

        val sortedInApps = listOf(suppressedInApp1, suppressedInApp2)

        // Act
        val immediateResult = InAppSelectionStrategy.Immediate.selectInApps(sortedInApps, suppressionHandler)
        suppressedInApps.clear()  // Reset for delayed test
        val delayedResult = InAppSelectionStrategy.Delayed.selectInApps(sortedInApps, suppressionHandler)

        // Assert
        assertEquals(0, immediateResult.size)
        assertEquals(0, delayedResult.size)
    }

    // ==================== HELPER METHODS ====================

    private fun createInApp(
        id: String,
        priority: Int,
        suppressed: Boolean
    ): JSONObject {
        return JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, id)
            .put(Constants.INAPP_PRIORITY, priority)
            .put(Constants.INAPP_SUPPRESSED, suppressed)
    }

    private fun createDelayedInApp(
        id: String,
        delay: Int,
        priority: Int,
        suppressed: Boolean
    ): JSONObject {
        return JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, id)
            .put(INAPP_DELAY_AFTER_TRIGGER, delay)
            .put(Constants.INAPP_PRIORITY, priority)
            .put(Constants.INAPP_SUPPRESSED, suppressed)
    }
}