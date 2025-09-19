package com.clevertap.android.pushtemplates

import android.os.Build
import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants.*
import com.clevertap.android.sdk.Constants
import io.mockk.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class TemplateDataFactoryTest {

    private lateinit var mockBundle: Bundle
    private val defaultAltText = "Default Alt Text"
    private val mockNotificationIds = arrayListOf(1, 2, 3)
    private val notificationIdsProvider = { mockNotificationIds }

    companion object {
        private const val SAMPLE_TITLE = "Test Title"
        private const val SAMPLE_MESSAGE = "Test Message"
        private const val SAMPLE_SUMMARY = "Test Summary"
        private const val SAMPLE_SUBTITLE = "Test Subtitle"
        private const val SAMPLE_COLOR = "#FF0000"
        private const val SAMPLE_IMAGE_URL = "https://example.com/image.jpg"
        private const val SAMPLE_GIF_URL = "https://example.com/image.gif"
    }

    @Before
    fun setUp() {
        mockBundle = mockk<Bundle>(relaxed = true)
        
        // Mock static methods
        mockkStatic(Utils::class)
        
        // Default mock behaviors
        every { Utils.fromJson(any()) } returns mockBundle
        every { Utils.createColorMap(any(), any()) } returns mapOf(
            PT_TITLE_COLOR to SAMPLE_COLOR,
            PT_MSG_COLOR to SAMPLE_COLOR,
            PT_BG to SAMPLE_COLOR,
            PT_META_CLR to SAMPLE_COLOR,
            PT_SMALL_ICON_COLOUR to SAMPLE_COLOR,
            PT_CHRONO_TITLE_COLOUR to SAMPLE_COLOR,
            PT_PRODUCT_DISPLAY_ACTION_COLOUR to SAMPLE_COLOR,
            PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR to SAMPLE_COLOR
        )
        
        every { Utils.getActionKeys(any()) } returns JSONArray()
        every { Utils.getImageDataListFromExtras(any(), any()) } returns arrayListOf()
        every { Utils.getDeepLinkListFromExtras(any()) } returns arrayListOf()
        every { Utils.getBigTextFromExtras(any()) } returns arrayListOf()
        every { Utils.getSmallTextFromExtras(any()) } returns arrayListOf()
        every { Utils.getPriceFromExtras(any()) } returns arrayListOf()
        every { Utils.getFlipInterval(any()) } returns 4000
        every { Utils.getTimerEnd(any(), any()) } returns 300
        every { Utils.getTimerThreshold(any()) } returns 10
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createTemplateData should return null for null template type`() {
        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = null,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNull(result)
    }

    @Test
    fun `createTemplateData should return null for unsupported template type`() {
        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.VIDEO, // Not implemented
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNull(result)
    }

    @Test
    fun `createTemplateData should parse pt_json when provided`() {
        // Given
        val ptJsonObject = JSONObject().apply {
            put("test_key", "test_value")
        }
        every { mockBundle.getString(PT_JSON) } returns ptJsonObject.toString()
        
        // When
        TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        verify { Utils.fromJson(any()) }
    }

    @Test
    fun `createTemplateData should handle invalid pt_json gracefully`() {
        // Given
        every { mockBundle.getString(PT_JSON) } returns "invalid_json"
        
        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is BasicTemplateData)
    }

    @Test
    fun `createTemplateData should create BasicTemplateData for BASIC template type`() {
        // Given
        setupBasicMockBundle()

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is BasicTemplateData)
        
        val basicData = result as BasicTemplateData
        assertEquals(TemplateType.BASIC, basicData.templateType)
        assertNotNull(basicData.baseContent)
        assertNotNull(basicData.mediaData)
    }

    @Test
    fun `createTemplateData should create AutoCarouselTemplateData for AUTO_CAROUSEL template type`() {
        // Given
        setupBasicMockBundle()
        every { Utils.getFlipInterval(any()) } returns 5000

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.AUTO_CAROUSEL,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is AutoCarouselTemplateData)
        
        val carouselData = result as AutoCarouselTemplateData
        assertEquals(TemplateType.AUTO_CAROUSEL, carouselData.templateType)
        assertEquals(5000, carouselData.flipInterval)
        assertNotNull(carouselData.carouselData)
    }

    @Test
    fun `createTemplateData should create ManualCarouselTemplateData for MANUAL_CAROUSEL template type`() {
        // Given
        setupBasicMockBundle()
        val carouselType = "filmstrip"
        every { mockBundle.getString(PT_MANUAL_CAROUSEL_TYPE) } returns carouselType

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.MANUAL_CAROUSEL,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is ManualCarouselTemplateData)
        
        val manualCarouselData = result as ManualCarouselTemplateData
        assertEquals(TemplateType.MANUAL_CAROUSEL, manualCarouselData.templateType)
        assertEquals(carouselType, manualCarouselData.carouselType)
        assertNotNull(manualCarouselData.carouselData)
    }

    @Test
    fun `createTemplateData should create RatingTemplateData for RATING template type`() {
        // Given
        setupBasicMockBundle()
        val defaultDeepLink = "https://example.com/rating"
        every { mockBundle.getString(PT_DEFAULT_DL) } returns defaultDeepLink

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.RATING,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is RatingTemplateData)
        
        val ratingData = result as RatingTemplateData
        assertEquals(TemplateType.RATING, ratingData.templateType)
        assertEquals(defaultDeepLink, ratingData.defaultDeepLink)
        assertNotNull(ratingData.baseContent)
        assertNotNull(ratingData.mediaData)
    }

    @Test
    fun `createTemplateData should create FiveIconsTemplateData for FIVE_ICONS template type`() {
        // Given
        setupBasicMockBundle()
        val imageList = arrayListOf(ImageData("url1", "alt1"), ImageData("url2", "alt2"))
        val deepLinkList = arrayListOf("dl1", "dl2")
        
        every { Utils.getImageDataListFromExtras(any(), any()) } returns imageList
        every { Utils.getDeepLinkListFromExtras(any()) } returns deepLinkList

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.FIVE_ICONS,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is FiveIconsTemplateData)
        
        val fiveIconsData = result as FiveIconsTemplateData
        assertEquals(TemplateType.FIVE_ICONS, fiveIconsData.templateType)
        assertEquals(imageList, fiveIconsData.imageList)
        assertEquals(deepLinkList, fiveIconsData.deepLinkList)
        assertEquals(SAMPLE_TITLE, fiveIconsData.title)
    }

    @Test
    fun `createTemplateData should create ProductTemplateData for PRODUCT_DISPLAY template type`() {
        // Given
        setupBasicMockBundle()
        val imageList = arrayListOf(ImageData("product1", "alt1"))
        val bigTextList = arrayListOf("Big Text 1")
        val smallTextList = arrayListOf("Small Text 1")
        val priceList = arrayListOf("$99.99")
        val displayAction = "Buy Now"
        
        every { Utils.getImageDataListFromExtras(any(), any()) } returns imageList
        every { Utils.getBigTextFromExtras(any()) } returns bigTextList
        every { Utils.getSmallTextFromExtras(any()) } returns smallTextList
        every { Utils.getPriceFromExtras(any()) } returns priceList
        every { mockBundle.getString(PT_PRODUCT_DISPLAY_ACTION) } returns displayAction
        every { mockBundle.getString(PT_PRODUCT_DISPLAY_LINEAR) } returns "false"

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.PRODUCT_DISPLAY,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is ProductTemplateData)
        
        val productData = result as ProductTemplateData
        assertEquals(TemplateType.PRODUCT_DISPLAY, productData.templateType)
        assertEquals(imageList, productData.imageList)
        assertEquals(bigTextList, productData.bigTextList)
        assertEquals(smallTextList, productData.smallTextList)
        assertEquals(priceList, productData.priceList)
        assertEquals(displayAction, productData.displayActionText)
        assertFalse(productData.isLinear)
    }

    @Test
    fun `createTemplateData should create ZeroBezelTemplateData for ZERO_BEZEL template type`() {
        // Given
        setupBasicMockBundle()
        val smallView = "text_only"
        every { mockBundle.getString(PT_SMALL_VIEW) } returns smallView

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.ZERO_BEZEL,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is ZeroBezelTemplateData)
        
        val zeroBezelData = result as ZeroBezelTemplateData
        assertEquals(TemplateType.ZERO_BEZEL, zeroBezelData.templateType)
        assertEquals(smallView, zeroBezelData.smallView)
        assertNotNull(zeroBezelData.baseContent)
        assertNotNull(zeroBezelData.mediaData)
        assertNotNull(zeroBezelData.collapsedMediaData)
    }

    @Test
    fun `createTemplateData should create TimerTemplateData for TIMER template type`() {
        // Given
        setupBasicMockBundle()
        val timerEnd = 300
        val timerThreshold = 10
        val chronoColor = "#00FF00"
        
        every { Utils.getTimerEnd(any(), any()) } returns timerEnd
        every { Utils.getTimerThreshold(any()) } returns timerThreshold
        every { Utils.createColorMap(any(), any()) } returns mapOf(
            PT_CHRONO_TITLE_COLOUR to chronoColor
        )
        every { mockBundle.getString(PT_RENDER_TERMINAL) } returns "true"

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.TIMER,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is TimerTemplateData)
        
        val timerData = result as TimerTemplateData
        assertEquals(TemplateType.TIMER, timerData.templateType)
        assertEquals(timerEnd, timerData.timerEnd)
        assertEquals(timerThreshold, timerData.timerThreshold)
        assertEquals(chronoColor, timerData.chronometerTitleColor)
        assertTrue(timerData.renderTerminal)
        assertNotNull(timerData.baseContent)
        assertNotNull(timerData.mediaData)
        assertNotNull(timerData.terminalTextData)
        assertNotNull(timerData.terminalMediaData)
    }

    @Test
    fun `createTemplateData should create InputBoxTemplateData for INPUT_BOX template type`() {
        // Given
        setupBasicMockBundle()
        val inputLabel = "Enter your feedback"
        val inputFeedback = "Thank you!"
        val inputAutoOpen = "true"
        val dismissOnClick = "false"
        
        every { mockBundle.getString(PT_INPUT_LABEL) } returns inputLabel
        every { mockBundle.getString(PT_INPUT_FEEDBACK) } returns inputFeedback
        every { mockBundle.getString(PT_INPUT_AUTO_OPEN) } returns inputAutoOpen
        every { mockBundle.getString(PT_DISMISS_ON_CLICK) } returns dismissOnClick

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.INPUT_BOX,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is InputBoxTemplateData)
        
        val inputBoxData = result as InputBoxTemplateData
        assertEquals(TemplateType.INPUT_BOX, inputBoxData.templateType)
        assertEquals(inputLabel, inputBoxData.inputLabel)
        assertEquals(inputFeedback, inputBoxData.inputFeedback)
        assertEquals(inputAutoOpen, inputBoxData.inputAutoOpen)
        assertEquals(dismissOnClick, inputBoxData.dismissOnClick)
        assertNotNull(inputBoxData.textData)
        assertNotNull(inputBoxData.imageData)
    }

    @Test
    fun `createTemplateData should create CancelTemplateData for CANCEL template type`() {
        // Given
        val cancelNotifId = "123"
        every { mockBundle.getString(PT_CANCEL_NOTIF_ID) } returns cancelNotifId

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.CANCEL,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertNotNull(result)
        assertTrue(result is CancelTemplateData)
        
        val cancelData = result as CancelTemplateData
        assertEquals(TemplateType.CANCEL, cancelData.templateType)
        assertEquals(cancelNotifId, cancelData.cancelNotificationId)
        assertEquals(mockNotificationIds, cancelData.cancelNotificationIds)
    }

    @Test
    fun `createTemplateData should handle dark mode colors correctly`() {
        // Given
        setupBasicMockBundle()
        val darkModeColors = mapOf(
            PT_TITLE_COLOR to "#FFFFFF",
            PT_MSG_COLOR to "#CCCCCC",
            PT_BG to "#000000"
        )
        every { Utils.createColorMap(any(), eq(true)) } returns darkModeColors

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = true,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        verify { Utils.createColorMap(any(), eq(true)) }
        assertNotNull(result)
        assertTrue(result is BasicTemplateData)
    }

    @Test
    fun `getStringWithFallback should return primary value when available`() {
        // Given
        every { mockBundle.getString(PT_TITLE) } returns SAMPLE_TITLE
        every { mockBundle.getString(Constants.NOTIF_TITLE) } returns "Fallback Title"
        
        setupBasicMockBundle()

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertTrue(result is BasicTemplateData)
        assertEquals(SAMPLE_TITLE, (result as BasicTemplateData).baseContent.textData.title)
    }

    @Test
    fun `getStringWithFallback should return fallback value when primary is null`() {
        // Given
        setupBasicMockBundle()

        val fallbackTitle = "Fallback Title"
        every { mockBundle.getString(PT_TITLE) } returns null
        every { mockBundle.getString(Constants.NOTIF_TITLE) } returns fallbackTitle

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertTrue(result is BasicTemplateData)
        assertEquals(fallbackTitle, (result as BasicTemplateData).baseContent.textData.title)
    }

    @Test
    fun `getStringWithFallback should return fallback value when primary is empty`() {
        // Given
        setupBasicMockBundle()

        val fallbackTitle = "Fallback Title"
        every { mockBundle.getString(PT_TITLE) } returns ""
        every { mockBundle.getString(Constants.NOTIF_TITLE) } returns fallbackTitle

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertTrue(result is BasicTemplateData)
        assertEquals(fallbackTitle, (result as BasicTemplateData).baseContent.textData.title)
    }

    @Test
    fun `TimerTemplateData_toBasicTemplateData should create correct BasicTemplateData`() {
        // Given
        val timerData = createSampleTimerTemplateData()

        // When
        val basicData = with(TemplateDataFactory) { timerData.toBasicTemplateData() }

        // Then
        assertEquals(TemplateType.BASIC, basicData.templateType)
        assertEquals(timerData.baseContent, basicData.baseContent)
        assertEquals(timerData.mediaData, basicData.mediaData)
        assertEquals(timerData.actions, basicData.actions)
    }

    @Test
    fun `FiveIconsTemplateData_toBaseContent should create correct BaseContent`() {
        // Given
        val fiveIconsData = createSampleFiveIconsTemplateData()

        // When
        val baseContent = with(TemplateDataFactory) { fiveIconsData.toBaseContent() }

        // Then
        assertEquals(fiveIconsData.title, baseContent.textData.title)
        assertEquals(fiveIconsData.subtitle, baseContent.textData.subtitle)
        assertEquals(fiveIconsData.backgroundColor, baseContent.colorData.backgroundColor)
        assertEquals(fiveIconsData.smallIconColor, baseContent.colorData.smallIconColor)
        assertEquals(fiveIconsData.deepLinkList, baseContent.deepLinkList)
    }

    @Test
    fun `InputBoxTemplateData_toBaseContent should create correct BaseContent`() {
        // Given
        val inputBoxData = createSampleInputBoxTemplateData()

        // When
        val baseContent = with(TemplateDataFactory) { inputBoxData.toBaseContent() }

        // Then
        assertEquals(inputBoxData.textData, baseContent.textData)
        assertEquals(inputBoxData.deepLinkList, baseContent.deepLinkList)
        assertNotNull(baseContent.colorData)
        assertNotNull(baseContent.iconData)
    }

    @Test
    fun `getActions should return correct actions for BasicTemplateData`() {
        // Given
        val actions = JSONArray().apply { put("action1") }
        val basicData = BasicTemplateData(
            baseContent = createSampleBaseContent(),
            mediaData = createSampleMediaData(),
            actions = actions
        )

        // When
        val result = with(TemplateDataFactory) { basicData.getActions() }

        // Then
        assertEquals(actions, result)
    }

    @Test
    fun `getActions should return null for FiveIconsTemplateData`() {
        // Given
        val fiveIconsData = createSampleFiveIconsTemplateData()

        // When
        val result = with(TemplateDataFactory) { fiveIconsData.getActions() }

        // Then
        assertNull(result)
    }

    @Test
    fun `getActions should return carousel actions for AutoCarouselTemplateData`() {
        // Given
        val actions = JSONArray().apply { put("carousel_action") }
        val carouselData = CarouselData(
            baseContent = createSampleBaseContent(),
            actions = actions,
            imageList = arrayListOf(),
            scaleType = PTScaleType.CENTER_CROP
        )
        val autoCarouselData = AutoCarouselTemplateData(
            carouselData = carouselData,
            flipInterval = 4000
        )

        // When
        val result = with(TemplateDataFactory) { autoCarouselData.getActions() }

        // Then
        assertEquals(actions, result)
    }

    @Test
    fun `createCollapsedMediaData should use collapsed values when provided`() {
        // Given
        setupBasicMockBundle()
        val collapsedImage = "collapsed_image.jpg"
        val collapsedGif = "collapsed.gif"
        val collapsedFrames = "5"
        val collapsedAltText = "Collapsed Alt"
        
        every { mockBundle.getString(PT_BIG_IMG_COLLAPSED) } returns collapsedImage
        every { mockBundle.getString(PT_GIF_COLLAPSED) } returns collapsedGif
        every { mockBundle.getString(PT_GIF_FRAMES_COLLAPSED) } returns collapsedFrames
        every { mockBundle.getString(PT_BIG_IMG_COLLAPSED_ALT_TEXT) } returns collapsedAltText
        every { mockBundle.getString(PT_SCALE_TYPE_COLLAPSED) } returns "FIT_CENTER"

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.ZERO_BEZEL,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertTrue(result is ZeroBezelTemplateData)
        val zeroBezelData = result as ZeroBezelTemplateData
        assertEquals(collapsedImage, zeroBezelData.collapsedMediaData.bigImage.url)
        assertEquals(collapsedGif, zeroBezelData.collapsedMediaData.gif.url)
        assertEquals(5, zeroBezelData.collapsedMediaData.gif.numberOfFrames)
        assertEquals(collapsedAltText, zeroBezelData.collapsedMediaData.bigImage.altText)
        assertEquals(PTScaleType.FIT_CENTER, zeroBezelData.collapsedMediaData.scaleType)
    }

    @Test
    fun `createCollapsedMediaData should use default values when collapsed values are null`() {
        // Given
        setupBasicMockBundle()
        every { mockBundle.getString(PT_BIG_IMG_COLLAPSED) } returns null
        every { mockBundle.getString(PT_GIF_COLLAPSED) } returns null
        every { mockBundle.getString(PT_GIF_FRAMES_COLLAPSED) } returns null
        every { mockBundle.getString(PT_BIG_IMG_COLLAPSED_ALT_TEXT) } returns null
        every { mockBundle.getString(PT_SCALE_TYPE_COLLAPSED) } returns null

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.ZERO_BEZEL,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertTrue(result is ZeroBezelTemplateData)
        val zeroBezelData = result as ZeroBezelTemplateData
        
        // Should use default media data values
        assertEquals(zeroBezelData.mediaData.bigImage.url, zeroBezelData.collapsedMediaData.bigImage.url)
        assertEquals(zeroBezelData.mediaData.gif.url, zeroBezelData.collapsedMediaData.gif.url)
        assertEquals(zeroBezelData.mediaData.gif.numberOfFrames, zeroBezelData.collapsedMediaData.gif.numberOfFrames)
        assertEquals(zeroBezelData.mediaData.scaleType, zeroBezelData.collapsedMediaData.scaleType)
    }

    @Test
    fun `createTerminalTextData should use terminal values when provided`() {
        // Given
        setupBasicMockBundle()
        val terminalTitle = "Terminal Title"
        val terminalMessage = "Terminal Message"
        val terminalSummary = "Terminal Summary"
        
        every { mockBundle.getString(PT_TITLE_ALT) } returns terminalTitle
        every { mockBundle.getString(PT_MSG_ALT) } returns terminalMessage
        every { mockBundle.getString(PT_MSG_SUMMARY_ALT) } returns terminalSummary

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.TIMER,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertTrue(result is TimerTemplateData)
        val timerData = result as TimerTemplateData
        assertEquals(terminalTitle, timerData.terminalTextData.title)
        assertEquals(terminalMessage, timerData.terminalTextData.message)
        assertEquals(terminalSummary, timerData.terminalTextData.messageSummary)
        assertNull(timerData.terminalTextData.subtitle) // Terminal doesn't have subtitle
    }

    @Test
    fun `createTerminalTextData should use default values when terminal values are null`() {
        // Given
        setupBasicMockBundle()
        every { mockBundle.getString(PT_TITLE_ALT) } returns null
        every { mockBundle.getString(PT_MSG_ALT) } returns null
        every { mockBundle.getString(PT_MSG_SUMMARY_ALT) } returns null

        // When
        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.TIMER,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        // Then
        assertTrue(result is TimerTemplateData)
        val timerData = result as TimerTemplateData
        
        // Should use base content text data as default
        assertEquals(timerData.baseContent.textData.title, timerData.terminalTextData.title)
        assertEquals(timerData.baseContent.textData.message, timerData.terminalTextData.message)
        assertEquals(timerData.baseContent.textData.messageSummary, timerData.terminalTextData.messageSummary)
    }

    @Test
    fun `PTScaleType_fromString should handle case insensitive values`() {
        // Test that the factory correctly uses PTScaleType.fromString
        setupBasicMockBundle()
        every { mockBundle.getString(PT_SCALE_TYPE) } returns "center_crop"

        val result = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.BASIC,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        assertTrue(result is BasicTemplateData)
        assertEquals(PTScaleType.CENTER_CROP, (result as BasicTemplateData).mediaData.scaleType)
    }

    // Helper methods
    private fun setupBasicMockBundle() {
        every { mockBundle.getString(PT_JSON) } returns null
        every { mockBundle.getString(PT_TITLE) } returns SAMPLE_TITLE
        every { mockBundle.getString(Constants.NOTIF_TITLE) } returns SAMPLE_TITLE
        every { mockBundle.getString(PT_MSG) } returns SAMPLE_MESSAGE
        every { mockBundle.getString(Constants.NOTIF_MSG) } returns SAMPLE_MESSAGE
        every { mockBundle.getString(PT_MSG_SUMMARY) } returns SAMPLE_SUMMARY
        every { mockBundle.getString(Constants.WZRK_MSG_SUMMARY) } returns SAMPLE_SUMMARY
        every { mockBundle.getString(PT_SUBTITLE) } returns SAMPLE_SUBTITLE
        every { mockBundle.getString(Constants.WZRK_SUBTITLE) } returns SAMPLE_SUBTITLE
        every { mockBundle.getString(PT_BIG_IMG) } returns SAMPLE_IMAGE_URL
        every { mockBundle.getString(Constants.WZRK_BIG_PICTURE) } returns SAMPLE_IMAGE_URL
        every { mockBundle.getString(PT_BIG_IMG_ALT_TEXT) } returns defaultAltText
        every { mockBundle.getString(PT_GIF) } returns SAMPLE_GIF_URL
        every { mockBundle.getString(PT_GIF_FRAMES) } returns "10"
        every { mockBundle.getString(PT_SCALE_TYPE) } returns "CENTER_CROP"
        every { mockBundle.getString(PT_NOTIF_ICON) } returns "icon_url"
        every { mockBundle.getString(Constants.WZRK_COLOR) } returns SAMPLE_COLOR
    }

    private fun createSampleBaseContent(): BaseContent {
        return BaseContent(
            textData = BaseTextData(SAMPLE_TITLE, SAMPLE_MESSAGE, SAMPLE_SUMMARY, SAMPLE_SUBTITLE),
            colorData = BaseColorData(),
            iconData = IconData(),
            deepLinkList = arrayListOf()
        )
    }

    private fun createSampleMediaData(): MediaData {
        return MediaData(
            bigImage = ImageData(SAMPLE_IMAGE_URL, defaultAltText),
            gif = GifData(SAMPLE_GIF_URL, 10),
            scaleType = PTScaleType.CENTER_CROP
        )
    }

    private fun createSampleTimerTemplateData(): TimerTemplateData {
        return TimerTemplateData(
            baseContent = createSampleBaseContent(),
            mediaData = createSampleMediaData(),
            actions = JSONArray(),
            terminalTextData = BaseTextData("Terminal", "Terminal Msg", "Terminal Summary", null),
            terminalMediaData = createSampleMediaData(),
            chronometerTitleColor = SAMPLE_COLOR,
            timerEnd = 300,
            timerThreshold = 10,
            renderTerminal = true
        )
    }

    private fun createSampleFiveIconsTemplateData(): FiveIconsTemplateData {
        return FiveIconsTemplateData(
            imageList = arrayListOf(),
            deepLinkList = arrayListOf("dl1", "dl2"),
            backgroundColor = SAMPLE_COLOR,
            smallIconColor = SAMPLE_COLOR,
            title = SAMPLE_TITLE,
            subtitle = SAMPLE_SUBTITLE
        )
    }

    private fun createSampleInputBoxTemplateData(): InputBoxTemplateData {
        return InputBoxTemplateData(
            textData = BaseTextData(SAMPLE_TITLE, SAMPLE_MESSAGE),
            actions = JSONArray(),
            deepLinkList = arrayListOf("dl1"),
            imageData = ImageData(SAMPLE_IMAGE_URL, defaultAltText),
            inputLabel = "Input Label",
            inputFeedback = "Feedback",
            inputAutoOpen = "true",
            dismissOnClick = "false"
        )
    }
}
