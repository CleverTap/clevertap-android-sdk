package com.clevertap.android.sdk.events

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import org.json.JSONObject
import java.util.concurrent.Future
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class BaseEventQueueManager {

    @AnyThread
    abstract fun queueEvent(context: Context, event: JSONObject, eventType: Int): Future<*>?

    @AnyThread
    abstract fun queueEvent(
        context: Context,
        event: JSONObject,
        eventType: Int,
        flattenedEventData: FlattenedEventData
    ): Future<*>?

    @WorkerThread
    abstract fun addToQueue(
        context: Context,
        event: JSONObject,
        eventType: Int,
        flattenedEventData: FlattenedEventData
    )

    abstract fun flush()

    abstract fun flushQueueAsync(context: Context, eventGroup: EventGroup)

    abstract fun pushBasicProfile(
        baseProfile: JSONObject?,
        removeFromSharedPrefs: Boolean,
        profileChanges: FlattenedEventData
    )

    abstract fun pushInitialEventsAsync()

    @WorkerThread
    abstract fun flushQueueSync(context: Context, eventGroup: EventGroup)

    @WorkerThread
    abstract fun flushQueueSync(context: Context, eventGroup: EventGroup, caller: String?)

    @WorkerThread
    abstract fun flushQueueSync(
        context: Context,
        eventGroup: EventGroup,
        caller: String?,
        isUserSwitchFlush: Boolean
    )

    @WorkerThread
    abstract fun sendImmediately(context: Context, eventGroup: EventGroup, eventData: JSONObject?)

    abstract fun scheduleQueueFlush(context: Context)

    /**
     * Flushes queued events immediately when the whole app moves to background, bypassing the
     * scheduled flush delay so events reach the server before the process may be killed.
     */
    abstract fun flushOnAppBackground(context: Context)
}
