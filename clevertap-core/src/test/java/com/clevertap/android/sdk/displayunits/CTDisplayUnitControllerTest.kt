package com.clevertap.android.sdk.displayunits

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.displayunits.model.MockCleverTapDisplayUnit
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertNull(ctDisplayUnitController.getAllDisplayUnits())
        assertNull(ctDisplayUnitController.getDisplayUnitForID("12121212"))
    }

    @Test
    fun test_updateDisplayUnits_whenListIsEmpty_cacheIsEmpty() {
        ctDisplayUnitController.updateDisplayUnits(emptyList())
        assertNull(ctDisplayUnitController.getAllDisplayUnits())
    }

    @Test
    fun test_getDisplayUnitForID_whenEmptyOrNullUnitID_returnNullDisplayUnit() {
        assertNull(ctDisplayUnitController.getDisplayUnitForID(null))
        assertNull(ctDisplayUnitController.getDisplayUnitForID(""))
    }

    @Test
    fun test_reset() {
        ctDisplayUnitController.updateDisplayUnits(mockDisplayUnits(1))
        assertNotNull(ctDisplayUnitController.getAllDisplayUnits())
        assertTrue(ctDisplayUnitController.getAllDisplayUnits()!!.size == 1)

        ctDisplayUnitController.reset()
        assertNull(ctDisplayUnitController.getAllDisplayUnits())
        assertNull(ctDisplayUnitController.getDisplayUnitForID("mock-notification-id"))
    }

    @Test
    fun test_updateDisplayUnits_validList_populatesCache() {
        ctDisplayUnitController.updateDisplayUnits(mockDisplayUnits(1))
        assertNotNull(ctDisplayUnitController.getAllDisplayUnits())
        assertTrue(ctDisplayUnitController.getAllDisplayUnits()!!.size > 0)
        val displayUnit = ctDisplayUnitController.getDisplayUnitForID("mock-notification-id")
        assertNotNull(displayUnit)
        assertTrue(displayUnit is CleverTapDisplayUnit)
    }

    @Test
    fun test_updateDisplayUnits_replacesPreviousCache() {
        ctDisplayUnitController.updateDisplayUnits(mockDisplayUnits(1))
        assertNotNull(ctDisplayUnitController.getAllDisplayUnits())

        ctDisplayUnitController.updateDisplayUnits(emptyList())
        assertNull(ctDisplayUnitController.getAllDisplayUnits())
    }
}
