package com.clevertap.android.pushtemplates

import com.clevertap.android.sdk.Logger
import kotlin.math.roundToInt

interface GifDecoder {
    fun decode(bytes: ByteArray, maxFrames: Int): GifResult
}

class GifDecoderImpl(
    private val adapter: GifDrawableAdapter = GifDrawableAdapterImpl()
) : GifDecoder {

    override fun decode(bytes: ByteArray, maxFrames: Int): GifResult {
        return runCatching {
            val gifDrawable = adapter.create(bytes)
            val totalFrames = adapter.getFrameCount(gifDrawable)
            val selectedIndices = selectFrameIndices(totalFrames, maxFrames)

            val frames = selectedIndices.mapNotNull { index ->
                runCatching { adapter.getFrameAt(gifDrawable, index) }
                    .getOrElse {
                        Logger.v("Exception while extracting frame at index: $index", it)
                        null
                    }
            }

            GifResult(frames, adapter.getDuration(gifDrawable))
        }.getOrElse {
            Logger.v("GIF decoding failed", it)
            GifResult.failure()
        }
    }

    private fun selectFrameIndices(totalFrames: Int, maxFrames: Int): List<Int> {
        if (totalFrames <= 0 || maxFrames <= 0) return emptyList()

        return when {
            maxFrames >= totalFrames -> (0 until totalFrames).toList()
            maxFrames == 1 -> listOf(0)
            else -> {
                val step = (totalFrames - 1).toDouble() / (maxFrames - 1)
                (0 until maxFrames).map { (it * step).roundToInt() }.distinct()
            }
        }
    }
}

