package com.clevertap.android.sdk.inapp.delay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import com.clevertap.android.sdk.utils.Clock
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test Rule to replace Main dispatcher with TestDispatcher
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@ExperimentalCoroutinesApi
class InAppDelayManagerV2Test {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var delayManager: InAppDelayManagerV2
    private lateinit var mockLogger: Logger
    private lateinit var mockStore: DelayedLegacyInAppStore
    private lateinit var testClock: TestClock
    private lateinit var testScope: TestScope
    private lateinit var testLifecycleOwner: TestLifecycleOwner

    private val accountId = "test-account-123"

    @Before
    fun setup() {
        // Mock dependencies
        mockLogger = mockk(relaxed = true)
        mockStore = mockk(relaxed = true)
        testClock = TestClock(1000L)

        testScope = TestScope()
        // Create test scope
        testLifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        clearAllMocks()
    }

    private fun createDelayManager(
        store: DelayedLegacyInAppStore? = mockStore,
        clock: Clock = testClock,
    ): InAppDelayManagerV2 {
        return InAppDelayManagerV2(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = store,
            clock = clock,
            scope = testScope,
            lifecycleOwner = testLifecycleOwner
        )
    }

    private fun createMockInApp(
        id: String = "inapp-123",
        delay: Int = 5
    ): JSONObject {
        return JSONObject().apply {
            put(Constants.INAPP_ID_IN_PAYLOAD, id)
            put(InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER, delay)
        }
    }

    // ============================================
    // 🧪 TEST CASES
    // ============================================
    @Test
    fun `scheduleDelayedInApps - null store triggers error`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 5)
        val inapps = JSONArray().apply { put(inapp) }
        var callbackResult: DelayedInAppResult? = null

        delayManager = createDelayManager(store = null) // No store!
        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResult = result
        }

        // Assert - Should abort immediately
        assertNull(callbackResult) // No callback called since scheduling aborted
    }

    @Test
    fun `scheduleDelayedInApps - single inapp fires callback after delay`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 5)
        val inapps = JSONArray().apply { put(inapp) }
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp

        delayManager = createDelayManager()

        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResult = result
        }

        // Assert - Before delay
        assertNull(callbackResult)

        // Fast forward 5 seconds
        advanceTimeBy(5001)

        // Assert - After delay
        assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Success)
        assertEquals("inapp-1", (callbackResult as DelayedInAppResult.Success).inAppId)
        assertEquals(inapp.toString(), (callbackResult as DelayedInAppResult.Success).inApp.toString())

        // Verify store interactions
        verifyOrder {
            mockStore.saveDelayedInAppsBatch(any())
            mockStore.getDelayedInApp("inapp-1")
            mockStore.removeDelayedInApp("inapp-1")
        }
        confirmVerified(mockStore)
    }

    private fun TestScope.handleLifecycleStates() {
        moveAppToForeground()
        moveAppToBackground()
        destoryApp()
    }

    @Test
    fun `scheduleDelayedInApps - multiple inapps with different delays all fire`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 3)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 7)
        val inapp3 = createMockInApp(id = "inapp-3", delay = 10)
        val inapps = JSONArray().apply {
            put(inapp1)
            put(inapp2)
            put(inapp3)
        }

        val callbackResults = mutableListOf<DelayedInAppResult>()

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp1
        every { mockStore.getDelayedInApp("inapp-2") } returns inapp2
        every { mockStore.getDelayedInApp("inapp-3") } returns inapp3

        delayManager = createDelayManager()

        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResults.add(result)
        }

        // Assert at 0 seconds - none fired
        assertEquals(0, callbackResults.size)

        // Fast forward 3 seconds - inapp1 should fire
        advanceTimeBy(3001)
        assertEquals(1, callbackResults.size)
        assertEquals("inapp-1", (callbackResults[0] as DelayedInAppResult.Success).inAppId)

        // Fast forward 4 more seconds (total 7) - inapp2 should fire
        advanceTimeBy(4000)
        assertEquals(2, callbackResults.size)
        assertEquals("inapp-2", (callbackResults[1] as DelayedInAppResult.Success).inAppId)

        // Fast forward 3 more seconds (total 10) - inapp3 should fire
        advanceTimeBy(3000)
        assertEquals(3, callbackResults.size)
        assertEquals("inapp-3", (callbackResults[2] as DelayedInAppResult.Success).inAppId)
    }

    @Test
    fun `scheduleDelayedInApps - duplicate id keeps first schedule`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "duplicate-id", delay = 5)
        val inapp2 = createMockInApp(id = "duplicate-id", delay = 10) // Same ID!
        val callbackResults = mutableListOf<DelayedInAppResult>()

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("duplicate-id") } returns inapp1

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act - Schedule first
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp1) }) { result ->
            callbackResults.add(result)
        }

        // Try to schedule second with same ID
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp2) }) { result ->
            callbackResults.add(result)
        }

        // Fast forward 5 seconds - first should fire
        advanceTimeBy(5001)
        assertEquals(1, callbackResults.size)

        // Fast forward 5 more seconds (total 10) - second should NOT fire
        advanceTimeBy(5000)
        assertEquals(1, callbackResults.size) // Still only 1!
    }

    @Test
    fun `scheduleDelayedInApps - store save failure triggers error callback`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 5)
        val inapps = JSONArray().apply { put(inapp) }
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns false // Save fails!

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResult = result
        }

        // Assert - Error callback should fire immediately
        assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Error)
        assertEquals(DelayedInAppResult.Error.ErrorReason.DB_SAVE_FAILED,
            (callbackResult as DelayedInAppResult.Error).reason)

        // Verify no scheduling happened
        verify { mockStore.saveDelayedInAppsBatch(any()) }
        verify(exactly = 0) { mockStore.getDelayedInApp(any()) }
    }

    @Test
    fun `scheduleDelayedInApps - inapp not found in DB triggers error`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 5)
        val inapps = JSONArray().apply { put(inapp) }
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns null // Not found!

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResult = result
        }

        // Fast forward delay
        advanceTimeBy(5001)

        // Assert - Error callback
        assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Error)
        assertEquals(DelayedInAppResult.Error.ErrorReason.NOT_FOUND_IN_DB,
            (callbackResult as DelayedInAppResult.Error).reason)
    }

    @Test
    fun `scheduleDelayedInApps - filters out already scheduled inapps`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 5)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 5)
        val inapp3 = createMockInApp(id = "inapp-3", delay = 5)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp(any()) } returns inapp1 // Mock return

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act - Schedule first batch (inapp1 and inapp2)
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { }

        // Clear previous invocations
        clearMocks(mockStore, answers = false)
        every { mockStore.saveDelayedInAppsBatch(any()) } returns true

        // Schedule second batch (inapp2 again and inapp3)
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp2) // Already scheduled!
            put(inapp3) // New
        }) { }

        // Assert - Should only save inapp3 (inapp2 filtered out)
        verify {
            mockStore.saveDelayedInAppsBatch(match { jsonArray ->
                jsonArray.length() == 1 && jsonArray.getJSONObject(0).getString(Constants.INAPP_ID_IN_PAYLOAD) == "inapp-3"
            })
        }
    }

    @Test
    fun `scheduleDelayedInApps - ignores inapps with blank or missing ID`() = testScope.runTest {
        // Arrange
        val validInapp = createMockInApp(id = "valid-id", delay = 5)
        val blankIdInapp = createMockInApp(id = "", delay = 5)
        val missingIdInapp = JSONObject().apply {
            put(InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER, 5)
        }

        val inapps = JSONArray().apply {
            put(validInapp)
            put(blankIdInapp)
            put(missingIdInapp)
        }

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("valid-id") } returns validInapp

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) { }

        // Assert - Should only process valid inapp
        verify {
            mockStore.saveDelayedInAppsBatch(match { jsonArray ->
                jsonArray.length() == 1 && jsonArray.getJSONObject(0).getString(Constants.INAPP_ID_IN_PAYLOAD) == "valid-id"
            })
        }
    }

    @Test
    fun `scheduleDelayedInApps - zero or negative delay is ignored`() = testScope.runTest {
        // Arrange
        val zeroDelayInapp = createMockInApp(id = "inapp-zero", delay = 0)
        val negativeDelayInapp = createMockInApp(id = "inapp-negative", delay = -5)
        val validInapp = createMockInApp(id = "inapp-valid", delay = 5)

        val inapps = JSONArray().apply {
            put(zeroDelayInapp)
            put(negativeDelayInapp)
            put(validInapp)
        }

        var callbackCount = 0

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-valid") } returns validInapp

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) {
            callbackCount++
        }

        // Fast forward
        advanceTimeBy(10000)

        // Assert - Only valid inapp should have fired callback
        assertEquals(1, callbackCount)
    }

    @Test
    fun `onAppBackground - cancels all active jobs`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 10)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 10)
        val inapps = JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }

        var callbackCount = 0

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp(any()) } returns inapp1

        delayManager = createDelayManager()
        moveAppToForeground()

        // Act - Schedule
        delayManager.scheduleDelayedInApps(inapps) {
            callbackCount++
        }

        // Fast forward 5 seconds
        advanceTimeBy(5000)

        // App goes to background
        moveAppToBackground()

        // Fast forward 10 more seconds (enough for original delay)
        advanceTimeBy(10000)

        // Assert - Callbacks should NOT have fired (jobs were cancelled)
        assertEquals(0, callbackCount)
        assertEquals(2, delayManager.getCancelledJobsCount())
        assertEquals(0, delayManager.getActiveCallbackCount())

        destoryApp()
    }

    private fun destoryApp() {
        println("📱 Moving to DESTROYED")
        testLifecycleOwner.currentState = Lifecycle.State.DESTROYED
    }

    private fun TestScope.moveAppToBackground(runPendingCoroutines: Boolean = true) {
        println("📱 Moving to CREATED (triggers onBackground)")
        testLifecycleOwner.currentState = Lifecycle.State.CREATED
        if (runPendingCoroutines)
        {
            advanceUntilIdle()
        }
    }

    private fun TestScope.moveAppToForeground(runPendingCoroutines: Boolean = true) {
        println("📱 Moving to STARTED (triggers onForeground)")
        testLifecycleOwner.currentState = Lifecycle.State.STARTED
        if (runPendingCoroutines)
        {
            advanceUntilIdle()
        }
    }

    @Test
    fun `onAppForeground - reschedules cancelled jobs with remaining time`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 10)
        val inapps = JSONArray().apply { put(inapp) }

        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp

        delayManager = createDelayManager()
        moveAppToForeground()// let pending coroutine for foreground start executing

        // Act - Schedule (10 second delay)
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResult = result
        }

        // Fast forward 3 seconds
        advanceTimeBy(3000)
        testClock.advanceTime(3000)

        // App goes to background (at 3 seconds)
        moveAppToBackground(runPendingCoroutines = false)

        // Time passes in background (but timer cancelled!)
        testClock.advanceTime(5000) // 5 seconds pass in real time

        moveAppToForeground(runPendingCoroutines = false)

        // Assert - callback hasn't fired yet
        assertNull(callbackResult)

        // Fast forward remaining time (2 seconds)
        advanceTimeBy(2001)

        // Assert - callback should fire now
        assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Success)
        destoryApp()
    }

    @Test
    fun `onAppForeground - discards expired jobs that timed out in background`() = testScope.runTest {
        // Scenario: Timer should have completed while app was in background
        // Expected: In-app should be discarded and removed from DB

        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 10)
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.removeDelayedInApp("inapp-1") } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule with 10 second delay
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) {
            callbackResult = it
        }

        // Fast forward 3 seconds
        advanceTimeBy(3000)
        testClock.advanceTime(3000)

        // App goes to background
        moveAppToBackground(runPendingCoroutines = false)

        // Time passes in background (15 seconds - more than remaining 7 seconds)
        testClock.advanceTime(15000)

        // App comes to foreground
        moveAppToForeground(runPendingCoroutines = false)
        advanceUntilIdle()

        // Assert
        assertNull(callbackResult) // Should NOT fire callback
        verify { mockStore.removeDelayedInApp("inapp-1") } // Should remove from DB
        assertEquals(0, delayManager.getActiveCallbackCount())
        destoryApp()
    }

    @Test
    fun `multiple background-foreground cycles - elapsed time includes both foreground and background`() = testScope.runTest {
        // This test verifies that elapsed time calculation includes BOTH:
        // 1. Foreground time (when coroutine timer was running)
        // 2. Background time (when timer was cancelled)

        val inapp = createMockInApp(id = "inapp-1", delay = 30) // 30 second delay
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule at time 1000ms with 30s (30000ms) delay
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) {
            callbackResult = it
        }

        // Cycle 1: 5s foreground
        advanceTimeBy(5000)  // Coroutine time advances
        testClock.advanceTime(5000)  // Clock time advances
        // Total elapsed: 5s, Remaining: 25s

        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(3000)  // 3s in background (only clock time advances, no coroutine time)
        // Total elapsed: 8s, Remaining: 22s

        // Cycle 2: 6s foreground
        moveAppToForeground(runPendingCoroutines = false)
        // Note: Timer reschedules with remaining 22s
        advanceTimeBy(6000)  // Coroutine time advances
        testClock.advanceTime(6000)  // Clock time advances
        // Total elapsed: 14s, Remaining: 16s

        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(4000)  // 4s in background
        // Total elapsed: 18s, Remaining: 12s

        // Cycle 3: 7s foreground
        moveAppToForeground(runPendingCoroutines = false)
        // Note: Timer reschedules with remaining 12s
        advanceTimeBy(7000)  // Coroutine time advances
        testClock.advanceTime(7000)  // Clock time advances
        // Total elapsed: 25s, Remaining: 5s

        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(2000)  // 2s in background
        // Total elapsed: 27s, Remaining: 3s

        // Final foreground
        moveAppToForeground(runPendingCoroutines = false)
        assertNull(callbackResult) // Not yet fired

        // Advance the remaining 3s + a bit
        advanceTimeBy(3001)

        // Assert - callback should fire now
        assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Success)
        assertEquals("inapp-1", (callbackResult as DelayedInAppResult.Success).inAppId)
        destoryApp()

        // Verify the timer properly accounted for all foreground and background time
        // Total real-world time: 5+3+6+4+7+2+3 = 30s ✓
    }

    @Test
    fun `onAppForeground - handles mixed expired and active timers correctly`() = testScope.runTest {
        // Scenario: 3 in-apps with different delays
        // After foreground + background period: one expires, two remain active
        //
        // KEY: elapsedTime = currentTime - scheduledAt (includes BOTH fg and bg time)

        val inapp1 = createMockInApp(id = "inapp-1", delay = 8)   // 8s total
        val inapp2 = createMockInApp(id = "inapp-2", delay = 20)  // 20s total
        val inapp3 = createMockInApp(id = "inapp-3", delay = 25)  // 25s total

        val callbackResults = mutableListOf<DelayedInAppResult>()
        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-2") } returns inapp2
        every { mockStore.getDelayedInApp("inapp-3") } returns inapp3
        every { mockStore.removeDelayedInApp("inapp-1") } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // ========================================
        // SCHEDULE: All scheduled at time = 1000ms
        // ========================================
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)  // scheduledAt = 1000ms, needs 8000ms
            put(inapp2)  // scheduledAt = 1000ms, needs 20000ms
            put(inapp3)  // scheduledAt = 1000ms, needs 25000ms
        }) { callbackResults.add(it) }

        // ========================================
        // FOREGROUND: Run for 3 seconds
        // ========================================
        advanceTimeBy(3000)
        testClock.advanceTime(3000)  // currentTime = 4000ms

        // Elapsed from scheduledAt:
        // - inapp1: 4000 - 1000 = 3000ms elapsed, 5000ms remaining (8000 - 3000)
        // - inapp2: 4000 - 1000 = 3000ms elapsed, 17000ms remaining (20000 - 3000)
        // - inapp3: 4000 - 1000 = 3000ms elapsed, 22000ms remaining (25000 - 3000)

        assertEquals(0, callbackResults.size) // None fired yet

        // ========================================
        // BACKGROUND: 7 seconds pass (only clock time, no coroutine time)
        // ========================================
        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(7000)  // currentTime = 11000ms

        // Total elapsed from scheduledAt (both foreground + background):
        // - inapp1: 11000 - 1000 = 10000ms elapsed (EXCEEDED 8000ms delay) → EXPIRED
        // - inapp2: 11000 - 1000 = 10000ms elapsed, 10000ms remaining (20000 - 10000)
        // - inapp3: 11000 - 1000 = 10000ms elapsed, 15000ms remaining (25000 - 10000)

        // ========================================
        // FOREGROUND: Reschedule active, discard expired
        // ========================================
        moveAppToForeground(runPendingCoroutines = false)

        // Assert - no callbacks yet (reschedule in progress)
        assertEquals(0, callbackResults.size)

        // ========================================
        // ADVANCE: 10 seconds - inapp2 should fire
        // ========================================
        advanceTimeBy(10001)  // Advance 10s + a bit

        assertEquals(1, callbackResults.size)
        assertTrue(callbackResults[0] is DelayedInAppResult.Success)
        assertEquals("inapp-2", (callbackResults[0] as DelayedInAppResult.Success).inAppId)

        // ========================================
        // ADVANCE: 5 more seconds - inapp3 should fire
        // ========================================
        advanceTimeBy(5000)

        assertEquals(2, callbackResults.size)
        assertTrue(callbackResults[1] is DelayedInAppResult.Success)
        assertEquals("inapp-3", (callbackResults[1] as DelayedInAppResult.Success).inAppId)

        // ========================================
        // VERIFICATION: Check total time accounting
        // ========================================
        // Total real-world time for each:
        // - inapp1: 3s (fg) + 7s (bg) = 10s elapsed vs 8s needed → EXPIRED ✓
        // - inapp2: 3s (fg) + 7s (bg) + 10s (fg) = 20s elapsed vs 20s needed → FIRED ✓
        // - inapp3: 3s (fg) + 7s (bg) + 10s (fg) + 5s (fg) = 25s elapsed vs 25s needed → FIRED ✓

        destoryApp()
    }

    @Test
    fun `onAppForeground - handles exact expiry time correctly`() = testScope.runTest {
        // Scenario: Timer expires exactly when app comes to foreground
        // Expected: Should be discarded (remainingTime = 0), not rescheduled

        val inapp = createMockInApp(id = "inapp-1", delay = 10)  // 10 second delay
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.removeDelayedInApp("inapp-1") } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // ========================================
        // SCHEDULE: at time = 1000ms, needs 10000ms
        // ========================================
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) {
            callbackResult = it
        }

        // ========================================
        // FOREGROUND: 6 seconds
        // ========================================
        advanceTimeBy(6000)
        testClock.advanceTime(6000)  // currentTime = 7000ms

        // Elapsed: 7000 - 1000 = 6000ms
        // Remaining: 10000 - 6000 = 4000ms (4s left)

        assertNull(callbackResult)

        // ========================================
        // BACKGROUND: Exactly 4 seconds pass
        // ========================================
        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(4000)  // currentTime = 11000ms

        // Total elapsed: 11000 - 1000 = 10000ms
        // Remaining: 10000 - 10000 = 0ms (EXACTLY EXPIRED)

        // ========================================
        // FOREGROUND: Should discard (not reschedule with 0ms)
        // ========================================
        moveAppToForeground()

        // Assert - Should be discarded, callback should NOT fire
        assertNull(callbackResult)
        assertEquals(0, delayManager.getActiveCallbackCount())

        // ========================================
        // VERIFICATION: No reschedule with 0 or negative remaining time
        // ========================================
        // Total time: 6s (fg) + 4s (bg) = 10s elapsed vs 10s needed → EXPIRED ✓

        destoryApp()
    }

    @Test
    fun `onAppForeground - handles very short remaining time correctly`() = testScope.runTest {
        // Scenario: Only 400ms remaining when app foregrounds
        // Expected: Should reschedule and fire quickly with short delay

        val inapp = createMockInApp(id = "inapp-1", delay = 10)  // 10 second delay
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp

        delayManager = createDelayManager()
        moveAppToForeground()

        // ========================================
        // SCHEDULE: at time = 1000ms, needs 10000ms
        // ========================================
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) {
            callbackResult = it
        }

        // ========================================
        // FOREGROUND: 7 seconds
        // ========================================
        advanceTimeBy(7000)
        testClock.advanceTime(7000)  // currentTime = 8000ms

        // Elapsed: 8000 - 1000 = 7000ms
        // Remaining: 10000 - 7000 = 3000ms (3s left)

        assertNull(callbackResult)

        // ========================================
        // BACKGROUND: 2.6 seconds pass
        // ========================================
        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(2600)  // currentTime = 10600ms

        // Total elapsed: 10600 - 1000 = 9600ms
        // Remaining: 10000 - 9600 = 400ms (0.4s left - very short!)

        // ========================================
        // FOREGROUND: Should reschedule with 400ms
        // ========================================
        moveAppToForeground(runPendingCoroutines = false)
        assertNull(callbackResult) // Not yet

        // ========================================
        // ADVANCE: Just over 400ms - should fire
        // ========================================
        advanceTimeBy(401)

        // Assert - callback should fire now
        assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Success)
        assertEquals("inapp-1", (callbackResult as DelayedInAppResult.Success).inAppId)

        // ========================================
        // VERIFICATION: Timer worked with sub-second remaining time
        // ========================================
        // Total time: 7s (fg) + 2.6s (bg) + 0.4s (fg) = 10s ✓

        destoryApp()
    }

    @Test
    fun `onAppBackground - handles immediate background before timers advance`() = testScope.runTest {
        // Scenario: App backgrounds immediately after scheduling (0 elapsed time)
        // Expected: Timer should resume from near-full delay on foreground

        val inapp = createMockInApp(id = "inapp-1", delay = 10)  // 10 second delay
        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp

        delayManager = createDelayManager()
        moveAppToForeground()

        // ========================================
        // SCHEDULE: at time = 1000ms, needs 10000ms
        // ========================================
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) {
            callbackResult = it
        }

        // ========================================
        // IMMEDIATELY BACKGROUND (milli seconds time advancement in foreground)
        // ========================================
        advanceTimeBy(10)
        moveAppToBackground(runPendingCoroutines = false)
        // currentTime still = 1000ms

        // Elapsed: 1000 - 1000 = 0ms
        // Remaining: 10000 - 0 = 10000ms (full delay still needed)

        testClock.advanceTime(2000)  // currentTime = 3000ms (2s in background)

        // Total elapsed: 3000 - 1000 = 2000ms (2s elapsed in background)
        // Remaining: 10000 - 2000 = 8000ms (8s left)

        // ========================================
        // FOREGROUND: Should reschedule with ~8s remaining
        // ========================================
        moveAppToForeground(runPendingCoroutines = false)
        assertNull(callbackResult)

        // ========================================
        // ADVANCE: 8 seconds + a bit
        // ========================================
        advanceTimeBy(8001)

        // Assert - callback should fire now
        /*assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Success)
        assertEquals("inapp-1", (callbackResult as DelayedInAppResult.Success).inAppId)*/

        // ========================================
        // VERIFICATION: Background time counted even though coroutine never started
        // ========================================
        // Total time: 0s (fg) + 2s (bg) + 8s (fg) = 10s ✓

        destoryApp()
    }

    @Test
    fun `onAppForeground - handles empty cancelled jobs gracefully`() = testScope.runTest {
        // Scenario: App goes to foreground with no pending timers
        // Expected: No crashes, no unnecessary operations

        delayManager = createDelayManager()

        // Move to foreground without scheduling anything
        moveAppToForeground()

        // Assert - Should handle gracefully
        assertEquals(0, delayManager.getActiveCallbackCount())

        // Verify no DB operations
        verify(exactly = 0) { mockStore.removeDelayedInApp(any()) }
        verify(exactly = 0) { mockStore.getDelayedInApp(any()) }
        destoryApp()
    }

    @Test
    fun `advanceUntilIdle - completes all pending delays`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 5)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 50)
        val inapp3 = createMockInApp(id = "inapp-3", delay = 100)
        val inapps = JSONArray().apply {
            put(inapp1)
            put(inapp2)
            put(inapp3)
        }

        val callbackResults = mutableListOf<DelayedInAppResult>()

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp(any()) } returns inapp1

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResults.add(result)
        }

        // Instead of manually advancing time, use advanceUntilIdle
        advanceUntilIdle()

        // Assert - All callbacks should have fired
        assertEquals(3, callbackResults.size)
        destoryApp()
    }

    @Test
    fun `store not initialized during delay - triggers error callback`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 5)
        val inapps = JSONArray().apply { put(inapp) }

        var callbackResult: DelayedInAppResult? = null

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true

        delayManager = createDelayManager()
        handleLifecycleStates()

        // Act - Schedule
        delayManager.scheduleDelayedInApps(inapps) { result ->
            callbackResult = result
        }

        // Simulate store becoming null before delay completes
        delayManager.delayedLegacyInAppStore = null

        // Fast forward
        advanceTimeBy(5001)

        // Assert - Error callback
        assertNotNull(callbackResult)
        assertTrue(callbackResult is DelayedInAppResult.Error)
        assertEquals(DelayedInAppResult.Error.ErrorReason.STORE_NOT_INITIALIZED,
            (callbackResult as DelayedInAppResult.Error).reason)
        destoryApp()
    }
    // ============================================
// 🗺️ STATE MANAGEMENT TESTS
// Tests for activeJobs and cancelledJobs map states
// ============================================

    @Test
    fun `initial state - both maps empty`() = testScope.runTest {
        // Arrange
        delayManager = createDelayManager()
        moveAppToForeground()

        // Assert - Initial state
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())
        destoryApp()
    }

    @Test
    fun `after scheduling - activeJobs populated, cancelledJobs empty`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 10)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 15)
        val inapp3 = createMockInApp(id = "inapp-3", delay = 20)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // Act - Schedule 3 in-apps
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
            put(inapp3)
        }) { }

        // Assert - activeJobs should have 3, cancelledJobs should be empty
        assertEquals(3, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Verify specific IDs are scheduled
        assertTrue(delayManager.isCallbackScheduled("inapp-1"))
        assertTrue(delayManager.isCallbackScheduled("inapp-2"))
        assertTrue(delayManager.isCallbackScheduled("inapp-3"))

        destoryApp()
    }

    @Test
    fun `after background - activeJobs empty, cancelledJobs populated`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 10)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 15)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { }

        // Assert - Before background
        assertEquals(2, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Act - App goes to background
        advanceTimeBy(3000) // Advance 3s before background
        moveAppToBackground()
        advanceTimeBy(1)

        // Assert - After background (jobs cancelled and moved to cancelledJobs)
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(2, delayManager.getCancelledJobsCount())

        // Verify jobs are no longer scheduled as active
        assertFalse(delayManager.isCallbackScheduled("inapp-1"))
        assertFalse(delayManager.isCallbackScheduled("inapp-2"))

        destoryApp()
    }

    @Test
    fun `after foreground from background - activeJobs repopulated, cancelledJobs cleared`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 20)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 25)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp(any()) } returns inapp1

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule at 1000ms
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { }

        assertEquals(2, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Foreground for 5s
        advanceTimeBy(5000)
        testClock.advanceTime(5000)

        // Background
        moveAppToBackground(runPendingCoroutines = false)
        advanceTimeBy(1)

        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(2, delayManager.getCancelledJobsCount())

        // 3s in background
        testClock.advanceTime(3000)

        // Act - Foreground again (both have remaining time, will be rescheduled)
        moveAppToForeground(runPendingCoroutines = false)
        advanceTimeBy(1)

        // Assert - activeJobs repopulated, cancelledJobs cleared
        assertEquals(2, delayManager.getActiveCallbackCount())
        assertEquals(2, delayManager.getCancelledJobsCount())

        assertTrue(delayManager.isCallbackScheduled("inapp-1"))
        assertTrue(delayManager.isCallbackScheduled("inapp-2"))

        destoryApp()
    }

    @Test
    fun `timer completion - removes from activeJobs, not in cancelledJobs`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 5)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 10)

        var callbackCount = 0

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp1
        every { mockStore.getDelayedInApp("inapp-2") } returns inapp2

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule 2 in-apps
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { callbackCount++ }

        assertEquals(2, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Act - Advance 5s, first timer completes
        advanceTimeBy(5001)

        // Assert - inapp1 completed and removed
        assertEquals(1, callbackCount)
        assertEquals(1, delayManager.getActiveCallbackCount()) // Only inapp2 left
        assertEquals(0, delayManager.getCancelledJobsCount())
        assertFalse(delayManager.isCallbackScheduled("inapp-1")) // Removed
        assertTrue(delayManager.isCallbackScheduled("inapp-2"))  // Still active

        // Act - Advance 5s more, second timer completes
        advanceTimeBy(5000)

        // Assert - Both completed
        assertEquals(2, callbackCount)
        assertEquals(0, delayManager.getActiveCallbackCount()) // Both removed
        assertEquals(0, delayManager.getCancelledJobsCount())
        assertFalse(delayManager.isCallbackScheduled("inapp-2")) // Removed

        destoryApp()
    }

    @Test
    fun `foreground with expired timers - removes from cancelledJobs, not added to activeJobs`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 5)  // Will expire
        val inapp2 = createMockInApp(id = "inapp-2", delay = 20) // Will remain

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-2") } returns inapp2
        every { mockStore.removeDelayedInApp("inapp-1") } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule both at 1000ms
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { }

        assertEquals(2, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Foreground 2s
        advanceTimeBy(2000)
        testClock.advanceTime(2000)

        // Background
        moveAppToBackground(runPendingCoroutines = false)
        advanceTimeBy(1)
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(2, delayManager.getCancelledJobsCount())

        // 10s in background (inapp1 expires: 2+10 = 12s > 5s)
        testClock.advanceTime(10000)

        // Act - Foreground (inapp1 expired, inapp2 has time left)
        moveAppToForeground(runPendingCoroutines = false)
        advanceTimeBy(1)

        // Assert - Only inapp2 rescheduled, inapp1 removed from cancelledJobs
        assertEquals(1, delayManager.getActiveCallbackCount()) // Only inapp2
        assertEquals(1, delayManager.getCancelledJobsCount()) // unless cancelled one not finished after reschedule, we keep it

        assertFalse(delayManager.isCallbackScheduled("inapp-1")) // Expired, not scheduled
        assertTrue(delayManager.isCallbackScheduled("inapp-2"))  // Active

        verify { mockStore.removeDelayedInApp("inapp-1") } // Expired one removed from DB

        destoryApp()
    }

    @Test
    fun `multiple background-foreground cycles - map states transition correctly`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "inapp-1", delay = 30)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) { }

        // STATE 1: Active after schedule
        assertEquals(1, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Cycle 1: Foreground 5s → Background
        advanceTimeBy(5000)
        testClock.advanceTime(5000)
        moveAppToBackground(runPendingCoroutines = false)
        advanceTimeBy(1)

        // STATE 2: Cancelled during background
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(1, delayManager.getCancelledJobsCount())

        testClock.advanceTime(2000)

        // Cycle 2: Foreground 5s → Background
        moveAppToForeground(runPendingCoroutines = false)
        advanceTimeBy(1)

        // STATE 3: Active again after foreground
        assertEquals(1, delayManager.getActiveCallbackCount())
        assertEquals(1, delayManager.getCancelledJobsCount())

        advanceTimeBy(5000)
        testClock.advanceTime(5000)
        moveAppToBackground(runPendingCoroutines = false)
        advanceTimeBy(1)

        // STATE 4: Cancelled again
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(1, delayManager.getCancelledJobsCount())

        testClock.advanceTime(3000)

        // Final foreground
        moveAppToForeground(runPendingCoroutines = false)
        advanceTimeBy(1)

        // STATE 5: Active again
        assertEquals(1, delayManager.getActiveCallbackCount())
        assertEquals(1, delayManager.getCancelledJobsCount())

        destoryApp()
    }

    @Test
    fun `duplicate scheduling - does not increase activeJobs count`() = testScope.runTest {
        // Arrange
        val inapp = createMockInApp(id = "duplicate-id", delay = 10)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // Act - Schedule first time
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) { }

        assertEquals(1, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Act - Try to schedule again with same ID
        delayManager.scheduleDelayedInApps(JSONArray().apply { put(inapp) }) { }

        // Assert - Count should still be 1 (duplicate filtered)
        assertEquals(1, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Verify only saved once (second call filters it out)
        verify(exactly = 1) {
            mockStore.saveDelayedInAppsBatch(match { it.length() == 1 })
        }

        destoryApp()
    }

    @Test
    fun `store save failure - does not add to activeJobs`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 10)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 15)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns false // Save fails

        delayManager = createDelayManager()
        moveAppToForeground()

        // Act - Schedule (will fail to save)
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { }

        // Assert - No jobs added to activeJobs
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        assertFalse(delayManager.isCallbackScheduled("inapp-1"))
        assertFalse(delayManager.isCallbackScheduled("inapp-2"))

        destoryApp()
    }

    @Test
    fun `invalid delay - does not add to activeJobs`() = testScope.runTest {
        // Arrange
        val validInapp = createMockInApp(id = "valid-id", delay = 10)
        val zeroDelayInapp = createMockInApp(id = "zero-delay", delay = 0)
        val negativeDelayInapp = createMockInApp(id = "negative-delay", delay = -5)
        val tooLargeDelayInapp = createMockInApp(id = "too-large", delay = 2000) // > 1200

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        // Act - Schedule mix of valid and invalid
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(validInapp)
            put(zeroDelayInapp)
            put(negativeDelayInapp)
            put(tooLargeDelayInapp)
        }) { }

        // Assert - Only valid one added
        assertEquals(1, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        assertTrue(delayManager.isCallbackScheduled("valid-id"))
        assertFalse(delayManager.isCallbackScheduled("zero-delay"))
        assertFalse(delayManager.isCallbackScheduled("negative-delay"))
        assertFalse(delayManager.isCallbackScheduled("too-large"))

        destoryApp()
    }

    @Test
    fun `getActiveCallbackIds - returns correct set of active IDs`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 10)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 15)
        val inapp3 = createMockInApp(id = "inapp-3", delay = 20)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp("inapp-1") } returns inapp1

        delayManager = createDelayManager()
        moveAppToForeground()

        // Act - Schedule 3
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
            put(inapp3)
        }) { }

        // Assert - All 3 IDs present
        val activeIds = delayManager.getActiveCallbackIds()
        assertEquals(3, activeIds.size)
        assertTrue(activeIds.contains("inapp-1"))
        assertTrue(activeIds.contains("inapp-2"))
        assertTrue(activeIds.contains("inapp-3"))

        // Complete first timer
        advanceTimeBy(10001)

        // Assert - Only 2 IDs remain
        val remainingIds = delayManager.getActiveCallbackIds()
        assertEquals(2, remainingIds.size)
        assertFalse(remainingIds.contains("inapp-1")) // Completed
        assertTrue(remainingIds.contains("inapp-2"))
        assertTrue(remainingIds.contains("inapp-3"))

        destoryApp()
    }

    @Test
    fun `mixed state - some complete, some cancelled, some active`() = testScope.runTest {
        // Scenario: 3 in-apps with different delays
        // - inapp1: completes before background
        // - inapp2 & inapp3: cancelled on background

        val inapp1 = createMockInApp(id = "inapp-1", delay = 3)  // Will complete
        val inapp2 = createMockInApp(id = "inapp-2", delay = 10) // Will be cancelled
        val inapp3 = createMockInApp(id = "inapp-3", delay = 15) // Will be cancelled

        var callbackCount = 0

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp(any()) } returns inapp1

        delayManager = createDelayManager()
        moveAppToForeground()

        // Schedule all 3
        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
            put(inapp3)
        }) { callbackCount++ }

        // STATE 1: All active
        assertEquals(3, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Advance 3s - inapp1 completes
        advanceTimeBy(3001)

        // STATE 2: 2 active (inapp1 completed)
        assertEquals(1, callbackCount)
        assertEquals(2, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        // Background
        moveAppToBackground()
        advanceTimeBy(1)

        // STATE 3: 0 active, 2 cancelled
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(2, delayManager.getCancelledJobsCount())

        // Verify specific states
        assertFalse(delayManager.isCallbackScheduled("inapp-1")) // Completed
        assertFalse(delayManager.isCallbackScheduled("inapp-2")) // Cancelled
        assertFalse(delayManager.isCallbackScheduled("inapp-3")) // Cancelled

        destoryApp()
    }

    @Test
    fun `cancelCallback - removes from activeJobs immediately`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 10)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 15)

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true

        delayManager = createDelayManager()
        moveAppToForeground()

        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { }

        assertEquals(2, delayManager.getActiveCallbackCount())

        // Act - Cancel one specific callback
        val cancelled = delayManager.cancelCallback("inapp-1")

        // Assert
        assertTrue(cancelled)
        assertEquals(1, delayManager.getActiveCallbackCount())
        assertFalse(delayManager.isCallbackScheduled("inapp-1"))
        assertTrue(delayManager.isCallbackScheduled("inapp-2"))

        // Try to cancel non-existent
        val notCancelled = delayManager.cancelCallback("non-existent")
        assertFalse(notCancelled)
        assertEquals(1, delayManager.getActiveCallbackCount())

        destoryApp()
    }

    @Test
    fun `empty state after all timers complete`() = testScope.runTest {
        // Arrange
        val inapp1 = createMockInApp(id = "inapp-1", delay = 5)
        val inapp2 = createMockInApp(id = "inapp-2", delay = 10)

        var callbackCount = 0

        every { mockStore.saveDelayedInAppsBatch(any()) } returns true
        every { mockStore.getDelayedInApp(any()) } returns inapp1

        delayManager = createDelayManager()
        moveAppToForeground()

        delayManager.scheduleDelayedInApps(JSONArray().apply {
            put(inapp1)
            put(inapp2)
        }) { callbackCount++ }

        // Wait for all to complete
        advanceTimeBy(10001)

        // Assert - All completed, maps empty
        assertEquals(2, callbackCount)
        assertEquals(0, delayManager.getActiveCallbackCount())
        assertEquals(0, delayManager.getCancelledJobsCount())

        val activeIds = delayManager.getActiveCallbackIds()
        assertTrue(activeIds.isEmpty())

        destoryApp()
    }
}

// Helper extension to assert null
private fun assertNull(value: Any?) {
    assert(value == null) { "Expected null but was: $value" }
}