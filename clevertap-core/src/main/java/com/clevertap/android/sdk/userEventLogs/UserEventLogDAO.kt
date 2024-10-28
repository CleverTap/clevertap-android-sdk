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
    fun upSertEventsByDeviceID(deviceID: String, eventNameList: Set<String>): Boolean

    // Read an event by deviceID
    @WorkerThread
    fun readEventByDeviceID(deviceID: String, eventName: String): UserEventLog?

    // Check if an event exists by deviceID
    @WorkerThread
    fun eventExistsByDeviceID(deviceID: String, eventName: String): Boolean

    // Get all events for a particular deviceID
    @WorkerThread
    fun allEventsByDeviceID(deviceID: String): List<UserEventLog>

    // Get all events
    @WorkerThread
    fun allEvents(): List<UserEventLog>
    @WorkerThread
    fun cleanUpExtraEvents(threshold: Int): Boolean
}
