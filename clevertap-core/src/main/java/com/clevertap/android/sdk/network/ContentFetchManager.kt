package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils.getNow
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
internal class ContentFetchManager(
    config: CleverTapInstanceConfig,
    private val coreMetaData: CoreMetaData,
    private val deviceIdChangeTimeout: Long = TIMEOUT_DELAY_MS,
    private val parallelRequests: Int = DEFAULT_PARALLEL_REQUESTS
) {
    companion object {
        private const val DEFAULT_PARALLEL_REQUESTS = 5
        private const val TAG = "ContentFetch"
        //todo fix this value
        private const val TIMEOUT_DELAY_MS = 2 * 60L
    }

    var parentJob = SupervisorJob()

    private var scope = CoroutineScope(
        parentJob + CtDefaultDispatchers().io().limitedParallelism(parallelRequests)
    )
    private val logger: Logger = config.logger
    private var networkManager: NetworkManager? = null

    fun setNetworkManager(manager: NetworkManager) {
        this.networkManager = manager
    }

    fun handleContentFetch(contentFetchItems: JSONArray, packageName: String) {
        scope.launch {
            try {
                val payload = getContentFetchPayload(contentFetchItems, packageName)
                if (payload.length() > 0) {
                    fetchContent(payload)
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
                    put("evtData", item)
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
            put("type", "event")
            put("evtName", "content_fetch")
            put("s", coreMetaData.currentSessionId)
            put("pg", CoreMetaData.getActivityCount())
            put("ep", getNow())
            put("f", coreMetaData.isFirstSession)
            put("lsl", coreMetaData.lastSessionLength)
            put("pai", packageName)
            coreMetaData.screenName?.let { put("n", it) }
        }
    }

    private suspend fun fetchContent(payload: JSONArray) {
        try {
            networkManager?.sendContentFetchRequest(payload)
                ?: logger.verbose(TAG, "NetworkManager not set, cannot send content fetch request.")
        } catch (e: Exception) {
            logger.verbose(TAG, "Failed to send content fetch request", e)
        }
    }

    fun completePendingContentFetch() = runBlocking {
        logger.verbose(TAG, "Waiting up to ${deviceIdChangeTimeout}ms to cancel running jobs")

        val allJobsCompleted = withTimeoutOrNull(deviceIdChangeTimeout) {
            parentJob.join()
            true
        }

        if (allJobsCompleted != true) {
            logger.verbose(TAG, "Timeout reached. Cancelling remaining jobs...")
            parentJob.cancelAndJoin()
        } else {
            logger.verbose(TAG, "All content fetch jobs completed successfully.")
        }

        resetScope()
    }

    private fun resetScope() {
        parentJob = SupervisorJob()
        scope = CoroutineScope(
            parentJob + CtDefaultDispatchers().io().limitedParallelism(parallelRequests)
        )
    }
}
