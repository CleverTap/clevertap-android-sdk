package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CTInboxStyleConfigTest:BaseTestCase() {
    
    private lateinit var config: CTInboxStyleConfig
    override fun setUp() {
        super.setUp()
        
        config = CTInboxStyleConfig()
    }
    @Test
    fun test_getter_setter_BackButtonColor() {
        config.backButtonColor = "#123456"
        assertEquals("#123456",config.backButtonColor)
    }

    @Test
    fun test_getter_setter_firstTabTitle() {
        config.firstTabTitle = "title"
        assertEquals("title",config.firstTabTitle)
    }

    @Test
    fun test_getter_setter_InboxBackgroundColor() {
        config.inboxBackgroundColor = "#123456"
        assertEquals("#123456",config.inboxBackgroundColor)
    }

    @Test
    fun test_getter_setter_NavBarColor() {
        config.navBarColor = "#123456"
        assertEquals("#123456",config.navBarColor)
    }

    @Test
    fun test_getter_setter_NavBarTitle() {
        config.navBarTitle = "title"
        assertEquals("title",config.navBarTitle)
    }

    @Test
    fun test_getter_setter_NavBarTitleColor() {
        config.navBarTitleColor = "#123456"
        assertEquals("#123456",config.navBarTitleColor)
    }

    @Test
    fun test_getter_setter_NoMessageViewText() {
        config.noMessageViewText = "title"
        assertEquals("title",config.noMessageViewText)
    }

    @Test
    fun test_getter_setter_NoMessageViewTextColor() {
        config.noMessageViewTextColor = "#123456"
        assertEquals("#123456",config.noMessageViewTextColor)
    }

    @Test
    fun test_getter_setter_SelectedTabColor() {
        config.selectedTabColor = "#123456"
        assertEquals("#123456",config.selectedTabColor)
    }

    @Test
    fun test_getter_setter_SelectedTabIndicatorColor() {
        config.selectedTabIndicatorColor = "#123456"
        assertEquals("#123456",config.selectedTabIndicatorColor)
    }

    @Test
    fun test_getter_setter_TabBackgroundColor() {
        config.tabBackgroundColor = "#123456"
        assertEquals("#123456",config.tabBackgroundColor)
    }

    @Test
    fun test_getter_setter_Tabs() {
        config.tabs = arrayListOf("title")
        assertEquals(arrayListOf("title"),config.tabs)
    }

    @Test
    fun test_getter_setter_UnselectedTabColor() {
        config.unselectedTabColor = "#123456"
        assertEquals("#123456",config.unselectedTabColor)
    }

    @Test
    fun isUsingTabs() {
        config.tabs = null
        assertFalse(config.isUsingTabs)
        config.tabs = arrayListOf()
        assertFalse(config.isUsingTabs)
        config.tabs = arrayListOf("title")
        assertTrue(config.isUsingTabs)
    }
}