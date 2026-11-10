package com.clevertap.android.sdk.inapp

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class InAppPreviewHandlerTest : BaseTestCase() {

    private lateinit var inAppPreviewHandler: InAppPreviewHandler
    private lateinit var mockCTExecutors: MockCTExecutors

    @MockK(relaxed = true)
    private lateinit var networkManager: NetworkManager

    @MockK(relaxed = true)
    private lateinit var inAppResponse: InAppResponse

    @MockK(relaxed = true)
    private lateinit var context: Context

    private lateinit var testLogger: TestLogger

    @Before
    fun setUpInAppPreviewHandler() {
        MockKAnnotations.init(this)
        testLogger = TestLogger()
        mockCTExecutors = MockCTExecutors()
        inAppPreviewHandler = InAppPreviewHandler(
            mockCTExecutors, networkManager, inAppResponse, context,
            testLogger
        )
        mockkStatic(Utils::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Utils::class)
    }

    // ========== Tests for handleInAppPreview() ==========

    @Test
    fun `handleInAppPreview should process regular inapp payload from bundle`() {
        // Given
        val inappPayload = JSONObject().apply {
            put("key", "value")
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
        }

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    json.has(Constants.INAPP_JSON_RESPONSE_KEY) &&
                            json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY).length() == 1
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should fetch payload from S3 URL when available`() {
        // Given
        val s3Url = "https://s3.amazonaws.com/test/inapp.json"
        val inappPayload = JSONObject().apply {
            put("key", "value")
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_S3_URL_KEY, s3Url)
        }

        every { networkManager.fetchInAppPreviewPayloadFromUrl(s3Url) } returns inappPayload

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    json.has(Constants.INAPP_JSON_RESPONSE_KEY)
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should prefer S3 URL over inline payload`() {
        // Given
        val s3Url = "https://s3.amazonaws.com/test/inapp.json"
        val s3Payload = JSONObject().apply {
            put("source", "s3")
        }
        val inlinePayload = JSONObject().apply {
            put("source", "inline")
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_S3_URL_KEY, s3Url)
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inlinePayload.toString())
        }

        every { networkManager.fetchInAppPreviewPayloadFromUrl(s3Url) } returns s3Payload

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify { networkManager.fetchInAppPreviewPayloadFromUrl(s3Url) }
        verify {
            inAppResponse.processResponse(
                match { json ->
                    val inappNotifs = json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)
                    inappNotifs.getJSONObject(0).getString("source") == "s3"
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should fallback to inline payload when S3 fetch fails`() {
        // Given
        val s3Url = "https://s3.amazonaws.com/test/inapp.json"
        val inlinePayload = JSONObject().apply {
            put("source", "inline")
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_S3_URL_KEY, s3Url)
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inlinePayload.toString())
        }

        every { networkManager.fetchInAppPreviewPayloadFromUrl(s3Url) } returns null

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    val inappNotifs = json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)
                    inappNotifs.getJSONObject(0).getString("source") == "inline"
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should not process when no payload is available`() {
        // Given
        val extras = Bundle()

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }


    // ========== Tests for image-interstitial type handling ==========

    @Test
    fun `handleInAppPreview should convert image-interstitial type to custom-html`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        val htmlTemplate = "<html>${Constants.INAPP_HTML_SPLIT}</html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    val inappNotifs = json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)
                    val inapp = inappNotifs.getJSONObject(0)
                    inapp.getString(Constants.KEY_TYPE) == Constants.KEY_CUSTOM_HTML &&
                            inapp.getJSONObject(Constants.INAPP_DATA_TAG).has(Constants.INAPP_HTML_TAG)
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should convert advanced-builder type to custom-html`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_ADVANCED_BUILDER_TYPE)
        }

        val htmlTemplate = "<html>${Constants.INAPP_HTML_SPLIT}</html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    val inappNotifs = json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)
                    val inapp = inappNotifs.getJSONObject(0)
                    inapp.getString(Constants.KEY_TYPE) == Constants.KEY_CUSTOM_HTML
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should not convert other types to custom-html`() {
        // Given
        val inappPayload = JSONObject().apply {
            put("key", "value")
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, "some-other-type")
        }

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    val inappNotifs = json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)
                    val inapp = inappNotifs.getJSONObject(0)
                    !inapp.has(Constants.KEY_TYPE) || inapp.getString(Constants.KEY_TYPE) != Constants.KEY_CUSTOM_HTML
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should abort when image-interstitial HTML wrapping fails`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        every { Utils.readAssetFile(context, any()) } returns null

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    @Test
    fun `handleInAppPreview should preserve existing data object when wrapping image-interstitial`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val existingData = JSONObject().apply {
            put("existingKey", "existingValue")
        }
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
            put(Constants.INAPP_DATA_TAG, existingData)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        val htmlTemplate = "<html>${Constants.INAPP_HTML_SPLIT}</html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    val inappNotifs = json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)
                    val inapp = inappNotifs.getJSONObject(0)
                    val dataTag = inapp.getJSONObject(Constants.INAPP_DATA_TAG)
                    dataTag.has("existingKey") &&
                            dataTag.getString("existingKey") == "existingValue" &&
                            dataTag.has(Constants.INAPP_HTML_TAG)
                },
                null,
                context
            )
        }
    }

    @Test
    fun `handleInAppPreview should create data object when missing for image-interstitial`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        val htmlTemplate = "<html>${Constants.INAPP_HTML_SPLIT}</html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify {
            inAppResponse.processResponse(
                match { json ->
                    val inappNotifs = json.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY)
                    val inapp = inappNotifs.getJSONObject(0)
                    inapp.has(Constants.INAPP_DATA_TAG) &&
                            inapp.getJSONObject(Constants.INAPP_DATA_TAG).has(Constants.INAPP_HTML_TAG)
                },
                null,
                context
            )
        }
    }


    @Test
    fun `handleInAppPreview should handle missing imageInterstitialConfig`() {
        // Given
        val inappPayload = JSONObject().apply {
            put("otherKey", "value")
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        val htmlTemplate = "<html>${Constants.INAPP_HTML_SPLIT}</html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then - should abort processing
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    @Test
    fun `handleInAppPreview should handle blank imageInterstitialConfig`() {
        // Given
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, "")
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        val htmlTemplate = "<html>${Constants.INAPP_HTML_SPLIT}</html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then - should abort processing
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    @Test
    fun `handleInAppPreview should handle HTML file read IOException`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } throws IOException("File not found")

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then - should abort processing gracefully
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    @Test
    fun `handleInAppPreview should handle HTML without split token`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        val htmlTemplate = "<html><body>No split token here</body></html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then - should abort processing
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    @Test
    fun `handleInAppPreview should handle HTML with multiple split tokens`() {
        // Given
        val imageInterstitialConfig = """{"key": "value"}"""
        val inappPayload = JSONObject().apply {
            put(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG, imageInterstitialConfig)
        }
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inappPayload.toString())
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, Constants.INAPP_IMAGE_INTERSTITIAL_TYPE)
        }

        val htmlTemplate = "<html>${Constants.INAPP_HTML_SPLIT}<body>${Constants.INAPP_HTML_SPLIT}</body></html>"
        every { Utils.readAssetFile(context, Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME) } returns htmlTemplate

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then - should abort processing (split results in more than 2 parts)
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    // ========== Edge cases and error handling ==========

    @Test
    fun `handleInAppPreview should handle empty bundle`() {
        // Given
        val extras = Bundle()

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    @Test
    fun `handleInAppPreview should handle null values in bundle`() {
        // Given
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, null)
            putString(Constants.INAPP_PREVIEW_S3_URL_KEY, null)
        }

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }

    @Test
    fun `handleInAppPreview should handle blank strings in bundle`() {
        // Given
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, "   ")
            putString(Constants.INAPP_PREVIEW_S3_URL_KEY, "")
        }

        // When
        inAppPreviewHandler.handleInAppPreview(extras)

        // Then
        verify(exactly = 0) { inAppResponse.processResponse(any(), any(), any()) }
    }
}
