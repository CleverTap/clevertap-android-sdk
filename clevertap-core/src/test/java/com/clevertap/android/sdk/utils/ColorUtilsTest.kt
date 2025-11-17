package com.clevertap.android.sdk.utils

import org.junit.Test
import kotlin.test.assertEquals

class ColorUtilsTest {

    private val fallbackColor = "#FFFFFF"

    @Test
    fun `returns same color when valid hex color provided`() {
        val input = "#FF0000" // Red
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals("#FF0000", result)
    }

    @Test
    fun `returns same color when valid color with alpha provided`() {
        val input = "#80FF0000" // Semi-transparent red
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals("#80FF0000", result)
    }

    @Test
    fun `returns fallback when input is null`() {
        val input: String? = null
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun `returns fallback when input is blank`() {
        val input = ""
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun `returns fallback when input is invalid string`() {
        val input = "notacolor"
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun `returns fallback when hash missing in hex`() {
        val input = "FF0000"
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun `handles lowercase hex properly`() {
        val input = "#ff00ff" // Magenta lowercase
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals("#ff00ff", result)
    }

    @Test
    fun `returns default white when both input and fallback are null`() {
        val input: String? = null
        val fallback: String? = null
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#FFFFFF", result)
    }

    @Test
    fun `returns default white when both input and fallback are invalid`() {
        val input = "invalidColor"
        val fallback = "notAColor"
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#FFFFFF", result)
    }

    @Test
    fun `returns fallback when input invalid but fallback valid`() {
        val input = "badcolor"
        val fallback = "#00FF00" // Green
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#00FF00", result)
    }

    @Test
    fun `returns default white when fallback invalid`() {
        val input = null
        val fallback = "notacolor"
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#FFFFFF", result)
    }

    @Test
    fun `returns valid fallback when input blank and fallback valid`() {
        val input = ""
        val fallback = "#ABCDEF"
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#ABCDEF", result)
    }
}