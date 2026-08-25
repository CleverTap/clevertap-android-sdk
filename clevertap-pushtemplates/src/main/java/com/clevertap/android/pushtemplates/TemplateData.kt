package com.clevertap.android.pushtemplates

import org.json.JSONArray
import java.util.ArrayList

// Base sealed class for all template data types
internal sealed class TemplateData {
    abstract val templateType: TemplateType
}

internal data class ImageData(
    val url: String? = null,
    val altText: String
)

internal data class GifData(
    val url: String? = null,
    val numberOfFrames: Int = 10,
)

internal data class ActionButton(
    val id: String,
    val label: String,
    val icon: Int
)

internal data class BaseTextData(
    val title: String? = null,
    val message: String? = null,
    val messageSummary: String? = null,
    val subtitle: String? = null,
)

/**
 * Border/corner configuration for the expanded media (image or GIF).
 *
 * [borderColor] is already parsed into an Android colour int by [TemplateDataFactory], so an
 * unparseable colour from the payload lands here as null and [isActive] stays honest about
 * whether there is anything to draw.
 *
 * [cornerRadiusDp] and [borderWidthDp] are both in dp, matching every other border and radius key in
 * the payload. They are converted to bitmap pixels at draw time, because the border is baked into
 * the bitmap and campaign images vary in resolution.
 * See NotificationBitmapUtils.applyRoundedBorderToBitmap.
 */
internal data class ImageBorderData(
    val borderColor: Int? = null,
    val cornerRadiusDp: Int = 0,
    val borderWidthDp: Int? = null,
) {
    val isActive: Boolean get() = cornerRadiusDp > 0 || borderColor != null
}

/**
 * Rounded corners and borders are baked into the bitmap, so a CENTER_CROP image view would crop
 * them away. Returns FIT_CENTER whenever a border is active, otherwise the requested scale type.
 */
internal fun ImageBorderData?.effectiveScaleType(requested: PTScaleType): PTScaleType {
    if (this == null || !isActive) return requested
    if (requested != PTScaleType.FIT_CENTER) {
        PTLog.debug(
            "Image border is active, overriding scale type $requested with FIT_CENTER so the " +
                    "rounded corners and border stay visible"
        )
    }
    return PTScaleType.FIT_CENTER
}

internal data class MediaData(
    val bigImage: ImageData,
    val gif: GifData,
    val scaleType: PTScaleType = PTScaleType.CENTER_CROP,
    val imageBorderData: ImageBorderData = ImageBorderData(),
)

internal data class IconData(
    val largeIcon: String? = null,
)

internal data class BaseColorData(
    val titleColor: String? = null,
    val messageColor: String? = null,
    val backgroundColor: String? = null,
    val metaColor: String? = null,
)

internal data class BaseContent(
    val textData: BaseTextData,
    val colorData: BaseColorData,
    val iconData: IconData,
    val deepLinkList: ArrayList<String>,
    val notificationBehavior: NotificationBehavior
)

internal data class NotificationBehavior(
    val isSticky: Boolean = false,
    val dismissAfter: Long? = null,
)

internal data class CarouselData(
    val baseContent: BaseContent,
    val actions: JSONArray? = null,
    val imageList: ArrayList<ImageData>,
    val scaleType: PTScaleType = PTScaleType.CENTER_CROP,
    val imageBorderData: ImageBorderData = ImageBorderData(),
)

internal data class BasicTemplateData(
    override val templateType: TemplateType = TemplateType.BASIC,
    val baseContent: BaseContent,
    val mediaData: MediaData,
    val actions: JSONArray? = null,
) : TemplateData()

internal data class FiveIconsTemplateData(
    override val templateType: TemplateType = TemplateType.FIVE_ICONS,
    val imageList: ArrayList<ImageData>,
    val deepLinkList: ArrayList<String>,
    val backgroundColor: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val notificationBehavior: NotificationBehavior,
    val imageBorderData: ImageBorderData = ImageBorderData(),
) : TemplateData()

internal data class ManualCarouselTemplateData(
    override val templateType: TemplateType = TemplateType.MANUAL_CAROUSEL,
    val carouselData: CarouselData,
    val carouselType: String? = null,
) : TemplateData()

internal data class AutoCarouselTemplateData(
    override val templateType: TemplateType = TemplateType.AUTO_CAROUSEL,
    val carouselData: CarouselData,
    val flipInterval: Int = PTConstants.PT_FLIP_INTERVAL_TIME,
) : TemplateData()

internal data class RatingTemplateData(
    override val templateType: TemplateType = TemplateType.RATING,
    val baseContent: BaseContent,
    val mediaData: MediaData,
    val defaultDeepLink: String? = null,
) : TemplateData()

/**
 * How the rating positions are drawn. Sourced from the required pt_rating_style key; a missing or
 * unrecognised value yields null so the renderer can fall back to Basic per R-22.
 */
internal enum class RatingStyleType(private val value: String) {
    ICON("icon"), TEXT("text");

    override fun toString(): String = value

    companion object {

        fun fromString(value: String?): RatingStyleType? = when (value?.lowercase()) {
            "icon" -> ICON
            "text" -> TEXT
            else -> null
        }
    }
}

/**
 * One rating position. Only the fields belonging to the configured [RatingStyleType] are populated:
 * icon style fills [iconUrl] / [selectedIconUrl], text style fills [label].
 *
 * [deepLink] is the optional pt_dl{n} override. When set, submitting with this position selected
 * opens it instead of the submit button's own destination.
 */
internal data class RatingPositionData(
    val iconUrl: String? = null,
    val selectedIconUrl: String? = null,
    val label: String? = null,
    val deepLink: String? = null,
) {
    /** A position the renderer can actually draw, given the template's style. */
    fun isRenderable(style: RatingStyleType): Boolean = when (style) {
        RatingStyleType.ICON -> !iconUrl.isNullOrBlank()
        RatingStyleType.TEXT -> !label.isNullOrBlank()
    }
}

/**
 * Submit button configuration. [label] and [deepLink] are required by the payload contract; the
 * renderer treats either being absent as an invalid payload.
 */
internal data class RatingCtaData(
    val label: String? = null,
    val deepLink: String? = null,
    val backgroundColor: String? = null,
    val borderColor: String? = null,
    val textColor: String? = null,
    val cornerRadiusDp: Int = PTConstants.PT_RATING_CTA_RADIUS_DEFAULT,
)

/**
 * The pt_custom_rating template — configurable 2-5 positions, icon or text style, and a submit
 * button that defers the rating event until the user confirms.
 *
 * Deliberately a separate type from [RatingTemplateData]: the Classic pt_rating template is frozen
 * and must not share a rendering or intent path with this one.
 */
internal data class CustomRatingTemplateData(
    override val templateType: TemplateType = TemplateType.CUSTOM_RATING,
    val baseContent: BaseContent,
    val mediaData: MediaData,
    val defaultDeepLink: String? = null,
    val ratingStyle: RatingStyleType? = null,
    val ratingCount: Int = 0,
    val positions: List<RatingPositionData> = emptyList(),
    val iconColor: String? = null,
    val selectedIconColor: String? = null,
    val labelColor: String? = null,
    val selectedLabelColor: String? = null,
    val ctaData: RatingCtaData = RatingCtaData(),
    val confirmationMessage: String? = null,
) : TemplateData() {

    /**
     * Positions the renderer can draw. A position whose asset failed to resolve is substituted with
     * the built-in star pair rather than dropped, so this only counts positions with usable config.
     */
    val renderablePositionCount: Int
        get() = ratingStyle?.let { style -> positions.count { it.isRenderable(style) } } ?: 0

    /**
     * True when the text style is configured but at least one position has no label. The row then
     * falls back to the built-in stars rather than drawing a gap where a chip should be (R-23).
     */
    val hasIncompleteTextRow: Boolean
        get() = ratingStyle == RatingStyleType.TEXT && renderablePositionCount < ratingCount
}

internal data class TimerTemplateData(
    override val templateType: TemplateType = TemplateType.TIMER,
    val baseContent: BaseContent,
    val mediaData: MediaData,
    val actions: JSONArray? = null,
    val terminalTextData: BaseTextData,
    val terminalMediaData: MediaData,
    val chronometerTitleColor: String? = null,
    val chronometerBgColor: String? = null,
    val chronometerBorderColor: String? = null,
    val chronometerStyle: ButtonStyle = ButtonStyle.SOLID,
    val chronometerGradientColor1: String? = null,
    val chronometerGradientColor2: String? = null,
    val chronometerGradientDirection: Double = PTConstants.PT_BTN_GRAD_DIR_DEFAULT,
    val renderTerminal: Boolean = true,
    val chronometerBorderRadius: Float = 6f,
    val chronometerBorderWidth: Float? = null,
    ) : TemplateData()

internal data class ZeroBezelTemplateData(
    override val templateType: TemplateType = TemplateType.ZERO_BEZEL,
    val baseContent: BaseContent,
    val actions: JSONArray? = null,
    val mediaData: MediaData,
    val showCollapsedBackgroundImage: Boolean = true,
    val collapsedMediaData: MediaData
) : TemplateData()

internal data class ProductTemplateData(
    override val templateType: TemplateType = TemplateType.PRODUCT_DISPLAY,
    val baseContent: BaseContent,
    val imageList: ArrayList<ImageData>,
    val scaleType: PTScaleType = PTScaleType.CENTER_CROP,
    val bigTextList: ArrayList<String>,
    val smallTextList: ArrayList<String>,
    val priceList: ArrayList<String>,
    val displayActionText: String? = null,
    val displayActionColor: String? = null,
    val displayActionTextColor: String? = null,
    val isLinear: Boolean = false,
    val imageBorderData: ImageBorderData = ImageBorderData(),
) : TemplateData()

internal data class InputBoxTemplateData(
    override val templateType: TemplateType = TemplateType.INPUT_BOX,
    val textData: BaseTextData,
    val actions: JSONArray? = null,
    val deepLinkList: ArrayList<String>,
    val imageData: ImageData,
    val inputLabel: String? = null,
    val inputFeedback: String? = null,
    val inputAutoOpen: String? = null,
    val dismissOnClick: String? = null,
    val notificationBehavior: NotificationBehavior
) : TemplateData()

internal data class CancelTemplateData(
    override val templateType: TemplateType = TemplateType.CANCEL,
    val cancelNotificationId: String? = null,
    val cancelNotificationIds: ArrayList<Int>
) : TemplateData()

internal enum class ButtonStyle(val key: String) {
    SOLID("solid"),
    GRADIENT_LINEAR("gradient_linear"),
    GRADIENT_RADIAL("gradient_radial");

    companion object {
        fun fromString(value: String?): ButtonStyle =
            entries.firstOrNull { it.key == value } ?: SOLID
    }
}

internal data class VerticalImageButtonData(
    val name: String,
    val deepLink: String? = null,
    val style: ButtonStyle = ButtonStyle.SOLID,
    val buttonColor: String? = null,
    val borderColor: String? = null,
    val textColor: String? = null,
    val gradientColor1: String? = null,
    val gradientColor2: String? = null,
    val gradientDirection: Double = PTConstants.PT_BTN_GRAD_DIR_DEFAULT,
    val borderRadius: Float = PTConstants.PT_BTN_BORDER_RADIUS_DEFAULT,
    val borderWidth: Float = PTConstants.PT_BTN_BORDER_WIDTH_DEFAULT,
)

internal data class VerticalImageTemplateData(
    override val templateType: TemplateType = TemplateType.VERTICAL_IMAGE,
    val baseContent: BaseContent,
    val mediaData: MediaData,
    val collapsedMediaData: MediaData?,
    val actions: JSONArray? = null,
    val text1: String? = null,
    val text2: String? = null,
    val text1Color: String? = null,
    val text2Color: String? = null,
    val buttonData: VerticalImageButtonData? = null,
    val collapsedButtonData: VerticalImageButtonData? = null,
) : TemplateData()