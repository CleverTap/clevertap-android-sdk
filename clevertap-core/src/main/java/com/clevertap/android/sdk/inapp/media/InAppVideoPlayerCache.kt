package com.clevertap.android.sdk.inapp.media

import androidx.annotation.MainThread
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle

/**
 * Singleton cache that keeps an [InAppVideoPlayerHandle] alive across a single configuration
 * change (rotation). On rotation the Fragment is destroyed and recreated, so the handle must
 * survive outside the Fragment's scope.
 *
 * Only one entry is kept at a time — in-apps show one at a time, so this is safe.
 */
internal object InAppVideoPlayerCache {

    private var handle: InAppVideoPlayerHandle? = null
    private var cachedUrl: String? = null
    private var cachedIsFullscreen: Boolean = false

    /**
     * Stores [handle] keyed by [url] so the new Fragment can reclaim it after rotation.
     * [isFullscreen] preserves the fullscreen state so the new Fragment can restore it.
     * Any previously stored entry is replaced (shouldn't happen in practice).
     */
    fun store(handle: InAppVideoPlayerHandle, url: String, isFullscreen: Boolean = false) {
        this.handle = handle
        this.cachedUrl = url
        this.cachedIsFullscreen = isFullscreen
    }

    /**
     * Returns and removes the cached handle if its URL matches [url], or null otherwise.
     * A URL mismatch means the cache holds a stale handle from a previous in-app session;
     * in that case [release] is called automatically to clean it up.
     */
    @MainThread
    fun consume(url: String): InAppVideoPlayerHandle? {
        if (cachedUrl != url) {
            release()
            return null
        }
        val h = handle
        handle = null
        cachedUrl = null
        return h
    }

    @MainThread
    fun consumeFullscreen(): Boolean {
        val fs = cachedIsFullscreen
        cachedIsFullscreen = false
        return fs
    }

    /**
     * Fully releases any cached player. Call on explicit in-app dismissal to ensure no
     * orphaned player survives if the cache was never consumed (e.g., dismiss during rotation).
     */
    @MainThread
    fun release() {
        handle?.pause()
        handle = null
        cachedUrl = null
        cachedIsFullscreen = false
    }
}
