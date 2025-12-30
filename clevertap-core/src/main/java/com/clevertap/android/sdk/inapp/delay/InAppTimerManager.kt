package com.clevertap.android.sdk.inapp.delay


import androidx.annotation.WorkerThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.utils.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Core timer manager that handles scheduling, lifecycle, and callback management
 * This is the reusable component for both delay and in-action features
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppTimerManager(
    private val accountId: String,
    private val logger: Logger,
    private val clock: Clock = Clock.SYSTEM,
    private val scope: CoroutineScope = ProcessLifecycleOwner.get().lifecycleScope +
            Dispatchers.Default.limitedParallelism(PARALLEL_SCHEDULERS),
    private val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) {
    companion object {
        private const val PARALLEL_SCHEDULERS = 20
        private const val TAG = "[InAppTimerManager]:"
    }

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val cancelledJobs = ConcurrentHashMap<String, CancelledJobData>()

    init {
        scope.launch {
            logCoroutineInfo("lifeCycleOwner scope launch, $coroutineContext, ${coroutineContext[Job]?.parent}}")
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                logCoroutineInfo("process lifeCycleOwner: started, $coroutineContext, ${coroutineContext[Job]?.parent}}")
                try {
                    onAppForeground()
                    awaitCancellation()
                } catch (c: CancellationException) {
                    logCoroutineInfo("process lifeCycleOwner: Stopped, $coroutineContext, ${coroutineContext[Job]?.parent}}")
                    withContext(NonCancellable) {
                        logCoroutineInfo("process lifeCycleOwner: withContext block, $coroutineContext, ${coroutineContext[Job]?.parent}}")
                        onAppBackground()
                    }
                    ensureActive()
                }
            }
        }
    }

    /**
     * Schedule a timer with callback after specified delay
     * @param id Unique identifier for the timer
     * @param delayInMs Delay in milliseconds
     * @param callback Lambda invoked after delay completes
     * @return Job handle
     */
    @WorkerThread
    fun scheduleTimer(
        id: String,
        delayInMs: Long,
        callback: (TimerResult) -> Unit
    ): Job {
        // Keep existing active job if present
        activeJobs[id]?.let { existingJob ->
            if (existingJob.isActive) {
                logger.verbose(
                    accountId,
                    "$TAG Timer with id '$id' already scheduled, keeping existing"
                )
                return existingJob
            } else {
                activeJobs.remove(id)
            }
        }

        val job = scope.launch {
            val scheduledAt = clock.currentTimeMillis()
            try {
                delay(delayInMs)

                // Timer completed successfully
                callback(TimerResult.Completed(id, scheduledAt))
                cancelledJobs.remove(id)

            } catch (e: CancellationException) {
                logger.verbose(accountId, "$TAG Cancelled timer with id: $id")
                cancelledJobs.putIfAbsent(id, CancelledJobData(delayInMs, scheduledAt, callback))
                ensureActive() // rethrow cancellation
            } catch (e: Exception) {
                logger.verbose(accountId, "$TAG Error in timer with id: $id", e)
                callback(TimerResult.Error(id, e))
                cancelledJobs.remove(id)
            } finally {
                activeJobs.remove(id)
            }
        }

        activeJobs[id] = job
        logger.verbose(accountId, "$TAG Scheduled timer with id '$id' for ${delayInMs}ms delay")

        return job
    }

    /**
     * Cancel a specific timer by ID
     */
    fun cancelTimer(id: String): Boolean {
        return activeJobs[id]?.let { job ->
            job.cancel()
            activeJobs.remove(id)
            logger.verbose(accountId, "$TAG Cancelled timer with id: $id")
            true
        } ?: false
    }

    /**
     * Cancel all active timers
     */
    private suspend fun cancelAllTimers() {
        val cancelledCount = activeJobs.size
        val jobsToCancel = activeJobs.values.toList()
        jobsToCancel.forEach { it.cancelAndJoin() }
        logger.verbose(accountId, "$TAG Cancelled $cancelledCount timers")
    }

    /**
     * Get count of active timers
     */
    fun getActiveTimerCount(): Int = activeJobs.size

    /**
     * Check if a timer is scheduled and active
     */
    fun isTimerScheduled(id: String): Boolean {
        return activeJobs[id]?.isActive ?: false
    }

    /**
     * App went to background - cancel all timers
     */
    suspend fun onAppBackground() {
        cancelAllTimers()
    }

    /**
     * App came to foreground - reschedule cancelled timers with remaining time
     */
    fun onAppForeground() {
        logger.verbose(accountId, "$TAG Handling foreground - rescheduling cancelled timers")

        val currentTime = clock.currentTimeMillis()
        val toReschedule = mutableListOf<RescheduleData>()
        val toDiscard = mutableListOf<String>()

        cancelledJobs.forEach { (id, cancelledData) ->
            val originalDelayInMs = cancelledData.originalDelayInMs
            val scheduledAt = cancelledData.scheduledAt

            val elapsedTime = currentTime - scheduledAt
            val remainingTime = originalDelayInMs - elapsedTime

            logger.verbose(
                accountId,
                "$TAG Id $id - Original delay: ${originalDelayInMs}ms, " +
                        "Elapsed: ${elapsedTime}ms, Remaining: ${remainingTime}ms"
            )

            if (remainingTime > 0) {
                toReschedule.add(RescheduleData(id, remainingTime, cancelledData.callback))
            } else {
                toDiscard.add(id)
            }
        }

        // Reschedule timers
        var rescheduledCount = 0
        toReschedule.forEach { data ->
            scheduleTimer(data.id, data.remainingTimeInMs, data.callback)
            rescheduledCount++
            logger.verbose(
                accountId,
                "$TAG Rescheduled ${data.id} with ${data.remainingTimeInMs}ms remaining"
            )
        }

        // Discard expired timers
        var discardedCount = 0
        toDiscard.forEach { id ->
            val cancelledData = cancelledJobs.remove(id)
            discardedCount++
            cancelledData?.callback?.invoke(TimerResult.Discarded(id))
            logger.verbose(accountId, "$TAG Discarded expired timer: $id")
        }

        logger.verbose(
            accountId,
            "$TAG Foreground handling complete - Rescheduled: $rescheduledCount, Discarded: $discardedCount"
        )
    }

    private fun logCoroutineInfo(msg: String) {
        logger.verbose(accountId, "$TAG Running on: [${Thread.currentThread().name}] | $msg")
    }

    /**
     * Result sealed class for timer completion
     */
    sealed class TimerResult {
        data class Completed(val id: String, val scheduledAt: Long) : TimerResult()
        data class Error(val id: String, val exception: Exception) : TimerResult()
        data class Discarded(val id: String) : TimerResult()
    }

    private data class RescheduleData(
        val id: String,
        val remainingTimeInMs: Long,
        val callback: (TimerResult) -> Unit
    )

    private data class CancelledJobData(
        val originalDelayInMs: Long,
        val scheduledAt: Long,
        val callback: (TimerResult) -> Unit
    )
}