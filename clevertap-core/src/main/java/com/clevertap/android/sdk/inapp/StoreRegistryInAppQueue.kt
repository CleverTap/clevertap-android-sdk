package com.clevertap.android.sdk.inapp

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.utils.prepend
import org.json.JSONArray
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
    override fun enqueue(jsonObject: JSONObject) {
        val currentQueue = getQueue()
        currentQueue.put(jsonObject)
        saveQueue(currentQueue)
    }

    @WorkerThread
    @Synchronized
    override fun enqueueAll(jsonArray: JSONArray) {
        val currentQueue = getQueue()
        for (i in 0 until jsonArray.length()) {
            try {
                currentQueue.put(jsonArray.getJSONObject(i))
            } catch (e: Exception) {
                Logger.d(
                    logTag,
                    "InAppController: Malformed InApp notification: " + e.message
                )
            }
        }
        saveQueue(currentQueue)
    }

    @WorkerThread
    @Synchronized
    override fun insertInFront(jsonObject: JSONObject) {
        val currentQueue = getQueue()
        currentQueue.prepend(jsonObject)
        saveQueue(currentQueue)
    }

    @WorkerThread
    @Synchronized
    override fun dequeue(): JSONObject? {
        val currentQueue = getQueue()
        if (currentQueue.length() == 0) {
            return null
        }
        val removedObject = currentQueue.remove(0)
        saveQueue(currentQueue)
        return removedObject as? JSONObject
    }

    @WorkerThread
    @Synchronized
    override fun getQueueLength(): Int {
        val currentQueue = getQueue()
        return currentQueue.length()
    }

    /**
     * Retrieves the server-side In-App queue from the [InAppStore].
     *
     * @return The current server-side In-App queue.
     */
    private fun getQueue(): JSONArray {
        val inAppStore = storeRegistry.inAppStore ?: return JSONArray()
        return inAppStore.readServerSideInApps()
    }

    /**
     * Saves the modified server-side In-App queue back to the [InAppStore].
     *
     * @param queue The updated server-side In-App queue.
     */
    private fun saveQueue(queue: JSONArray) = storeRegistry.inAppStore?.storeServerSideInApps(queue)
}
