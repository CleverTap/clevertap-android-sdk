package com.clevertap.android.sdk.utils

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ColorUtilsTest {

    private val fallbackColor = "#FFFFFF"

    @Test
    fun returnsSameColorWhenValidHexColorProvided() {
        val input = "#FF0000"
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals("#FF0000", result)
    }

    @Test
    fun returnsSameColorWhenValidAlphaColorProvided() {
        val input = "#80FF0000"
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals("#80FF0000", result)
    }

    @Test
    fun returnsFallbackWhenInputIsNull() {
        val input: String? = null
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun returnsFallbackWhenInputIsBlank() {
        val input = ""
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun returnsFallbackWhenInputIsInvalidString() {
        val input = "notacolor"
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun returnsFallbackWhenHashMissing() {
        val input = "FF0000"
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals(fallbackColor, result)
    }

    @Test
    fun handlesLowercaseHexProperly() {
        val input = "#ff00ff"
        val result = input.toValidColorOrFallback(fallbackColor)
        assertEquals("#ff00ff", result)
    }

    @Test
    fun returnsDefaultWhiteWhenInputAndFallbackNull() {
        val input: String? = null
        val fallback: String? = null
        val result = input.toValidColorOrFallback(fallback ?: "")
        assertEquals("#FFFFFF", result)
    }

    @Test
    fun returnsDefaultWhiteWhenBothInvalid() {
        val input = "invalidColor"
        val fallback = "notAColor"
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#FFFFFF", result)
    }

    @Test
    fun returnsFallbackWhenInputInvalidFallbackValid() {
        val input = "badcolor"
        val fallback = "#00FF00"
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#00FF00", result)
    }

    @Test
    fun returnsDefaultWhiteWhenFallbackInvalid() {
        val input = null
        val fallback = "notacolor"
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#FFFFFF", result)
    }

    @Test
    fun returnsValidFallbackWhenInputBlankFallbackValid() {
        val input = ""
        val fallback = "#ABCDEF"
        val result = input.toValidColorOrFallback(fallback)
        assertEquals("#ABCDEF", result)
    }
}
