package com.clevertap.android.sdk.inapp

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CTInAppNotificationTest {

    fun `initWithJSON for advanced builder custom-html should initialize correctly for cover`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        // Assert
        assertNotNull(notification)
        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeCoverHTML, notification.inAppType)

        assertFalse(notification.isDarkenScreen)
        assertFalse(notification.isShowClose)
        assertEquals("c", notification.position)
        assertEquals(0, notification.width)
        assertEquals(0, notification.height)
        assertEquals(100, notification.widthPercentage)
        assertEquals(100, notification.heightPercentage)
        assertEquals(-1, notification.maxPerSession)
        assertEquals(-1, notification.aspectRatio)

        assertTrue(notification.isJsEnabled)
        assertEquals(1742969683, notification.id)
        assertEquals("1742969683_20250326", notification.campaignId)

        assertEquals(1743142761, notification.timeToLive)
        assertEquals("some-html-block", notification.html)
    }

    @Test
    fun `initWithJSON half interstitial json should initialize correctly`() {
        // Arrange - Half interstitial inapp
        val jsonObject = JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        // Assert
        assertNotNull(notification)
        assertEquals("half-interstitial", notification.type)
        assertEquals("#ffffff", notification.backgroundColor)
        assertFalse(notification.isTablet)
        assertTrue(notification.isHideCloseButton)
        assertEquals("message from half inter", notification.message)
        assertEquals("#434761", notification.messageColor)
        assertEquals("Hi from half inter", notification.title)
        assertEquals("#434761", notification.titleColor)

        assertNotNull(notification.buttons)
        assertEquals(2, notification.buttons.size)
        assertEquals(2, notification.buttonCount)

        // Check ButtonOne
        val buttonOne = notification.buttons[0]
        assertEquals("ButtonOne", buttonOne.text)
        assertEquals("#000000", buttonOne.textColor)
        assertEquals("#1EB858", buttonOne.backgroundColor)
        assertEquals("#1EB858", buttonOne.borderColor)
        assertEquals("4", buttonOne.borderRadius)
        assertNotNull(buttonOne.action)

        // Check ButtonTwo
        val buttonTwo = notification.buttons[1]
        assertEquals("ButtonTwo", buttonTwo.text)
        assertEquals("#000000", buttonTwo.textColor)
        assertEquals("#1EB858", buttonTwo.backgroundColor)
        assertEquals("#1EB858", buttonTwo.borderColor)
        assertEquals("4", buttonTwo.borderRadius)
        assertNotNull(notification.mediaList)
        assertEquals(1, notification.mediaList.size)
        assertTrue(notification.isPortrait)
        assertFalse(notification.isLandscape)
        assertEquals("1742963524", notification.id)
        assertEquals("1742963524_20250326", notification.campaignId)
        assertEquals(1743136393L, notification.timeToLive)
        assertEquals(-1, notification.totalLifetimeCount)
        assertEquals(-1, notification.totalDailyCount)
        assertEquals(-1, notification.maxPerSession)
        assertEquals(CTInAppType.CTInAppTypeHalfInterstitial, notification.inAppType)
    }

    @Test
    fun `initWithJSON missingFields should handle defaults`() {
        // Arrange
        val type = "garbage"
        val jsonString = """
            {
                "type": $type
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        // Assert
        assertNotNull(notification)
        assertFalse(notification.isLocalInApp)
        assertFalse(notification.fallBackToNotificationSettings())
        assertFalse(notification.isExcludeFromCaps)
        assertEquals(-1, notification.totalLifetimeCount)
        assertEquals(-1, notification.totalDailyCount)
        assertEquals(-1, notification.maxPerSession)
        assertFalse(notification.isTablet)
        assertEquals("#FFFFFF", notification.backgroundColor)
        assertTrue(notification.isPortrait)
        assertFalse(notification.isLandscape)
        assertNull(notification.title)
        assertNull(notification.titleColor)
        assertNull(notification.message)
        assertNull(notification.messageColor)
        assertFalse(notification.isHideCloseButton)
        assertNotNull(notification.mediaList)
        assertEquals(0, notification.mediaList.size)
    }

    @Test
    fun `initWithJSON invalidJson should set error`() {
        // Arrange
        val jsonString = "invalid json"
        try {
            val jsonObject = JSONObject(jsonString)
        } catch (e: Exception) {
            // Act
            val notification = CTInAppNotification().initWithJSON(JSONObject(), true)
            // Assert
            assertNotNull(notification)
            assertNotNull(notification.error)
        }
    }

    @Test
    fun `initWithJSON customHtmlType should call legacy configure`() {
        // Arrange
        val jsonString = """
            {
                "type": "custom_html"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        // Assert
        assertNotNull(notification)
        assertEquals("custom_html", notification.type)
    }
}