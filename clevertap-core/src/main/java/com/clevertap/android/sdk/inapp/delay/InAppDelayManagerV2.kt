package com.clevertap.android.sdk.inapp.delay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MAX_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MIN_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import com.clevertap.android.sdk.iterator
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.utils.filterObjects
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppDelayManagerV2(
    private val accountId: String,
    private val logger: Logger,
    internal var delayedLegacyInAppStore: DelayedLegacyInAppStore? = null,
    private val clock: Clock = Clock.SYSTEM,
    private val scope: CoroutineScope = ProcessLifecycleOwner.get().lifecycleScope + Dispatchers.Default.limitedParallelism(
        PARALLEL_SCHEDULERS
    ),
    private val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) {
    companion object {
        private const val PARALLEL_SCHEDULERS = 20 // worst case - we assume 1 in-app per minute, session length 20 mins
        private const val TAG = "[InAppDelayManager]:"
    }
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val cancelledJobs =
        ConcurrentHashMap<String, CancelledJobData>()

    init {
        //System.setProperty("kotlinx.coroutines.debug","on")
        scope.launch {
            logCoroutineInfo("lifeCycleOwner scope launch, $coroutineContext, ${coroutineContext[Job]?.parent}}")
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                logCoroutineInfo("process lifeCycleOwner: started, $coroutineContext, ${coroutineContext[Job]?.parent}}")
                try {
                    onAppForeground()
                    awaitCancellation()
                } catch (c: CancellationException) {
                    logCoroutineInfo("process lifeCycleOwner: Stopped, $coroutineContext, ${coroutineContext[Job]?.parent}}")
                    withContext(NonCancellable){
                        logCoroutineInfo("process lifeCycleOwner: withContext block, $coroutineContext, ${coroutineContext[Job]?.parent}}")
                        onAppBackground() // cleanup
                    }
                    ensureActive()// rethrow cancellation
                }
            }
        }
    }

    /**
     * Schedules an in-app callback to be executed after a specified delay.
     * If a callback with the same ID is already scheduled and active,
     * the existing one is kept and returned.
     *
     * @param id Unique identifier for the in-app message
     * @param delayInMs Delay in milliseconds before callback execution
     * @param callbackDispatcher Dispatcher to execute the callback on (default: Main)
     * @param callback Lambda to be invoked with DelayedInAppResult after delay
     * @return Job handle - either existing or newly created
     */
    private fun scheduleInAppCallbackWithDispatcher(
        id: String,
        delayInMs: Long,
        callback: (DelayedInAppResult) -> Unit
    ): Job {
        // Keep existing active job if present
        activeJobs[id]?.let { existingJob ->
            if (existingJob.isActive) {
                logger.verbose(accountId,"$TAG InApp callback with id '$id' already scheduled, keeping existing")
                return existingJob
            } else {
                activeJobs.remove(id)
            }
        }

        val job = scope.launch {
            val scheduledAt = clock.currentTimeMillis()
            try {
                delay(delayInMs)

                // Check if store is initialized
                val store = delayedLegacyInAppStore
                if (store == null) {
                    logger.verbose(accountId, "$TAG DelayedLegacyInAppStore is null for callback id: $id")
                    callback(DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.STORE_NOT_INITIALIZED, id))
                    return@launch
                }

                // Fetch from database
                val delayedInAppJSONObj = store.getDelayedInApp(id)

                // Create appropriate result based on retrieval
                val result = if (delayedInAppJSONObj != null) {
                    DelayedInAppResult.Success(delayedInAppJSONObj, id)
                } else {
                    DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.NOT_FOUND_IN_DB, id)
                }

                // Invoke callback with result
                callback(result)
                cancelledJobs.remove(id)
                store.removeDelayedInApp(id)
            } catch (e: CancellationException) {
                logger.verbose(accountId, "$TAG Cancelled InApp callback with id: $id")
                cancelledJobs.putIfAbsent(id, CancelledJobData(delayInMs, scheduledAt, callback))
                ensureActive()// rethrow cancellation
                //callback(DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.CANCELLED, id, e))
            } catch (e: Exception) {
                logger.verbose(accountId, "$TAG Error in InApp callback with id: $id", e)
                callback(DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.UNKNOWN, id, e))
                cancelledJobs.remove(id)
                delayedLegacyInAppStore!!.removeDelayedInApp(id)
            } finally {
                activeJobs.remove(id)
            }
        }

        activeJobs[id] = job
        logger.verbose(accountId,"$TAG Scheduled new InApp callback with id '$id' for ${delayInMs}ms delay")

        return job
    }

    /**
     * Schedule multiple delayed in-apps
     */
    internal fun scheduleDelayedInApps(
        delayedInApps: JSONArray,
        callback: (DelayedInAppResult) -> Unit
    ) {
        logger.verbose(
            accountId,
            "$TAG Scheduling ${delayedInApps.length()} delayed in-apps"
        )

        if (delayedLegacyInAppStore == null) {
            logger.verbose(
                accountId,
                "$TAG DelayedLegacyInAppStore is null, aborting scheduling"
            )
            return
        }

        val newDelayedInAppsToSchedule = delayedInApps.filterObjects { jsonObject ->
            val inAppId = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD)
            inAppId.isNotBlank() && activeJobs[inAppId] == null
        }

        val saveSuccess = delayedLegacyInAppStore!!.saveDelayedInAppsBatch(newDelayedInAppsToSchedule)

        if (!saveSuccess) {
            newDelayedInAppsToSchedule.iterator<JSONObject> {
                val inAppId = it.optString(Constants.INAPP_ID_IN_PAYLOAD)
                callback(DelayedInAppResult.Error(
                    DelayedInAppResult.Error.ErrorReason.DB_SAVE_FAILED,
                    inAppId
                ))
            }
            return
        }

        newDelayedInAppsToSchedule.iterator<JSONObject> {
            val inAppId = it.optString(Constants.INAPP_ID_IN_PAYLOAD)
            val delayInMilliSeconds = getInAppDelayInMs(it)

            if (delayInMilliSeconds > 0) {
                scheduleInAppCallbackWithDispatcher(
                    inAppId,
                    delayInMilliSeconds,
                    callback
                )
            }
        }
    }

    /**
     * Called when app goes to background
     * Cancels all active jobs (will be rescheduled on foreground)
     */
    suspend fun onAppBackground() {
        cancelAllCallbacks()
    }

    /**
     * Called when app comes to foreground
     * Reschedules in-apps based on elapsed time
     */
    fun onAppForeground() {
        logger.verbose(
            accountId,
            "$TAG App coming to foreground, checking for pending in-apps"
        )

        if (delayedLegacyInAppStore == null) {
            logger.verbose(
                accountId,
                "$TAG DelayedLegacyInAppStore is null, aborting foreground handling"
            )
            return
        }

        if (cancelledJobs.isEmpty()) {
            logger.verbose(accountId, "$TAG No pending delayed in-apps found")
            return
        }

        logger.verbose(
            accountId,
            "$TAG Found ${cancelledJobs.size} pending delayed in-apps"
        )

        val currentTime = clock.currentTimeMillis()

        val toReschedule = mutableListOf<RescheduleData>()
        val toDiscard = mutableListOf<String>()

        cancelledJobs.forEach { (inAppId, jobData) ->
            val originalDelayInMs = jobData.originalDelayInMs
            val scheduledAt = jobData.scheduledAt
            val callback = jobData.callback

            // Calculate elapsed time since scheduling started
            val elapsedTimeMs = currentTime - scheduledAt

            // Calculate remaining time
            val remainingTimeInMs = originalDelayInMs - elapsedTimeMs

            logger.verbose(
                accountId,
                "$TAG InApp $inAppId - Original delay: ${originalDelayInMs}ms, " +
                        "Elapsed: ${elapsedTimeMs}ms, Remaining: ${remainingTimeInMs}ms"
            )

            if (remainingTimeInMs > 0) {
                // Mark for rescheduling
                toReschedule.add(
                    RescheduleData(
                        inAppId = inAppId,
                        remainingTimeInMs = remainingTimeInMs,
                        callback = callback
                    )
                )
            } else {
                // Mark for discard
                toDiscard.add(inAppId)
            }
        }

        var rescheduledCount = 0
        toReschedule.forEach { data ->
            scheduleInAppCallbackWithDispatcher(
                data.inAppId,
                data.remainingTimeInMs,
                data.callback
            )
            rescheduledCount++

            logger.verbose(
                accountId,
                "$TAG Rescheduled ${data.inAppId} with ${data.remainingTimeInMs}ms remaining"
            )
        }

        var discardedCount = 0
        toDiscard.forEach { inAppId ->
            delayedLegacyInAppStore!!.removeDelayedInApp(inAppId)
            cancelledJobs.remove(inAppId)
            discardedCount++

            logger.verbose(
                accountId,
                "$TAG Discarded expired in-app $inAppId"
            )
        }

        logger.verbose(
            accountId,
            "$TAG Foreground handling complete - Rescheduled: $rescheduledCount, Discarded: $discardedCount"
        )
    }

    /**
     * Cancels a scheduled callback by ID
     */
    internal fun cancelCallback(id: String): Boolean {
        return activeJobs[id]?.let { job ->
            job.cancel()
            activeJobs.remove(id)
            logger.verbose(accountId,"Cancelled InApp callback with id: $id")
            true
        } ?: false
    }

    /**
     * Cancels all scheduled callbacks
     */
    private suspend fun cancelAllCallbacks() {
        val cancelledCount = activeJobs.size
        val jobsToCancel = activeJobs.values.toList()
        jobsToCancel.forEach {
            it.cancelAndJoin()
        }
        logger.verbose(accountId, "$TAG Cancelled $cancelledCount InApp callbacks")
    }

    /**
     * Get count of active scheduled callbacks
     */
    internal fun getActiveCallbackCount(): Int = activeJobs.size
    internal fun getCancelledJobsCount(): Int = cancelledJobs.size

    /**
     * Check if a callback with specific ID is scheduled and active
     */
    internal fun isCallbackScheduled(id: String): Boolean {
        return activeJobs[id]?.isActive ?: false
    }

    /**
     * Get all active callback IDs
     */
    internal fun getActiveCallbackIds(): Set<String> {
        return activeJobs.filterValues { it.isActive }.keys.toSet()
    }

    /**
     * Clean up resources
     */
    internal suspend fun cleanup() {
        cancelAllCallbacks()
        scope.cancel()
    }

    /**
     * Extract delay from in-app JSON
     */
    private fun getInAppDelayInMs(inApp: JSONObject): Long {
        val delaySeconds = inApp.optInt(INAPP_DELAY_AFTER_TRIGGER, 0)
        return if (delaySeconds in INAPP_MIN_DELAY_SECONDS..INAPP_MAX_DELAY_SECONDS) delaySeconds.seconds.inWholeMilliseconds else 0
    }

    private fun logCoroutineInfo(msg: String) {
        logger.verbose(
            accountId,
            "$TAG Running on: [${Thread.currentThread().name}] | $msg"
        )
    }

    private data class RescheduleData(
        val inAppId: String,
        val remainingTimeInMs: Long,
        val callback: (DelayedInAppResult) -> Unit
    )
    private data class CancelledJobData(
        val originalDelayInMs: Long,
        val scheduledAt: Long,
        val callback: (DelayedInAppResult) -> Unit
    )
}