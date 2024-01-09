package com.clevertap.android.sdk.inapp.data

import android.content.res.Configuration
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.CTInAppNotificationMedia
import com.clevertap.android.sdk.inapp.evaluation.LimitAdapter
import com.clevertap.android.sdk.inapp.evaluation.TriggerAdapter
import com.clevertap.android.sdk.iterator
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.safeGetJSONArray
import com.clevertap.android.sdk.toList
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Class that wraps functionality for response and return relevant methods to get data
 */
class InAppResponseAdapter(
    responseJson: JSONObject
) {

    companion object {

        private const val IN_APP_DEFAULT_DAILY = 10
        private const val IN_APP_DEFAULT_SESSION = 10

        private const val IN_APP_SESSION_KEY = Constants.INAPP_MAX_PER_SESSION_KEY
        private const val IN_APP_DAILY_KEY = Constants.INAPP_MAX_PER_DAY_KEY

        @JvmStatic
        fun getListOfWhenLimits(limitJSON: JSONObject): List<LimitAdapter> {
            val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()

            return frequencyLimits.toList<JSONObject>().map { LimitAdapter(it) }.toMutableList()
        }
    }

    val preloadImages: List<String>
    val preloadGifs: List<String>

    val legacyInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)

    val clientSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_CS)

    val serverSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_SS)

    val appLaunchServerSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_APP_LAUNCHED_KEY)

    init {
        val imageList = mutableListOf<String>()
        val gifList = mutableListOf<String>()

        //fetchMediaUrls(legacyInApps, list)
        //fetchMediaUrls(appLaunchServerSideInApps, list)
        fetchMediaUrls(clientSideInApps, imageList, gifList)

        preloadImages = imageList
        preloadGifs = gifList
    }

    private fun fetchMediaUrls(
        data: Pair<Boolean, JSONArray?>,
        imageList: MutableList<String>,
        gifList: MutableList<String>
    ) {
        if (data.first) {
            data.second?.iterator<JSONObject> { jsonObject ->
                val portrait = jsonObject.optJSONObject(Constants.KEY_MEDIA)

                if (portrait != null) {
                    val portraitMedia = CTInAppNotificationMedia()
                        .initWithJSON(portrait, Configuration.ORIENTATION_PORTRAIT)

                    if (portraitMedia != null && portraitMedia.mediaUrl != null) {
                        if (portraitMedia.isImage) {
                            imageList.add(portraitMedia.mediaUrl)
                        } else if (portraitMedia.isGIF) {
                            gifList.add(portraitMedia.mediaUrl)
                        }
                    }
                }
                val landscape = jsonObject.optJSONObject(Constants.KEY_MEDIA_LANDSCAPE)
                if (landscape != null) {
                    val landscapeMedia = CTInAppNotificationMedia()
                            .initWithJSON(landscape, Configuration.ORIENTATION_LANDSCAPE)

                    if (landscapeMedia != null && landscapeMedia.mediaUrl != null) {
                        if (landscapeMedia.isImage) {
                            imageList.add(landscapeMedia.mediaUrl)
                        } else if (landscapeMedia.isGIF) {
                            gifList.add(landscapeMedia.mediaUrl)
                        }
                    }
                }
            }
        }
    }

    val inAppsPerSession: Int = responseJson.optInt(IN_APP_SESSION_KEY, IN_APP_DEFAULT_SESSION)

    val inAppsPerDay: Int = responseJson.optInt(IN_APP_DAILY_KEY, IN_APP_DEFAULT_DAILY)

    val inAppMode: String = responseJson.optString(Constants.INAPP_DELIVERY_MODE_KEY, "");

    val staleInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_STALE_KEY)
}

// Define a common interface for the properties that are common to both data classes
interface InAppBase {

    val campaignId: String  //TODO: can be null?
    val wzrk_id: String
    val priority: Int
    val shouldSuppress: Boolean
    val wzrk_pivot: String
    val wzrk_cgId: Int?
    val whenLimits: List<LimitAdapter>
    val whenTriggers: List<TriggerAdapter>

    companion object {

        @Throws(JSONException::class)
        fun getListOfWhenLimitsAndOccurrenceLimits(limitJSON: JSONObject): List<LimitAdapter> {
            val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()
            val occurrenceLimits = limitJSON.optJSONArray(Constants.INAPP_OCCURRENCE_LIMITS).orEmptyArray()

            return (frequencyLimits.toList<JSONObject>() + occurrenceLimits.toList()).map { LimitAdapter(it) }
                .toMutableList()
        }

        @Throws(JSONException::class)
        fun getListOfWhenTriggers(triggerJson: JSONObject): List<TriggerAdapter> {
            val whenTriggers = triggerJson.optJSONArray(Constants.INAPP_WHEN_TRIGGERS).orEmptyArray()
            return (0 until whenTriggers.length()).map { TriggerAdapter(whenTriggers[it] as JSONObject) }
        }
    }
}

data class InAppClientSide(
    override val campaignId: String,
    override val wzrk_id: String,
    override val priority: Int,
    override val wzrk_pivot: String,
    override val shouldSuppress: Boolean,
    var wzrk_ttl: Long?,
    override val wzrk_cgId: Int,   //TODO: non-optional in case of csInApps?
    override val whenLimits: List<LimitAdapter>,
    override val whenTriggers: List<TriggerAdapter>,
    //val inAppNotification: CTInAppNotification   //TODO: should add?
) : InAppBase {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(Constants.INAPP_ID_IN_PAYLOAD, campaignId)
        jsonObject.put(Constants.NOTIFICATION_ID_TAG, wzrk_id)
        jsonObject.put(Constants.INAPP_PRIORITY, priority)
        jsonObject.put(Constants.INAPP_WZRK_PIVOT, wzrk_pivot)
        jsonObject.put(Constants.INAPP_SUPPRESSED, shouldSuppress)
        wzrk_ttl?.let { jsonObject.put(Constants.WZRK_TIME_TO_LIVE, it) }
        jsonObject.put(Constants.INAPP_WZRK_CGID, wzrk_cgId)

        //TODO: add toJsonObject() to LimitAdapter - DONE
        jsonObject.put(Constants.INAPP_WHEN_TRIGGERS, JSONArray(whenTriggers.map { it.toJsonObject() }))

        //TODO: add toJsonObject() to TriggerAdapter - DONE
        jsonObject.put(Constants.INAPP_WHEN_LIMITS, JSONArray(whenLimits.map { it.toJsonObject() }))
        return jsonObject
    }

    companion object {

        fun fromJSONObject(jsonObject: JSONObject): InAppClientSide? {
            return try {
                InAppClientSide(
                    campaignId = jsonObject.optLong(Constants.INAPP_ID_IN_PAYLOAD).toString(),
                    wzrk_id = jsonObject.optString(Constants.NOTIFICATION_ID_TAG),
                    priority = jsonObject.optInt(Constants.INAPP_PRIORITY),
                    wzrk_pivot = jsonObject.optString(Constants.INAPP_WZRK_PIVOT, "wzrk_default"),
                    shouldSuppress = jsonObject.optBoolean(Constants.INAPP_SUPPRESSED),
                    wzrk_ttl = getTtl(jsonObject),
                    wzrk_cgId = jsonObject.optInt(Constants.INAPP_WZRK_CGID),
                    whenLimits = InAppBase.getListOfWhenLimitsAndOccurrenceLimits(jsonObject),
                    whenTriggers = InAppBase.getListOfWhenTriggers(jsonObject),
                )
            } catch (e: Exception) {
                //TODO: log error
                null
            }
        }

        private fun getTtl(inApp: JSONObject): Long? {
            val offset = inApp[Constants.WZRK_TIME_TO_LIVE_OFFSET] as? Long
            return if (offset != null) {
                val now = Clock.SYSTEM.currentTimeSeconds()
                now + offset
            } else {
                // return TTL as null since it cannot be calculated due to null offset value
                // The default TTL will be set in the CTInAppNotification
                null
            }
        }

        fun getListFromJsonArray(jsonArray: JSONArray): List<InAppClientSide> {
            return jsonArray.toList<JSONObject>().mapNotNull { fromJSONObject(it) }
        }
    }
}

data class InAppServerSide(
    override val campaignId: String,
    override val wzrk_id: String,
    override val priority: Int,
    override val wzrk_pivot: String,
    override val shouldSuppress: Boolean,
    override val wzrk_cgId: Int?,    //TODO: optional in case of ssInApps?
    val excludeGlobalFCaps: Boolean, //<-- An additional key compare to CS InApp. If globalCap is off, efc with -1 or 1 is received.
    override val whenLimits: List<LimitAdapter>,
    override val whenTriggers: List<TriggerAdapter>
) : InAppBase {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(Constants.INAPP_ID_IN_PAYLOAD, campaignId)
        jsonObject.put(Constants.NOTIFICATION_ID_TAG, wzrk_id)
        jsonObject.put(Constants.INAPP_PRIORITY, priority)
        jsonObject.put(Constants.INAPP_WZRK_PIVOT, wzrk_pivot)
        jsonObject.put(Constants.INAPP_SUPPRESSED, shouldSuppress)
        jsonObject.put(Constants.INAPP_WZRK_CGID, wzrk_cgId)
        jsonObject.put(Constants.KEY_EXCLUDE_GLOBAL_CAPS, excludeGlobalFCaps)

        //TODO: add toJsonObject() to LimitAdapter - DONE
        jsonObject.put(Constants.INAPP_WHEN_TRIGGERS, JSONArray(whenTriggers.map { it.toJsonObject() }))

        //TODO: add toJsonObject() to TriggerAdapter - DONE
        jsonObject.put(Constants.INAPP_WHEN_LIMITS, JSONArray(whenLimits.map { it.toJsonObject() }))
        return jsonObject
    }

    companion object {

        fun fromJSONObject(jsonObject: JSONObject): InAppServerSide? {
            return try {
                InAppServerSide(
                    campaignId = jsonObject.optLong(Constants.INAPP_ID_IN_PAYLOAD)
                        .toString(), //TODO: redundant toString?
                    wzrk_id = jsonObject.optString(Constants.NOTIFICATION_ID_TAG),
                    priority = jsonObject.optInt(Constants.INAPP_PRIORITY),
                    wzrk_pivot = jsonObject.optString(Constants.INAPP_WZRK_PIVOT, "wzrk_default"),
                    shouldSuppress = jsonObject.optBoolean(Constants.INAPP_SUPPRESSED),
                    wzrk_cgId = jsonObject.optInt(Constants.INAPP_WZRK_CGID),
                    excludeGlobalFCaps = jsonObject.optBoolean(Constants.KEY_EXCLUDE_GLOBAL_CAPS),
                    whenLimits = InAppBase.getListOfWhenLimitsAndOccurrenceLimits(jsonObject),
                    whenTriggers = InAppBase.getListOfWhenTriggers(jsonObject),
                )
            } catch (e: Exception) {
                //TODO: log error
                null
            }
        }

        fun getListFromJsonArray(jsonArray: JSONArray): List<InAppServerSide> {
            return jsonArray.toList<JSONObject>().mapNotNull { fromJSONObject(it) }
        }
    }
}



