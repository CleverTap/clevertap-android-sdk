package com.clevertap.android.pushtemplates

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.network.DownloadedBitmap
import io.mockk.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale
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
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val configuration = mockk<Configuration>()
        val contentResolver = mockk<ContentResolver>()
        configuration.locale = Locale.ENGLISH

        every { context.resources } returns resources
        every { context.contentResolver } returns contentResolver
        every { resources.configuration } returns configuration

        // When
        val result = Utils.getTimeStamp(context, 1749033690811L)

        // Then
        assertEquals("4:11 PM", result)
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
        val currentTimestamp = 15000L
        // Given
        every { mockBundle.keySet() } returns setOf(PTConstants.PT_TIMER_END)
        every { mockBundle.getString(PTConstants.PT_TIMER_END) } returns "-1"

        // When
        val result = Utils.getTimerEnd(mockBundle, currentTimestamp)

        // Then
        assertEquals(Integer.MIN_VALUE, result)
    }

    @Test
    fun `getTimerEnd should calculate difference when valid timestamp provided`() {
        val futureTimestamp = 20L
        val currentTimestamp = 15000L
        every { mockBundle.keySet() } returns setOf(PTConstants.PT_TIMER_END)
        every { mockBundle.getString(PTConstants.PT_TIMER_END) } returns futureTimestamp.toString()

        // When
        val result = Utils.getTimerEnd(mockBundle, currentTimestamp)

        // Then
        assertEquals(5, result)
    }

    @Test
    fun `getTimerEnd should handle formatted timestamp with D_ prefix`() {
        // Given
        val futureTimestamp = 20L
        val currentTimestamp = 15000L
        val formattedValue = "\$D_$futureTimestamp"
        every { mockBundle.keySet() } returns setOf(PTConstants.PT_TIMER_END)
        every { mockBundle.getString(PTConstants.PT_TIMER_END) } returns formattedValue

        // When
        val result = Utils.getTimerEnd(mockBundle, currentTimestamp)

        // Then
        assertEquals(5, result)
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

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
            every { mockInstance.pushNotificationClickedEvent(mockBundle) } just Runs

            Utils.raiseNotificationClicked(mockContext, mockBundle, mockConfig)

            // Then
            verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
            verify { mockInstance.pushNotificationClickedEvent(mockBundle) }
            verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any()) }
        }
    }

    @Test
    fun `raiseNotificationClicked should use getDefaultInstance when config is null`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
            every { mockInstance.pushNotificationClickedEvent(mockBundle) } just Runs

            Utils.raiseNotificationClicked(mockContext, mockBundle, null)

            // Then
            verify { CleverTapAPI.getDefaultInstance(mockContext) }
            verify { mockInstance.pushNotificationClickedEvent(mockBundle) }
            verify(exactly = 0) { CleverTapAPI.instanceWithConfig(any(), any()) }
        }
    }

    @Test
    fun `raiseNotificationClicked should not call pushNotificationClickedEvent when instance is null with config`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns null

            Utils.raiseNotificationClicked(mockContext, mockBundle, mockConfig)

            // Then
            verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
            // No verification for pushNotificationClickedEvent as instance is null
        }
    }

    @Test
    fun `raiseNotificationClicked should not call pushNotificationClickedEvent when instance is null without config`() {
        // Given
        val mockContext = mockk<Context>()

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns null

            Utils.raiseNotificationClicked(mockContext, mockBundle, null)

            // Then
            verify { CleverTapAPI.getDefaultInstance(mockContext) }
            // No verification for pushNotificationClickedEvent as instance is null
        }
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

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
            every { mockInstance.pushEvent(eventName, eventProps) } just Runs

            Utils.raiseCleverTapEvent(mockContext, mockConfig, eventName, eventProps)

            // Then
            verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
            verify { mockInstance.pushEvent(eventName, eventProps) }
            verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any()) }
        }
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

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
            every { mockInstance.pushEvent(eventName, eventProps) } just Runs

            Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

            // Then
            verify { CleverTapAPI.getDefaultInstance(mockContext) }
            verify { mockInstance.pushEvent(eventName, eventProps) }
            verify(exactly = 0) { CleverTapAPI.instanceWithConfig(any(), any()) }
        }
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

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns null

            Utils.raiseCleverTapEvent(mockContext, mockConfig, eventName, eventProps)

            // Then
            verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
            // No verification for pushEvent as instance is null
        }
    }

    @Test
    fun `raiseCleverTapEvent should not call pushEvent when instance is null without config`() {
        // Given
        val mockContext = mockk<Context>()
        val eventName = "test_event"
        val eventProps = hashMapOf<String, Any>().apply {
            put("key1", "value1")
        }

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns null

            Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

            // Then
            verify { CleverTapAPI.getDefaultInstance(mockContext) }
            // No verification for pushEvent as instance is null
        }
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

        // When
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
            every { mockInstance.pushEvent(any(), any()) } just Runs

            Utils.raiseCleverTapEvent(mockContext, mockConfig, null, eventProps)

            // Then
            verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
            verify(exactly = 0) { mockInstance.pushEvent(any(), any()) }
        }
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
        
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
            every { mockInstance.pushEvent(any(), any()) } just Runs

            // When
            Utils.raiseCleverTapEvent(mockContext, mockConfig, "", eventProps)

            // Then
            verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
            verify(exactly = 0) { mockInstance.pushEvent(any(), any()) }
        }
    }

    @Test
    fun `raiseCleverTapEvent should call pushEvent with empty properties when eventProps is empty`() {
        // Given
        val mockContext = mockk<Context>()
        val mockConfig = mockk<CleverTapInstanceConfig>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "test_event"
        val eventProps = hashMapOf<String, Any>()
        
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) } returns mockInstance
            every { mockInstance.pushEvent(eventName, eventProps) } just Runs

            // When
            Utils.raiseCleverTapEvent(mockContext, mockConfig, eventName, eventProps)

            // Then
            verify { CleverTapAPI.instanceWithConfig(mockContext, mockConfig) }
            verify { mockInstance.pushEvent(eventName, eventProps) }
        }
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
        
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
            every { mockInstance.pushEvent(any(), any()) } just Runs

            // When
            Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

            // Then
            verify { CleverTapAPI.getDefaultInstance(mockContext) }
            // Should call pushEvent because whitespace is not considered empty by isEmpty()
            verify { mockInstance.pushEvent(eventName, eventProps) }
        }
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
        
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
            every { mockInstance.pushEvent(eventName, eventProps) } just Runs

            // When
            Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

            // Then
            verify { CleverTapAPI.getDefaultInstance(mockContext) }
            verify { mockInstance.pushEvent(eventName, eventProps) }
        }
    }

    @Test
    fun `raiseCleverTapEvent should handle null event properties gracefully`() {
        // Given
        val mockContext = mockk<Context>()
        val mockInstance = mockk<CleverTapAPI>()
        val eventName = "test_event"
        val eventProps: HashMap<String, Any>? = null
        
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
            every { mockInstance.pushEvent(eventName, eventProps) } just Runs

            // When
            Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

            // Then
            verify { CleverTapAPI.getDefaultInstance(mockContext) }
            verify { mockInstance.pushEvent(eventName, eventProps) }
        }
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
        
        mockkStatic(CleverTapAPI::class) {
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
        
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDefaultInstance(mockContext) } returns mockInstance
            every { mockInstance.pushEvent(eventName, eventProps) } just Runs

            // When
            Utils.raiseCleverTapEvent(mockContext, null, eventName, eventProps)

            // Then
            verify { mockInstance.pushEvent(eventName, eventProps) }
        }
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

    // Tests for fromJson method

    @Test
    fun `fromJson should return empty bundle when JSONObject is empty`() {
        // Given
        val jsonObject = JSONObject()

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertNotNull(result)
        assertTrue(result.keySet().isEmpty())
    }

    @Test
    fun `fromJson should convert string values to bundle strings`() {
        // Given
        val jsonString = """
            {
                "title": "Test Title",
                "message": "Test Message",
                "id": "123"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(3, result.keySet().size)
        assertEquals("Test Title", result.getString("title"))
        assertEquals("Test Message", result.getString("message"))
        assertEquals("123", result.getString("id"))
    }

    @Test
    fun `fromJson should convert non-empty JSONArray to string array`() {
        // Given
        val jsonString = """
            {
                "images": ["image1.jpg", "image2.jpg", "image3.jpg"],
                "tags": ["tag1", "tag2"]
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(2, result.keySet().size)
        
        val images = result.getStringArray("images")
        assertNotNull(images)
        assertEquals(3, images!!.size)
        assertEquals("image1.jpg", images[0])
        assertEquals("image2.jpg", images[1])
        assertEquals("image3.jpg", images[2])
        
        val tags = result.getStringArray("tags")
        assertNotNull(tags)
        assertEquals(2, tags!!.size)
        assertEquals("tag1", tags[0])
        assertEquals("tag2", tags[1])
    }

    @Test
    fun `fromJson should convert empty JSONArray to empty string array`() {
        // Given
        val jsonString = """
            {
                "empty_array": [],
                "another_empty": []
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(2, result.keySet().size)
        
        val emptyArray1 = result.getStringArray("empty_array")
        assertNotNull(emptyArray1)
        assertEquals(0, emptyArray1!!.size)
        
        val emptyArray2 = result.getStringArray("another_empty")
        assertNotNull(emptyArray2)
        assertEquals(0, emptyArray2!!.size)
    }

    @Test
    fun `fromJson should handle mixed string and array values`() {
        // Given
        val jsonString = """
            {
                "title": "Test Title",
                "images": ["img1.jpg", "img2.jpg"],
                "description": "Test Description",
                "tags": ["tag1"],
                "id": "456"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(5, result.keySet().size)
        
        assertEquals("Test Title", result.getString("title"))
        assertEquals("Test Description", result.getString("description"))
        assertEquals("456", result.getString("id"))
        
        val images = result.getStringArray("images")
        assertNotNull(images)
        assertEquals(2, images!!.size)
        assertEquals("img1.jpg", images[0])
        assertEquals("img2.jpg", images[1])
        
        val tags = result.getStringArray("tags")
        assertNotNull(tags)
        assertEquals(1, tags!!.size)
        assertEquals("tag1", tags[0])
    }

    @Test
    fun `fromJson should handle JSONArray with null elements`() {
        // Given
        val jsonObject = JSONObject()
        val jsonArray = JSONArray()
        jsonArray.put("item1")
        jsonArray.put(JSONObject.NULL)
        jsonArray.put("item3")
        jsonObject.put("mixed_array", jsonArray)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(1, result.keySet().size)
        
        val mixedArray = result.getStringArray("mixed_array")
        assertNotNull(mixedArray)
        assertEquals(3, mixedArray!!.size)
        assertEquals("item1", mixedArray[0])
        assertEquals("null", mixedArray[1]) // JSONObject.NULL.toString() returns "null"
        assertEquals("item3", mixedArray[2])
    }

    @Test
    fun `fromJson should handle single element JSONArray`() {
        // Given
        val jsonString = """
            {
                "single_item": ["only_item"]
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(1, result.keySet().size)
        
        val singleArray = result.getStringArray("single_item")
        assertNotNull(singleArray)
        assertEquals(1, singleArray!!.size)
        assertEquals("only_item", singleArray[0])
    }

    @Test
    fun `fromJson should handle empty string values`() {
        // Given
        val jsonString = """
            {
                "empty_string": "",
                "normal_string": "content"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(2, result.keySet().size)
        assertEquals("", result.getString("empty_string"))
        assertEquals("content", result.getString("normal_string"))
    }

    @Test
    fun `fromJson should handle numeric values as strings`() {
        // Given
        val jsonString = """
            {
                "number": 123,
                "float": 45.67,
                "boolean": true
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(3, result.keySet().size)
        assertEquals("123", result.getString("number"))
        assertEquals("45.67", result.getString("float"))
        assertEquals("true", result.getString("boolean"))
    }

    @Test
    fun `fromJson should handle JSONArray with numeric values`() {
        // Given
        val jsonString = """
            {
                "numbers": [1, 2, 3],
                "mixed_types": ["text", 123, true, 45.67]
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(2, result.keySet().size)
        
        val numbers = result.getStringArray("numbers")
        assertNotNull(numbers)
        assertEquals(3, numbers!!.size)
        assertEquals("1", numbers[0])
        assertEquals("2", numbers[1])
        assertEquals("3", numbers[2])
        
        val mixedTypes = result.getStringArray("mixed_types")
        assertNotNull(mixedTypes)
        assertEquals(4, mixedTypes!!.size)
        assertEquals("text", mixedTypes[0])
        assertEquals("123", mixedTypes[1])
        assertEquals("true", mixedTypes[2])
        assertEquals("45.67", mixedTypes[3])
    }

    @Test
    fun `fromJson should handle special characters in strings`() {
        // Given
        val jsonString = """
            {
                "special_chars": "Hello\nWorld\t!",
                "unicode": "café\u00A9",
                "symbols": "@#$%^&*()"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(3, result.keySet().size)
        assertEquals("Hello\nWorld\t!", result.getString("special_chars"))
        assertEquals("café©", result.getString("unicode"))
        assertEquals("@#$%^&*()", result.getString("symbols"))
    }

    @Test
    fun `fromJson should handle nested JSONObject as string`() {
        // Given
        val jsonString = """
            {
                "nested": {"inner": "value"},
                "normal": "string"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(2, result.keySet().size)
        assertEquals("string", result.getString("normal"))
        // Nested object should be converted to its string representation
        val nestedString = result.getString("nested")
        assertTrue(nestedString!!.contains("inner"))
        assertTrue(nestedString.contains("value"))
    }

    @Test
    fun `fromJson should handle large JSONArray`() {
        // Given
        val jsonObject = JSONObject()
        val largeArray = JSONArray()
        for (i in 0 until 100) {
            largeArray.put("item_$i")
        }
        jsonObject.put("large_array", largeArray)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(1, result.keySet().size)
        
        val resultArray = result.getStringArray("large_array")
        assertNotNull(resultArray)
        assertEquals(100, resultArray!!.size)
        assertEquals("item_0", resultArray[0])
        assertEquals("item_50", resultArray[50])
        assertEquals("item_99", resultArray[99])
    }

    @Test
    fun `fromJson should handle keys with special characters`() {
        // Given
        val jsonString = """
            {
                "key-with-dashes": "value1",
                "key_with_underscores": "value2",
                "key.with.dots": "value3",
                "key with spaces": "value4"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(4, result.keySet().size)
        assertEquals("value1", result.getString("key-with-dashes"))
        assertEquals("value2", result.getString("key_with_underscores"))
        assertEquals("value3", result.getString("key.with.dots"))
        assertEquals("value4", result.getString("key with spaces"))
    }

    @Test
    fun `fromJson should handle JSONArray with empty strings`() {
        // Given
        val jsonString = """
            {
                "array_with_empty": ["", "content", "", "more_content"]
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(1, result.keySet().size)
        
        val arrayWithEmpty = result.getStringArray("array_with_empty")
        assertNotNull(arrayWithEmpty)
        assertEquals(4, arrayWithEmpty!!.size)
        assertEquals("", arrayWithEmpty[0])
        assertEquals("content", arrayWithEmpty[1])
        assertEquals("", arrayWithEmpty[2])
        assertEquals("more_content", arrayWithEmpty[3])
    }

    @Test
    fun `fromJson should handle very long strings`() {
        // Given
        val longString = "a".repeat(10000)
        val jsonString = """
            {
                "long_string": "$longString"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(1, result.keySet().size)
        assertEquals(longString, result.getString("long_string"))
        assertEquals(10000, result.getString("long_string")?.length)
    }

    @Test
    fun `fromJson should preserve order independence`() {
        // Given
        val jsonString1 = """
            {
                "a": "value_a",
                "b": ["item1", "item2"],
                "c": "value_c"
            }
        """.trimIndent()
        val jsonString2 = """
            {
                "c": "value_c",
                "a": "value_a",
                "b": ["item1", "item2"]
            }
        """.trimIndent()

        // When
        val result1 = Utils.fromJson(JSONObject(jsonString1))
        val result2 = Utils.fromJson(JSONObject(jsonString2))

        // Then
        assertEquals(3, result1.keySet().size)
        assertEquals(3, result2.keySet().size)
        
        assertEquals(result1.getString("a"), result2.getString("a"))
        assertEquals(result1.getString("c"), result2.getString("c"))
        
        val array1 = result1.getStringArray("b")
        val array2 = result2.getStringArray("b")
        assertNotNull(array1)
        assertNotNull(array2)
        assertEquals(array1!!.size, array2!!.size)
        for (i in array1.indices) {
            assertEquals(array1[i], array2[i])
        }
    }

    @Test
    fun `fromJson should handle real-world notification payload`() {
        // Given
        val jsonString = """
            {
                "pt_id": "campaign_123",
                "pt_title": "Special Offer",
                "pt_msg": "Don't miss out on this deal!",
                "pt_img": ["image1.jpg", "image2.jpg"],
                "pt_cta": ["Buy Now", "Learn More"],
                "pt_deeplink": ["myapp://buy", "myapp://info"],
                "pt_timer_end": "1640995200",
                "pt_bg": "#FF0000"
            }
        """.trimIndent()
        val jsonObject = JSONObject(jsonString)

        // When
        val result = Utils.fromJson(jsonObject)

        // Then
        assertEquals(8, result.keySet().size)
        
        // String values
        assertEquals("campaign_123", result.getString("pt_id"))
        assertEquals("Special Offer", result.getString("pt_title"))
        assertEquals("Don't miss out on this deal!", result.getString("pt_msg"))
        assertEquals("1640995200", result.getString("pt_timer_end"))
        assertEquals("#FF0000", result.getString("pt_bg"))
        
        // Array values
        val images = result.getStringArray("pt_img")
        assertNotNull(images)
        assertEquals(2, images!!.size)
        assertEquals("image1.jpg", images[0])
        assertEquals("image2.jpg", images[1])
        
        val ctas = result.getStringArray("pt_cta")
        assertNotNull(ctas)
        assertEquals(2, ctas!!.size)
        assertEquals("Buy Now", ctas[0])
        assertEquals("Learn More", ctas[1])
        
        val deeplinks = result.getStringArray("pt_deeplink")
        assertNotNull(deeplinks)
        assertEquals(2, deeplinks!!.size)
        assertEquals("myapp://buy", deeplinks[0])
        assertEquals("myapp://info", deeplinks[1])
    }

    // Tests for fromJson method end

    // Tests for loadImageURLIntoRemoteView method

    @Test
    fun `loadImageURLIntoRemoteView should set bitmap when image is successfully downloaded`() {
        // Given
        val imageViewID = 123
        val imageUrl = "https://example.com/image.jpg"
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()
        val mockBitmap = mockk<Bitmap>()

        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockDownloadedBitmap

            mockkStatic(PTLog::class) {
                every { PTLog.verbose(any()) } just Runs

                mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                    every { Utils.setFallback(any()) } just Runs

                    // Execute the method under test
                    Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)

                    // Verify the interactions
                    verify { Utils.setFallback(false) }
                    verify { mockRemoteViews.setImageViewBitmap(imageViewID, mockBitmap) }
                    verify { PTLog.verbose(match { it.contains("Fetched IMAGE") && it.contains(imageUrl) }) }
                    verify(exactly = 0) { Utils.setFallback(true) }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should set fallback when image download fails`() {
        // Given
        val imageViewID = 456
        val imageUrl = "https://example.com/invalid-image.jpg"
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()

        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.NO_NETWORK

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockDownloadedBitmap

            mockkStatic(PTLog::class) {
                every { PTLog.debug(any()) } just Runs

                mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                    every { Utils.setFallback(any()) } just Runs

                    // Execute the method under test
                    Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)

                    // Verify the interactions
                    verify { Utils.setFallback(false) }
                    verify { Utils.setFallback(true) }
                    verify { PTLog.debug(match { it.contains("Image was not perfect") && it.contains(imageUrl) }) }
                    verify(exactly = 0) { mockRemoteViews.setImageViewBitmap(any(), any()) }
                    verify(exactly = 0) { PTLog.verbose(any()) }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should handle null image URL gracefully`() {
        // Given
        val imageViewID = 789
        val imageUrl: String? = null
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()

        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.NO_NETWORK

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockDownloadedBitmap

            mockkStatic(PTLog::class) {
                every { PTLog.debug(any()) } just Runs

                mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                    every { Utils.setFallback(any()) } just Runs

                    Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)

                    verify { Utils.setFallback(false) }
                    verify { Utils.setFallback(true) }
                    verify { PTLog.debug(match { it.contains("Image was not perfect") && it.contains("null") }) }
                    verify(exactly = 0) { mockRemoteViews.setImageViewBitmap(any(), any()) }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should handle empty image URL`() {
        // Given
        val imageViewID = 101
        val imageUrl = ""
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()

        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.NO_NETWORK

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockDownloadedBitmap

            mockkStatic(PTLog::class) {
                every { PTLog.debug(any()) } just Runs

                mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                    every { Utils.setFallback(any()) } just Runs

                    Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)

                    verify { Utils.setFallback(false) }
                    verify { Utils.setFallback(true) }
                    verify { PTLog.debug(match { it.contains("Image was not perfect") && it.contains(imageUrl) }) }
                    verify(exactly = 0) { mockRemoteViews.setImageViewBitmap(any(), any()) }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should handle different image view IDs`() {
        // Given
        val imageViewIDs = listOf(1, 100, 999, -1, 0)
        val imageUrl = "https://example.com/test.jpg"
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()
        val mockBitmap = mockk<Bitmap>()

        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockDownloadedBitmap

            mockkStatic(PTLog::class) {
                every { PTLog.verbose(any()) } just Runs

                mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                    every { Utils.setFallback(any()) } just Runs

                    imageViewIDs.forEach { imageViewID ->
                        Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)
                        verify { mockRemoteViews.setImageViewBitmap(imageViewID, mockBitmap) }
                    }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should handle various image URL formats`() {
        // Given
        val imageViewID = 303
        val imageUrls = listOf(
            "https://example.com/image.jpg",
            "http://test.com/pic.png",
            "https://cdn.example.com/assets/image.gif",
            "https://example.com/image-with-dashes.jpg",
            "https://example.com/image_with_underscores.png"
        )
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()
        val mockBitmap = mockk<Bitmap>()

        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockDownloadedBitmap

            mockkStatic(PTLog::class) {
                every { PTLog.verbose(any()) } just Runs

                mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                    every { Utils.setFallback(any()) } just Runs

                    imageUrls.forEach { imageUrl ->
                        Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)
                        verify { mockRemoteViews.setImageViewBitmap(imageViewID, mockBitmap) }
                        verify { PTLog.verbose(match { it.contains(imageUrl) }) }
                    }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should call setFallback false at start regardless of outcome`() {
        // Given
        val imageViewID = 404
        val imageUrl = "https://example.com/image.jpg"
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()

        // When & Then
        mockkStatic("com.clevertap.android.pushtemplates.Utils") {
            every { Utils.setFallback(any()) } just Runs

            mockkStatic(PTLog::class) {
                every { PTLog.verbose(any()) } just Runs
                every { PTLog.debug(any()) } just Runs

                mockkStatic(HttpBitmapLoader::class) {
                    // Test with successful image download
                    val mockSuccessBitmap = mockk<DownloadedBitmap>()
                    every { mockSuccessBitmap.status } returns DownloadedBitmap.Status.SUCCESS
                    every { mockSuccessBitmap.bitmap } returns mockk<Bitmap>()
                    every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockSuccessBitmap

                    Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)
                    verify { Utils.setFallback(false) }

                    // Test with failed image download
                    val mockFailedBitmap = mockk<DownloadedBitmap>()
                    every { mockFailedBitmap.status } returns DownloadedBitmap.Status.NO_NETWORK
                    every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockFailedBitmap

                    Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)
                    verify(exactly = 2) { Utils.setFallback(false) } // Called twice
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should log correct messages for success and failure cases`() {
        // Given
        val imageViewID = 505
        val successUrl = "https://example.com/success.jpg"
        val failUrl = "https://example.com/fail.jpg"
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()
        val mockBitmap = mockk<Bitmap>()

        // When & Then
        mockkStatic("com.clevertap.android.pushtemplates.Utils") {
            every { Utils.setFallback(any()) } just Runs

            mockkStatic(PTLog::class) {
                every { PTLog.verbose(any()) } just Runs
                every { PTLog.debug(any()) } just Runs

                mockkStatic(HttpBitmapLoader::class) {
                    // Test success case
                    val mockSuccessBitmap = mockk<DownloadedBitmap>()
                    every { mockSuccessBitmap.status } returns DownloadedBitmap.Status.SUCCESS
                    every { mockSuccessBitmap.bitmap } returns mockBitmap
                    every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockSuccessBitmap

                    Utils.loadImageURLIntoRemoteView(imageViewID, successUrl, mockRemoteViews, mockContext)

                    verify { PTLog.verbose(match {
                        it.contains("Fetched IMAGE") &&
                                it.contains(successUrl) &&
                                it.contains("millis")
                    }) }
                    verify(exactly = 0) { PTLog.debug(any()) }

                    // Test failure case
                    val mockFailedBitmap = mockk<DownloadedBitmap>()
                    every { mockFailedBitmap.status } returns DownloadedBitmap.Status.NO_NETWORK
                    every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockFailedBitmap

                    Utils.loadImageURLIntoRemoteView(imageViewID, failUrl, mockRemoteViews, mockContext)

                    verify { PTLog.debug(match {
                        it.contains("Image was not perfect") &&
                                it.contains(failUrl) &&
                                it.contains("hiding image view")
                    }) }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should handle malformed URLs`() {
        // Given
        val imageViewID = 606
        val malformedUrls = listOf(
            "not-a-url",
            "://missing-protocol",
            "https://",
            "ftp://unsupported-protocol.com/image.jpg"
        )
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()

        val mockFailedBitmap = mockk<DownloadedBitmap>()
        every { mockFailedBitmap.status } returns DownloadedBitmap.Status.NO_NETWORK

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockFailedBitmap

            mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                every { Utils.setFallback(any()) } just Runs

                mockkStatic(PTLog::class) {
                    every { PTLog.debug(any()) } just Runs

                    malformedUrls.forEach { malformedUrl ->
                        Utils.loadImageURLIntoRemoteView(imageViewID, malformedUrl, mockRemoteViews, mockContext)
                        verify { Utils.setFallback(true) }
                        verify { PTLog.debug(match { it.contains(malformedUrl) }) }
                        verify(exactly = 0) { mockRemoteViews.setImageViewBitmap(any(), any()) }
                    }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should handle context being null`() {
        // Given
        val imageViewID = 808
        val imageUrl = "https://example.com/image.jpg"
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext: Context? = null

        val mockFailedBitmap = mockk<DownloadedBitmap>()
        every { mockFailedBitmap.status } returns DownloadedBitmap.Status.NO_NETWORK

        // When & Then
        mockkStatic(HttpBitmapLoader::class) {
            every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockFailedBitmap

            mockkStatic("com.clevertap.android.pushtemplates.Utils") {
                every { Utils.setFallback(any()) } just Runs

                mockkStatic(PTLog::class) {
                    every { PTLog.debug(any()) } just Runs

                    Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)

                    verify { Utils.setFallback(false) }
                    verify { Utils.setFallback(true) }
                    verify(exactly = 0) { mockRemoteViews.setImageViewBitmap(any(), any()) }
                }
            }
        }
    }

    @Test
    fun `loadImageURLIntoRemoteView should handle different bitmap download statuses`() {
        // Given
        val imageViewID = 909
        val imageUrl = "https://example.com/image.jpg"
        val mockRemoteViews = mockk<RemoteViews>(relaxed = true)
        val mockContext = mockk<Context>()

        val downloadStatuses = listOf(
            DownloadedBitmap.Status.NO_NETWORK,
            DownloadedBitmap.Status.SIZE_LIMIT_EXCEEDED,
            DownloadedBitmap.Status.DOWNLOAD_FAILED
        )

        // When & Then
        mockkStatic("com.clevertap.android.pushtemplates.Utils") {
            every { Utils.setFallback(any()) } just Runs

            mockkStatic(PTLog::class) {
                every { PTLog.verbose(any()) } just Runs
                every { PTLog.debug(any()) } just Runs

                mockkStatic(HttpBitmapLoader::class) {
                    downloadStatuses.forEach { status ->
                        val mockFailedBitmap = mockk<DownloadedBitmap>()
                        every { mockFailedBitmap.status } returns status
                        every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns mockFailedBitmap

                        Utils.loadImageURLIntoRemoteView(imageViewID, imageUrl, mockRemoteViews, mockContext)

                        verify { Utils.setFallback(true) }
                        verify { PTLog.debug(match { it.contains("Image was not perfect") }) }
                        verify(exactly = 0) { mockRemoteViews.setImageViewBitmap(any(), any()) }
                    }
                }
            }
        }
    }

    // Tests for loadImageURLIntoRemoteView method end

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
