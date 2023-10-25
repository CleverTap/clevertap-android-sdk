package com.clevertap.android.sdk.response.data

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.safeGetJSONArray
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

        private const val IN_APP_SESSION_KEY = Constants.INAPP_MAX_PER_SESSION_KEY
        private const val IN_APP_DAILY_KEY = Constants.INAPP_MAX_PER_DAY_KEY
    }

    fun inApps(): InApps {

        return InApps(
            inAppPerDay = inAppsPerDay(),
            inAppPerSession = inAppsPerSession(),
            inAppClientSide = clientSideInApps().second,
            inAppServerSide = serverSideInApps().second
        )
    }

    fun inAppsPerSession(): Int = response.optInt(IN_APP_SESSION_KEY, IN_APP_DEFAULT_SESSION)

    fun inAppsPerDay(): Int = response.optInt(IN_APP_DAILY_KEY, IN_APP_DEFAULT_DAILY)

    fun legacyInApps(): Pair<Boolean, JSONArray?> = response.safeGetJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)

    fun appLaunchInApps(): Pair<Boolean, JSONArray?> = response.safeGetJSONArray(Constants.INAPP_NOTIFS_APP_LAUNCHED_KEY)

    fun clientSideInApps(): Pair<Boolean, JSONArray?> = response.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_CS)

    fun serverSideInApps(): Pair<Boolean, JSONArray?> = response.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_SS)

//    fun clientSideInApps(): List<InAppClientSide> {
//        val jsonArray = response.safeGetJSONArray("inapp_notifs_cs").second ?: return emptyList()
//        return jsonArray.toList().mapNotNull { InAppClientSide.fromJSONObject(it) }
//    }
//
//    fun serverSideInApps(): List<InAppServerSide> {
//        val jsonArray = response.safeGetJSONArray("inapp_notifs_cs").second ?: return emptyList()
//        return jsonArray.toList().mapNotNull { InAppServerSide.fromJSONObject(it) }
//    }
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





