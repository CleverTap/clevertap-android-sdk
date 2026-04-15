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

    companion object {
        /**
         * Resolves which media URL should be used for this in-app, without creating a handler.
         * Call this once and store the result so rotation re-uses the same URL instead of
         * picking a different one based on the new orientation.
         */
        fun resolveMediaUrl(
            inAppNotification: CTInAppNotification,
            currentOrientation: Int
        ): String? = (
            inAppNotification.getInAppMediaForOrientation(currentOrientation)
                ?: inAppNotification.mediaList.firstOrNull()
        )?.mediaUrl

        fun create(
            fragment: Fragment,
            inAppNotification: CTInAppNotification,
            currentOrientation: Int,
            isTablet: Boolean,
            resourceProvider: FileResourceProvider,
            supportsStreamMedia: Boolean = false,
            onActionClick: (() -> Unit)? = null,
            lockedMediaUrl: String? = null
        ): InAppMediaHandler {
            val media = lockedMediaUrl
                ?.let { url -> inAppNotification.mediaList.firstOrNull { it.mediaUrl == url } }
                ?: inAppNotification.getInAppMediaForOrientation(currentOrientation)
                ?: inAppNotification.mediaList.firstOrNull()
                ?: return NoOpMediaHandler

            return when {
                media.isImage() -> InAppImageHandler(media, resourceProvider)
                media.isGIF() -> InAppGifHandler(media, resourceProvider)
                (media.isVideo() || media.isAudio()) && supportsStreamMedia ->
                    InAppStreamMediaHandler(fragment, media, isTablet, onActionClick)
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
