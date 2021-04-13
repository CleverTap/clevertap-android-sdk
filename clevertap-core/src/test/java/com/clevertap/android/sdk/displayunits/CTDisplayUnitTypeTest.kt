package com.clevertap.android.sdk.displayunits

import org.junit.*

internal class CTDisplayUnitTypeTest {

    @Test
    fun test_type_valid() {
        Assert.assertEquals(CTDisplayUnitType.SIMPLE, CTDisplayUnitType.type("simple"))
        Assert.assertEquals(CTDisplayUnitType.SIMPLE_WITH_IMAGE, CTDisplayUnitType.type("simple-image"))
        Assert.assertEquals(CTDisplayUnitType.CAROUSEL, CTDisplayUnitType.type("carousel"))
        Assert.assertEquals(CTDisplayUnitType.CAROUSEL_WITH_IMAGE, CTDisplayUnitType.type("carousel-image"))
        Assert.assertEquals(CTDisplayUnitType.MESSAGE_WITH_ICON, CTDisplayUnitType.type("message-icon"))
        Assert.assertEquals(CTDisplayUnitType.CUSTOM_KEY_VALUE, CTDisplayUnitType.type("custom-key-value"))
    }

    @Test
    fun test_type_inValid() {
        Assert.assertNull(CTDisplayUnitType.type("random"))
        Assert.assertNull(CTDisplayUnitType.type(""))
    }

    @Test
    fun test_toString() {
        var type = "simple"
        Assert.assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "simple-image"
        Assert.assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "carousel"
        Assert.assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "carousel-image"
        Assert.assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "message-icon"
        Assert.assertEquals(type, CTDisplayUnitType.type(type).toString())

        type = "custom-key-value"
        Assert.assertEquals(type, CTDisplayUnitType.type(type).toString())
    }
}