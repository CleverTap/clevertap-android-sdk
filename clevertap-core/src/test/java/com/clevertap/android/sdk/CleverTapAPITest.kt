package com.clevertap.android.sdk

import android.location.Location
import com.clevertap.android.sdk.utils.Utils
import com.clevertap.android.shared.test.BaseTestCase
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
        mockStatic(CleverTapFactory::class.java).use {
            corestate = MockCoreState(application, cleverTapInstanceConfig)
            corestate.postAsyncSafelyHandler = MockPostAsyncSafelyHandler(cleverTapInstanceConfig)
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

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_less_than_5_secs() {
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
                assertFalse("isCreatedPostAppLaunch must be false", cleverTapInstanceConfig.isCreatedPostAppLaunch)
                verify(corestate.sessionManager).setLastVisitTime()
                verify(corestate.deviceInfo).setDeviceNetworkInfoReportingFromStorage()
                verify(corestate.deviceInfo).setCurrentUserOptOutStateFromStorage()

                val string = StorageHelper.getString(application, "instance:" + cleverTapInstanceConfig.accountId, "")
                assertEquals(cleverTapInstanceConfig.toJSONString(), string)
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

    @Test
    fun test_setGeofenceCallback() {
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

    @Test
    fun test_pushGeoFenceError() {
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

    @Test
    fun test_pushGeoFenceExitedEvent() {
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

    @Test
    fun test_pushGeoFenceEnteredEvent() {
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

    @Test
    fun test_changeCredentials_whenDefaultConfigNotNull_credentialsMustNotChange() {
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

    @Test
    fun test_changeCredentials_whenDefaultConfigNull_credentialsMustChange() {
        CleverTapAPI.defaultConfig = null
        CleverTapAPI.changeCredentials("acct123", "token123", "eu")
        val instance = ManifestInfo.getInstance(application)
        assertEquals("acct123", instance.accountId)
        assertEquals("token123", instance.acountToken)
        assertEquals("eu", instance.accountRegion)
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