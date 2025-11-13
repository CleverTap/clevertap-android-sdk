package com.clevertap.android.sdk.utils

import androidx.core.graphics.toColorInt

/**
 * Safely validates a color string and returns a valid color string.
 *
 * If this string is a valid color (e.g. "#RRGGBB" or "#AARRGGBB"), it's returned as-is.
 * If it's null, blank, or invalid, the provided [fallback] color string is returned instead.
 * If [fallback] is also null or invalid, "#FFFFFF" is used as a default safe fallback.
 */
fun String?.toValidColorOrFallback(fallback: String?): String {
    val safeFallback = fallback?.takeIf {
        it.startsWith("#") && (it.length == 7 || it.length == 9)
    } ?: "#FFFFFF"

    if (this.isNullOrBlank()) return safeFallback

    // Must start with '#' and have proper hex length
    if (!this.startsWith("#") || (this.length != 7 && this.length != 9)) {
        return safeFallback
    }

    return try {
        this.toColorInt() // Validate by trying to convert
        this
    } catch (e: Exception) {
        safeFallback
    }
}