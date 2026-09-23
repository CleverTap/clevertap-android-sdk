package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import android.text.TextUtils
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleverTapDisplayUnitContentTest : BaseTestCase() {

    @Test
    fun test_toContent_nullObject_ReturnInvalidObject() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(null)
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.message))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.messageColor))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.media))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.actionUrl))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.contentType))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.icon))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.posterUrl))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.title))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.titleColor))
        Assert.assertTrue(!TextUtils.isEmpty(displayUnitContent.error))
    }

    @Test
    fun test_toDisplayUnit_validArray_ReturnValidObject() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.message))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.messageColor))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.media))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.actionUrl))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.contentType))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.icon))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.posterUrl))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.title))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.titleColor))
        Assert.assertFalse(!TextUtils.isEmpty(displayUnitContent.error))
    }

    @Test
    fun test_toString() {
        val displayUnitContentString =
            CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent()).toString()
        Assert.assertTrue(displayUnitContentString.contains("title:"))
        Assert.assertTrue(displayUnitContentString.contains("titleColor:"))
        Assert.assertTrue(displayUnitContentString.contains("message:"))
        Assert.assertTrue(displayUnitContentString.contains("messageColor:"))
        Assert.assertTrue(displayUnitContentString.contains("media:"))
        Assert.assertTrue(displayUnitContentString.contains("posterUrl:"))
        Assert.assertTrue(displayUnitContentString.contains("actionUrl:"))
        Assert.assertTrue(displayUnitContentString.contains("error:"))
    }

    @Test
    fun test_createFromParcel_verify() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        val parcel = Parcel.obtain()
        displayUnitContent.writeToParcel(parcel, displayUnitContent.describeContents())
        parcel.setDataPosition(0)

        val createdFromParcel = CleverTapDisplayUnitContent.CREATOR.createFromParcel(parcel)
        Assert.assertEquals(displayUnitContent.message, createdFromParcel.message)
        Assert.assertEquals(displayUnitContent.messageColor, createdFromParcel.messageColor)
        Assert.assertEquals(displayUnitContent.media, createdFromParcel.media)
        Assert.assertEquals(displayUnitContent.actionUrl, createdFromParcel.actionUrl)
        Assert.assertEquals(displayUnitContent.contentType, createdFromParcel.contentType)
        Assert.assertEquals(displayUnitContent.icon, createdFromParcel.icon)
        Assert.assertEquals(displayUnitContent.posterUrl, createdFromParcel.posterUrl)
        Assert.assertEquals(displayUnitContent.title, createdFromParcel.title)
        Assert.assertEquals(displayUnitContent.titleColor, createdFromParcel.titleColor)
        Assert.assertEquals(displayUnitContent.error, createdFromParcel.error)
    }

    @Test
    fun test_mediaIsVideo() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        Assert.assertFalse(displayUnitContent.mediaIsVideo())

        displayUnitContent.contentType = "audio"
        Assert.assertFalse(displayUnitContent.mediaIsVideo())

        displayUnitContent.contentType = "videoabc"
        Assert.assertTrue(displayUnitContent.mediaIsVideo())
    }

    @Test
    fun test_mediaIsAudio() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        Assert.assertFalse(displayUnitContent.mediaIsAudio())

        displayUnitContent.contentType = "video"
        Assert.assertFalse(displayUnitContent.mediaIsAudio())

        displayUnitContent.contentType = "audioabc"
        Assert.assertTrue(displayUnitContent.mediaIsAudio())
    }

    @Test
    fun test_mediaIsGIF() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        Assert.assertFalse(displayUnitContent.mediaIsGIF())

        displayUnitContent.contentType = "image"
        Assert.assertFalse(displayUnitContent.mediaIsGIF())

        displayUnitContent.contentType = "image/gif"
        Assert.assertTrue(displayUnitContent.mediaIsGIF())
    }

    @Test
    fun test_mediaIsImage() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        displayUnitContent.contentType = null
        Assert.assertFalse(displayUnitContent.mediaIsImage())

        displayUnitContent.contentType = "image/gif"
        Assert.assertFalse(displayUnitContent.mediaIsImage())

        displayUnitContent.contentType = "image"
        Assert.assertTrue(displayUnitContent.mediaIsImage())
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
        Assert.assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
        Assert.assertEquals("0", content.metaData!!["wzrk_index"])
        Assert.assertEquals("Title1", content.metaData!!["wzrk_c2a"])
    }

    @Test
    fun test_toContent_withAndroidUrl_derivesActionAndData() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(serverMetadata(), "https://www.android.com")
        )
        Assert.assertEquals("url", content.metaData!!["wzrk_action"])
        Assert.assertEquals("https://www.android.com", content.metaData!!["wzrk_data"])
    }

    @Test
    fun test_toContent_withoutAndroidUrl_omitsActionAndData() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(serverMetadata(), null))
        Assert.assertNull(content.metaData!!["wzrk_action"])
        Assert.assertNull(content.metaData!!["wzrk_data"])
        Assert.assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
    }

    @Test
    fun test_toContent_whitespaceOnlyAndroidUrl_omitsActionAndData() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(serverMetadata(), "   "))
        Assert.assertNull(content.metaData!!["wzrk_action"])
        Assert.assertNull(content.metaData!!["wzrk_data"])
    }

    @Test
    fun test_toContent_trimsAndroidUrlBeforeUsingItAsData() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(serverMetadata(), "  https://www.android.com  ")
        )
        Assert.assertEquals("https://www.android.com", content.metaData!!["wzrk_data"])
    }

    /**
     * Pins the merge order: the server block is copied in last, so where the BE sends
     * wzrk_action / wzrk_data its values replace the SDK-derived pair. Reordering the
     * two blocks in parseMetaData must fail this test.
     */
    @Test
    fun test_toContent_serverActionAndData_overrideDerivedPair() {
        val metadata = serverMetadata().put("wzrk_action", "none").put("wzrk_data", "")
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(metadata, "https://www.android.com")
        )
        Assert.assertEquals("none", content.metaData!!["wzrk_action"])
        Assert.assertEquals("", content.metaData!!["wzrk_data"])
    }

    @Test
    fun test_toContent_noMetadata_metaDataIsNull() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(null, "https://www.android.com")
        )
        Assert.assertNull(content.metaData)
    }

    @Test
    fun test_toContent_emptyMetadata_metaDataIsNull() {
        val content = CleverTapDisplayUnitContent.toContent(
            contentJson(JSONObject(), "https://www.android.com")
        )
        Assert.assertNull(content.metaData)
    }

    @Test
    fun test_toContent_metadataWrongType_metaDataIsNull() {
        val json = contentJson(null, null).put("metadata", "not-an-object")
        val content = CleverTapDisplayUnitContent.toContent(json)
        Assert.assertNull(content.metaData)
    }

    /**
     * Android-only guard: a nested value cannot be written to a Parcel, and the unit is
     * Parcelable, so it is dropped rather than crashing the host app on the next parcel.
     */
    @Test
    fun test_toContent_nestedMetadataValue_isSkipped() {
        val metadata = serverMetadata().put("wzrk_nested", JSONObject().put("a", "b"))
        val content = CleverTapDisplayUnitContent.toContent(contentJson(metadata, null))
        Assert.assertNull(content.metaData!!["wzrk_nested"])
        Assert.assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
    }

    /** The getter must hand back a copy, or a caller edits the item's stored attribution. */
    @Test
    fun test_getMetaData_returnsCopy_callerCannotMutateStoredAttribution() {
        val content = CleverTapDisplayUnitContent.toContent(contentJson(serverMetadata(), null))
        content.metaData!!["wzrk_element_id"] = "tampered"
        content.metaData!!["injected"] = "x"

        Assert.assertEquals("1907971814", content.metaData!!["wzrk_element_id"])
        Assert.assertNull(content.metaData!!["injected"])
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
        Assert.assertEquals("1907971814", fromParcel.metaData!!["wzrk_element_id"])
        Assert.assertEquals("0", fromParcel.metaData!!["wzrk_index"])
        Assert.assertEquals("url", fromParcel.metaData!!["wzrk_action"])
        Assert.assertEquals("https://www.android.com", fromParcel.metaData!!["wzrk_data"])
    }
}
