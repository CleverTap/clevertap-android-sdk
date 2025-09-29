package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MAX_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MIN_DELAY_SECONDS
import com.clevertap.android.sdk.iterator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class InAppDelayManagerV2(
    private val accountId: String,
    private val logger: Logger,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val activeJobs = ConcurrentHashMap<String, Job>()

    /**
     * Schedules an in-app callback to be executed after a specified delay.
     * If a callback with the same ID is already scheduled and active,
     * the existing one is kept and returned.
     *
     * @param id Unique identifier for the in-app message
     * @param inAppObject JSONObject containing in-app data
     * @param delayInMs Delay in milliseconds before callback execution
     * @param callbackDispatcher Dispatcher to execute the callback on (default: Main)
     * @param callback Lambda to be invoked with the in-app object after delay
     * @return Job handle - either existing or newly created
     */
    private fun scheduleInAppCallbackWithDispatcher(
        id: String,
        inAppObject: JSONObject,
        delayInMs: Long,
        callbackDispatcher: CoroutineDispatcher,
        callback: (JSONObject) -> Unit
    ): Job {
        // Keep existing active job if present
        activeJobs[id]?.let { existingJob ->
            if (existingJob.isActive) {
                logger.verbose(accountId,"InApp callback with id '$id' already scheduled, keeping existing")
                return existingJob
            } else {
                activeJobs.remove(id)
            }
        }

        val job = scope.launch {
            try {
                delay(delayInMs)

                //withContext(callbackDispatcher) {
                callback(inAppObject)
                //}
            } catch (e: CancellationException) {
                logger.verbose(accountId, "Cancelled InApp callback with id: $id")
            } finally {
                activeJobs.remove(id)
            }
        }

        activeJobs[id] = job
        logger.verbose(accountId,"Scheduled new InApp callback with id '$id' for ${delayInMs}ms delay")

        return job
    }

    /**
     * Schedule multiple delayed in-apps
     */
    internal fun scheduleDelayedInApps(
        delayedInApps: JSONArray,
        callbackDispatcher: CoroutineDispatcher = Dispatchers.Main,
        callback: (JSONObject) -> Unit
    ) {
        logger.verbose(
            accountId,
            "InAppDelayManager: Scheduling ${delayedInApps.length()} delayed in-apps"
        )
        delayedInApps.iterator<JSONObject> {
            val inAppId = it.optString(Constants.INAPP_ID_IN_PAYLOAD)
            val delayInMilliSeconds = getInAppDelayInMs(it)
            try {
                if (delayInMilliSeconds > 0) {
                    scheduleInAppCallbackWithDispatcher(
                        inAppId,
                        it,
                        delayInMilliSeconds,
                        callbackDispatcher,
                        callback
                    )
                }
            } catch (e: Exception) {
                logger.verbose(
                    accountId,
                    "InAppDelayManager: Error scheduling delayed in-app $inAppId",
                    e
                )
            }
        }
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
    private fun cancelAllCallbacks() {
        val cancelledCount = activeJobs.size
        val jobsToCancel = activeJobs.values.toList()
        activeJobs.clear()
        jobsToCancel.forEach { it.cancel() }
        logger.verbose(accountId, "Cancelled $cancelledCount InApp callbacks")
    }

    /**
     * Get count of active scheduled callbacks
     */
    internal fun getActiveCallbackCount(): Int = activeJobs.size

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
    internal fun cleanup() {
        cancelAllCallbacks()
        scope.cancel()
    }

    /**
     * Extract delay from in-app JSON
     */
    internal fun getInAppDelayInMs(inApp: JSONObject): Long {
        val delaySeconds = inApp.optInt(INAPP_DELAY_AFTER_TRIGGER, 0)
        return if (delaySeconds in INAPP_MIN_DELAY_SECONDS..INAPP_MAX_DELAY_SECONDS) delaySeconds.seconds.inWholeMilliseconds else 0
    }
}