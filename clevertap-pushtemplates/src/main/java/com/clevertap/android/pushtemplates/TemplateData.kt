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

internal data class MediaData(
    val bigImage: ImageData,
    val gif: GifData,
    val scaleType: PTScaleType = PTScaleType.CENTER_CROP,
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
    val notificationBehavior: NotificationBehavior
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
    val chronometerGradientDirection: GradientDirection = GradientDirection.LEFT_RIGHT,
    val renderTerminal: Boolean = true,
    val chronometerBorderRadius: Float = 6f,
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
    GRADIENT("gradient");

    companion object {
        fun fromString(value: String?): ButtonStyle =
            entries.firstOrNull { it.key == value } ?: SOLID
    }
}

internal enum class GradientDirection(val key: String) {
    LEFT_RIGHT("left_right"),
    RIGHT_LEFT("right_left"),
    TOP_BOTTOM("top_bottom"),
    BOTTOM_TOP("bottom_top"),
    DIAGONAL_TL_BR("diagonal_tl_br"),
    DIAGONAL_BL_TR("diagonal_bl_tr"),
    RADIAL("radial");

    companion object {
        fun fromString(value: String?): GradientDirection =
            entries.firstOrNull { it.key == value } ?: LEFT_RIGHT
    }
}

internal data class VerticalImageButtonData(
    val name: String? = null,
    val deepLink: String? = null,
    val style: ButtonStyle = ButtonStyle.SOLID,
    val buttonColor: String? = null,
    val borderColor: String? = null,
    val textColor: String? = null,
    val gradientColor1: String? = null,
    val gradientColor2: String? = null,
    val gradientDirection: GradientDirection = GradientDirection.LEFT_RIGHT,
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