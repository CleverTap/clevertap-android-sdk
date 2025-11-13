package com.clevertap.android.sdk.utils

import androidx.core.graphics.toColorInt

/**
 * Safely validates a color string and returns a valid color string.
 *
 * If this string is a valid color (e.g. "#RRGGBB" or "#AARRGGBB"), it's returned as-is.
 * If it's null, blank, or invalid, the provided [fallback] color string is returned instead.
 *
 * Example:
 * ```
 * val valid = "#FF0000".toValidColorOrFallback("#FFFFFF") // "#FF0000"
 * val invalid = "notacolor".toValidColorOrFallback("#FFFFFF") // "#FFFFFF"
 * val empty = "".toValidColorOrFallback("#FFFFFF") // "#FFFFFF"
 * ```
 *
 * @receiver The color string to validate (e.g. "#FFFFFF", "#AARRGGBB")
 * @param fallback The fallback color string if invalid
 * @return A valid color string
 */
fun String?.toValidColorOrFallback(fallback: String): String {
    if (this.isNullOrBlank()) return fallback

    // Must start with '#' and have proper hex length
    if (!this.startsWith("#") || (this.length != 7 && this.length != 9)) {
        return fallback
    }

    return try {
        this.toColorInt() // Validate by trying to convert
        this
    } catch (e: Exception) {
        fallback
    }
}