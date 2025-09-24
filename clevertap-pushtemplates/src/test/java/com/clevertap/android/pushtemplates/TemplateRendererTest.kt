package com.clevertap.android.pushtemplates

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.TemplateDataFactory.toBasicTemplateData
import com.clevertap.android.pushtemplates.content.FiveIconBigContentView
import com.clevertap.android.pushtemplates.content.FiveIconSmallContentView
import com.clevertap.android.pushtemplates.handlers.CancelTemplateHandler
import com.clevertap.android.pushtemplates.handlers.TimerTemplateHandler
import com.clevertap.android.pushtemplates.styles.*
import com.clevertap.android.pushtemplates.validators.ContentValidator
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.NOTIF_MSG
import com.clevertap.android.sdk.Constants.NOTIF_TITLE
import com.clevertap.android.sdk.ManifestInfo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TemplateRendererTest {

    @MockK
    private lateinit var mockNotificationBuilder: NotificationCompat.Builder

    @MockK
    private lateinit var mockConfig: CleverTapInstanceConfig

    @MockK
    private lateinit var mockContentValidator: ContentValidator

    @MockK
    private lateinit var mockNotificationManager: NotificationManager

    @MockK
    private lateinit var mockBitmap: Bitmap

    @MockK
    private lateinit var mockManifestInfo: ManifestInfo

    @MockK(relaxed = true)
    private lateinit var mockBasicTemplateData: BasicTemplateData

    @MockK(relaxed = true)
    private lateinit var mockAutoCarouselTemplateData: AutoCarouselTemplateData

    @MockK(relaxed = true)
    private lateinit var mockManualCarouselTemplateData: ManualCarouselTemplateData

    @MockK(relaxed = true)
    private lateinit var mockRatingTemplateData: RatingTemplateData

    @MockK(relaxed = true)
    private lateinit var mockFiveIconsTemplateData: FiveIconsTemplateData

    @MockK(relaxed = true)
    private lateinit var mockProductTemplateData: ProductTemplateData

    @MockK(relaxed = true)
    private lateinit var mockZeroBezelTemplateData: ZeroBezelTemplateData

    @MockK(relaxed = true)
    private lateinit var mockTimerTemplateData: TimerTemplateData

    @MockK(relaxed = true)
    private lateinit var mockInputBoxTemplateData: InputBoxTemplateData

    @MockK(relaxed = true)
    private lateinit var mockCancelTemplateData: CancelTemplateData

    private lateinit var testBundle: Bundle
    private lateinit var templateRenderer: TemplateRenderer
    private lateinit var context: Context
    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testBundle = Bundle()

        // Use Robolectric's application context for ShadowPackageManager
        context = RuntimeEnvironment.getApplication()
        shadowPackageManager = shadowOf(context.packageManager)

        val service = ServiceInfo().also {
            it.name = "com.clevertap.android.sdk.pushnotification.CTNotificationIntentService"
            it.packageName = "com.clevertap.android.pushtemplates.test"
        }
        val packageInfo = PackageInfo().also {
            it.services = arrayOf(service)
            it.packageName = "com.clevertap.android.pushtemplates.test"
        }

        shadowPackageManager.installPackage(packageInfo)

        // Mock static methods
        mockkObject(TemplateDataFactory)
        mockkObject(ValidatorFactory)
        mockkStatic(Utils::class)
        mockkStatic(ManifestInfo::class)

        // Default bundle setup
        testBundle.putString(PTConstants.PT_ID, "pt_carousel")
        testBundle.putString(PTConstants.PT_TITLE, "Test Title")
        testBundle.putString(PTConstants.PT_MSG, "Test Message")

        // Setup common mocks
        every { ManifestInfo.getInstance(any()) } returns mockManifestInfo
        every { mockManifestInfo.intentServiceName } returns "com.clevertap.android.sdk.pushnotification.CTNotificationIntentService"
        every { Utils.isDarkMode(any()) } returns false

        // We use a mock for NotificationManager as this is simpler to verify
        mockNotificationManager = mockk(relaxed = true)

        // Create TemplateRenderer instance with the mock context
        templateRenderer = TemplateRenderer(context, testBundle, mockConfig)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun test_getTitle_returnsCorrectTitle() {
        // Act
        val title = templateRenderer.getTitle(testBundle, context)

        // Assert
        assertEquals("Test Title", title)
    }

    @Test
    fun test_getMessage_returnsCorrectMessage() {
        // Act
        val message = templateRenderer.getMessage(testBundle)

        // Assert
        assertEquals("Test Message", message)
    }

    @Test
    fun test_getMessage_emptyMainMessage() {
        val fallbackBundle = Bundle()
        fallbackBundle.putString(PTConstants.PT_MSG, "")
        fallbackBundle.putString(NOTIF_MSG, "Fallback Message")

        // Act
        val message = templateRenderer.getMessage(fallbackBundle)

        // Assert
        assertEquals("Fallback Message", message)
    }

    @Test
    fun test_getTitle_emptyMainTitle() {
        val fallbackBundle = Bundle()
        fallbackBundle.putString(PTConstants.PT_TITLE, "")
        fallbackBundle.putString(NOTIF_TITLE, "Fallback Title")

        // Act
        val message = templateRenderer.getTitle(fallbackBundle, context)

        // Assert
        assertEquals("Fallback Title", message)
    }


    @Test
    fun test_getMessage_fallbackCorrectMessage() {
        val fallbackBundle = Bundle()
        fallbackBundle.putString(NOTIF_MSG, "Fallback Message")

        // Act
        val message = templateRenderer.getMessage(fallbackBundle)

        // Assert
        assertEquals("Fallback Message", message)
    }

    @Test
    fun test_getTitle_fallbackCorrectTitle() {
        val fallbackBundle = Bundle()
        fallbackBundle.putString(NOTIF_TITLE, "Fallback Title")

        // Act
        val message = templateRenderer.getTitle(fallbackBundle, context)

        // Assert
        assertEquals("Fallback Title", message)
    }

    @Test
    fun test_getCollapseKey_returnsCorrectValue() {
        // Arrange
        val expectedCollapseKey = "test_collapse_key"
        testBundle.putString(PTConstants.PT_COLLAPSE_KEY, expectedCollapseKey)

        // Act
        val collapseKey = templateRenderer.getCollapseKey(testBundle)

        // Assert
        assertEquals(expectedCollapseKey, collapseKey)
    }

    @Test
    fun test_getCollapse_returnsFallbackValue() {
        // Arrange
        val expectedCollapseKey = "test_fallback_collapse_key"
        val fallbackBundle = Bundle()
        fallbackBundle.putString(Constants.WZRK_COLLAPSE, expectedCollapseKey)

        // Act
        val collapseKey = templateRenderer.getCollapseKey(fallbackBundle)

        // Assert
        assertEquals(expectedCollapseKey, collapseKey)
    }

    @Test
    fun test_setSmallIcon_setsIconCorrectly() {
        // Arrange
        val testIcon = 123
        every { Utils.setBitMapColour(any(), testIcon, any(), any()) } returns mockBitmap

        // Act
        templateRenderer.setSmallIcon(testIcon, context)

        // Assert
        verify { Utils.setBitMapColour(any(), testIcon, any(), any()) }
        assertEquals(testIcon, templateRenderer.smallIcon)
    }


    @Test
    fun test_setSmallIcon_throwsNPE() {
        // Arrange
        val testIcon = 123
        every { Utils.setBitMapColour(any(), testIcon, any(), any()) } throws NullPointerException()

        // Act
        templateRenderer.setSmallIcon(testIcon, context)

        // Assert
        verify { Utils.setBitMapColour(any(), testIcon, any(), any()) }
    }

    @Test
    fun test_renderNotification_basic_template_valid() {
        val basicBundle = Bundle(testBundle)
        basicBundle.putString(PTConstants.PT_ID, "pt_basic")

        val templateRendererLocal = TemplateRenderer(context, basicBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.BASIC,
                basicBundle,
                false,
                any(),
                any()
            )
        } returns mockBasicTemplateData
        every { ValidatorFactory.getValidator(mockBasicTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(BasicStyle::class)
        every {
            anyConstructed<BasicStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            basicBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<BasicStyle>().builderFromStyle(
                any(),
                basicBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun test_renderNotification_basic_template_invalid() {
        val basicBundle = Bundle(testBundle)
        basicBundle.putString(PTConstants.PT_ID, "pt_basic")

        val templateRendererLocal = TemplateRenderer(context, basicBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.BASIC,
                basicBundle,
                false,
                any(),
                any()
            )
        } returns mockBasicTemplateData
        every { ValidatorFactory.getValidator(mockBasicTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            basicBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_auto_carousel_template_invalid() {
        val autoCarouselBundle = Bundle(testBundle)
        autoCarouselBundle.putString(PTConstants.PT_ID, "pt_carousel")

        val templateRendererLocal = TemplateRenderer(context, autoCarouselBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.AUTO_CAROUSEL,
                autoCarouselBundle,
                false,
                any(),
                any()
            )
        } returns mockAutoCarouselTemplateData
        every { ValidatorFactory.getValidator(mockAutoCarouselTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            autoCarouselBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_manual_carousel_template_invalid() {
        val manualCarouselBundle = Bundle(testBundle)
        manualCarouselBundle.putString(PTConstants.PT_ID, "pt_manual_carousel")

        val templateRendererLocal = TemplateRenderer(context, manualCarouselBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.MANUAL_CAROUSEL,
                manualCarouselBundle,
                false,
                any(),
                any()
            )
        } returns mockManualCarouselTemplateData
        every { ValidatorFactory.getValidator(mockManualCarouselTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            manualCarouselBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_rating_template_invalid() {
        val ratingBundle = Bundle(testBundle)
        ratingBundle.putString(PTConstants.PT_ID, "pt_rating")

        val templateRendererLocal = TemplateRenderer(context, ratingBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.RATING,
                ratingBundle,
                false,
                any(),
                any()
            )
        } returns mockRatingTemplateData
        every { ValidatorFactory.getValidator(mockRatingTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            ratingBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_five_icons_template_invalid() {
        val fiveIconsBundle = Bundle(testBundle)
        fiveIconsBundle.putString(PTConstants.PT_ID, "pt_five_icons")

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.FIVE_ICONS,
                fiveIconsBundle,
                false,
                any(),
                any()
            )
        } returns mockFiveIconsTemplateData
        every { ValidatorFactory.getValidator(mockFiveIconsTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            fiveIconsBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_product_display_template_invalid() {
        val productBundle = Bundle(testBundle)
        productBundle.putString(PTConstants.PT_ID, "pt_product_display")

        val templateRendererLocal = TemplateRenderer(context, productBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.PRODUCT_DISPLAY,
                productBundle,
                false,
                any(),
                any()
            )
        } returns mockProductTemplateData
        every { ValidatorFactory.getValidator(mockProductTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            productBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_zero_bezel_template_invalid() {
        val zeroBezelBundle = Bundle(testBundle)
        zeroBezelBundle.putString(PTConstants.PT_ID, "pt_zero_bezel")

        val templateRendererLocal = TemplateRenderer(context, zeroBezelBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.ZERO_BEZEL,
                zeroBezelBundle,
                false,
                any(),
                any()
            )
        } returns mockZeroBezelTemplateData
        every { ValidatorFactory.getValidator(mockZeroBezelTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            zeroBezelBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_timer_template_invalid() {
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { ValidatorFactory.getValidator(mockTimerTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_input_box_template_invalid() {
        val inputBoxBundle = Bundle(testBundle)
        inputBoxBundle.putString(PTConstants.PT_ID, "pt_input")

        val templateRendererLocal = TemplateRenderer(context, inputBoxBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.INPUT_BOX,
                inputBoxBundle,
                false,
                any(),
                any()
            )
        } returns mockInputBoxTemplateData
        every { ValidatorFactory.getValidator(mockInputBoxTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRendererLocal.renderNotification(
            inputBoxBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_basic_template_null_validator() {
        val basicBundle = Bundle(testBundle)
        basicBundle.putString(PTConstants.PT_ID, "pt_basic")

        val templateRendererLocal = TemplateRenderer(context, basicBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.BASIC,
                basicBundle,
                false,
                any(),
                any()
            )
        } returns mockBasicTemplateData
        every { ValidatorFactory.getValidator(mockBasicTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            basicBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_auto_carousel_template_null_validator() {
        val autoCarouselBundle = Bundle(testBundle)
        autoCarouselBundle.putString(PTConstants.PT_ID, "pt_carousel")

        val templateRendererLocal = TemplateRenderer(context, autoCarouselBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.AUTO_CAROUSEL,
                autoCarouselBundle,
                false,
                any(),
                any()
            )
        } returns mockAutoCarouselTemplateData
        every { ValidatorFactory.getValidator(mockAutoCarouselTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            autoCarouselBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_manual_carousel_template_null_validator() {
        val manualCarouselBundle = Bundle(testBundle)
        manualCarouselBundle.putString(PTConstants.PT_ID, "pt_manual_carousel")

        val templateRendererLocal = TemplateRenderer(context, manualCarouselBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.MANUAL_CAROUSEL,
                manualCarouselBundle,
                false,
                any(),
                any()
            )
        } returns mockManualCarouselTemplateData
        every { ValidatorFactory.getValidator(mockManualCarouselTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            manualCarouselBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_rating_template_null_validator() {
        val ratingBundle = Bundle(testBundle)
        ratingBundle.putString(PTConstants.PT_ID, "pt_rating")

        val templateRendererLocal = TemplateRenderer(context, ratingBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.RATING,
                ratingBundle,
                false,
                any(),
                any()
            )
        } returns mockRatingTemplateData
        every { ValidatorFactory.getValidator(mockRatingTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            ratingBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_five_icons_template_null_validator() {
        val fiveIconsBundle = Bundle(testBundle)
        fiveIconsBundle.putString(PTConstants.PT_ID, "pt_five_icons")

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.FIVE_ICONS,
                fiveIconsBundle,
                false,
                any(),
                any()
            )
        } returns mockFiveIconsTemplateData
        every { ValidatorFactory.getValidator(mockFiveIconsTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            fiveIconsBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_product_display_template_null_validator() {
        val productBundle = Bundle(testBundle)
        productBundle.putString(PTConstants.PT_ID, "pt_product_display")

        val templateRendererLocal = TemplateRenderer(context, productBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.PRODUCT_DISPLAY,
                productBundle,
                false,
                any(),
                any()
            )
        } returns mockProductTemplateData
        every { ValidatorFactory.getValidator(mockProductTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            productBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_zero_bezel_template_null_validator() {
        val zeroBezelBundle = Bundle(testBundle)
        zeroBezelBundle.putString(PTConstants.PT_ID, "pt_zero_bezel")

        val templateRendererLocal = TemplateRenderer(context, zeroBezelBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.ZERO_BEZEL,
                zeroBezelBundle,
                false,
                any(),
                any()
            )
        } returns mockZeroBezelTemplateData
        every { ValidatorFactory.getValidator(mockZeroBezelTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            zeroBezelBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_timer_template_null_validator() {
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { ValidatorFactory.getValidator(mockTimerTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_input_box_template_null_validator() {
        val inputBoxBundle = Bundle(testBundle)
        inputBoxBundle.putString(PTConstants.PT_ID, "pt_input")

        val templateRendererLocal = TemplateRenderer(context, inputBoxBundle, mockConfig)

        // Arrange
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.INPUT_BOX,
                inputBoxBundle,
                false,
                any(),
                any()
            )
        } returns mockInputBoxTemplateData
        every { ValidatorFactory.getValidator(mockInputBoxTemplateData) } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            inputBoxBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_auto_carousel_template_valid() {
        // Arrange
        val carouselBundle = Bundle(testBundle)
        carouselBundle.putString(PTConstants.PT_ID, "pt_carousel")

        val templateRendererLocal = TemplateRenderer(context, carouselBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.AUTO_CAROUSEL,
                carouselBundle,
                false,
                any(),
                any()
            )
        } returns mockAutoCarouselTemplateData
        every { ValidatorFactory.getValidator(mockAutoCarouselTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(AutoCarouselStyle::class)
        every {
            anyConstructed<AutoCarouselStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            carouselBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<AutoCarouselStyle>().builderFromStyle(
                any(),
                carouselBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun test_renderNotification_rating_template_valid() {
        // Arrange
        val ratingBundle = Bundle(testBundle)
        ratingBundle.putString(PTConstants.PT_ID, "pt_rating")

        val templateRendererLocal = TemplateRenderer(context, ratingBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.RATING,
                ratingBundle,
                false,
                any(),
                any()
            )
        } returns mockRatingTemplateData
        every { ValidatorFactory.getValidator(mockRatingTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(RatingStyle::class)
        every {
            anyConstructed<RatingStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            ratingBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<RatingStyle>().builderFromStyle(
                any(),
                ratingBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }


    @Test
    fun test_renderNotification_manual_carousel_template_valid() {
        // Arrange
        val carouselBundle = Bundle(testBundle)
        carouselBundle.putString(PTConstants.PT_ID, "pt_manual_carousel")

        val templateRendererLocal = TemplateRenderer(context, carouselBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.MANUAL_CAROUSEL,
                carouselBundle,
                false,
                any(),
                any()
            )
        } returns mockManualCarouselTemplateData
        every { ValidatorFactory.getValidator(mockManualCarouselTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(ManualCarouselStyle::class)
        every {
            anyConstructed<ManualCarouselStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            carouselBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<ManualCarouselStyle>().builderFromStyle(
                any(),
                carouselBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }


    @Test
    fun test_renderNotification_five_icons_template_valid() {
        // Arrange
        val fiveIconsBundle = Bundle(testBundle)
        fiveIconsBundle.putString(PTConstants.PT_ID, "pt_five_icons")

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.FIVE_ICONS,
                fiveIconsBundle,
                false,
                any(),
                any()
            )
        } returns mockFiveIconsTemplateData
        every { ValidatorFactory.getValidator(mockFiveIconsTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        val mockSmallContentView = mockk<FiveIconSmallContentView>()
        val mockBigContentView = mockk<FiveIconBigContentView>()
        every { mockSmallContentView.getUnloadedFiveIconsCount() } returns 1
        every { mockBigContentView.getUnloadedFiveIconsCount() } returns 1

        mockkConstructor(FiveIconStyle::class)
        every {
            anyConstructed<FiveIconStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder
        every { anyConstructed<FiveIconStyle>().fiveIconSmallContentView } returns mockSmallContentView
        every { anyConstructed<FiveIconStyle>().fiveIconBigContentView } returns mockBigContentView

        // Act
        val result = templateRendererLocal.renderNotification(
            fiveIconsBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<FiveIconStyle>().builderFromStyle(
                any(),
                fiveIconsBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }


    @Test
    fun test_renderNotification_five_icons_small_unloaded_count_3() {
        // Arrange
        val fiveIconsBundle = Bundle(testBundle)
        fiveIconsBundle.putString(PTConstants.PT_ID, "pt_five_icons")

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.FIVE_ICONS,
                fiveIconsBundle,
                false,
                any(),
                any()
            )
        } returns mockFiveIconsTemplateData
        every { ValidatorFactory.getValidator(mockFiveIconsTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        val mockSmallContentView = mockk<FiveIconSmallContentView>()
        val mockBigContentView = mockk<FiveIconBigContentView>()
        every { mockSmallContentView.getUnloadedFiveIconsCount() } returns 3
        every { mockBigContentView.getUnloadedFiveIconsCount() } returns 1

        mockkConstructor(FiveIconStyle::class)
        every {
            anyConstructed<FiveIconStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder
        every { anyConstructed<FiveIconStyle>().fiveIconSmallContentView } returns mockSmallContentView
        every { anyConstructed<FiveIconStyle>().fiveIconBigContentView } returns mockBigContentView

        // Act
        val result = templateRendererLocal.renderNotification(
            fiveIconsBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }


    @Test
    fun test_renderNotification_five_icons_big_unloaded_count_3() {
        // Arrange
        val fiveIconsBundle = Bundle(testBundle)
        fiveIconsBundle.putString(PTConstants.PT_ID, "pt_five_icons")

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.FIVE_ICONS,
                fiveIconsBundle,
                false,
                any(),
                any()
            )
        } returns mockFiveIconsTemplateData
        every { ValidatorFactory.getValidator(mockFiveIconsTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        val mockSmallContentView = mockk<FiveIconSmallContentView>()
        val mockBigContentView = mockk<FiveIconBigContentView>()
        every { mockSmallContentView.getUnloadedFiveIconsCount() } returns 1
        every { mockBigContentView.getUnloadedFiveIconsCount() } returns 3

        mockkConstructor(FiveIconStyle::class)
        every {
            anyConstructed<FiveIconStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder
        every { anyConstructed<FiveIconStyle>().fiveIconSmallContentView } returns mockSmallContentView
        every { anyConstructed<FiveIconStyle>().fiveIconBigContentView } returns mockBigContentView

        // Act
        val result = templateRendererLocal.renderNotification(
            fiveIconsBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    fun test_renderNotification_five_icons_template_invalid_unloaded_icons() {
        // Arrange
        val fiveIconsBundle = Bundle(testBundle)
        fiveIconsBundle.putString(PTConstants.PT_ID, "pt_five_icons")

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.FIVE_ICONS,
                fiveIconsBundle,
                false,
                any(),
                any()
            )
        } returns mockFiveIconsTemplateData
        every { ValidatorFactory.getValidator(mockFiveIconsTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        val mockSmallContentView = mockk<FiveIconSmallContentView>()
        val mockBigContentView = mockk<FiveIconBigContentView>()
        every { mockSmallContentView.getUnloadedFiveIconsCount() } returns 3 // More than 2
        every { mockBigContentView.getUnloadedFiveIconsCount() } returns 1

        mockkConstructor(FiveIconStyle::class)
        every {
            anyConstructed<FiveIconStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder
        every { anyConstructed<FiveIconStyle>().fiveIconSmallContentView } returns mockSmallContentView
        every { anyConstructed<FiveIconStyle>().fiveIconBigContentView } returns mockBigContentView

        // Act
        val result = templateRendererLocal.renderNotification(
            fiveIconsBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<FiveIconStyle>().builderFromStyle(
                any(),
                fiveIconsBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertNull(result) // Should return null when too many unloaded icons
    }

    @Test
    fun test_renderNotification_product_display_template_valid() {
        // Arrange
        val productBundle = Bundle(testBundle)
        productBundle.putString(PTConstants.PT_ID, "pt_product_display")

        val templateRendererLocal = TemplateRenderer(context, productBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.PRODUCT_DISPLAY,
                productBundle,
                false,
                any(),
                any()
            )
        } returns mockProductTemplateData
        every { ValidatorFactory.getValidator(mockProductTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(ProductDisplayStyle::class)
        every {
            anyConstructed<ProductDisplayStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            productBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<ProductDisplayStyle>().builderFromStyle(
                any(),
                productBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun test_renderNotification_zero_bezel_template_valid() {
        // Arrange
        val zeroBezelBundle = Bundle(testBundle)
        zeroBezelBundle.putString(PTConstants.PT_ID, "pt_zero_bezel")

        val templateRendererLocal = TemplateRenderer(context, zeroBezelBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.ZERO_BEZEL,
                zeroBezelBundle,
                false,
                any(),
                any()
            )
        } returns mockZeroBezelTemplateData
        every { ValidatorFactory.getValidator(mockZeroBezelTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(ZeroBezelStyle::class)
        every {
            anyConstructed<ZeroBezelStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            zeroBezelBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<ZeroBezelStyle>().builderFromStyle(
                any(),
                zeroBezelBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun test_renderNotification_timer_template_valid() {
        // Arrange
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")
        timerBundle.putString(PTConstants.PT_TIMER_END, "10")

        val mockBaseContent = mockk<BaseContent>()
        val mockNotificationBehavior = mockk<NotificationBehavior>()

        every { mockTimerTemplateData.baseContent } returns mockBaseContent
        every { mockBaseContent.notificationBehavior } returns mockNotificationBehavior
        every { mockNotificationBehavior.dismissAfter } returns 10000 // 10 seconds in milliseconds
        every { mockTimerTemplateData.renderTerminal } returns true

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { ValidatorFactory.getValidator(mockTimerTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(TimerStyle::class)
        mockkObject(TimerTemplateHandler)
        every {
            anyConstructed<TimerStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<TimerStyle>().builderFromStyle(
                any(),
                timerBundle,
                123,
                mockNotificationBuilder
            )
        }
        verify { TimerTemplateHandler.scheduleTimer(context, timerBundle, 123, 10000, mockTimerTemplateData, mockConfig, any()) }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun test_renderNotification_timer_template_valid_render_terminal_false() {
        // Arrange
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")

        val mockBaseContent = mockk<BaseContent>()
        val mockNotificationBehavior = mockk<NotificationBehavior>()

        every { mockTimerTemplateData.baseContent } returns mockBaseContent
        every { mockBaseContent.notificationBehavior } returns mockNotificationBehavior
        every { mockNotificationBehavior.dismissAfter } returns 15000 // 10 seconds in milliseconds
        every { mockTimerTemplateData.renderTerminal } returns false

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { ValidatorFactory.getValidator(mockTimerTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(TimerStyle::class)
        mockkObject(TimerTemplateHandler)
        every {
            anyConstructed<TimerStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<TimerStyle>().builderFromStyle(
                any(),
                timerBundle,
                123,
                mockNotificationBuilder
            )
        }
        verify(exactly = 0) { TimerTemplateHandler.scheduleTimer(any(), any(), any(), any(), any(), any()) }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun test_renderNotification_timerEnd_null() {
        // Arrange
        val timerBundle = Bundle(testBundle)

        val mockBaseContent = mockk<BaseContent>()
        val mockNotificationBehavior = mockk<NotificationBehavior>()

        every { mockTimerTemplateData.baseContent } returns mockBaseContent
        every { mockBaseContent.notificationBehavior } returns mockNotificationBehavior
        every { mockNotificationBehavior.dismissAfter } returns null

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { ValidatorFactory.getValidator(mockTimerTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(TimerStyle::class)
        every {
            anyConstructed<TimerStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        assertNull(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun test_renderNotification_timer_template_below_oreo() {
        // Arrange
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { mockTimerTemplateData.toBasicTemplateData() } returns mockBasicTemplateData
        every { ValidatorFactory.getValidator(any()) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(BasicStyle::class)
        every {
            anyConstructed<BasicStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<BasicStyle>().builderFromStyle(
                any(),
                timerBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun test_renderNotification_timer_template_below_oreo_null_validator() {
        // Arrange
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { mockTimerTemplateData.toBasicTemplateData() } returns mockBasicTemplateData
        every { ValidatorFactory.getValidator(mockTimerTemplateData) } returns null

        mockkConstructor(BasicStyle::class)
        every {
            anyConstructed<BasicStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun test_renderNotification_timer_template_below_oreo_false_validator() {
        // Arrange
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")

        val templateRendererLocal = TemplateRenderer(context, timerBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.TIMER,
                timerBundle,
                false,
                any(),
                any()
            )
        } returns mockTimerTemplateData
        every { mockTimerTemplateData.toBasicTemplateData() } returns mockBasicTemplateData
        every { ValidatorFactory.getValidator(mockTimerTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        mockkConstructor(BasicStyle::class)
        every {
            anyConstructed<BasicStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            timerBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_input_box_template_valid() {
        // Arrange
        val inputBoxBundle = Bundle(testBundle)
        inputBoxBundle.putString(PTConstants.PT_ID, "pt_input")

        val templateRendererLocal = TemplateRenderer(context, inputBoxBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.INPUT_BOX,
                inputBoxBundle,
                false,
                any(),
                any()
            )
        } returns mockInputBoxTemplateData
        every { ValidatorFactory.getValidator(mockInputBoxTemplateData) } returns mockContentValidator
        every { mockContentValidator.validate() } returns true

        mockkConstructor(InputBoxStyle::class)
        every {
            anyConstructed<InputBoxStyle>().builderFromStyle(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRendererLocal.renderNotification(
            inputBoxBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<InputBoxStyle>().builderFromStyle(
                any(),
                inputBoxBundle,
                123,
                mockNotificationBuilder
            )
        }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun test_renderNotification_unknown_template_type() {
        // Arrange
        val unknownBundle = Bundle(testBundle)
        unknownBundle.putString(PTConstants.PT_ID, "pt_unknown")

        val templateRendererLocal = TemplateRenderer(context, unknownBundle, mockConfig)

        every {
            TemplateDataFactory.createTemplateData(
                any(),
                unknownBundle,
                false,
                any(),
                any()
            )
        } returns null

        // Act
        val result = templateRendererLocal.renderNotification(
            unknownBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        // Should log "operation not defined!" and return null
        assertNull(result)
    }

    @Test
    fun test_renderNotification_with_null_template_id() {
        // Arrange
        val nullIdBundle = Bundle()
        nullIdBundle.putString(PTConstants.PT_TITLE, "Test Title")
        nullIdBundle.putString(PTConstants.PT_MSG, "Test Message")
        // Note: We're not adding PT_ID on purpose

        // Act
        val result = templateRenderer.renderNotification(
            nullIdBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_getActionButtons_with_valid_actions() {
        // Arrange
        val actionsJson = JSONArray(
            """
            [
                {
                    "l": "Action 1",
                    "id": "action1",
                    "dl": "https://example.com",
                    "ac": true
                },
                {
                    "l": "Action 2",
                    "id": "action2",
                    "dl": "https://example2.com",
                    "ac": false
                }
            ]
        """
        )

        // Mock the necessary methods with relaxed settings
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        every { Utils.setPackageNameFromResolveInfoList(any(), any()) } just Runs

        // Act
        val result = templateRenderer.getActionButtons(context, testBundle, 123, actionsJson)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Action 1", result[0].label)
        assertEquals("Action 2", result[1].label)
    }

    @Test
    fun test_setActionButtons_with_empty_action_buttons_list() {
        // Arrange - Use an empty actionButtons list
        templateRenderer.actionButtons = emptyList()
        templateRenderer.actionButtonPendingIntents = mutableMapOf()

        // Act
        val result = templateRenderer.setActionButtons(
            context,
            testBundle,
            123,
            mockNotificationBuilder,
            null
        )

        // Assert
        assertEquals(mockNotificationBuilder, result)
        verify(exactly = 0) { mockNotificationBuilder.addAction(any(), any(), any()) }
    }

    @Test
    fun test_setActionButtons_with_valid_action_buttons_and_pending_intents() {
        // Arrange
        val mockPendingIntent1 = mockk<android.app.PendingIntent>()
        val mockPendingIntent2 = mockk<android.app.PendingIntent>()

        val actionButton1 = ActionButton("action1", "Action 1", 123)
        val actionButton2 = ActionButton("action2", "Action 2", 456)

        templateRenderer.actionButtons = listOf(actionButton1, actionButton2)
        templateRenderer.actionButtonPendingIntents = mutableMapOf(
            "action1" to mockPendingIntent1,
            "action2" to mockPendingIntent2
        )

        every {
            mockNotificationBuilder.addAction(
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRenderer.setActionButtons(
            context,
            testBundle,
            123,
            mockNotificationBuilder,
            null
        )

        // Assert
        assertEquals(mockNotificationBuilder, result)
        verify { mockNotificationBuilder.addAction(123, "Action 1", mockPendingIntent1) }
        verify { mockNotificationBuilder.addAction(456, "Action 2", mockPendingIntent2) }
        verify(exactly = 2) { mockNotificationBuilder.addAction(any(), any(), any()) }
    }

    @Test
    fun test_setActionButtons_with_action_buttons_but_no_pending_intents() {
        // Arrange
        val actionButton1 = ActionButton("action1", "Action 1", 123)
        val actionButton2 = ActionButton("action2", "Action 2", 456)

        templateRenderer.actionButtons = listOf(actionButton1, actionButton2)
        templateRenderer.actionButtonPendingIntents = mutableMapOf() // Empty pending intents

        // Act
        val result = templateRenderer.setActionButtons(
            context,
            testBundle,
            123,
            mockNotificationBuilder,
            null
        )

        // Assert
        assertEquals(mockNotificationBuilder, result)
        verify(exactly = 0) { mockNotificationBuilder.addAction(any(), any(), any()) }
    }

    @Test
    fun test_setActionButtons_with_partial_pending_intents() {
        // Arrange
        val mockPendingIntent1 = mockk<android.app.PendingIntent>()

        val actionButton1 = ActionButton("action1", "Action 1", 123)
        val actionButton2 = ActionButton("action2", "Action 2", 456)
        val actionButton3 = ActionButton("action3", "Action 3", 789)

        templateRenderer.actionButtons = listOf(actionButton1, actionButton2, actionButton3)
        templateRenderer.actionButtonPendingIntents = mutableMapOf(
            "action1" to mockPendingIntent1,
            // Note: action2 and action3 don't have pending intents
        )

        every {
            mockNotificationBuilder.addAction(
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRenderer.setActionButtons(
            context,
            testBundle,
            123,
            mockNotificationBuilder,
            null
        )

        // Assert
        assertEquals(mockNotificationBuilder, result)
        verify { mockNotificationBuilder.addAction(123, "Action 1", mockPendingIntent1) }
        verify(exactly = 1) { mockNotificationBuilder.addAction(any(), any(), any()) }
    }

    @Test
    fun test_setActionButtons_with_null_context() {
        // Arrange
        val mockPendingIntent1 = mockk<android.app.PendingIntent>()
        val actionButton1 = ActionButton("action1", "Action 1", 123)

        templateRenderer.actionButtons = listOf(actionButton1)
        templateRenderer.actionButtonPendingIntents = mutableMapOf("action1" to mockPendingIntent1)

        every {
            mockNotificationBuilder.addAction(
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRenderer.setActionButtons(
            null, // null context
            testBundle,
            123,
            mockNotificationBuilder,
            null
        )

        // Assert
        assertEquals(mockNotificationBuilder, result)
        verify { mockNotificationBuilder.addAction(123, "Action 1", mockPendingIntent1) }
    }

    @Test
    fun test_setActionButtons_with_null_extras() {
        // Arrange
        val mockPendingIntent1 = mockk<android.app.PendingIntent>()
        val actionButton1 = ActionButton("action1", "Action 1", 123)

        templateRenderer.actionButtons = listOf(actionButton1)
        templateRenderer.actionButtonPendingIntents = mutableMapOf("action1" to mockPendingIntent1)

        every {
            mockNotificationBuilder.addAction(
                any(),
                any(),
                any()
            )
        } returns mockNotificationBuilder

        // Act
        val result = templateRenderer.setActionButtons(
            context,
            null, // null extras
            123,
            mockNotificationBuilder,
            null
        )

        // Assert
        assertEquals(mockNotificationBuilder, result)
        verify { mockNotificationBuilder.addAction(123, "Action 1", mockPendingIntent1) }
    }

    @Test
    fun test_getActionButtons_with_missing_required_fields() {
        // Arrange
        val actionsJson = JSONArray(
            """
            [
                {
                    "l": "",
                    "id": "action1"
                },
                {
                    "l": "Action 2",
                    "id": ""
                }
            ]
        """
        )

        // Act
        val result = templateRenderer.getActionButtons(context, testBundle, 123, actionsJson)

        // Assert
        assertTrue(result.isEmpty())
    }


    @Test
    fun test_renderNotification_cancel_template_valid() {
        val cancelBundle = Bundle()
        cancelBundle.putString(PTConstants.PT_ID, "pt_cancel")

        val templateRendererLocal = TemplateRenderer(context, cancelBundle, mockConfig)

        // Arrange
        mockkObject(CancelTemplateHandler)
        every {
            TemplateDataFactory.createTemplateData(
                TemplateType.CANCEL,
                cancelBundle,
                false,
                any(),
                any()
            )
        } returns mockCancelTemplateData


        // Act
        val result = templateRendererLocal.renderNotification(
            cancelBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        verify { CancelTemplateHandler.renderCancelNotification(context, mockCancelTemplateData) }
        assertNull(result)
    }
}