package com.clevertap.android.pushtemplates.media

import android.graphics.Bitmap

/**
 * Represents the result of a GIF processing operation.
 */
internal sealed interface GifResult {
    
    /**
     * Represents a successful GIF processing result.
     * @param frames The processed GIF frames
     * @param duration The total duration of the GIF animation in milliseconds
     */
    data class Success(
        val frames: List<Bitmap>,
        val duration: Int
    ) : GifResult
    
    /**
     * Represents a failed GIF processing result.
     * @param reason The reason for the failure
     */
    data class Error(
        val reason: String
    ) : GifResult
}