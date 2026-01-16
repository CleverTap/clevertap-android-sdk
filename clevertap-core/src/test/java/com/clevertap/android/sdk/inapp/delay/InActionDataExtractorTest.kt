package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_INACTION_DURATION
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InActionDataExtractorTest {

    private lateinit var extractor: InActionDataExtractor

    @Before
    fun setUp() {
        extractor = InActionDataExtractor()
    }

    // ==================== EXTRACT DELAY TESTS ====================

    @Test
    fun `extractDelay returns 0 for in-app without inactionDuration field`() {
        // Arrange
        val inApp = JSONObject().put("id", "test")

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for inactionDuration value 0`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = 0)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns milliseconds for inactionDuration value 1 (minimum valid)`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = 1)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(1000L, result) // 1 second = 1000 ms
    }

    @Test
    fun `extractDelay returns milliseconds for inactionDuration value 1200 (maximum valid)`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = 1200)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(1200000L, result) // 1200 seconds = 1,200,000 ms
    }

    @Test
    fun `extractDelay returns 0 for inactionDuration value 1201 (exceeds maximum)`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = 1201)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for negative inactionDuration value`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = -1)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for very large negative inactionDuration value`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = -1000)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns 0 for very large positive inactionDuration value`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = 100000)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay returns correct milliseconds for mid-range inactionDuration`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = 600)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(600000L, result) // 600 seconds = 600,000 ms
    }

    @Test
    fun `extractDelay returns correct milliseconds for inactionDuration value 10`() {
        // Arrange
        val inApp = createInActionInApp(inactionDuration = 10)

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
    fun `createSuccessResult returns ReadyToFetch with correct targetId and metadata`() {
        // Arrange
        val id = "12345"
        val data = JSONObject()
            .put("id", id)
            .put("title", "Test Title")
            .put("message", "Test Message")

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is InActionResult.ReadyToFetch)
        val success = result as InActionResult.ReadyToFetch
        assertEquals(12345L, success.targetId)
        assertEquals(data, success.metadata)
    }

    @Test
    fun `createSuccessResult preserves all metadata fields`() {
        // Arrange
        val id = "99999"
        val data = JSONObject()
            .put("id", id)
            .put("title", "Title")
            .put("message", "Message")
            .put("priority", 100)
            .put("backgroundColor", "#FFFFFF")
            .put(INAPP_INACTION_DURATION, 30)

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is InActionResult.ReadyToFetch)
        val success = result as InActionResult.ReadyToFetch
        assertEquals("Title", success.metadata.getString("title"))
        assertEquals("Message", success.metadata.getString("message"))
        assertEquals(100, success.metadata.getInt("priority"))
        assertEquals("#FFFFFF", success.metadata.getString("backgroundColor"))
        assertEquals(30, success.metadata.getInt(INAPP_INACTION_DURATION))
    }

    @Test
    fun `createSuccessResult converts string id to Long targetId`() {
        // Arrange
        val id = "1733906268"
        val data = JSONObject().put("someField", "value")

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is InActionResult.ReadyToFetch)
        val success = result as InActionResult.ReadyToFetch
        assertEquals(1733906268L, success.targetId)
    }

    @Test
    fun `createSuccessResult handles empty metadata`() {
        // Arrange
        val id = "54321"
        val data = JSONObject()

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is InActionResult.ReadyToFetch)
        val success = result as InActionResult.ReadyToFetch
        assertEquals(54321L, success.targetId)
        assertEquals(0, success.metadata.length())
    }

    @Test
    fun `createSuccessResult handles large targetId`() {
        // Arrange
        val id = "9223372036854775807" // Long.MAX_VALUE
        val data = JSONObject()

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is InActionResult.ReadyToFetch)
        val success = result as InActionResult.ReadyToFetch
        assertEquals(Long.MAX_VALUE, success.targetId)
    }

    @Test
    fun `createSuccessResult handles zero targetId`() {
        // Arrange
        val id = "0"
        val data = JSONObject()

        // Act
        val result = extractor.createSuccessResult(id, data)

        // Assert
        assertTrue(result is InActionResult.ReadyToFetch)
        val success = result as InActionResult.ReadyToFetch
        assertEquals(0L, success.targetId)
    }

    // ==================== CREATE ERROR RESULT TESTS ====================

    @Test
    fun `createErrorResult returns Error with correct targetId and message`() {
        // Arrange
        val id = "12345"
        val message = "Test error message"

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is InActionResult.Error)
        val error = result as InActionResult.Error
        assertEquals(12345L, error.targetId)
        assertEquals(message, error.message)
    }

    @Test
    fun `createErrorResult contains correct targetId`() {
        // Arrange
        val id = "67890"
        val message = "Another error"

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is InActionResult.Error)
        val error = result as InActionResult.Error
        assertEquals(67890L, error.targetId)
    }

    @Test
    fun `createErrorResult handles empty message`() {
        // Arrange
        val id = "11111"
        val message = ""

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is InActionResult.Error)
        val error = result as InActionResult.Error
        assertEquals("", error.message)
    }

    @Test
    fun `createErrorResult handles zero targetId`() {
        // Arrange
        val id = "0"
        val message = "Error with zero id"

        // Act
        val result = extractor.createErrorResult(id, message)

        // Assert
        assertTrue(result is InActionResult.Error)
        val error = result as InActionResult.Error
        assertEquals(0L, error.targetId)
    }

    // ==================== CREATE DISCARDED RESULT TESTS ====================

    @Test
    fun `createDiscardedResult returns Discarded with correct id`() {
        // Arrange
        val id = "123"

        // Act
        val result = extractor.createDiscardedResult(id)

        // Assert
        assertTrue(result is InActionResult.Discarded)
        val discarded = result as InActionResult.Discarded
        assertEquals(id.toLong(), discarded.targetId)
    }

    @Test
    fun `createDiscardedResult contains expected reason message`() {
        // Arrange
        val id = "123"

        // Act
        val result = extractor.createDiscardedResult(id)

        // Assert
        assertTrue(result is InActionResult.Discarded)
        val discarded = result as InActionResult.Discarded
        assertEquals("Timer expired while app was backgrounded", discarded.reason)
    }

    @Test
    fun `createDiscardedResult handles numeric string id`() {
        // Arrange
        val id = "12345"

        // Act
        val result = extractor.createDiscardedResult(id)

        // Assert
        assertTrue(result is InActionResult.Discarded)
        val discarded = result as InActionResult.Discarded
        assertEquals("12345".toLong(), discarded.targetId)
    }

    // ==================== BOUNDARY VALUE TESTS ====================

    @Test
    fun `extractDelay boundary test - inactionDuration 0 returns 0`() {
        assertEquals(0L, extractor.extractDelay(createInActionInApp(inactionDuration = 0)))
    }

    @Test
    fun `extractDelay boundary test - inactionDuration 1 returns 1000ms`() {
        assertEquals(1000L, extractor.extractDelay(createInActionInApp(inactionDuration = 1)))
    }

    @Test
    fun `extractDelay boundary test - inactionDuration 2 returns 2000ms`() {
        assertEquals(2000L, extractor.extractDelay(createInActionInApp(inactionDuration = 2)))
    }

    @Test
    fun `extractDelay boundary test - inactionDuration 1199 returns 1199000ms`() {
        assertEquals(1199000L, extractor.extractDelay(createInActionInApp(inactionDuration = 1199)))
    }

    @Test
    fun `extractDelay boundary test - inactionDuration 1200 returns 1200000ms`() {
        assertEquals(1200000L, extractor.extractDelay(createInActionInApp(inactionDuration = 1200)))
    }

    @Test
    fun `extractDelay boundary test - inactionDuration 1201 returns 0`() {
        assertEquals(0L, extractor.extractDelay(createInActionInApp(inactionDuration = 1201)))
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `extractDelay ignores delayAfterTrigger field`() {
        // Arrange - in-action extractor should only check inactionDuration
        val inApp = JSONObject()
            .put("id", "test")
            .put("delayAfterTrigger", 100) // Should be ignored

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay with both delayAfterTrigger and inactionDuration uses inactionDuration`() {
        // Arrange
        val inApp = JSONObject()
            .put("id", "test")
            .put("delayAfterTrigger", 100)
            .put(INAPP_INACTION_DURATION, 60)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(60000L, result) // Uses inactionDuration (60 seconds)
    }

    @Test
    fun `extractDelay with string inactionDuration returns 0`() {
        // Arrange - malformed data
        val inApp = JSONObject()
            .put("id", "test")
            .put(INAPP_INACTION_DURATION, "sixty")

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    @Test
    fun `extractDelay with null inactionDuration returns 0`() {
        // Arrange
        val inApp = JSONObject()
            .put("id", "test")
        inApp.put(INAPP_INACTION_DURATION, JSONObject.NULL)

        // Act
        val result = extractor.extractDelay(inApp)

        // Assert
        assertEquals(0L, result)
    }

    // ==================== HELPER METHODS ====================

    private fun createInActionInApp(inactionDuration: Int): JSONObject {
        return JSONObject()
            .put("id", "test_inapp")
            .put(INAPP_INACTION_DURATION, inactionDuration)
    }
}