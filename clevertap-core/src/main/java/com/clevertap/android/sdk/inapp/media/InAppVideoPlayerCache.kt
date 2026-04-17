package com.clevertap.android.sdk.inapp.media

import androidx.annotation.MainThread
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle

/**
 * Singleton cache that keeps an [InAppVideoPlayerHandle] alive across a single configuration
 * change (rotation). On rotation the Fragment is destroyed and recreated, so the handle must
 * survive outside the Fragment's scope.
 *
 * Only one entry is kept at a time — in-apps show one at a time, so this is safe.
 *
 * This cache is solely responsible for the player handle and fullscreen state.
 */
internal object InAppVideoPlayerCache {

    private var handle: InAppVideoPlayerHandle? = null
    private var cachedIsFullscreen: Boolean = false

    /**
     * Stores [handle] so the new Fragment can reclaim it after rotation.
     * [isFullscreen] preserves the fullscreen state so the new Fragment can restore it.
     * Any previously stored entry is replaced (shouldn't happen in practice).
     */
    @MainThread
    fun store(handle: InAppVideoPlayerHandle, isFullscreen: Boolean = false) {
        this.handle = handle
        this.cachedIsFullscreen = isFullscreen
    }

    /**
     * Returns and removes the cached handle, or null if none is stored.
     */
    @MainThread
    fun consume(): InAppVideoPlayerHandle? {
        val h = handle
        handle = null
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
        cachedIsFullscreen = false
    }
}
