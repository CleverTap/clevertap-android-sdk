package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CTInboxMessageTest {

    @Test
    fun testCustomData() {
        // Create a mock JSONObject for customData
        val customDataJson = JSONObject()
        customDataJson.put("key", "key1")
        customDataJson.put("value", JSONObject().put(Constants.KEY_TEXT, "value1"))

        // Create a JSONObject for the main message data
        val messageJson = JSONObject()
        messageJson.put(
            Constants.KEY_MSG,
            JSONObject().put(Constants.KEY_CUSTOM_KV, JSONArray().put(customDataJson))
        )

        // Create the CTInboxMessage instance using the constructor
        val inboxMessage = CTInboxMessage(messageJson)

        // Get the customData from the inboxMessage
        val customData = inboxMessage.getCustomData()

        // Verify the customData contents
        assertNotNull(customData)
        assertTrue(customData.has("key1"))
        assertFalse(customData.has("value"))
        assertEquals("value1", customData.getString("key1"))
    }

    @Test
    fun testCustomDataWithMultipleKV() {
        // Create a mock JSONObject for customData
        val customDataJson = JSONObject()
        customDataJson.put("key", "key1")
        customDataJson.put("value", JSONObject().put(Constants.KEY_TEXT, "value1"))

        val customDataJson1 = JSONObject()
        customDataJson1.put("key", "key2")
        customDataJson1.put("value", JSONObject().put(Constants.KEY_TEXT, "value2"))

        // Create a JSONObject for the main message data
        val messageJson = JSONObject()
        messageJson.put(
            Constants.KEY_MSG,
            JSONObject().put(Constants.KEY_CUSTOM_KV, JSONArray().apply {
                put(customDataJson)
                put(customDataJson1)
            })
        )

        // Create the CTInboxMessage instance using the constructor
        val inboxMessage = CTInboxMessage(messageJson)

        // Get the customData from the inboxMessage
        val customData = inboxMessage.customData

        // Verify the customData contents
        assertNotNull(customData)
        assertTrue(customData.has("key1"))
        assertTrue(customData.has("key2"))
        assertFalse(customData.has("value"))
        assertEquals("value1", customData.getString("key1"))
        assertEquals("value2", customData.getString("key2"))
    }

    @Test
    fun testCustomData_whenEmpty_returnEmptyJson() {
        // Create a mock JSONObject for customData
        val customDataJson = JSONObject()

        // Create a JSONObject for the main message data
        val messageJson = JSONObject()
        messageJson.put(
            Constants.KEY_MSG,
            JSONObject().put(Constants.KEY_CUSTOM_KV, JSONArray().put(customDataJson))
        )

        // Create the CTInboxMessage instance using the constructor
        val inboxMessage = CTInboxMessage(messageJson)

        // Get the customData from the inboxMessage
        val customData = inboxMessage.getCustomData()

        // Verify the customData contents
        assertNotNull(customData)
        assertEquals(0, customData.length())
    }

    @Test
    fun testCustomData_whenKVAbsent_returnEmptyJson() {
        // Create a mock JSONObject for customData
        val customDataJson = JSONObject()

        // Create a JSONObject for the main message data
        val messageJson = JSONObject()
        messageJson.put(Constants.KEY_MSG, JSONObject())

        // Create the CTInboxMessage instance using the constructor
        val inboxMessage = CTInboxMessage(messageJson)

        // Get the customData from the inboxMessage
        val customData = inboxMessage.getCustomData()

        // Verify the customData contents
        assertNotNull(customData)
        assertEquals(0, customData.length())
    }

    @Test
    fun `getters are reachable as both Java-style calls and Kotlin properties`() {
        val json = JSONObject()
            .put(Constants.KEY_ID, "msg-1")
            .put(Constants.KEY_IS_READ, true)
            .put(
                Constants.KEY_MSG, JSONObject()
                    .put(Constants.KEY_TYPE, "simple")
                    .put(Constants.KEY_BG, "#FFFFFF")
                    .put(
                        Constants.KEY_CONTENT, JSONArray().put(
                            JSONObject().put(
                                Constants.KEY_TITLE,
                                JSONObject().put(Constants.KEY_TEXT, "hello")
                            )
                        )
                    )
            )
        val message = CTInboxMessage(json)

        assertEquals("msg-1", message.getMessageId())
        assertEquals("msg-1", message.messageId)
        assertTrue(message.isRead())
        assertTrue(message.isRead)
        assertEquals(CTInboxMessageType.SimpleMessage, message.getType())
        assertEquals(CTInboxMessageType.SimpleMessage, message.type)

        val content = message.getInboxMessageContents()[0]
        assertEquals(content, message.inboxMessageContents[0])
        assertEquals("hello", content.getTitle())
        assertEquals("hello", content.title)
        content.setPosterUrl("poster")
        assertEquals("poster", content.posterUrl)
        content.posterUrl = "poster2"
        assertEquals("poster2", content.getPosterUrl())
    }
}
