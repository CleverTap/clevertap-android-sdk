package com.clevertap.android.sdk.utils

import androidx.core.graphics.toColorInt

/**
 * Safely validates a color string and returns a valid color string.
 *
 * - If [this] is a valid color (e.g. "#RRGGBB" or "#AARRGGBB"), it's returned as-is.
 * - If [this] is null, blank, or invalid, [fallback] is validated and returned if valid.
 * - If both are invalid or null, "#FFFFFF" is returned as the ultimate safe fallback.
 *
 * @receiver The color string to validate.
 * @param fallback The fallback color string if this is invalid.
 * @return A valid color string (guaranteed non-null).
 */
fun String?.toValidColorOrFallback(fallback: String?): String {
    // Validate fallback safely
    val safeFallback = fallback?.takeIf {
        it.startsWith("#") && (it.length == 7 || it.length == 9)
    }?.let { candidate ->
        try {
            candidate.toColorInt() // Will throw if invalid
            candidate
        } catch (e: Exception) {
            null
        }
    } ?: "#FFFFFF"

    // Validate main input
    if (this.isNullOrBlank()) return safeFallback

    if (!this.startsWith("#") || (this.length != 7 && this.length != 9)) {
        return safeFallback
    }

    return try {
        this.toColorInt()
        this
    } catch (e: Exception) {
        safeFallback
    }
}
