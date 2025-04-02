package com.clevertap.android.sdk.inapp

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.utils.prepend
import org.json.JSONArray
import org.json.JSONObject

/**
 * The `InAppQueue` class manages the queue of ss In-App messages.
 * It provides methods to enqueue, dequeue, and retrieve the length of the queue.
 *
 * <p>
 * The queue is stored in the [InAppStore] under the server-side mode, ensuring persistent storage.
 * </p>
 *
 * @property config The configuration for the CleverTap SDK instance.
 * @property storeRegistry The registry providing access to different stores, including [InAppStore].
 *
 * @constructor Creates an InAppQueue with the given configuration and store registry.
 */
internal class InAppQueue(
    private val config: CleverTapInstanceConfig,
    private val storeRegistry: StoreRegistry
) {

    /**
     * Enqueues a single In-App message in the queue.
     *
     * @param jsonObject The In-App message to enqueue.
     */
    @WorkerThread
    @Synchronized
    fun enqueue(jsonObject: JSONObject) {
        val currentQueue = getQueue()
        currentQueue.put(jsonObject)
        saveQueue(currentQueue)
    }

    /**
     * Enqueues multiple In-App messages from a JSONArray in the queue.
     *
     * @param jsonArray The JSONArray containing multiple In-App messages to enqueue.
     */
    @WorkerThread
    @Synchronized
    fun enqueueAll(jsonArray: JSONArray) {
        val currentQueue = getQueue()
        for (i in 0 until jsonArray.length()) {
            try {
                currentQueue.put(jsonArray.getJSONObject(i))
            } catch (e: Exception) {
                Logger.d(
                    config.accountId,
                    "InAppController: Malformed InApp notification: " + e.message
                )
            }
        }
        saveQueue(currentQueue)
    }

    /**
     * Insert a single In-App message in front of the queue.
     *
     * @param jsonObject The In-App message to insert.
     */
    @WorkerThread
    @Synchronized
    fun insertInFront(jsonObject: JSONObject) {
        val currentQueue = getQueue()
        currentQueue.prepend(jsonObject)
        saveQueue(currentQueue)
    }

    /**
     * Dequeues and removes the first In-App message from the queue.
     *
     * @return The dequeued In-App message, or null if the queue is empty.
     */
    @WorkerThread
    @Synchronized
    fun dequeue(): JSONObject? {
        val currentQueue = getQueue()
        if (currentQueue.length() == 0) {
            return null
        }
        val removedObject = currentQueue.remove(0)
        saveQueue(currentQueue)
        return removedObject as? JSONObject
    }

    /**
     * Gets the current length of the In-App queue.
     *
     * @return The length of the In-App queue.
     */
    @WorkerThread
    @Synchronized
    fun getQueueLength(): Int {
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
