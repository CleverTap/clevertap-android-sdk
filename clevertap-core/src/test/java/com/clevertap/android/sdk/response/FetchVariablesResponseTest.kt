package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class FetchVariablesResponseTest : BaseTestCase() {

    private lateinit var fetchVariablesResponse: FetchVariablesResponse
    private lateinit var mockControllerManager: ControllerManager
    private lateinit var mockCallbackManager: BaseCallbackManager
    private lateinit var mockContext: Context
    private lateinit var mockCtVariables: CTVariables
    private lateinit var mockFetchVariablesCallback: FetchVariablesCallback

    @Before
    fun setUpFetchVariablesResponse() {
        mockControllerManager = mockk(relaxed = true)
        mockCallbackManager = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        mockCtVariables = mockk(relaxed = true)
        mockFetchVariablesCallback = mockk(relaxed = true)

        every { mockControllerManager.ctVariables } returns mockCtVariables
        every { mockCallbackManager.fetchVariablesCallback } returns mockFetchVariablesCallback

        fetchVariablesResponse = FetchVariablesResponse(
            cleverTapInstanceConfig,
            mockControllerManager,
            mockCallbackManager
        )
    }

    // ==================== Early Return Tests ====================

    @Test
    fun `processResponse should return early when analytics only is enabled`() {
        cleverTapInstanceConfig.isAnalyticsOnly = true
        val jsonBody = createResponseWithBothKeys()

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockCtVariables.handleVariableResponse(any(), any()) }
        verify(exactly = 0) { mockCtVariables.handleAbVariantsResponse(any()) }
    }

    @Test
    fun `processResponse should return early when response is null`() {
        fetchVariablesResponse.processResponse(null, "stringBody", mockContext)

        verify(exactly = 0) { mockCtVariables.handleVariableResponse(any(), any()) }
        verify(exactly = 0) { mockCtVariables.handleAbVariantsResponse(any()) }
    }

    // ==================== Vars Key Tests ====================

    @Test
    fun `processResponse should handle valid vars response`() {
        val varsJson = JSONObject().put("key", "value")
        val jsonBody = JSONObject().put(Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY, varsJson)

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 1) { mockCtVariables.handleVariableResponse(varsJson, mockFetchVariablesCallback) }
        verify(exactly = 1) { mockCallbackManager.setFetchVariablesCallback(null) }
    }

    @Test
    fun `processResponse should skip vars when key is missing`() {
        val jsonBody = JSONObject().put("other_key", "value")

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockCtVariables.handleVariableResponse(any(), any()) }
    }

    @Test
    fun `processResponse should handle exception when vars value is not JSONObject`() {
        val jsonBody = JSONObject().put(Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY, "invalid_string")

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockCtVariables.handleVariableResponse(any(), any()) }
    }

    // ==================== AbVariantInfo Key Tests ====================

    @Test
    fun `processResponse should handle valid abVariantInfo response`() {
        val abVariantArray = JSONArray().put(JSONObject().put("variantId", "v1"))
        val jsonBody = JSONObject().put(Constants.REQUEST_VARIANTS_JSON_RESPONSE_KEY, abVariantArray)

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 1) { mockCtVariables.handleAbVariantsResponse(abVariantArray) }
    }

    @Test
    fun `processResponse should skip abVariantInfo when key is missing`() {
        val jsonBody = JSONObject().put("other_key", "value")

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockCtVariables.handleAbVariantsResponse(any()) }
    }

    @Test
    fun `processResponse should handle abVariantInfo when value is not JSONArray (invalid data)`() {
        val jsonBody = JSONObject().put(Constants.REQUEST_VARIANTS_JSON_RESPONSE_KEY, "invalid_string")

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        // optJSONArray returns null for non-array values
        verify(exactly = 0) { mockCtVariables.handleAbVariantsResponse(any()) }
    }

    @Test
    fun `processResponse should not handle abVariantInfo when value is null`() {
        val jsonBody = JSONObject()

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        // optJSONArray returns null for non-array values
        verify(exactly = 0) { mockCtVariables.handleAbVariantsResponse(any()) }
    }

    // ==================== Both Keys Present ====================

    @Test
    fun `processResponse should handle both vars and abVariantInfo keys`() {
        val jsonBody = createResponseWithBothKeys()

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 1) { mockCtVariables.handleAbVariantsResponse(any()) }
        verify(exactly = 1) { mockCtVariables.handleVariableResponse(any(), any()) }
    }

    // ==================== CTVariables Null Tests ====================

    @Test
    fun `processResponse should not crash when CTVariables is null for vars`() {
        every { mockControllerManager.ctVariables } returns null
        val jsonBody = JSONObject().put(Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY, JSONObject())

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockCtVariables.handleVariableResponse(any(), any()) }
    }

    @Test
    fun `processResponse should not crash when CTVariables is null for abVariantInfo`() {
        every { mockControllerManager.ctVariables } returns null
        val jsonBody = JSONObject().put(Constants.REQUEST_VARIANTS_JSON_RESPONSE_KEY, JSONArray())

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 0) { mockCtVariables.handleAbVariantsResponse(any()) }
    }

    // ==================== Callback Tests ====================

    @Test
    fun `processResponse should handle null callback gracefully`() {
        every { mockCallbackManager.fetchVariablesCallback } returns null
        val jsonBody = JSONObject().put(Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY, JSONObject())

        fetchVariablesResponse.processResponse(jsonBody, null, mockContext)

        verify(exactly = 1) { mockCtVariables.handleVariableResponse(any(), null) }
    }

    // ==================== Helper Methods ====================

    private fun createResponseWithBothKeys(): JSONObject {
        return JSONObject().apply {
            put(Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY, JSONObject().put("key", "value"))
            put(Constants.REQUEST_VARIANTS_JSON_RESPONSE_KEY, JSONArray().put(JSONObject()))
        }
    }
}
