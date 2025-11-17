package com.clevertap.android.pushtemplates.handlers

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.clevertap.android.pushtemplates.CancelTemplateData
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class CancelTemplateHandlerTest {

    private lateinit var mockContext: Context
    private lateinit var mockNotificationManager: NotificationManager

    @Before
    fun setUp() {
        mockContext = mockk<Context>(relaxed = true)
        mockNotificationManager = mockk<NotificationManager>(relaxed = true)

        // Mock context to return NotificationManager
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `renderCancelNotification should cancel single notification when cancelNotificationId is valid string`() {
        // Given
        val notificationIdString = "123"
        val expectedNotificationId = 123
        val templateData = CancelTemplateData(
            cancelNotificationId = notificationIdString,
            cancelNotificationIds = arrayListOf()
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 1) { mockNotificationManager.cancel(expectedNotificationId) }
    }

    @Test
    fun `renderCancelNotification should cancel multiple notifications when cancelNotificationId is null and cancelNotificationIds has values`() {
        // Given
        val notificationIds = arrayListOf(100, 200, 300)
        val templateData = CancelTemplateData(
            cancelNotificationId = null,
            cancelNotificationIds = notificationIds
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 1) { mockNotificationManager.cancel(100) }
        verify(exactly = 1) { mockNotificationManager.cancel(200) }
        verify(exactly = 1) { mockNotificationManager.cancel(300) }
    }

    @Test
    fun `renderCancelNotification should cancel multiple notifications when cancelNotificationId is empty and cancelNotificationIds has values`() {
        // Given
        val notificationIds = arrayListOf(400, 500)
        val templateData = CancelTemplateData(
            cancelNotificationId = "",
            cancelNotificationIds = notificationIds
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 1) { mockNotificationManager.cancel(400) }
        verify(exactly = 1) { mockNotificationManager.cancel(500) }
    }

    @Test
    fun `renderCancelNotification should prioritize cancelNotificationId over cancelNotificationIds when both are valid`() {
        // Given
        val singleNotificationId = "999"
        val multipleNotificationIds = arrayListOf(100, 200, 300)
        val templateData = CancelTemplateData(
            cancelNotificationId = singleNotificationId,
            cancelNotificationIds = multipleNotificationIds
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 1) { mockNotificationManager.cancel(999) }
        verify(exactly = 0) { mockNotificationManager.cancel(100) }
        verify(exactly = 0) { mockNotificationManager.cancel(200) }
        verify(exactly = 0) { mockNotificationManager.cancel(300) }
    }

    @Test
    fun `renderCancelNotification should not cancel any notification when both cancelNotificationId and cancelNotificationIds are invalid`() {
        // Given
        val templateData = CancelTemplateData(
            cancelNotificationId = null,
            cancelNotificationIds = arrayListOf()
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 0) { mockNotificationManager.cancel(any()) }
    }

    @Test
    fun `renderCancelNotification should not cancel any notification when cancelNotificationId is empty and cancelNotificationIds is empty`() {
        // Given
        val templateData = CancelTemplateData(
            cancelNotificationId = "",
            cancelNotificationIds = arrayListOf()
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 0) { mockNotificationManager.cancel(any()) }
    }

    @Test
    fun `renderCancelNotification should handle single notification in cancelNotificationIds list`() {
        // Given
        val singleNotificationId = arrayListOf(777)
        val templateData = CancelTemplateData(
            cancelNotificationId = null,
            cancelNotificationIds = singleNotificationId
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 1) { mockNotificationManager.cancel(777) }
        verify(exactly = 1) { mockNotificationManager.cancel(any()) }
    }

    @Test
    fun `renderCancelNotification should properly get NotificationManager from context`() {
        // Given
        val templateData = CancelTemplateData(
            cancelNotificationId = "123",
            cancelNotificationIds = arrayListOf()
        )

        // When
        CancelTemplateHandler.renderCancelNotification(mockContext, templateData)

        // Then
        verify(exactly = 1) { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) }
    }
}