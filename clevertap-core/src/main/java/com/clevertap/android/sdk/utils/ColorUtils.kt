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

    // Validate fallback using toColorInt()
    val safeFallback = fallback
        ?.takeIf { it.startsWith("#") }  // minimal structure check
        ?.let { candidate ->
            try {
                candidate.toColorInt()   // authoritative validation
                candidate
            } catch (_: Exception) {
                null
            }
        } ?: "#FFFFFF"

    // Validate main input
    val value = this ?: return safeFallback
    if (!value.startsWith("#")) return safeFallback

    return try {
        value.toColorInt()
        value
    } catch (e: Exception) {
        safeFallback
    }
}