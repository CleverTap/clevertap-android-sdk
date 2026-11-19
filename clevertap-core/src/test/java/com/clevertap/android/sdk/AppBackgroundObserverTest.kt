package com.clevertap.android.sdk

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppBackgroundObserverTest : BaseTestCase() {

    private lateinit var lifecycleOwner: TestLifecycleOwner
    private var backgroundCount = 0

    @Before
    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(UnconfinedTestDispatcher())
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        backgroundCount = 0
        AppBackgroundObserver(
            lifecycleOwner = lifecycleOwner,
            accountId = cleverTapInstanceConfig.accountId,
            logger = cleverTapInstanceConfig.logger,
            onAppBackground = { backgroundCount++ }
        ).register()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_onStop_afterForeground_invokesCallback() {
        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        lifecycleOwner.currentState = Lifecycle.State.CREATED

        assertEquals(1, backgroundCount)
    }

    @Test
    fun test_foregroundOnly_doesNotInvokeCallback() {
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        assertEquals(0, backgroundCount)
    }

    @Test
    fun test_eachBackgroundTransition_invokesCallbackOnce() {
        repeat(3) {
            lifecycleOwner.currentState = Lifecycle.State.RESUMED
            lifecycleOwner.currentState = Lifecycle.State.CREATED
        }

        assertEquals(3, backgroundCount)
    }

    @Test
    fun test_destroyAfterBackground_doesNotInvokeCallbackAgain() {
        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        lifecycleOwner.currentState = Lifecycle.State.DESTROYED

        assertEquals(1, backgroundCount)
    }

    @Test
    fun test_callbackThrows_isSwallowed() {
        val owner = TestLifecycleOwner(Lifecycle.State.CREATED)
        AppBackgroundObserver(
            lifecycleOwner = owner,
            accountId = cleverTapInstanceConfig.accountId,
            logger = cleverTapInstanceConfig.logger,
            onAppBackground = { throw IllegalStateException("boom") }
        ).register()

        owner.currentState = Lifecycle.State.RESUMED
        owner.currentState = Lifecycle.State.CREATED
    }

    @Test
    fun test_addObserverThrows_isSwallowedAndLogged() {
        val error = IllegalStateException("addObserver failed")
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val owner = object : LifecycleOwner {
            override val lifecycle: Lifecycle = AddObserverThrowingLifecycle(
                delegate = testOwner.lifecycle,
                error = error
            )
        }
        val logger = mockk<ILogger>(relaxed = true)
        var count = 0

        AppBackgroundObserver(
            lifecycleOwner = owner,
            accountId = cleverTapInstanceConfig.accountId,
            logger = logger,
            onAppBackground = { count++ }
        ).register()

        testOwner.currentState = Lifecycle.State.RESUMED
        testOwner.currentState = Lifecycle.State.CREATED

        verify {
            logger.verbose(cleverTapInstanceConfig.accountId, "Failed to register app background observer", error)
        }
        assertEquals(0, count)
    }

    @Test
    fun test_lifecycleScopeThrows_isSwallowedAndLogged() {
        val error = IllegalStateException("lifecycle unavailable")
        val owner = object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() = throw error
        }
        val logger = mockk<ILogger>(relaxed = true)

        AppBackgroundObserver(
            lifecycleOwner = owner,
            accountId = cleverTapInstanceConfig.accountId,
            logger = logger,
            onAppBackground = {}
        ).register()

        verify {
            logger.verbose(cleverTapInstanceConfig.accountId, "Failed to register app background observer", error)
        }
    }

    /**
     * Delegates to a real lifecycle so lifecycleScope can register its own observer,
     * but refuses [AppBackgroundObserver] to drive the failure path in register().
     */
    private class AddObserverThrowingLifecycle(
        private val delegate: Lifecycle,
        private val error: Throwable
    ) : Lifecycle() {
        override val currentState: State
            get() = delegate.currentState

        override fun addObserver(observer: LifecycleObserver) {
            if (observer is AppBackgroundObserver) {
                throw error
            }
            delegate.addObserver(observer)
        }

        override fun removeObserver(observer: LifecycleObserver) {
            delegate.removeObserver(observer)
        }
    }
}
