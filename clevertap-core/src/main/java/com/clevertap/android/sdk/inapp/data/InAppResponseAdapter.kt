package com.clevertap.android.sdk.inapp.data

import android.content.res.Configuration
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.CTInAppNotificationMedia
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData.CREATOR.createFromJson
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.evaluation.LimitAdapter
import com.clevertap.android.sdk.iterator
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.safeGetJSONArray
import com.clevertap.android.sdk.safeGetJSONArrayOrNullIfEmpty
import com.clevertap.android.sdk.toList
import org.json.JSONArray
import org.json.JSONObject

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

    val legacyInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArrayOrNullIfEmpty(Constants.INAPP_JSON_RESPONSE_KEY)
    val clientSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_CS)
    val serverSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArray(Constants.INAPP_NOTIFS_KEY_SS)
    val appLaunchServerSideInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArrayOrNullIfEmpty(Constants.INAPP_NOTIFS_APP_LAUNCHED_KEY)
    
    val preloadImages: List<String>
    val preloadGifs: List<String>
    val preloadFiles: List<String>
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
                }).distinctBy { it.first } // todo no need to copy over and over
    }

    private fun fetchMediaUrls(
        imageList: MutableList<String>,
        gifList: MutableList<String>
    ) {
        if (clientSideInApps.first) {
            clientSideInApps.second?.iterator<JSONObject> { jsonObject ->
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

    private fun fetchFilesUrlsForTemplates(filesList: MutableList<String>, templatesManager: TemplatesManager) {
        if (clientSideInApps.first) {
            for (i in 0 until clientSideInApps.second!!.length()) {
                val customTemplateInAppData = createFromJson(clientSideInApps.second!!.getJSONObject(i))
                if (customTemplateInAppData != null) {
                    val fileArgs = customTemplateInAppData.getFileArgsUrls(
                        templatesManager
                    )
                    filesList.addAll(fileArgs.map { it.first })
                }
            }
        }
    }

    val inAppsPerSession: Int = responseJson.optInt(IN_APP_SESSION_KEY, IN_APP_DEFAULT_SESSION)

    val inAppsPerDay: Int = responseJson.optInt(IN_APP_DAILY_KEY, IN_APP_DEFAULT_DAILY)

    val inAppMode: String = responseJson.optString(Constants.INAPP_DELIVERY_MODE_KEY, "")

    val staleInApps: Pair<Boolean, JSONArray?> = responseJson.safeGetJSONArrayOrNullIfEmpty(Constants.INAPP_NOTIFS_STALE_KEY)
}

enum class CtCacheType {
    IMAGE, GIF, FILES
}


