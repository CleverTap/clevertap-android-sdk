package com.clevertap.android.pushtemplates

import android.graphics.Bitmap
import pl.droidsonroids.gif.GifDrawable
import kotlin.math.roundToInt

class TemplateMediaManager(private val templateRepository: TemplateRepository) {
    companion object {
        private const val INVALID_DURATION = -1
    }

    data class GifResult(
        val frames: List<Bitmap>?,
        val duration: Int
    ) {
        companion object {
            fun failure(): GifResult {
                return GifResult(null, INVALID_DURATION)
            }
        }
    }


    fun getGifFrames(gifUrl: String?, maxFrames: Int): GifResult {
        if (gifUrl.isNullOrBlank() || !gifUrl.startsWith("https") || !gifUrl.lowercase().endsWith(".gif")) {
            PTLog.debug("Invalid GIF URL: $gifUrl")
            return GifResult.failure()
        }

        val rawBytes = templateRepository.getGifBytes(gifUrl)
        if (rawBytes == null) {
            PTLog.debug("Failed to download GIF from URL: $gifUrl")
            return GifResult.failure()
        }

        return runCatching {
            val gifDrawable = GifDrawable(rawBytes)
            val totalFrames = gifDrawable.numberOfFrames
            val frames = getOptimizedFrames(gifDrawable, maxFrames, totalFrames)
            GifResult(frames, gifDrawable.duration)
        }.getOrElse { e ->
            PTLog.debug("GIF decoding failed for URL: $gifUrl", e)
            GifResult.failure()
        }
    }


    private fun getOptimizedFrames(
        gifDrawable: GifDrawable?,
        maxFrames: Int,
        totalFrames: Int
    ): List<Bitmap>? {
        if (gifDrawable == null) {
            PTLog.debug("GifDrawable is null")
            return null
        }

        if (totalFrames <= 0 || maxFrames <= 0) {
            PTLog.debug("Invalid frame counts - totalFrames: $totalFrames, maxFrames: $maxFrames")
            return null
        }

        val selectedIndices = if (maxFrames >= totalFrames) {
            (0 until totalFrames).toList()
        } else {
            val step = (totalFrames - 1).toDouble() / (maxFrames - 1)
            (0 until maxFrames).map { (it * step).roundToInt() }.distinct()
        }

        val frames = selectedIndices.mapNotNull { index ->
            runCatching { gifDrawable.seekToFrameAndGet(index) }
                .getOrElse {
                    PTLog.debug("Exception while extracting frame at index: $index", it)
                    null
                }
        }

        return frames
    }
}