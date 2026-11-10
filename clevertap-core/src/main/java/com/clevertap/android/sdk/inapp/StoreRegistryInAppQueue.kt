package com.clevertap.android.sdk.inapp

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.validation.ValidationResult
import org.json.JSONObject

/**
 * <p>
 * The queue is stored in the [InAppStore] under the server-side mode, ensuring persistent storage.
 * </p>
 *
 * <p>
 * The whole queue is decrypted on read and encrypted as one string on every save, so a very large
 * queue can fail with [OutOfMemoryError]. When that happens the queue is dropped instead of crashing
 * the app, since every later read or save would fail the same way, and the drop is reported through
 * [onQueueDropped].
 * </p>
 *
 * @property storeRegistry The registry providing access to different stores, including [InAppStore].
 * @property logTag The tag to use when logging.
 * @property onQueueDropped Receives a [ValidationResult] describing a dropped queue, reported as `wzrk_error`.
 *
 * @constructor Creates an InAppQueue with the given store registry.
 */
internal class StoreRegistryInAppQueue(
    private val storeRegistry: StoreRegistry,
    private val logTag: String,
    private val onQueueDropped: (ValidationResult) -> Unit = {}
) : InAppQueue {

    @WorkerThread
    @Synchronized
    override fun enqueue(inApp: JSONObject) {
        val currentQueue = getQueue().toMutableList()
        currentQueue.add(inApp)
        saveQueue(currentQueue)
    }

    @WorkerThread
    @Synchronized
    override fun enqueueAll(inApps: List<JSONObject>) {
        if (inApps.isEmpty()) return

        val currentQueue = getQueue().toMutableList()
        currentQueue.addAll(inApps)
        saveQueue(currentQueue)
    }

    @WorkerThread
    @Synchronized
    override fun insertInFront(inApp: JSONObject) {
        val currentQueue = getQueue().toMutableList()
        currentQueue.add(0, inApp)
        saveQueue(currentQueue)
    }

    @WorkerThread
    @Synchronized
    override fun dequeue(): JSONObject? {
        val currentQueue = getQueue().toMutableList()
        if (currentQueue.isEmpty()) {
            return null
        }
        val removedObject = currentQueue.removeAt(0)
        if (!saveQueue(currentQueue)) {
            // The queue was dropped; don't show an in-app under memory pressure
            return null
        }
        return removedObject
    }

    @WorkerThread
    @Synchronized
    override fun getQueueLength(): Int {
        val currentQueue = getQueue()
        return currentQueue.size
    }

    /**
     * Retrieves the server-side In-App queue from the [InAppStore].
     * If the stored queue is too large to read, it is dropped and an empty queue is returned.
     *
     * @return The current server-side In-App queue.
     */
    private fun getQueue(): List<JSONObject> {
        val inAppStore = storeRegistry.inAppStore ?: return emptyList()
        return try {
            inAppStore.readServerSideInApps()
        } catch (oom: OutOfMemoryError) {
            Logger.v(logTag, "In-app queue too large to read, dropping it", oom)
            val storedLength = inAppStore.serverSideInAppsStoredLength()
            inAppStore.removeServerSideInApps()
            reportDroppedQueue(Constants.INAPP_QUEUE_READ_OOM_ERROR_CODE) {
                "${Constants.INAPP_QUEUE_READ_OOM_ERROR_MSG}, len=$storedLength"
            }
            emptyList()
        }
    }

    /**
     * Saves the modified server-side In-App queue back to the [InAppStore].
     * If the queue is too large to save, it is dropped.
     *
     * @param queue The updated server-side In-App queue.
     * @return false if the queue was dropped, true otherwise.
     */
    private fun saveQueue(queue: List<JSONObject>): Boolean {
        val inAppStore = storeRegistry.inAppStore ?: return true
        return try {
            inAppStore.storeServerSideInApps(queue)
            true
        } catch (oom: OutOfMemoryError) {
            Logger.v(logTag, "In-app queue too large to save, dropping it", oom)
            val storedLength = inAppStore.serverSideInAppsStoredLength()
            inAppStore.removeServerSideInApps()
            reportDroppedQueue(Constants.INAPP_QUEUE_SAVE_OOM_ERROR_CODE) {
                "${Constants.INAPP_QUEUE_SAVE_OOM_ERROR_MSG}, items=${queue.size}, prevLen=$storedLength"
            }
            false
        }
    }

    /**
     * Reports a dropped queue. Runs under memory pressure, so any failure here is swallowed
     * rather than crashing the app.
     */
    private inline fun reportDroppedQueue(errorCode: Int, errorDesc: () -> String) {
        try {
            onQueueDropped(ValidationResult(errorCode, errorDesc()))
        } catch (t: Throwable) {
            // Reporting is best effort
        }
    }
}
