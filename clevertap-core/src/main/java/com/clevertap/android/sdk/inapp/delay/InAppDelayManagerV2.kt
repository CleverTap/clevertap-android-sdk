package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MAX_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MIN_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import com.clevertap.android.sdk.iterator
import com.clevertap.android.sdk.utils.filterObjects
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

internal class InAppDelayManagerV2(
    private val accountId: String,
    private val logger: Logger,
    internal var delayedLegacyInAppStore: DelayedLegacyInAppStore? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val activeJobs = ConcurrentHashMap<String, Job>()

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
        callbackDispatcher: CoroutineDispatcher,
        callback: (DelayedInAppResult) -> Unit
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

                // Check if store is initialized
                val store = delayedLegacyInAppStore
                if (store == null) {
                    logger.verbose(accountId, "DelayedLegacyInAppStore is null for callback id: $id")
                    callback(DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.STORE_NOT_INITIALIZED, id))
                    return@launch
                }

                // Fetch from database
                val delayedInAppJSONObj = store.getDelayedInApp(id)

                // Create appropriate result based on retrieval
                val result = if (delayedInAppJSONObj != null) {
                    store.removeDelayedInApp(id)
                    DelayedInAppResult.Success(delayedInAppJSONObj, id)
                } else {
                    DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.NOT_FOUND_IN_DB, id)
                }

                // Invoke callback with result
                callback(result)

            } catch (e: CancellationException) {
                logger.verbose(accountId, "Cancelled InApp callback with id: $id")
                callback(DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.CANCELLED, id, e))
            } catch (e: Exception) {
                logger.verbose(accountId, "Error in InApp callback with id: $id", e)
                callback(DelayedInAppResult.Error(DelayedInAppResult.Error.ErrorReason.UNKNOWN, id, e))
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
        callback: (DelayedInAppResult) -> Unit
    ) {
        logger.verbose(
            accountId,
            "InAppDelayManager: Scheduling ${delayedInApps.length()} delayed in-apps"
        )

        if (delayedLegacyInAppStore == null) {
            logger.verbose(
                accountId,
                "InAppDelayManager: DelayedLegacyInAppStore is null, aborting scheduling"
            )
            return
        }

        val newDelayedInAppsToSchedule = delayedInApps.filterObjects { jsonObject ->
            val inAppId = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD)
            inAppId.isNotBlank() && activeJobs[inAppId] == null
        }

        delayedLegacyInAppStore!!.saveDelayedInAppsBatch(newDelayedInAppsToSchedule)

        newDelayedInAppsToSchedule.iterator<JSONObject> {
            val inAppId = it.optString(Constants.INAPP_ID_IN_PAYLOAD)
            val delayInMilliSeconds = getInAppDelayInMs(it)

            try {
                if (delayInMilliSeconds > 0) {
                    scheduleInAppCallbackWithDispatcher(
                        inAppId,
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