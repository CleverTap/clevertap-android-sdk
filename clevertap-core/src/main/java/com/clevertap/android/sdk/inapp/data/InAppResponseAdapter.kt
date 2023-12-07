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

        const val IN_APP_DEFAULT_DAILY = 10
        const val IN_APP_DEFAULT_SESSION = 10

        const val IN_APP_SESSION_KEY = Constants.INAPP_MAX_PER_SESSION_KEY
        const val IN_APP_DAILY_KEY = Constants.INAPP_MAX_PER_DAY_KEY

        @JvmStatic
        fun getListOfWhenLimits(limitJSON: JSONObject): List<LimitAdapter> {
            val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()

            return frequencyLimits.toList<JSONObject>().map { LimitAdapter(it) }.toMutableList()
        }
    }

    val preloadImage: List<String>

    val legacyInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)

    val clientSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_CS)

    val serverSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_SS)

    init {
        val list = mutableListOf<String>()

        // do legacy inapps stuff
        if (legacyInApps.first) {
            legacyInApps.second?.iterator<JSONObject> { jsonObject ->
                val portrait = jsonObject.optJSONObject(Constants.KEY_MEDIA)

                if (portrait != null) {
                    val portraitMedia = CTInAppNotificationMedia()
                        .initWithJSON(portrait, Configuration.ORIENTATION_PORTRAIT)

                    if (portraitMedia != null && portraitMedia.mediaUrl != null) {
                        list.add(portraitMedia.mediaUrl)
                    }
                }
                val landscape = jsonObject.optJSONObject(Constants.KEY_MEDIA_LANDSCAPE)
                if (landscape != null) {
                    val landscapeMedia = CTInAppNotificationMedia()
                            .initWithJSON(landscape, Configuration.ORIENTATION_LANDSCAPE)

                    if (landscapeMedia != null && landscapeMedia.mediaUrl != null) {
                        list.add(landscapeMedia.mediaUrl)
                    }
                }
            }
        }

        // do cs inapps stuff
        if (clientSideInApps.first) {
            clientSideInApps.second?.iterator<JSONObject> { jsonObject ->
                val portrait = jsonObject.optJSONObject(Constants.KEY_MEDIA)

                if (portrait != null) {
                    val portraitMedia = CTInAppNotificationMedia()
                        .initWithJSON(portrait, Configuration.ORIENTATION_PORTRAIT)

                    if (portraitMedia != null && portraitMedia.mediaUrl != null) {
                        list.add(portraitMedia.mediaUrl)
                    }
                }
                val landscape = jsonObject.optJSONObject(Constants.KEY_MEDIA_LANDSCAPE)
                if (landscape != null) {
                    val landscapeMedia = CTInAppNotificationMedia()
                            .initWithJSON(landscape, Configuration.ORIENTATION_LANDSCAPE)

                    if (landscapeMedia != null && landscapeMedia.mediaUrl != null) {
                        list.add(landscapeMedia.mediaUrl)
                    }
                }
            }
        }

        preloadImage = list
    }

    val inAppsPerSession: Int = responseJson.optInt(IN_APP_SESSION_KEY, IN_APP_DEFAULT_SESSION)

    val inAppsPerDay: Int = responseJson.optInt(IN_APP_DAILY_KEY, IN_APP_DEFAULT_DAILY)

    val inAppMode: String = responseJson.optString(Constants.INAPP_DELIVERY_MODE_KEY, "");

    val staleInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_STALE_KEY)

    val appLaunchServerSideInApps: Pair<Boolean, JSONArray?> =
        responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_APP_LAUNCHED_KEY)
}



