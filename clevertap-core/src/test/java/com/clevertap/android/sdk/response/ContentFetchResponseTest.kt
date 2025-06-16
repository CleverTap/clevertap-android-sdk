package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class ContentFetchResponseTest : BaseTestCase() {

    private lateinit var contentFetchResponse: ContentFetchResponse
    private lateinit var mockContentFetchManager: ContentFetchManager
    private lateinit var mockContext: Context

    @Before
    fun setUpContentFetchManager() {
        mockContentFetchManager = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        
        // Setup mock context to return a package name
        every { mockContext.packageName } returns "com.test.package"

        contentFetchResponse = ContentFetchResponse(
            config = cleverTapInstanceConfig,
            contentFetchManager = mockContentFetchManager
        )
    }

    @Test
    fun `processResponse should return early when analytics only is enabled`() {
        // Arrange
        cleverTapInstanceConfig.isAnalyticsOnly = true
        val jsonBody = createValidContentFetchResponse()

        // Act
        contentFetchResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockContentFetchManager.handleContentFetch(any(), any()) }
    }

    @Test
    fun `processResponse should return early when jsonBody is null`() {
        // Act
        contentFetchResponse.processResponse(null, "some string", mockContext)

        verify(exactly = 0) { mockContentFetchManager.handleContentFetch(any(), any()) }
    }

    @Test
    fun `processResponse should return early when content_fetch key is missing`() {
        // Arrange
        val jsonBody = JSONObject().apply {
            put("other_key", "other_value")
        }

        // Act
        contentFetchResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockContentFetchManager.handleContentFetch(any(), any()) }
    }

    @Test
    fun `processResponse should handle empty content fetch array`() {
        // Arrange
        val jsonBody = JSONObject().apply {
            put(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY, JSONArray())
        }

        // Act
        contentFetchResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockContentFetchManager.handleContentFetch(any(), any()) }
    }


    @Test
    fun `processResponse should correct content fetch array`() {
        val jsonBody = createValidContentFetchResponse()
        val contentFetchArray = jsonBody.getJSONArray(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY)

        // Act
        contentFetchResponse.processResponse(jsonBody, null, mockContext)
        
        verify { 
            mockContentFetchManager.handleContentFetch(contentFetchArray, "com.test.package")
        }
    }

    @Test
    fun `processResponse should handle JSON parsing exception gracefully`() {
        // Arrange - Create a JSON object with invalid content_fetch value (not an array)
        val jsonBody = JSONObject().apply {
            put(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY, "invalid_array_value")
        }

        // Act
        contentFetchResponse.processResponse(jsonBody, null, mockContext)

        // Assert
        verify(exactly = 0) { mockContentFetchManager.handleContentFetch(any(), any()) }
    }


    private fun createValidContentFetchResponse(): JSONObject {
        val contentFetchArray = JSONArray().apply {
            put(JSONObject().apply {
                put("tgtId", "content1")
                put("title", "Test Content 1")
            })
            put(JSONObject().apply {
                put("tgtId", "content2")
                put("title", "Test Content 2")
            })
        }
        
        return JSONObject().apply {
            put(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY, contentFetchArray)
        }
    }
}
