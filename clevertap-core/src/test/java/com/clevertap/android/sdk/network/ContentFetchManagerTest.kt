package com.clevertap.android.sdk.network

import TestDispatchers
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.network.api.ContentFetchRequestBody
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.http.Request
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.response.ClevertapResponseHandler
import com.clevertap.android.sdk.utils.configMock
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ContentFetchManagerTest {
    private val testScheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(testScheduler)
    private lateinit var contentFetchManager: ContentFetchManager
    private lateinit var mockConfig: CleverTapInstanceConfig
    private lateinit var mockCoreMetaData: CoreMetaData
    private lateinit var mockQueueHeaderBuilder: QueueHeaderBuilder
    private lateinit var mockCtApiWrapper: CtApiWrapper
    private lateinit var mockCtApi: CtApi
    private lateinit var mockClevertapResponseHandler: ClevertapResponseHandler
    private lateinit var testClock: TestClock

    @Before
    fun setUp() {
        mockConfig = configMock()
        mockCoreMetaData = mockk(relaxed = true)
        mockQueueHeaderBuilder = mockk(relaxed = true)
        mockCtApiWrapper = mockk(relaxed = true)
        mockCtApi = mockk(relaxed = true)
        mockClevertapResponseHandler = mockk(relaxed = true)
        testClock = TestClock(1234567890000L) // Set a fixed time

        every { mockCtApiWrapper.ctApi } returns mockCtApi

        // Setup default core metadata values
        every { mockCoreMetaData.currentSessionId } returns 12345
        every { mockCoreMetaData.isFirstSession } returns false
        every { mockCoreMetaData.lastSessionLength } returns 300
        every { mockCoreMetaData.screenName } returns "HomeScreen"
        CoreMetaData.setActivityCount(1)

        contentFetchManager = ContentFetchManager(
            config = mockConfig,
            coreMetaData = mockCoreMetaData,
            queueHeaderBuilder = mockQueueHeaderBuilder,
            ctApiWrapper = mockCtApiWrapper,
            parallelRequests = 5,
            clock = testClock,
            dispatchers
        )
    }

    @After
    fun tearDown() {
        contentFetchManager.cancelAllResponseJobs()
    }

    @Test
    fun `handleContentFetch should process valid content fetch items successfully`() = testScheduler.run {
        // Arrange
        val contentFetchItems = createValidContentFetchItems()
        val packageName = "com.test.app"
        val mockHeader = createMockHeader()
        val mockResponse = createMockSuccessResponse()
        var capturedRequestBody: ContentFetchRequestBody? = null

        every { mockQueueHeaderBuilder.buildHeader(null) } returns mockHeader
        every { mockCtApi.sendContentFetch(capture(slot<ContentFetchRequestBody>())) } answers {
            capturedRequestBody = firstArg()
            mockResponse
        }

        // Act
        contentFetchManager.clevertapResponseHandler = mockClevertapResponseHandler
        contentFetchManager.handleContentFetch(contentFetchItems, packageName)

        advanceUntilIdle()

        // Assert
        assertNotNull(capturedRequestBody)
        val requestBodyString = capturedRequestBody!!.toString()
        val parsedArray = JSONArray(requestBodyString)

        // Should have header + 2 content fetch events
        assertEquals(3, parsedArray.length())

        // Verify header
        val header = parsedArray.getJSONObject(0)
        assertEquals("__d2", header.getString("g"))
        assertEquals("meta", header.getString("type"))
        assertEquals("test-account-id", header.getString("id"))
        assertEquals(1234567890, header.getInt("ts"))

        // Verify first content fetch event
        val firstContentEvent = parsedArray.getJSONObject(1)
        assertEquals("event", firstContentEvent.getString("type"))
        assertEquals("content_fetch", firstContentEvent.getString("evtName"))
        assertEquals(12345, firstContentEvent.getInt("s"))
        assertEquals(1, firstContentEvent.getInt("pg"))
        assertEquals(1234567890, firstContentEvent.getInt("ep"))
        assertFalse(firstContentEvent.getBoolean("f"))
        assertEquals(300, firstContentEvent.getInt("lsl"))
        assertEquals(packageName, firstContentEvent.getString("pai"))
        assertEquals("HomeScreen", firstContentEvent.getString("n"))

        // Verify first evtData
        val firstEvtData = firstContentEvent.getJSONObject("evtData")
        assertEquals("banner_target_1", firstEvtData.getString("tgtId"))
        assertEquals("banner_key_1", firstEvtData.getString("key"))
        assertTrue(firstEvtData.has("eventProperties"))
        val firstEventProperties = firstEvtData.getJSONObject("eventProperties")
        assertEquals("app_launch", firstEventProperties.getString("source"))

        // Verify second content fetch event
        val secondContentEvent = parsedArray.getJSONObject(2)
        assertEquals("event", secondContentEvent.getString("type"))
        assertEquals("content_fetch", secondContentEvent.getString("evtName"))
        assertEquals(12345, secondContentEvent.getInt("s"))
        assertEquals(1, secondContentEvent.getInt("pg"))
        assertEquals(1234567890, secondContentEvent.getInt("ep"))
        assertFalse(secondContentEvent.getBoolean("f"))
        assertEquals(300, secondContentEvent.getInt("lsl"))
        assertEquals(packageName, secondContentEvent.getString("pai"))
        assertEquals("HomeScreen", secondContentEvent.getString("n"))

        // Verify second evtData
        val secondEvtData = secondContentEvent.getJSONObject("evtData")
        assertEquals("popup_target_1", secondEvtData.getString("tgtId"))
        assertEquals("welcome_popup", secondEvtData.getString("messageKey"))
        assertEquals(1234567890, secondEvtData.getInt("ts"))

        verify { mockQueueHeaderBuilder.buildHeader(null) }
        verify { mockCtApi.sendContentFetch(any()) }
        verify { mockClevertapResponseHandler.handleResponse(false, any(), any(), any()) }
    }

    @Test
    fun `handleContentFetch should not call sendContentFetch when no valid content fetch items`() = testScheduler.run {
        // Arrange
        val emptyContentFetchItems = JSONArray()
        val packageName = "com.test.app"

        // Act
        contentFetchManager.handleContentFetch(emptyContentFetchItems, packageName)

        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { mockQueueHeaderBuilder.buildHeader(any()) }
        verify(exactly = 0) { mockCtApi.sendContentFetch(any()) }
    }


    @Test
    fun `handleContentFetch should not send request when header building fails`() = testScheduler.run {
        // Arrange
        val contentFetchItems = createValidContentFetchItems()
        val packageName = "com.test.app"

        every { mockQueueHeaderBuilder.buildHeader(null) } returns null

        // Act
        contentFetchManager.handleContentFetch(contentFetchItems, packageName)

        advanceUntilIdle()

        // Assert
        verify { mockQueueHeaderBuilder.buildHeader(null) }
        verify(exactly = 0) { mockCtApi.sendContentFetch(any()) }
    }

    @Test
    fun `handleContentFetch should handle API exception gracefully`() = testScheduler.run {
        // Arrange
        val contentFetchItems = createValidContentFetchItems()
        val packageName = "com.test.app"
        val mockHeader = createMockHeader()

        every { mockQueueHeaderBuilder.buildHeader(null) } returns mockHeader
        every { mockCtApi.sendContentFetch(any()) } throws RuntimeException("Network error")

        // Act
        contentFetchManager.handleContentFetch(contentFetchItems, packageName)

        advanceUntilIdle()

        // Assert - Should not crash and should log the error
        verify { mockCtApi.sendContentFetch(any()) }
        verify(exactly = 0) { mockClevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
    }

    @Test
    fun `handleContentFetch should handle successful response with valid JSON body`() = testScheduler.run {
        // Arrange
        val contentFetchItems = createValidContentFetchItems()
        val packageName = "com.test.app"
        val mockHeader = createMockHeader()
        val responseJson = JSONObject().apply {
            put("status", "success")
            put("data", "content_data")
        }
        val mockResponse = createMockResponseWithBody(200, responseJson.toString())

        every { mockQueueHeaderBuilder.buildHeader(null) } returns mockHeader
        every { mockCtApi.sendContentFetch(any()) } returns mockResponse

        // Act
        contentFetchManager.clevertapResponseHandler = mockClevertapResponseHandler
        contentFetchManager.handleContentFetch(contentFetchItems, packageName)

        advanceUntilIdle()

        // Assert
        verify {
            mockClevertapResponseHandler.handleResponse(
                false,
                any(),
                responseJson.toString(),
                any()
            )
        }
    }

    @Test
    fun `handleContentFetch should handle successful response with invalid JSON body`() = testScheduler.run {
        // Arrange
        val contentFetchItems = createValidContentFetchItems()
        val packageName = "com.test.app"
        val mockHeader = createMockHeader()
        val invalidJsonBody = "{invalid json}"
        val mockResponse = createMockResponseWithBody(200, invalidJsonBody)

        every { mockQueueHeaderBuilder.buildHeader(null) } returns mockHeader
        every { mockCtApi.sendContentFetch(any()) } returns mockResponse

        // Act
        contentFetchManager.clevertapResponseHandler = mockClevertapResponseHandler
        contentFetchManager.handleContentFetch(contentFetchItems, packageName)

        advanceUntilIdle()

        verify(exactly = 0) { mockClevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
    }

    @Test
    fun `handleContentFetch should handle 429 rate limit response`() = testScheduler.run {
        // Arrange
        val contentFetchItems = createValidContentFetchItems()
        val packageName = "com.test.app"
        val mockHeader = createMockHeader()
        val mockResponse = createMockResponseWithBody(429, "Rate limited")

        every { mockQueueHeaderBuilder.buildHeader(null) } returns mockHeader
        every { mockCtApi.sendContentFetch(any()) } returns mockResponse

        // Act
        contentFetchManager.handleContentFetch(contentFetchItems, packageName)

        advanceUntilIdle()

        verify { mockCtApi.sendContentFetch(any()) }
        verify(exactly = 0) { mockClevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
    }

    @Test
    fun `handleContentFetch should handle other error response codes`() = testScheduler.run {
        // Arrange
        val contentFetchItems = createValidContentFetchItems()
        val packageName = "com.test.app"
        val mockHeader = createMockHeader()
        val mockResponse = createMockResponseWithBody(500, "Internal server error")

        every { mockQueueHeaderBuilder.buildHeader(null) } returns mockHeader
        every { mockCtApi.sendContentFetch(any()) } returns mockResponse

        // Act
        contentFetchManager.handleContentFetch(contentFetchItems, packageName)

        advanceUntilIdle()

        // Assert
        verify { mockCtApi.sendContentFetch(any()) }
        verify(exactly = 0) { mockClevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
    }

    // Helper methods
    private fun createValidContentFetchItems(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("tgtId", "banner_target_1")
                put("eventProperties", JSONObject().apply {
                    put("source", "app_launch")
                })
                put("key", "banner_key_1")
            })
            put(JSONObject().apply {
                put("tgtId", "popup_target_1")
                put("messageKey", "welcome_popup")
                put("ts", 1234567890)
            })
        }
    }

    private fun createMockHeader(): JSONObject {
        return JSONObject().apply {
            put("g", "__d2")
            put("type", "meta")
            put("id", "test-account-id")
            put("ts", 1234567890)
        }
    }

    private fun createMockSuccessResponse(): Response {
        val mockRequest = mockk<Request>(relaxed = true)
        return Response(
            request = mockRequest,
            code = 200,
            headers = emptyMap(),
            bodyStream = "{}".byteInputStream(),
            closeDelegate = {}
        )
    }

    private fun createMockResponseWithBody(code: Int, body: String): Response {
        val mockRequest = mockk<Request>(relaxed = true)
        return Response(
            request = mockRequest,
            code = code,
            headers = emptyMap(),
            bodyStream = body.byteInputStream(),
            closeDelegate = {}
        )
    }
}
