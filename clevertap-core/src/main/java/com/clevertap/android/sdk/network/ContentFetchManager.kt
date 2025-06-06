package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.events.EventGroup
import org.json.JSONArray
import org.json.JSONObject

internal class ContentFetchManager(
    config: CleverTapInstanceConfig
) {
    companion object {
        const val TAG: String = "ContentFetch"
    }

    private var networkManager: NetworkManager? = null
    private val logger: Logger = config.logger

    fun setNetworkManager(networkManager: NetworkManager) {
        this.networkManager = networkManager
    }

    fun processContentFetchItems(context: Context, contentFetchItems: List<JSONObject>) {
        logger.verbose(TAG, "Processing ${contentFetchItems.size} content fetch items")

        val payload = JSONArray()

        contentFetchItems.forEach { item ->
            try {
                val event = JSONObject().apply {
                    put("type", "event")
                    put("evtName", "content_fetch")
                    put("evtData", item)
                }

                payload.put(event)
                logger.verbose(TAG, "Processed content fetch for item: $item")
            } catch (e: Exception) {
                logger.verbose(TAG, "Error processing content fetch for tgtId: $item", e)
            }
        }

        sendContentFetchRequest(context, payload)
    }

    private fun sendContentFetchRequest(context: Context, requestArray: JSONArray) {
        try {

            logger.verbose(TAG, "Content fetch request payload: $requestArray")

            // Delegate to NetworkManager to send the request
            // Todo - Add handshake if not completed
            networkManager?.sendQueue(context, EventGroup.CONTENT_FETCH, requestArray, null)
                ?: logger.verbose(TAG, "NetworkManager not set, cannot send content fetch request")
        } catch (e: Exception) {
            logger.verbose(TAG, "Failed to send content fetch request ", e)
        }
    }
}