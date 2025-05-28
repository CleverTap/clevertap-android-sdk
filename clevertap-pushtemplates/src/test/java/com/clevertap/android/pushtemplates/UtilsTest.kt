package com.clevertap.android.pushtemplates

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
class UtilsTest {

    private lateinit var mockBundle: Bundle

    @Before
    fun setUp() {
        mockBundle = mockk<Bundle>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getFlipInterval should return default value when bundle has no flip interval key`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns null

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is empty string`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns ""

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is null`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns null

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is not a valid number`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "abc"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval contains special characters`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "123abc!@#"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is zero`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "0"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is negative`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "-500"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is less than default`() {
        // Given
        val smallInterval = (PTConstants.PT_FLIP_INTERVAL_TIME - 1000).toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns smallInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return provided value when it equals default`() {
        // Given
        val defaultInterval = PTConstants.PT_FLIP_INTERVAL_TIME.toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns defaultInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return provided value when it is greater than default`() {
        // Given
        val largeInterval = (PTConstants.PT_FLIP_INTERVAL_TIME + 2000).toString()
        val expectedValue = PTConstants.PT_FLIP_INTERVAL_TIME + 2000
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns largeInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `getFlipInterval should handle very large valid numbers`() {
        // Given
        val largeInterval = "999999"
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns largeInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(999999, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval has leading spaces`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "  5000"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval has trailing spaces`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "5000  "

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is decimal number`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "5000.5"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when parsing throws NumberFormatException`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "2147483648" // Integer.MAX_VALUE + 1

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should verify correct method calls`() {
        // Given
        val testInterval = "6000"
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns testInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        verify(exactly = 1) { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) }
        assertEquals(6000, result)
    }

    @Test
    fun `getFlipInterval should handle edge case of Integer MIN_VALUE`() {
        // Given
        val minValue = Integer.MIN_VALUE.toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns minValue

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should handle edge case of Integer MAX_VALUE`() {
        // Given
        val maxValue = Integer.MAX_VALUE.toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns maxValue

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(Integer.MAX_VALUE, result)
    }

    // Tests for getColourOrNull method

    @Test
    fun `getColourOrNull should return parsed color for valid hex color`() {
        // Given
        val validColor = "#FF0000" // Red
        val expectedColor = -65536 // Red color value

        // When
        val result = Utils.getColourOrNull(validColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColourOrNull should return parsed color for valid 6-digit hex color`() {
        // Given
        val validColor = "#00FF00" // Green
        val expectedColor = -16711936 // Green color value

        // When
        val result = Utils.getColourOrNull(validColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColourOrNull should return parsed color for valid 8-digit hex color with alpha`() {
        // Given
        val validColor = "#80FF0000" // Semi-transparent red
        val expectedColor = -2130771968 // Semi-transparent red color value

        // When
        val result = Utils.getColourOrNull(validColor)

        // Then
        assertEquals(expectedColor, result)
    }


    @Test
    fun `getColourOrNull should return parsed color for black`() {
        // Given
        val blackColor = "#000000"
        val expectedColor = -16777216 // Black color value

        // When
        val result = Utils.getColourOrNull(blackColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColourOrNull should return parsed color for white`() {
        // Given
        val whiteColor = "#FFFFFF"
        val expectedColor = -1 // White color value

        // When
        val result = Utils.getColourOrNull(whiteColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColourOrNull should return null for null input`() {
        // Given
        val nullColor: String? = null

        // When
        val result = Utils.getColourOrNull(nullColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for empty string`() {
        // Given
        val emptyColor = ""

        // When
        val result = Utils.getColourOrNull(emptyColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for invalid hex color without hash`() {
        // Given
        val invalidColor = "FF0000" // Missing hash

        // When
        val result = Utils.getColourOrNull(invalidColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for invalid hex color with invalid characters`() {
        // Given
        val invalidColor = "#GG0000" // Invalid hex characters

        // When
        val result = Utils.getColourOrNull(invalidColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for invalid hex length`() {
        // Given
        val invalidColor = "#FF00" // Invalid length (5 characters)

        // When
        val result = Utils.getColourOrNull(invalidColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for hex color with spaces`() {
        // Given
        val invalidColor = "#FF 00 00" // Spaces in hex

        // When
        val result = Utils.getColourOrNull(invalidColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for hex color with leading spaces`() {
        // Given
        val invalidColor = "  #FF0000" // Leading spaces

        // When
        val result = Utils.getColourOrNull(invalidColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for hex color with trailing spaces`() {
        // Given
        val invalidColor = "#FF0000  " // Trailing spaces

        // When
        val result = Utils.getColourOrNull(invalidColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for random string`() {
        // Given
        val randomString = "randomtext"

        // When
        val result = Utils.getColourOrNull(randomString)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for special characters`() {
        // Given
        val specialChars = "@#$%^&*()"

        // When
        val result = Utils.getColourOrNull(specialChars)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should handle case sensitivity correctly`() {
        // Given - Both should be valid as hex is case insensitive
        val lowerCaseColor = "#ff0000"
        val upperCaseColor = "#FF0000"
        val expectedColor = -65536 // Red color value

        // When
        val lowerResult = Utils.getColourOrNull(lowerCaseColor)
        val upperResult = Utils.getColourOrNull(upperCaseColor)

        // Then
        assertEquals(expectedColor, lowerResult)
        assertEquals(expectedColor, upperResult)
        assertEquals(lowerResult, upperResult)
    }

    @Test
    fun `getColourOrNull should return null for too long hex string`() {
        // Given
        val tooLongColor = "#FF0000FF00" // 9 digits after hash (too long)

        // When
        val result = Utils.getColourOrNull(tooLongColor)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return null for just hash symbol`() {
        // Given
        val justHash = "#"

        // When
        val result = Utils.getColourOrNull(justHash)

        // Then
        assertNull(result)
    }

    @Test
    fun `getColourOrNull should return parsed color for transparent`() {
        // Given
        val transparentColor = "#00000000" // Fully transparent
        val expectedColor = 0 // Transparent color value

        // When
        val result = Utils.getColourOrNull(transparentColor)

        // Then
        assertEquals(expectedColor, result)
    }


    @Test
    fun `getColourOrNull should handle mixed case hex correctly`() {
        // Given
        val mixedCaseColor = "#Ff00Aa" // Mixed case hex
        val expectedColor = -65366 // Color value for #FF00AA

        // When
        val result = Utils.getColourOrNull(mixedCaseColor)

        // Then
        assertEquals(expectedColor, result)
    }

    // Tests for getColour method

    @Test
    fun `getColour should return parsed color when valid color provided`() {
        // Given
        val validColor = "#FF0000" // Red
        val defaultColor = "#000000" // Black
        val expectedColor = -65536 // Red color value

        // When
        val result = Utils.getColour(validColor, defaultColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColour should return default color when invalid color provided`() {
        // Given
        val invalidColor = "invalid"
        val defaultColor = "#00FF00" // Green
        val expectedColor = -16711936 // Green color value

        // When
        val result = Utils.getColour(invalidColor, defaultColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColour should return default color when null color provided`() {
        // Given
        val nullColor: String? = null
        val defaultColor = "#0000FF" // Blue
        val expectedColor = -16776961 // Blue color value

        // When
        val result = Utils.getColour(nullColor, defaultColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColour should return default color when empty color provided`() {
        // Given
        val emptyColor = ""
        val defaultColor = "#FFFFFF" // White
        val expectedColor = -1 // White color value

        // When
        val result = Utils.getColour(emptyColor, defaultColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColour should handle both valid colors correctly`() {
        // Given
        val validColor = "#FF0000" // Red
        val validDefault = "#00FF00" // Green (should not be used)
        val expectedColor = -65536 // Red color value

        // When
        val result = Utils.getColour(validColor, validDefault)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test
    fun `getColour should work with 8-digit hex colors`() {
        // Given
        val validColor = "#80FF0000" // Semi-transparent red
        val defaultColor = "#000000" // Black
        val expectedColor = -2130771968 // Semi-transparent red color value

        // When
        val result = Utils.getColour(validColor, defaultColor)

        // Then
        assertEquals(expectedColor, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getColour should throw exception when both colors are invalid`() {
        // Given
        val invalidColor = "invalid"
        val invalidDefault = "alsoInvalid"

        // When
        Utils.getColour(invalidColor, invalidDefault)

        // Then - Exception should be thrown
    }

    // Tests for createColorMap method

    @Test
    fun `createColorMap should return map with light mode colors when isDarkMode is false`() {
        // Given
        every { mockBundle.getString(match { it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns null
        every { mockBundle.getString(match { !it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#FFFFFF"

        // When
        val result = Utils.createColorMap(mockBundle, false)

        // Then
        for (key in PTConstants.COLOR_KEYS) {
            assertEquals("#FFFFFF", result[key])
        }
    }

    @Test
    fun `createColorMap should return map with light mode colors when isDarkMode is false and all colours present`() {
        // Given
        every { mockBundle.getString(match { it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#123456"
        every { mockBundle.getString(match { !it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#FFFFFF"

        // When
        val result = Utils.createColorMap(mockBundle, false)

        // Then
        for (key in PTConstants.COLOR_KEYS) {
            assertEquals("#FFFFFF", result[key])
        }
    }

    @Test
    fun `createColorMap should return map with dark mode colors when isDarkMode is true and dark colors exist`() {
        // Given
        every { mockBundle.getString(match { it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#123456"
        every { mockBundle.getString(match { !it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns null

        // When
        val result = Utils.createColorMap(mockBundle, true)

        // Then
        for (key in PTConstants.COLOR_KEYS) {
            assertEquals("#123456", result[key])
        }
    }

    @Test
    fun `createColorMap should return map with dark mode colors when isDarkMode is true and all colors exist`() {
        // Given
        every { mockBundle.getString(match { it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#123456"
        every { mockBundle.getString(match { !it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#FFFFFF"

        // When
        val result = Utils.createColorMap(mockBundle, true)

        // Then
        for (key in PTConstants.COLOR_KEYS) {
            assertEquals("#123456", result[key])
        }
    }

    @Test
    fun `createColorMap should return null for key when no colours exist`() {
        // Given
        every { mockBundle.getString(match { it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns null
        every { mockBundle.getString(match { !it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns null

        // When
        val result = Utils.createColorMap(mockBundle, true)

        // Then
        for (key in PTConstants.COLOR_KEYS) {
            assertNull(result[key])
        }
    }

    @Test
    fun `createColorMap should return light mode colours when isDarkMode is true and darkMode colours are missing`() {
        // Given
        every { mockBundle.getString(match { it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns null
        every { mockBundle.getString(match { !it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#FFFFFF"

        // When
        val result = Utils.createColorMap(mockBundle, true)

        // Then
        for (key in PTConstants.COLOR_KEYS) {
            assertEquals("#FFFFFF", result[key])
        }
    }

    @Test
    fun `createColorMap should return return null for key when isDarkMode is false and lightMode colours are missing`() {
        // Given
        every { mockBundle.getString(match { it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns "#FFFFFF"
        every { mockBundle.getString(match { !it.endsWith(PTConstants.PT_DARK_MODE_SUFFIX) }) } returns null

        // When
        val result = Utils.createColorMap(mockBundle, false)

        // Then
        for (key in PTConstants.COLOR_KEYS) {
            assertNull(result[key])
        }
    }

    // Tests for loadImageBitmapIntoRemoteView method

    @Test
    fun `loadImageBitmapIntoRemoteView should set bitmap on remote view`() {
        // Given
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockBitmap = mockk<Bitmap>()
        val imageViewId = 123

        // When
        Utils.loadImageBitmapIntoRemoteView(imageViewId, mockBitmap, mockRemoteViews)

        // Then
        verify { mockRemoteViews.setImageViewBitmap(imageViewId, mockBitmap) }
    }

    // Tests for loadImageRidIntoRemoteView method

    @Test
    fun `loadImageRidIntoRemoteView should set resource on remote view`() {
        // Given
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val imageViewId = 123
        val resourceId = 456

        // When
        Utils.loadImageRidIntoRemoteView(imageViewId, resourceId, mockRemoteViews)

        // Then
        verify { mockRemoteViews.setImageViewResource(imageViewId, resourceId) }
    }

    // Tests for getTimeStamp method

    @Test
    fun `getTimeStamp should return formatted time string`() {
        // Given
        val mockContext = mockk<Context>()
        mockkStatic("android.text.format.DateUtils")
        every { android.text.format.DateUtils.formatDateTime(any(), any(), any()) } returns "12:34 PM"

        // When
        val result = Utils.getTimeStamp(mockContext)

        // Then
        assertEquals("12:34 PM", result)
    }

    // Tests for getApplicationName method

    @Test
    fun `getApplicationName should return string from labelRes when available`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppInfo = mockk<ApplicationInfo>()
        mockAppInfo.labelRes = 123
        every { mockContext.applicationInfo } returns mockAppInfo
        every { mockContext.getString(123) } returns "Test App"

        // When
        val result = Utils.getApplicationName(mockContext)

        // Then
        assertEquals("Test App", result)
    }

    @Test
    fun `getApplicationName should return nonLocalizedLabel when labelRes is 0`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppInfo = mockk<ApplicationInfo>()
        mockAppInfo.labelRes = 0
        mockAppInfo.nonLocalizedLabel = "Direct Label"
        every { mockContext.applicationInfo } returns mockAppInfo

        // When
        val result = Utils.getApplicationName(mockContext)

        // Then
        assertEquals("Direct Label", result)
    }

    // Tests for getTimerEnd method

    @Test
    fun `getTimerEnd should return MIN_VALUE when timer end value is -1`() {
        // Given
        every { mockBundle.keySet() } returns setOf(PTConstants.PT_TIMER_END)
        every { mockBundle.getString(PTConstants.PT_TIMER_END) } returns "-1"

        // When
        val result = Utils.getTimerEnd(mockBundle)

        // Then
        assertEquals(Integer.MIN_VALUE, result)
    }

    @Test
    fun `getTimerEnd should calculate difference when valid timestamp provided`() {
        // Given
        val futureTimestamp = (System.currentTimeMillis() / 1000) + 3600 // 1 hour from now
        every { mockBundle.keySet() } returns setOf(PTConstants.PT_TIMER_END)
        every { mockBundle.getString(PTConstants.PT_TIMER_END) } returns futureTimestamp.toString()

        // When
        val result = Utils.getTimerEnd(mockBundle)

        // Then
        assertTrue("Result should be positive for future timestamp", result > 0)
        assertTrue("Result should be around 3600 seconds", result > 3500 && result <= 3600)
    }

    @Test
    fun `getTimerEnd should handle formatted timestamp with D_ prefix`() {
        // Given
        val futureTimestamp = (System.currentTimeMillis() / 1000) + 1800 // 30 minutes from now
        val formattedValue = "\$D_$futureTimestamp"
        every { mockBundle.keySet() } returns setOf(PTConstants.PT_TIMER_END)
        every { mockBundle.getString(PTConstants.PT_TIMER_END) } returns formattedValue

        // When
        val result = Utils.getTimerEnd(mockBundle)

        // Then
        assertTrue("Result should be positive for future timestamp", result > 0)
        assertTrue("Result should be around 1800 seconds", result > 1700 && result <= 1800)
    }


    // Tests for getFallback method

    @Test
    fun `getFallback should return current fallback value`() {
        // Given
        PTConstants.PT_FALLBACK = true

        // When
        val result = Utils.getFallback()

        // Then
        assertTrue(result)

        // Reset
        PTConstants.PT_FALLBACK = false
        val resetResult = Utils.getFallback()
        assertFalse(resetResult)
    }

    // Tests for getImageListFromExtras method

    @Test
    fun `getImageListFromExtras should return list of image URLs`() {
        // Given
        val keys = setOf("pt_img1", "pt_img2", "other_key", "pt_img_large")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString("pt_img1") } returns "https://example.com/img1.jpg"
        every { mockBundle.getString("pt_img2") } returns "https://example.com/img2.jpg"
        every { mockBundle.getString("pt_img_large") } returns "https://example.com/large.jpg"
        every { mockBundle.getString("other_key") } returns "not_an_image"

        // When
        val result = Utils.getImageListFromExtras(mockBundle)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.contains("https://example.com/img1.jpg"))
        assertTrue(result.contains("https://example.com/img2.jpg"))
        assertTrue(result.contains("https://example.com/large.jpg"))
        assertFalse(result.contains("not_an_image"))
    }

    @Test
    fun `getImageListFromExtras should return empty list when no image keys exist`() {
        // Given
        val keys = setOf("other_key1", "other_key2")
        every { mockBundle.keySet() } returns keys

        // When
        val result = Utils.getImageListFromExtras(mockBundle)

        // Then
        assertTrue(result.isEmpty())
    }

    // Tests for _getManifestStringValueForKey method

    @Test
    fun `_getManifestStringValueForKey should return string value when object exists`() {
        // Given
        val keyName = "test_key"
        every { mockBundle.get(keyName) } returns "test_value"

        // When
        val result = Utils._getManifestStringValueForKey(mockBundle, keyName)

        // Then
        assertEquals("test_value", result)
    }

    @Test
    fun `_getManifestStringValueForKey should return string representation of non-string object`() {
        // Given
        val keyName = "number_key"
        every { mockBundle.get(keyName) } returns 12345

        // When
        val result = Utils._getManifestStringValueForKey(mockBundle, keyName)

        // Then
        assertEquals("12345", result)
    }

    @Test
    fun `_getManifestStringValueForKey should return null when object is null`() {
        // Given
        val keyName = "null_key"
        every { mockBundle.get(keyName) } returns null

        // When
        val result = Utils._getManifestStringValueForKey(mockBundle, keyName)

        // Then
        assertNull(result)
    }

    @Test
    fun `_getManifestStringValueForKey should return null when exception is thrown`() {
        // Given
        val keyName = "exception_key"
        every { mockBundle.get(keyName) } throws RuntimeException("Test exception")

        // When
        val result = Utils._getManifestStringValueForKey(mockBundle, keyName)

        // Then
        assertNull(result)
    }

    @Test
    fun `_getManifestStringValueForKey should handle boolean values`() {
        // Given
        val keyName = "boolean_key"
        every { mockBundle.get(keyName) } returns true

        // When
        val result = Utils._getManifestStringValueForKey(mockBundle, keyName)

        // Then
        assertEquals("true", result)
    }

    // Tests for getAppIconAsIntId method

    @Test
    fun `getAppIconAsIntId should return icon resource id from ApplicationInfo`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppInfo = mockk<ApplicationInfo>()
        val expectedIconId = 123456
        mockAppInfo.icon = expectedIconId
        every { mockContext.applicationInfo } returns mockAppInfo

        // When
        val result = Utils.getAppIconAsIntId(mockContext)

        // Then
        assertEquals(expectedIconId, result)
    }

    @Test
    fun `getAppIconAsIntId should return 0 when icon is not set`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppInfo = mockk<ApplicationInfo>()
        mockAppInfo.icon = 0 // Default/unset icon
        every { mockContext.applicationInfo } returns mockAppInfo

        // When
        val result = Utils.getAppIconAsIntId(mockContext)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `getAppIconAsIntId should handle negative icon values`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppInfo = mockk<ApplicationInfo>()
        val negativeIconId = -1
        mockAppInfo.icon = negativeIconId
        every { mockContext.applicationInfo } returns mockAppInfo

        // When
        val result = Utils.getAppIconAsIntId(mockContext)

        // Then
        assertEquals(negativeIconId, result)
    }

    // Tests for raiseNotificationClicked method

    @Test
    fun `raiseNotificationClicked should use instanceWithConfig when config is provided`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val mockInstance = mockk<CleverTapAPI>()
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
        every { mockInstance.pushNotificationClickedEvent(mockBundle) } just Runs

        // When
        Utils.raiseNotificationClicked(mockContext, mockBundle, mockConfig)

        // Then
        verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
        verify { mockInstance.pushNotificationClickedEvent(mockBundle) }
        verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any()) }
    }

    @Test
    fun `raiseNotificationClicked should use getDefaultInstance when config is null`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
        every { mockInstance.pushNotificationClickedEvent(mockBundle) } just Runs

        // When
        Utils.raiseNotificationClicked(mockContext, mockBundle, null)

        // Then
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        verify { mockInstance.pushNotificationClickedEvent(mockBundle) }
        verify(exactly = 0) { CleverTapAPI.instanceWithConfig(any(), any()) }
    }

    @Test
    fun `raiseNotificationClicked should not call pushNotificationClickedEvent when instance is null with config`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns null

        // When
        Utils.raiseNotificationClicked(mockContext, mockBundle, mockConfig)

        // Then
        verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
        // No verification for pushNotificationClickedEvent as instance is null
    }

    @Test
    fun `raiseNotificationClicked should not call pushNotificationClickedEvent when instance is null without config`() {
        // Given
        val mockContext = mockk<Context>()
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns null

        // When
        Utils.raiseNotificationClicked(mockContext, mockBundle, null)

        // Then
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        // No verification for pushNotificationClickedEvent as instance is null
    }

    // Tests for getActionKeys method

    @Test
    fun `getActionKeys should return JSONArray when valid JSON string exists`() {
        // Given
        val jsonString = "[{\"id\":\"1\",\"label\":\"Action 1\"},{\"id\":\"2\",\"label\":\"Action 2\"}]"
        every { mockBundle.getString(Constants.WZRK_ACTIONS) } returns jsonString

        // When
        val result = Utils.getActionKeys(mockBundle)

        // Then
        assertNotNull(result)
        assertEquals(2, result!!.length())
        assertEquals("1", result.getJSONObject(0).getString("id"))
        assertEquals("Action 1", result.getJSONObject(0).getString("label"))
        assertEquals("2", result.getJSONObject(1).getString("id"))
        assertEquals("Action 2", result.getJSONObject(1).getString("label"))
    }

    @Test
    fun `getActionKeys should return empty JSONArray when empty array string provided`() {
        // Given
        val emptyArrayString = "[]"
        every { mockBundle.getString(Constants.WZRK_ACTIONS) } returns emptyArrayString

        // When
        val result = Utils.getActionKeys(mockBundle)

        // Then
        assertNotNull(result)
        assertEquals(0, result!!.length())
    }

    @Test
    fun `getActionKeys should return null when WZRK_ACTIONS key has null value`() {
        // Given
        every { mockBundle.getString(Constants.WZRK_ACTIONS) } returns null

        // When
        val result = Utils.getActionKeys(mockBundle)

        // Then
        assertNull(result)
    }

    @Test
    fun `getActionKeys should return null when invalid JSON string provided`() {
        // Given
        val invalidJsonString = "invalid json string"
        every { mockBundle.getString(Constants.WZRK_ACTIONS) } returns invalidJsonString

        // When
        val result = Utils.getActionKeys(mockBundle)

        // Then
        assertNull(result)
    }

    @Test
    fun `getActionKeys should return null when malformed JSON array provided`() {
        // Given
        val malformedJsonString = "[{\"id\":\"1\",\"label\":\"Action 1\"},{\"id\":\"2\",\"label\":\"Action 2\"]"
        every { mockBundle.getString(Constants.WZRK_ACTIONS) } returns malformedJsonString

        // When
        val result = Utils.getActionKeys(mockBundle)

        // Then
        assertNull(result)
    }

    @Test
    fun `getActionKeys should return JSONArray with single item when single action provided`() {
        // Given
        val singleActionString = "[{\"id\":\"1\",\"label\":\"Single Action\"}]"
        every { mockBundle.getString(Constants.WZRK_ACTIONS) } returns singleActionString

        // When
        val result = Utils.getActionKeys(mockBundle)

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.length())
        assertEquals("1", result.getJSONObject(0).getString("id"))
        assertEquals("Single Action", result.getJSONObject(0).getString("label"))
    }

    @Test
    fun `getActionKeys should handle JSON with different structure`() {
        // Given
        val differentStructureString = "[\"action1\",\"action2\",\"action3\"]"
        every { mockBundle.getString(Constants.WZRK_ACTIONS) } returns differentStructureString

        // When
        val result = Utils.getActionKeys(mockBundle)

        // Then
        assertNotNull(result)
        assertEquals(3, result!!.length())
        assertEquals("action1", result.getString(0))
        assertEquals("action2", result.getString(1))
        assertEquals("action3", result.getString(2))
    }

    // Tests for convertRatingBundleObjectToHashMap method

    @Test
    fun `convertRatingBundleObjectToHashMap should remove config key`() {
        // Given
        val keys = setOf("config", "wzrk_test", PTConstants.PT_ID)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.remove("config") } just Runs
        every { mockBundle.get("wzrk_test") } returns "test_value"
        every { mockBundle.get(PTConstants.PT_ID) } returns "pt_id_value"

        // When
        val result = Utils.convertRatingBundleObjectToHashMap(mockBundle)

        // Then
        verify { mockBundle.remove("config") }
        assertEquals(2, result.size)
        assertEquals("test_value", result["wzrk_test"])
        assertEquals("pt_id_value", result[PTConstants.PT_ID])
    }

    @Test
    fun `convertRatingBundleObjectToHashMap should include wzrk_ keys`() {
        // Given
        val keys = setOf("wzrk_campaign", "wzrk_id", "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.remove("config") } just Runs
        every { mockBundle.get("wzrk_campaign") } returns "campaign_123"
        every { mockBundle.get("wzrk_id") } returns "id_456"
        every { mockBundle.get("other_key") } returns "ignored"

        // When
        val result = Utils.convertRatingBundleObjectToHashMap(mockBundle)

        // Then
        assertEquals(2, result.size)
        assertEquals("campaign_123", result["wzrk_campaign"])
        assertEquals("id_456", result["wzrk_id"])
        assertFalse(result.containsKey("other_key"))
    }

    @Test
    fun `convertRatingBundleObjectToHashMap should include PT_ID key`() {
        // Given
        val keys = setOf(PTConstants.PT_ID, "random_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.remove("config") } just Runs
        every { mockBundle.get(PTConstants.PT_ID) } returns "pt_123"
        every { mockBundle.get("random_key") } returns "ignored"

        // When
        val result = Utils.convertRatingBundleObjectToHashMap(mockBundle)

        // Then
        assertEquals(1, result.size)
        assertEquals("pt_123", result[PTConstants.PT_ID])
        assertFalse(result.containsKey("random_key"))
    }

    @Test
    fun `convertRatingBundleObjectToHashMap should handle nested Bundle recursively`() {
        // Given
        val nestedBundle = mockk<Bundle>()
        val nestedKeys = setOf("wzrk_nested")
        every { nestedBundle.keySet() } returns nestedKeys
        every { nestedBundle.remove("config") } just Runs
        every { nestedBundle.get("wzrk_nested") } returns "nested_value"

        val keys = setOf("wzrk_parent")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.remove("config") } just Runs
        every { mockBundle.get("wzrk_parent") } returns nestedBundle

        // When
        val result = Utils.convertRatingBundleObjectToHashMap(mockBundle)

        // Then
        assertEquals(1, result.size)
        assertEquals("nested_value", result["wzrk_nested"])
    }

    @Test
    fun `convertRatingBundleObjectToHashMap should return empty map when no valid keys exist`() {
        // Given
        val keys = setOf("random_key", "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.remove("config") } just Runs
        every { mockBundle.get(any()) } returns "ignored"

        // When
        val result = Utils.convertRatingBundleObjectToHashMap(mockBundle)

        // Then
        assertTrue(result.isEmpty())
    }

    // Tests for getEventNameFromExtras method

    @Test
    fun `getEventNameFromExtras should return event name when PT_EVENT_NAME_KEY exists`() {
        // Given
        val keys = setOf(PTConstants.PT_EVENT_NAME_KEY, "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_EVENT_NAME_KEY) } returns "test_event"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getEventNameFromExtras(mockBundle)

        // Then
        assertEquals("test_event", result)
    }

    @Test
    fun `getEventNameFromExtras should return event name when key contains PT_EVENT_NAME_KEY`() {
        // Given
        val keyWithSuffix = PTConstants.PT_EVENT_NAME_KEY + "_suffix"
        val keys = setOf(keyWithSuffix, "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(keyWithSuffix) } returns "custom_event"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getEventNameFromExtras(mockBundle)

        // Then
        assertEquals("custom_event", result)
    }

    @Test
    fun `getEventNameFromExtras should return null when no PT_EVENT_NAME_KEY exists`() {
        // Given
        val keys = setOf("random_key", "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(any()) } returns "some_value"

        // When
        val result = Utils.getEventNameFromExtras(mockBundle)

        // Then
        assertNull(result)
    }

    @Test
    fun `getEventNameFromExtras should return last matching event name when multiple keys exist`() {
        // Given
        val key1 = PTConstants.PT_EVENT_NAME_KEY + "1"
        val key2 = PTConstants.PT_EVENT_NAME_KEY + "2"
        val keys = setOf(key1, key2)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(key1) } returns "first_event"
        every { mockBundle.getString(key2) } returns "second_event"

        // When
        val result = Utils.getEventNameFromExtras(mockBundle)

        // Then
        // Note: The method iterates through keySet, so result depends on iteration order
        // We just verify one of the valid event names is returned
        assertTrue(result == "first_event" || result == "second_event")
    }

    // Tests for getEventPropertiesFromExtras(Bundle extras) method

    @Test
    fun `getEventPropertiesFromExtras should return properties when PT_EVENT_PROPERTY_KEY exists with separator`() {
        // Given
        val propertyKey = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "campaign_id"
        val keys = setOf(propertyKey, "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKey) } returns "campaign_123"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle)

        // Then
        assertEquals(1, result.size)
        assertEquals("campaign_123", result["campaign_id"])
    }

    @Test
    fun `getEventPropertiesFromExtras should return multiple properties when multiple valid keys exist`() {
        // Given
        val propertyKey1 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "user_id"
        val propertyKey2 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "session_id"
        val keys = setOf(propertyKey1, propertyKey2, "random_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKey1) } returns "user_456"
        every { mockBundle.getString(propertyKey2) } returns "session_789"
        every { mockBundle.getString("random_key") } returns "ignored"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle)

        // Then
        assertEquals(2, result.size)
        assertEquals("user_456", result["user_id"])
        assertEquals("session_789", result["session_id"])
    }

    @Test
    fun `getEventPropertiesFromExtras should return empty map when no valid property keys exist`() {
        // Given
        val keys = setOf("random_key", "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(any()) } returns "some_value"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getEventPropertiesFromExtras should skip properties without separator`() {
        // Given
        val propertyKeyWithoutSeparator = PTConstants.PT_EVENT_PROPERTY_KEY + "no_separator"
        val propertyKeyWithSeparator = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "valid_prop"
        val keys = setOf(propertyKeyWithoutSeparator, propertyKeyWithSeparator)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKeyWithoutSeparator) } returns "should_be_skipped"
        every { mockBundle.getString(propertyKeyWithSeparator) } returns "valid_value"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle)

        // Then
        assertEquals(1, result.size)
        assertEquals("valid_value", result["valid_prop"])
    }

    @Test
    fun `getEventPropertiesFromExtras should skip properties with empty or null values`() {
        // Given
        val propertyKey1 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "empty_prop"
        val propertyKey2 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "null_prop"
        val propertyKey3 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "valid_prop"
        val keys = setOf(propertyKey1, propertyKey2, propertyKey3)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKey1) } returns ""
        every { mockBundle.getString(propertyKey2) } returns null
        every { mockBundle.getString(propertyKey3) } returns "valid_value"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle)

        // Then
        assertEquals(1, result.size)
        assertEquals("valid_value", result["valid_prop"])
    }

    // Tests for getEventPropertiesFromExtras(Bundle extras, String pkey, String value) method

    @Test
    fun `getEventPropertiesFromExtras with pkey should replace matching property value`() {
        // Given
        val propertyKey1 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "user_id"
        val propertyKey2 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "session_id"
        val keys = setOf(propertyKey1, propertyKey2)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKey1) } returns "original_user"
        every { mockBundle.getString(propertyKey2) } returns "session_123"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle, "original_user", "replaced_user")

        // Then
        assertEquals(2, result.size)
        assertEquals("replaced_user", result["user_id"])
        assertEquals("session_123", result["session_id"])
    }

    @Test
    fun `getEventPropertiesFromExtras with pkey should use original value when no match found`() {
        // Given
        val propertyKey1 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "user_id"
        val propertyKey2 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "session_id"
        val keys = setOf(propertyKey1, propertyKey2)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKey1) } returns "user_456"
        every { mockBundle.getString(propertyKey2) } returns "session_789"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle, "nonexistent_value", "replacement")

        // Then
        assertEquals(2, result.size)
        assertEquals("user_456", result["user_id"])
        assertEquals("session_789", result["session_id"])
    }

    @Test
    fun `getEventPropertiesFromExtras with pkey should handle case insensitive matching`() {
        // Given
        val propertyKey = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "campaign"
        val keys = setOf(propertyKey)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKey) } returns "CAMPAIGN_ID"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle, "campaign_id", "new_campaign")

        // Then
        assertEquals(1, result.size)
        assertEquals("new_campaign", result["campaign"])
    }

    @Test
    fun `getEventPropertiesFromExtras with pkey should skip properties without separator`() {
        // Given
        val propertyKeyWithoutSeparator = PTConstants.PT_EVENT_PROPERTY_KEY + "no_separator"
        val propertyKeyWithSeparator = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "valid_prop"
        val keys = setOf(propertyKeyWithoutSeparator, propertyKeyWithSeparator)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKeyWithoutSeparator) } returns "should_be_skipped"
        every { mockBundle.getString(propertyKeyWithSeparator) } returns "valid_value"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle, "valid_value", "replaced_value")

        // Then
        assertEquals(1, result.size)
        assertEquals("replaced_value", result["valid_prop"])
    }

    @Test
    fun `getEventPropertiesFromExtras with pkey should skip properties with empty or null values`() {
        // Given
        val propertyKey1 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "empty_prop"
        val propertyKey2 = PTConstants.PT_EVENT_PROPERTY_KEY + PTConstants.PT_EVENT_PROPERTY_SEPERATOR + "valid_prop"
        val keys = setOf(propertyKey1, propertyKey2)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(propertyKey1) } returns ""
        every { mockBundle.getString(propertyKey2) } returns "valid_value"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle, "valid_value", "replaced_value")

        // Then
        assertEquals(1, result.size)
        assertEquals("replaced_value", result["valid_prop"])
    }

    @Test
    fun `getEventPropertiesFromExtras with pkey should return empty map when no valid properties exist`() {
        // Given
        val keys = setOf("random_key", "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(any()) } returns "some_value"

        // When
        val result = Utils.getEventPropertiesFromExtras(mockBundle, "some_value", "replacement")

        // Then
        assertTrue(result.isEmpty())
    }

    // Tests for raiseCleverTapEvent method

    @Test
    fun `raiseCleverTapEvent should use instanceWithConfig when config is provided`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "test_event"
        val eventProps = hashMapOf<String,Any>().apply {
            put("key1", "value1")
            put("key2", "value2")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
        every { mockInstance.pushEvent(eventName, eventProps) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, mockConfig, eventName, eventProps)

        // Then
        verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
        verify { mockInstance.pushEvent(eventName, eventProps) }
        verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any()) }
    }

    @Test
    fun `raiseCleverTapEvent should use getDefaultInstance when config is null`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "test_event"
        val eventProps = hashMapOf<String, Any>().apply {
            put("key1", "value1")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
        every { mockInstance.pushEvent(eventName, eventProps) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

        // Then
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        verify { mockInstance.pushEvent(eventName, eventProps) }
        verify(exactly = 0) { CleverTapAPI.instanceWithConfig(any(), any()) }
    }

    @Test
    fun `raiseCleverTapEvent should not call pushEvent when instance is null with config`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val eventName = "test_event"
        val eventProps = hashMapOf<String, Any>().apply {
            put("key1", "value1")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns null

        // When
        Utils.raiseCleverTapEvent(mockContext, mockConfig, eventName, eventProps)

        // Then
        verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
        // No verification for pushEvent as instance is null
    }

    @Test
    fun `raiseCleverTapEvent should not call pushEvent when instance is null without config`() {
        // Given
        val mockContext = mockk<Context>()
        val eventName = "test_event"
        val eventProps = hashMapOf<String, Any>().apply {
            put("key1", "value1")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns null

        // When
        Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

        // Then
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        // No verification for pushEvent as instance is null
    }

    @Test
    fun `raiseCleverTapEvent should not call pushEvent when event name is null`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventProps = hashMapOf<String, Any>().apply {
            put("key1", "value1")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
        every { mockInstance.pushEvent(any(), any()) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, mockConfig, null, eventProps)

        // Then
        verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
        verify(exactly = 0) { mockInstance.pushEvent(any(), any()) }
    }

    @Test
    fun `raiseCleverTapEvent should not call pushEvent when event name is empty`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventProps = hashMapOf<String, Any>().apply {
            put("key1", "value1")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
        every { mockInstance.pushEvent(any(), any()) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, mockConfig, "", eventProps)

        // Then
        verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
        verify(exactly = 0) { mockInstance.pushEvent(any(), any()) }
    }

    @Test
    fun `raiseCleverTapEvent should call pushEvent with empty properties when eventProps is empty`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "test_event"
        val eventProps = hashMapOf<String, Any>()
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
        every { mockInstance.pushEvent(eventName, eventProps) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, mockConfig, eventName, eventProps)

        // Then
        verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
        verify { mockInstance.pushEvent(eventName, eventProps) }
    }

    @Test
    fun `raiseCleverTapEvent should handle whitespace in event name correctly`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "  "
        val eventProps = hashMapOf<String, Any>().apply {
            put("key1", "value1")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
        every { mockInstance.pushEvent(any(), any()) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

        // Then
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        // Should call pushEvent because whitespace is not considered empty by isEmpty()
        verify { mockInstance.pushEvent(eventName, eventProps) }
    }

    @Test
    fun `raiseCleverTapEvent should work with complex event properties`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "complex_event"
        val eventProps = hashMapOf<String, Any>().apply {
            put("string_prop", "test_value")
            put("number_prop", 123)
            put("boolean_prop", true)
            put("nested_object", mapOf("inner_key" to "inner_value"))
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
        every { mockInstance.pushEvent(eventName, eventProps) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

        // Then
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        verify { mockInstance.pushEvent(eventName, eventProps) }
    }

    @Test
    fun `raiseCleverTapEvent should handle null event properties gracefully`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "test_event"
        val eventProps: HashMap<String, Any>? = null
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
        every { mockInstance.pushEvent(eventName, eventProps) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

        // Then
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        verify { mockInstance.pushEvent(eventName, eventProps) }
    }

    @Test
    fun `raiseCleverTapEvent should verify method call order`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "order_test_event"
        val eventProps = hashMapOf<String, Any>().apply {
            put("test_key", "test_value")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
        every { mockInstance.pushEvent(eventName, eventProps) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, mockConfig, eventName, eventProps)

        // Then
        verifyOrder {
            CleverTapAPI.instanceWithConfig(mockContext, mockConfig)
            mockInstance.pushEvent(eventName, eventProps)
        }
    }

    @Test
    fun `raiseCleverTapEvent should handle event name with special characters`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "event_with_@#$%_special_chars"
        val eventProps = hashMapOf<String, Any>().apply {
            put("key", "value")
        }
        
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
        every { mockInstance.pushEvent(eventName, eventProps) } just Runs

        // When
        Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

        // Then
        verify { mockInstance.pushEvent(eventName, eventProps) }
    }

    // Tests for raiseCleverTapEvent method end

    // Tests for deleteImageFromStorage method

    @Test
    fun `deleteImageFromStorage should extract push ID from intent and setup directory operations`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val pushId = "test_push_123"
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns pushId
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns arrayOf("image_test_push_123.jpg", "other_file.jpg")

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) }
        verify { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) }
        verify { mockDirectory.list() }
    }

    @Test
    fun `deleteImageFromStorage should handle null push ID from intent`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns null
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns arrayOf("image_null_123.jpg", "other_file.jpg")

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) }
        verify { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) }
        verify { mockDirectory.list() }
    }

    @Test
    fun `deleteImageFromStorage should handle null file list gracefully`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val pushId = "test_push_123"
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns pushId
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns null

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) }
        verify { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) }
        verify { mockDirectory.list() }
    }

    @Test
    fun `deleteImageFromStorage should handle empty file list gracefully`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val pushId = "test_push_123"
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns pushId
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns arrayOf()

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) }
        verify { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) }
        verify { mockDirectory.list() }
    }


    @Test
    fun `deleteImageFromStorage should call getDir with correct parameters`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val pushId = "test_push_123"
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns pushId
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns arrayOf("test_file.jpg")

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) }
    }

    @Test
    fun `deleteImageFromStorage should get directory absolute path`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val pushId = "test_push_123"
        val expectedPath = "/data/data/com.test/files/push_templates"
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns pushId
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns expectedPath
        every { mockDirectory.list() } returns arrayOf("test_file.jpg")

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockDirectory.absolutePath }
    }

    @Test
    fun `deleteImageFromStorage should process file list when files exist`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val pushId = "test_push_123"
        val fileList = arrayOf("image_test_push_123.jpg", "other_file.jpg", "test_push_123_backup.png")
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns pushId
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns fileList

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockDirectory.list() }
        // Method should process all files in the list
    }

    @Test
    fun `deleteImageFromStorage should work with different push ID formats`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val pushId = "campaign_2024_spring_abc123"
        val fileList = arrayOf(
            "image_campaign_2024_spring_abc123.jpg",
            "unrelated_file.png"
        )
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns pushId
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns fileList

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) }
        verify { mockDirectory.list() }
    }

    @Test
    fun `deleteImageFromStorage should handle files with null pattern when push ID is null`() {
        // Given
        val mockContext = mockk<Context>()
        val mockAppContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val fileList = arrayOf(
            "image_null_123.jpg",
            "cache_null_456.png",
            "regular_file.jpg"
        )
        
        every { mockContext.applicationContext } returns mockAppContext
        every { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) } returns null
        
        mockkConstructor(ContextWrapper::class)
        val mockDirectory = mockk<File>()
        every { anyConstructed<ContextWrapper>().getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE) } returns mockDirectory
        every { mockDirectory.absolutePath } returns "/mock/path"
        every { mockDirectory.list() } returns fileList

        // When
        Utils.deleteImageFromStorage(mockContext, mockIntent)

        // Then
        verify { mockIntent.getStringExtra(Constants.WZRK_PUSH_ID) }
        verify { mockDirectory.list() }
        // Method should identify files containing "null" pattern
    }

    // Tests for deleteImageFromStorage method end

    // Tests for isNotificationChannelEnabled method

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `isNotificationChannelEnabled should return false when SDK version is below O`() {
        // Given
        val mockChannel = mockk<NotificationChannel>()

        // When
        val result = Utils.isNotificationChannelEnabled(mockChannel)

        // Then
        assertFalse(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `isNotificationChannelEnabled should return false when channel is null`() {
        // When
        val result = Utils.isNotificationChannelEnabled(null)

        // Then
        assertFalse(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `isNotificationChannelEnabled should return false when channel importance is NONE`() {
        // Given
        val mockChannel = mockk<NotificationChannel>()
        every { mockChannel.importance } returns NotificationManager.IMPORTANCE_NONE


        // When
        val result = Utils.isNotificationChannelEnabled(mockChannel)

        // Then
        assertFalse(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `isNotificationChannelEnabled should return true when channel importance is not NONE`() {
        // Given
        val mockChannel = mockk<NotificationChannel>()
        every { mockChannel.importance } returns NotificationManager.IMPORTANCE_HIGH

        // When
        val result = Utils.isNotificationChannelEnabled(mockChannel)

        // Then
        assertTrue(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `isNotificationChannelEnabled should return true for various importance levels`() {
        // Given
        val mockChannel = mockk<NotificationChannel>()

        val importanceLevels = listOf(
            NotificationManager.IMPORTANCE_MIN,
            NotificationManager.IMPORTANCE_LOW,
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationManager.IMPORTANCE_HIGH,
            NotificationManager.IMPORTANCE_MAX
        )

        importanceLevels.forEach { importance ->
            // Given
            every { mockChannel.importance } returns importance

            // When
            val result = Utils.isNotificationChannelEnabled(mockChannel)

            // Then
            assertTrue("Should return true for importance level $importance", result)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `isNotificationChannelEnabled should handle SDK version exactly at O`() {
        // Given
        val mockChannel = mockk<NotificationChannel>()
        every { mockChannel.importance } returns NotificationManager.IMPORTANCE_DEFAULT

        // When
        val result = Utils.isNotificationChannelEnabled(mockChannel)

        // Then
        assertTrue(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `isNotificationChannelEnabled should handle SDK version above O`() {
        // Given
        val mockChannel = mockk<NotificationChannel>()
        every { mockChannel.importance } returns NotificationManager.IMPORTANCE_DEFAULT

        // When
        val result = Utils.isNotificationChannelEnabled(mockChannel)

        // Then
        assertTrue(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `isNotificationChannelEnabled should return false when both conditions fail`() {
        // Given
        val mockChannel = mockk<NotificationChannel>()
        every { mockChannel.importance } returns NotificationManager.IMPORTANCE_NONE

        // When
        val result = Utils.isNotificationChannelEnabled(mockChannel)

        // Then
        assertFalse(result)
    }

    // Tests for isNotificationChannelEnabled method end

    // Tests for isNotificationInTray method

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `isNotificationInTray should return true when notification with matching ID exists`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val mockStatusBarNotification3 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2, mockStatusBarNotification3)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.id } returns 456
        every { mockStatusBarNotification2.id } returns targetNotificationId
        every { mockStatusBarNotification3.id } returns 789

        // When
        val result = Utils.isNotificationInTray(mockContext, targetNotificationId)

        // Then
        assertTrue(result)
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
        verify { mockNotificationManager.activeNotifications }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `isNotificationInTray should return false when notification with matching ID does not exist`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.id } returns 456
        every { mockStatusBarNotification2.id } returns 789

        // When
        val result = Utils.isNotificationInTray(mockContext, targetNotificationId)

        // Then
        assertFalse(result)
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
        verify { mockNotificationManager.activeNotifications }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `isNotificationInTray should return false when no notifications exist`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val notifications = arrayOf<StatusBarNotification>()
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications

        // When
        val result = Utils.isNotificationInTray(mockContext, targetNotificationId)

        // Then
        assertFalse(result)
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
        verify { mockNotificationManager.activeNotifications }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `isNotificationInTray should return true when first notification matches`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.id } returns targetNotificationId
        every { mockStatusBarNotification2.id } returns 789

        // When
        val result = Utils.isNotificationInTray(mockContext, targetNotificationId)

        // Then
        assertTrue(result)
        verify { mockStatusBarNotification1.id }
        // Should not check second notification since first matches
        verify(exactly = 0) { mockStatusBarNotification2.id }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `isNotificationInTray should handle negative notification IDs`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = -1
        val mockStatusBarNotification = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification.id } returns targetNotificationId

        // When
        val result = Utils.isNotificationInTray(mockContext, targetNotificationId)

        // Then
        assertTrue(result)
    }

    // Tests for getNotificationById method

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationById should return notification when matching ID exists`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val mockNotification = mockk<Notification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.id } returns 456
        every { mockStatusBarNotification2.id } returns targetNotificationId
        every { mockStatusBarNotification2.notification } returns mockNotification

        // When
        val result = Utils.getNotificationById(mockContext, targetNotificationId)

        // Then
        assertEquals(mockNotification, result)
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
        verify { mockNotificationManager.activeNotifications }
        verify { mockStatusBarNotification2.notification }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationById should return null when matching ID does not exist`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.id } returns 456
        every { mockStatusBarNotification2.id } returns 789

        // When
        val result = Utils.getNotificationById(mockContext, targetNotificationId)

        // Then
        assertNull(result)
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
        verify { mockNotificationManager.activeNotifications }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationById should return null when no notifications exist`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val notifications = arrayOf<StatusBarNotification>()
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications

        // When
        val result = Utils.getNotificationById(mockContext, targetNotificationId)

        // Then
        assertNull(result)
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
        verify { mockNotificationManager.activeNotifications }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationById should return first matching notification when multiple match`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 123
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val mockNotification1 = mockk<Notification>()
        val mockNotification2 = mockk<Notification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.id } returns targetNotificationId
        every { mockStatusBarNotification2.id } returns targetNotificationId
        every { mockStatusBarNotification1.notification } returns mockNotification1
        every { mockStatusBarNotification2.notification } returns mockNotification2

        // When
        val result = Utils.getNotificationById(mockContext, targetNotificationId)

        // Then
        assertEquals(mockNotification1, result) // Should return first match
        verify { mockStatusBarNotification1.notification }
        verify(exactly = 0) { mockStatusBarNotification2.notification } // Should not call second
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationById should handle zero notification ID`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val targetNotificationId = 0
        val mockStatusBarNotification = mockk<StatusBarNotification>()
        val mockNotification = mockk<Notification>()
        val notifications = arrayOf(mockStatusBarNotification)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification.id } returns targetNotificationId
        every { mockStatusBarNotification.notification } returns mockNotification

        // When
        val result = Utils.getNotificationById(mockContext, targetNotificationId)

        // Then
        assertEquals(mockNotification, result)
    }

    // Tests for getNotificationIds method

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationIds should return matching package notifications when SDK is M or above`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val packageName = "com.test.app"
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val mockStatusBarNotification3 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2, mockStatusBarNotification3)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockContext.packageName } returns packageName
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.packageName } returns packageName
        every { mockStatusBarNotification1.id } returns 123
        every { mockStatusBarNotification2.packageName } returns "com.other.app"
        every { mockStatusBarNotification2.id } returns 456
        every { mockStatusBarNotification3.packageName } returns packageName
        every { mockStatusBarNotification3.id } returns 789


        // When
        val result = Utils.getNotificationIds(mockContext)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains(123))
        assertFalse(result.contains(456)) // Different package
        assertTrue(result.contains(789))
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
        verify { mockContext.packageName }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationIds should return empty list when no notifications exist`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val notifications = arrayOf<StatusBarNotification>()
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns notifications

        // When
        val result = Utils.getNotificationIds(mockContext)

        // Then
        assertTrue(result.isEmpty())
        verify { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationIds should return empty list when no notifications match package`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val packageName = "com.test.app"
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockContext.packageName } returns packageName
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.packageName } returns "com.other.app"
        every { mockStatusBarNotification2.packageName } returns "com.another.app"


        // When
        val result = Utils.getNotificationIds(mockContext)

        // Then
        assertTrue(result.isEmpty())
        verify { mockContext.packageName }
        verify { mockStatusBarNotification1.packageName }
        verify { mockStatusBarNotification2.packageName }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationIds should handle case insensitive package name comparison`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val packageName = "com.test.app"
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockContext.packageName } returns packageName
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.packageName } returns "COM.TEST.APP" // Different case
        every { mockStatusBarNotification1.id } returns 123
        every { mockStatusBarNotification2.packageName } returns "com.Test.App" // Mixed case
        every { mockStatusBarNotification2.id } returns 456

        // When
        val result = Utils.getNotificationIds(mockContext)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains(123))
        assertTrue(result.contains(456))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationIds should handle SDK version exactly at M`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val packageName = "com.test.app"
        val mockStatusBarNotification = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockContext.packageName } returns packageName
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification.packageName } returns packageName
        every { mockStatusBarNotification.id } returns 123


        // When
        val result = Utils.getNotificationIds(mockContext)

        // Then
        assertEquals(1, result.size)
        assertTrue(result.contains(123))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `getNotificationIds should handle SDK version above M`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val packageName = "com.test.app"
        val mockStatusBarNotification = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockContext.packageName } returns packageName
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification.packageName } returns packageName
        every { mockStatusBarNotification.id } returns 123

        // When
        val result = Utils.getNotificationIds(mockContext)

        // Then
        assertEquals(1, result.size)
        assertTrue(result.contains(123))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getNotificationIds should handle duplicate notification IDs`() {
        // Given
        val mockContext = mockk<Context>()
        val mockNotificationManager = mockk<NotificationManager>()
        val packageName = "com.test.app"
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        val notifications = arrayOf(mockStatusBarNotification1, mockStatusBarNotification2)
        
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockContext.packageName } returns packageName
        every { mockNotificationManager.activeNotifications } returns notifications
        every { mockStatusBarNotification1.packageName } returns packageName
        every { mockStatusBarNotification1.id } returns 123
        every { mockStatusBarNotification2.packageName } returns packageName
        every { mockStatusBarNotification2.id } returns 123 // Same ID

        // When
        val result = Utils.getNotificationIds(mockContext)

        // Then
        assertEquals(2, result.size) // Both should be added (method doesn't deduplicate)
        assertTrue(result.contains(123))
        assertEquals(123, result[0])
        assertEquals(123, result[1])
    }

    // Tests for notification methods end

    // Tests for getTimerThreshold method

    @Test
    fun `getTimerThreshold should return default -1 when no timer threshold key exists`() {
        // Given
        val keys = setOf("random_key", "other_key")
        every { mockBundle.keySet() } returns keys

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `getTimerThreshold should return parsed value when exact PT_TIMER_THRESHOLD key exists`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD, "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns "30"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(30, result)
        verify { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) }
    }

    @Test
    fun `getTimerThreshold should return parsed value when key contains PT_TIMER_THRESHOLD`() {
        // Given
        val timerKey = PTConstants.PT_TIMER_THRESHOLD + "_custom"
        val keys = setOf(timerKey, "random_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(timerKey) } returns "60"
        every { mockBundle.getString("random_key") } returns "ignored"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(60, result)
        verify { mockBundle.getString(timerKey) }
    }

    @Test
    fun `getTimerThreshold should return last found value when multiple timer threshold keys exist`() {
        // Given
        val timerKey1 = PTConstants.PT_TIMER_THRESHOLD + "1"
        val timerKey2 = PTConstants.PT_TIMER_THRESHOLD + "2" 
        val keys = setOf(timerKey1, timerKey2)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(timerKey1) } returns "25"
        every { mockBundle.getString(timerKey2) } returns "45"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        // Note: Result depends on keySet iteration order, but should be one of the valid values
        assertTrue("Result should be one of the timer threshold values", result == 25 || result == 45)
    }

    @Test
    fun `getTimerThreshold should return -1 when timer threshold value is null`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns null

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(-1, result)
    }


    @Test
    fun `getTimerThreshold should return parsed value for negative numbers`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns "-100"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(-100, result)
    }

    @Test
    fun `getTimerThreshold should return parsed value for zero`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns "0"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `getTimerThreshold should return parsed value for large positive numbers`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns "999999"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(999999, result)
    }


    @Test
    fun `getTimerThreshold should handle edge case of Integer MIN_VALUE`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns Integer.MIN_VALUE.toString()

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(Integer.MIN_VALUE, result)
    }

    @Test
    fun `getTimerThreshold should handle edge case of Integer MAX_VALUE`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns Integer.MAX_VALUE.toString()

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(Integer.MAX_VALUE, result)
    }

    @Test
    fun `getTimerThreshold should find timer threshold key with prefix`() {
        // Given
        val timerKey = "custom_" + PTConstants.PT_TIMER_THRESHOLD
        val keys = setOf(timerKey, "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(timerKey) } returns "75"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(75, result)
    }

    @Test
    fun `getTimerThreshold should find timer threshold key anywhere in the key name`() {
        // Given
        val timerKey = "prefix_" + PTConstants.PT_TIMER_THRESHOLD + "_suffix"
        val keys = setOf(timerKey, "random_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(timerKey) } returns "120"
        every { mockBundle.getString("random_key") } returns "ignored"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(120, result)
    }

    @Test
    fun `getTimerThreshold should return -1 when key contains partial match but not full PT_TIMER_THRESHOLD`() {
        // Given
        val partialKey = "pt_timer_thresh" // Missing 'old' part
        val keys = setOf(partialKey, "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(partialKey) } returns "50"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `getTimerThreshold should verify correct method calls`() {
        // Given
        val keys = setOf(PTConstants.PT_TIMER_THRESHOLD, "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) } returns "40"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        verify(exactly = 1) { mockBundle.keySet() }
        verify(exactly = 1) { mockBundle.getString(PTConstants.PT_TIMER_THRESHOLD) }
        verify(exactly = 0) { mockBundle.getString("other_key") }
        assertEquals(40, result)
    }

    @Test
    fun `getTimerThreshold should handle mixed case in key name`() {
        // Given
        val timerKey = PTConstants.PT_TIMER_THRESHOLD.uppercase() // Different case
        val keys = setOf(timerKey)
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(timerKey) } returns "85"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        // Should not match because contains() is case-sensitive
        assertEquals(-1, result)
    }

    @Test
    fun `getTimerThreshold should process multiple keys but only check those containing timer threshold`() {
        // Given
        val timerKey = PTConstants.PT_TIMER_THRESHOLD + "_test"
        val keys = setOf("key1", "key2", timerKey, "key3", "key4")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString(timerKey) } returns "95"
        every { mockBundle.getString("key1") } returns "value1"
        every { mockBundle.getString("key2") } returns "value2"
        every { mockBundle.getString("key3") } returns "value3"
        every { mockBundle.getString("key4") } returns "value4"

        // When
        val result = Utils.getTimerThreshold(mockBundle)

        // Then
        assertEquals(95, result)
        verify(exactly = 1) { mockBundle.getString(timerKey) }
        // Should not call getString on keys that don't contain PT_TIMER_THRESHOLD
        verify(exactly = 0) { mockBundle.getString("key1") }
        verify(exactly = 0) { mockBundle.getString("key2") }
        verify(exactly = 0) { mockBundle.getString("key3") }
        verify(exactly = 0) { mockBundle.getString("key4") }
    }

    // Tests for getTimerThreshold method end

    // Tests for getCTAListFromExtras method

    @Test
    fun `getCTAListFromExtras should return list of CTA values`() {
        // Given
        val keys = setOf("pt_cta1", "pt_cta_action", "other_key")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString("pt_cta1") } returns "Buy Now"
        every { mockBundle.getString("pt_cta_action") } returns "Learn More"
        every { mockBundle.getString("other_key") } returns "ignored"

        // When
        val result = Utils.getCTAListFromExtras(mockBundle)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("Buy Now"))
        assertTrue(result.contains("Learn More"))
        assertFalse(result.contains("ignored"))
    }

    @Test
    fun `getCTAListFromExtras should return empty list when no CTA keys exist`() {
        // Given
        val keys = setOf("random_key")
        every { mockBundle.keySet() } returns keys

        // When
        val result = Utils.getCTAListFromExtras(mockBundle)

        // Then
        assertTrue(result.isEmpty())
    }

    // Tests for getDeepLinkListFromExtras method

    @Test
    fun `getDeepLinkListFromExtras should return list of deep link URLs`() {
        // Given
        val keys = setOf("pt_dl1", "pt_dl_main", "unrelated")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString("pt_dl1") } returns "myapp://action1"
        every { mockBundle.getString("pt_dl_main") } returns "https://myapp.com/main"
        every { mockBundle.getString("unrelated") } returns "ignored"

        // When
        val result = Utils.getDeepLinkListFromExtras(mockBundle)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("myapp://action1"))
        assertTrue(result.contains("https://myapp.com/main"))
        assertFalse(result.contains("ignored"))
    }

    // Tests for getBigTextFromExtras method

    @Test
    fun `getBigTextFromExtras should return list of big text values`() {
        // Given
        val keys = setOf("pt_bt1", "pt_bt_title", "other")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString("pt_bt1") } returns "Big Title 1"
        every { mockBundle.getString("pt_bt_title") } returns "Main Big Title"
        every { mockBundle.getString("other") } returns "ignored"

        // When
        val result = Utils.getBigTextFromExtras(mockBundle)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("Big Title 1"))
        assertTrue(result.contains("Main Big Title"))
        assertFalse(result.contains("ignored"))
    }

    // Tests for getSmallTextFromExtras method

    @Test
    fun `getSmallTextFromExtras should return list of small text values`() {
        // Given
        val keys = setOf("pt_st1", "pt_st_subtitle", "random")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString("pt_st1") } returns "Small text 1"
        every { mockBundle.getString("pt_st_subtitle") } returns "Subtitle text"
        every { mockBundle.getString("random") } returns "ignored"

        // When
        val result = Utils.getSmallTextFromExtras(mockBundle)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("Small text 1"))
        assertTrue(result.contains("Subtitle text"))
        assertFalse(result.contains("ignored"))
    }

    // Tests for getPriceFromExtras method

    @Test
    fun `getPriceFromExtras should return price values but exclude price_list`() {
        // Given
        val keys = setOf("pt_price1", "pt_price_main", "pt_price_list", "other")
        every { mockBundle.keySet() } returns keys
        every { mockBundle.getString("pt_price1") } returns "$19.99"
        every { mockBundle.getString("pt_price_main") } returns "$29.99"
        every { mockBundle.getString("pt_price_list") } returns "should_be_excluded"
        every { mockBundle.getString("other") } returns "ignored"

        // When
        val result = Utils.getPriceFromExtras(mockBundle)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("$19.99"))
        assertTrue(result.contains("$29.99"))
        assertFalse(result.contains("should_be_excluded"))
        assertFalse(result.contains("ignored"))
    }

    @Test
    fun `getPriceFromExtras should return empty list when only price_list keys exist`() {
        // Given
        val keys = setOf("pt_price_list", "other_key")
        every { mockBundle.keySet() } returns keys

        // When
        val result = Utils.getPriceFromExtras(mockBundle)

        // Then
        assertTrue(result.isEmpty())
    }
}
