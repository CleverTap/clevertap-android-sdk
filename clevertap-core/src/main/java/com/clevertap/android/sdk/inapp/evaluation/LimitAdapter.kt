package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import org.json.JSONObject

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

    companion object {
        fun fromString(type: String): LimitType {
            return values().find { it.type == type } ?: Ever
        }
    }
}


class LimitAdapter(limitJSON: JSONObject) {

    val limitType: LimitType = LimitType.fromString(limitJSON.optString(Constants.KEY_TYPE))

    val limit: Int = limitJSON.optInt(Constants.KEY_LIMIT)

    val frequency: Int = limitJSON.optInt(Constants.KEY_FREQUENCY)

    fun toJsonObject(): JSONObject {
        val limitJson = JSONObject()

        limitJson.put(Constants.KEY_TYPE, limitType.toString())
        limitJson.put(Constants.KEY_LIMIT, limit)
        limitJson.put(Constants.KEY_FREQUENCY, frequency)

        return limitJson
    }
}

