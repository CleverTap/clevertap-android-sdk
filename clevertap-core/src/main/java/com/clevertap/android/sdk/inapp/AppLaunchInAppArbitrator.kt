package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Holds the `/a1` app-launch in-app winner, buffers the `/content` winner, and shows exactly one —
 * the higher priority of the two — so a single launch never shows two app-launch in-apps.
 *
 * Three phases: no window (`phase == null`, shows immediately), OPEN (buffering), CLOSED
 * (suppressing — a winner has been shown, late responses dropped; this is what guarantees "exactly
 * one"). State transitions run under [lock]; [showWinner] is invoked outside it.
 *
 * Teardown self-heals and does not depend on the external completion signal: whichever fires first
 * between [onContentFetchComplete] and a hard [hardTeardownMs] backstop (on this arbiter's own
 * never-cancelled scope) tears the window down, so a skipped completion can't leave it stuck.
 */
internal class AppLaunchInAppArbitrator(
    private val logger: Logger,
    private val logTag: String,
    private val timeoutMs: Long,
    private val hardTeardownMs: Long,
    private val sortByPriority: (List<JSONObject>) -> List<JSONObject>,
    private val showWinner: (JSONObject) -> Unit,
    dispatchers: DispatcherProvider = CtDefaultDispatchers()
) {

    private enum class Phase { OPEN, CLOSED }

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io())

    private var phase: Phase? = null
    private var timeoutJob: Job? = null
    private val buffered = mutableListOf<JSONObject>()

    // Option 2 state: content candidates predicted against at /a1 time (empty = Option 1), and the
    // winner displayed (for the mispredict diagnostic).
    private var synthetics: List<JSONObject> = emptyList()
    private var shownWinner: JSONObject? = null

    /** Open a window and arm the timer. No-op if one is already open (the first window survives). */
    fun openWindow(syntheticCandidates: List<JSONObject> = emptyList()) = synchronized(lock) {
        if (phase != null) {
            logger.verbose(logTag, "[Arbitration] window already open, ignoring")
            return
        }
        phase = Phase.OPEN
        buffered.clear()
        synthetics = syntheticCandidates
        shownWinner = null
        // Lifecycle timer on the arbiter's own (never-cancelled) scope: show the /a1 winner at the
        // timeout, then self-heal at the hard backstop even if completion never fires.
        timeoutJob = scope.launch {
            try {
                delay(timeoutMs)
                closeAndShow("timeout")
                delay((hardTeardownMs - timeoutMs).coerceAtLeast(0))
                forceTeardown("hard backstop")
            } catch (c: CancellationException) {
                throw c // normal cancellation (completion / abandon)
            } catch (t: Throwable) {
                // Never let a timer failure crash the host app; tear down so it can't get stuck.
                logger.verbose(logTag, "[Arbitration] lifecycle timer failed; tearing down", t)
                forceTeardown("lifecycle failure")
            }
        }
        logger.verbose(logTag, "[Arbitration] window opened (synthetics=${syntheticCandidates.size})")
    }

    /** Synthetic candidates of the OPEN window (Option 2), or null when none / no window. */
    fun syntheticCandidates(): List<JSONObject>? = synchronized(lock) {
        if (phase == Phase.OPEN) synthetics else null
    }

    /**
     * Option 2 fast path: caller has decided the `/a1` winner wins and is showing [shown] itself.
     * Move to the suppressing phase; the timer is left running so the backstop still tears down.
     */
    fun closeForFastPath(shown: JSONObject) = synchronized(lock) {
        if (phase != Phase.OPEN) {
            return
        }
        phase = Phase.CLOSED
        shownWinner = shown
        logger.verbose(logTag, "[Arbitration] fast path: /a1 winner shown immediately, window closed")
    }

    /** Route a response's app-launch winners: returned as-is when no window, else buffered (OPEN) or dropped (CLOSED). */
    fun routeWinners(winners: List<JSONObject>): List<JSONObject> = synchronized(lock) {
        when (phase) {
            null -> winners
            Phase.OPEN -> {
                buffered.addAll(winners)
                logger.verbose(logTag, "[Arbitration] buffered ${winners.size} winner(s), holding")
                emptyList()
            }
            Phase.CLOSED -> {
                logMispredictIfAny(winners)
                logger.verbose(logTag, "[Arbitration] window closed, dropping ${winners.size} late winner(s)")
                emptyList()
            }
        }
    }

    // Logs when a dropped late winner would have outranked the one already shown (a misprediction).
    private fun logMispredictIfAny(dropped: List<JSONObject>) {
        val shown = shownWinner ?: return
        val outranks = dropped.any { sortByPriority(listOf(shown, it)).firstOrNull() === it }
        if (outranks) {
            logger.verbose(
                logTag,
                "[Arbitration] MISPREDICT: a dropped content winner would have outranked the shown in-app"
            )
        }
    }

    /** Completion signal for the content-fetch batch: show the merged winner if not already shown, then tear down. */
    fun onContentFetchComplete() {
        closeAndShow("content fetch complete")
        teardown("content fetch complete")
    }

    private fun forceTeardown(reason: String) = teardown(reason)

    /** Discard the window WITHOUT showing — used on user switch so the previous user's in-app isn't shown to the new one. */
    fun abandon() = teardown("user switch")

    // Resets to the no-window state. Idempotent under [lock].
    private fun teardown(reason: String) = synchronized(lock) {
        if (phase == null) {
            return // already torn down
        }
        timeoutJob?.cancel()
        timeoutJob = null
        phase = null
        buffered.clear()
        synthetics = emptyList()
        shownWinner = null
        logger.verbose(logTag, "[Arbitration] window torn down ($reason)")
    }

    private fun closeAndShow(reason: String) {
        val winner: JSONObject? = synchronized(lock) {
            if (phase != Phase.OPEN) {
                return
            }
            phase = Phase.CLOSED
            val selected = sortByPriority(buffered).firstOrNull()
            shownWinner = selected
            logger.verbose(
                logTag,
                "[Arbitration] closing ($reason): ${buffered.size} buffered -> ${if (selected != null) "1 winner" else "nothing"}"
            )
            selected
        }
        if (winner != null) {
            showWinner(winner)
        }
    }
}
