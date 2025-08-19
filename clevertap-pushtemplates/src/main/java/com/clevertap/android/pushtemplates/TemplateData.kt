package com.clevertap.android.pushtemplates

import android.app.PendingIntent
import android.graphics.Bitmap
import org.json.JSONArray
import java.util.ArrayList

data class ImageData(
    val url: String,
    val altText: String
)

data class GifData(
    val gif: String? = null,
    val numberOfFrames: Int = 10,
)

data class ActionButton(
    val id: String,
    val label: String,
    val icon: Int
)

data class BaseTextData(
    val title: String? = null,
    val message: String? = null,
    val messageSummary: String? = null,
    val subtitle: String? = null,
)

data class MediaData(
    val bigImage: ImageData? = null,
    val gif: GifData? = null,
    val scaleType: PTScaleType? = PTScaleType.CENTER_CROP,
)

data class IconData(
    val largeIcon: String? = null,
    val smallIcon: Bitmap? = null,
)

data class BaseColorData(
    val titleColor: String? = null,
    val messageColor: String? = null,
    val backgroundColor: String? = null,
    val metaColor: String? = null,
    val smallIconColor: String? = null,
)

data class ActionButtonData(
    val actions: JSONArray? = null,
    val actionButtons: List<ActionButton> = emptyList<ActionButton>(),
    val actionButtonPendingIntents: MutableMap<String, PendingIntent> = mutableMapOf<String, PendingIntent>()
)

data class CarouselData(
    val textData: BaseTextData,
    val colorData: BaseColorData,
    val actions: ActionButtonData,
    val mediaList: ArrayList<ImageData>? = null,
    val largeIcon: String? = null,
    val deepLinkList: ArrayList<String>? = null,
    val scaleType: PTScaleType? = PTScaleType.CENTER_CROP,
)

data class ProductTextData(
    val bigTextList: ArrayList<String>? = null,
    val smallTextList: ArrayList<String>? = null,
    val priceList: ArrayList<String>? = null,
)

data class BasicTemplateData(
    val textData: BaseTextData,
    val colorData: BaseColorData,
    val actions: ActionButtonData,
    val mediaData: MediaData? = null,
    val iconData: IconData? = null,
    val deepLinkList: ArrayList<String>? = null,
)

data class FiveIconsTemplateData(
    val imageList: ArrayList<ImageData>? = null,
    val deepLinkList: ArrayList<String>? = null,
    val backgroundColor: String? = null,
    val smallIconColor: String? = null,
    val title: String? = null,
)

data class ManualCarouselTemplateData(
    val carouselData: CarouselData,
    val carouselType: String? = null,
)

data class AutoCarouselTemplateData(
    val carouselData: CarouselData,
    val flipInterval: Int = 0,
)

data class RatingTemplateData(
    val textData: BaseTextData,
    val colorData: BaseColorData,
    val iconData: IconData? = null,
    val mediaData: MediaData? = null,
    val deepLinkList: ArrayList<String>? = null,
    val defaultDeepLink : String? = null,
)

data class TimerTemplateData(
    val textData: BaseTextData,
    val colorData: BaseColorData,
    val iconData: IconData? = null,
    val mediaData: MediaData? = null,
    val actions: ActionButtonData,
    val deepLinkList: ArrayList<String>? = null,
    val terminalTextData : BaseTextData,
    val terminalMediaData: MediaData? = null,
    val chronometerTitleColor: String? = null,
    val timerEnd: Int = 0,
    val timerThreshold: Int = 0,
    val renderTerminal: Boolean = true,
)

data class ZeroBezelTemplateData(
    val textData: BaseTextData,
    val colorData: BaseColorData,
    val actions: ActionButtonData,
    val iconData: IconData? = null,
    val mediaData: MediaData? = null,
    val deepLinkList: ArrayList<String>? = null,
    val smallView : String? = null,
    val collapsedMediaData: MediaData? = null,
)

data class ProductTemplateData(
    val textData: BaseTextData,
    val colorData: BaseColorData,
    val actions: ActionButtonData,
    val mediaData: MediaData? = null,
    val iconData: IconData? = null,
    val deepLinkList: ArrayList<String>? = null,
    val productTextData: ProductTextData,
    val displayActionText: String? = null,
    val displayActionColor: String? = null,
    val displayActionTextColor: String? = null,
    val isLinear: Boolean = false,
)

data class InputBoxTemplateData(
    val textData: BaseTextData,
    val actions: ActionButtonData,
    val iconData: IconData? = null,
    val imageData: ImageData? = null,
    val inputLabel: String? = null,
    val inputFeedback: String? = null,
    val inputAutoOpen: String? = null,
    val dismissOnClick : String? = null,
)

data class CancelTemplateData(
    val cancelNotificationId: String? = null,
    val cancelNotificationIds: ArrayList<Int>? = null,
)
