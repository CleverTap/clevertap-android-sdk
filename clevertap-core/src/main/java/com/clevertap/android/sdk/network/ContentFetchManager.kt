package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal class ContentFetchManager(
    config: CleverTapInstanceConfig,
    parallelRequests: Int = DEFAULT_PARALLEL_REQUESTS
) {
    companion object {
        private const val DEFAULT_PARALLEL_REQUESTS = 5
        private const val TAG: String = "ContentFetch"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(CtDefaultDispatchers().io().limitedParallelism(parallelRequests))

    private var networkManager: NetworkManager? = null
    private val logger: Logger = config.logger

    fun setNetworkManager(networkManager: NetworkManager) {
        this.networkManager = networkManager
    }

    fun handleContentFetch(contentFetchItems: JSONArray) {
        scope.launch {
            val payload = JSONArray()

            for (i in 0 until contentFetchItems.length()) {
                val item = contentFetchItems.opt(i) ?: continue
                try {
                    val event = JSONObject().apply {
                        put("type", "event")
                        put("evtName", "content_fetch")
                        put("evtData", item)
                    }
                    payload.put(event)
                    logger.verbose(TAG, "Added content fetch item: $item")
                } catch (e: Exception) {
                    logger.verbose(TAG, "Error adding content fetch item: $item", e)
                }
            }

            if (payload.length() > 0) {
                fetchContent(payload)
            } else {
                logger.verbose(TAG, "No valid content fetch items to send.")
            }
        }
    }


    private fun fetchContent(requestArray: JSONArray) {
        try {
            // Todo - Add handshake if not completed
            networkManager?.sendContentFetchRequest(requestArray)
                ?: logger.verbose(TAG, "NetworkManager not set, cannot send content fetch request")
        } catch (e: Exception) {
            logger.verbose(TAG, "Failed to send content fetch request ", e)
        }
    }

    //todo add deviceID clearing
}