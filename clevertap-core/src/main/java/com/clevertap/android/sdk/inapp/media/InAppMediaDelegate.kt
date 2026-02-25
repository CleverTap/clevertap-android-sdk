package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal data class InAppMediaConfig(
    val imageViewId: Int,
    val clickableMedia: Boolean,
    val useOrientationForImage: Boolean = true,
    val videoFrameId: Int = 0,
    val fillVideoFrame: Boolean = true
)

internal fun View.setContentDescriptionIfNotBlank(contentDescription: String) {
    if (contentDescription.isNotBlank()) {
        this.contentDescription = contentDescription
    }
}

internal const val CLICKABLE_MEDIA_TAG = 0

/**
 * Coordinator for all media types in InApp notification fragments.
 *
 * Creates exactly one [InAppMediaHandler] based on the actual media type,
 * so each handler owns its own lifecycle without routing logic.
 *
 * @param supportsStreamMedia When `true`, allows creation of [InAppStreamMediaHandler] for
 *   video/audio playback. When `false` (default), video/audio media falls back to [NoOpMediaHandler].
 */
internal class InAppMediaDelegate(
    fragment: Fragment,
    inAppNotification: CTInAppNotification,
    currentOrientation: Int,
    isTablet: Boolean,
    resourceProvider: FileResourceProvider,
    supportsStreamMedia: Boolean = false
) {

    private val handler: InAppMediaHandler = createHandler(
        fragment, inAppNotification, currentOrientation, isTablet, resourceProvider, supportsStreamMedia
    )

    fun onStart() {
        handler.onStart()
    }

    fun onResume() {
        handler.onResume()
    }

    fun onPause() {
        handler.onPause()
    }

    fun onStop() {
        handler.onStop()
    }

    fun cleanup() {
        handler.cleanup()
    }

    fun clear() {
        handler.clear()
    }

    fun setMediaForInApp(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener? = null
    ) {
        handler.setup(relativeLayout, config, clickListener)
    }

}

private fun createHandler(
    fragment: Fragment,
    inAppNotification: CTInAppNotification,
    currentOrientation: Int,
    isTablet: Boolean,
    resourceProvider: FileResourceProvider,
    supportsStreamMedia: Boolean
): InAppMediaHandler {
    val media = inAppNotification.mediaList.firstOrNull()
        ?: return NoOpMediaHandler

    return when {
        media.isImage() -> InAppImageHandler(
            inAppNotification, media, currentOrientation, resourceProvider
        )
        media.isGIF() -> InAppGifHandler(media, resourceProvider)
        (media.isVideo() || media.isAudio()) && supportsStreamMedia -> InAppStreamMediaHandler(fragment, media, isTablet)
        else -> NoOpMediaHandler
    }
}
