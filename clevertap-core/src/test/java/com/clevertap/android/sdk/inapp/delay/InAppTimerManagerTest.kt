package com.clevertap.android.sdk.inapp.delay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.TestClock
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test Rule to replace Main dispatcher with TestDispatcher
 */
@ExperimentalCoroutinesApi
class TimerManagerMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@ExperimentalCoroutinesApi
class InAppTimerManagerTest {

    @get:Rule
    val mainDispatcherRule = TimerManagerMainDispatcherRule()

    private lateinit var timerManager: InAppTimerManager
    private lateinit var mockLogger: ILogger
    private lateinit var testClock: TestClock
    private lateinit var testScope: TestScope
    private lateinit var testLifecycleOwner: TestLifecycleOwner

    private val accountId = "test-account-123"

    @Before
    fun setup() {
        mockLogger = mockk(relaxed = true)
        testClock = TestClock(1000L)
        testScope = TestScope()
        testLifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        clearAllMocks()
    }

    // ==================== HELPER METHODS ====================

    private fun createTimerManager(
        tagSuffix: String = "Test"
    ): InAppTimerManager {
        return InAppTimerManager(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            scope = testScope,
            lifecycleOwner = testLifecycleOwner,
            tagSuffix = tagSuffix
        )
    }

    private fun TestScope.handleLifecycleStates() {
        moveAppToForeground()
    }

    private fun TestScope.moveAppToForeground(runPendingCoroutines: Boolean = true) {
        println("📱 Moving to STARTED (triggers onForeground)")
        testLifecycleOwner.currentState = Lifecycle.State.STARTED
        if (runPendingCoroutines) {
            advanceUntilIdle()
        }
    }

    private fun TestScope.moveAppToBackground(runPendingCoroutines: Boolean = true) {
        println("📱 Moving to CREATED (triggers onBackground)")
        testLifecycleOwner.currentState = Lifecycle.State.CREATED
        if (runPendingCoroutines) {
            advanceUntilIdle()
        }
    }

    private fun destroyApp() {
        println("📱 Moving to DESTROYED")
        testLifecycleOwner.currentState = Lifecycle.State.DESTROYED
    }

    // ==================== SCHEDULE TIMER TESTS ====================

    @Test
    fun `scheduleTimer - single timer fires callback after delay`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act
        timerManager.scheduleTimer("timer-1", 5000L) { result ->
            callbackResult = result
        }

        // Assert - Before delay
        assertNull(callbackResult)
        assertEquals(1, timerManager.getActiveTimerCount())
        assertTrue(timerManager.isTimerScheduled("timer-1"))

        // Fast forward 5 seconds
        advanceTimeBy(5001)

        // Assert - After delay
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)
        assertEquals("timer-1", (callbackResult as InAppTimerManager.TimerResult.Completed).id)
        assertEquals(0, timerManager.getActiveTimerCount())

        destroyApp()
    }

    @Test
    fun `scheduleTimer - multiple timers with different delays fire in order`() =
        testScope.runTest {
            // Arrange
            val callbackResults = mutableListOf<InAppTimerManager.TimerResult>()

            timerManager = createTimerManager()
            handleLifecycleStates()

            // Act - Schedule timers with different delays
            timerManager.scheduleTimer("timer-3sec", 3000L) { callbackResults.add(it) }
            timerManager.scheduleTimer("timer-5sec", 5000L) { callbackResults.add(it) }
            timerManager.scheduleTimer("timer-1sec", 1000L) { callbackResults.add(it) }

            // Assert - All scheduled
            assertEquals(3, timerManager.getActiveTimerCount())

            // Fast forward 1 second - timer-1sec fires
            advanceTimeBy(1001)
            assertEquals(1, callbackResults.size)
            assertEquals(
                "timer-1sec",
                (callbackResults[0] as InAppTimerManager.TimerResult.Completed).id
            )

            // Fast forward 2 more seconds - timer-3sec fires
            advanceTimeBy(2000)
            assertEquals(2, callbackResults.size)
            assertEquals(
                "timer-3sec",
                (callbackResults[1] as InAppTimerManager.TimerResult.Completed).id
            )

            // Fast forward 2 more seconds - timer-5sec fires
            advanceTimeBy(2000)
            assertEquals(3, callbackResults.size)
            assertEquals(
                "timer-5sec",
                (callbackResults[2] as InAppTimerManager.TimerResult.Completed).id
            )

            assertEquals(0, timerManager.getActiveTimerCount())
            destroyApp()
        }

    @Test
    fun `scheduleTimer - duplicate id keeps existing timer`() = testScope.runTest {
        // Arrange
        var firstCallbackCount = 0
        var secondCallbackCount = 0

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act - Schedule first timer
        timerManager.scheduleTimer("timer-1", 5000L) { firstCallbackCount++ }

        // Try to schedule second timer with same id
        timerManager.scheduleTimer("timer-1", 3000L) { secondCallbackCount++ }

        // Assert - Should still have only 1 timer
        assertEquals(1, timerManager.getActiveTimerCount())

        // Fast forward 3 seconds - second callback should NOT fire
        advanceTimeBy(3001)
        assertEquals(0, firstCallbackCount)
        assertEquals(0, secondCallbackCount)

        // Fast forward 2 more seconds - first callback SHOULD fire
        advanceTimeBy(2000)
        assertEquals(1, firstCallbackCount)
        assertEquals(0, secondCallbackCount)

        destroyApp()
    }

    @Test
    fun `scheduleTimer - returns job handle`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act
        val job = timerManager.scheduleTimer("timer-1", 5000L) {}

        // Assert
        assertNotNull(job)
        assertTrue(job.isActive)

        advanceTimeBy(5001)
        assertFalse(job.isActive)

        destroyApp()
    }

    @Test
    fun `scheduleTimer - completed result contains scheduledAt time`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null
        testClock.setCurrentTime(5000L)

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act
        timerManager.scheduleTimer("timer-1", 3000L) { result ->
            callbackResult = result
        }

        advanceTimeBy(3001)

        // Assert
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)
        assertEquals(5000L, (callbackResult as InAppTimerManager.TimerResult.Completed).scheduledAt)

        destroyApp()
    }

    @Test
    fun `scheduleTimer - negative delay executes immediately`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act - Schedule with negative delay
        timerManager.scheduleTimer("timer-1", -5000L) { result ->
            callbackResult = result
        }

        // Note: Kotlin's delay() treats negative values as 0, so it should complete immediately
        advanceUntilIdle()

        // Assert - Timer should have completed immediately
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)
        assertEquals("timer-1", (callbackResult as InAppTimerManager.TimerResult.Completed).id)
        assertEquals(0, timerManager.getActiveTimerCount())

        destroyApp()
    }

    @Test
    fun `scheduleTimer - zero delay executes immediately`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act - Schedule with zero delay
        timerManager.scheduleTimer("timer-1", 0L) { result ->
            callbackResult = result
        }

        advanceUntilIdle()

        // Assert - Timer should have completed immediately
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)
        assertEquals("timer-1", (callbackResult as InAppTimerManager.TimerResult.Completed).id)
        assertEquals(0, timerManager.getActiveTimerCount())

        destroyApp()
    }

    // ==================== CANCEL TIMER TESTS ====================

    @Test
    fun `cancelTimer - cancels scheduled timer`() = testScope.runTest {
        // Arrange
        var callbackFired = false

        timerManager = createTimerManager()
        handleLifecycleStates()

        timerManager.scheduleTimer("timer-1", 5000L) { callbackFired = true }
        assertEquals(1, timerManager.getActiveTimerCount())

        // Act
        val cancelled = timerManager.cancelTimer("timer-1")

        // Assert
        assertTrue(cancelled)
        assertEquals(0, timerManager.getActiveTimerCount())
        assertFalse(timerManager.isTimerScheduled("timer-1"))

        // Fast forward - callback should NOT fire
        advanceTimeBy(6000)
        assertFalse(callbackFired)

        destroyApp()
    }

    @Test
    fun `cancelTimer - returns false for non-existent timer`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act
        val cancelled = timerManager.cancelTimer("non-existent")

        // Assert
        assertFalse(cancelled)

        destroyApp()
    }

    @Test
    fun `cancelTimer - only cancels specified timer`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        timerManager.scheduleTimer("timer-1", 5000L) {}
        timerManager.scheduleTimer("timer-2", 5000L) {}
        timerManager.scheduleTimer("timer-3", 5000L) {}

        assertEquals(3, timerManager.getActiveTimerCount())

        // Act
        timerManager.cancelTimer("timer-2")

        // Assert
        assertEquals(2, timerManager.getActiveTimerCount())
        assertTrue(timerManager.isTimerScheduled("timer-1"))
        assertFalse(timerManager.isTimerScheduled("timer-2"))
        assertTrue(timerManager.isTimerScheduled("timer-3"))

        destroyApp()
    }

    // ==================== GET ACTIVE TIMER COUNT TESTS ====================

    @Test
    fun `getActiveTimerCount - returns 0 initially`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        // Assert
        assertEquals(0, timerManager.getActiveTimerCount())

        destroyApp()
    }

    @Test
    fun `getActiveTimerCount - reflects scheduled timers`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act & Assert
        timerManager.scheduleTimer("timer-1", 5000L) {}
        assertEquals(1, timerManager.getActiveTimerCount())

        timerManager.scheduleTimer("timer-2", 5000L) {}
        assertEquals(2, timerManager.getActiveTimerCount())

        timerManager.scheduleTimer("timer-3", 5000L) {}
        assertEquals(3, timerManager.getActiveTimerCount())

        destroyApp()
    }

    @Test
    fun `getActiveTimerCount - decreases after timer completes`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        timerManager.scheduleTimer("timer-1", 2000L) {}
        timerManager.scheduleTimer("timer-2", 5000L) {}
        assertEquals(2, timerManager.getActiveTimerCount())

        // Act - First timer completes
        advanceTimeBy(2001)

        // Assert
        assertEquals(1, timerManager.getActiveTimerCount())

        // Second timer completes
        advanceTimeBy(3000)
        assertEquals(0, timerManager.getActiveTimerCount())

        destroyApp()
    }

    // ==================== IS TIMER SCHEDULED TESTS ====================

    @Test
    fun `isTimerScheduled - returns false for non-existent timer`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        // Assert
        assertFalse(timerManager.isTimerScheduled("non-existent"))

        destroyApp()
    }

    @Test
    fun `isTimerScheduled - returns true for active timer`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        timerManager.scheduleTimer("timer-1", 5000L) {}

        // Assert
        assertTrue(timerManager.isTimerScheduled("timer-1"))

        destroyApp()
    }

    @Test
    fun `isTimerScheduled - returns false after timer completes`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        timerManager.scheduleTimer("timer-1", 2000L) {}
        assertTrue(timerManager.isTimerScheduled("timer-1"))

        // Act
        advanceTimeBy(2001)

        // Assert
        assertFalse(timerManager.isTimerScheduled("timer-1"))

        destroyApp()
    }

    // ==================== CLEANUP TESTS ====================

    @Test
    fun `cleanup - cancels all timers and clears state`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        timerManager.scheduleTimer("timer-1", 5000L) {}
        timerManager.scheduleTimer("timer-2", 5000L) {}
        timerManager.scheduleTimer("timer-3", 5000L) {}
        assertEquals(3, timerManager.getActiveTimerCount())

        // Act
        timerManager.cleanup()

        // Assert
        assertEquals(0, timerManager.getActiveTimerCount())
        assertFalse(timerManager.isTimerScheduled("timer-1"))
        assertFalse(timerManager.isTimerScheduled("timer-2"))
        assertFalse(timerManager.isTimerScheduled("timer-3"))

        destroyApp()
    }

    @Test
    fun `cleanup - does not throw on empty state`() = testScope.runTest {
        // Arrange
        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act & Assert - should not throw
        timerManager.cleanup()
        assertEquals(0, timerManager.getActiveTimerCount())

        destroyApp()
    }

    // ==================== APP BACKGROUND TESTS ====================

    @Test
    fun `onAppBackground - cancels all active timers`() = testScope.runTest {
        // Arrange
        var callbackCount = 0

        timerManager = createTimerManager()
        moveAppToForeground()

        timerManager.scheduleTimer("timer-1", 10000L) { callbackCount++ }
        timerManager.scheduleTimer("timer-2", 10000L) { callbackCount++ }
        assertEquals(2, timerManager.getActiveTimerCount())

        // Fast forward 5 seconds
        advanceTimeBy(5000)

        // Act - App goes to background
        moveAppToBackground()

        // Assert - Timers cancelled
        assertEquals(0, timerManager.getActiveTimerCount())

        // Fast forward past original delay - callbacks should NOT fire
        advanceTimeBy(10000)
        assertEquals(0, callbackCount)

        destroyApp()
    }

    // ==================== APP FOREGROUND TESTS ====================

    @Test
    fun `onAppForeground - reschedules cancelled timers with remaining time`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        moveAppToForeground()

        // Schedule timer for 10 seconds
        timerManager.scheduleTimer("timer-1", 10000L) { result ->
            callbackResult = result
        }

        // Fast forward 3 seconds
        advanceTimeBy(3000)
        testClock.advanceTime(3000) // currentTime = 4000ms

        // App goes to background
        moveAppToBackground(runPendingCoroutines = false)

        // Time passes in background (timer cancelled but time tracked)
        testClock.advanceTime(2000) // currentTime = 6000ms, 5s elapsed total

        // Assert - Timer not fired yet
        assertNull(callbackResult)

        // Act - App comes to foreground (should reschedule with ~5s remaining)
        moveAppToForeground(runPendingCoroutines = false)
        assertNull(callbackResult) // Still not fired

        // Fast forward remaining time (5 seconds + buffer)
        advanceTimeBy(5001)

        // Assert - Timer should fire now
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)
        assertEquals("timer-1", (callbackResult as InAppTimerManager.TimerResult.Completed).id)

        destroyApp()
    }

    @Test
    fun `onAppForeground - discards expired timers`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        moveAppToForeground()

        // Schedule timer for 5 seconds
        timerManager.scheduleTimer("timer-1", 5000L) { result ->
            callbackResult = result
        }

        // Fast forward 2 seconds
        advanceTimeBy(2000)
        testClock.advanceTime(2000)

        // App goes to background
        moveAppToBackground(runPendingCoroutines = false)

        // Time passes in background - MORE than remaining time
        testClock.advanceTime(10000) // 10 seconds pass, timer should have expired

        // Act - App comes to foreground
        moveAppToForeground()

        // Assert - Timer should be discarded, not completed
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Discarded)
        assertEquals("timer-1", (callbackResult as InAppTimerManager.TimerResult.Discarded).id)

        destroyApp()
    }

    @Test
    fun `onAppForeground - handles mixed expired and active timers`() = testScope.runTest {
        // Arrange
        val callbackResults = mutableListOf<InAppTimerManager.TimerResult>()

        timerManager = createTimerManager()
        moveAppToForeground()

        // Schedule timers with different delays
        timerManager.scheduleTimer("timer-short", 5000L) { callbackResults.add(it) }  // 5s
        timerManager.scheduleTimer("timer-long", 20000L) { callbackResults.add(it) }  // 20s

        // Fast forward 3 seconds in foreground
        advanceTimeBy(3000)
        testClock.advanceTime(3000)

        // App goes to background
        moveAppToBackground(runPendingCoroutines = false)

        // Time passes in background - 7 seconds (timer-short expires, timer-long has time remaining)
        testClock.advanceTime(7000) // Total elapsed: 10s

        // Act - App comes to foreground
        moveAppToForeground(runPendingCoroutines = false)
        runCurrent()

        // Assert - timer-short should be discarded immediately
        assertEquals(1, callbackResults.size)
        assertTrue(callbackResults[0] is InAppTimerManager.TimerResult.Discarded)
        assertEquals(
            "timer-short",
            (callbackResults[0] as InAppTimerManager.TimerResult.Discarded).id
        )

        // Fast forward remaining time for timer-long (20s - 10s = 10s)
        advanceTimeBy(10001)

        // Assert - timer-long should complete
        assertEquals(2, callbackResults.size)
        assertTrue(callbackResults[1] is InAppTimerManager.TimerResult.Completed)
        assertEquals(
            "timer-long",
            (callbackResults[1] as InAppTimerManager.TimerResult.Completed).id
        )

        destroyApp()
    }

    @Test
    fun `onAppForeground - handles exact expiry time as discarded`() = testScope.runTest {
        // Scenario: Timer expires exactly when app comes to foreground (remainingTime = 0)
        // Expected: Should be discarded, not rescheduled

        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        moveAppToForeground()

        // Schedule timer for 10 seconds
        timerManager.scheduleTimer("timer-1", 10000L) { result ->
            callbackResult = result
        }

        // Fast forward 5 seconds
        advanceTimeBy(5000)
        testClock.advanceTime(5000)

        // App goes to background
        moveAppToBackground(runPendingCoroutines = false)

        // Exactly 5 seconds pass (timer exactly expires)
        testClock.advanceTime(5000) // Total: 10s = exact delay

        // Act - App comes to foreground
        moveAppToForeground()

        // Assert - Should be discarded (remainingTime <= 0)
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Discarded)

        destroyApp()
    }

    @Test
    fun `onAppForeground - multiple background-foreground cycles`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        moveAppToForeground()

        // Schedule timer for 30 seconds
        timerManager.scheduleTimer("timer-1", 30000L) { result ->
            callbackResult = result
        }

        // Cycle 1: 5s foreground, 3s background
        advanceTimeBy(5000)
        testClock.advanceTime(5000) // elapsed: 5s
        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(3000) // elapsed: 8s
        moveAppToForeground(runPendingCoroutines = false)

        // Cycle 2: 4s foreground, 6s background
        advanceTimeBy(4000)
        testClock.advanceTime(4000) // elapsed: 12s
        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(6000) // elapsed: 18s
        moveAppToForeground(runPendingCoroutines = false)

        // Cycle 3: 7s foreground, 2s background
        advanceTimeBy(7000)
        testClock.advanceTime(7000) // elapsed: 25s
        moveAppToBackground(runPendingCoroutines = false)
        testClock.advanceTime(2000) // elapsed: 27s
        moveAppToForeground(runPendingCoroutines = false)

        // Still not fired
        assertNull(callbackResult)

        // Final 3 seconds to complete
        advanceTimeBy(3001)

        // Assert
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)

        destroyApp()
    }

    // ==================== TAG SUFFIX TESTS ====================

    @Test
    fun `timerManager with different tagSuffix creates separate instance`() = testScope.runTest {
        // Arrange
        val timerManager1 = createTimerManager(tagSuffix = "Delayed")
        val timerManager2 = createTimerManager(tagSuffix = "InAction")

        handleLifecycleStates()

        // Act
        timerManager1.scheduleTimer("timer-1", 5000L) {}
        timerManager2.scheduleTimer("timer-2", 5000L) {}

        // Assert - Each manager has its own timers
        assertEquals(1, timerManager1.getActiveTimerCount())
        assertEquals(1, timerManager2.getActiveTimerCount())
        assertTrue(timerManager1.isTimerScheduled("timer-1"))
        assertFalse(timerManager1.isTimerScheduled("timer-2"))
        assertFalse(timerManager2.isTimerScheduled("timer-1"))
        assertTrue(timerManager2.isTimerScheduled("timer-2"))

        destroyApp()
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `scheduleTimer - handles very short delay`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act - Schedule with 1ms delay
        timerManager.scheduleTimer("timer-1", 1L) { result ->
            callbackResult = result
        }

        advanceTimeBy(2)

        // Assert
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)

        destroyApp()
    }

    @Test
    fun `scheduleTimer - handles very long delay`() = testScope.runTest {
        // Arrange
        var callbackResult: InAppTimerManager.TimerResult? = null

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act - Schedule with 1 hour delay
        timerManager.scheduleTimer("timer-1", 3600000L) { result ->
            callbackResult = result
        }

        // Assert - Timer is scheduled
        assertTrue(timerManager.isTimerScheduled("timer-1"))
        assertNull(callbackResult)

        // Fast forward 1 hour
        advanceTimeBy(3600001)

        // Assert - Timer completed
        assertNotNull(callbackResult)
        assertTrue(callbackResult is InAppTimerManager.TimerResult.Completed)

        destroyApp()
    }


    @Test
    fun `concurrent timers with same delay complete together`() = testScope.runTest {
        // Arrange
        val callbackResults = mutableListOf<String>()

        timerManager = createTimerManager()
        handleLifecycleStates()

        // Act - Schedule multiple timers with same delay
        timerManager.scheduleTimer("timer-a", 5000L) { callbackResults.add("a") }
        timerManager.scheduleTimer("timer-b", 5000L) { callbackResults.add("b") }
        timerManager.scheduleTimer("timer-c", 5000L) { callbackResults.add("c") }

        assertEquals(3, timerManager.getActiveTimerCount())

        advanceTimeBy(5001)

        // Assert - All should complete
        assertEquals(3, callbackResults.size)
        assertTrue(callbackResults.contains("a"))
        assertTrue(callbackResults.contains("b"))
        assertTrue(callbackResults.contains("c"))
        assertEquals(0, timerManager.getActiveTimerCount())

        destroyApp()
    }
}