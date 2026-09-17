package com.clevertap.android.sdk.inapp

import TestDispatchers
import com.clevertap.android.sdk.Logger
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AppLaunchInAppArbitratorTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(scheduler)
    private val shown = mutableListOf<JSONObject>()

    // Mirrors EvaluationManager.sortByPriority: priority DESC, then ti ASC.
    private val sortByPriority: (List<JSONObject>) -> List<JSONObject> = { list ->
        list.sortedWith(
            compareByDescending<JSONObject> { it.optInt("priority", 1) }
                .thenBy { it.optString("ti") }
        )
    }

    private lateinit var arbitrator: AppLaunchInAppArbitrator

    @Before
    fun setUp() {
        shown.clear()
        arbitrator = AppLaunchInAppArbitrator(
            logger = mockk<Logger>(relaxed = true),
            logTag = "test",
            timeoutMs = 3000L,
            sortByPriority = sortByPriority,
            showWinner = { shown.add(it) },
            dispatchers = dispatchers
        )
    }

    private fun inApp(ti: String, priority: Int) =
        JSONObject().put("ti", ti).put("priority", priority)

    @Test
    fun `no window - winners are returned to caller unchanged`() {
        val winners = listOf(inApp("1", 1))
        assertEquals(winners, arbitrator.routeWinners(winners))
        assertTrue(shown.isEmpty()) // no window => arbitrator never shows; caller does
    }

    @Test
    fun `open window buffers both winners and shows the highest priority on completion`() {
        arbitrator.openWindow()
        assertTrue(arbitrator.routeWinners(listOf(inApp("100", 1))).isEmpty()) // /a1 buffered
        assertTrue(arbitrator.routeWinners(listOf(inApp("200", 5))).isEmpty()) // /content buffered

        arbitrator.onContentFetchComplete()

        assertEquals(1, shown.size)
        assertEquals("200", shown[0].optString("ti")) // priority 5 beats 1
    }

    @Test
    fun `equal priority merge resolves by earliest ti`() {
        arbitrator.openWindow()
        arbitrator.routeWinners(listOf(inApp("100", 1)))
        arbitrator.routeWinners(listOf(inApp("200", 1)))
        arbitrator.onContentFetchComplete()

        assertEquals(1, shown.size)
        assertEquals("100", shown[0].optString("ti")) // lower ti wins the tie
    }

    @Test
    fun `timeout shows the a1 winner and then suppresses late content winners`() {
        arbitrator.openWindow()
        arbitrator.routeWinners(listOf(inApp("100", 1))) // /a1 buffered

        scheduler.advanceUntilIdle() // fire the 3s timeout

        assertEquals(1, shown.size)
        assertEquals("100", shown[0].optString("ti"))

        // A /content winner arriving after the timeout is dropped, never shown as a second in-app.
        assertTrue(arbitrator.routeWinners(listOf(inApp("200", 9))).isEmpty())
        assertEquals(1, shown.size)
    }

    @Test
    fun `completion after timeout does not show a second in-app`() {
        arbitrator.openWindow()
        arbitrator.routeWinners(listOf(inApp("100", 1)))
        scheduler.advanceUntilIdle()

        arbitrator.onContentFetchComplete() // tears down only; no second show

        assertEquals(1, shown.size)
    }

    @Test
    fun `completion with only the a1 winner falls back to showing it`() {
        arbitrator.openWindow()
        arbitrator.routeWinners(listOf(inApp("100", 1)))

        arbitrator.onContentFetchComplete() // /content never produced a winner

        assertEquals(1, shown.size)
        assertEquals("100", shown[0].optString("ti"))
    }

    @Test
    fun `fast path close suppresses a later content winner without arbitrator showing`() {
        arbitrator.openWindow(listOf(inApp("200", 1))) // opened with synthetics (Option 2)
        assertNotNull(arbitrator.syntheticCandidates())

        arbitrator.closeForFastPath(inApp("100", 1)) // caller shows the /a1 winner itself
        assertNull(arbitrator.syntheticCandidates())  // window closed

        assertTrue(arbitrator.routeWinners(listOf(inApp("200", 1))).isEmpty()) // content dropped
        assertTrue(shown.isEmpty()) // fast path shows via the caller, not the arbitrator

        arbitrator.onContentFetchComplete()
    }

    @Test
    fun `syntheticCandidates is exposed only while a window with synthetics is open`() {
        assertNull(arbitrator.syntheticCandidates()) // no window
        arbitrator.openWindow() // Option 1 (no synthetics)
        assertEquals(0, arbitrator.syntheticCandidates()?.size)
        arbitrator.onContentFetchComplete()
        assertNull(arbitrator.syntheticCandidates())
    }

    @Test
    fun `second openWindow is a no-op - first window survives`() {
        arbitrator.openWindow(listOf(inApp("200", 1)))
        arbitrator.openWindow() // ignored
        assertEquals(1, arbitrator.syntheticCandidates()?.size)
    }
}
