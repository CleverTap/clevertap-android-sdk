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
import com.clevertap.android.pushtemplates.content.FiveIconBigContentView
import com.clevertap.android.pushtemplates.content.FiveIconSmallContentView
import com.clevertap.android.pushtemplates.styles.*
import com.clevertap.android.pushtemplates.validators.ContentValidator
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapInstanceConfig
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
import java.lang.reflect.Field

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
        mockkObject(ValidatorFactory.Companion)
        mockkStatic(Utils::class)
        mockkStatic(ManifestInfo::class)

        // Default bundle setup
        testBundle.putString(PTConstants.PT_ID, "pt_carousel")
        testBundle.putString(PTConstants.PT_TITLE, "Test Title")
        testBundle.putString(PTConstants.PT_MSG, "Test Message")

        // Setup common mocks
        every { ManifestInfo.getInstance(any()) } returns mockManifestInfo
        every { mockManifestInfo.intentServiceName } returns "com.clevertap.android.sdk.pushnotification.CTNotificationIntentService"

        // We use a mock for NotificationManager as this is simpler to verify
        mockNotificationManager = mockk(relaxed = true)

        // Create TemplateRenderer instance with the mock context
        templateRenderer = TemplateRenderer(context, testBundle)
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
    fun test_getCollapseKey_returnsCorrectValue() {
        // Arrange
        val expectedCollapseKey = "test_collapse_key"
        testBundle.putString(PTConstants.PT_COLLAPSE_KEY, expectedCollapseKey)

        // Use reflection to set the private field
        val field: Field = TemplateRenderer::class.java.getDeclaredField("pt_collapse_key")
        field.isAccessible = true
        field.set(templateRenderer, expectedCollapseKey)

        // Act
        val collapseKey = templateRenderer.getCollapseKey(testBundle)

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
    fun test_renderNotification_basic_template_valid() {
        val basicBundle = Bundle(testBundle)
        basicBundle.putString(PTConstants.PT_ID, "pt_basic")

        val templateRendererLocal = TemplateRenderer(context, basicBundle)

        // Arrange
        every {
            ValidatorFactory.getValidator(
                TemplateType.BASIC,
                any()
            )
        } returns mockContentValidator
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
        // Arrange
        every {
            ValidatorFactory.getValidator(
                TemplateType.BASIC,
                any()
            )
        } returns mockContentValidator
        every { mockContentValidator.validate() } returns false

        // Act
        val result = templateRenderer.renderNotification(
            testBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_renderNotification_auto_carousel_template_valid() {
        // Arrange
        val carouselBundle = Bundle(testBundle)
        carouselBundle.putString(PTConstants.PT_ID, "pt_carousel")

        val templateRendererLocal = TemplateRenderer(context, testBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.AUTO_CAROUSEL,
                any()
            )
        } returns mockContentValidator
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
            testBundle,
            context,
            mockNotificationBuilder,
            mockConfig,
            123
        )

        // Assert
        verify {
            anyConstructed<AutoCarouselStyle>().builderFromStyle(
                any(),
                testBundle,
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

        val templateRendererLocal = TemplateRenderer(context, ratingBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.RATING,
                any()
            )
        } returns mockContentValidator
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

        val templateRendererLocal = TemplateRenderer(context, carouselBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.MANUAL_CAROUSEL,
                any()
            )
        } returns mockContentValidator
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

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.FIVE_ICONS,
                any()
            )
        } returns mockContentValidator
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
    fun test_renderNotification_five_icons_template_invalid_unloaded_icons() {
        // Arrange
        val fiveIconsBundle = Bundle(testBundle)
        fiveIconsBundle.putString(PTConstants.PT_ID, "pt_five_icons")

        val templateRendererLocal = TemplateRenderer(context, fiveIconsBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.FIVE_ICONS,
                any()
            )
        } returns mockContentValidator
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

        val templateRendererLocal = TemplateRenderer(context, productBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.PRODUCT_DISPLAY,
                any()
            )
        } returns mockContentValidator
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

        val templateRendererLocal = TemplateRenderer(context, zeroBezelBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.ZERO_BEZEL,
                any()
            )
        } returns mockContentValidator
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

        every { Utils.getTimerEnd(timerBundle) } returns 10

        val templateRendererLocal = TemplateRenderer(context, timerBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.TIMER,
                any()
            )
        } returns mockContentValidator
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
        every { mockNotificationBuilder.setTimeoutAfter(any()) } returns mockNotificationBuilder

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
        verify { mockNotificationBuilder.setTimeoutAfter(11000) }
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N]) // Below Nougat
    fun test_renderNotification_timer_template_below_oreo() {
        // Arrange
        val timerBundle = Bundle(testBundle)
        timerBundle.putString(PTConstants.PT_ID, "pt_timer")

        val templateRendererLocal = TemplateRenderer(context, timerBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.TIMER,
                any()
            )
        } returns mockContentValidator
        every {
            ValidatorFactory.getValidator(
                TemplateType.BASIC,
                any()
            )
        } returns mockContentValidator
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
    fun test_renderNotification_input_box_template_valid() {
        // Arrange
        val inputBoxBundle = Bundle(testBundle)
        inputBoxBundle.putString(PTConstants.PT_ID, "pt_input")

        val templateRendererLocal = TemplateRenderer(context, inputBoxBundle)

        every {
            ValidatorFactory.getValidator(
                TemplateType.INPUT_BOX,
                any()
            )
        } returns mockContentValidator
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

        val templateRendererLocal = TemplateRenderer(context, unknownBundle)

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
}


