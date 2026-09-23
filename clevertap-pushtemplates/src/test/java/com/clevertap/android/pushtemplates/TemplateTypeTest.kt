package com.clevertap.android.pushtemplates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * pt_five_icons and pt_icons are separate templates.
 */
class TemplateTypeTest {

    @Test
    fun `pt_five_icons resolves to FIVE_ICONS`() {
        assertEquals(TemplateType.FIVE_ICONS, TemplateType.fromString("pt_five_icons"))
    }

    @Test
    fun `pt_icons resolves to ICONS`() {
        assertEquals(TemplateType.ICONS, TemplateType.fromString("pt_icons"))
    }

    @Test
    fun `five icons and icons template are different types`() {
        assertNotEquals(TemplateType.fromString("pt_five_icons"), TemplateType.fromString("pt_icons"))
    }

    @Test
    fun `toString returns the pt_id`() {
        assertEquals("pt_five_icons", TemplateType.FIVE_ICONS.toString())
        assertEquals("pt_icons", TemplateType.ICONS.toString())
    }
}
