package com.clevertap.android.sdk.customviews

import android.os.Bundle
import androidx.annotation.RestrictTo

/** Where each video of an inbox screen was left. The lists of all its tabs share one instance. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class InboxVideoStates {

    /** Where a video was left, for the media it was playing. */
    internal class VideoState(val mediaUrl: String, val positionMs: Long, val volume: Float)

    private val states = mutableMapOf<String, VideoState>()

    internal fun remember(messageId: String, state: VideoState) {
        states[messageId] = state
    }

    /** The saved state of the message, only if its media is still the same. */
    internal fun stateOf(messageId: String?, mediaUrl: String?): VideoState? {
        if (messageId == null) {
            return null
        }
        return states[messageId]?.takeIf { it.mediaUrl == mediaUrl }
    }

    /** Everything that was remembered, to give to a new screen through [restore]. */
    fun save(): Bundle {
        val saved = Bundle()
        for ((messageId, state) in states) {
            saved.putBundle(
                messageId,
                Bundle().apply {
                    putString(KEY_MEDIA_URL, state.mediaUrl)
                    putLong(KEY_POSITION_MS, state.positionMs)
                    putFloat(KEY_VOLUME, state.volume)
                }
            )
        }
        return saved
    }

    /** Takes back what [save] returned. A null or empty bundle changes nothing. */
    fun restore(saved: Bundle?) {
        if (saved == null) {
            return
        }
        for (messageId in saved.keySet()) {
            val state = saved.getBundle(messageId) ?: continue
            val mediaUrl = state.getString(KEY_MEDIA_URL) ?: continue
            states[messageId] = VideoState(
                mediaUrl = mediaUrl,
                positionMs = state.getLong(KEY_POSITION_MS),
                volume = state.getFloat(KEY_VOLUME)
            )
        }
    }

    private companion object {

        const val KEY_MEDIA_URL = "mediaUrl"
        const val KEY_POSITION_MS = "positionMs"
        const val KEY_VOLUME = "volume"
    }
}
