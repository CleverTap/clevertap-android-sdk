package com.clevertap.android.pushtemplates

import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants.ONE_SECOND_LONG
import com.clevertap.android.pushtemplates.PTConstants.PT_BG
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG_ALT
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG_ALT_ALT_TEXT
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG_ALT_TEXT
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG_COLLAPSED
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG_COLLAPSED_ALT_TEXT
import com.clevertap.android.pushtemplates.PTConstants.PT_CANCEL_NOTIF_ID
import com.clevertap.android.pushtemplates.PTConstants.PT_CHRONO_TITLE_COLOUR
import com.clevertap.android.pushtemplates.PTConstants.PT_DEFAULT_DL
import com.clevertap.android.pushtemplates.PTConstants.PT_DISMISS
import com.clevertap.android.pushtemplates.PTConstants.PT_DISMISS_ON_CLICK
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF_ALT
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF_COLLAPSED
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF_FRAMES
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF_FRAMES_ALT
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF_FRAMES_COLLAPSED
import com.clevertap.android.pushtemplates.PTConstants.PT_INPUT_AUTO_OPEN
import com.clevertap.android.pushtemplates.PTConstants.PT_INPUT_FEEDBACK
import com.clevertap.android.pushtemplates.PTConstants.PT_INPUT_LABEL
import com.clevertap.android.pushtemplates.PTConstants.PT_MANUAL_CAROUSEL_TYPE
import com.clevertap.android.pushtemplates.PTConstants.PT_META_CLR
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG_ALT
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG_COLOR
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG_SUMMARY
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG_SUMMARY_ALT
import com.clevertap.android.pushtemplates.PTConstants.PT_NOTIF_ICON
import com.clevertap.android.pushtemplates.PTConstants.PT_PRODUCT_DISPLAY_ACTION
import com.clevertap.android.pushtemplates.PTConstants.PT_PRODUCT_DISPLAY_ACTION_COLOUR
import com.clevertap.android.pushtemplates.PTConstants.PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR
import com.clevertap.android.pushtemplates.PTConstants.PT_PRODUCT_DISPLAY_LINEAR
import com.clevertap.android.pushtemplates.PTConstants.PT_RENDER_TERMINAL
import com.clevertap.android.pushtemplates.PTConstants.PT_SCALE_TYPE
import com.clevertap.android.pushtemplates.PTConstants.PT_SCALE_TYPE_ALT
import com.clevertap.android.pushtemplates.PTConstants.PT_SCALE_TYPE_COLLAPSED
import com.clevertap.android.pushtemplates.PTConstants.PT_SMALL_VIEW
import com.clevertap.android.pushtemplates.PTConstants.PT_STICKY
import com.clevertap.android.pushtemplates.PTConstants.PT_SUBTITLE
import com.clevertap.android.pushtemplates.PTConstants.PT_TITLE
import com.clevertap.android.pushtemplates.PTConstants.PT_TITLE_ALT
import com.clevertap.android.pushtemplates.PTConstants.PT_TITLE_COLOR
import com.clevertap.android.pushtemplates.handlers.TimerTemplateHandler
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.WZRK_COLOR
import org.json.JSONArray

/**
 * Factory class for creating TemplateData objects based on template type.
 * This factory follows the same parsing logic as TemplateRenderer and provides
 * a clean interface for instantiating the appropriate TemplateData subclass.
 */
internal object TemplateDataFactory {

    /**
     * Creates a TemplateData object based on the provided template type.
     *
     * @param templateType The type of template (from TemplateType enum)
     * @param extras Bundle containing template configuration data
     * @param isDarkMode Whether the device is in dark mode
     * @param defaultAltText Default alt text for images (e.g., from context.getString(R.string.pt_big_image_alt))
     * @return The appropriate TemplateData object or null if template type is not supported
     */
    fun createTemplateData(
        templateType: TemplateType?,
        extras: Bundle,
        isDarkMode: Boolean,
        defaultAltText: String,
        notificationIdsProvider: () -> ArrayList<Int>
    ): TemplateData? {
        // Handle dark mode colors (same logic as TemplateRenderer)
        val darkModeAdaptiveColors = Utils.createColorMap(extras, isDarkMode)

        return when (templateType) {
            TemplateType.BASIC -> createBasicTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.AUTO_CAROUSEL -> createAutoCarouselTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.MANUAL_CAROUSEL -> createManualCarouselTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.RATING -> createRatingTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.FIVE_ICONS -> createFiveIconsTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.PRODUCT_DISPLAY -> createProductTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.ZERO_BEZEL -> createZeroBezelTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.TIMER -> createTimerTemplateData(
                extras,
                darkModeAdaptiveColors,
                defaultAltText
            )

            TemplateType.INPUT_BOX -> createInputBoxTemplateData(extras, defaultAltText)
            TemplateType.CANCEL -> createCancelTemplateData(
                extras,
                notificationIdsProvider
            )

            else -> null
        }
    }

    private fun createBasicTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): BasicTemplateData {
        return BasicTemplateData(
            baseContent = createBaseContent(extras, colorMap),
            mediaData = createMediaData(extras, defaultAltText),
            actions = Utils.getActionKeys(extras)
        )
    }

    private fun createAutoCarouselTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): AutoCarouselTemplateData {
        return AutoCarouselTemplateData(
            carouselData = createCarouselData(extras, colorMap, defaultAltText),
            flipInterval = Utils.getFlipInterval(extras)
        )
    }

    private fun createManualCarouselTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): ManualCarouselTemplateData {
        return ManualCarouselTemplateData(
            carouselData = createCarouselData(extras, colorMap, defaultAltText),
            carouselType = extras.getString(PT_MANUAL_CAROUSEL_TYPE)
        )
    }

    private fun createRatingTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): RatingTemplateData {
        val defaultDeepLink = extras.getString(PT_DEFAULT_DL)
            ?: extras.getString(Constants.DEEP_LINK_KEY)

        return RatingTemplateData(
            baseContent = createBaseContent(extras, colorMap),
            mediaData = createMediaData(extras, defaultAltText),
            defaultDeepLink = defaultDeepLink
        )
    }

    private fun createFiveIconsTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): FiveIconsTemplateData {
        return FiveIconsTemplateData(
            imageList = Utils.getImageDataListFromExtras(extras, defaultAltText),
            deepLinkList = Utils.getDeepLinkListFromExtras(extras),
            backgroundColor = colorMap[PT_BG],
            title = getStringWithFallback(extras, PT_TITLE, Constants.NOTIF_TITLE),
            subtitle = getStringWithFallback(extras, PT_SUBTITLE, Constants.WZRK_SUBTITLE),
            notificationBehavior = createNotificationBehaviorData(extras)
        )
    }

    private fun createProductTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): ProductTemplateData {
        return ProductTemplateData(
            baseContent = createBaseContent(extras, colorMap),
            imageList = Utils.getImageDataListFromExtras(extras, defaultAltText),
            scaleType = PTScaleType.fromString(extras.getString(PT_SCALE_TYPE)),
            bigTextList = Utils.getBigTextFromExtras(extras),
            smallTextList = Utils.getSmallTextFromExtras(extras),
            priceList = Utils.getPriceFromExtras(extras),
            displayActionText = extras.getString(PT_PRODUCT_DISPLAY_ACTION),
            displayActionColor = colorMap[PT_PRODUCT_DISPLAY_ACTION_COLOUR],
            displayActionTextColor = colorMap[PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR],
            isLinear = extras.getString(PT_PRODUCT_DISPLAY_LINEAR)
                ?.equals("true", ignoreCase = true) ?: false
        )
    }

    private fun createZeroBezelTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): ZeroBezelTemplateData {
        val mediaData = createMediaData(extras, defaultAltText)
        return ZeroBezelTemplateData(
            baseContent = createBaseContent(extras, colorMap),
            actions = Utils.getActionKeys(extras),
            mediaData = mediaData,
            showCollapsedBackgroundImage = extras.getString(PT_SMALL_VIEW)?.equals("text_only", ignoreCase = true) != true,
            collapsedMediaData = createCollapsedMediaData(extras, mediaData)
        )
    }

    private fun createTimerTemplateData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): TimerTemplateData {
        val mediaData = createMediaData(extras, defaultAltText)
        val timerEnd = Utils.getTimerEnd(extras, System.currentTimeMillis())
        val timerThreshold = Utils.getTimerThreshold(extras)
        val dismissAfter = TimerTemplateHandler.getDismissAfterMs(timerEnd, timerThreshold)
        val baseContent = createBaseContent(extras, colorMap)
        val resolvedBaseContent = baseContent.copy(notificationBehavior = baseContent.notificationBehavior.copy(dismissAfter = dismissAfter))
        return TimerTemplateData(
            baseContent = resolvedBaseContent,
            mediaData = mediaData,
            actions = Utils.getActionKeys(extras),
            terminalTextData = createTerminalTextData(extras, baseContent.textData),
            terminalMediaData = createTerminalMediaData(extras, defaultAltText, mediaData),
            chronometerTitleColor = colorMap[PT_CHRONO_TITLE_COLOUR],
            renderTerminal = extras.getString(PT_RENDER_TERMINAL)
                ?.equals("true", ignoreCase = true) ?: true
        )
    }

    private fun createInputBoxTemplateData(
        extras: Bundle,
        defaultAltText: String
    ): InputBoxTemplateData {
        return InputBoxTemplateData(
            textData = createBaseTextData(extras),
            actions = Utils.getActionKeys(extras),
            deepLinkList = Utils.getDeepLinkListFromExtras(extras),
            imageData = createSingleImageData(extras, defaultAltText),
            inputLabel = extras.getString(PT_INPUT_LABEL),
            inputFeedback = extras.getString(PT_INPUT_FEEDBACK),
            inputAutoOpen = extras.getString(PT_INPUT_AUTO_OPEN),
            dismissOnClick = extras.getString(PT_DISMISS_ON_CLICK),
            notificationBehavior = createNotificationBehaviorData(extras)
        )
    }

    private fun createCancelTemplateData(
        extras: Bundle,
        notificationIdsProvider: () -> ArrayList<Int>
    ): CancelTemplateData {
        return CancelTemplateData(
            cancelNotificationId = extras.getString(PT_CANCEL_NOTIF_ID),
            cancelNotificationIds = notificationIdsProvider()
        )
    }

    // Helper methods for creating nested data objects
    private fun createBaseContent(extras: Bundle, colorMap: Map<String, String>): BaseContent {
        return BaseContent(
            textData = createBaseTextData(extras),
            colorData = createBaseColorData(colorMap, extras.getString(WZRK_COLOR)),
            iconData = createIconData(extras),
            deepLinkList = Utils.getDeepLinkListFromExtras(extras),
            notificationBehavior = createNotificationBehaviorData(extras)
        )
    }

    private fun createBaseTextData(extras: Bundle): BaseTextData {
        return BaseTextData(
            title = getStringWithFallback(extras, PT_TITLE, Constants.NOTIF_TITLE),
            message = getStringWithFallback(extras, PT_MSG, Constants.NOTIF_MSG),
            messageSummary = getStringWithFallback(
                extras,
                PT_MSG_SUMMARY,
                Constants.WZRK_MSG_SUMMARY
            ),
            subtitle = getStringWithFallback(extras, PT_SUBTITLE, Constants.WZRK_SUBTITLE)
        )
    }

    private fun createBaseColorData(
        colorMap: Map<String, String>,
        defaultColor: String?
    ): BaseColorData {
        return BaseColorData(
            titleColor = colorMap[PT_TITLE_COLOR],
            messageColor = colorMap[PT_MSG_COLOR],
            backgroundColor = colorMap[PT_BG],
            metaColor = colorMap[PT_META_CLR] ?: defaultColor,
        )
    }

    private fun createMediaData(extras: Bundle, defaultAltText: String): MediaData {
        val bigImage = getStringWithFallback(extras, PT_BIG_IMG, Constants.WZRK_BIG_PICTURE)
        val gif = extras.getString(PT_GIF)

        return MediaData(
            bigImage = ImageData(
                url = bigImage,
                altText = extras.getString(PT_BIG_IMG_ALT_TEXT, defaultAltText)
            ),
            gif = GifData(
                url = gif,
                numberOfFrames = extras.getString(PT_GIF_FRAMES)?.toIntOrNull() ?: 10
            ),
            scaleType = PTScaleType.fromString(extras.getString(PT_SCALE_TYPE))
        )
    }

    private fun createCollapsedMediaData(extras: Bundle, defaultMediaData: MediaData): MediaData {
        val bigImageCollapsed = extras.getString(PT_BIG_IMG_COLLAPSED)
        val gifCollapsed = extras.getString(PT_GIF_COLLAPSED)

        return MediaData(
            bigImage = ImageData(
                url = bigImageCollapsed ?: defaultMediaData.bigImage.url,
                altText = extras.getString(
                    PT_BIG_IMG_COLLAPSED_ALT_TEXT,
                    defaultMediaData.bigImage.altText
                )
            ),

            // Priority for picking url:
            // 1. Use gifCollapsed if available
            // 2. If gifCollapsed is null but bigImageCollapsed exists, force url = null
            // 3. If both are null, fallback to defaultMediaData.gif.url
            gif = GifData(
                url = when {
                    gifCollapsed != null -> gifCollapsed
                    bigImageCollapsed != null -> null
                    else -> defaultMediaData.gif.url
                },
                numberOfFrames = extras.getString(PT_GIF_FRAMES_COLLAPSED)?.toIntOrNull()
                    ?: defaultMediaData.gif.numberOfFrames
            ),
            scaleType = PTScaleType.fromString(
                extras.getString(
                    PT_SCALE_TYPE_COLLAPSED,
                    defaultMediaData.scaleType.name
                )
            )
        )
    }

    private fun createIconData(extras: Bundle): IconData {
        val largeIcon = extras.getString(PT_NOTIF_ICON)
        return IconData(largeIcon = largeIcon)
    }

    private fun createCarouselData(
        extras: Bundle,
        colorMap: Map<String, String>,
        defaultAltText: String
    ): CarouselData {
        return CarouselData(
            baseContent = createBaseContent(extras, colorMap),
            actions = Utils.getActionKeys(extras),
            imageList = Utils.getImageDataListFromExtras(extras, defaultAltText),
            scaleType = PTScaleType.fromString(extras.getString(PT_SCALE_TYPE))
        )
    }

    private fun createTerminalTextData(
        extras: Bundle,
        defaultTextData: BaseTextData
    ): BaseTextData {
        return BaseTextData(
            title = extras.getString(PT_TITLE_ALT) ?: defaultTextData.title,
            message = extras.getString(PT_MSG_ALT) ?: defaultTextData.message,
            messageSummary = extras.getString(PT_MSG_SUMMARY_ALT) ?: defaultTextData.messageSummary,
            subtitle = null // Terminal text data doesn't seem to have subtitle in the current implementation
        )
    }

    private fun createTerminalMediaData(
        extras: Bundle,
        defaultAltText: String,
        defaultMediaData: MediaData
    ): MediaData {
        val terminalBigImage = extras.getString(PT_BIG_IMG_ALT) ?: defaultMediaData.bigImage.url
        val terminalGif = extras.getString(PT_GIF_ALT)

        return MediaData(
            bigImage = ImageData(
                url = terminalBigImage,
                altText = extras.getString(PT_BIG_IMG_ALT_ALT_TEXT, defaultAltText)
            ),
            gif = GifData(
                url = terminalGif,
                numberOfFrames = extras.getString(PT_GIF_FRAMES_ALT)?.toIntOrNull() ?: defaultMediaData.gif.numberOfFrames
            ),
            scaleType = PTScaleType.fromString(
                extras.getString(
                    PT_SCALE_TYPE_ALT,
                    defaultMediaData.scaleType.name
                )
            )
        )
    }

    private fun createSingleImageData(extras: Bundle, defaultAltText: String): ImageData {
        val imageUrl = extras.getString(PT_BIG_IMG) ?: extras.getString(Constants.WZRK_BIG_PICTURE)

        return ImageData(
            url = imageUrl,
            altText = extras.getString(PT_BIG_IMG_ALT_TEXT, defaultAltText)
        )

    }

    private fun createNotificationBehaviorData(extras: Bundle) : NotificationBehavior {
        return NotificationBehavior(
            isSticky = extras.getString(PT_STICKY).toBoolean(),
            dismissAfter = extras.getString(PT_DISMISS)?.toLongOrNull()?.let { it * ONE_SECOND_LONG })
    }

    /**
     * Gets string value with fallback logic, same as TemplateRenderer.setKeysFromDashboard
     */
    private fun getStringWithFallback(
        extras: Bundle,
        primaryKey: String,
        fallbackKey: String
    ): String? {
        val primaryValue = extras.getString(primaryKey)
        return if (primaryValue.isNullOrEmpty()) {
            extras.getString(fallbackKey)
        } else {
            primaryValue
        }
    }

    /**
     * Converts a TimerTemplateData to BasicTemplateData by copying all compatible fields.
     * This is useful for converting a timer template to a basic template when the timer expires
     * or when fallback behavior is needed.
     *
     * @return A new BasicTemplateData with copied compatible fields
     */
    internal fun TimerTemplateData.toBasicTemplateData(): BasicTemplateData {
        return BasicTemplateData(
            baseContent = this.baseContent,
            mediaData = this.mediaData,
            actions = this.actions
        )
    }

    internal fun FiveIconsTemplateData.toBaseContent(): BaseContent {
        return BaseContent(
            textData = BaseTextData(title = this.title, subtitle = this.subtitle),
            colorData = BaseColorData(
                backgroundColor = this.backgroundColor,
            ),
            iconData = IconData(),
            deepLinkList = this.deepLinkList,
            notificationBehavior = this.notificationBehavior
        )
    }


    internal fun InputBoxTemplateData.toBaseContent(): BaseContent {
        return BaseContent(
            textData = this.textData,
            colorData = BaseColorData(),
            iconData = IconData(),
            deepLinkList = this.deepLinkList,
            notificationBehavior = this.notificationBehavior
        )
    }

    internal fun TemplateData.getActions(): JSONArray? {
        return when (this) {
            is BasicTemplateData -> {
                this.actions
            }

            is AutoCarouselTemplateData -> {
                this.carouselData.actions
            }

            is ManualCarouselTemplateData -> {
                this.carouselData.actions
            }

            is ZeroBezelTemplateData -> {
                this.actions
            }

            is TimerTemplateData -> {
                this.actions
            }

            is InputBoxTemplateData -> {
                this.actions
            }

            else -> null
        }
    }
}

