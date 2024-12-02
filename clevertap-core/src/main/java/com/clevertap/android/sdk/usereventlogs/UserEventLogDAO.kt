package com.clevertap.android.sdk.usereventlogs

import androidx.annotation.WorkerThread

interface UserEventLogDAO {

    // Insert a new event by deviceID
    @WorkerThread
    fun insertEvent(deviceID: String, eventName: String, normalizedEventName: String): Long

    // Update an event by deviceID
    @WorkerThread
    fun updateEventByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Boolean

    @WorkerThread
    fun upsertEventsByDeviceIdAndNormalizedEventName(
        deviceID: String,
        setOfActualAndNormalizedEventNamePair: Set<Pair<String, String>>
    ): Boolean

    // Read an event by deviceID
    @WorkerThread
    fun readEventByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): UserEventLog?

    // Read an event count by deviceID
    @WorkerThread
    fun readEventCountByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Int

    // Read an event firstTs by deviceID
    @WorkerThread
    fun readEventFirstTsByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Long

    // Read an event lastTs by deviceID
    @WorkerThread
    fun readEventLastTsByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Long

    // Check if an event exists by deviceID
    @WorkerThread
    fun eventExistsByDeviceIdAndNormalizedEventName(deviceID: String, normalizedEventName: String): Boolean

    // Check if an event exists by deviceID and count
    @WorkerThread
    fun eventExistsByDeviceIdAndNormalizedEventNameAndCount(deviceID: String, normalizedEventName: String, count: Int): Boolean

    // Get all events for a particular deviceID
    @WorkerThread
    fun allEventsByDeviceID(deviceID: String): List<UserEventLog>

    // Get all events
    @WorkerThread
    fun allEvents(): List<UserEventLog>
    @WorkerThread
    fun cleanUpExtraEvents(threshold: Int = 11_520, numberOfRowsToCleanup: Int = 2_304): Boolean
}
