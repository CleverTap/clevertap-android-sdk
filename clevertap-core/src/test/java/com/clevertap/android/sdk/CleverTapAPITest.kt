package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleverTapAPITest : BaseTestCase() {

    private lateinit var corestate: MockCoreState

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        corestate = MockCoreState(application, cleverTapInstanceConfig)
        corestate.postAsyncSafelyHandler = MockPostAsyncSafelyHandler(cleverTapInstanceConfig)
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
            // Arrange
            `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                .thenReturn(corestate)


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

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_less_than_5_secs() {

        mockStatic(CleverTapFactory::class.java).use {
            // Arrange
            `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                .thenReturn(corestate)

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