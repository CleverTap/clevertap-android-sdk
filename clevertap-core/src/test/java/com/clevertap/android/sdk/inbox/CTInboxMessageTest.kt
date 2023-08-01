package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}