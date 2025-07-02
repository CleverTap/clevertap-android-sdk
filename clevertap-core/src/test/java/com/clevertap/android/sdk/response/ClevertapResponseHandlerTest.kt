package com.clevertap.android.sdk.response

import android.content.Context
import io.mockk.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClevertapResponseHandlerTest {

    private lateinit var mockContext: Context
    private lateinit var mockInAppResponse: InAppResponse
    private lateinit var mockInboxResponse: InboxResponse
    private lateinit var mockDisplayUnitResponse: DisplayUnitResponse
    private lateinit var mockFetchVariablesResponse: FetchVariablesResponse
    private lateinit var mockGenericResponse: CleverTapResponse

    private lateinit var responseHandler: ClevertapResponseHandler
    private lateinit var mockBodyJson: JSONObject
    private val bodyString = "{\"test\": \"data\"}"

    @Before
    fun setUp() {
        mockContext = mockk()
        mockInAppResponse = mockk(relaxed = true)
        mockInboxResponse = mockk(relaxed = true)
        mockDisplayUnitResponse = mockk(relaxed = true)
        mockFetchVariablesResponse = mockk(relaxed = true)
        mockGenericResponse = mockk(relaxed = true)
        mockBodyJson = mockk()
    }

    @Test
    fun `handleResponse with isUserSwitching false processes all responses`() {
        // Given
        val responses = listOf(
            mockInAppResponse,
            mockInboxResponse,
            mockDisplayUnitResponse,
            mockFetchVariablesResponse,
            mockGenericResponse
        )
        responseHandler = ClevertapResponseHandler(mockContext, responses)

        // When
        responseHandler.handleResponse(
            isFullResponse = true,
            bodyJson = mockBodyJson,
            bodyString = bodyString,
            isUserSwitching = false
        )

        // Then
        responses.forEach { response ->
            verify { response.processResponse(mockBodyJson, bodyString, mockContext) }
        }
    }

    @Test
    fun `handleResponse with isUserSwitching true excludes InboxResponse, DisplayUnitResponse, and FetchVariablesResponse`() {
        // Given
        val responses = listOf(
            mockInAppResponse,
            mockInboxResponse,
            mockDisplayUnitResponse,
            mockFetchVariablesResponse,
            mockGenericResponse
        )
        responseHandler = ClevertapResponseHandler(mockContext, responses)

        // When
        responseHandler.handleResponse(
            isFullResponse = false,
            bodyJson = mockBodyJson,
            bodyString = bodyString,
            isUserSwitching = true
        )

        // Then
        // Verify excluded responses are not processed
        verify(exactly = 0) { mockInboxResponse.processResponse(any(), any(), any()) }
        verify(exactly = 0) { mockDisplayUnitResponse.processResponse(any(), any(), any()) }
        verify(exactly = 0) { mockFetchVariablesResponse.processResponse(any(), any(), any()) }

        // Verify included responses are processed
        verify { mockInAppResponse.processResponse(mockBodyJson, bodyString, mockContext, true) }

        verify { mockGenericResponse.processResponse(mockBodyJson, bodyString, mockContext) }
    }

    @Test
    fun `handleResponse with isUserSwitching true and InAppResponse calls processResponse with isUserSwitching true`() {
        // Given
        val responses = listOf(mockInAppResponse)
        responseHandler = ClevertapResponseHandler(mockContext, responses)

        // When
        responseHandler.handleResponse(
            isFullResponse = true,
            bodyJson = mockBodyJson,
            bodyString = bodyString,
            isUserSwitching = true
        )

        // Then
        verify { mockInAppResponse.processResponse(mockBodyJson, bodyString, mockContext, true) }
    }

    @Test
    fun `handleResponse with isUserSwitching true and non-InAppResponse calls processResponse without isUserSwitching true`() {
        // Given
        val responses = listOf(mockGenericResponse)
        responseHandler = ClevertapResponseHandler(mockContext, responses)

        // When
        responseHandler.handleResponse(
            isFullResponse = true,
            bodyJson = mockBodyJson,
            bodyString = bodyString,
            isUserSwitching = true
        )

        // Then
        verify { mockGenericResponse.processResponse(mockBodyJson, bodyString, mockContext) }
    }


    @Test
    fun `handleResponse with mixed response types and isUserSwitching true processes correctly`() {
        // Given
        val mockOtherResponse1 = mockk<CleverTapResponse>(relaxed = true)
        val mockOtherResponse2 = mockk<CleverTapResponse>(relaxed = true)
        val responses = listOf(
            mockInAppResponse,
            mockInboxResponse,      // Should be excluded
            mockOtherResponse1,
            mockDisplayUnitResponse, // Should be excluded
            mockOtherResponse2,
            mockFetchVariablesResponse // Should be excluded
        )
        responseHandler = ClevertapResponseHandler(mockContext, responses)

        // When
        responseHandler.handleResponse(
            isFullResponse = false,
            bodyJson = mockBodyJson,
            bodyString = bodyString,
            isUserSwitching = true
        )

        // Then
        // Verify excluded responses
        verify(exactly = 0) { mockInboxResponse.processResponse(any(), any(), any()) }
        verify(exactly = 0) { mockDisplayUnitResponse.processResponse(any(), any(), any()) }
        verify(exactly = 0) { mockFetchVariablesResponse.processResponse(any(), any(), any()) }

        // Verify included responses
        verify { mockInAppResponse.processResponse(mockBodyJson, bodyString, mockContext, true) }

        verify { mockOtherResponse1.processResponse(mockBodyJson, bodyString, mockContext) }

        verify { mockOtherResponse2.processResponse(mockBodyJson, bodyString, mockContext) }
    }


    @Test
    fun `handleResponse with only excluded response types and isUserSwitching true processes nothing`() {
        // Given
        val responses = listOf(
            mockInboxResponse,
            mockDisplayUnitResponse,
            mockFetchVariablesResponse
        )
        responseHandler = ClevertapResponseHandler(mockContext, responses)

        // When
        responseHandler.handleResponse(
            isFullResponse = true,
            bodyJson = mockBodyJson,
            bodyString = bodyString,
            isUserSwitching = true
        )

        // Then
        verify(exactly = 0) { mockInboxResponse.processResponse(any(), any(), any()) }
        verify(exactly = 0) { mockDisplayUnitResponse.processResponse(any(), any(), any()) }
        verify(exactly = 0) { mockFetchVariablesResponse.processResponse(any(), any(), any()) }
    }
}