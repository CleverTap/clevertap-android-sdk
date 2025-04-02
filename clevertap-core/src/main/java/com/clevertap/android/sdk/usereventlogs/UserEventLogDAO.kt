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
    fun cleanUpExtraEvents(rowsThreshold: Int, numberOfRowsToCleanup: Int): Boolean
}
