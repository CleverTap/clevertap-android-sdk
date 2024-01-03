package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import org.json.JSONObject

/**
 * Enumeration representing different types of limits for in-app notifications.
 *
 * Each enum value corresponds to a specific type of limit that can be applied, such as "ever," "session,"
 * "seconds," "minutes," "hours," "days," "weeks," "onEvery," and "onExactly."
 *
 * @property type The string representation of the limit type.
 */
enum class LimitType(val type: String) {

    Ever("ever"),
    Session("session"),
    Seconds("seconds"),
    Minutes("minutes"),
    Hours("hours"),
    Days("days"),
    Weeks("weeks"),
    OnEvery("onEvery"),
    OnExactly("onExactly");

    /**
     * Companion object providing a method to convert a string representation into the corresponding [LimitType].
     *
     * @param type The string representation of the limit type.
     * @return The corresponding [LimitType] or [Ever] if no match is found.
     */
    companion object {

        fun fromString(type: String): LimitType {
            return values().find { it.type == type } ?: Ever
        }
    }
}

/**
 * Adapter class for converting a JSON object representing a limit into a [LimitAdapter] instance.
 *
 * The class extracts information about the limit type, limit value, and frequency from the provided JSON object.
 * It also provides a method to convert the [LimitAdapter] instance back into a JSON object.
 *
 * @param limitJSON The JSON object representing the limit information.
 */
class LimitAdapter(limitJSON: JSONObject) {

    /**
     * The type of the limit, determined by the [LimitType] enum.
     */
    val limitType: LimitType = LimitType.fromString(limitJSON.optString(Constants.KEY_TYPE))

    /**
     * The numeric value representing the limit.
     */
    val limit: Int = limitJSON.optInt(Constants.KEY_LIMIT)

    /**
     * The frequency associated with the limit (e.g., up to 4 times in 8 hours).
     * Here 4 is limit, 8 is frequency and hours is limit type.
     */
    val frequency: Int = limitJSON.optInt(Constants.KEY_FREQUENCY)

    /**
     * Converts the [LimitAdapter] instance into a JSON object.
     *
     * @return The JSON object representing the limit information.
     */
    fun toJsonObject(): JSONObject {
        val limitJson = JSONObject()

        limitJson.put(Constants.KEY_TYPE, limitType.toString())
        limitJson.put(Constants.KEY_LIMIT, limit)
        limitJson.put(Constants.KEY_FREQUENCY, frequency)

        return limitJson
    }
}

