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
    fun `returns fallback when input is incomplete hex`() {
        val input = "#FF"
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
    fun `returns fallback color when input invalid but fallback valid`() {
        val input = "#invalid"
        val fallback = "#00FF00" // Green
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#00FF00", result)
    }
}