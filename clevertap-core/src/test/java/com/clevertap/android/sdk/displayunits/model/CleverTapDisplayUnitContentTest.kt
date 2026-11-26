package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import android.text.TextUtils
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CleverTapDisplayUnitContentTest : BaseTestCase() {

    @Test
    fun test_toContent_nullObject_ReturnInvalidObject() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(null)
        assertTrue(TextUtils.isEmpty(displayUnitContent.message))
        assertTrue(TextUtils.isEmpty(displayUnitContent.messageColor))
        assertTrue(TextUtils.isEmpty(displayUnitContent.media))
        assertTrue(TextUtils.isEmpty(displayUnitContent.actionUrl))
        assertTrue(TextUtils.isEmpty(displayUnitContent.contentType))
        assertTrue(TextUtils.isEmpty(displayUnitContent.icon))
        assertTrue(TextUtils.isEmpty(displayUnitContent.posterUrl))
        assertTrue(TextUtils.isEmpty(displayUnitContent.title))
        assertTrue(TextUtils.isEmpty(displayUnitContent.titleColor))
        assertTrue(!TextUtils.isEmpty(displayUnitContent.error))
    }

    @Test
    fun test_toDisplayUnit_validArray_ReturnValidObject() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        assertFalse(TextUtils.isEmpty(displayUnitContent.message))
        assertFalse(TextUtils.isEmpty(displayUnitContent.messageColor))
        assertFalse(TextUtils.isEmpty(displayUnitContent.media))
        assertFalse(TextUtils.isEmpty(displayUnitContent.actionUrl))
        assertFalse(TextUtils.isEmpty(displayUnitContent.contentType))
        assertFalse(TextUtils.isEmpty(displayUnitContent.icon))
        assertFalse(TextUtils.isEmpty(displayUnitContent.posterUrl))
        assertFalse(TextUtils.isEmpty(displayUnitContent.title))
        assertFalse(TextUtils.isEmpty(displayUnitContent.titleColor))
        assertFalse(!TextUtils.isEmpty(displayUnitContent.error))
    }

    @Test
    fun test_toString() {
        val displayUnitContentString =
            CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent()).toString()
        assertTrue(displayUnitContentString.contains("title:"))
        assertTrue(displayUnitContentString.contains("titleColor:"))
        assertTrue(displayUnitContentString.contains("message:"))
        assertTrue(displayUnitContentString.contains("messageColor:"))
        assertTrue(displayUnitContentString.contains("media:"))
        assertTrue(displayUnitContentString.contains("posterUrl:"))
        assertTrue(displayUnitContentString.contains("actionUrl:"))
        assertTrue(displayUnitContentString.contains("error:"))
    }

    @Test
    fun test_createFromParcel_verify() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        val parcel = Parcel.obtain()
        displayUnitContent.writeToParcel(parcel, displayUnitContent.describeContents())
        parcel.setDataPosition(0)

        val createdFromParcel = CleverTapDisplayUnitContent.CREATOR.createFromParcel(parcel)
        assertEquals(displayUnitContent.message, createdFromParcel.message)
        assertEquals(displayUnitContent.messageColor, createdFromParcel.messageColor)
        assertEquals(displayUnitContent.media, createdFromParcel.media)
        assertEquals(displayUnitContent.actionUrl, createdFromParcel.actionUrl)
        assertEquals(displayUnitContent.contentType, createdFromParcel.contentType)
        assertEquals(displayUnitContent.icon, createdFromParcel.icon)
        assertEquals(displayUnitContent.posterUrl, createdFromParcel.posterUrl)
        assertEquals(displayUnitContent.title, createdFromParcel.title)
        assertEquals(displayUnitContent.titleColor, createdFromParcel.titleColor)
        assertEquals(displayUnitContent.error, createdFromParcel.error)
    }

    @Test
    fun test_mediaIsVideo() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        assertFalse(displayUnitContent.mediaIsVideo())

        displayUnitContent.contentType = "audio"
        assertFalse(displayUnitContent.mediaIsVideo())

        displayUnitContent.contentType = "videoabc"
        assertTrue(displayUnitContent.mediaIsVideo())
    }

    @Test
    fun test_mediaIsAudio() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        assertFalse(displayUnitContent.mediaIsAudio())

        displayUnitContent.contentType = "video"
        assertFalse(displayUnitContent.mediaIsAudio())

        displayUnitContent.contentType = "audioabc"
        assertTrue(displayUnitContent.mediaIsAudio())
    }

    @Test
    fun test_mediaIsGIF() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        assertFalse(displayUnitContent.mediaIsGIF())

        displayUnitContent.contentType = "image"
        assertFalse(displayUnitContent.mediaIsGIF())

        displayUnitContent.contentType = "image/gif"
        assertTrue(displayUnitContent.mediaIsGIF())
    }

    @Test
    fun test_mediaIsImage() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        assertFalse(displayUnitContent.mediaIsImage())

        displayUnitContent.contentType = "image/gif"
        assertFalse(displayUnitContent.mediaIsImage())

        displayUnitContent.contentType = "image"
        assertTrue(displayUnitContent.mediaIsImage())
    }

    // ── metaData (per-item wzrk_* attribution) ──────────────────────────────

    /** A content[] item as the BE sends it: `metadata` is a sibling of `action`. */
    private fun contentJson(metadata: JSONObject?, androidUrl: String?): JSONObject {
        val json = JSONObject().put("title", JSONObject().put("text", "Title1"))
        if (androidUrl != null) {
            json.put(
                "action",
                JSONObject().put("url", JSONObject().put("android", JSONObject().put("text", androidUrl)))
            )
        }
        if (metadata != null) {
            json.put("metadata", metadata)
        }
        return json
    }

    private fun serverMetadata(): JSONObject = JSONObject()
        .put("wzrk_element_id", "1907971814")
        .put("wzrk_index", "0")
        .put("wzrk_c2a", "Title1")

    @Test
    fun test_toContent_copiesServerMetadataVerbatim() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(serverMetadata(), null))
        assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
        assertEquals("0", content.metaData!!["wzrk_index"])
        assertEquals("Title1", content.metaData!!["wzrk_c2a"])
    }

    @Test
    fun test_toContent_withAndroidUrl_derivesActionAndData() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(serverMetadata(), "https://www.android.com")
        )
        assertEquals("url", content.metaData!!["wzrk_action"])
        assertEquals("https://www.android.com", content.metaData!!["wzrk_data"])
    }

    @Test
    fun test_toContent_withoutAndroidUrl_omitsActionAndData() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(serverMetadata(), null))
        assertNull(content.metaData!!["wzrk_action"])
        assertNull(content.metaData!!["wzrk_data"])
        assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
    }

    @Test
    fun test_toContent_whitespaceOnlyAndroidUrl_omitsActionAndData() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(serverMetadata(), "   "))
        assertNull(content.metaData!!["wzrk_action"])
        assertNull(content.metaData!!["wzrk_data"])
    }

    @Test
    fun test_toContent_trimsAndroidUrlBeforeUsingItAsData() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(serverMetadata(), "  https://www.android.com  ")
        )
        assertEquals("https://www.android.com", content.metaData!!["wzrk_data"])
    }

    /**
     * Pins the merge order: the SDK-derived pair is written last, so it wins over
     * anything the BE sends for wzrk_action / wzrk_data. Only the SDK knows which
     * platform url actually ran, so a BE-sent pair would be the wrong platform's.
     * Reordering the two blocks in parseMetaData must fail this test.
     */
    @Test
    fun test_toContent_derivedActionAndData_overrideServerPair() {
        val metadata = serverMetadata()
            .put("wzrk_action", "none")
            .put("wzrk_data", "myapp://offers/50-ios")
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(metadata, "https://www.android.com")
        )
        assertEquals("url", content.metaData!!["wzrk_action"])
        assertEquals("https://www.android.com", content.metaData!!["wzrk_data"])
        // Identity keys still come from the server untouched.
        assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
    }

    /** With no Android url there is nothing to derive, so whatever the BE sent stays. */
    @Test
    fun test_toContent_noAndroidUrl_keepsServerSentActionPair() {
        val metadata = serverMetadata().put("wzrk_action", "none").put("wzrk_data", "")
        val content = CleverTapDisplayUnitContent.toContent(contentJson(metadata, null))
        assertEquals("none", content.metaData!!["wzrk_action"])
        assertEquals("", content.metaData!!["wzrk_data"])
    }

    /**
     * getString() would coerce this to "12345" and report it as the tapped url.
     * Only a real JSON string counts, matching the iOS isKindOfClass: guard.
     */
    @Test
    fun test_toContent_nonStringAndroidUrl_omitsActionAndData() {
        val json = JSONObject()
            .put("title", JSONObject().put("text", "Title1"))
            .put(
                "action",
                JSONObject().put("url", JSONObject().put("android", JSONObject().put("text", 12345)))
            )
            .put("metadata", serverMetadata())
        val content = CleverTapDisplayUnitContent.toContent(json)
        assertNull(content.metaData!!["wzrk_action"])
        assertNull(content.metaData!!["wzrk_data"])
        assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
        assertEquals("", content.actionUrl)
    }

    /**
     * The realistic version of the case above - the BE sends "text": null when a slide
     * has no url, and getString() would turn that into the literal string "null".
     */
    @Test
    fun test_toContent_nullAndroidUrl_omitsActionAndData() {
        val json = JSONObject()
            .put("title", JSONObject().put("text", "Title1"))
            .put(
                "action",
                JSONObject().put(
                    "url",
                    JSONObject().put("android", JSONObject().put("text", JSONObject.NULL))
                )
            )
            .put("metadata", serverMetadata())
        val content = CleverTapDisplayUnitContent.toContent(json)
        assertNull(content.metaData!!["wzrk_action"])
        assertNull(content.metaData!!["wzrk_data"])
        assertEquals("", content.actionUrl)
    }

    /**
     * The url pair does not depend on BE metadata: an item with an Android url but no
     * `metadata` still carries the SDK-derived action and data, and nothing else. Matches iOS.
     */
    @Test
    fun test_toContent_noMetadataWithAndroidUrl_metaDataHasOnlyDerivedPair() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(null, "https://www.android.com")
        )
        assertEquals(
            hashMapOf<String, Any?>("wzrk_action" to "url", "wzrk_data" to "https://www.android.com"),
            content.metaData
        )
    }

    @Test
    fun test_toContent_emptyMetadataWithAndroidUrl_metaDataHasOnlyDerivedPair() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(JSONObject(), "https://www.android.com")
        )
        assertEquals(
            hashMapOf<String, Any?>("wzrk_action" to "url", "wzrk_data" to "https://www.android.com"),
            content.metaData
        )
    }

    @Test
    fun test_toContent_metadataWrongTypeWithAndroidUrl_metaDataHasOnlyDerivedPair() {
        val json = contentJson(null, "https://www.android.com").put("metadata", "not-an-object")
        val content = CleverTapDisplayUnitContent.toContent(json)
        assertEquals(
            hashMapOf<String, Any?>("wzrk_action" to "url", "wzrk_data" to "https://www.android.com"),
            content.metaData
        )
    }

    /** With neither metadata nor a url there is nothing to attribute, so metaData stays null. */
    @Test
    fun test_toContent_noMetadataNoAndroidUrl_metaDataIsNull() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(null, null))
        assertNull(content.metaData)
    }

    @Test
    fun test_toContent_emptyMetadataNoAndroidUrl_metaDataIsNull() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(JSONObject(), null))
        assertNull(content.metaData)
    }

    @Test
    fun test_toContent_noMetadataWhitespaceOnlyAndroidUrl_metaDataIsNull() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(null, "   "))
        assertNull(content.metaData)
    }

    /** The derived pair alone must still survive parcelling. */
    @Test
    fun test_toContent_noMetadataWithAndroidUrl_survivesParcelling() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(null, "https://www.android.com")
        )
        val parcel = Parcel.obtain()
        content.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val fromParcel = CleverTapDisplayUnitContent.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        assertEquals("url", fromParcel.metaData!!["wzrk_action"])
        assertEquals("https://www.android.com", fromParcel.metaData!!["wzrk_data"])
    }

    @Test
    fun test_toContent_metadataWrongType_metaDataIsNull() {
        val json = contentJson(null, null).put("metadata", "not-an-object")
        val content = CleverTapDisplayUnitContent.toContent(json)
        assertNull(content.metaData)
    }

    /**
     * Android-only guard: a nested value cannot be written to a Parcel, and the unit is
     * Parcelable, so it is dropped rather than crashing the host app on the next parcel.
     */
    @Test
    fun test_toContent_nestedMetadataValue_isSkipped() {
        val metadata = serverMetadata().put("wzrk_nested", JSONObject().put("a", "b"))
        val content = CleverTapDisplayUnitContent.toContent(contentJson(metadata, null))
        assertNull(content.metaData!!["wzrk_nested"])
        assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
    }

    /**
     * An explicit "key": null comes back from opt() as the JSONObject.NULL sentinel, which
     * is not Parcel-writable either, so it must be dropped too.
     */
    @Test
    fun test_toContent_jsonNullMetadataValue_isSkippedAndParcels() {
        val metadata = serverMetadata().put("wzrk_null", JSONObject.NULL)
        val content = CleverTapDisplayUnitContent.toContent(contentJson(metadata, null))
        assertFalse(content.metaData!!.containsKey("wzrk_null"))
        assertEquals("1907971814", content.metaData!!["wzrk_element_id"])

        val parcel = Parcel.obtain()
        content.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val fromParcel = CleverTapDisplayUnitContent.CREATOR.createFromParcel(parcel)
        assertEquals("1907971814", fromParcel.metaData!!["wzrk_element_id"])
    }

    /** The getter must hand back a copy, or a caller edits the item's stored attribution. */
    @Test
    fun test_getMetaData_returnsCopy_callerCannotMutateStoredAttribution() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(serverMetadata(), null))
        content.metaData!!["wzrk_element_id"] = "tampered"
        content.metaData!!["injected"] = "x"

        assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
        assertNull(content.metaData!!["injected"])
    }

    @Test
    fun test_metaData_survivesParcelling() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(serverMetadata(), "https://www.android.com")
        )
        val parcel = Parcel.obtain()
        content.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val fromParcel = CleverTapDisplayUnitContent.CREATOR.createFromParcel(parcel)
        assertEquals("1907971814", fromParcel.metaData!!["wzrk_element_id"])
        assertEquals("0", fromParcel.metaData!!["wzrk_index"])
        assertEquals("url", fromParcel.metaData!!["wzrk_action"])
        assertEquals("https://www.android.com", fromParcel.metaData!!["wzrk_data"])
    }
}
