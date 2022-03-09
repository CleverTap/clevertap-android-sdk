package com.clevertap.android.sdk;

import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.Constant.Companion
import org.junit.*
import org.junit.runner.*;
import org.mockito.*
import org.robolectric.RobolectricTestRunner;
import java.util.concurrent.Callable
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class CleverTapFactoryTest : BaseTestCase() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_getCoreState_returnsNonNull(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullCoreMetaData(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.coreMetaData)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullValidationResultStack(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.validationResultStack)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullCTLockManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.ctLockManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullMainLooperHandler(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.mainLooperHandler)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullInstanceConfig(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.config)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveInstanceConfigWithActIdActTokenSameAsPassedConfig(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertEquals(Companion.ACC_ID,coreState.config.accountId)
        assertEquals(Companion.ACC_TOKEN,coreState.config.accountToken)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullEventMediator(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.eventMediator)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullLocalDataStore(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.localDataStore)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullDeviceInfo(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.deviceInfo)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullCallbackManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.callbackManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullSessionManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.sessionManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullDBManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.databaseManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullControllerManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.controllerManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullInAppFCManagerInsideControllerManager(){
        // Create instance first to avoid stackoverflow error
        CleverTapAPI.instanceWithConfig(application,cleverTapInstanceConfig)
        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.executors(Mockito.any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
            assertNotNull(coreState.controllerManager.inAppFCManager)
        }
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullNetworkManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.networkManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullEventQueueManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.baseEventQueueManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullAnalyticsManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.analyticsManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullInAppController(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.inAppController)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullInControllerInsideControllerManager(){
        // Create instance first to avoid stackoverflow error
        CleverTapAPI.instanceWithConfig(application,cleverTapInstanceConfig)
        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.executors(Mockito.any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
            assertNotNull(coreState.controllerManager.inAppController)
        }
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullCTFeatureFlagsControllerInsideControllerManager(){
        // Create instance first to avoid stackoverflow error
        CleverTapAPI.instanceWithConfig(application,cleverTapInstanceConfig)
        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.executors(Mockito.any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
            assertNotNull(coreState.controllerManager.ctFeatureFlagsController)
        }
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullLocationManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.locationManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullPushProviders(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.pushProviders)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullActivityLifeCycleManager(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.activityLifeCycleManager)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullLoginController(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.loginController)
    }
}
