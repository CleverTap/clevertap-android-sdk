package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import java.util.TreeMap
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CleverTapDisplayUnitTest : BaseTestCase() {

    // Helper function to compare JSONObjects by their string representation
    private fun assertJsonEquals(expected: org.json.JSONObject?, actual: org.json.JSONObject?) {
        assertEquals(expected?.toString(), actual?.toString())
    }

    @Test
    fun test_toDisplayUnit_nullArray_ReturnInvalidObject() {
        val displayUnit = CleverTapDisplayUnit.toDisplayUnit(null)
        assertNull(displayUnit.bgColor)
        assertNull(displayUnit.jsonObject)
        assertNull(displayUnit.contents)
        assertNull(displayUnit.customExtras)
        assertNull(displayUnit.type)
        assertNull(displayUnit.wzrkFields)
        assertNotNull(displayUnit.error)
        assertEquals(displayUnit.unitID, "")
    }

    @Test
    fun test_toDisplayUnit_validArray_ReturnValidObject() {
        val displayUnit = CleverTapDisplayUnit.toDisplayUnit(MockCleverTapDisplayUnit().getAUnit())
        assertNotNull(displayUnit.bgColor)
        assertNotNull(displayUnit.jsonObject)
        assertNotNull(displayUnit.contents)
        assertTrue(displayUnit.customExtras!!.size > 0)
        assertNotNull(displayUnit.type)
        assertNotNull(displayUnit.wzrkFields)
        assertNull(displayUnit.error)
        assertNotNull(displayUnit.unitID)
    }

    @Test
    fun test_toString() {
        val displayUnitString = CleverTapDisplayUnit.toDisplayUnit(MockCleverTapDisplayUnit().getAUnit()).toString()
        assertTrue(displayUnitString.contains("Unit id-"))
        assertTrue(displayUnitString.contains("Type-"))
        assertTrue(displayUnitString.contains("bgColor-"))
        assertTrue(displayUnitString.contains("Content Item:"))
        assertTrue(displayUnitString.contains("Custom KV:"))
        assertTrue(displayUnitString.contains("JSON -"))
        assertTrue(displayUnitString.contains("Error-"))
    }

    @Test
    fun test_createFromParcel_verify() {
        val displayUnit = CleverTapDisplayUnit.toDisplayUnit(MockCleverTapDisplayUnit().getAUnit())
        val parcel = Parcel.obtain()
        displayUnit.writeToParcel(parcel, displayUnit.describeContents())
        parcel.setDataPosition(0)

        val createdFromParcel = CleverTapDisplayUnit.CREATOR.createFromParcel(parcel)
        assertEquals(displayUnit.unitID, createdFromParcel.unitID)
        assertEquals(displayUnit.wzrkFields.toString(), createdFromParcel.wzrkFields.toString())
        assertEquals(displayUnit.error, createdFromParcel.error)
        assertEquals(displayUnit.type, createdFromParcel.type)
        assertEquals(TreeMap(displayUnit.customExtras).toString(), TreeMap(createdFromParcel.customExtras).toString())
        assertEquals(displayUnit.bgColor, createdFromParcel.bgColor)
        assertJsonEquals(displayUnit.jsonObject, createdFromParcel.jsonObject)
    }

    // ── getMetaDataForContent ──────────────────────────────────────────────

    private fun carouselJson(): org.json.JSONObject = org.json.JSONObject()
        .put("wzrk_id", "unit_123")
        .put("type", "carousel")
        .put(
            "content",
            org.json.JSONArray()
                .put(
                    org.json.JSONObject()
                        .put("title", org.json.JSONObject().put("text", "Title1"))
                        .put(
                            "action",
                            org.json.JSONObject().put(
                                "url",
                                org.json.JSONObject().put(
                                    "android",
                                    org.json.JSONObject().put("text", "https://www.android.com")
                                )
                            )
                        )
                        .put(
                            "metadata",
                            org.json.JSONObject()
                                .put("wzrk_element_id", "1907971814")
                                .put("wzrk_index", "0")
                        )
                )
                // image-only slide, no metadata
                .put(org.json.JSONObject().put("title", org.json.JSONObject().put("text", "Title2")))
        )

    @Test
    fun test_getMetaDataForContent_returnsThatItemsAttribution() {
        val unit = CleverTapDisplayUnit.toDisplayUnit(carouselJson())
        val metaData = unit.getMetaDataForContent(0)
        assertEquals("1907971814", metaData["wzrk_element_id"])
        assertEquals("0", metaData["wzrk_index"])
        assertEquals("https://www.android.com", metaData["wzrk_data"])
    }

    /** The getter must hand back a copy, or a caller edits the unit's stored attribution. */
    @Test
    fun test_getMetaDataForContent_returnsCopy_callerCannotMutateStoredAttribution() {
        val unit = CleverTapDisplayUnit.toDisplayUnit(carouselJson())
        val props = unit.getMetaDataForContent(0)
        props["wzrk_element_id"] = "tampered"
        props["injected"] = "x"

        assertEquals("1907971814", unit.getMetaDataForContent(0)["wzrk_element_id"])
        assertNull(unit.getMetaDataForContent(0)["injected"])
    }

    @Test
    fun test_getMetaDataForContent_itemWithoutMetadata_returnsEmptyMap() {
        val unit = CleverTapDisplayUnit.toDisplayUnit(carouselJson())
        assertTrue(unit.getMetaDataForContent(1).isEmpty())
    }

    @Test
    fun test_getMetaDataForContent_indexPastEnd_returnsEmptyMap() {
        val unit = CleverTapDisplayUnit.toDisplayUnit(carouselJson())
        assertTrue(unit.getMetaDataForContent(99).isEmpty())
    }

    @Test
    fun test_getMetaDataForContent_negativeIndex_returnsEmptyMap() {
        val unit = CleverTapDisplayUnit.toDisplayUnit(carouselJson())
        assertTrue(unit.getMetaDataForContent(-1).isEmpty())
    }

    @Test
    fun test_getMetaDataForContent_unitWithNoContent_returnsEmptyMap() {
        val unit = CleverTapDisplayUnit.toDisplayUnit(org.json.JSONObject().put("wzrk_id", "x"))
        assertTrue(unit.getMetaDataForContent(0).isEmpty())
    }
}
