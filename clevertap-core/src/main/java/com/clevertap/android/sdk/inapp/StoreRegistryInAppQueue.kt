package com.clevertap.android.sdk.inapp

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import org.json.JSONObject

/**
 * <p>
 * The queue is stored in the [InAppStore] under the server-side mode, ensuring persistent storage.
 * </p>
 *
 * @property storeRegistry The registry providing access to different stores, including [InAppStore].
 * @property logTag The tag to use when logging.
 *
 * @constructor Creates an InAppQueue with the given store registry.
 */
internal class StoreRegistryInAppQueue(
    private val storeRegistry: StoreRegistry,
    private val logTag: String
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
        saveQueue(currentQueue)
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
     *
     * @return The current server-side In-App queue.
     */
    private fun getQueue(): List<JSONObject> {
        val inAppStore = storeRegistry.inAppStore ?: return emptyList()
        return inAppStore.readServerSideInApps()
    }

    /**
     * Saves the modified server-side In-App queue back to the [InAppStore].
     *
     * @param queue The updated server-side In-App queue.
     */
    private fun saveQueue(queue: List<JSONObject>) = storeRegistry.inAppStore?.storeServerSideInApps(queue)
}
