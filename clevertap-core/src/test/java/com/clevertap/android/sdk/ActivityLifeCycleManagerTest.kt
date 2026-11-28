package com.clevertap.android.sdk

import android.net.Uri
import android.os.Bundle
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.clevertap.android.sdk.inbox.InboxV2Bridge
import com.clevertap.android.sdk.network.fetch.FetchTrigger
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FakeClock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ActivityLifeCycleManagerTest : BaseTestCase() {

    private lateinit var activityLifeCycleManager: ActivityLifeCycleManager
    private lateinit var listener: CTPushProviderListener
    private lateinit var manifestInfo: ManifestInfo
    private lateinit var coreState: CoreState
    private lateinit var inboxV2Bridge: InboxV2Bridge

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        coreState = MockCoreStateKotlin(cleverTapInstanceConfig)
        listener = mockk()
        manifestInfo = mockk()
        inboxV2Bridge = mockk(relaxed = true)
        activityLifeCycleManager = ActivityLifeCycleManager(
            appCtx, cleverTapInstanceConfig, coreState.analyticsManager, coreState.coreMetaData,
            coreState.sessionManager, coreState.pushProviders, coreState.callbackManager, coreState.inAppController,
            coreState.baseEventQueueManager,
            MockCTExecutors(),
            FakeClock(),
            inboxV2Bridge
        )
    }

    @Test
    fun test_activityPaused_appForegroundInCoreMetadataMustBeFalse() {
        activityLifeCycleManager.activityPaused()
        assertFalse(CoreMetaData.isAppForeground())
    }

    @Ignore("Failing; disabled in 'Fix failing test cases' (June 2022), needs investigation")
    @Test
    fun test_activityPaused_whenInCurrentSession_LastSessionEpochMustBeSavedInPreference() {
        coreState.coreMetaData.currentSessionId = 100
        activityLifeCycleManager.activityPaused()
        assertNotEquals(-1,StorageHelper.getInt(appCtx,
            StorageHelper.storageKeyWithSuffix(cleverTapInstanceConfig.accountId, Constants.LAST_SESSION_EPOCH),-1))

    }

    @Test
    fun test_activityPaused_whenNotInCurrentSession_LastSessionEpochMustNotBeSavedInPreference() {
        coreState.coreMetaData.currentSessionId = 0
        activityLifeCycleManager.activityPaused()
        assertEquals(-1, StorageHelper.getInt(appCtx,
            StorageHelper.storageKeyWithSuffix(cleverTapInstanceConfig.accountId, Constants.LAST_SESSION_EPOCH),-1))

    }

    @Test
    fun test_appBackgrounded_flushesEventQueue() {
        activityLifeCycleManager.appBackgrounded()
        verify { coreState.baseEventQueueManager.flushOnAppBackground(appCtx) }
    }

    @Test
    fun test_activityPaused_doesNotFlushEventQueue() {
        activityLifeCycleManager.activityPaused()
        verify(exactly = 0) { coreState.baseEventQueueManager.flushOnAppBackground(any()) }
    }

    @Test
    fun test_activityResumed_whenAppLaunchedIsPushed() {
        val geofenceCallback = object : GeofenceCallback{
            override fun handleGeoFences(jsonObject: JSONObject) {
            }

            override fun triggerLocation() {
            }
        }
        val geofenceCallbackSpy = spyk(geofenceCallback)
        coreState.coreMetaData.isAppLaunchPushed = true
        coreState.callbackManager.geofenceCallback = geofenceCallbackSpy
        activityLifeCycleManager.activityResumed()
        verify { coreState.sessionManager.checkTimeoutSession() }
        verify(exactly = 0) { coreState.analyticsManager.pushAppLaunchedEvent() }
        verify(exactly = 0) { coreState.analyticsManager.fetchFeatureFlags() }
        verify(exactly = 0) { inboxV2Bridge.submit(any(), any()) }
        verify(exactly = 0) { coreState.pushProviders.onTokenRefresh() }
        verify(exactly = 0) { geofenceCallbackSpy.triggerLocation() }
        verify { coreState.baseEventQueueManager.pushInitialEventsAsync() }
        verify { coreState.inAppController.showNotificationIfAvailable() }
    }

    @Test
    fun test_activityResumed_whenAppLaunchedIsNotPushed() {
        val geofenceCallback = object : GeofenceCallback{
            override fun handleGeoFences(jsonObject: JSONObject) {
            }

            override fun triggerLocation() {
            }
        }
        val installReferrerClient = mockk<InstallReferrerClient>()
        val installReferrerClientBuilder = mockk<InstallReferrerClient.Builder>()
        val geofenceCallbackSpy = spyk(geofenceCallback)
        coreState.coreMetaData.isAppLaunchPushed = false
        coreState.coreMetaData.isInstallReferrerDataSent = false
        coreState.coreMetaData.isFirstSession = true
        coreState.coreMetaData.isInstallReferrerDataSent = false
        coreState.callbackManager.geofenceCallback = geofenceCallbackSpy

        mockkStatic(InstallReferrerClient::class) {
            val referrerDetails = mockk<ReferrerDetails>(relaxed = true)
            val listenerSlot = slot<InstallReferrerStateListener>()
            every { InstallReferrerClient.newBuilder(appCtx) } returns installReferrerClientBuilder
            every { installReferrerClientBuilder.build() } returns installReferrerClient
            every { installReferrerClient.installReferrer } returns referrerDetails
            val installReferrer = "https://play.google.com/com.company"
            every { referrerDetails.installReferrer } returns installReferrer

            mockkStatic(Utils::class) {
                every {
                    Utils.cleanupOldGIFs(
                        appCtx,
                        cleverTapInstanceConfig,
                        FakeClock()
                    )
                } returns Unit
                activityLifeCycleManager.activityResumed()
                verify { Utils.cleanupOldGIFs(any(), any(), any()) }
            }

            verify { installReferrerClient.startConnection(capture(listenerSlot)) }
            val installReferrerStateListener: InstallReferrerStateListener =
                listenerSlot.captured
            installReferrerStateListener.onInstallReferrerSetupFinished(InstallReferrerClient.InstallReferrerResponse.OK)

            verify { installReferrerClient.installReferrer }
            verify { coreState.analyticsManager.pushInstallReferrer(installReferrer) }
            assertTrue(coreState.coreMetaData.isInstallReferrerDataSent)

            verify { coreState.sessionManager.checkTimeoutSession() }
            verify { coreState.analyticsManager.pushAppLaunchedEvent() }
            verify { coreState.analyticsManager.fetchFeatureFlags() }
            verify(exactly = 1) { inboxV2Bridge.submit(FetchTrigger.SYSTEM, null) }
            verify { coreState.pushProviders.onTokenRefresh() }
            verify { geofenceCallbackSpy.triggerLocation() }
            verify { coreState.baseEventQueueManager.pushInitialEventsAsync() }
            verify { coreState.inAppController.showNotificationIfAvailable() }
        }
    }

    @Test
    fun `subsequent activityResumed calls within the same launch do not re-submit V2 fetch`() {
        coreState.coreMetaData.isAppLaunchPushed = true

        activityLifeCycleManager.activityResumed()
        activityLifeCycleManager.activityResumed()

        verify(exactly = 0) { inboxV2Bridge.submit(any(), any()) }
    }

    @Test
    fun `onActivityCreated for another account pushes nothing`() {
        val notification = Bundle().apply { putString(Constants.NOTIFICATION_TAG, "true") }

        activityLifeCycleManager.onActivityCreated(notification, Uri.parse("app://deeplink"), "other-account")

        verify(exactly = 0) { coreState.analyticsManager.pushNotificationClickedEvent(any()) }
        verify(exactly = 0) { coreState.analyticsManager.pushDeepLink(any(), any()) }
    }

    @Test
    fun `onActivityCreated pushes notification click and deep link for this account`() {
        val notification = Bundle().apply { putString(Constants.NOTIFICATION_TAG, "true") }
        val deepLink = Uri.parse("app://deeplink")

        activityLifeCycleManager.onActivityCreated(notification, deepLink, cleverTapInstanceConfig.accountId)

        verify(exactly = 1) { coreState.analyticsManager.pushNotificationClickedEvent(notification) }
        verify(exactly = 1) { coreState.analyticsManager.pushDeepLink(deepLink, false) }
    }

    @Test
    fun `onActivityCreated ignores a notification bundle without the CleverTap tag`() {
        val notification = Bundle().apply { putString("other", "value") }

        activityLifeCycleManager.onActivityCreated(notification, null, cleverTapInstanceConfig.accountId)

        verify(exactly = 0) { coreState.analyticsManager.pushNotificationClickedEvent(any()) }
        verify(exactly = 0) { coreState.analyticsManager.pushDeepLink(any(), any()) }
    }

    @Test
    fun `onActivityCreated swallows a deep link failure`() {
        val deepLink = Uri.parse("app://deeplink")
        every { coreState.analyticsManager.pushDeepLink(deepLink, false) } throws RuntimeException("boom")

        activityLifeCycleManager.onActivityCreated(null, deepLink, cleverTapInstanceConfig.accountId)

        verify(exactly = 1) { coreState.analyticsManager.pushDeepLink(deepLink, false) }
    }
}
