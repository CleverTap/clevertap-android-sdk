package com.clevertap.android.sdk.inapp

import android.os.Parcel
import io.mockk.every
import io.mockk.mockk
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CTInAppNotificationTest {

    @Test
    fun `initWithJSON for advanced builder custom-html should initialize correctly for html header`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_HEADER)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeHeaderHTML, notification.inAppType)
    }

    @Test
    fun `initWithJSON for advanced builder custom-html should initialize correctly for html footer`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_FOOTER)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeFooterHTML, notification.inAppType)
    }

    @Test
    fun `initWithJSON for advanced builder custom-html should initialize correctly for html header legacy`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_HEADER_LEGACY)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeHeaderHTML, notification.inAppType)
    }

    @Test
    fun `initWithJSON for advanced builder custom-html should initialize correctly for html footer legacy`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_FOOTER_LEGACY)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeFooterHTML, notification.inAppType)
    }

    @Test
    fun `initWithJSON for advanced builder custom-html should initialize correctly for interstitial`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeInterstitialHTML, notification.inAppType)
    }

    @Test
    fun `initWithJSON for advanced builder custom-html should initialize correctly for half interstitial`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_HALF_INTERSTITIAL)

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeHalfInterstitialHTML, notification.inAppType)
    }

    @Test
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
        assertEquals('c', notification.position)
        assertEquals(0, notification.width)
        assertEquals(0, notification.height)
        assertEquals(100, notification.widthPercentage)
        assertEquals(100, notification.heightPercentage)
        assertEquals(-1, notification.maxPerSession)
        assertEquals(-1.0, notification.aspectRatio, Double.MIN_VALUE)

        assertTrue(notification.isJsEnabled)
        assertEquals("1742969683", notification.id)
        assertEquals("1742969683_20250326", notification.campaignId)

        assertEquals(1743142761L, notification.timeToLive)
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
        val inAppNotification = mockk<JSONObject>()
        every { inAppNotification.has(any()) } throws JSONException("some exception")
        every { inAppNotification.get(any()) } throws JSONException("some exception")

        // Act
        val notification = CTInAppNotification().initWithJSON(inAppNotification, true)
        // Assert
        assertNotNull(notification)
        assertNotNull(notification.error)
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

    @Test
    fun checkTimeForParcelize() {
        val timeInMillisHalfInter = measureTimeMillis { parcelizeCheck(JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL)) }
        System.out.println("Time taken to parcelize half inter = $timeInMillisHalfInter")
        assertTrue(timeInMillisHalfInter < 50)

        val timeInMillisHtml = measureTimeMillis { parcelizeCheck(JSONObject(InAppFixtures.TYPE_BIG_HTML)) }
        System.out.println("Time taken to parcelize html = $timeInMillisHtml")
        assertTrue(timeInMillisHtml < 50)
    }

    fun parcelizeCheck(
        jsonObject : JSONObject
    ) {

        // Act
        val notification = CTInAppNotification().initWithJSON(jsonObject, true)

        val parcel = Parcel.obtain()
        notification.writeToParcel(parcel, 0)

        //>>>>> Record dataPosition
        val eop = parcel.dataPosition()

        // After you're done with writing, you need to reset the parcel for reading:
        parcel.setDataPosition(0)

        // Reconstruct object from parcel and asserts:
        val createdFromParcel: CTInAppNotification = CTInAppNotification.CREATOR.createFromParcel(parcel)

        // Assert
        assertInAppNotificationsAreEqual(createdFromParcel, notification)
        assertEquals(eop, parcel.dataPosition());
    }

    private fun assertInAppNotificationsAreEqual(
        expected: CTInAppNotification,
        actual: CTInAppNotification
    ) {
        assertEquals(expected.actionExtras, actual.actionExtras)
        assertEquals(expected.backgroundColor, actual.backgroundColor)
        assertEquals(expected.buttonCount, actual.buttonCount)
        assertEquals(expected.campaignId, actual.campaignId)
        if (expected.customExtras != null && actual.customExtras != null) {
            assertEquals(expected.customExtras.toString(), actual.customExtras.toString())
        }
        assertEquals(expected.customInAppUrl, actual.customInAppUrl)
        assertEquals(expected.isDarkenScreen, actual.isDarkenScreen)
        assertEquals(expected.error, actual.error)
        assertEquals(expected.isExcludeFromCaps, actual.isExcludeFromCaps)
        assertEquals(expected.height, actual.height)
        assertEquals(expected.width, actual.width)
        assertEquals(expected.heightPercentage, actual.heightPercentage)
        assertEquals(expected.widthPercentage, actual.widthPercentage)
        assertEquals(expected.aspectRatio, actual.aspectRatio, Double.MIN_VALUE)
        assertEquals(expected.isHideCloseButton, actual.isHideCloseButton)
        assertEquals(expected.html, actual.html)
        assertEquals(expected.id, actual.id)
        assertEquals(expected.inAppType, actual.inAppType)
        assertEquals(expected.isLandscape, actual.isLandscape)
        assertEquals(expected.isPortrait, actual.isPortrait)
        assertEquals(expected.isTablet, actual.isTablet)
        assertEquals(expected.isJsEnabled, actual.isJsEnabled)
        //assertEquals(expected.jsonDescription, actual.jsonDescription)
        assertEquals(expected.message, actual.message)
        assertEquals(expected.messageColor, actual.messageColor)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.title, actual.title)
        assertEquals(expected.titleColor, actual.titleColor)
        assertEquals(expected.isLocalInApp, actual.isLocalInApp)
        assertEquals(expected.fallBackToNotificationSettings(), actual.fallBackToNotificationSettings())
        assertEquals(expected.totalLifetimeCount, actual.totalLifetimeCount)
        assertEquals(expected.totalDailyCount, actual.totalDailyCount)
        assertEquals(expected.maxPerSession, actual.maxPerSession)
        assertEquals(expected.isPortrait, actual.isPortrait)
        assertEquals(expected.isLandscape, actual.isLandscape)
        assertEquals(expected.timeToLive, actual.timeToLive)
        //assertEquals(expected.buttons, actual.buttons)
        assertEquals(expected.position, actual.position)
        assertEquals(expected.isShowClose, actual.isShowClose)
        assertNotEquals(expected.isVideoSupported, actual.isVideoSupported)

    }
}