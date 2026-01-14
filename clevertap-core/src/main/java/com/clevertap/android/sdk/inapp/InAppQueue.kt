package com.clevertap.android.sdk.inapp

import androidx.annotation.WorkerThread
import org.json.JSONObject

/**
 * The `InAppQueue` class manages the queue of ss In-App messages.
 * It provides methods to enqueue, dequeue, and retrieve the length of the queue.
 **/
internal interface InAppQueue {

    /**
     * Enqueues a single In-App message in the queue.
     *
     * @param inApp The In-App message to enqueue.
     */
    @WorkerThread
    fun enqueue(inApp: JSONObject)

    /**
     * Enqueues multiple In-App messages from a List in the queue.
     *
     * @param inApps The List containing multiple In-App messages to enqueue.
     */
    @WorkerThread
    fun enqueueAll(inApps: List<JSONObject>)

    /**
     * Insert a single In-App message in front of the queue.
     *
     * @param inApp The In-App message to insert.
     */
    @WorkerThread
    fun insertInFront(inApp: JSONObject)

    /**
     * Dequeues and removes the first In-App message from the queue.
     *
     * @return The dequeued In-App message, or null if the queue is empty.
     */
    @WorkerThread
    fun dequeue(): JSONObject?

    /**
     * Gets the current length of the In-App queue.
     *
     * @return The length of the In-App queue.
     */
    @WorkerThread
    fun getQueueLength(): Int
}
