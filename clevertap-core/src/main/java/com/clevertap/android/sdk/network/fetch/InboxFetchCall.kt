package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.utils.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

/**
 * HTTP + parse for `POST /inbox/v2/getMessages`.
 *
 * Builds a fresh request body on every call (so post-login GUID is picked up),
 * invokes [CtApi.sendInboxFetch], and maps the outcome to a [CallResult]:
 *  - HTTP 200 with parseable body → [CallResult.Success] carrying the JSON
 *  - HTTP 200 with empty body     → [CallResult.NetworkFailure]
 *  - HTTP 403                     → [CallResult.Disabled]
 *  - other non-2xx                → [CallResult.HttpError]
 *  - any exception during build/send/parse → [CallResult.NetworkFailure]
 *
 * Runs on the injected [dispatcher] (default [Dispatchers.IO]) so a caller on
 * the main thread still moves network work off the UI thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InboxFetchCall(
    private val ctApi: CtApi,
    private val queueHeaderBuilder: QueueHeaderBuilder,
    private val coreMetaData: CoreMetaData,
    private val packageName: String,
    private val logger: Logger,
    private val clock: Clock = Clock.SYSTEM,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : EndpointCall<JSONObject> {

    override suspend fun execute(): CallResult<JSONObject> = withContext(dispatcher) {
        val body = try {
            val header = queueHeaderBuilder.buildHeader(null)
                ?: return@withContext CallResult.NetworkFailure(IOException("header build failed"))
            val event = buildInboxV2Event(
                evtName = Constants.WZRK_FETCH,
                evtData = JSONObject().put("t", Constants.FETCH_TYPE_INBOX_V2),
                coreMetaData = coreMetaData,
                clock = clock,
                packageName = packageName
            )
            EventRequestBody(header, event).toJsonString()
        } catch (e: Exception) {
            return@withContext CallResult.NetworkFailure(e)
        }

        logger.debug("InboxV2", "Send fetch (t=${Constants.FETCH_TYPE_INBOX_V2}): $body")

        try {
            ctApi.sendInboxFetch(body).use { response ->
                when (response.code) {
                    200 -> {
                        val raw = response.readBody()
                            ?: return@use CallResult.NetworkFailure(IOException("empty body"))
                        logger.verbose("InboxV2", "fetch sent successfully (HTTP 200, ${raw.length} bytes)")
                        CallResult.Success(JSONObject(raw))
                    }
                    403 -> {
                        logger.info("InboxV2", "403 — account not enabled")
                        CallResult.Disabled
                    }
                    else -> {
                        logger.info("InboxV2", "fetch failed HTTP ${response.code}")
                        CallResult.HttpError(response.code, response.readBody())
                    }
                }
            }
        } catch (e: Exception) {
            logger.verbose("InboxV2", "fetch failed: $e")
            CallResult.NetworkFailure(e)
        }
    }
}
