package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONObject

internal class ContentFetchManager(
    private val config: CleverTapInstanceConfig
) {

    private var networkManager: NetworkManager? = null
    private val logger: Logger = config.logger

    fun setNetworkManager(networkManager: NetworkManager) {
        this.networkManager = networkManager
    }

    fun processContentFetchItems(context: Context, contentFetchItems: List<JSONObject>) {
        logger.verbose(config.accountId, "Processing ${contentFetchItems.size} content fetch items")

        // Process items sequentially
        for (item in contentFetchItems) {
            try {
                val tgtId = item.optString("tgtId", "unknown")
                logger.verbose(config.accountId, "Processing content fetch for tgtId: $tgtId")

                sendContentFetchRequest(context, item)

                logger.verbose(config.accountId, "Completed content fetch for tgtId: $tgtId")

            } catch (e: Exception) {
                val tgtId = item.optString("tgtId", "unknown")
                logger.verbose(config.accountId, "Error processing content fetch for tgtId: $tgtId", e)
            }
        }

        logger.verbose(config.accountId, "All content fetch requests completed")
    }

    private fun sendContentFetchRequest(context: Context, item: JSONObject) {
        try {
            // Create the request payload similar to sendQueue format
            val requestArray = JSONArray()

            // Add single content fetch event
            val event = JSONObject().apply {
                put("type", "event")
                put("evtName", "content_fetch")
                put("evtData", item)
            }
            requestArray.put(event)

            val tgtId = item.optString("tgtId", "unknown")
            logger.debug(config.accountId, "Sending content fetch request for tgtId: $tgtId")
            logger.verbose(config.accountId, "Content fetch payload: $requestArray")

            // Delegate to NetworkManager to send the request
            networkManager?.sendContentFetchRequest(context, requestArray.toString())
                ?: logger.verbose(config.accountId, "NetworkManager not set, cannot send content fetch request")

        } catch (e: Exception) {
            val tgtId = item.optString("tgtId", "unknown")
            logger.verbose(config.accountId, "Failed to send content fetch request for tgtId: $tgtId", e)
        }
    }
}