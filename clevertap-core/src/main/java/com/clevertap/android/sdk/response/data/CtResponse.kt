package com.clevertap.android.sdk.response.data

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.evaluation.LimitType
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.response.data.InAppBase.Companion.getListOfWhenLimits
import com.clevertap.android.sdk.safeGetJSONArray
import com.clevertap.android.sdk.toList
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONException
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

    fun inAppsPerSession(): Int = response.optInt(IN_APP_SESSION_KEY, IN_APP_DEFAULT_SESSION)

    fun inAppsPerDay(): Int = response.optInt(IN_APP_DAILY_KEY, IN_APP_DEFAULT_DAILY)

    fun legacyInApps(): Pair<Boolean, JSONArray?> =
        response.safeGetJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)

    fun appLaunchServerSideInApps(): Pair<Boolean, JSONArray?> =
        response.safeGetJSONArray(Constants.INAPP_NOTIFS_APP_LAUNCHED_KEY)

    fun clientSideInApps(): Pair<Boolean, JSONArray?> =
        response.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_CS)

    fun serverSideInApps(): Pair<Boolean, JSONArray?> =
        response.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_SS)

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

// Define a common interface for the properties that are common to both data classes
interface InAppBase {
    val campaignId: String // TODO: can this be nullable??
    val whenTriggers: JSONArray
    val whenLimits: List<WhenLimit>
    val excludeGlobalFCaps: Int
    val priority: Int
    val shouldSuppress: Boolean
    val wzrk_pivot: String?
    val wzrk_cgId: Int?

    companion object {
        @Throws(JSONException::class)
        fun getListOfWhenLimits(limitJSON: JSONObject): List<WhenLimit> {
            val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()
            val occurrenceLimits =
                limitJSON.optJSONArray(Constants.INAPP_OCCURRENCE_LIMITS).orEmptyArray()

            val whenLimits: MutableList<JSONObject> = mutableListOf()
            whenLimits.addAll(frequencyLimits.toList())
            whenLimits.addAll(occurrenceLimits.toList())

            val whenLimitsList = ArrayList<WhenLimit>()

            for (i in 0 until whenLimits.size) {
                val whenLimitJson = whenLimits[i]
                val whenLimit = WhenLimit.fromJSONObject(whenLimitJson)
                whenLimitsList.add(whenLimit)
            }
            return whenLimitsList
        }
    }
}

data class InAppClientSide(
    val wzrk_id: Int,
    val wzrk_ttl_offset: Long?,
    var wzrk_ttl: Long?,
    override val campaignId: String,
    override val shouldSuppress: Boolean,
    override val excludeGlobalFCaps: Int,
    override val priority: Int,
    override val wzrk_pivot: String?,
    override val wzrk_cgId: Int?,
    override val whenTriggers: JSONArray,
    override val whenLimits: List<WhenLimit>
) : InAppBase {
    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(Constants.NOTIFICATION_ID_TAG, wzrk_id)
        wzrk_ttl_offset?.let { jsonObject.put(Constants.WZRK_TIME_TO_LIVE_OFFSET, it) }
        wzrk_ttl?.let { jsonObject.put(Constants.WZRK_TIME_TO_LIVE, it) }
        jsonObject.put(Constants.INAPP_ID_IN_PAYLOAD, campaignId)
        jsonObject.put(Constants.INAPP_WHEN_TRIGGERS, whenTriggers)
        jsonObject.put(Constants.INAPP_SUPPRESSED, shouldSuppress)
        jsonObject.put(Constants.KEY_EXCLUDE_GLOBAL_CAPS, excludeGlobalFCaps)
        jsonObject.put(Constants.INAPP_PRIORITY, priority)
        wzrk_pivot?.let { jsonObject.put(Constants.INAPP_WZRK_PIVOT, it) }
        wzrk_cgId?.let { jsonObject.put(Constants.INAPP_WZRK_CGID, it) }
        jsonObject.put(Constants.INAPP_WHEN_LIMITS, JSONArray(whenLimits.map { it.toJsonObject() }))
        return jsonObject
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject): InAppClientSide? {
            return try {
                InAppClientSide(
                    campaignId = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD),
                    wzrk_id = jsonObject.optInt(Constants.NOTIFICATION_ID_TAG),
                    wzrk_pivot = jsonObject.optString(Constants.INAPP_WZRK_PIVOT),
                    wzrk_cgId = jsonObject.optInt(Constants.INAPP_WZRK_CGID),
                    wzrk_ttl_offset = jsonObject.optLong(Constants.WZRK_TIME_TO_LIVE_OFFSET),
                    wzrk_ttl = getTtl(jsonObject),
                    whenTriggers = jsonObject.optJSONArray(Constants.INAPP_WHEN_TRIGGERS)
                        .orEmptyArray(),
                    whenLimits = getListOfWhenLimits(jsonObject),
                    excludeGlobalFCaps = jsonObject.optInt(Constants.KEY_EXCLUDE_GLOBAL_CAPS),
                    priority = jsonObject.optInt(Constants.INAPP_PRIORITY),
                    shouldSuppress = jsonObject.optBoolean(Constants.INAPP_SUPPRESSED),
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun getTtl(inApp: JSONObject): Long? {
            val offset = inApp[Constants.WZRK_TIME_TO_LIVE_OFFSET] as? Long
            if (offset != null) {
                val now = Clock.SYSTEM.currentTimeSeconds()
                return now + offset
            } else {
                // Remove/nullify TTL since it cannot be calculated based on the TTL offset
                // The default TTL will be set in CTInAppNotification
                //inApp.remove(Constants.WZRK_TIME_TO_LIVE)
                return null;
            }
        }

        fun getListFromJsonArray(jsonArray: JSONArray): List<InAppClientSide> {
            return jsonArray.toList().mapNotNull { fromJSONObject(it) }
        }
    }
}

data class InAppServerSide(
    override val campaignId: String,
    override val excludeGlobalFCaps: Int,
    override val priority: Int,
    override val shouldSuppress: Boolean,
    override val wzrk_pivot: String = "wzrk_default",
    override val wzrk_cgId: Int?,
    override val whenTriggers: JSONArray,
    override val whenLimits: List<WhenLimit>,
) : InAppBase {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(Constants.INAPP_ID_IN_PAYLOAD, campaignId)
        jsonObject.put(Constants.INAPP_WHEN_TRIGGERS, whenTriggers)
        jsonObject.put(Constants.KEY_EXCLUDE_GLOBAL_CAPS, excludeGlobalFCaps)
        jsonObject.put(Constants.INAPP_PRIORITY, priority)
        jsonObject.put(Constants.INAPP_SUPPRESSED, shouldSuppress)
        jsonObject.put(Constants.INAPP_WZRK_PIVOT, wzrk_pivot)
        wzrk_cgId?.let { jsonObject.put(Constants.INAPP_WZRK_CGID, it) }
        jsonObject.put(Constants.INAPP_WHEN_LIMITS, JSONArray(whenLimits.map { it.toJsonObject() }))
        return jsonObject
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject): InAppServerSide? {
            return try {
                InAppServerSide(
                    campaignId = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD),
                    whenTriggers = jsonObject.optJSONArray(Constants.INAPP_WHEN_TRIGGERS)
                        .orEmptyArray(),
                    excludeGlobalFCaps = jsonObject.optInt(Constants.KEY_EXCLUDE_GLOBAL_CAPS),
                    priority = jsonObject.optInt(Constants.INAPP_PRIORITY),
                    shouldSuppress = jsonObject.optBoolean(Constants.INAPP_SUPPRESSED),
                    wzrk_cgId = jsonObject.optInt(Constants.INAPP_WZRK_CGID),
                    whenLimits = getListOfWhenLimits(jsonObject),
                )
            } catch (e: Exception) {
                null
            }
        }

        fun getListFromJsonArray(jsonArray: JSONArray): List<InAppServerSide> {
            return jsonArray.toList().mapNotNull { fromJSONObject(it) }
        }
    }
}

data class WhenLimit(
    val limitType: LimitType, val limit: Int, val frequency: Int
) {
    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(Constants.KEY_TYPE, limitType.toString())
        jsonObject.put(Constants.KEY_LIMIT, limit)
        if (frequency >= 0) {
            jsonObject.put(Constants.KEY_FREQUENCY, frequency)
        }
        return jsonObject
    }

    companion object {
        @Throws(JSONException::class)
        fun fromJSONObject(jsonObject: JSONObject): WhenLimit {
            val type = jsonObject.optString(Constants.KEY_TYPE)
            val limitType = LimitType.fromString(type)
            val limit = jsonObject.optInt(Constants.KEY_LIMIT)
            val frequency = jsonObject.optInt(
                Constants.KEY_FREQUENCY, -1
            ) // Use -1 as the default if "frequency" is not present

            return WhenLimit(limitType, limit, frequency)
        }
    }
}





