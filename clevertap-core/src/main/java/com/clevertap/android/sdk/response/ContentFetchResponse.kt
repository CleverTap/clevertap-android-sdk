package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.ContentFetchItem
import com.clevertap.android.sdk.network.ContentFetchManager
import org.json.JSONArray
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ContentFetchResponse(
    private val config: CleverTapInstanceConfig,
    private val contentFetchManager: ContentFetchManager
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        processResponse(jsonBody, stringBody, context, ResponseContext.DEFAULT)
    }

    override fun processResponse(
        jsonBody: JSONObject?,
        stringBody: String?,
        context: Context,
        responseContext: ResponseContext
    ) {
        logger.verbose(config.accountId, "Processing Content Fetch response...")

        val body = processableBodyOrNull(jsonBody = jsonBody, responseContext = responseContext) ?: return

        try {
            logger.verbose(config.accountId, "Processing Content Fetch response")
            val contentFetchArray = body.getJSONArray(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY)
            processContentFetchItems(contentFetchArray, context)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to parse content fetch response", t)
        }
    }

    /**
     * Returns [jsonBody] if it carries a content_fetch directive this instance should act on,
     * otherwise logs why it is skipped and returns null.
     */
    private fun processableBodyOrNull(jsonBody: JSONObject?, responseContext: ResponseContext): JSONObject? {
        // Recursion guard: a /content response is re-fed through this same decorator chain. A
        // content_fetch nested inside it would trigger another fetch, unbounded. Only /a1 may open
        // a fetch.
        if (responseContext.source == CTResponseSource.CONTENT_FETCH) {
            logger.verbose(config.accountId,
                           "Ignoring content_fetch from a content fetch response (recursion guard)")
            return null
        }

        if (config.isAnalyticsOnly) {
            logger.verbose(config.accountId,
                           "CleverTap instance is configured to analytics only, not processing Content Fetch response")
            return null
        }

        if (jsonBody == null) {
            logger.verbose(config.accountId, "Can't parse Content Fetch Response, JSON response object is null")
            return null
        }

        if (!jsonBody.has(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY)) {
            logger.verbose(config.accountId, "JSON object doesn't contain the content_fetch key")
            return null
        }
        return jsonBody
    }

    private fun processContentFetchItems(contentFetchArray: JSONArray, context: Context) {
        if (contentFetchArray.length() == 0) {
            logger.verbose(config.accountId, "No content fetch items to process")
            return
        }

        // Typed, addressable view of the directive.
        val items = ContentFetchItem.listFrom(contentFetchArray)
        logger.verbose(config.accountId,
                       "Found ${items.size} content fetch items, responseKeys=${items.map { it.responseKey }}")

        // Only an app-launch-in-app batch may settle the arbitration window on completion; inbox /
        // native-display / other-event fetches must not close a window they didn't open.
        val firesArbitrationComplete = items.any { it.isAppLaunchInApp() }
        contentFetchManager.handleContentFetch(contentFetchArray, context.packageName, firesArbitrationComplete)
    }
}
