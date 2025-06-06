package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.ContentFetchManager
import org.json.JSONArray
import org.json.JSONObject

internal class ContentFetchResponse(
    private val config: CleverTapInstanceConfig,
    private val contentFetchManager: ContentFetchManager
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        logger.verbose(config.accountId, "Processing Content Fetch response...")

        if (config.isAnalyticsOnly) {
            logger.verbose(config.accountId,
                           "CleverTap instance is configured to analytics only, not processing Content Fetch response")
            return
        }

        if (jsonBody == null) {
            logger.verbose(config.accountId, "Can't parse Content Fetch Response, JSON response object is null")
            return
        }

        if (!jsonBody.has(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY)) {
            logger.verbose(config.accountId, "JSON object doesn't contain the content_fetch key")
            return
        }

        try {
            logger.verbose(config.accountId, "Processing Content Fetch response")
            val contentFetchArray = jsonBody.getJSONArray(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY)
            processContentFetchItems(contentFetchArray)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to parse content fetch response", t)
        }
    }

    private fun processContentFetchItems(contentFetchArray: JSONArray) {
        if (contentFetchArray.length() == 0) {
            logger.verbose(config.accountId, "No content fetch items to process")
            return
        }

        logger.verbose(config.accountId, "Found ${contentFetchArray.length()} content fetch items")
        contentFetchManager.handleContentFetch(contentFetchArray)
    }
}