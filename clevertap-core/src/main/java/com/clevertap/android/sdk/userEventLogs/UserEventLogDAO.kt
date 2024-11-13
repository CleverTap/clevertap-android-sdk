package com.clevertap.android.sdk.userEventLogs

import androidx.annotation.WorkerThread

interface UserEventLogDAO {

    // Insert a new event by deviceID
    @WorkerThread
    fun insertEventByDeviceID(deviceID: String, eventName: String): Long

    // Update an event by deviceID
    @WorkerThread
    fun updateEventByDeviceID(deviceID: String, eventName: String): Boolean

    @WorkerThread
    fun upsertEventsByDeviceID(deviceID: String, eventNameList: Set<String>): Boolean

    // Read an event by deviceID
    @WorkerThread
    fun readEventByDeviceID(deviceID: String, eventName: String): UserEventLog?

    // Read an event count by deviceID
    @WorkerThread
    fun readEventCountByDeviceID(deviceID: String, eventName: String): Int

    // Read an event firstTs by deviceID
    @WorkerThread
    fun readEventFirstTsByDeviceID(deviceID: String, eventName: String): Long

    // Read an event lastTs by deviceID
    @WorkerThread
    fun readEventLastTsByDeviceID(deviceID: String, eventName: String): Long

    // Check if an event exists by deviceID
    @WorkerThread
    fun eventExistsByDeviceID(deviceID: String, eventName: String): Boolean

    // Check if an event exists by deviceID and count
    @WorkerThread
    fun eventExistsByDeviceIDAndCount(deviceID: String, eventName: String, count: Int): Boolean

    // Get all events for a particular deviceID
    @WorkerThread
    fun allEventsByDeviceID(deviceID: String): List<UserEventLog>

    // Get all events
    @WorkerThread
    fun allEvents(): List<UserEventLog>
    @WorkerThread
    fun cleanUpExtraEvents(threshold: Int = 11_520, numberOfRowsToCleanup: Int = 2_304): Boolean
}
