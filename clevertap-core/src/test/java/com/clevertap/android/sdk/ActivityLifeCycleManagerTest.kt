package com.clevertap.android.sdk

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
import junit.framework.Assert.assertEquals
import kotlin.test.assertIsNot
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

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


}