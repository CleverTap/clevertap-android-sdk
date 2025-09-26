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
    val renderTerminal: Boolean = true,
) : TemplateData()

internal data class ZeroBezelTemplateData(
    override val templateType: TemplateType = TemplateType.ZERO_BEZEL,
    val baseContent: BaseContent,
    val actions: JSONArray? = null,
    val mediaData: MediaData,
    val smallView: String? = null,
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