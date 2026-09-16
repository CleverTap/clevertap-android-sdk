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
 * - **OPEN** — winners are buffered; ends on completion or timeout.
 * - **CLOSED** (suppressing) — a winner has been shown; late responses are dropped. This, not the
 *   timeout, is what guarantees "exactly one". Torn down on completion.
 */
internal class AppLaunchInAppArbitrator(
    private val logger: Logger,
    private val logTag: String,
    private val timeoutMs: Long,
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

    /**
     * Open a window and arm the timeout. No-op if one is already open — the first window survives
     * (a second `/a1` with a content fetch does not restart arbitration).
     */
    fun openWindow() = synchronized(lock) {
        if (phase != null) {
            logger.verbose(logTag, "[Arbitration] window already open, ignoring")
            return
        }
        phase = Phase.OPEN
        buffered.clear()
        timeoutJob = scope.launch {
            delay(timeoutMs)
            // UX bound: show the /a1 winner now; the window stays in its CLOSED-suppressing phase
            // until completion tears it down, so a slow /content reply is dropped, not shown.
            closeAndShow("timeout")
        }
        logger.verbose(logTag, "[Arbitration] window opened")
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
                // TODO(SDK-6144): emit a mispredict diagnostic when a dropped winner would have
                // outranked the one already shown.
                logger.verbose(logTag, "[Arbitration] window closed, dropping ${winners.size} late winner(s)")
                emptyList()
            }
        }
    }

    /**
     * Completion signal for the content-fetch batch (success/error/timeout/cancellation). Closes the
     * window and shows the merged winner if not already shown, then tears everything down.
     */
    fun onContentFetchComplete() {
        closeAndShow("content fetch complete")
        synchronized(lock) {
            timeoutJob?.cancel()
            timeoutJob = null
            phase = null
            buffered.clear()
            logger.verbose(logTag, "[Arbitration] window torn down")
        }
    }

    private fun closeAndShow(reason: String) {
        val winner: JSONObject? = synchronized(lock) {
            if (phase != Phase.OPEN) return
            phase = Phase.CLOSED
            val selected = sortByPriority(buffered).firstOrNull()
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
