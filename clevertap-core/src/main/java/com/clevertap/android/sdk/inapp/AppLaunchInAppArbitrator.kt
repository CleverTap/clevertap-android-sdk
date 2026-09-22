package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * State machine for the App-Launch × content-fetch in-app arbitration window (Option 1, SDK-6141).
 *
 * On App Launch, `/a1` can carry both `inapp_notifs_applaunched` and a `content_fetch`; the
 * `/content` reply is re-fed through the in-app chain and today produces a *second* app-launch
 * in-app. This arbiter holds the `/a1` winner, buffers the `/content` winner, and on close shows
 * exactly one — the higher-priority of the two.
 *
 * "Merge" here is winner-merge, not payload-merge: each response still runs the normal
 * evaluate→sort→select, and only the per-response winners are buffered. Because the priority sort
 * is a total order, `max(A ∪ B) == max(max(A), max(B))`, so buffering each winner is equivalent to
 * pooling all candidates — and cheaper.
 *
 * The window self-closes after [timeoutMs] via a cancellable coroutine ([timeoutJob]); the mutating
 * state transitions run under [lock] so the `/a1` worker thread and the `/content` coroutine can
 * touch the buffer safely. [showWinner] is invoked outside the lock.
 *
 * Three phases:
 * - **no window** (`phase == null`) — every response shows immediately (today's behaviour).
 * - **OPEN** — winners are buffered; ends on completion or the [timeoutMs] show-timeout.
 * - **CLOSED** (suppressing) — a winner has been shown; late responses are dropped. This, not the
 *   timeout, is what guarantees "exactly one".
 *
 * **Self-healing is not dependent on the external completion signal.** The window is torn down by
 * whichever comes first: [onContentFetchComplete] (the prompt path) or a hard [hardTeardownMs]
 * backstop armed on this arbiter's own scope (a `SupervisorJob` that is never cancelled and is
 * independent of the content-fetch coroutine, the decorator chain, and the callback wiring). So even
 * if the completion signal is skipped for any reason — cancelled fetch coroutine, an aborted
 * decorator loop, an unwired callback — the window cannot get permanently stuck and suppress
 * app-launch in-apps for the rest of the session. Completion is an optimization, not a correctness
 * dependency.
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

    // Option 2 state. [synthetics] are the content candidates predicted against at /a1 time (empty =
    // Option 1). [shownWinner] is what was displayed, used for the mispredict diagnostic.
    private var synthetics: List<JSONObject> = emptyList()
    private var shownWinner: JSONObject? = null

    /**
     * Open a window and arm the timeout. No-op if one is already open — the first window survives
     * (a second `/a1` with a content fetch does not restart arbitration).
     *
     * @param syntheticCandidates content candidates for the Option 2 fast path; empty means Option 1
     *   (wait), the only mode active until the backend sends `priority` per item.
     */
    fun openWindow(syntheticCandidates: List<JSONObject> = emptyList()) = synchronized(lock) {
        if (phase != null) {
            logger.verbose(logTag, "[Arbitration] window already open, ignoring")
            return
        }
        phase = Phase.OPEN
        buffered.clear()
        synthetics = syntheticCandidates
        shownWinner = null
        // Single lifecycle timer on this arbiter's own (never-cancelled) scope. It runs to completion
        // regardless of the content-fetch coroutine, so the window can never stick even if the
        // completion signal is skipped. Completion (onContentFetchComplete) cancels it and tears down
        // earlier on the happy path.
        timeoutJob = scope.launch {
            delay(timeoutMs)
            // UX bound: show the /a1 winner now (no-op if already closed via fast path / completion).
            closeAndShow("timeout")
            // Hard backstop: guarantee the CLOSED-suppressing phase ends and the window self-heals
            // even if no completion signal ever arrives. hardTeardownMs is comfortably beyond the
            // fetch's own request timeout, so by the time this fires no /content is still in flight.
            delay((hardTeardownMs - timeoutMs).coerceAtLeast(0))
            forceTeardown("hard backstop")
        }
        logger.verbose(logTag, "[Arbitration] window opened (synthetics=${syntheticCandidates.size})")
    }

    /**
     * The synthetic content candidates of the currently OPEN window (Option 2), or null when there
     * is no open window or it was opened without them (Option 1).
     */
    fun syntheticCandidates(): List<JSONObject>? = synchronized(lock) {
        if (phase == Phase.OPEN) synthetics else null
    }

    /**
     * Option 2 fast path: the caller has predicted the `/a1` winner wins and is showing [shown] now.
     * Move to the suppressing phase; a later `/content` winner is dropped (with a mispredict log if it
     * would have outranked [shown]). The lifecycle timer is deliberately left running so its hard
     * backstop still tears the window down if the completion signal never arrives.
     */
    fun closeForFastPath(shown: JSONObject) = synchronized(lock) {
        if (phase != Phase.OPEN) return
        phase = Phase.CLOSED
        shownWinner = shown
        logger.verbose(logTag, "[Arbitration] fast path: /a1 winner shown immediately, window closed")
    }

    /**
     * Route one response's app-launch winners.
     *
     * @return the winners to show now — the input unchanged when there is no window, or empty when
     *         they were buffered (OPEN) or dropped (CLOSED).
     */
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

    // Called under [lock]. Logs when a dropped late winner would have outranked the one already
    // shown — i.e. the fast path (or the timeout) mispredicted the outcome.
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

    /**
     * Completion signal for the content-fetch batch (success/error/timeout/cancellation). Closes the
     * window and shows the merged winner if not already shown, then tears everything down.
     */
    fun onContentFetchComplete() {
        closeAndShow("content fetch complete")
        teardown("content fetch complete")
    }

    // Hard backstop, fired by the lifecycle timer. Guarantees the window ends even if no completion
    // signal ever arrives. No-op if completion already tore it down.
    private fun forceTeardown(reason: String) = teardown(reason)

    // Resets to the no-window state. Idempotent under [lock].
    private fun teardown(reason: String) = synchronized(lock) {
        if (phase == null) return // already torn down
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
            if (phase != Phase.OPEN) return
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
