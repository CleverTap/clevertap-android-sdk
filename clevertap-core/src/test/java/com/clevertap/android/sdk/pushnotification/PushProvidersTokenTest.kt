package com.clevertap.android.sdk.pushnotification

import android.content.Context
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.CleverTapAPI.DevicePushTokenRefreshListener
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Characterization coverage for the token lifecycle in [PushProviders] — caching/retrieval and the
 * onNewToken refresh notification. Executors are made synchronous via [MockCTExecutors]; token
 * storage uses the real (Robolectric) SharedPreferences.
 */
@RunWith(RobolectricTestRunner::class)
class PushProvidersTokenTest : BaseTestCase() {

    private val fcm = PushConstants.FCM

    @Before
    override fun setUp() {
        super.setUp()
        mockkStatic(CTExecutorFactory::class)
        every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
    }

    @After
    fun tearDownStatics() {
        unmockkStatic(CTExecutorFactory::class)
    }

    private fun buildSut(): PushProviders {
        return PushProviders(
            appCtx, cleverTapInstanceConfig, mockk<BaseDatabaseManager>(relaxed = true),
            mockk<ValidationResultStack>(relaxed = true), mockk<AnalyticsManager>(relaxed = true),
            mockk<CTWorkManager>(relaxed = true), mockk<Clock>(relaxed = true)
        )
    }

    @Test
    fun `cacheToken then getCachedToken round-trips the token`() {
        val sut = buildSut()
        sut.cacheToken("tok-123", fcm)
        assertEquals("tok-123", sut.getCachedToken(fcm))
    }

    @Test
    fun `cacheToken with empty token is ignored`() {
        val sut = buildSut()
        sut.cacheToken("", fcm)
        assertNull(sut.getCachedToken(fcm))
    }

    @Test
    fun `getCachedToken is null when nothing cached`() {
        assertNull(buildSut().getCachedToken(fcm))
    }

    @Test
    fun `isNotificationSupported is false when no provider tokens exist`() {
        // No providers were discovered (init() skipped), so there are no available push types.
        assertFalse(buildSut().isNotificationSupported)
    }

    @Test
    fun `onNewToken notifies the token-refresh listener`() {
        val sut = buildSut()
        val listener = mockk<DevicePushTokenRefreshListener>(relaxed = true)
        sut.setDevicePushTokenRefreshListener(listener)

        sut.onNewToken("fresh-tok", fcm)

        verify(exactly = 1) { listener.devicePushTokenDidRefresh("fresh-tok", fcm) }
    }

    @Test
    fun `onNewToken with empty token does not notify the listener`() {
        val sut = buildSut()
        val listener = mockk<DevicePushTokenRefreshListener>(relaxed = true)
        sut.setDevicePushTokenRefreshListener(listener)

        sut.onNewToken("", fcm)

        verify(exactly = 0) { listener.devicePushTokenDidRefresh(any(), any()) }
    }
}
