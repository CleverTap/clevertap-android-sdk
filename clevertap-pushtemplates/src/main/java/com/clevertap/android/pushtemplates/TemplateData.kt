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
 * Corner/border configuration for a template's content images.
 *
 * Both size values are percentages, never pixels. A pixel value cannot survive the trip: the
 * dashboard preview and the notification tray are different sizes, so the same number would be a
 * large corner in one place and an invisible one in the other. A percentage is resolved by each
 * side against its own container and therefore looks the same on both.
 *
 * **What the percentage is of: the displayed picture's height.** That one rule covers both scale
 * types, but it is reached two different ways, because only a CENTER_CROP image fills the area it
 * is given:
 *
 * - CENTER_CROP fills the area, so the picture's height *is* the area's height. The views draw the
 *   styling against [com.clevertap.android.pushtemplates.R.dimen.pt_image_style_reference].
 * - FIT_CENTER is scaled to fit and centred, so the picture is smaller than the area and neither
 *   view's edge is the picture's edge. The styling is baked into the bitmap against the bitmap's
 *   own height; the tray then scales bitmap and styling together, so the result is still the same
 *   share of the picture's height.
 *
 * The dashboard preview must follow the same rule, and in particular must take its percentage of
 * the *picture's* height for "keep original", not of the whole image area.
 *
 * [cornerRadiusPercent] is a percentage of that height. [borderWidthValue] is NOT: it is divided
 * by 1000, the way Native Display resolves a border width, so the same dashboard value gives the
 * same stroke on both channels.
 *
 * A border needs both of its keys: [borderWidthValue] switches it on and [borderColor] says what
 * to paint it with, so a colour without a width and a width without a colour both draw nothing.
 * [borderColor] is already parsed by [TemplateDataFactory], so an unparseable payload colour lands
 * here as null and the border is simply skipped while the corner radius still applies.
 */
internal data class ImageBorderData(
    val cornerRadiusPercent: Float = 0f,
    val borderWidthValue: Float = 0f,
    val borderColor: Int? = null,
    /**
     * Draw the Zero Bezel text scrim into the bitmap instead of leaving it to the separate scrim
     * view. That view covers the picture and has square corners of its own, so rounding only the
     * picture leaves a square card with a rounded picture inside it. Painting the scrim inside the
     * same rounded clip removes the square view from the picture entirely.
     *
     * Only meaningful while [isActive]: with no styling to draw there is nothing to round, and the
     * scrim view is left to do its normal job.
     */
    val withScrim: Boolean = false,
) {

    /** Nothing is stroked without both a width to draw and a colour to draw it in. */
    val hasBorder: Boolean get() = borderWidthValue > 0f && borderColor != null

    val isActive: Boolean get() = cornerRadiusPercent > 0f || hasBorder
}

/**
 * Image styling (corner radius + border) is only offered on API 31+. Below that the payload keys
 * are ignored at parse time (see TemplateDataFactory.createImageBorderData) and images render as
 * they always have. API 31 is where RemoteViews gained the outline and margin setters that let a
 * CENTER_CROP image keep its styling through the tray's crop.
 */
internal val useNativeImageStyling: Boolean
    get() = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

/**
 * Whether styling for an image shown with [this] scale type is drawn by the views rather than
 * baked into the bitmap.
 *
 * Only a CENTER_CROP image fills its view, so only there does a view border hug the picture and
 * only there would baking be cropped away. A FIT_CENTER image is scaled to fit and centred: a view
 * border would frame the empty band around it, and baking is exact because nothing is cropped.
 */
internal fun PTScaleType.usesNativeImageStyling(): Boolean =
    useNativeImageStyling && this == PTScaleType.CENTER_CROP

/**
 * The same styling with the Zero Bezel scrim folded in, so it is painted inside the rounded clip
 * rather than by the square-cornered scrim view sitting on top of the picture.
 *
 * Only for a scale type that fills the area. There, the picture, the scrim and the text all cover
 * the same rectangle, so moving the scrim into the picture loses nothing. A FIT_CENTER picture is
 * smaller than the area: its text still sits at the area's bottom edge, outside the picture, and
 * baking the scrim into the picture would put the darkening where there is no text while taking it
 * away from where there is. That case keeps the scrim view.
 *
 * Returns the data unchanged when there is no styling to draw: without a corner radius there is
 * nothing to round, and the scrim view can go on doing its normal job.
 */
internal fun ImageBorderData?.forZeroBezel(scaleType: PTScaleType): ImageBorderData? =
    if (this != null && isActive && scaleType.usesNativeImageStyling()) copy(withScrim = true)
    else this

/**
 * The styling to bake into a bitmap shown with [scaleType], or null when there is nothing to bake.
 *
 * A scrim can only ever be drawn into the bitmap, so when one is wanted it is baked in whatever the
 * scale type. The radius and the border are separate: where the views can draw those they still do,
 * because a baked corner or ring is cropped away by a CENTER_CROP. That leaves the bitmap carrying
 * the scrim alone, square-cornered, and the view's clip rounds it along with the picture.
 */
internal fun ImageBorderData?.bakedInto(scaleType: PTScaleType): ImageBorderData? = when {
    this == null -> null
    !scaleType.usesNativeImageStyling() -> this
    withScrim && isActive -> ImageBorderData(withScrim = true)
    else -> null
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
    // No image styling here, by product's decision: neither variant of Product Catalog takes a
    // corner radius, border width or border colour. The field is absent rather than left inert so
    // the exclusion cannot be undone by accident.
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