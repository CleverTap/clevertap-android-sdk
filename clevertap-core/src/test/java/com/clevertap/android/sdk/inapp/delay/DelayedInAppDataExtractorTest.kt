package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DelayedInAppDataExtractorTest{

    private lateinit var extractor: DelayedInAppDataExtractor

    @Before
    fun setUp() {
        extractor = DelayedInAppDataExtractor()
    }

    // ==================== EXTRACT DELAY TESTS ====================

    @Test
    fun `extractDelay returns 0 for in-app without delay field`() {
        // Arrange
        val inApp = JSONObject().put("id", "test")

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for delay value 0`() {
        // Arrange
        val inApp = createDelayedInApp(delay = 0)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns milliseconds for delay value 1 (minimum valid)`() {
        // Arrange
        val inApp = createDelayedInApp(delay = 1)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(1000L, result) // 1 second = 1000 ms
    }

    @Test
    fun `extractDelay returns milliseconds for delay value 1200 (maximum valid)`() {
        // Arrange
        val inApp = createDelayedInApp(delay = 1200)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(1200000L, result) // 1200 seconds = 1,200,000 ms
    }

    @Test
    fun `extractDelay returns 0 for delay value 1201 (exceeds maximum)`() {
        // Arrange
        val inApp = createDelayedInApp(delay = 1201)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for negative delay value`() {
        // Arrange
        val inApp = createDelayedInApp(delay = -1)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for very large negative delay value`() {
        // Arrange
        val inApp = createDelayedInApp(delay = -1000)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for very large positive delay value`() {
        // Arrange
        val inApp = createDelayedInApp(delay = 100000)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns correct milliseconds for mid-range delay`() {
        // Arrange
        val inApp = createDelayedInApp(delay = 600)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(600000L, result) // 600 seconds = 600,000 ms
    }

    @Test
    fun `extractDelay returns correct milliseconds for delay value 10`() {
        // Arrange
        val inApp = createDelayedInApp(delay = 10)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(10000L, result) // 10 seconds = 10,000 ms
    }

    @Test
    fun `extractDelay handles empty JSONObject`() {
        // Arrange
        val inApp = JSONObject()

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    // ==================== CREATE SUCCESS RESULT TESTS ====================

    @Test
    fun `createSuccessResult returns Success with correct id and data`() {
        // Arrange
        val id = "test_inapp_id"
        val data = JSONObject()
            .put("id", id)
            .put("title", "Test Title")
            .put("message", "Test Message")

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is DelayedInAppResult.Success)
        val success = result as DelayedInAppResult.Success
        assertEquals(id, success.inAppId)
        assertEquals(data, success.inApp)
    }

    @Test
    fun `createSuccessResult preserves all data fields`() {
        // Arrange
        val id = "complex_inapp"
        val data = JSONObject()
            .put("id", id)
            .put("title", "Title")
            .put("message", "Message")
            .put("priority", 100)
            .put("backgroundColor", "#FFFFFF")
            .put(INAPP_DELAY_AFTER_TRIGGER, 30)

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is DelayedInAppResult.Success)
        val success = result as DelayedInAppResult.Success
        assertEquals("Title", success.inApp.getString("title"))
        assertEquals("Message", success.inApp.getString("message"))
        assertEquals(100, success.inApp.getInt("priority"))
        assertEquals("#FFFFFF", success.inApp.getString("backgroundColor"))
        assertEquals(30, success.inApp.getInt(INAPP_DELAY_AFTER_TRIGGER))
    }

    @Test
    fun `createSuccessResult handles empty id`() {
        // Arrange
        val id = ""
        val data = JSONObject().put("someField", "value")

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is DelayedInAppResult.Success)
        val success = result as DelayedInAppResult.Success
        assertEquals("", success.inAppId)
    }

    @Test
    fun `createSuccessResult handles empty data`() {
        // Arrange
        val id = "test_id"
        val data = JSONObject()

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is DelayedInAppResult.Success)
        val success = result as DelayedInAppResult.Success
        assertEquals(id, success.inAppId)
        assertEquals(0, success.inApp.length())
    }

    // ==================== CREATE ERROR RESULT TESTS ====================

    @Test
    fun `createErrorResult returns Error with UNKNOWN reason`() {
        // Arrange
        val id = "error_inapp_id"
        val message = "Test error message"

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is DelayedInAppResult.Error)
        val error = result as DelayedInAppResult.Error
        assertEquals(DelayedInAppResult.Error.ErrorReason.UNKNOWN, error.reason)
    }

    @Test
    fun `createErrorResult contains id`() {
        // Arrange
        val id = "error_inapp_id"
        val message = "Test error message"

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is DelayedInAppResult.Error)
        val error = result as DelayedInAppResult.Error
        assertEquals(id, error.inAppId)
    }

    @Test
    fun `createErrorResult contains exception with message`() {
        // Arrange
        val id = "error_inapp_id"
        val message = "Specific error occurred"

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is DelayedInAppResult.Error)
        val error = result as DelayedInAppResult.Error
        assertNotNull(error.throwable)
        assertEquals(message, error.throwable?.message)
    }

    @Test
    fun `createErrorResult handles empty id`() {
        // Arrange
        val id = ""
        val message = "Error with empty id"

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is DelayedInAppResult.Error)
        val error = result as DelayedInAppResult.Error
        assertEquals("", error.inAppId)
    }

    @Test
    fun `createErrorResult handles empty message`() {
        // Arrange
        val id = "test_id"
        val message = ""

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is DelayedInAppResult.Error)
        val error = result as DelayedInAppResult.Error
        assertEquals("", error.throwable?.message)
    }

    // ==================== CREATE DISCARDED RESULT TESTS ====================

    @Test
    fun `createDiscardedResult returns Discarded with correct id`() {
        // Arrange
        val id = "discarded_inapp_id"

        // Act
        val result = extractor.createDiscardedResult(id)

        // Assert
        assertTrue(result is DelayedInAppResult.Discarded)
        val discarded = result as DelayedInAppResult.Discarded
        assertEquals(id, discarded.id)
    }

    @Test
    fun `createDiscardedResult contains expected reason message`() {
        // Arrange
        val id = "test_id"

        // Act
        val result = extractor.createDiscardedResult(id)

        // Assert
        assertTrue(result is DelayedInAppResult.Discarded)
        val discarded = result as DelayedInAppResult.Discarded
        assertEquals("Timer expired while app was backgrounded", discarded.reason)
    }

    @Test
    fun `createDiscardedResult handles empty id`() {
        // Arrange
        val id = ""

        // Act
        val result = extractor.createDiscardedResult(id)

        // Assert
        assertTrue(result is DelayedInAppResult.Discarded)
        val discarded = result as DelayedInAppResult.Discarded
        assertEquals("", discarded.id)
    }

    // ==================== BOUNDARY VALUE TESTS ====================

    @Test
    fun `extractDelay boundary test - delay 0 returns 0`() {
        assertEquals(0L, extractor.extractDelay(createDelayedInApp(delay = 0)))
    }

    @Test
    fun `extractDelay boundary test - delay 1 returns 1000ms`() {
        assertEquals(1000L, extractor.extractDelay(createDelayedInApp(delay = 1)))
    }

    @Test
    fun `extractDelay boundary test - delay 2 returns 2000ms`() {
        assertEquals(2000L, extractor.extractDelay(createDelayedInApp(delay = 2)))
    }

    @Test
    fun `extractDelay boundary test - delay 1199 returns 1199000ms`() {
        assertEquals(1199000L, extractor.extractDelay(createDelayedInApp(delay = 1199)))
    }

    @Test
    fun `extractDelay boundary test - delay 1200 returns 1200000ms`() {
        assertEquals(1200000L, extractor.extractDelay(createDelayedInApp(delay = 1200)))
    }

    @Test
    fun `extractDelay boundary test - delay 1201 returns 0`() {
        assertEquals(0L, extractor.extractDelay(createDelayedInApp(delay = 1201)))
    }

    // ==================== HELPER METHODS ====================

    private fun createDelayedInApp(delay: Int): JSONObject {
        return JSONObject()
            .put("id", "test_inapp")
            .put(INAPP_DELAY_AFTER_TRIGGER, delay)
    }
}