package com.clevertap.android.pushtemplates.handlers

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import com.clevertap.android.pushtemplates.BaseColorData
import com.clevertap.android.pushtemplates.BaseContent
import com.clevertap.android.pushtemplates.BaseTextData
import com.clevertap.android.pushtemplates.BasicTemplateData
import com.clevertap.android.pushtemplates.GifData
import com.clevertap.android.pushtemplates.IconData
import com.clevertap.android.pushtemplates.ImageData
import com.clevertap.android.pushtemplates.MediaData
import com.clevertap.android.pushtemplates.NotificationBehavior
import com.clevertap.android.pushtemplates.PTConstants.ONE_SECOND_LONG
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG_ALT_TEXT
import com.clevertap.android.pushtemplates.PTConstants.PT_COLLAPSE_KEY
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF
import com.clevertap.android.pushtemplates.PTConstants.PT_ID
import com.clevertap.android.pushtemplates.PTConstants.PT_JSON
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG_SUMMARY
import com.clevertap.android.pushtemplates.PTConstants.PT_TIMER_MIN_THRESHOLD
import com.clevertap.android.pushtemplates.PTConstants.PT_TITLE
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TimerTemplateData
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import io.mockk.*
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
class TimerTemplateHandlerTest {

    private lateinit var mockContext: Context
    private lateinit var mockApplicationContext: Context
    private lateinit var mockBundle: Bundle
    private lateinit var mockConfig: CleverTapInstanceConfig
    private lateinit var mockHandler: Handler
    private lateinit var mockCleverTapAPI: CleverTapAPI
    private lateinit var timerTemplateData: TimerTemplateData

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val DELAY = 5000L
        private const val ACCOUNT_ID = "test-account-id"
    }

    @Before
    fun setUp() {
        mockContext = mockk<Context>(relaxed = true)
        mockApplicationContext = mockk<Context>(relaxed = true)
        mockBundle = mockk<Bundle>(relaxed = true)
        mockConfig = mockk<CleverTapInstanceConfig>(relaxed = true)
        mockHandler = mockk<Handler>(relaxed = true)
        mockCleverTapAPI = mockk<CleverTapAPI>(relaxed = true)

        // Setup context mocks
        every { mockContext.applicationContext } returns mockApplicationContext

        // Create sample TimerTemplateData
        timerTemplateData = createSampleTimerTemplateData()

        // Mock static methods
        mockkStatic(Utils::class)
        mockkObject(ValidatorFactory)
        mockkStatic(PushNotificationUtil::class)
        mockkStatic(CleverTapAPI::class)
        mockkConstructor(TemplateRenderer::class)

        // Default mock behaviors
        every { Utils.isNotificationInTray(any(), any()) } returns true
        every { ValidatorFactory.getValidator(any<BasicTemplateData>())?.validate() } returns true
        every { PushNotificationUtil.getAccountIdFromNotificationBundle(any()) } returns ACCOUNT_ID
        every { CleverTapAPI.getGlobalInstance(any(), any()) } returns mockCleverTapAPI
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `scheduleTimer should not execute when delay is null`() {
        // Given
        val nullDelay: Long? = null

        // When
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            mockBundle,
            NOTIFICATION_ID,
            nullDelay,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        // Then
        verify(exactly = 0) { mockHandler.postDelayed(any(), any()) }
    }

    @Test
    fun `scheduleTimer should post delayed execution when delay is provided`() {
        // Given
        val slot = slot<Runnable>()
        every { mockHandler.postDelayed(capture(slot), any()) } returns true

        // When
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            mockBundle,
            NOTIFICATION_ID,
            DELAY,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        // Then
        verify { mockHandler.postDelayed(any(), (DELAY - 100)) }
        assertNotNull(slot.captured)
    }

    @Test
    fun `scheduleTimer should not render notification when notification is not in tray`() {
        // Given
        every { Utils.isNotificationInTray(any(), any()) } returns false
        val slot = slot<Runnable>()
        every { mockHandler.postDelayed(capture(slot), any()) } returns true

        // When
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            mockBundle,
            NOTIFICATION_ID,
            DELAY,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        // Execute the delayed runnable
        slot.captured.run()

        // Then
        verify(exactly = 0) { mockCleverTapAPI.renderPushNotification(any(), any(), any()) }
    }

    @Test
    fun `scheduleTimer should not render notification when validator returns false`() {
        // Given
        every { ValidatorFactory.getValidator(any())?.validate() } returns false
        val slot = slot<Runnable>()
        every { mockHandler.postDelayed(capture(slot), any()) } returns true

        // When
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            mockBundle,
            NOTIFICATION_ID,
            DELAY,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        // Execute the delayed runnable
        slot.captured.run()

        // Then
        verify(exactly = 0) { mockCleverTapAPI.renderPushNotification(any(), any(), any()) }
    }

    @Test
    fun `scheduleTimer should not render notification when validator returns null`() {
        // Given
        every { ValidatorFactory.getValidator(any()) } returns null
        val slot = slot<Runnable>()
        every { mockHandler.postDelayed(capture(slot), any()) } returns true

        // When
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            mockBundle,
            NOTIFICATION_ID,
            DELAY,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        // Execute the delayed runnable
        slot.captured.run()

        // Then
        verify(exactly = 0) { mockCleverTapAPI.renderPushNotification(any(), any(), any()) }
    }

    @Test
    fun `scheduleTimer should update JSON with terminal data`() {
        // Arrange
        val slot = slot<Runnable>()
        every { mockHandler.postDelayed(capture(slot), any()) } returns true

        // Act
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            createSampleTimerBundle(),
            NOTIFICATION_ID,
            DELAY,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        slot.captured.run()

        // Assert
        verify {
            mockCleverTapAPI.renderPushNotification(
                any(), any(), withArg { finalBundle ->

                    val jsonStr = finalBundle.getString(PT_JSON)
                    val jsonObj = JSONObject(jsonStr!!)

                    // Verify terminal text data is applied
                    assertEquals("Terminal Title", jsonObj.getString(PT_TITLE))
                    assertEquals("Terminal Message", jsonObj.getString(PT_MSG))
                    assertEquals("Terminal Summary", jsonObj.getString(PT_MSG_SUMMARY))

                    // Verify terminal media data is applied
                    assertEquals("terminal_image_url", jsonObj.getString(PT_BIG_IMG))
                    assertEquals("Terminal Image alt text", jsonObj.getString(PT_BIG_IMG_ALT_TEXT))
                    assertEquals("terminal_gif_url", jsonObj.getString(PT_GIF))
                })
        }
    }

    @Test
    fun `scheduleTimer should render notification when all conditions are met`() {
        // Given
        every { mockBundle.clone() } returns mockBundle
        every { mockBundle.getString(PT_JSON) } returns JSONObject().toString()

        val slot = slot<Runnable>()
        every { mockHandler.postDelayed(capture(slot), any()) } returns true

        // When
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            mockBundle,
            NOTIFICATION_ID,
            DELAY,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        // Execute the delayed runnable
        slot.captured.run()

        // Then
        verify {
            mockCleverTapAPI.renderPushNotification(
                any(), mockApplicationContext, mockBundle
            )
        }
    }

    @Test
    fun `scheduleTimer should render notification when all conditions are met_but_invalid_pt_json`() {
        // Given
        every { mockBundle.clone() } returns mockBundle
        every { mockBundle.getString(PT_JSON) } returns "invalid"

        val slot = slot<Runnable>()
        every { mockHandler.postDelayed(capture(slot), any()) } returns true

        // When
        TimerTemplateHandler.scheduleTimer(
            mockContext,
            mockBundle,
            NOTIFICATION_ID,
            DELAY,
            timerTemplateData,
            mockConfig,
            mockHandler
        )

        // Execute the delayed runnable
        slot.captured.run()

        // Then
        verify {
            mockCleverTapAPI.renderPushNotification(
                any(), mockApplicationContext, mockBundle
            )
        }
    }

    @Test
    fun `getDismissAfterMs should return null when both timerThreshold and timerEnd are below minimum`() {
        // When
        val result = TimerTemplateHandler.getDismissAfterMs(timerThreshold = PT_TIMER_MIN_THRESHOLD - 1, timerEnd = PT_TIMER_MIN_THRESHOLD - 1)

        // Then
        assertNull(result)
    }

    @Test
    fun `getDismissAfterMs should return timerThreshold plus one second when threshold is valid and not -1`() {
        // Given
        val validThreshold = PT_TIMER_MIN_THRESHOLD + 5

        // When
        val result = TimerTemplateHandler.getDismissAfterMs(
            timerThreshold = validThreshold,
            timerEnd = PT_TIMER_MIN_THRESHOLD - 1
        )

        // Then
        assertEquals(validThreshold * ONE_SECOND_LONG + ONE_SECOND_LONG, result)
    }

    @Test
    fun `getDismissAfterMs should return timerEnd plus one second when threshold is -1 and timerEnd is valid`() {
        // Given
        val validTimerEnd = PT_TIMER_MIN_THRESHOLD + 3

        // When
        val result = TimerTemplateHandler.getDismissAfterMs(timerThreshold = -1, timerEnd = validTimerEnd)

        // Then
        assertEquals(validTimerEnd * ONE_SECOND_LONG + ONE_SECOND_LONG, result)
    }

    @Test
    fun `getDismissAfterMs should return timerEnd plus one second when threshold is below minimum and timerEnd is valid`() {
        // Given
        val validTimerEnd = PT_TIMER_MIN_THRESHOLD + 2

        // When
        val result = TimerTemplateHandler.getDismissAfterMs(timerThreshold = PT_TIMER_MIN_THRESHOLD - 1, timerEnd = validTimerEnd)

        // Then
        assertEquals(validTimerEnd * ONE_SECOND_LONG + ONE_SECOND_LONG, result)
    }

    @Test
    fun `getDismissAfterMs should prioritize timerThreshold over timerEnd when both are valid`() {
        // Given
        val validThreshold = PT_TIMER_MIN_THRESHOLD + 3
        val validTimerEnd = PT_TIMER_MIN_THRESHOLD + 5

        // When
        val result = TimerTemplateHandler.getDismissAfterMs(timerThreshold = validThreshold, timerEnd = validTimerEnd)

        // Then
        assertEquals(validThreshold * ONE_SECOND_LONG + ONE_SECOND_LONG, result)
        assertNotEquals(validTimerEnd * ONE_SECOND_LONG + ONE_SECOND_LONG, result)
    }

    @Test
    fun `getDismissAfterMs should handle edge case when threshold equals minimum`() {
        // When
        val result = TimerTemplateHandler.getDismissAfterMs(timerThreshold = PT_TIMER_MIN_THRESHOLD, timerEnd = PT_TIMER_MIN_THRESHOLD - 1)

        // Then
        assertEquals(PT_TIMER_MIN_THRESHOLD * ONE_SECOND_LONG + ONE_SECOND_LONG, result)
    }

    @Test
    fun `getDismissAfterMs should handle edge case when timerEnd equals minimum`() {
        // When
        val result = TimerTemplateHandler.getDismissAfterMs(timerThreshold = -1, timerEnd = PT_TIMER_MIN_THRESHOLD)

        // Then
        assertEquals(PT_TIMER_MIN_THRESHOLD * ONE_SECOND_LONG + ONE_SECOND_LONG, result)
    }
    private fun createSampleTimerBundle(): Bundle {
        val ptJsonObj = JSONObject().apply {
            put(PT_TITLE, "Timer Title")
            put(PT_MSG, "Timer Message")
            put(PT_MSG_SUMMARY, "Timer Summary")
            put(PT_BIG_IMG, "image_url")
            put(PT_BIG_IMG_ALT_TEXT, "Image alt text")
            put(PT_GIF, "gif_url")
        }

        return Bundle().apply {
            putString(PT_ID, "pt_timer")
            putString(PT_JSON, ptJsonObj.toString())
            putString(Constants.WZRK_PUSH_ID, "push_id_123")
            putString(Constants.PT_NOTIF_ID, "notif_id_123")
            putString(Constants.WZRK_COLLAPSE, "collapse_key")
            putString(PT_COLLAPSE_KEY, "collapse_value")
            putString("wzrk_rnv", "1") // will be removed inside scheduleTimer
        }
    }

    private fun createSampleTimerTemplateData(): TimerTemplateData {
        val textData = BaseTextData(
            title = "Timer Title",
            message = "Timer Message",
            messageSummary = "Timer Summary",
            subtitle = "Timer Subtitle"
        )

        val terminalTextData = BaseTextData(
            title = "Terminal Title",
            message = "Terminal Message",
            messageSummary = "Terminal Summary",
            subtitle = "Terminal Subtitle"
        )

        val colorData = BaseColorData(
            titleColor = "#FF0000",
            messageColor = "#00FF00",
            backgroundColor = "#0000FF",
            metaColor = "#FFFF00",
            smallIconColor = "#FF00FF"
        )

        val iconData = IconData(
            largeIcon = "large_icon_url", smallIconBitmap = null
        )

        val baseContent = BaseContent(
            textData = textData,
            colorData = colorData,
            iconData = iconData,
            deepLinkList = arrayListOf("deeplink1", "deeplink2"),
            notificationBehavior = NotificationBehavior()
        )

        val imageData = ImageData(
            url = "image_url", altText = "Image alt text"
        )

        val gifData = GifData(
            url = "gif_url", numberOfFrames = 5
        )

        val mediaData = MediaData(
            bigImage = imageData, gif = gifData, scaleType = PTScaleType.CENTER_CROP
        )

        val terminalImageData = ImageData(
            url = "terminal_image_url", altText = "Terminal Image alt text"
        )

        val terminalGifData = GifData(
            url = "terminal_gif_url", numberOfFrames = 3
        )

        val terminalMediaData = MediaData(
            bigImage = terminalImageData, gif = terminalGifData, scaleType = PTScaleType.FIT_CENTER
        )

        return TimerTemplateData(
            baseContent = baseContent,
            mediaData = mediaData,
            actions = null,
            terminalTextData = terminalTextData,
            terminalMediaData = terminalMediaData,
            chronometerTitleColor = "#AAAAAA",
            renderTerminal = true
        )
    }
}