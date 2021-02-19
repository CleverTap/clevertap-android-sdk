package com.clevertap.android.sdk.displayunits

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.displayunits.model.MockCleverTapDisplayUnit
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTDisplayUnitControllerTest : BaseTestCase() {

    private lateinit var ctDisplayUnitController: CTDisplayUnitController

    @Before
    override fun setUp() {
        super.setUp()
        ctDisplayUnitController = CTDisplayUnitController()
    }

    @Test
    fun test_updateDisplayUnits_whenResponseArrayIsNull_returnNullDisplayUnit() {
        val list = ctDisplayUnitController.updateDisplayUnits(null)
        Assert.assertNull(list)
        Assert.assertNull(ctDisplayUnitController.allDisplayUnits)
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID("12121212"))
    }

    @Test
    fun test_getDisplayUnitForID_whenEmptyOrNullUnitID_returnNullDisplayUnit() {
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID(null))
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID(""))
    }

    @Test
    fun test_reset() {
        // first we put non empty response to ensure that after reset the values are cleared or not

        val list = ctDisplayUnitController.updateDisplayUnits(MockCleverTapDisplayUnit().getMockResponse(1))
        Assert.assertNotNull(list)
        Assert.assertTrue(list?.size == 1)

        //after reset the units should get cleared
        ctDisplayUnitController.reset()
        Assert.assertNull(ctDisplayUnitController.allDisplayUnits)
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID("12121212"))
    }

    @Test
    fun test_updateDisplayUnits_whenAnyException_returnNullDisplayUnit() {
        mockStatic(CleverTapDisplayUnit::class.java).use {
            `when`(CleverTapDisplayUnit.toDisplayUnit(any(JSONObject::class.java))).thenThrow(
                java.lang.RuntimeException(
                    "Something went wrong"
                )
            )
            ctDisplayUnitController.updateDisplayUnits(MockCleverTapDisplayUnit().getMockResponse(1))
            Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID(null))
            Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID(""))
        }
    }

    @Test
    fun test_updateDisplayUnits_validDisplayUnitResponse_shouldReturnValidDisplayUnit() {
        ctDisplayUnitController.updateDisplayUnits(MockCleverTapDisplayUnit().getMockResponse(1))
        Assert.assertNotNull(ctDisplayUnitController.allDisplayUnits)
        Assert.assertTrue(ctDisplayUnitController.allDisplayUnits!!.size > 0)
        val displayUnit = ctDisplayUnitController.getDisplayUnitForID("mock-notification-id")
        Assert.assertNotNull(displayUnit)
        Assert.assertTrue(displayUnit is CleverTapDisplayUnit)
    }
}