package com.clevertap.android.sdk

import android.location.Location
import android.os.Bundle
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.pushnotification.CoreNotificationRenderer
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.usereventlogs.UserEventLogTestData
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

    // Common setup helper functions
    private fun executeMockExecutors(block: () -> Unit) {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            block()
        }
    }

    private fun executeMockFactory(block: () -> Unit) {
        mockStatic(CleverTapFactory::class.java).use {
            `when`(CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, null))
                .thenReturn(corestate)
            block()
        }
    }

    private fun executeMockFactoryWithAny(block: () -> Unit) {
        mockStatic(CleverTapFactory::class.java).use {
            `when`(
                CleverTapFactory.getCoreState(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                )
            )
                .thenReturn(corestate)
            block()
        }
    }

    private fun executeBasicTest(block: () -> Unit) {
        executeMockExecutors {
            executeMockFactory {
                block()
            }
        }
    }

    private fun executeBasicTestWithAny(block: () -> Unit) {
        executeMockExecutors {
            executeMockFactoryWithAny {
                block()
            }
        }
    }

    private fun initializeCleverTapAPI() {
        cleverTapAPI = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)
    }

    private fun verifyCommonConstructorBehavior() {
        verify(corestate.sessionManager).setLastVisitTime()
        verify(corestate.sessionManager).setUserLastVisitTs()
        verify(corestate.deviceInfo).setDeviceNetworkInfoReportingFromStorage()
        verify(corestate.deviceInfo).setCurrentUserOptOutStateFromStorage()
        val actualConfig = StorageHelper.getString(
            application,
            "instance:" + cleverTapInstanceConfig.accountId,
            ""
        )
        assertEquals(cleverTapInstanceConfig.toJSONString(), actualConfig)
    }

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        corestate = MockCoreState(cleverTapInstanceConfig)
    }

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_greater_than_5_secs() {
        executeBasicTest {
            mockStatic(Utils::class.java).use {
                // Arrange
                `when`(Utils.getNow()).thenReturn(Int.MAX_VALUE)
                CoreMetaData.setInitialAppEnteredForegroundTime(0)

                // Act
                initializeCleverTapAPI()

                // Assert
                assertTrue(
                    "isCreatedPostAppLaunch must be true",
                    cleverTapInstanceConfig.isCreatedPostAppLaunch
                )
                verifyCommonConstructorBehavior()
            }
        }
    }

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_less_than_5_secs() {
        executeBasicTest {
            mockStatic(Utils::class.java).use {
                // Arrange
                `when`(Utils.getNow()).thenReturn(0)
                CoreMetaData.setInitialAppEnteredForegroundTime(Int.MAX_VALUE)

                // Act
                initializeCleverTapAPI()

                // Assert
                assertFalse(
                    "isCreatedPostAppLaunch must be false",
                    cleverTapInstanceConfig.isCreatedPostAppLaunch
                )
                verifyCommonConstructorBehavior()
            }
        }
    }

    @Test
    fun test_setLocationForGeofences() {
        executeBasicTest {
            val location = Location("").apply {
                latitude = 17.4444
                longitude = 4.444
            }

            initializeCleverTapAPI()
            cleverTapAPI.setLocationForGeofences(location, 45)

            assertTrue(corestate.coreMetaData.isLocationForGeofence)
            assertEquals(corestate.coreMetaData.geofenceSDKVersion, 45)
            verify(corestate.locationManager)._setLocation(location)
        }
    }

    @Test
    fun test_setGeofenceCallback() {
        executeBasicTest {
            // Arrange
            val geofenceCallback = object : GeofenceCallback {
                override fun handleGeoFences(jsonObject: JSONObject?) {
                }

                override fun triggerLocation() {
                }
            }

            // Act
            initializeCleverTapAPI()
            cleverTapAPI.geofenceCallback = geofenceCallback

            // Assert
            assertEquals(geofenceCallback, cleverTapAPI.geofenceCallback)
        }
    }

    @Test
    fun test_pushGeoFenceError() {
        executeBasicTest {
            // Arrange
            val expectedErrorCode = 999
            val expectedErrorMessage = "Fire in the hall"

            // Act
            initializeCleverTapAPI()
            cleverTapAPI.pushGeoFenceError(expectedErrorCode, expectedErrorMessage)

            // Assert
            val actualValidationResult = corestate.validationResultStack.popValidationResult()
            assertEquals(expectedErrorCode, actualValidationResult.errorCode)
            assertEquals(expectedErrorMessage, actualValidationResult.errorDesc)
        }
    }

    @Test
    fun test_pushGeoFenceExitedEvent() {
        executeBasicTest {
            // Arrange
            val expectedJson = JSONObject("{\"key\":\"value\"}")
            val argumentCaptor = ArgumentCaptor.forClass(JSONObject::class.java)

            // Act
            initializeCleverTapAPI()
            cleverTapAPI.pushGeoFenceExitedEvent(expectedJson)

            // Assert
            verify(corestate.analyticsManager).raiseEventForGeofences(
                ArgumentMatchers.anyString(),
                argumentCaptor.capture()
            )
            assertEquals(expectedJson, argumentCaptor.value)
        }
    }

    @Test
    fun test_pushGeoFenceEnteredEvent() {
        executeBasicTest {
            // Arrange
            val expectedJson = JSONObject("{\"key\":\"value\"}")
            val argumentCaptor = ArgumentCaptor.forClass(JSONObject::class.java)

            // Act
            initializeCleverTapAPI()
            cleverTapAPI.pushGeofenceEnteredEvent(expectedJson)

            // Assert
            verify(corestate.analyticsManager).raiseEventForGeofences(
                ArgumentMatchers.anyString(),
                argumentCaptor.capture()
            )
            assertEquals(expectedJson, argumentCaptor.value)
        }
    }

    @Test
    fun test_changeCredentials_whenDefaultConfigNotNull_credentialsMustNotChange() {
        executeBasicTestWithAny {
            val expectedAccountId = "acct123"
            val expectedToken = "token123"
            val expectedRegion = "eu"
            CleverTapAPI.getDefaultInstance(application)

            val manifestInfo = ManifestInfo.getInstance(application)
            // Act
            CleverTapAPI.changeCredentials(expectedAccountId, expectedToken, expectedRegion)

            // Assert
            with(manifestInfo) {
                assertNotEquals(expectedAccountId, accountId)
                assertNotEquals(expectedToken, acountToken)
                assertNotEquals(expectedRegion, accountRegion)
            }
        }
    }

    @Test
    fun test_changeCredentials_whenDefaultConfigNull_credentialsMustChange() {
        executeMockExecutors {
            // Arrange
            val expectedAccountId = "acct123"
            val expectedToken = "token123"
            val expectedRegion = "eu"
            CleverTapAPI.defaultConfig = null

            // Act
            CleverTapAPI.changeCredentials(expectedAccountId, expectedToken, expectedRegion)

            // Assert
            val manifestInfo = ManifestInfo.getInstance(application)
            with(manifestInfo) {
                assertEquals(expectedAccountId, accountId)
                assertEquals(expectedToken, acountToken)
                assertEquals(expectedRegion, accountRegion)
            }
        }
    }

    @Test
    fun test_createNotification_whenInstancesNull__createNotificationMustBeCalled() {
        executeBasicTestWithAny {
            val bundle = Bundle()
            val lock = Object()
            `when`(corestate.pushProviders.pushRenderingLock).thenReturn(lock)
            CleverTapAPI.createNotification(application, bundle)
            verify(corestate.pushProviders).pushNotificationRenderer =
                any(CoreNotificationRenderer::class.java)
            verify(corestate.pushProviders)._createNotification(
                application,
                bundle,
                Constants.EMPTY_NOTIFICATION_ID
            )
        }
    }

    @Test
    fun test_createNotification_whenInstanceNotNullAndAcctIDMatches__createNotificationMustBeCalled() {
        executeBasicTestWithAny {
            val bundle = Bundle()
            val lock = Object()
            bundle.putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
            CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

            `when`(corestate.pushProviders.pushRenderingLock).thenReturn(lock)
            CleverTapAPI.createNotification(application, bundle)
            verify(corestate.pushProviders).pushNotificationRenderer =
                any(CoreNotificationRenderer::class.java)
            verify(corestate.pushProviders)._createNotification(
                application,
                bundle,
                Constants.EMPTY_NOTIFICATION_ID
            )
        }
    }

    @Test
    fun test_createNotification_whenInstanceNotNullAndAcctIdDontMatch_createNotificationMustNotBeCalled() {
        executeMockFactoryWithAny {
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

        executeMockFactoryWithAny {
            val bundle = Bundle()
            CleverTapAPI.processPushNotification(application, bundle)
            verify(corestate.pushProviders).processCustomPushNotification(bundle)
        }
    }

    @Test
    fun test_processPushNotification_whenInstancesNotNull__processCustomPushNotificationMustBeCalled() {
        executeBasicTestWithAny {
            val bundle = Bundle()
            bundle.putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
            CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)
            CleverTapAPI.processPushNotification(application, bundle)
            verify(corestate.pushProviders).processCustomPushNotification(bundle)
        }
    }

    @Test
    fun deleteInboxMessagesForIDs_inboxControllerNull_logsError() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = null
        val controllerManager = mock(ControllerManager::class.java)
        corestate.controllerManager = controllerManager

        // Act
        executeBasicTest {
            `when`(controllerManager.ctInboxController).thenReturn(inboxController)
            initializeCleverTapAPI()
            cleverTapAPI.deleteInboxMessagesForIDs(messageIDs)

            // Assert
            verify(controllerManager).ctInboxController
            verifyNoMoreInteractions(controllerManager)
        }
    }

    @Test
    fun deleteInboxMessagesForIDs_inboxControllerNotNull_deletesMessages() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = mock(CTInboxController::class.java)
        val controllerManager = mock(ControllerManager::class.java)
        corestate.controllerManager = controllerManager

        // Act
        executeBasicTest {
            `when`(controllerManager.ctInboxController).thenReturn(inboxController)
            initializeCleverTapAPI()
            cleverTapAPI.deleteInboxMessagesForIDs(messageIDs)

            // Assert
            verify(controllerManager, times(2)).ctInboxController
            verify(inboxController).deleteInboxMessagesForIDs(messageIDs)
        }
    }

    @Test
    fun markReadInboxMessagesForIDs_inboxControllerNull_logsError() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = null
        val controllerManager = mock(ControllerManager::class.java)
        corestate.controllerManager = controllerManager

        // Act
        executeBasicTest {
            `when`(controllerManager.ctInboxController).thenReturn(inboxController)
            initializeCleverTapAPI()
            cleverTapAPI.markReadInboxMessagesForIDs(messageIDs)

            // Assert
            verify(controllerManager).ctInboxController
            verifyNoMoreInteractions(controllerManager)
        }
    }

    @Test
    fun markReadInboxMessagesForIDs_inboxControllerNotNull_marksRead() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = mock(CTInboxController::class.java)
        val controllerManager = mock(ControllerManager::class.java)
        corestate.controllerManager = controllerManager

        // Act
        executeBasicTest {
            `when`(controllerManager.ctInboxController).thenReturn(inboxController)
            initializeCleverTapAPI()
            cleverTapAPI.markReadInboxMessagesForIDs(messageIDs)

            // Assert
            verify(controllerManager, times(2)).ctInboxController
            verify(inboxController).markReadInboxMessagesForIDs(messageIDs)
        }
    }

    @Test
    fun `test getUserEventLogCount`() {
        // Arrange
        val evt = UserEventLogTestData.EventNames.TEST_EVENT
        `when`(corestate.localDataStore.readUserEventLogCount(evt)).thenReturn(1)

        // Act
        executeBasicTest {
            initializeCleverTapAPI()
            val userEventLogCountActual = cleverTapAPI.getUserEventLogCount(evt)

            // Assert
            assertEquals(1, userEventLogCountActual)
            verify(corestate.localDataStore).readUserEventLogCount(evt)
        }
    }

    @Test
    fun `test getUserEventLog`() {
        // Arrange
        val evt = UserEventLogTestData.EventNames.TEST_EVENT
        val log = UserEventLogTestData.EventNames.sampleUserEventLogsForSameDeviceId[0]
        `when`(corestate.localDataStore.readUserEventLog(evt)).thenReturn(log)

        // Act
        executeBasicTest {
            initializeCleverTapAPI()
            val userEventLogActual = cleverTapAPI.getUserEventLog(evt)

            // Assert
            assertSame(log, userEventLogActual)
            verify(corestate.localDataStore).readUserEventLog(evt)
        }
    }

    @Test
    fun `test getUserEventLogHistory`() {
        // Arrange
        val logs = UserEventLogTestData.EventNames.sampleUserEventLogsForSameDeviceId
        `when`(corestate.localDataStore.readUserEventLogs()).thenReturn(logs)

        // Act
        executeBasicTest {
            initializeCleverTapAPI()
            val historyActual = cleverTapAPI.userEventLogHistory

            // Assert
            assertEquals(2, historyActual.size)
            assertEquals(logs[0], historyActual.values.elementAt(0))
            assertEquals(logs[1], historyActual.values.elementAt(1))
            verify(corestate.localDataStore).readUserEventLogs()
        }
    }

    @Test
    fun `test getUserLastVisitTs`() {
        val expectedUserLastVisitTs = UserEventLogTestData.TestTimestamps.SAMPLE_TIMESTAMP
        // Arrange
        `when`(corestate.sessionManager.userLastVisitTs).thenReturn(expectedUserLastVisitTs)

        // Act
        executeBasicTest {
            initializeCleverTapAPI()
            val lastVisitTsActual = cleverTapAPI.userLastVisitTs

            // Assert
            assertEquals(expectedUserLastVisitTs, lastVisitTsActual)
            verify(corestate.sessionManager).userLastVisitTs
        }
    }

    @Test
    fun `test getUserAppLaunchCount`() {
        // Arrange
        `when`(corestate.localDataStore.readUserEventLogCount(Constants.APP_LAUNCHED_EVENT))
            .thenReturn(5)

        // Act
        executeBasicTest {
            initializeCleverTapAPI()
            val appLaunchCountActual = cleverTapAPI.userAppLaunchCount

            // Assert
            assertEquals(5, appLaunchCountActual)
            verify(corestate.localDataStore).readUserEventLogCount(Constants.APP_LAUNCHED_EVENT)
        }
    }

    @After
    fun tearDown() {
        CleverTapAPI.setInstances(null) // clear existing CleverTapAPI instances
    }
}