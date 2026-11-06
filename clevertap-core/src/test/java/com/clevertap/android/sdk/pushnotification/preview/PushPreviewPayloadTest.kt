package com.clevertap.android.sdk.pushnotification.preview

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PushPreviewPayloadTest : BaseTestCase() {

    @Test
    fun `fromBundle returns null when no preview field is present`() {
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "true")
            putString(Constants.NOTIFICATION_ID_TAG, "0_0")
        }

        assertNull(PushPreviewPayload.fromBundle(extras))
        assertFalse(PushPreviewPayload.hasPreviewFields(extras))
    }

    @Test
    fun `a preview key present with a null value does not count as a preview`() {
        // A wrapper that forwards every key of a push may put wzrk_inapp_type with no value.
        // Such a bundle must take the ordinary click path, not the preview gate.
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "true")
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, null)
        }

        assertFalse(PushPreviewPayload.hasPreviewFields(extras))
        assertNull(PushPreviewPayload.fromBundle(extras))
    }

    @Test
    fun `fromBundle keeps only the five preview fields`() {
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "true")
            putString(Constants.WZRK_ACCT_ID_KEY, "ACC-1")
            putString(Constants.WZRK_COLLAPSE, "1_2")
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, "advanced-builder")
            putString(Constants.INAPP_PREVIEW_S3_URL_KEY, "https://host/inapp-preview-campaign/1/a")
        }

        val payload = PushPreviewPayload.fromBundle(extras)

        assertNotNull(payload)
        assertTrue(PushPreviewPayload.hasPreviewFields(extras))
        val bundle = payload.toBundle()
        assertEquals(2, bundle.size())
        assertEquals("advanced-builder", bundle.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY))
        assertEquals("https://host/inapp-preview-campaign/1/a", bundle.getString(Constants.INAPP_PREVIEW_S3_URL_KEY))
        assertFalse(bundle.containsKey(Constants.WZRK_ACCT_ID_KEY))
        assertFalse(bundle.containsKey(Constants.WZRK_COLLAPSE))
    }

    @Test
    fun `fromBundle stringifies a non-string value instead of dropping it`() {
        // A wrapper library may store the field with the wrong type; getString would return null.
        val extras = Bundle().apply {
            putBoolean(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, true)
        }

        val payload = PushPreviewPayload.fromBundle(extras)

        assertEquals("true", payload?.get(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY))
    }

    @Test
    fun `payload survives a round trip through JSON`() {
        val extras = Bundle().apply {
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, "custom-html")
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, """{"type":"custom-html"}""")
            putString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY, """{"msg":[]}""")
            putString(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY, """{"adUnit":[]}""")
        }
        val original = PushPreviewPayload.fromBundle(extras)!!

        val restored = PushPreviewPayload.fromJson(JSONObject(original.toJson().toString()))!!

        for (key in PushPreviewPayload.KEYS) {
            assertEquals(original[key], restored[key], "field $key must survive the round trip")
        }
        assertEquals(PushPreviewKey.of(original), PushPreviewKey.of(restored))
    }

    @Test
    fun `fromJson returns null for a stored row without preview fields`() {
        assertNull(PushPreviewPayload.fromJson(JSONObject()))
        assertNull(PushPreviewPayload.fromJson(JSONObject("""{"unrelated":1}""")))
    }
}
