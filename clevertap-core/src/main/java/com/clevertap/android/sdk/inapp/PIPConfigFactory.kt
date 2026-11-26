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
import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PIPConfigFactory {

    private const val LOG_TAG = "PIPConfigFactory"
    private const val MAX_ANIMATION_DURATION_MS = 5000L
    private const val CUBIC_BEZIER_PARAM_COUNT = 4

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

        val (media, mediaType) = resolvePipMedia(inAppNotification, logger) ?: return null

        // Accessibility: media alt text
        val contentDesc = media.contentDescription.takeIf { it.isNotBlank() }.orEmpty()

        val fallbackUrl = parseFallbackUrl(inAppNotification)

        val position = parsePosition(pipJson)

        // Margins (percentage of screen)
        val margins = pipJson.optJSONObject("margins")
        val verticalMargin = optMarginPercent(margins, "vertical")
        val horizontalMargin = optMarginPercent(margins, "horizontal")

        // Width
        val widthPercent = pipJson.optInt("width", PIPConfig.DEFAULT_WIDTH_PERCENT)

        // Aspect ratio
        val arJson = pipJson.optJSONObject("aspectRatio")
        val arNum = optAspectRatioPart(arJson, "numerator", PIPConfig.DEFAULT_ASPECT_RATIO_NUMERATOR)
        val arDen = optAspectRatioPart(arJson, "denominator", PIPConfig.DEFAULT_ASPECT_RATIO_DENOMINATOR)

        // Controls visibility
        val ctrl = pipJson.optJSONObject("controls")
        val dragEnabled = optControlEnabled(ctrl, "drag")
        val showPlayPause = optControlEnabled(ctrl, "playPause")
        val showMute = optControlEnabled(ctrl, "mute")
        val showExpandCollapse = optControlEnabled(ctrl, "expandCollapse")

        val animConfig = parseAnimationConfig(pipJson)

        val action = parseAction(pipJson)

        // Corner radius
        val cornerRadiusDp = pipJson.optInt("cornerRadius", 0)

        // Border
        val borderJson = pipJson.optJSONObject("border")
        val borderEnabled = borderJson?.optBoolean("enabled", false) ?: false
        val borderColor = parseBorderColor(borderJson)
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

    /**
     * Returns the portrait media and its PIP media type, or `null` (after logging why) when there is
     * no usable media or its type is not supported.
     */
    private fun resolvePipMedia(
        inAppNotification: CTInAppNotification,
        logger: ILogger
    ): Pair<CTInAppNotificationMedia, PIPMediaType>? {
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
        return media to mediaType
    }

    /** Fallback URL from raw JSON media object. */
    private fun parseFallbackUrl(inAppNotification: CTInAppNotification): String? {
        val rawMediaJson = inAppNotification.jsonDescription.optJSONObject("media")
        return rawMediaJson?.optString("fallback_url", "")?.takeIf { it.isNotBlank() }
    }

    private fun parsePosition(pipJson: JSONObject): PIPPosition {
        return pipJson.optString("position", "").takeIf { it.isNotBlank() }
            ?.let { mapPosition(it) } ?: PIPPosition.BOTTOM_RIGHT
    }

    private fun parseAnimationConfig(pipJson: JSONObject): PIPAnimationConfig {
        return pipJson.optJSONObject("animation")?.let { mapAnimationConfig(it) }
            ?: PIPAnimationConfig()
    }

    private fun parseAction(pipJson: JSONObject): CTInAppAction? {
        return pipJson.optJSONObject("onClick")
            ?.let { CTInAppAction.createFromJson(it) }
            ?.takeIf { it.type != null }
    }

    private fun parseBorderColor(borderJson: JSONObject?): Int {
        return borderJson?.optString("color", "")
            ?.let { parseColor(it) } ?: Color.BLACK
    }

    private fun optMarginPercent(margins: JSONObject?, key: String): Int {
        return margins?.optInt(key, PIPConfig.DEFAULT_EDGE_MARGIN_PERCENT)
            ?: PIPConfig.DEFAULT_EDGE_MARGIN_PERCENT
    }

    private fun optAspectRatioPart(aspectRatio: JSONObject?, key: String, default: Double): Double {
        return aspectRatio?.optDouble(key, default)?.takeIf { it > 0.0 } ?: default
    }

    private fun optControlEnabled(controls: JSONObject?, key: String): Boolean {
        return controls?.optBoolean(key, true) ?: true
    }

    private val POSITIONS_BY_NAME = mapOf(
        "top-left" to PIPPosition.TOP_LEFT,
        "top-center" to PIPPosition.TOP_CENTER,
        "top-right" to PIPPosition.TOP_RIGHT,
        "center-left" to PIPPosition.LEFT_CENTER,
        "center" to PIPPosition.CENTER,
        "center-right" to PIPPosition.RIGHT_CENTER,
        "bottom-left" to PIPPosition.BOTTOM_LEFT,
        "bottom-center" to PIPPosition.BOTTOM_CENTER,
        "bottom-right" to PIPPosition.BOTTOM_RIGHT,
    )

    private fun mapPosition(position: String): PIPPosition? {
        return POSITIONS_BY_NAME[position.lowercase()]
    }

    private fun mapAnimationConfig(animJson: org.json.JSONObject): PIPAnimationConfig? {
        val type = when (animJson.optString("type", "").lowercase()) {
            "instant" -> PIPAnimation.INSTANT
            "dissolve" -> PIPAnimation.DISSOLVE
            "move-in", "move_in", "movein" -> PIPAnimation.MOVE_IN
            else -> return null
        }

        val durationMs = animJson.optLong("duration", PIPAnimationConfig.DEFAULT_DURATION_MS)
            .coerceIn(0, MAX_ANIMATION_DURATION_MS)

        val interpolator = mapEasing(
            animJson.optString("easing", ""),
            animJson.optString("bezier", ""),
        )

        val moveInDirection = mapMoveInDirection(animJson.optString("moveInDirection", ""))

        return PIPAnimationConfig(type, durationMs, interpolator, moveInDirection)
    }

    private fun mapMoveInDirection(direction: String): PIPAnimationConfig.MoveInDirection? {
        return when (direction.lowercase()) {
            "left" -> PIPAnimationConfig.MoveInDirection.LEFT
            "right" -> PIPAnimationConfig.MoveInDirection.RIGHT
            "top" -> PIPAnimationConfig.MoveInDirection.TOP
            "bottom" -> PIPAnimationConfig.MoveInDirection.BOTTOM
            else -> null
        }
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
        if (parts.size != CUBIC_BEZIER_PARAM_COUNT) {
            return null
        }
        return try {
            PathInterpolator(parts[0], parts[1], parts[2], parts[3])
        } catch (_: Exception) {
            null
        }
    }

    private fun parseColor(hex: String): Int? {
        if (hex.isBlank()) {
            return null
        }
        return try {
            hex.toColorInt()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
