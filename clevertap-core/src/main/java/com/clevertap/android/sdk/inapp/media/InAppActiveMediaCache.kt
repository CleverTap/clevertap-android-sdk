package com.clevertap.android.sdk.inapp.media

import androidx.annotation.MainThread

/**
 * Tracks the URL of whichever media item is currently displayed in an InApp notification,
 * regardless of media type (image, GIF, video, audio).
 *
 * This lets [InAppMediaHandler.create] re-select the same media after a configuration change
 * (e.g. rotation) instead of falling through to [CTInAppNotification.getInAppMediaForOrientation],
 * which might return a different item for the new orientation.
 *
 * Lifecycle:
 * - **store**: called by each handler's `setup()` once the media is actually rendered.
 * - **peekUrl**: checked at the top of [InAppMediaHandler.create]; does not consume the entry.
 * - **release**: called by each handler's `cleanup()` on explicit in-app dismissal.
 */
internal object InAppActiveMediaCache {

    private var activeUrl: String? = null

    @MainThread
    fun store(url: String) {
        activeUrl = url
    }

    @MainThread
    fun peekUrl(): String? = activeUrl

    @MainThread
    fun release() {
        activeUrl = null
    }
}
