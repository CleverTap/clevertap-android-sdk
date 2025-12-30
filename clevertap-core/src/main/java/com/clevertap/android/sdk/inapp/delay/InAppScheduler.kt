package com.clevertap.android.sdk.inapp.delay

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.iterator
import com.clevertap.android.sdk.utils.filterObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Unified scheduler that combines timer management with storage strategy
 * Can be used for both delayed and in-action features
 */
internal class InAppScheduler<T>(
    private val timerManager: InAppTimerManager,
    internal val storageStrategy: InAppSchedulingStrategy,
    private val dataExtractor: InAppDataExtractor<T>,
    private val logger: Logger,
    private val accountId: String,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "[InAppScheduler]:"
    }

    /**
     * Schedule multiple in-apps with appropriate storage strategy
     * @param inApps JSONArray of in-apps to schedule
     * @param onComplete Callback when timer completes with result
     */
    @WorkerThread
    fun schedule(
        inApps: JSONArray,
        onComplete: (T) -> Unit
    ) {
        logger.verbose(accountId, "$TAG Scheduling ${inApps.length()} in-apps")

        // Step 1: Filter already scheduled in-apps
        val newInApps = inApps.filterObjects { jsonObject ->
            val id = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD)
            !timerManager.isTimerScheduled(id)
        }

        // Step 2: Prepare/store data using strategy
        val prepared = storageStrategy.prepareForScheduling(newInApps)
        if (!prepared) {
            logger.verbose(accountId, "$TAG Failed to prepare in-apps for scheduling")
            newInApps.iterator<JSONObject> {
                val id = it.optString(Constants.INAPP_ID_IN_PAYLOAD)
                onComplete(dataExtractor.createErrorResult(id, "Preparation failed"))
            }
            return
        }

        // Step 3: Schedule timers for each in-app
        newInApps.iterator<JSONObject> {
            val id = it.optString(Constants.INAPP_ID_IN_PAYLOAD)
            val delayInMs = dataExtractor.extractDelay(it)

            if (delayInMs > 0) {
                scheduleWithTimer(id, delayInMs, onComplete)
            }
        }
    }

    /**
     * Schedule a single in-app with timer
     */
    @WorkerThread
    private fun scheduleWithTimer(
        id: String,
        delayInMs: Long,
        onComplete: (T) -> Unit
    ) {
        timerManager.scheduleTimer(id, delayInMs) { timerResult ->
            when (timerResult) {
                is InAppTimerManager.TimerResult.Completed -> {
                    // Timer completed, retrieve and process
                    scope.launch { //TODO: do we require relaunch of coroutine? callback ideally should be running on provider's thread, relaunch will make operation non-atomic. from scheduling till callback execution op should be atomic
                        val data = storageStrategy.retrieveAfterTimer(id)
                        val result = if (data != null) {
                            dataExtractor.createSuccessResult(id, data)
                        } else {
                            dataExtractor.createErrorResult(id, "Data not found")
                        }

                        onComplete(result)
                        storageStrategy.cleanup(id)
                    }
                }
                is InAppTimerManager.TimerResult.Error -> {
                    scope.launch {
                        onComplete(dataExtractor.createErrorResult(id, timerResult.exception.message ?: "Unknown error"))
                        storageStrategy.cleanup(id)
                    }
                }
                is InAppTimerManager.TimerResult.Discarded -> {
                    scope.launch {
                        onComplete(dataExtractor.createDiscardedResult(id))
                        storageStrategy.cleanup(id)
                    }
                    logger.verbose(accountId, "$TAG Timer discarded, cleaned up: $id")
                }
            }
        }
    }

    /**
     * Cancel a scheduled in-app
     */
    fun cancel(id: String): Boolean {
        return timerManager.cancelTimer(id)
    }

    /**
     * Get active count
     */
    fun getActiveCount(): Int = timerManager.getActiveTimerCount()

    /**
     * Check if scheduled
     */
    fun isScheduled(id: String): Boolean = timerManager.isTimerScheduled(id)

    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
    }
}