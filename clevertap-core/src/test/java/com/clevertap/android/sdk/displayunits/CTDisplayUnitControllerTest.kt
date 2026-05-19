package com.clevertap.android.sdk.displayunits

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.displayunits.model.MockCleverTapDisplayUnit
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTDisplayUnitControllerTest : BaseTestCase() {

    private lateinit var ctDisplayUnitController: CTDisplayUnitController

    @Before
    override fun setUp() {
        super.setUp()
        ctDisplayUnitController = CTDisplayUnitController()
    }

    private fun mockDisplayUnits(noItems: Int): ArrayList<CleverTapDisplayUnit> {
        val jsonArray = MockCleverTapDisplayUnit().getMockResponse(noItems)
        val list = ArrayList<CleverTapDisplayUnit>()
        for (i in 0 until jsonArray.length()) {
            list.add(CleverTapDisplayUnit.toDisplayUnit(jsonArray.getJSONObject(i)))
        }
        return list
    }

    @Test
    fun test_updateDisplayUnits_whenListIsNull_cacheIsEmpty() {
        ctDisplayUnitController.updateDisplayUnits(null)
        Assert.assertNull(ctDisplayUnitController.allDisplayUnits)
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID("12121212"))
    }

    @Test
    fun test_updateDisplayUnits_whenListIsEmpty_cacheIsEmpty() {
        ctDisplayUnitController.updateDisplayUnits(emptyList())
        Assert.assertNull(ctDisplayUnitController.allDisplayUnits)
    }

    @Test
    fun test_getDisplayUnitForID_whenEmptyOrNullUnitID_returnNullDisplayUnit() {
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID(null))
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID(""))
    }

    @Test
    fun test_reset() {
        ctDisplayUnitController.updateDisplayUnits(mockDisplayUnits(1))
        Assert.assertNotNull(ctDisplayUnitController.allDisplayUnits)
        Assert.assertTrue(ctDisplayUnitController.allDisplayUnits!!.size == 1)

        ctDisplayUnitController.reset()
        Assert.assertNull(ctDisplayUnitController.allDisplayUnits)
        Assert.assertNull(ctDisplayUnitController.getDisplayUnitForID("mock-notification-id"))
    }

    @Test
    fun test_updateDisplayUnits_validList_populatesCache() {
        ctDisplayUnitController.updateDisplayUnits(mockDisplayUnits(1))
        Assert.assertNotNull(ctDisplayUnitController.allDisplayUnits)
        Assert.assertTrue(ctDisplayUnitController.allDisplayUnits!!.size > 0)
        val displayUnit = ctDisplayUnitController.getDisplayUnitForID("mock-notification-id")
        Assert.assertNotNull(displayUnit)
        Assert.assertTrue(displayUnit is CleverTapDisplayUnit)
    }

    @Test
    fun test_updateDisplayUnits_replacesPreviousCache() {
        ctDisplayUnitController.updateDisplayUnits(mockDisplayUnits(1))
        Assert.assertNotNull(ctDisplayUnitController.allDisplayUnits)

        ctDisplayUnitController.updateDisplayUnits(emptyList())
        Assert.assertNull(ctDisplayUnitController.allDisplayUnits)
    }
}
