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
import java.util.*

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
    fun `getColourOrNull should return parsed color for valid hex color with hash`() {
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

}
