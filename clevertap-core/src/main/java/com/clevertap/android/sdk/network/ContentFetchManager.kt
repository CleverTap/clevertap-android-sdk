package com.clevertap.android.sdk.network

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Utils.getNow
import com.clevertap.android.sdk.network.api.ContentFetchRequestBody
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.response.DisplayUnitResponse
import com.clevertap.android.sdk.toJsonOrNull
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
internal class ContentFetchManager(
    config: CleverTapInstanceConfig,
    private val context: Context,
    private val coreMetaData: CoreMetaData,
    private val queueHeaderBuilder: QueueHeaderBuilder,
    private val ctApiWrapper: CtApiWrapper,
    private val displayUnitResponse: DisplayUnitResponse,
    private val deviceIdChangeTimeout: Long = TIMEOUT_DELAY_MS,
    private val parallelRequests: Int = DEFAULT_PARALLEL_REQUESTS
) {
    companion object {
        private const val DEFAULT_PARALLEL_REQUESTS = 5
        private const val TAG = "ContentFetch"
        private const val TIMEOUT_DELAY_MS = 2 * 60L //todo fix this value
    }

    var parentJob = SupervisorJob()

    private var scope = CoroutineScope(
        parentJob + CtDefaultDispatchers().io().limitedParallelism(parallelRequests)
    )
    private val logger = config.logger
    private val accountId = config.accountId

    private val jobs = mutableListOf<Job>()

    fun handleContentFetch(contentFetchItems: JSONArray, packageName: String) {
        val job = scope.launch {
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
        jobs.add(job)
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
            sendContentFetchRequest(payload)
        } catch (e: Exception) {
            logger.verbose(TAG, "Failed to send content fetch request", e)
        }
    }

    @WorkerThread
    private suspend fun sendContentFetchRequest(content: JSONArray): Boolean {
        val header = queueHeaderBuilder.buildHeader(null) ?: return false
        val body = ContentFetchRequestBody(header, content)
        logger.debug(accountId, "Fetching Content: $body")

        try {
            ctApiWrapper.ctApi.sendContentFetch(body).use { response ->
                currentCoroutineContext().ensureActive()
                return handleContentFetchResponse(response)
            }
        } catch (_: CancellationException) {
            logger.verbose(accountId, "Fetch job was cancelled.")
            return false
        } catch (e: Exception) {
            logger.debug(accountId, "An exception occurred while fetching content.", e)
            return false
        }
    }

    /**
     * Handles the response from content fetch requests
     * Processes through normal ResponseDecorator route
     */
    private fun handleContentFetchResponse(response: Response): Boolean {
        if (response.isSuccess()) {
            val bodyString = response.readBody()
            val bodyJson = bodyString.toJsonOrNull()

            logger.info(accountId, "Content fetch response received successfully")

            displayUnitResponse.processResponse(bodyJson, bodyString, this.context)
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
        jobs.forEach { job ->
            job.cancel()
        }
        runBlocking {
            withTimeoutOrNull(deviceIdChangeTimeout) {
                parentJob.join()
            } ?: run {
                parentJob.cancel(CancellationException("Timeout reached"))
                scope.cancel()
                logger.verbose(TAG, "Cancellation timeout reached, forcing cleanup, resetting entire scope")
            }
        }
        jobs.clear()
        resetScope()
    }

    private fun resetScope() {
        parentJob = SupervisorJob()
        scope = CoroutineScope(
            parentJob + CtDefaultDispatchers().io().limitedParallelism(parallelRequests)
        )
    }
}
