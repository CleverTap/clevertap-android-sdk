package com.clevertap.android.sdk.inapp

import android.content.res.Configuration
import android.graphics.Color
import com.clevertap.android.sdk.ILogger
import android.animation.TimeInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimation
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimationConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPCallbacks
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition
import androidx.core.graphics.toColorInt

internal object PIPConfigFactory {

    private const val LOG_TAG = "PIPConfigFactory"

    fun create(
        inAppNotification: CTInAppNotification,
        callbacks: PIPCallbacks,
        logger: ILogger
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
            media.isAudio() -> PIPMediaType.AUDIO
            media.isVideo() -> PIPMediaType.VIDEO
            media.isGIF() -> PIPMediaType.GIF
            media.isImage() -> PIPMediaType.IMAGE
            else -> {
                logger.debug(LOG_TAG, "Unsupported media type: ${media.contentType}")
                return null
            }
        }

        // Accessibility: media alt text
        val contentDesc = media.contentDescription.takeIf { it.isNotBlank() } ?: ""

        // Fallback URL from raw JSON media object
        val rawMediaJson = inAppNotification.jsonDescription.optJSONObject("media")
        val fallbackUrl = rawMediaJson?.optString("fallback_url", "")?.takeIf { it.isNotBlank() }

        // Position
        val position = pipJson.optString("position", "").takeIf { it.isNotBlank() }
            ?.let { mapPosition(it) } ?: PIPPosition.BOTTOM_RIGHT

        // Margins (percentage of screen)
        val margins = pipJson.optJSONObject("margins")
        val verticalMargin = margins?.optInt("vertical", 3) ?: 3
        val horizontalMargin = margins?.optInt("horizontal", 3) ?: 3

        // Width
        val widthPercent = pipJson.optInt("width", 35)

        // Aspect ratio
        val arJson = pipJson.optJSONObject("aspectRatio")
        val arNum = arJson?.optDouble("numerator", 16.0)?.takeIf { it > 0.0 } ?: 16.0
        val arDen = arJson?.optDouble("denominator", 9.0)?.takeIf { it > 0.0 } ?: 9.0

        // Controls visibility
        val ctrl = pipJson.optJSONObject("controls")
        val dragEnabled = ctrl?.optBoolean("drag", true) ?: true
        val showPlayPause = ctrl?.optBoolean("playPause", true) ?: true
        val showMute = ctrl?.optBoolean("mute", true) ?: true
        val showExpandCollapse = ctrl?.optBoolean("expandCollapse", true) ?: true

        // Animation
        val animConfig = pipJson.optJSONObject("animation")?.let { mapAnimationConfig(it) }
            ?: PIPAnimationConfig()

        // Action
        val action = pipJson.optJSONObject("onClick")
            ?.let { CTInAppAction.createFromJson(it) }
            ?.takeIf { it.type != null }

        // Corner radius
        val cornerRadiusDp = pipJson.optInt("cornerRadius", 0)

        // Border
        val borderJson = pipJson.optJSONObject("border")
        val borderEnabled = borderJson?.optBoolean("enabled", false) ?: false
        val borderColor = borderJson?.optString("color", "")
            ?.let { parseColor(it) } ?: Color.BLACK
        val borderWidthDp = borderJson?.optInt("width", 0) ?: 0

        // Close button: `close: true` in PIP JSON means show close button
        val rawJson = inAppNotification.jsonDescription
        val showClose = rawJson.optBoolean("close", true)

        return try {
            PIPConfig(
                mediaUrl = media.mediaUrl,
                mediaType = mediaType,
                fallbackUrl = fallbackUrl,
                mediaContentDescription = contentDesc,
                widthPercent = widthPercent,
                aspectRatioNumerator = arNum,
                aspectRatioDenominator = arDen,
                initialPosition = position,
                horizontalEdgeMarginPercent = horizontalMargin,
                verticalEdgeMarginPercent = verticalMargin,
                animationConfig = animConfig,
                action = action,
                showCloseButton = showClose,
                dragEnabled = dragEnabled,
                showPlayPauseButton = showPlayPause,
                showMuteButton = showMute,
                showExpandCollapseButton = showExpandCollapse,
                cornerRadiusDp = cornerRadiusDp,
                borderEnabled = borderEnabled,
                borderColor = borderColor,
                borderWidthDp = borderWidthDp,
                callbacks = callbacks,
            )
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

    private fun mapAnimationConfig(animJson: org.json.JSONObject): PIPAnimationConfig? {
        val type = when (animJson.optString("type", "").lowercase()) {
            "instant" -> PIPAnimation.INSTANT
            "dissolve" -> PIPAnimation.DISSOLVE
            "move-in", "move_in", "movein" -> PIPAnimation.MOVE_IN
            else -> return null
        }

        val durationMs = animJson.optLong("duration", PIPAnimationConfig.DEFAULT_DURATION_MS)
            .coerceIn(0, 5000)

        val interpolator = mapEasing(
            animJson.optString("easing", ""),
            animJson.optString("bezier", ""),
        )

        val moveInDirection = when (animJson.optString("moveInDirection", "").lowercase()) {
            "left" -> PIPAnimationConfig.MoveInDirection.LEFT
            "right" -> PIPAnimationConfig.MoveInDirection.RIGHT
            "top" -> PIPAnimationConfig.MoveInDirection.TOP
            "bottom" -> PIPAnimationConfig.MoveInDirection.BOTTOM
            else -> null
        }

        return PIPAnimationConfig(type, durationMs, interpolator, moveInDirection)
    }

    private fun mapEasing(easing: String, bezier: String): TimeInterpolator {
        return when (easing.lowercase()) {
            "linear" -> LinearInterpolator()
            "ease-in" -> AccelerateInterpolator()
            "ease-out" -> DecelerateInterpolator()
            "ease-in-out" -> AccelerateDecelerateInterpolator()
            "cubic-bezier" -> parseBezier(bezier) ?: PIPAnimationConfig.DEFAULT_INTERPOLATOR
            else -> PIPAnimationConfig.DEFAULT_INTERPOLATOR
        }
    }

    private fun parseBezier(bezier: String): TimeInterpolator? {
        val parts = bezier.split(",").mapNotNull { it.trim().toFloatOrNull() }
        if (parts.size != 4) return null
        return try {
            PathInterpolator(parts[0], parts[1], parts[2], parts[3])
        } catch (_: Exception) {
            null
        }
    }

    private fun parseColor(hex: String): Int? {
        if (hex.isBlank()) return null
        return try {
            hex.toColorInt()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
