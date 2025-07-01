package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Utils.getNow
import com.clevertap.android.sdk.network.api.ContentFetchRequestBody
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.response.ClevertapResponseHandler
import com.clevertap.android.sdk.toJsonOrNull
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
internal class ContentFetchManager(
    config: CleverTapInstanceConfig,
    private val coreMetaData: CoreMetaData,
    private val queueHeaderBuilder: QueueHeaderBuilder,
    private val ctApiWrapper: CtApiWrapper,
    private val parallelRequests: Int = DEFAULT_PARALLEL_REQUESTS
) {
    companion object {
        private const val DEFAULT_PARALLEL_REQUESTS = 5
        private const val TAG = "ContentFetch"
    }

    var clevertapResponseHandler: ClevertapResponseHandler? = null

    var parentJob = SupervisorJob()

    private var scope = CoroutineScope(
        parentJob + CtDefaultDispatchers().io().limitedParallelism(parallelRequests)
    )
    private val logger = config.logger
    private val accountId = config.accountId

    fun handleContentFetch(contentFetchItems: JSONArray, packageName: String) {
        scope.launch {
            try {
                val payload = getContentFetchPayload(contentFetchItems, packageName)
                if (payload.length() > 0) {
                    sendContentFetchRequest(payload)
                } else {
                    logger.verbose(TAG, "No valid content fetch items to send.")
                }
            } catch (_: CancellationException) {
                logger.verbose(TAG, "Fetch job was cancelled.")
            } catch (e: Exception) {
                logger.verbose(TAG, "Unexpected error during content fetch", e)
            }
        }
    }

    private fun getContentFetchPayload(
        contentFetchItems: JSONArray,
        packageName: String
    ): JSONArray {
        val payload = JSONArray()

        for (i in 0 until contentFetchItems.length()) {
            val item = contentFetchItems.opt(i) ?: continue
            try {
                val event = getMetaData(packageName).apply {
                    put(Constants.KEY_EVT_DATA, item)
                }
                payload.put(event)
                logger.verbose(TAG, "Added content fetch item: $item")
            } catch (e: Exception) {
                logger.verbose(TAG, "Error adding content fetch item: $item", e)
            }
        }

        return payload
    }

    private fun getMetaData(packageName: String): JSONObject {
        return JSONObject().apply {
            put(Constants.KEY_TYPE, "event")
            put(Constants.KEY_EVT_NAME, "content_fetch")
            put("s", coreMetaData.currentSessionId)
            put("pg", CoreMetaData.getActivityCount())
            put("ep", getNow())
            put("f", coreMetaData.isFirstSession)
            put("lsl", coreMetaData.lastSessionLength)
            put("pai", packageName)
            coreMetaData.screenName?.let { put("n", it) }
        }
    }

    private suspend fun sendContentFetchRequest(content: JSONArray): Boolean {
        val header = queueHeaderBuilder.buildHeader(null) ?: return false
        val body = ContentFetchRequestBody(header, content)
        logger.debug(accountId, "Fetching Content: $body")

        try {
            ctApiWrapper.ctApi.sendContentFetch(body).use { response ->
                return handleContentFetchResponse(response, !currentCoroutineContext().isActive)
            }
        } catch (e: Exception) {
            logger.debug(accountId, "An exception occurred while fetching content.", e)
            return false
        }
    }

    /**
     * Handles the response from content fetch requests
     * Processes through normal ResponseDecorator route
     */
    private fun handleContentFetchResponse(response: Response, isUserSwitching: Boolean): Boolean {
        if (response.isSuccess()) {
            val bodyString = response.readBody()
            val bodyJson = bodyString.toJsonOrNull()

            logger.info(accountId, "Content fetch response received successfully")

            if (bodyString == null || bodyJson == null) {
                // B.E error; should never happen but consider this as success.
                return true
            }

            clevertapResponseHandler?.handleResponse(false, bodyJson, bodyString, isUserSwitching)
            return true
        } else {
            when (response.code) {
                429 -> {
                    logger.info(accountId, "Content fetch request was rate limited (429). Consider reducing request frequency.")
                }

                else -> logger.info(accountId, "Content fetch request failed with response code: ${response.code}")
            }
            return false
        }
    }

    fun cancelAllResponseJobs() {
        parentJob.cancel()
        runBlocking {
            parentJob.children.forEach { it.join() }
        }
        scope.cancel()
        resetScope()
    }

    private fun resetScope() {
        parentJob = SupervisorJob()
        scope = CoroutineScope(
            parentJob + CtDefaultDispatchers().io().limitedParallelism(parallelRequests)
        )
    }
}
