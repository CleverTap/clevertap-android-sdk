package com.clevertap.android.sdk.inapp.data

import android.content.res.Configuration
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.CTInAppNotificationMedia
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData.CREATOR.createFromJson
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DEFAULT_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MAX_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MIN_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.evaluation.LimitAdapter
import com.clevertap.android.sdk.iterator
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.partition
import com.clevertap.android.sdk.safeGetJSONArray
import com.clevertap.android.sdk.safeGetJSONArrayOrNullIfEmpty
import com.clevertap.android.sdk.toList
import org.json.JSONArray
import org.json.JSONObject

object InAppDelayConstants{
    const val INAPP_DELAY_AFTER_TRIGGER = "delayAfterTrigger"
    const val INAPP_DEFAULT_DELAY_SECONDS = 0
    const val INAPP_MIN_DELAY_SECONDS = 1
    const val INAPP_MAX_DELAY_SECONDS = 1200
}
/**
 * Class that wraps functionality for response and return relevant methods to get data
 */
internal class InAppResponseAdapter(
    responseJson: JSONObject,
    templatesManager: TemplatesManager
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

    private val legacyInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArrayOrNullIfEmpty(Constants.INAPP_JSON_RESPONSE_KEY)
    val partitionedLegacyInApps = partitionInAppsByDelay(legacyInApps.second)
    private val clientSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_CS)
    val partitionedClientSideInApps = partitionInAppsByDelay(clientSideInApps.second)
    val serverSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_SS)
    private val appLaunchServerSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArrayOrNullIfEmpty(Constants.INAPP_NOTIFS_APP_LAUNCHED_KEY)
    val partitionedAppLaunchServerSideInApps = partitionInAppsByDelay(appLaunchServerSideInApps.second)

    private val preloadImages: List<String>
    private val preloadGifs: List<String>
    private val preloadFiles: List<String>
    val preloadAssets: List<String>
    val preloadAssetsMeta: List<Pair<String, CtCacheType>>

    init {
        val imageList = mutableListOf<String>()
        val gifList = mutableListOf<String>()
        val filesList = mutableListOf<String>()

        fetchMediaUrls(
            imageList = imageList,
            gifList = gifList
        )
        fetchFilesUrlsForTemplates(
            filesList = filesList,
            templatesManager = templatesManager
        )

        preloadImages = imageList
        preloadGifs = gifList
        preloadFiles = filesList

        preloadAssets = imageList + gifList + filesList
        preloadAssetsMeta = (imageList.map { Pair(it, CtCacheType.IMAGE) } +
                gifList.map { Pair(it, CtCacheType.GIF) } +
                filesList.map {
                    Pair(it, CtCacheType.FILES)
                }).distinctBy { it.first }
    }

    private fun fetchMediaUrls(
        imageList: MutableList<String>,
        gifList: MutableList<String>
    ) {
        if (clientSideInApps.first) {
            clientSideInApps.second?.iterator<JSONObject> { jsonObject ->
                val portrait = jsonObject.optJSONObject(Constants.KEY_MEDIA)

                if (portrait != null) {
                    val portraitMedia = CTInAppNotificationMedia.create(
                        portrait,
                        Configuration.ORIENTATION_PORTRAIT
                    )

                    if (portraitMedia != null && portraitMedia.mediaUrl.isNotBlank()) {
                        if (portraitMedia.isImage()) {
                            imageList.add(portraitMedia.mediaUrl)
                        } else if (portraitMedia.isGIF()) {
                            gifList.add(portraitMedia.mediaUrl)
                        }
                    }
                }
                val landscape = jsonObject.optJSONObject(Constants.KEY_MEDIA_LANDSCAPE)
                if (landscape != null) {
                    val landscapeMedia = CTInAppNotificationMedia.create(landscape, Configuration.ORIENTATION_LANDSCAPE)

                    if (landscapeMedia != null && landscapeMedia.mediaUrl.isNotBlank()) {
                        if (landscapeMedia.isImage()) {
                            imageList.add(landscapeMedia.mediaUrl)
                        } else if (landscapeMedia.isGIF()) {
                            gifList.add(landscapeMedia.mediaUrl)
                        }
                    }
                }
            }
        }
    }

    private fun fetchFilesUrlsForTemplates(filesList: MutableList<String>, templatesManager: TemplatesManager) {
        if (clientSideInApps.first) {
            val inAppsList = clientSideInApps.second ?: return
            for (i in 0 until inAppsList.length()) {
                createFromJson(inAppsList.optJSONObject(i))?.getFileArgsUrls(
                    templatesManager,
                    filesList
                )
            }
        }
    }

    val inAppsPerSession: Int = responseJson.optInt(IN_APP_SESSION_KEY, IN_APP_DEFAULT_SESSION)

    val inAppsPerDay: Int = responseJson.optInt(IN_APP_DAILY_KEY, IN_APP_DEFAULT_DAILY)

    val inAppMode: String = responseJson.optString(Constants.INAPP_DELIVERY_MODE_KEY, "")

    val staleInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArrayOrNullIfEmpty(Constants.INAPP_NOTIFS_STALE_KEY)

    /**
     * Core partitioning logic that separates in-apps based on delay
     */
    private fun partitionInAppsByDelay(inAppsArray: JSONArray?): PartitionedInApps {
        if (inAppsArray == null) {
            return PartitionedInApps.empty()
        }
        val (immediateList, delayedList) = inAppsArray.partition<JSONObject> { inApp ->
            hasNoDelay(inApp)
        }

        return PartitionedInApps(
            immediateInApps = immediateList,
            delayedInApps = delayedList
        )
    }

    /**
     * Helper function to determine if an in-app has no delay or delay is 0
     * An in-app is considered immediate if:
     * 1. It doesn't have a delayAfterTrigger field, OR
     * 2. The delayAfterTrigger field is 0 or negative, OR
     * 3. The delayAfterTrigger field is outside valid range (1-1200)
     *
     * @param inApp JSONObject representing the in-app notification
     * @return true if immediate, false if delayed
     */
    private fun hasNoDelay(inApp: JSONObject): Boolean = getValidatedInAppDelay(inApp) == INAPP_DEFAULT_DELAY_SECONDS


    /**
     * Helper function to get the delay value in seconds for a delayed in-app
     * @param inApp JSONObject representing the in-app notification
     * @return delay in seconds, 0 if no delay or invalid delay
     */
    private fun getValidatedInAppDelay(inApp: JSONObject): Int {
        val delaySeconds = inApp.optInt(INAPP_DELAY_AFTER_TRIGGER, INAPP_DEFAULT_DELAY_SECONDS)
        return if (delaySeconds in INAPP_MIN_DELAY_SECONDS..INAPP_MAX_DELAY_SECONDS) {
            delaySeconds
        } else {
            INAPP_DEFAULT_DELAY_SECONDS
        }
    }
}

enum class CtCacheType {
    IMAGE, GIF, FILES
}


