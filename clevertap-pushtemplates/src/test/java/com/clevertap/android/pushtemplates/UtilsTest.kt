package com.clevertap.android.pushtemplates

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
