package com.clevertap.android.sdk.displayunits

import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class CTDisplayUnitTypeTest {

    @Test
    fun test_type_valid() {
        assertEquals(CTDisplayUnitType.SIMPLE, CTDisplayUnitType.type("simple"))
        assertEquals(CTDisplayUnitType.SIMPLE_WITH_IMAGE, CTDisplayUnitType.type("simple-image"))
        assertEquals(CTDisplayUnitType.CAROUSEL, CTDisplayUnitType.type("carousel"))
        assertEquals(CTDisplayUnitType.CAROUSEL_WITH_IMAGE, CTDisplayUnitType.type("carousel-image"))
        assertEquals(CTDisplayUnitType.MESSAGE_WITH_ICON, CTDisplayUnitType.type("message-icon"))
        assertEquals(CTDisplayUnitType.CUSTOM_KEY_VALUE, CTDisplayUnitType.type("custom-key-value"))
    }

    @Test
    fun test_type_inValid() {
        assertNull(CTDisplayUnitType.type("random"))
        assertNull(CTDisplayUnitType.type(""))
    }

    @Test
    fun test_toString() {
        var type = "simple"
        assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "simple-image"
        assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "carousel"
        assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "carousel-image"
        assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "message-icon"
        assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "custom-key-value"
        assertEquals(type, CTDisplayUnitType.type(type).toString())
    }
}