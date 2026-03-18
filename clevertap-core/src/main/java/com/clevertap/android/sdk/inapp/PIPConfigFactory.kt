package com.clevertap.android.sdk.inapp

import android.content.res.Configuration
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimation
import com.clevertap.android.sdk.inapp.pipsdk.PIPCallbacks
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition

internal object PIPConfigFactory {

    private const val LOG_TAG = "PIPConfigFactory"

    fun create(
        inAppNotification: CTInAppNotification,
        callbacks: PIPCallbacks,
        logger: Logger
    ): PIPConfig? {
        val pipJson = inAppNotification.pipConfigJson
        if (pipJson == null) {
            logger.debug(LOG_TAG, "No pip config JSON found")
            return null
        }

        val media = inAppNotification.getInAppMediaForOrientation(Configuration.ORIENTATION_PORTRAIT)
        if (media == null || media.mediaUrl.isBlank()) {
            logger.debug(LOG_TAG, "No media found for PIP")
            return null
        }

        val mediaType = when {
            media.isVideo() -> PIPMediaType.VIDEO
            media.isGIF() -> PIPMediaType.GIF
            media.isImage() -> PIPMediaType.IMAGE
            else -> {
                logger.debug(LOG_TAG, "Unsupported media type: ${media.contentType}")
                return null
            }
        }

        val builder = PIPConfig.builder(media.mediaUrl, mediaType)
            .callbacks(callbacks)

        // Fallback URL from raw JSON media object
        val rawMediaJson = inAppNotification.jsonDescription.optJSONObject("media")
        rawMediaJson?.optString("fallback_url", "")?.takeIf { it.isNotBlank() }?.let {
            builder.fallbackUrl(it)
        }

        // Position
        pipJson.optString("position", "").takeIf { it.isNotBlank() }?.let { pos ->
            mapPosition(pos)?.let { builder.initialPosition(it) }
        }

        // Margins
        pipJson.optJSONObject("margins")?.let { margins ->
            if (margins.has("vertical")) {
                builder.verticalEdgeMarginDp(margins.optInt("vertical", 16))
            }
            if (margins.has("horizontal")) {
                builder.horizontalEdgeMarginDp(margins.optInt("horizontal", 16))
            }
        }

        // Width
        if (pipJson.has("width")) {
            builder.widthPercent(pipJson.optInt("width", 35))
        }

        // Aspect ratio
        pipJson.optJSONObject("aspectRatio")?.let { ar ->
            val num = ar.optInt("numerator", 16)
            val den = ar.optInt("denominator", 9)
            if (num > 0 && den > 0) builder.aspectRatio(num, den)
        }

        // Controls visibility
        pipJson.optJSONObject("controls")?.let { ctrl ->
            if (ctrl.has("drag")) builder.dragEnabled(ctrl.optBoolean("drag", true))
            if (ctrl.has("playPause")) builder.showPlayPauseButton(ctrl.optBoolean("playPause", true))
            if (ctrl.has("mute")) builder.showMuteButton(ctrl.optBoolean("mute", true))
            if (ctrl.has("expandCollapse")) builder.showExpandCollapseButton(ctrl.optBoolean("expandCollapse", true))
        }

        // Animation
        pipJson.optString("animation", "").takeIf { it.isNotBlank() }?.let { anim ->
            mapAnimation(anim)?.let { builder.animation(it) }
        }

        // Redirect URL
        pipJson.optJSONObject("onClick")?.optString("android", "")
            ?.takeIf { it.isNotBlank() }?.let {
                builder.redirectUrl(it)
            }

        // Close button: `close: true` in PIP JSON means show close button
        val rawJson = inAppNotification.jsonDescription
        if (rawJson.has("close")) {
            builder.showCloseButton(rawJson.optBoolean("close", true))
        }

        return try {
            builder.build()
        } catch (e: IllegalArgumentException) {
            logger.debug(LOG_TAG, "Failed to build PIPConfig: ${e.message}")
            null
        }
    }

    private fun mapPosition(position: String): PIPPosition? {
        return when (position.lowercase()) {
            "top-left" -> PIPPosition.TOP_LEFT
            "top-center" -> PIPPosition.TOP_CENTER
            "top-right" -> PIPPosition.TOP_RIGHT
            "center-left" -> PIPPosition.LEFT_CENTER
            "center" -> PIPPosition.CENTER
            "center-right" -> PIPPosition.RIGHT_CENTER
            "bottom-left" -> PIPPosition.BOTTOM_LEFT
            "bottom-center" -> PIPPosition.BOTTOM_CENTER
            "bottom-right" -> PIPPosition.BOTTOM_RIGHT
            else -> null
        }
    }

    private fun mapAnimation(animation: String): PIPAnimation? {
        return when (animation.lowercase()) {
            "instant" -> PIPAnimation.INSTANT
            "dissolve" -> PIPAnimation.DISSOLVE
            "move-in", "move_in", "movein" -> PIPAnimation.MOVE_IN
            else -> null
        }
    }
}
