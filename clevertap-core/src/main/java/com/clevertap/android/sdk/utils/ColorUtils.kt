package com.clevertap.android.sdk.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt

/**
 * Safely converts a [String] to a color integer.
 *
 * This function attempts to parse a color string (e.g., "#RRGGBB" or "#AARRGGBB")
 * into an Android color integer. If the string is null, blank, or invalid, it returns
 * the provided [defaultColor].
 *
 * Example:
 * ```
 * val color = "#FF0000".toColorIntOrDefault() // Returns red
 * val invalid = "redcolor".toColorIntOrDefault(Color.GRAY) // Returns gray
 * ```
 *
 * @receiver The color string to parse (e.g., "#FFFFFF", "#80FF0000").
 * @param defaultColor The fallback color if parsing fails (default is [Color.WHITE]).
 * @return The parsed color integer or [defaultColor] if parsing fails.
 */
@ColorInt
fun String?.toColorIntOrDefault(@ColorInt defaultColor: Int = Color.WHITE): Int {
    // If the string is null or empty, return the default color immediately
    if (this.isNullOrBlank()) return defaultColor

    return try {
        // Try converting the string to a color integer
        this.toColorInt()
    } catch (e: IllegalArgumentException) {
        // Handles invalid color format (e.g., "abc123")
        defaultColor
    } catch (e: StringIndexOutOfBoundsException) {
        // Handles malformed color strings (e.g., "#F")
        defaultColor
    }
}

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

    return try {
        this.toColorInt() // Validate by trying to convert
        this // Valid, return same string
    } catch (e: IllegalArgumentException) {
        fallback
    } catch (e: StringIndexOutOfBoundsException) {
        fallback
    }
}

/**
 * Safely parses a color string into an Android color integer.
 *
 * This utility provides two overloads:
 * - [parseColor(colorString, defaultColor)] → returns the parsed color or a provided fallback color.
 * - [parseColor(colorString)] → same as above but defaults to WHITE on failure.
 *
 * Supported formats:
 * ```
 * #RRGGBB
 * #AARRGGBB
 * ```
 *
 * Example:
 * ```
 * val red = parseColor("#FF0000")                 // Returns red
 * val semiTransparent = parseColor("#80FF0000")   // Returns semi-transparent red
 * val invalid = parseColor("notacolor", Color.GRAY) // Returns gray
 * ```
 */
object Color {

    /**
     * Safely parses a color string into a color integer.
     *
     * @param colorString The color string to parse (e.g. "#FFFFFF", "#AARRGGBB").
     * @param defaultColor The fallback color as a string (e.g. "#000000").
     * @return The parsed color integer, or parsed defaultColor if invalid.
     */
    @ColorInt
    fun parseColor(colorString: String?, defaultColor: String): Int {
        // Return parsed default color if the string is null or blank
        if (colorString.isNullOrBlank()) {
            return try {
                defaultColor.toColorInt()
            } catch (e: Exception) {
                // Fallback to black if default color is also invalid
                Color.WHITE
            }
        }

        return try {
            // Attempt to parse the color string
            colorString.toColorInt()
        } catch (e: IllegalArgumentException) {
            // Thrown if the string is not a valid color format
            try {
                defaultColor.toColorInt()
            } catch (e: Exception) {
                Color.WHITE
            }
        } catch (e: StringIndexOutOfBoundsException) {
            // Thrown if the string is malformed (e.g. incomplete hex)
            try {
                defaultColor.toColorInt()
            } catch (e: Exception) {
                Color.WHITE
            }
        }
    }

    /**
     * Parses a color string, defaulting to WHITE(#FFFFFF) if parsing fails.
     *
     * @param colorString The color string to parse.
     * @return The parsed color integer, or WHITE(#FFFFFF) if invalid or null.
     */
    @ColorInt
    fun parseColor(colorString: String?): Int {
        return parseColor(colorString, "#FFFFFF")
    }
}
