package com.clevertap.android.sdk.response

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.network.ContentFetchManager
import org.json.JSONArray
import org.json.JSONObject

internal class ContentFetchResponse(
    private val accountId: String,
    private val logger: ILogger,
) {

    fun processResponse(
        jsonBody: JSONObject?,
        packageName: String,
        contentFetchManager: ContentFetchManager
    ) {
        logger.verbose(accountId, "Processing Content Fetch response...")

        if (jsonBody == null) {
            logger.verbose(accountId, "Can't parse Content Fetch Response, JSON response object is null")
            return
        }

        if (!jsonBody.has(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY)) {
            logger.verbose(accountId, "JSON object doesn't contain the content_fetch key")
            return
        }

        try {
            logger.verbose(accountId, "Processing Content Fetch response")
            val contentFetchArray = jsonBody.getJSONArray(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY)
            processContentFetchItems(contentFetchArray, packageName, contentFetchManager)
        } catch (t: Throwable) {
            logger.verbose(accountId, "Failed to parse content fetch response", t)
        }
    }

    private fun processContentFetchItems(
        contentFetchArray: JSONArray,
        packageName: String,
        contentFetchManager: ContentFetchManager
    ) {
        if (contentFetchArray.length() == 0) {
            logger.verbose(accountId, "No content fetch items to process")
            return
        }

        logger.verbose(accountId, "Found ${contentFetchArray.length()} content fetch items")
        contentFetchManager.handleContentFetch(contentFetchArray, packageName)
    }
}