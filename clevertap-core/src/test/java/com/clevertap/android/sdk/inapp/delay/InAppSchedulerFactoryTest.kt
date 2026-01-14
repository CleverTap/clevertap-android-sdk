package com.clevertap.android.sdk.inapp.delay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class InAppSchedulerFactoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockLogger: ILogger
    private lateinit var testClock: TestClock
    private lateinit var testScope: TestScope
    private lateinit var testLifecycleOwner: TestLifecycleOwner
    private lateinit var mockDelayedLegacyInAppStore: DelayedLegacyInAppStore

    private val accountId = "test_account_id"

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        testClock = TestClock(1000L)
        mockDelayedLegacyInAppStore = mockk(relaxed = true)

        testScope = TestScope()
        testLifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        clearAllMocks()
    }

    // ==================== CREATE DELAYED IN-APP SCHEDULER TESTS ====================

    @Test
    fun `createDelayedInAppScheduler returns non-null scheduler`() {
        // Act
        val scheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        assertNotNull(scheduler)
    }

    @Test
    fun `createDelayedInAppScheduler with null store returns scheduler`() {
        // Act
        val scheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = null,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        assertNotNull(scheduler)
    }

    @Test
    fun `createDelayedInAppScheduler uses DelayedInAppStorageStrategy`() {
        // Act
        val scheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert - verify storage strategy type through scheduler's internal property
        assertTrue(scheduler.storageStrategy is DelayedInAppStorageStrategy)
    }

    @Test
    fun `createDelayedInAppScheduler passes delayedLegacyInAppStore to storage strategy`() {
        // Act
        val scheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        val storageStrategy = scheduler.storageStrategy as DelayedInAppStorageStrategy
        assertEquals(mockDelayedLegacyInAppStore, storageStrategy.delayedLegacyInAppStore)
    }

    @Test
    fun `createDelayedInAppScheduler with null store has null in storage strategy`() {
        // Act
        val scheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = null,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        val storageStrategy = scheduler.storageStrategy as DelayedInAppStorageStrategy
        assertNull(storageStrategy.delayedLegacyInAppStore)
    }

    @Test
    fun `createDelayedInAppScheduler returns scheduler with zero active count initially`() {
        // Act
        val scheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        assertEquals(0, scheduler.getActiveCount())
    }

    // ==================== CREATE IN-ACTION SCHEDULER TESTS ====================

    @Test
    fun `createInActionScheduler returns non-null scheduler`() {
        // Act
        val scheduler = InAppSchedulerFactory.createInActionScheduler(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        assertNotNull(scheduler)
    }

    @Test
    fun `createInActionScheduler uses InActionStorageStrategy`() {
        // Act
        val scheduler = InAppSchedulerFactory.createInActionScheduler(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert - verify storage strategy type
        assertTrue(scheduler.storageStrategy is InActionStorageStrategy)
    }

    @Test
    fun `createInActionScheduler returns scheduler with zero active count initially`() {
        // Act
        val scheduler = InAppSchedulerFactory.createInActionScheduler(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        assertEquals(0, scheduler.getActiveCount())
    }

    // ==================== COMPARISON TESTS ====================

    @Test
    fun `delayed and in-action schedulers use different storage strategies`() {
        // Act
        val delayedScheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        val inActionScheduler = InAppSchedulerFactory.createInActionScheduler(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert
        assertTrue(delayedScheduler.storageStrategy is DelayedInAppStorageStrategy)
        assertTrue(inActionScheduler.storageStrategy is InActionStorageStrategy)
        assertTrue(delayedScheduler.storageStrategy::class != inActionScheduler.storageStrategy::class)
    }

    @Test
    fun `delayed and in-action schedulers are independent instances`() {
        // Act
        val delayedScheduler = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        val inActionScheduler = InAppSchedulerFactory.createInActionScheduler(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert - different instances even with same accountId
        assertTrue(delayedScheduler !== inActionScheduler)
    }

    // ==================== MULTIPLE CREATION TESTS ====================

    @Test
    fun `multiple calls to createDelayedInAppScheduler create new instances`() {
        // Act
        val scheduler1 = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        val scheduler2 = InAppSchedulerFactory.createDelayedInAppScheduler(
            accountId = accountId,
            logger = mockLogger,
            delayedLegacyInAppStore = mockDelayedLegacyInAppStore,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert - each call creates a new instance
        assertTrue(scheduler1 !== scheduler2)
    }

    @Test
    fun `multiple calls to createInActionScheduler create new instances`() {
        // Act
        val scheduler1 = InAppSchedulerFactory.createInActionScheduler(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        val scheduler2 = InAppSchedulerFactory.createInActionScheduler(
            accountId = accountId,
            logger = mockLogger,
            clock = testClock,
            lifecycleOwner = testLifecycleOwner,
            scope = testScope
        )

        // Assert - each call creates a new instance
        assertTrue(scheduler1 !== scheduler2)
    }
}