package com.clevertap.android.sdk.inapp

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.isNotNullAndBlank
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.Task
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

internal class InAppPreviewHandler(
    private val executors: CTExecutors,
    private val networkManager: NetworkManager,
    private val inAppResponse: InAppResponse,
    private val context: Context,
    private val logger: ILogger
) {

    fun handleInAppPreview(extras: Bundle) {
        val task: Task<Unit> = executors.postAsyncSafelyTask()
        task.execute("testInappNotification") {
            try {
                val inappPreviewPayload = getPreviewPayload(extras) ?: return@execute
                val inappNotifs = JSONArray().put(
                    if (shouldUseHalfInterstitial(extras)) {
                        getHalfInterstitialInApp(inappPreviewPayload) ?: run {
                            logger.debug("Failed to parse the image-interstitial notification. Aborting preview display")
                            return@execute
                        }
                    } else {
                        inappPreviewPayload
                    }
                )

                val inAppResponseJson = JSONObject().apply {
                    put(Constants.INAPP_JSON_RESPONSE_KEY, inappNotifs)
                }

                inAppResponse.processResponse(inAppResponseJson, null, context)
            } catch (t: Throwable) {
                logger.verbose("Failed to display inapp notification from push notification payload", t)
            }
        }
    }

    private fun getPreviewPayload(extras: Bundle): JSONObject? {
        val s3Url = extras.getString(Constants.INAPP_PREVIEW_S3_URL_KEY)
        if (s3Url.isNotNullAndBlank()) {
            networkManager.fetchInAppPreviewPayloadFromUrl(s3Url)?.let { return it }
        }

        return extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY)?.let { JSONObject(it) }
    }

    private fun shouldUseHalfInterstitial(extras: Bundle): Boolean {
        val type = extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY)
        return type == Constants.INAPP_IMAGE_INTERSTITIAL_TYPE
                || type == Constants.INAPP_ADVANCED_BUILDER_TYPE
    }

    @Throws(JSONException::class)
    private fun getHalfInterstitialInApp(inapp: JSONObject): JSONObject? {
        val inAppConfig = inapp.optString(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG)
        val htmlContent = wrapImageInterstitialContent(inAppConfig) ?: run {
            logger.debug("Failed to parse the image-interstitial notification")
            return null
        }

        inapp.put(Constants.KEY_TYPE, Constants.KEY_CUSTOM_HTML)

        val dataObject = when (val data = inapp.opt(Constants.INAPP_DATA_TAG)) {
            is JSONObject -> JSONObject(data.toString()) // Create a mutable copy
            else -> JSONObject()
        }

        dataObject.put(Constants.INAPP_HTML_TAG, htmlContent)
        inapp.put(Constants.INAPP_DATA_TAG, dataObject)

        return inapp
    }

    fun wrapImageInterstitialContent(content: String?): String? {
        if (content.isNullOrBlank()) return null

        return try {
            val html = Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) ?: return null

            val parts = html.split(Constants.INAPP_HTML_SPLIT)
            if (parts.size == 2) {
                parts[0] + content + parts[1]
            } else {
                null
            }
        } catch (e: IOException) {
            logger.debug("Failed to read the image-interstitial HTML file", e)
            null
        }
    }
}
