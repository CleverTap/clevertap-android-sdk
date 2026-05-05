package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.utils.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Batch V2 inbox-delete sync — N ids in one HTTP call to `inbox/v2/deleteMessages`.
 *
 * Maps HTTP outcomes the same way [InboxFetchCall] does:
 *  - HTTP 200 → [CallResult.Success] (Unit; no payload)
 *  - HTTP 403 → [CallResult.Disabled]
 *  - other non-2xx → [CallResult.HttpError]
 *  - any exception during build/send → [CallResult.NetworkFailure]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InboxDeleteCall(
    private val ctApi: CtApi,
    private val queueHeaderBuilder: QueueHeaderBuilder,
    private val messages: List<CTInboxMessage>,
    private val coreMetaData: CoreMetaData,
    private val packageName: String,
    private val logger: Logger,
    private val clock: Clock = Clock.SYSTEM,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : EndpointCall<Unit> {

    override suspend fun execute(): CallResult<Unit> = withContext(dispatcher) {
        if (messages.isEmpty()) return@withContext CallResult.Success(Unit)

        val body = try {
            val header = queueHeaderBuilder.buildHeader(null)
                ?: return@withContext CallResult.NetworkFailure(IOException("header build failed"))
            EventRequestBody(header, buildDeletePayload(messages)).toJsonString()
        } catch (e: Exception) {
            return@withContext CallResult.NetworkFailure(e)
        }

        logger.debug("InboxV2", "Send delete (n=${messages.size}): $body")

        try {
            ctApi.sendInboxDelete(body).use { response ->
                when (response.code) {
                    200 -> {
                        logger.verbose("InboxV2", "delete sent successfully (n=${messages.size})")
                        CallResult.Success(Unit)
                    }
                    403 -> {
                        logger.info("InboxV2", "delete 403 — account not enabled")
                        CallResult.Disabled
                    }
                    else -> {
                        logger.info("InboxV2", "delete failed HTTP ${response.code}")
                        CallResult.HttpError(response.code, response.readBody())
                    }
                }
            }
        } catch (e: Exception) {
            logger.verbose("InboxV2", "delete failed: $e")
            CallResult.NetworkFailure(e)
        }
    }

    private fun buildDeletePayload(messages: List<CTInboxMessage>): JSONObject {
        val arr = JSONArray()
        for (m in messages) {
            val obj = JSONObject().put(Constants.WZRK_MID, m.messageId)
            m.wzrkParams?.let { params ->
                params.keys().forEach { key -> obj.put(key, params.get(key)) }
            }
            arr.put(obj)
        }
        val payload = JSONObject()
            .put("type", TYPE_DELETE_MESSAGES)
            .put(KEY_MESSAGES, arr)
        stampEventMetadata(payload, coreMetaData, clock, packageName)
        return payload
    }

    private companion object {
        const val TYPE_DELETE_MESSAGES = "deleteMessages"
        const val KEY_MESSAGES = "messages"
    }
}
