package com.clevertap.android.sdk

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Compile-time guard: public API types converted from Java must keep accepting both the
 * Java-style accessor calls and Kotlin property syntax that Kotlin callers could use before.
 */
class PublicApiSourceCompatTest {

    @Test
    fun `UTMDetail supports accessor calls and property syntax`() {
        val detail = UTMDetail()
        detail.setSource("function-form")
        assertEquals("function-form", detail.getSource())
        assertEquals("function-form", detail.source)

        detail.source = "property-form"
        assertEquals("property-form", detail.getSource())
        assertEquals("property-form", detail.source)
    }

    @Test
    fun `CTInboxStyleConfig supports accessor calls and property syntax`() {
        val config = CTInboxStyleConfig()
        config.setNavBarTitle("function-form")
        assertEquals("function-form", config.navBarTitle)

        config.navBarTitle = "property-form"
        assertEquals("property-form", config.getNavBarTitle())
        assertEquals(config.isUsingTabs(), config.isUsingTabs)
    }
}
