package com.clevertap.android.sdk

import android.location.Location
import android.os.Bundle
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.Constant
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleverTapAPITest : BaseTestCase() {

    private lateinit var corestate: MockCoreState

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                corestate = MockCoreState(application, cleverTapInstanceConfig)
            }
        }
    }

    /* @Test
     fun testActivity() {
         val activity = mock(Activity::class.java)
         val bundle = Bundle()
         //create
         activity.onCreate(bundle, null)
         CleverTapAPI.onActivityCreated(activity, null)
     }*/

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_greater_than_5_secs() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                mockStatic(Utils::class.java).use {

                    // Arrange
                    `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                        .thenReturn(corestate)
                    `when`(Utils.getNow()).thenReturn(Int.MAX_VALUE)

                    CoreMetaData.setInitialAppEnteredForegroundTime(0)

                    // Act
                    CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

                    // Assert
                    assertTrue("isCreatedPostAppLaunch must be true", cleverTapInstanceConfig.isCreatedPostAppLaunch)
                    verify(corestate.sessionManager).setLastVisitTime()
                    verify(corestate.deviceInfo).setDeviceNetworkInfoReportingFromStorage()
                    verify(corestate.deviceInfo).setCurrentUserOptOutStateFromStorage()
                    val actualConfig =
                        StorageHelper.getString(application, "instance:" + cleverTapInstanceConfig.accountId, "")
                    assertEquals(cleverTapInstanceConfig.toJSONString(), actualConfig)
                }
            }
        }
    }

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_less_than_5_secs() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(Utils::class.java).use {
                mockStatic(CleverTapFactory::class.java).use {
                    // Arrange
                    `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                        .thenReturn(corestate)
                    `when`(Utils.getNow()).thenReturn(0)

                    CoreMetaData.setInitialAppEnteredForegroundTime(Int.MAX_VALUE)

                    // Act
                    CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

                    // Assert
                    assertFalse(
                        "isCreatedPostAppLaunch must be false",
                        cleverTapInstanceConfig.isCreatedPostAppLaunch
                    )
                    verify(corestate.sessionManager).setLastVisitTime()
                    verify(corestate.deviceInfo).setDeviceNetworkInfoReportingFromStorage()
                    verify(corestate.deviceInfo).setCurrentUserOptOutStateFromStorage()

                    val string =
                        StorageHelper.getString(application, "instance:" + cleverTapInstanceConfig.accountId, "")
                    assertEquals(cleverTapInstanceConfig.toJSONString(), string)
                }
            }
        }
    }

    @Test
    fun test_setLocationForGeofences() {
        val location = Location("")
        location.apply {
            latitude = 17.4444
            longitude = 4.444
        }
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )

            mockStatic(CleverTapFactory::class.java).use {
                // Arrange
                `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                    .thenReturn(corestate)
                cleverTapAPI = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)
                cleverTapAPI.setLocationForGeofences(location, 45)
                assertTrue(corestate.coreMetaData.isLocationForGeofence)
                assertEquals(corestate.coreMetaData.geofenceSDKVersion, 45)
                verify(corestate.locationManager)._setLocation(location)
            }
        }
    }

    @Test
    fun test_setGeofenceCallback() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                // Arrange
                `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                    .thenReturn(corestate)
                cleverTapAPI = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

                val geofenceCallback = object : GeofenceCallback {
                    override fun handleGeoFences(jsonObject: JSONObject?) {
                        TODO("Not yet implemented")
                    }

                    override fun triggerLocation() {
                        TODO("Not yet implemented")
                    }
                }

                cleverTapAPI.geofenceCallback = geofenceCallback

                assertEquals(geofenceCallback, cleverTapAPI.geofenceCallback)
            }
        }
    }

    @Test
    fun test_pushGeoFenceError() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                    .thenReturn(corestate)
                cleverTapAPI = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

                val expectedErrorCode = 999
                val expectedErrorMessage = "Fire in the hall"

                cleverTapAPI.pushGeoFenceError(expectedErrorCode, expectedErrorMessage)

                val actualValidationResult = corestate.validationResultStack.popValidationResult()
                assertEquals(999, actualValidationResult.errorCode)
                assertEquals("Fire in the hall", actualValidationResult.errorDesc)
            }
        }
    }

    @Test
    fun test_pushGeoFenceExitedEvent() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                val argumentCaptor =
                    ArgumentCaptor.forClass(
                        JSONObject::class.java
                    )

                `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                    .thenReturn(corestate)
                cleverTapAPI = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

                val expectedJson = JSONObject("{\"key\":\"value\"}")

                cleverTapAPI.pushGeoFenceExitedEvent(expectedJson)

                verify(corestate.analyticsManager).raiseEventForGeofences(
                    ArgumentMatchers.anyString(), argumentCaptor.capture()
                )

                assertEquals(expectedJson, argumentCaptor.value)
            }
        }
    }

    @Test
    fun test_pushGeoFenceEnteredEvent() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                    .thenReturn(corestate)
                cleverTapAPI = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

                val expectedJson = JSONObject("{\"key\":\"value\"}")

                cleverTapAPI.pushGeofenceEnteredEvent(expectedJson)
                val argumentCaptor =
                    ArgumentCaptor.forClass(
                        JSONObject::class.java
                    )

                verify(corestate.analyticsManager).raiseEventForGeofences(
                    ArgumentMatchers.anyString(), argumentCaptor.capture()
                )

                assertEquals(expectedJson, argumentCaptor.value)
            }
        }
    }

    @Test
    fun test_changeCredentials_whenDefaultConfigNotNull_credentialsMustNotChange() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(
                    CleverTapFactory.getCoreState(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                    )
                )
                    .thenReturn(corestate)
                CleverTapAPI.getDefaultInstance(application)
                CleverTapAPI.changeCredentials("acct123", "token123", "eu")
                val instance = ManifestInfo.getInstance(application)
                assertNotEquals("acct123", instance.accountId)
                assertNotEquals("token123", instance.acountToken)
                assertNotEquals("eu", instance.accountRegion)
            }
        }
    }

    @Test
    fun test_changeCredentials_whenDefaultConfigNull_credentialsMustChange() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            CleverTapAPI.defaultConfig = null
            CleverTapAPI.changeCredentials("acct123", "token123", "eu")
            val instance = ManifestInfo.getInstance(application)
            assertEquals("acct123", instance.accountId)
            assertEquals("token123", instance.acountToken)
            assertEquals("eu", instance.accountRegion)
        }
    }

    @Test
    fun test_createNotification_whenInstancesNull__createNotificationMustBeCalled() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(
                    CleverTapFactory.getCoreState(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                    )
                )
                    .thenReturn(corestate)
                val bundle = Bundle()
                //CleverTapAPI.getDefaultInstance(application)
                //CleverTapAPI.setInstances(null)
                CleverTapAPI.createNotification(application, bundle)
                verify(corestate.pushProviders)._createNotification(
                    application,
                    bundle,
                    Constants.EMPTY_NOTIFICATION_ID
                )
            }
        }
    }

    @Test
    fun test_createNotification_whenInstanceNotNullAndAcctIDMatches__createNotificationMustBeCalled() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(
                    CleverTapFactory.getCoreState(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                    )
                )
                    .thenReturn(corestate)
                val bundle = Bundle()
                bundle.putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
                CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)
                CleverTapAPI.createNotification(application, bundle)
                verify(corestate.pushProviders)._createNotification(
                    application,
                    bundle,
                    Constants.EMPTY_NOTIFICATION_ID
                )
            }
        }
    }

    @Test
    fun test_createNotification_whenInstanceNotNullAndAcctIdDontMatch_createNotificationMustNotBeCalled() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(
                    CleverTapFactory.getCoreState(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                    )
                )
                    .thenReturn(corestate)
                val bundle = Bundle()
                bundle.putString(Constants.WZRK_ACCT_ID_KEY, "acct123")
                CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)
                CleverTapAPI.createNotification(application, bundle)
                verify(corestate.pushProviders, never())._createNotification(
                    application,
                    bundle,
                    Constants.EMPTY_NOTIFICATION_ID
                )
            }
        }
    }

    @Test
    fun test_getNotificationInfoWhenExtrasNull_fromCleverTapMustBeFalse() {

        val notificationInfo = CleverTapAPI.getNotificationInfo(null)
        assertFalse(notificationInfo.fromCleverTap)
    }

    @Test
    fun test_getNotificationInfoWhenNotificationTagMissing_fromCleverTapAndShouldRenderMustBeFalse() {

        val extras = Bundle()
        //extras.putString(Constants.NOTIFICATION_TAG,"tag")
        val notificationInfo = CleverTapAPI.getNotificationInfo(extras)
        assertFalse(notificationInfo.fromCleverTap)
        assertFalse(notificationInfo.shouldRender)
    }

    @Test
    fun test_getNotificationInfoWhenNMTagMissing_fromCleverTapMustBeTrueAndShouldRenderMustBeFalse() {

        val extras = Bundle()
        extras.putString(Constants.NOTIFICATION_TAG, "tag")
        val notificationInfo = CleverTapAPI.getNotificationInfo(extras)
        assertTrue(notificationInfo.fromCleverTap)
        assertFalse(notificationInfo.shouldRender)
    }

    @Test
    fun test_getNotificationInfoWhenNotificationTagAndNMTagPresent_fromCleverTapAndshouldRenderMustBeTrue() {

        val extras = Bundle()
        extras.putString(Constants.NOTIFICATION_TAG, "tag")
        extras.putString("nm", "nmTag")
        val notificationInfo = CleverTapAPI.getNotificationInfo(extras)
        assertTrue(notificationInfo.fromCleverTap)
        assertTrue(notificationInfo.shouldRender)
    }

    @Test
    fun test_processPushNotification_whenInstancesNull__processCustomPushNotificationMustBeCalled() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(
                    CleverTapFactory.getCoreState(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                    )
                )
                    .thenReturn(corestate)
                val bundle = Bundle()
                //CleverTapAPI.getDefaultInstance(application)
                //CleverTapAPI.setInstances(null)
                CleverTapAPI.processPushNotification(application, bundle)
                verify(corestate.pushProviders).processCustomPushNotification(bundle)
            }
        }
    }

    @Test
    fun test_processPushNotification_whenInstancesNotNull__processCustomPushNotificationMustBeCalled() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(CleverTapFactory::class.java).use {
                `when`(
                    CleverTapFactory.getCoreState(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                    )
                )
                    .thenReturn(corestate)
                val bundle = Bundle()
                bundle.putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
                CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)
                CleverTapAPI.processPushNotification(application, bundle)
                verify(corestate.pushProviders).processCustomPushNotification(bundle)
            }
        }
    }

/* @Test
 fun testPushDeepLink(){
     // Arrange
     var cleverTapAPISpy : CleverTapAPI = Mockito.spy(cleverTapAPI)
     val uri = Uri.parse("https://www.google.com/")

     //Act
     cleverTapAPISpy.pushDeepLink(uri)

     //Assert
     verify(cleverTapAPISpy).pushDeepLink(uri,false)
 }

 @Test
 fun testPushDeviceTokenEvent(){
     // Arrange
     val ctAPI = CleverTapAPI.instanceWithConfig(application,cleverTapInstanceConfig)
     var cleverTapAPISpy = Mockito.spy(ctAPI)

     cleverTapAPISpy.pushDeviceTokenEvent("12345",true,FCM)
     verify(cleverTapAPISpy).queueEvent(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt())
 }

 @Test
 fun testPushLink(){
     var cleverTapAPISpy : CleverTapAPI = Mockito.spy(cleverTapAPI)
     val uri = Uri.parse("https://www.google.com/")

     val mockStatic = Mockito.mockStatic(StorageHelper::class.java)
     `when`(StorageHelper.getInt(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).thenReturn(0)

     //Act
     cleverTapAPISpy.pushInstallReferrer("abc","def","ghi")

     verify(cleverTapAPISpy).pushDeepLink(ArgumentMatchers.any(), ArgumentMatchers.anyBoolean())
 }*/

    @After
    fun tearDown() {
        CleverTapAPI.setInstances(null) // clear existing CleverTapAPI instances
    }
}