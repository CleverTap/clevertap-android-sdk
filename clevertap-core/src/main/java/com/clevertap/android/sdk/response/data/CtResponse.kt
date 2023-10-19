package com.clevertap.android.sdk.response.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Class that wraps functionality for response and return relevant methods to get data
 */
data class CtResponse(
    val response: JSONObject
) {

    companion object {
        private const val IN_APP_DEFAULT_DAILY = 10
        private const val IN_APP_DEFAULT_SESSION = 10

        private const val IN_APP_SESSION_KEY = "imc"
        private const val IN_APP_DAILY_KEY = "imp"
    }

    fun inApps(): InApps {

        return InApps(
            inAppPerDay = inAppsPerDay(),
            inAppPerSession = inAppsPerSession(),
            inAppClientSide = clientSideInApps().second,
            inAppServerSide = serverSideInApps().second
        )
    }

    fun inAppsPerSession(): Int =
        if (response.has(IN_APP_SESSION_KEY) && response[IN_APP_SESSION_KEY] is Int) {
            response.getInt(IN_APP_SESSION_KEY)
        } else {
            IN_APP_DEFAULT_SESSION
        }

    fun inAppsPerDay(): Int = if (response.has(IN_APP_DAILY_KEY) && response[IN_APP_DAILY_KEY] is Int) {
        response.getInt(IN_APP_DAILY_KEY)
    } else {
        IN_APP_DEFAULT_DAILY
    }

    fun legacyInApps(): Pair<Boolean, JSONArray?> = inAppsPair("inapp_notifs")

    fun appLaunchInApps(): Pair<Boolean, JSONArray?> = inAppsPair("inapp_notifs_applaunched")

    fun clientSideInApps(): Pair<Boolean, JSONArray?> = inAppsPair("inapp_notifs_cs")

    fun serverSideInApps(): Pair<Boolean, JSONArray?> = inAppsPair("inapp_notifs_cs")

    private fun inAppsPair(key: String): Pair<Boolean, JSONArray?> {
        val has = response.has(key)

        if (has.not()) {
            return Pair(false, null)
        }

        val list: JSONArray = response.getJSONArray(key)

        return if (list.length() > 0) {
            Pair(true, list)
        } else {
            Pair(false, null)
        }
    }
}

data class InApps(
    val inAppPerDay: Int = 10,
    val inAppPerSession: Int = 10,
    val inAppClientSide: JSONArray?,
    val inAppServerSide: JSONArray?
    //val inAppClientSide: List<InAppClientSide>,
    //val inAppServerSide: List<InAppServerSide>,
)

data class InAppClientSide(
    val ti: String,
    val title: String,
    val message: String,
    val type: String,
    val suppressed: Boolean,
    val wzrk_id: Int,
    val wzrk_pivot: String,
    val wzrk_cgId: Int,
    val whenTriggers: String,
    val whenLimits: String,
    val excludeGlobalFCaps: Boolean,
    val priority: Int,
)

data class InAppServerSide(
    val ti: String,
    val whenTriggers: String,
    val whenLimits: String,
    val excludeGlobalFCaps: String,
)





