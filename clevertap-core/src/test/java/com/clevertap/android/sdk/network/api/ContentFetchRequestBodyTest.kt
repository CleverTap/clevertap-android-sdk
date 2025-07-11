package com.clevertap.android.sdk.network.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentFetchRequestBodyTest {
    @Test
    fun `toString should format realistic CleverTap payload correctly`() {
        // Arrange
        val header = JSONObject().apply {
            put("g", "__d2")
            put("type", "meta")
            put("af", JSONObject().apply {
                put("os", "Android")
                put("sdk", "5.2.0")
            })
            put("id", "device_123")
            put("ts", 1234567890)
        }
        
        val items = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "event")
                put("evtName", "content_fetch")
                put("evtData", JSONObject().apply {
                    put("tgtId", "banner_target_1")
                    put("eventProperties", JSONObject().apply {
                        put("source", "app_launch")
                    })
                    put("key", "banner_key_1")
                })
            })
        }

        // Act
        val requestBody = ContentFetchRequestBody(header, items)
        val result = requestBody.toString()

        // Assert
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
        assertTrue(result.contains("\"g\":\"__d2\""))
        assertTrue(result.contains("\"type\":\"meta\""))
        assertTrue(result.contains("\"evtName\":\"content_fetch\""))
        assertTrue(result.contains("\"tgtId\":\"banner_target_1\""))
    }

    @Test
    fun `toString should handle empty header and items`() {
        // Arrange
        val header = JSONObject()
        val items = JSONArray()
        val expectedString = "[{},]"

        // Act
        val requestBody = ContentFetchRequestBody(header, items)
        val result = requestBody.toString()

        // Assert
        assertEquals(expectedString, result)
    }

    @Test
    fun `toString should handle multiple content_fetch events correctly`() {
        // Arrange
        val header = createRealisticHeader()
        val items = JSONArray().apply {
            // First content_fetch event
            put(JSONObject().apply {
                put("type", "event")
                put("evtName", "content_fetch")
                put("evtData", JSONObject().apply {
                    put("tgtId", "banner_1")
                    put("eventProperties", JSONObject().apply {
                        put("category", "promotional")
                    })
                    put("key", "promo_banner")
                    put("notificationClickedId", "notif_123")
                })
            })
            
            // Second content_fetch event
            put(JSONObject().apply {
                put("type", "event")
                put("evtName", "content_fetch")
                put("evtData", JSONObject().apply {
                    put("tgtId", "popup_1")
                    put("messageKey", "welcome_popup")
                    put("ts", 1234567890)
                    put("batchId", "batch_456")
                    put("pushId", "push_789")
                    put("personalizations", JSONObject().apply {
                        put("name", "John Doe")
                        put("city", "New York")
                    })
                })
            })
        }

        // Act
        val requestBody = ContentFetchRequestBody(header, items)
        val result = requestBody.toString()

        // Assert
        assertTrue(result.contains("\"tgtId\":\"banner_1\""))
        assertTrue(result.contains("\"tgtId\":\"popup_1\""))
        assertTrue(result.contains("\"messageKey\":\"welcome_popup\""))
        assertTrue(result.contains("\"batchId\":\"batch_456\""))
        assertTrue(result.contains("\"pushId\":\"push_789\""))
    }

    @Test
    fun `toString should produce valid JSON that can be parsed back`() {
        // Arrange
        val header = createRealisticHeader()
        val items = createRealisticContentFetchItems()

        // Act
        val requestBody = ContentFetchRequestBody(header, items)
        val result = requestBody.toString()

        // Assert - Should be able to parse back to JSONArray
        val parsedArray = JSONArray(result)
        assertEquals(2, parsedArray.length()) // Header + 1 item
        
        // First element should be the header
        val parsedHeader = parsedArray.getJSONObject(0)
        assertEquals("meta", parsedHeader.getString("type"))
        assertEquals("__d2", parsedHeader.getString("g"))
        
        // Second element should be from items
        val parsedItem = parsedArray.getJSONObject(1)
        assertEquals("event", parsedItem.getString("type"))
        assertEquals("content_fetch", parsedItem.getString("evtName"))
        
        val evtData = parsedItem.getJSONObject("evtData")
        assertEquals("banner_target_123", evtData.getString("tgtId"))
    }

    @Test
    fun `toString should format correctly with complete CleverTap payload structure`() {
        // Arrange - Create a complete realistic payload
        val header = JSONObject().apply {
            put("g", "__d2")
            put("type", "meta")
            put("af", JSONObject().apply {
                put("os", "Android")
                put("osVersion", "11")
                put("sdk", "5.2.0")
                put("make", "Samsung")
                put("model", "Galaxy S21")
            })
            put("id", "device_abc123")
            put("s", 1234567890) // session id
            put("pg", 1) // page number
        }
        
        val items = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "event")
                put("evtName", "content_fetch")
                put("ep", 1234567890) // event timestamp
                put("evtData", JSONObject().apply {
                    put("tgtId", "inbox_message_1")
                    put("messageKey", "weekly_newsletter")
                    put("ts", 1234567890)
                    put("batchId", "batch_weekly_001")
                    put("pushId", "push_newsletter_123")
                    put("personalizations", JSONObject().apply {
                        put("firstName", "Jane")
                        put("lastName", "Smith")
                        put("email", "jane.smith@example.com")
                        put("preferences", JSONArray().apply {
                            put("tech_news")
                            put("product_updates")
                        })
                    })
                    put("eventProperties", JSONObject().apply {
                        put("source", "inbox_refresh")
                        put("category", "newsletter")
                        put("priority", "high")
                    })
                })
            })
        }

        // Act
        val requestBody = ContentFetchRequestBody(header, items)
        val result = requestBody.toString()

        // Assert
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
        
        // Verify header content
        assertTrue(result.contains("\"g\":\"__d2\""))
        assertTrue(result.contains("\"type\":\"meta\""))
        assertTrue(result.contains("\"os\":\"Android\""))
        assertTrue(result.contains("\"model\":\"Galaxy S21\""))
        
        // Verify event content
        assertTrue(result.contains("\"evtName\":\"content_fetch\""))
        assertTrue(result.contains("\"tgtId\":\"inbox_message_1\""))
        assertTrue(result.contains("\"messageKey\":\"weekly_newsletter\""))
        assertTrue(result.contains("\"firstName\":\"Jane\""))
        assertTrue(result.contains("\"source\":\"inbox_refresh\""))
        
        // Verify it's still valid JSON
        val parsedArray = JSONArray(result)
        assertEquals(2, parsedArray.length())
    }

    private fun createRealisticHeader(): JSONObject {
        return JSONObject().apply {
            put("g", "__d2")
            put("type", "meta")
            put("af", JSONObject().apply {
                put("os", "Android")
                put("sdk", "5.2.0")
                put("make", "Google")
                put("model", "Pixel 6")
            })
            put("id", "device_xyz789")
            put("s", 1234567890)
            put("ts", System.currentTimeMillis() / 1000)
        }
    }

    private fun createRealisticContentFetchItems(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("type", "event")
                put("evtName", "content_fetch")
                put("ep", System.currentTimeMillis() / 1000)
                put("evtData", JSONObject().apply {
                    put("tgtId", "banner_target_123")
                    put("eventProperties", JSONObject().apply {
                        put("source", "home_screen")
                        put("placement", "top_banner")
                    })
                    put("key", "home_banner_key")
                    put("notificationClickedId", "notif_click_456")
                })
            })
        }
    }
}
