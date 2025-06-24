package com.clevertap.android.sdk.inapp

import android.os.Parcel
import com.clevertap.android.sdk.Constants
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
    fun `constructor for advanced builder custom-html should initialize correctly for html header`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_HEADER)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeHeaderHTML, notification.inAppType)
    }

    @Test
    fun `constructor for advanced builder custom-html should initialize correctly for html footer`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_FOOTER)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeFooterHTML, notification.inAppType)
    }

    @Test
    fun `constructor for advanced builder custom-html should initialize correctly for html header legacy`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_HEADER_LEGACY)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeHeaderHTML, notification.inAppType)
    }

    @Test
    fun `constructor for advanced builder custom-html should initialize correctly for html footer legacy`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_FOOTER_LEGACY)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeFooterHTML, notification.inAppType)
    }

    @Test
    fun `constructor for advanced builder custom-html should initialize correctly for interstitial`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeInterstitialHTML, notification.inAppType)
    }

    @Test
    fun `constructor for advanced builder custom-html should initialize correctly for half interstitial`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_HALF_INTERSTITIAL)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        assertEquals("custom-html", notification.type)
        assertEquals(CTInAppType.CTInAppTypeHalfInterstitialHTML, notification.inAppType)
    }

    @Test
    fun `constructor for advanced builder custom-html should initialize correctly for cover`() {
        // Arrange - Html in app
        val jsonObject = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

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
    fun `constructor half interstitial json should initialize correctly`() {
        // Arrange - Half interstitial inapp
        val jsonObject = JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

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
    fun `constructor should set defaults for missing json fields`() {
        // Arrange
        val type = "garbage"
        val jsonString = """
            {
                "type": $type
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        // Assert
        assertNotNull(notification)
        assertFalse(notification.isLocalInApp)
        assertFalse(notification.fallBackToNotificationSettings)
        assertFalse(notification.isExcludeFromCaps)
        assertEquals(-1, notification.totalLifetimeCount)
        assertEquals(-1, notification.totalDailyCount)
        assertEquals(-1, notification.maxPerSession)
        assertFalse(notification.isTablet)
        assertEquals(Constants.WHITE, notification.backgroundColor)
        assertTrue(notification.isPortrait)
        assertFalse(notification.isLandscape)
        assertNull(notification.title)
        assertEquals(Constants.BLACK, notification.titleColor)
        assertNull(notification.message)
        assertEquals(Constants.BLACK, notification.messageColor)
        assertFalse(notification.isHideCloseButton)
        assertNotNull(notification.mediaList)
        assertEquals(0, notification.mediaList.size)
    }

    @Test
    fun `constructor invalidJson should set error`() {
        // Arrange
        val inAppNotification = mockk<JSONObject>()
        every { inAppNotification.has(any()) } throws JSONException("some exception")
        every { inAppNotification.get(any()) } throws JSONException("some exception")

        // Act
        val notification = CTInAppNotification(inAppNotification, true)

        assertNotNull(notification.error)
    }

    @Test
    fun `constructor customHtmlType should call legacy configure`() {
        // Arrange
        val jsonString = """
            {
                "type": "custom_html"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        // Assert
        assertNotNull(notification)
        assertEquals("custom_html", notification.type)
    }

    @Test
    fun `constructor checks invalid aspect ratio and defaults it to -1`() {

        // Arrange data which is invalid json from BE with aspect ratio 0.0
        val jo = JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_INVALID_ASPECT_RATIO)
        val notification = CTInAppNotification(jo, true)

        // assert it is set as default -1.0
        assertEquals(CTInAppNotification.HTML_DEFAULT_ASPECT_RATIO, notification.aspectRatio, Double.MIN_VALUE)
    }

    @Test
    fun checkTimeForParcelize() {
        val timeInMillisHalfInter = parcelizeCheck(JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL))
        println("Time taken to parcelize half inter = $timeInMillisHalfInter")
        assertTrue(timeInMillisHalfInter < 50)

        val timeInMillisHtml = parcelizeCheck(JSONObject(InAppFixtures.TYPE_BIG_HTML))
        println("Time taken to parcelize html = $timeInMillisHtml")
        assertTrue(timeInMillisHtml < 50)
    }

    fun parcelizeCheck(jsonObject: JSONObject): Long {

        // Act
        val notification = CTInAppNotification(jsonObject, true)

        var createdFromParcel: CTInAppNotification
        var eop = 0
        val parcel = Parcel.obtain()
        val parcelizeTime = measureTimeMillis {
            notification.writeToParcel(parcel, 0)

            //>>>>> Record dataPosition
            eop = parcel.dataPosition()

            // After you're done with writing, you need to reset the parcel for reading:
            parcel.setDataPosition(0)

            // Reconstruct object from parcel and asserts:
            createdFromParcel = CTInAppNotification.CREATOR.createFromParcel(parcel)
        }
        // Assert
        assertInAppNotificationsAreEqual(notification, createdFromParcel)
        assertEquals(eop, parcel.dataPosition());

        return parcelizeTime
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
        assertEquals(expected.jsonDescription.toString(), actual.jsonDescription.toString())
        assertEquals(expected.message, actual.message)
        assertEquals(expected.messageColor, actual.messageColor)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.title, actual.title)
        assertEquals(expected.titleColor, actual.titleColor)
        assertEquals(expected.isLocalInApp, actual.isLocalInApp)
        assertEquals(expected.fallBackToNotificationSettings, actual.fallBackToNotificationSettings)
        assertEquals(expected.totalLifetimeCount, actual.totalLifetimeCount)
        assertEquals(expected.totalDailyCount, actual.totalDailyCount)
        assertEquals(expected.maxPerSession, actual.maxPerSession)
        assertEquals(expected.isPortrait, actual.isPortrait)
        assertEquals(expected.isLandscape, actual.isLandscape)
        assertEquals(expected.timeToLive, actual.timeToLive)
        assertEquals(expected.buttons, actual.buttons)
        assertEquals(expected.mediaList, actual.mediaList)
        assertEquals(expected.position, actual.position)
        assertEquals(expected.isShowClose, actual.isShowClose)
        assertNotEquals(expected.isVideoSupported, actual.isVideoSupported)

    }
}