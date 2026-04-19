package com.clevertap.android.sdk.network.fetch

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkScopeTest {

    @Test
    fun `default dispatcher is Dispatchers IO`() {
        val scope = NetworkScope()
        assertSame(Dispatchers.IO, scope.coroutineScope.coroutineContext[ContinuationInterceptor])
    }

    @Test
    fun `custom dispatcher is used when provided`() {
        val custom = UnconfinedTestDispatcher()
        val scope = NetworkScope(custom)
        assertSame(custom, scope.coroutineScope.coroutineContext[ContinuationInterceptor])
    }

    @Test
    fun `SupervisorJob isolates child failures`() = runTest {
        val dispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        val scope = NetworkScope(dispatcher)
        val swallow = CoroutineExceptionHandler { _, _ -> /* isolate the failure */ }

        val failing = scope.coroutineScope.launch(swallow) { throw IllegalStateException("boom") }
        val sibling = scope.coroutineScope.launch { delay(10) }

        failing.join()
        sibling.join()

        assertTrue(sibling.isCompleted)
        assertFalse(sibling.isCancelled)
    }

    @Test
    fun `cancel deactivates the scope`() {
        val scope = NetworkScope()
        scope.cancel()
        assertFalse(scope.coroutineScope.isActive)
    }
}
