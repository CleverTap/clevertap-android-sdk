package com.clevertap.android.sdk.usereventlogs

/**
 * Data class representing an event log for user actions in the CleverTap SDK.
 *
 * This class stores information about a specific event including its name, occurrence timestamps,
 * frequency count, and the associated GUID/user identifier. It tracks both the original event name
 * and its normalized version (lowercase, no spaces) for consistent processing.
 *
 * @property eventName The original name of the event as provided by the user
 * @property normalizedEventName The processed version of the event name with spaces removed and converted to lowercase
 * @property firstTs Timestamp (in milliseconds) of when this event was first recorded
 * @property lastTs Timestamp (in milliseconds) of when this event was most recently recorded
 * @property countOfEvents The total number of times this event has occurred
 * @property deviceID  GUID/deviceID of the user where these events occurred
 *
 * @see com.clevertap.android.sdk.Utils.getNormalizedName
 */
data class UserEventLog(
    val eventName: String,   // The name of the event
    val normalizedEventName: String, // normalized version of the name of the event
    val firstTs: Long,       // The timestamp of the first occurrence of the event
    val lastTs: Long,        // The timestamp of the last occurrence of the event
    val countOfEvents: Int,  // The number of times the event has occurred
    val deviceID: String     // The GUID/deviceID of the user where the event occurred
)
