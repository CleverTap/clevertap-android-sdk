package com.clevertap.android.sdk

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.testing.TestLifecycleOwner
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
        val lifecycle = mockk<Lifecycle> {
            every { addObserver(any()) } throws error
        }
        val owner = mockk<LifecycleOwner> {
            every { this@mockk.lifecycle } returns lifecycle
        }
        val scope = mockk<LifecycleCoroutineScope> {
            every { coroutineContext } returns UnconfinedTestDispatcher()
        }
        val logger = mockk<ILogger>(relaxed = true)

        mockkStatic("androidx.lifecycle.LifecycleOwnerKt") {
            every { owner.lifecycleScope } returns scope

            AppBackgroundObserver(
                lifecycleOwner = owner,
                accountId = cleverTapInstanceConfig.accountId,
                logger = logger,
                onAppBackground = {}
            ).register()
        }

        verify { lifecycle.addObserver(any<AppBackgroundObserver>()) }
        verify {
            logger.verbose(cleverTapInstanceConfig.accountId, "Failed to register app background observer", error)
        }
    }

    @Test
    fun test_lifecycleScopeThrows_isSwallowedAndLogged() {
        val error = IllegalStateException("lifecycle unavailable")
        val owner = mockk<LifecycleOwner> {
            every { lifecycle } throws error
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
}
