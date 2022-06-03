package com.clevertap.android.sdk

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import com.clevertap.android.sdk.pushnotification.fcm.FcmSdkHandlerImpl
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM
import com.clevertap.android.sdk.pushnotification.fcm.TestFcmConstants.Companion.FCM_SENDER_ID
import com.clevertap.android.sdk.utils.PackageUtils
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.iid.FirebaseInstanceId
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import org.robolectric.RuntimeEnvironment

import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.mockito.*
import kotlin.test.assertIsNot
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ActivityLifeCycleManagerTest : BaseTestCase() {

    private lateinit var activityLifeCycleManager: ActivityLifeCycleManager
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var handler: FcmSdkHandlerImpl
    private lateinit var listener: CTPushProviderListener
    private lateinit var manifestInfo: ManifestInfo
    private lateinit var coreState: CoreState

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        coreState = MockCoreState(appCtx,cleverTapInstanceConfig)
        listener = mock(CTPushProviderListener::class.java)
        manifestInfo = mock(ManifestInfo::class.java)
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
        val mockActivity = mock(Activity::class.java)
        val geofenceCallbackSpy = spy(geofenceCallback)
        coreState.coreMetaData.isAppLaunchPushed = true
        coreState.callbackManager.geofenceCallback = geofenceCallbackSpy
        //`when`(coreState.callbackManager.geofenceCallback).thenReturn(geofenceCallback)
        activityLifeCycleManager.activityResumed(mockActivity)
        verify(coreState.sessionManager).checkTimeoutSession()
        verify(coreState.analyticsManager,times(0)).pushAppLaunchedEvent()
        verify(coreState.analyticsManager,times(0)).fetchFeatureFlags()
        verify(coreState.pushProviders,times(0)).onTokenRefresh()
        verify(geofenceCallbackSpy, never()).triggerLocation()
        verify(coreState.baseEventQueueManager).pushInitialEventsAsync()
        verify(coreState.inAppController).checkExistingInAppNotifications(mockActivity)
        verify(coreState.inAppController).checkPendingInAppNotifications(mockActivity)
    }

    @Test
    fun test_activityResumed_whenAppLaunchedIsNotPushed() {
        val geofenceCallback = object : GeofenceCallback{
            override fun handleGeoFences(jsonObject: JSONObject?) {
            }

            override fun triggerLocation() {
            }
        }
        val mockActivity = mock(Activity::class.java)
        val installReferrerClient = mock(InstallReferrerClient::class.java)
        val installReferrerClientBuilder = mock(InstallReferrerClient.Builder::class.java)
        val geofenceCallbackSpy = spy(geofenceCallback)
        coreState.coreMetaData.isAppLaunchPushed = false
        coreState.coreMetaData.isInstallReferrerDataSent = false
        coreState.coreMetaData.isFirstSession = true
        coreState.coreMetaData.isInstallReferrerDataSent = false
        coreState.callbackManager.geofenceCallback = geofenceCallbackSpy
        //`when`(coreState.callbackManager.geofenceCallback).thenReturn(geofenceCallback)

        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(InstallReferrerClient::class.java).use {
                val referrerDetails = mock(ReferrerDetails::class.java)
                val captor = ArgumentCaptor.forClass(InstallReferrerStateListener::class.java)
                `when`(InstallReferrerClient.newBuilder(appCtx)).thenReturn(installReferrerClientBuilder)
                `when`(installReferrerClientBuilder.build()).thenReturn(installReferrerClient)
                `when`(installReferrerClient.installReferrer).thenReturn(referrerDetails)
                `when`(referrerDetails.installReferrer).thenReturn("https://play.google.com/com.company")
                 activityLifeCycleManager.activityResumed(mockActivity)

                verify(installReferrerClient).startConnection(captor.capture())
                val installReferrerStateListener : InstallReferrerStateListener = captor.value
                installReferrerStateListener.onInstallReferrerSetupFinished(InstallReferrerClient.InstallReferrerResponse.OK)

                verify(installReferrerClient).installReferrer
                verify(coreState.analyticsManager).pushInstallReferrer("https://play.google.com/com.company")
                assertTrue(coreState.coreMetaData.isInstallReferrerDataSent)

                verify(coreState.sessionManager).checkTimeoutSession()
                verify(coreState.analyticsManager).pushAppLaunchedEvent()
                verify(coreState.analyticsManager).fetchFeatureFlags()
                verify(coreState.pushProviders).onTokenRefresh()
                verify(geofenceCallbackSpy).triggerLocation()
                verify(coreState.baseEventQueueManager).pushInitialEventsAsync()
                verify(coreState.inAppController).checkExistingInAppNotifications(mockActivity)
                verify(coreState.inAppController).checkPendingInAppNotifications(mockActivity)
            }

        }



    }


}