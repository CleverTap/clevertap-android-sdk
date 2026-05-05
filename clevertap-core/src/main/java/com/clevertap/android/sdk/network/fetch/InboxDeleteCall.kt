package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
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
 * Batch V2 inbox-delete sync — N ids in one HTTP call.
 *
 * Maps HTTP outcomes the same way [InboxFetchCall] does:
 *  - HTTP 200 → [CallResult.Success] (Unit; no payload)
 *  - HTTP 403 → [CallResult.Disabled]
 *  - other non-2xx → [CallResult.HttpError]
 *  - any exception during build/send → [CallResult.NetworkFailure]
 *
 * THREE BACKEND-PENDING LITERALS, all captured in [Companion]:
 *  1. delete URL path — in [CtApi.sendInboxDelete], currently
 *     `"inbox/v2/getMessages"`.
 *  2. [EVT_NAME_MESSAGE_DELETED] — event name string.
 *  3. [EVT_DATA_KEY_MESSAGES] — array container key inside `evtData`.
 *
 * Each flips with a one-line edit once backend confirms.
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
            EventRequestBody(header, buildDeleteEvent(messages)).toJsonString()
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

    private fun buildDeleteEvent(messages: List<CTInboxMessage>): JSONObject {
        val arr = JSONArray()
        for (m in messages) {
            val obj = JSONObject().put("_id", m.messageId)
            m.wzrkParams?.let { params ->
                params.keys().forEach { key -> obj.put(key, params.get(key)) }
            }
            arr.put(obj)
        }
        return buildInboxV2Event(
            evtName = EVT_NAME_MESSAGE_DELETED,
            evtData = JSONObject().put(EVT_DATA_KEY_MESSAGES, arr),
            coreMetaData = coreMetaData,
            clock = clock,
            packageName = packageName
        )
    }

    private companion object {
        const val EVT_NAME_MESSAGE_DELETED = "Message Deleted"
        const val EVT_DATA_KEY_MESSAGES = "messages"
    }
}
