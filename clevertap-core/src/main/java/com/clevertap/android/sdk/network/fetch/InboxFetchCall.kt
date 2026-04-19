package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
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
    private val logger: Logger,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : EndpointCall<JSONObject> {

    override suspend fun execute(): CallResult<JSONObject> = withContext(dispatcher) {
        val body = try {
            val header = queueHeaderBuilder.buildHeader(null)
                ?: return@withContext CallResult.NetworkFailure(IOException("header build failed"))
            val event = JSONObject().apply {
                put("type", "event")
                put("evtName", Constants.WZRK_FETCH)
                put("evtData", JSONObject().put("t", Constants.FETCH_TYPE_INBOX_V2))
            }
            EventRequestBody(header, event).toJsonString()
        } catch (e: Exception) {
            return@withContext CallResult.NetworkFailure(e)
        }

        try {
            ctApi.sendInboxFetch(body).use { response ->
                when (response.code) {
                    200 -> {
                        val raw = response.readBody()
                            ?: return@use CallResult.NetworkFailure(IOException("empty body"))
                        CallResult.Success(JSONObject(raw))
                    }
                    403 -> {
                        logger.info("InboxV2", "403 — account not enabled")
                        CallResult.Disabled
                    }
                    else -> CallResult.HttpError(response.code, response.readBody())
                }
            }
        } catch (e: Exception) {
            logger.verbose("InboxV2", "fetch failed: $e")
            CallResult.NetworkFailure(e)
        }
    }
}
