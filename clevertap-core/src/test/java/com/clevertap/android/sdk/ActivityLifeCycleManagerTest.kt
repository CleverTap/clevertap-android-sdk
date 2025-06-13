package com.clevertap.android.sdk

import android.app.Activity
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ActivityLifeCycleManagerTest : BaseTestCase() {

    private lateinit var activityLifeCycleManager: ActivityLifeCycleManager
    private lateinit var listener: CTPushProviderListener
    private lateinit var manifestInfo: ManifestInfo
    private lateinit var coreState: CoreState

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        coreState = MockCoreStateKotlin(cleverTapInstanceConfig)
        listener = mockk()
        manifestInfo = mockk()
        activityLifeCycleManager = ActivityLifeCycleManager(
            appCtx, cleverTapInstanceConfig, coreState.analyticsManager, coreState.coreMetaData,
            coreState.sessionManager, coreState.pushProviders, coreState.callbackManager, coreState.inAppController,
            coreState.baseEventQueueManager
        )
    }

    @Test
    fun test_activityPaused_appForegroundInCoreMetadataMustBeFalse() {
        activityLifeCycleManager.activityPaused()
        assertFalse(CoreMetaData.isAppForeground())
    }

    @Ignore
    @Test
    fun test_activityPaused_whenInCurrentSession_LastSessionEpochMustBeSavedInPreference() {
        coreState.coreMetaData.currentSessionId = 100
        activityLifeCycleManager.activityPaused()
        assertNotEquals(-1,StorageHelper.getInt(appCtx,
            StorageHelper.storageKeyWithSuffix(cleverTapInstanceConfig, Constants.LAST_SESSION_EPOCH),-1))

    }

    @Test
    fun test_activityPaused_whenNotInCurrentSession_LastSessionEpochMustNotBeSavedInPreference() {
        coreState.coreMetaData.currentSessionId = 0
        activityLifeCycleManager.activityPaused()
        assertEquals(-1,StorageHelper.getInt(appCtx,
            StorageHelper.storageKeyWithSuffix(cleverTapInstanceConfig, Constants.LAST_SESSION_EPOCH),-1))

    }

    @Test
    fun test_activityResumed_whenAppLaunchedIsPushed() {
        val geofenceCallback = object : GeofenceCallback{
            override fun handleGeoFences(jsonObject: JSONObject?) {
            }

            override fun triggerLocation() {
            }
        }
        val mockActivity = mockk<Activity>()
        val geofenceCallbackSpy = spyk(geofenceCallback)
        coreState.coreMetaData.isAppLaunchPushed = true
        coreState.callbackManager.geofenceCallback = geofenceCallbackSpy
        activityLifeCycleManager.activityResumed(mockActivity)
        verify { coreState.sessionManager.checkTimeoutSession() }
        verify(exactly = 0) { coreState.analyticsManager.pushAppLaunchedEvent() }
        verify(exactly = 0) { coreState.analyticsManager.fetchFeatureFlags() }
        verify(exactly = 0) { coreState.pushProviders.onTokenRefresh() }
        verify(exactly = 0) { geofenceCallbackSpy.triggerLocation() }
        verify { coreState.baseEventQueueManager.pushInitialEventsAsync() }
        verify { coreState.inAppController.showNotificationIfAvailable() }
    }

    @Test
    fun test_activityResumed_whenAppLaunchedIsNotPushed() {
        val geofenceCallback = object : GeofenceCallback{
            override fun handleGeoFences(jsonObject: JSONObject?) {
            }

            override fun triggerLocation() {
            }
        }
        val mockActivity = mockk<Activity>()
        val installReferrerClient = mockk<InstallReferrerClient>()
        val installReferrerClientBuilder = mockk<InstallReferrerClient.Builder>()
        val geofenceCallbackSpy = spyk(geofenceCallback)
        coreState.coreMetaData.isAppLaunchPushed = false
        coreState.coreMetaData.isInstallReferrerDataSent = false
        coreState.coreMetaData.isFirstSession = true
        coreState.coreMetaData.isInstallReferrerDataSent = false
        coreState.callbackManager.geofenceCallback = geofenceCallbackSpy

        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            mockkStatic(InstallReferrerClient::class) {
                val referrerDetails = mockk<ReferrerDetails>(relaxed = true)
                val listenerSlot = slot<InstallReferrerStateListener>()
                every { InstallReferrerClient.newBuilder(appCtx) } returns installReferrerClientBuilder
                every { installReferrerClientBuilder.build() } returns installReferrerClient
                every { installReferrerClient.installReferrer } returns referrerDetails
                val installReferrer = "https://play.google.com/com.company"
                every { referrerDetails.installReferrer } returns installReferrer

                 activityLifeCycleManager.activityResumed(mockActivity)

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
                verify { coreState.pushProviders.onTokenRefresh() }
                verify { geofenceCallbackSpy.triggerLocation() }
                verify { coreState.baseEventQueueManager.pushInitialEventsAsync() }
                verify { coreState.inAppController.showNotificationIfAvailable() }
            }
        }
    }
}
