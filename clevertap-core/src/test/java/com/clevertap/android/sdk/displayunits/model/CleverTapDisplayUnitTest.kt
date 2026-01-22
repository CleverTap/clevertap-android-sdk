package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import java.util.TreeMap

class CleverTapDisplayUnitTest : BaseTestCase() {

    // Helper function to compare JSONObjects by their string representation
    private fun assertJsonEquals(expected: org.json.JSONObject?, actual: org.json.JSONObject?) {
        Assert.assertEquals(expected?.toString(), actual?.toString())
    }

    @Test
    fun test_toDisplayUnit_nullArray_ReturnInvalidObject() {
        val displayUnit = CleverTapDisplayUnit.toDisplayUnit(null)
        Assert.assertNull(displayUnit.bgColor)
        Assert.assertNull(displayUnit.jsonObject)
        Assert.assertNull(displayUnit.contents)
        Assert.assertNull(displayUnit.customExtras)
        Assert.assertNull(displayUnit.type)
        Assert.assertNull(displayUnit.wzrkFields)
        Assert.assertNotNull(displayUnit.error)
        Assert.assertEquals(displayUnit.unitID, "")
    }

    @Test
    fun test_toDisplayUnit_validArray_ReturnValidObject() {
        val displayUnit = CleverTapDisplayUnit.toDisplayUnit(MockCleverTapDisplayUnit().getAUnit())
        Assert.assertNotNull(displayUnit.bgColor)
        Assert.assertNotNull(displayUnit.jsonObject)
        Assert.assertNotNull(displayUnit.contents)
        Assert.assertTrue(displayUnit.customExtras!!.size > 0)
        Assert.assertNotNull(displayUnit.type)
        Assert.assertNotNull(displayUnit.wzrkFields)
        Assert.assertNull(displayUnit.error)
        Assert.assertNotNull(displayUnit.unitID)
    }

    @Test
    fun test_toString() {
        val displayUnitString = CleverTapDisplayUnit.toDisplayUnit(MockCleverTapDisplayUnit().getAUnit()).toString()
        Assert.assertTrue(displayUnitString.contains("Unit id-"))
        Assert.assertTrue(displayUnitString.contains("Type-"))
        Assert.assertTrue(displayUnitString.contains("bgColor-"))
        Assert.assertTrue(displayUnitString.contains("Content Item:"))
        Assert.assertTrue(displayUnitString.contains("Custom KV:"))
        Assert.assertTrue(displayUnitString.contains("JSON -"))
        Assert.assertTrue(displayUnitString.contains("Error-"))
    }

    @Test
    fun test_createFromParcel_verify() {
        val displayUnit = CleverTapDisplayUnit.toDisplayUnit(MockCleverTapDisplayUnit().getAUnit())
        val parcel = Parcel.obtain()
        displayUnit.writeToParcel(parcel, displayUnit.describeContents())
        parcel.setDataPosition(0)

        val createdFromParcel = CleverTapDisplayUnit.CREATOR.createFromParcel(parcel)
        Assert.assertEquals(displayUnit.unitID, createdFromParcel.unitID)
        Assert.assertEquals(displayUnit.wzrkFields.toString(), createdFromParcel.wzrkFields.toString())
        Assert.assertEquals(displayUnit.error, createdFromParcel.error)
        Assert.assertEquals(displayUnit.type, createdFromParcel.type)
        Assert.assertEquals(
            TreeMap(displayUnit.customExtras).toString(),
            TreeMap(createdFromParcel.customExtras).toString()
        )
        Assert.assertEquals(displayUnit.bgColor, createdFromParcel.bgColor)
        assertJsonEquals(displayUnit.jsonObject, createdFromParcel.jsonObject)
    }
}