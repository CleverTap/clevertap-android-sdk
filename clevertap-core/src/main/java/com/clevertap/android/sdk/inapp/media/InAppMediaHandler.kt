package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

/**
 * Unified interface for all media types in InApp notification fragments.
 * Each concrete implementation owns its full lifecycle without routing logic.
 *
 * Extends [DefaultLifecycleObserver] so it can be registered on a fragment's lifecycle
 * to receive start/resume/pause/stop events automatically.
 */
internal interface InAppMediaHandler : DefaultLifecycleObserver {
    fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener? = null
    )
    fun cleanup() {}
    fun clear() {}

    companion object {
        /**
         * Creates the appropriate [InAppMediaHandler] based on the notification's media type.
         *
         * @param supportsStreamMedia When `true`, allows creation of [InAppStreamMediaHandler] for
         *   video/audio playback. When `false` (default), video/audio media falls back to [NoOpMediaHandler].
         */
        fun create(
            fragment: Fragment,
            inAppNotification: CTInAppNotification,
            currentOrientation: Int,
            isTablet: Boolean,
            resourceProvider: FileResourceProvider,
            supportsStreamMedia: Boolean = false
        ): InAppMediaHandler {
            val media = inAppNotification.mediaList.firstOrNull()
                ?: return NoOpMediaHandler

            return when {
                media.isImage() -> InAppImageHandler(
                    inAppNotification, media, currentOrientation, resourceProvider
                )
                media.isGIF() -> InAppGifHandler(media, resourceProvider)
                (media.isVideo() || media.isAudio()) && supportsStreamMedia ->
                    InAppStreamMediaHandler(fragment, media, isTablet)
                else -> NoOpMediaHandler
            }
        }
    }
}

internal fun View.setContentDescriptionIfNotBlank(contentDescription: String) {
    if (contentDescription.isNotBlank()) {
        this.contentDescription = contentDescription
    }
}

/**
 * No-op handler for InApp notifications without media.
 */
internal object NoOpMediaHandler : InAppMediaHandler {
    override fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener?
    ) = Unit
}
