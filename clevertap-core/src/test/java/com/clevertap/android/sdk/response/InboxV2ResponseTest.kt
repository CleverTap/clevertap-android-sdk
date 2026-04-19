package com.clevertap.android.sdk.response

import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.inbox.CTMessageDAO
import io.mockk.Ordering
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class InboxV2ResponseTest {

    private val config = mockk<CleverTapInstanceConfig>(relaxed = true)
    private val ctLockManager = mockk<CTLockManager>(relaxed = true)
    private val callbackManager = mockk<BaseCallbackManager>(relaxed = true)
    private val controllerManager = mockk<ControllerManager>(relaxed = true)
    private val controller = mockk<CTInboxController>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private lateinit var response: InboxV2Response

    @Before
    fun setUp() {
        every { ctLockManager.inboxControllerLock } returns Any()
        every { config.isAnalyticsOnly } returns false
        every { config.accountId } returns "acct"
        every { controllerManager.ctInboxController } returns controller
        every { controller.userId } returns "u"
        every { controller.processV2Response(any()) } returns true

        response = InboxV2Response(
            config = config,
            ctLockManager = ctLockManager,
            callbackManager = callbackManager,
            controllerManager = controllerManager,
            logger = logger
        )
    }

    private fun validResponseJson(ids: List<String> = listOf("m1")): JSONObject {
        val arr = JSONArray()
        ids.forEach { id ->
            arr.put(
                JSONObject()
                    .put("_id", id)
                    .put("date", 1)
                    .put("wzrk_ttl", 2)
                    .put("msg", JSONObject())
            )
        }
        return JSONObject().put("inbox_notifs_v2", arr)
    }

    @Test
    fun `analyticsOnly returns without touching controller`() {
        every { config.isAnalyticsOnly } returns true

        response.processResponse(validResponseJson())

        verify(exactly = 0) { controllerManager.initializeInbox() }
        verify(exactly = 0) { controller.processV2Response(any()) }
        verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
    }

    @Test
    fun `missing V2 key returns without touching controller`() {
        response.processResponse(JSONObject().put("unrelated_key", 1))

        verify(exactly = 0) { controllerManager.initializeInbox() }
        verify(exactly = 0) { controller.processV2Response(any()) }
    }

    @Test
    fun `null controller triggers initializeInbox then processes`() {
        every { controllerManager.ctInboxController } returnsMany listOf(null, controller)

        response.processResponse(validResponseJson())

        verify(ordering = Ordering.ORDERED) {
            controllerManager.initializeInbox()
            controller.processV2Response(any())
            callbackManager._notifyInboxMessagesDidUpdate()
        }
    }

    @Test
    fun `updated false does not fire inboxMessagesDidUpdate`() {
        every { controller.processV2Response(any()) } returns false

        response.processResponse(validResponseJson())

        verify { controller.processV2Response(any()) }
        verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
    }

    @Test
    fun `malformed response swallows exception without crashing`() {
        val broken = JSONObject().put("inbox_notifs_v2", 42)

        response.processResponse(broken)

        verify(exactly = 0) { controller.processV2Response(any()) }
        verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
    }

    @Test
    fun `parseDaos skips items with missing _id`() {
        val captured = slot<List<CTMessageDAO>>()
        every { controller.processV2Response(capture(captured)) } returns true

        val arr = JSONArray().apply {
            put(
                JSONObject()
                    .put("_id", "m1")
                    .put("date", 1)
                    .put("wzrk_ttl", 2)
                    .put("msg", JSONObject())
            )
            put(JSONObject().put("date", 99))
        }

        response.processResponse(JSONObject().put("inbox_notifs_v2", arr))

        assertEquals(1, captured.captured.size)
        assertEquals("m1", captured.captured.single().id)
    }
}
