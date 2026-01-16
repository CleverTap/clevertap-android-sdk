package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InAppSchedulerTest {

    private lateinit var mockTimerManager: InAppTimerManager
    private lateinit var mockStorageStrategy: InAppSchedulingStrategy
    private lateinit var mockDataExtractor: InAppDataExtractor<TestResult>
    private lateinit var mockLogger: ILogger
    private lateinit var testScope: TestScope
    private lateinit var scheduler: InAppScheduler<TestResult>

    private val accountId = "test_account_id"

    @Before
    fun setUp() {
        mockTimerManager = mockk(relaxed = true)
        mockStorageStrategy = mockk(relaxed = true)
        mockDataExtractor = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        scheduler = InAppScheduler(
            timerManager = mockTimerManager,
            storageStrategy = mockStorageStrategy,
            dataExtractor = mockDataExtractor,
            logger = mockLogger,
            accountId = accountId
        )
        testScope = TestScope()
    }

    // ==================== SCHEDULE - FILTERING TESTS ====================

    @Test
    fun `schedule filters out already scheduled in-apps`() {
        // Arrange
        val inApp1 = createInApp("inapp1")
        val inApp2 = createInApp("inapp2")
        val inApps = listOf(inApp1, inApp2)

        every { mockTimerManager.isTimerScheduled("inapp1") } returns true
        every { mockTimerManager.isTimerScheduled("inapp2") } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L

        val callback: (TestResult) -> Unit = {}

        // Act
        scheduler.schedule(inApps, callback)

        // Assert - prepareForScheduling should only receive inapp2
        verify {
            mockStorageStrategy.prepareForScheduling(match { list ->
                list.size == 1 && list[0].optString(Constants.INAPP_ID_IN_PAYLOAD) == "inapp2"
            })
        }
    }

    @Test
    fun `schedule filters out all in-apps when all already scheduled`() {
        // Arrange
        val inApps = listOf(createInApp("inapp1"), createInApp("inapp2"))

        every { mockTimerManager.isTimerScheduled(any()) } returns true

        val callback: (TestResult) -> Unit = {}

        // Act
        scheduler.schedule(inApps, callback)

        // Assert - prepareForScheduling should receive empty list
        verify {
            mockStorageStrategy.prepareForScheduling(emptyList())
        }
    }

    @Test
    fun `schedule does not filter any in-apps when none are scheduled`() {
        // Arrange
        val inApps = listOf(createInApp("inapp1"), createInApp("inapp2"), createInApp("inapp3"))

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L

        val callback: (TestResult) -> Unit = {}

        // Act
        scheduler.schedule(inApps, callback)

        // Assert - prepareForScheduling should receive all 3 in-apps
        verify {
            mockStorageStrategy.prepareForScheduling(match { it.size == 3 })
        }
    }

    // ==================== SCHEDULE - PREPARATION TESTS ====================

    @Test
    fun `schedule calls error callback when preparation fails`() {
        // Arrange
        val inApps = listOf(createInApp("inapp1"), createInApp("inapp2"))
        val results = mutableListOf<TestResult>()

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns false
        every { mockDataExtractor.createErrorResult(any(), any()) } answers {
            TestResult.Error(firstArg(), secondArg())
        }

        // Act
        scheduler.schedule(inApps) { results.add(it) }

        // Assert - should call error callback for each in-app
        assertEquals(2, results.size)
        assertTrue(results.all { it is TestResult.Error })
        verify(exactly = 2) { mockDataExtractor.createErrorResult(any(), "Preparation failed") }
        verify(exactly = 0) { mockTimerManager.scheduleTimer(any(), any(), any()) }
    }


    // ==================== SCHEDULE - TIMER SCHEDULING TESTS ====================

    @Test
    fun `schedule schedules timer for each in-app with valid delay`() {
        // Arrange
        val inApps = listOf(
            createInApp("inapp1"),
            createInApp("inapp2"),
            createInApp("inapp3")
        )

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L

        val callback: (TestResult) -> Unit = {}

        // Act
        scheduler.schedule(inApps, callback)

        // Assert - scheduleTimer should be called for each in-app
        verify(exactly = 3) { mockTimerManager.scheduleTimer(any(), eq(5000L), any()) }
        verify { mockTimerManager.scheduleTimer("inapp1", 5000L, any()) }
        verify { mockTimerManager.scheduleTimer("inapp2", 5000L, any()) }
        verify { mockTimerManager.scheduleTimer("inapp3", 5000L, any()) }
    }

    @Test
    fun `schedule does not schedule timer for in-app with zero delay`() {
        // Arrange
        val inApps = listOf(createInApp("inapp1"))

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 0L

        val callback: (TestResult) -> Unit = {}

        // Act
        scheduler.schedule(inApps, callback)

        // Assert - scheduleTimer should not be called for zero delay
        verify(exactly = 0) { mockTimerManager.scheduleTimer(any(), any(), any()) }
    }

    @Test
    fun `schedule schedules timers with different delays`() {
        // Arrange
        val inApp1 = createInApp("inapp1")
        val inApp2 = createInApp("inapp2")
        val inApps = listOf(inApp1, inApp2)

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(inApp1) } returns 5000L
        every { mockDataExtractor.extractDelay(inApp2) } returns 10000L

        val callback: (TestResult) -> Unit = {}

        // Act
        scheduler.schedule(inApps, callback)

        // Assert
        verify { mockTimerManager.scheduleTimer("inapp1", 5000L, any()) }
        verify { mockTimerManager.scheduleTimer("inapp2", 10000L, any()) }
    }

    @Test
    fun `schedule handles mixed valid and zero delays`() {
        // Arrange
        val inApp1 = createInApp("inapp1")
        val inApp2 = createInApp("inapp2")
        val inApp3 = createInApp("inapp3")
        val inApps = listOf(inApp1, inApp2, inApp3)

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(inApp1) } returns 5000L
        every { mockDataExtractor.extractDelay(inApp2) } returns 0L // zero delay
        every { mockDataExtractor.extractDelay(inApp3) } returns 10000L

        val callback: (TestResult) -> Unit = {}

        // Act
        scheduler.schedule(inApps, callback)

        // Assert - only inapp1 and inapp3 should be scheduled
        verify { mockTimerManager.scheduleTimer("inapp1", 5000L, any()) }
        verify(exactly = 0) { mockTimerManager.scheduleTimer("inapp2", any(), any()) }
        verify { mockTimerManager.scheduleTimer("inapp3", 10000L, any()) }
    }

    // ==================== TIMER RESULT - COMPLETED TESTS ====================

    @Test
    fun `timer completed calls success callback when data found`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()
        val results = mutableListOf<TestResult>()
        val retrievedData = JSONObject().put("id", "inapp1").put("title", "Test")

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every { mockStorageStrategy.retrieveAfterTimer("inapp1") } returns retrievedData
        every {
            mockDataExtractor.createSuccessResult(
                "inapp1",
                retrievedData
            )
        } returns TestResult.Success("inapp1", retrievedData)

        // Act
        scheduler.schedule(listOf(inApp)) { results.add(it) }

        // Simulate timer completion
        callbackSlot.captured(
            InAppTimerManager.TimerResult.Completed(
                "inapp1",
                System.currentTimeMillis()
            )
        )

        // Assert
        assertEquals(1, results.size)
        assertTrue(results[0] is TestResult.Success)
        verify { mockStorageStrategy.retrieveAfterTimer("inapp1") }
        verify { mockDataExtractor.createSuccessResult("inapp1", retrievedData) }
        verify { mockStorageStrategy.clear("inapp1") }
    }

    @Test
    fun `timer completed calls error callback when data not found`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()
        val results = mutableListOf<TestResult>()

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every { mockStorageStrategy.retrieveAfterTimer("inapp1") } returns null
        every {
            mockDataExtractor.createErrorResult(
                "inapp1",
                "Data not found"
            )
        } returns TestResult.Error("inapp1", "Data not found")

        // Act
        scheduler.schedule(listOf(inApp)) { results.add(it) }

        // Simulate timer completion
        callbackSlot.captured(
            InAppTimerManager.TimerResult.Completed(
                "inapp1",
                System.currentTimeMillis()
            )
        )

        // Assert
        assertEquals(1, results.size)
        assertTrue(results[0] is TestResult.Error)
        verify { mockStorageStrategy.retrieveAfterTimer("inapp1") }
        verify { mockDataExtractor.createErrorResult("inapp1", "Data not found") }
        verify { mockStorageStrategy.clear("inapp1") }
    }

    @Test
    fun `timer completed clears storage after success`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()
        val retrievedData = JSONObject()

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every { mockStorageStrategy.retrieveAfterTimer(any()) } returns retrievedData
        every {
            mockDataExtractor.createSuccessResult(
                any(),
                any()
            )
        } returns TestResult.Success("inapp1", retrievedData)

        // Act
        scheduler.schedule(listOf(inApp)) {}
        callbackSlot.captured(
            InAppTimerManager.TimerResult.Completed(
                "inapp1",
                System.currentTimeMillis()
            )
        )

        // Assert
        verifyOrder {
            mockStorageStrategy.retrieveAfterTimer("inapp1")
            mockStorageStrategy.clear("inapp1")
        }
    }

    // ==================== TIMER RESULT - ERROR TESTS ====================

    @Test
    fun `timer error calls error callback with exception message`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()
        val results = mutableListOf<TestResult>()
        val exception = RuntimeException("Timer failed")

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every {
            mockDataExtractor.createErrorResult(
                "inapp1",
                "Timer failed"
            )
        } returns TestResult.Error("inapp1", "Timer failed")

        // Act
        scheduler.schedule(listOf(inApp)) { results.add(it) }

        // Simulate timer error
        callbackSlot.captured(InAppTimerManager.TimerResult.Error("inapp1", exception))

        // Assert
        assertEquals(1, results.size)
        assertTrue(results[0] is TestResult.Error)
        assertEquals("Timer failed", (results[0] as TestResult.Error).message)
        verify { mockDataExtractor.createErrorResult("inapp1", "Timer failed") }
    }

    @Test
    fun `timer error with null message uses unknown error`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()
        val results = mutableListOf<TestResult>()
        val exception = RuntimeException()

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every {
            mockDataExtractor.createErrorResult(
                "inapp1",
                "Unknown error"
            )
        } returns TestResult.Error("inapp1", "Unknown error")

        // Act
        scheduler.schedule(listOf(inApp)) { results.add(it) }

        // Simulate timer error with null message
        callbackSlot.captured(InAppTimerManager.TimerResult.Error("inapp1", exception))

        // Assert
        verify { mockDataExtractor.createErrorResult("inapp1", "Unknown error") }
    }

    @Test
    fun `timer error clears storage`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()
        val exception = RuntimeException("Error")

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every {
            mockDataExtractor.createErrorResult(
                any(),
                any()
            )
        } returns TestResult.Error("inapp1", "Error")

        // Act
        scheduler.schedule(listOf(inApp)) {}
        callbackSlot.captured(InAppTimerManager.TimerResult.Error("inapp1", exception))

        // Assert
        verify { mockStorageStrategy.clear("inapp1") }
    }

    // ==================== TIMER RESULT - DISCARDED TESTS ====================

    @Test
    fun `timer discarded calls discarded callback`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()
        val results = mutableListOf<TestResult>()

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every { mockDataExtractor.createDiscardedResult("inapp1") } returns TestResult.Discarded(
            "inapp1",
            "Timer expired"
        )

        // Act
        scheduler.schedule(listOf(inApp)) { results.add(it) }

        // Simulate timer discarded
        callbackSlot.captured(InAppTimerManager.TimerResult.Discarded("inapp1"))

        // Assert
        assertEquals(1, results.size)
        assertTrue(results[0] is TestResult.Discarded)
        verify { mockDataExtractor.createDiscardedResult("inapp1") }
    }

    @Test
    fun `timer discarded clears storage`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val callbackSlot = slot<(InAppTimerManager.TimerResult) -> Unit>()

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every {
            mockTimerManager.scheduleTimer(
                any(),
                any(),
                capture(callbackSlot)
            )
        } returns mockk()
        every { mockDataExtractor.createDiscardedResult(any()) } returns TestResult.Discarded(
            "inapp1",
            "Timer expired"
        )

        // Act
        scheduler.schedule(listOf(inApp)) {}
        callbackSlot.captured(InAppTimerManager.TimerResult.Discarded("inapp1"))

        // Assert
        verify { mockStorageStrategy.clear("inapp1") }
    }

    // ==================== GET ACTIVE COUNT TESTS ====================

    @Test
    fun `getActiveCount returns count from timer manager`() {
        // Arrange
        every { mockTimerManager.getActiveTimerCount() } returns 5

        // Act
        val count = scheduler.getActiveCount()

        // Assert
        assertEquals(5, count)
        verify { mockTimerManager.getActiveTimerCount() }
    }

    @Test
    fun `getActiveCount returns 0 when no active timers`() {
        // Arrange
        every { mockTimerManager.getActiveTimerCount() } returns 0

        // Act
        val count = scheduler.getActiveCount()

        // Assert
        assertEquals(0, count)
    }


    // ==================== CANCEL ALL SCHEDULING TESTS ====================

    @Test
    fun `cancelAllScheduling cleans up both timer and storage`() = testScope.runTest {
        // Arrange
        coEvery { mockTimerManager.cleanup() } returns Unit

        // Act
        scheduler.cancelAllScheduling()

        // Assert
        coVerify {
            mockTimerManager.cleanup()
            mockStorageStrategy.clearAll()
        }
    }

    @Test
    fun `schedule multiple times does not duplicate timers for same id`() {
        // Arrange
        val inApp = createInApp("inapp1")

        every { mockTimerManager.isTimerScheduled("inapp1") } returns false andThen true
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L

        // Act - schedule twice
        scheduler.schedule(listOf(inApp)) {}
        scheduler.schedule(listOf(inApp)) {}

        // Assert - only first schedule should add timer
        verify(exactly = 1) { mockTimerManager.scheduleTimer("inapp1", 5000L, any()) }
    }

    // ==================== MULTIPLE IN-APPS COMPLETION TESTS ====================

    @Test
    fun `mixed timer results handled correctly`() {
        // Arrange
        val inApps = listOf(createInApp("success"), createInApp("error"), createInApp("discarded"))
        val results = mutableListOf<TestResult>()
        val callbacks = mutableMapOf<String, (InAppTimerManager.TimerResult) -> Unit>()

        every { mockTimerManager.isTimerScheduled(any()) } returns false
        every { mockStorageStrategy.prepareForScheduling(any()) } returns true
        every { mockDataExtractor.extractDelay(any()) } returns 5000L
        every { mockTimerManager.scheduleTimer(any(), any(), any()) } answers {
            callbacks[firstArg()] = thirdArg()
            mockk()
        }
        every { mockStorageStrategy.retrieveAfterTimer("success") } returns JSONObject()
        every {
            mockDataExtractor.createSuccessResult(
                any(),
                any()
            )
        } returns TestResult.Success("success", JSONObject())
        every { mockDataExtractor.createErrorResult(any(), any()) } answers {
            TestResult.Error(
                firstArg(),
                secondArg()
            )
        }
        every { mockDataExtractor.createDiscardedResult(any()) } answers {
            TestResult.Discarded(
                firstArg(),
                "expired"
            )
        }

        // Act
        scheduler.schedule(inApps) { results.add(it) }

        callbacks["success"]?.invoke(
            InAppTimerManager.TimerResult.Completed(
                "success",
                System.currentTimeMillis()
            )
        )
        callbacks["error"]?.invoke(
            InAppTimerManager.TimerResult.Error(
                "error",
                RuntimeException("Failed")
            )
        )
        callbacks["discarded"]?.invoke(InAppTimerManager.TimerResult.Discarded("discarded"))

        // Assert
        assertEquals(3, results.size)
        assertTrue(results[0] is TestResult.Success)
        assertTrue(results[1] is TestResult.Error)
        assertTrue(results[2] is TestResult.Discarded)
    }

    // ==================== HELPER METHODS ====================

    private fun createInApp(id: String): JSONObject {
        return JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, id)
    }

    // Test result sealed class for testing
    sealed class TestResult {
        data class Success(val id: String, val data: JSONObject) : TestResult()
        data class Error(val id: String, val message: String) : TestResult()
        data class Discarded(val id: String, val reason: String) : TestResult()
    }
}