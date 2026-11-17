package com.clevertap.android.sdk

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.clevertap.android.shared.test.BaseTestCase
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
            config = cleverTapInstanceConfig,
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
    fun test_callbackThrows_isSwallowed() {
        val owner = TestLifecycleOwner(Lifecycle.State.CREATED)
        AppBackgroundObserver(
            lifecycleOwner = owner,
            config = cleverTapInstanceConfig,
            onAppBackground = { throw IllegalStateException("boom") }
        ).register()

        owner.currentState = Lifecycle.State.RESUMED
        owner.currentState = Lifecycle.State.CREATED
    }
}
