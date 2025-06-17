package com.clevertap.android.sdk

import android.location.Location
import android.os.Bundle
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.pushnotification.CoreNotificationRenderer
import com.clevertap.android.sdk.usereventlogs.UserEventLogTestData
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.Constant
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleverTapAPITest : BaseTestCase() {

    private lateinit var corestate: MockCoreStateKotlin
    private lateinit var testClock: TestClock

    private fun initializeCleverTapAPI() {
        cleverTapAPI = CleverTapAPI(application, cleverTapInstanceConfig, corestate, testClock)

        // we need to do this for static methods of CleverTapAPI tests to work correctly.
        CleverTapAPI.setInstances(hashMapOf(Constant.ACC_ID to cleverTapAPI))
    }

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        corestate = MockCoreStateKotlin(cleverTapInstanceConfig)
        testClock = TestClock()
    }

    private fun verifyCommonConstructorBehavior() {
        verify { corestate.sessionManager.setLastVisitTime() }
        verify { corestate.sessionManager.setUserLastVisitTs() }
        verify { corestate.deviceInfo.setDeviceNetworkInfoReportingFromStorage() }
        verify { corestate.deviceInfo.setCurrentUserOptOutStateFromStorage() }
        val actualConfig = StorageHelper.getString(
            application,
            "instance:" + cleverTapInstanceConfig.accountId,
            ""
        )
        assertEquals(cleverTapInstanceConfig.toJSONString(), actualConfig)
    }

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_greater_than_5_secs() {
        testClock.setCurrentTime(Int.MAX_VALUE.toLong())
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

    @Test
    @Ignore("This might be actual bug which happens due to long to int conversion, current ts does not overflow and cause this test to fail in real device")
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_greater_than_5_secs_long_int_conversion() {
        testClock.setCurrentTime(Long.MAX_VALUE)
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

    @Test
    fun testCleverTapAPI_constructor_when_InitialAppEnteredForegroundTime_less_than_5_secs() {
        testClock.setCurrentTime(0)
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

    @Test
    fun test_setLocationForGeofences() {
        val location = Location("").apply {
            latitude = 17.4444
            longitude = 4.444
        }

        initializeCleverTapAPI()
        cleverTapAPI.setLocationForGeofences(location, 45)

        assertTrue(corestate.coreMetaData.isLocationForGeofence)
        assertEquals(corestate.coreMetaData.geofenceSDKVersion, 45)
        verify { corestate.locationManager._setLocation(location) }
    }

    @Test
    fun test_setGeofenceCallback() {
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

    @Test
    fun test_pushGeoFenceError() {
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

    @Test
    fun test_pushGeoFenceExitedEvent() {
        // Arrange
        val expectedJson = JSONObject("{\"key\":\"value\"}")
        val jsonSlot = slot<JSONObject>()

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushGeoFenceExitedEvent(expectedJson)

        // Assert
        verify {
            corestate.analyticsManager.raiseEventForGeofences(any(), capture(jsonSlot))
        }
        assertEquals(expectedJson, jsonSlot.captured)
    }

    @Test
    fun test_pushGeoFenceEnteredEvent() {
        // Arrange
        val expectedJson = JSONObject("{\"key\":\"value\"}")
        val jsonSlot = slot<JSONObject>()

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushGeofenceEnteredEvent(expectedJson)

        // Assert
        verify {
            corestate.analyticsManager.raiseEventForGeofences(any(), capture(jsonSlot))
        }
        assertEquals(expectedJson, jsonSlot.captured)
    }

    @Test
    fun test_changeCredentials_whenDefaultConfigNotNull_credentialsMustNotChange() {
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
            assertNotEquals(expectedToken, accountToken)
            assertNotEquals(expectedRegion, accountRegion)
        }
    }

    @Test
    fun test_changeCredentials_whenDefaultConfigNull_credentialsMustChange() {
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
            assertEquals(expectedToken, accountToken)
            assertEquals(expectedRegion, accountRegion)
        }
    }

    @Test
    fun test_createNotification_whenInstancesNull__createNotificationMustBeCalled() {
        initializeCleverTapAPI() // force so instance is created and cached within CleverTapAPI
        val bundle = Bundle().apply {
            putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
        }
        CleverTapAPI.createNotification(application, bundle, Constants.EMPTY_NOTIFICATION_ID)
        verify {
            corestate.pushProviders.pushNotificationRenderer = any<CoreNotificationRenderer>()
        }
        verify {
            corestate.pushProviders._createNotification(
                application,
                bundle,
                Constants.EMPTY_NOTIFICATION_ID
            )
        }
    }

    @Test
    fun test_createNotification_whenInstanceNotNullAndAcctIDMatches__createNotificationMustBeCalled() {
        initializeCleverTapAPI() // force so instance is created and cached within CleverTapAPI
        val bundle = Bundle().apply {
            putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
        }
        bundle.putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)

        CleverTapAPI.createNotification(application, bundle)
        verify {
            corestate.pushProviders.pushNotificationRenderer = any<CoreNotificationRenderer>()
        }
        verify {
            corestate.pushProviders._createNotification(
                application,
                bundle,
                Constants.EMPTY_NOTIFICATION_ID
            )
        }
    }

    @Test
    fun test_createNotification_whenInstanceNotNullAndAcctIdDontMatch_createNotificationMustNotBeCalled() {
        val bundle = Bundle().apply {
            putString(Constants.WZRK_ACCT_ID_KEY, "acct123")
        }
        CleverTapAPI.createNotification(application, bundle)
        verify(exactly = 0) {
            corestate.pushProviders._createNotification(
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
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "tag")
        }
        val notificationInfo = CleverTapAPI.getNotificationInfo(extras)
        assertTrue(notificationInfo.fromCleverTap)
        assertFalse(notificationInfo.shouldRender)
    }

    @Test
    fun test_getNotificationInfoWhenNotificationTagAndNMTagPresent_fromCleverTapAndshouldRenderMustBeTrue() {
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "tag")
            putString("nm", "nmTag")
        }
        val notificationInfo = CleverTapAPI.getNotificationInfo(extras)
        assertTrue(notificationInfo.fromCleverTap)
        assertTrue(notificationInfo.shouldRender)
    }

    @Test
    fun test_processPushNotification_whenInstancesNull__processCustomPushNotificationMustBeCalled() {
        initializeCleverTapAPI() // force so instance is created and cached within CleverTapAPI
        val bundle = Bundle().apply {
            putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
        }
        CleverTapAPI.processPushNotification(application, bundle)
        verify { corestate.pushProviders.processCustomPushNotification(bundle) }
    }

    @Test
    fun test_processPushNotification_whenInstancesNotNull__processCustomPushNotificationMustBeCalled() {
        initializeCleverTapAPI() // force so instance is created and cached within CleverTapAPI
        val bundle = Bundle().apply {
            putString(Constants.WZRK_ACCT_ID_KEY, Constant.ACC_ID)
        }
        CleverTapAPI.processPushNotification(application, bundle)
        verify { corestate.pushProviders.processCustomPushNotification(bundle) }
    }

    @Test
    fun deleteInboxMessagesForIDs_inboxControllerNull_logsError() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = null

        // Act
        every { corestate.controllerManager.ctInboxController } returns inboxController
        initializeCleverTapAPI()
        cleverTapAPI.deleteInboxMessagesForIDs(messageIDs)

        // Assert
        verify { corestate.controllerManager.ctInboxController }
        confirmVerified(corestate.controllerManager)
    }

    @Test
    fun deleteInboxMessagesForIDs_inboxControllerNotNull_deletesMessages() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = mockk<CTInboxController>(relaxed = true)

        // Act
        every { corestate.controllerManager.ctInboxController } returns inboxController
        initializeCleverTapAPI()
        cleverTapAPI.deleteInboxMessagesForIDs(messageIDs)

        // Assert
        verify(exactly = 2) { corestate.controllerManager.ctInboxController }
        verify { inboxController.deleteInboxMessagesForIDs(messageIDs) }
    }

    @Test
    fun markReadInboxMessagesForIDs_inboxControllerNull_logsError() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = null

        // Act
        every { corestate.controllerManager.ctInboxController } returns inboxController
        initializeCleverTapAPI()
        cleverTapAPI.markReadInboxMessagesForIDs(messageIDs)

        // Assert
        verify { corestate.controllerManager.ctInboxController }
        confirmVerified(corestate.controllerManager)
    }

    @Test
    fun markReadInboxMessagesForIDs_inboxControllerNotNull_marksRead() {
        // Arrange
        val messageIDs = arrayListOf("1", "2", "3")
        val inboxController = mockk<CTInboxController>(relaxed = true)

        // Act
        every { corestate.controllerManager.ctInboxController } returns inboxController
        initializeCleverTapAPI()
        cleverTapAPI.markReadInboxMessagesForIDs(messageIDs)

        // Assert
        verify(exactly = 2) { corestate.controllerManager.ctInboxController }
        verify { inboxController.markReadInboxMessagesForIDs(messageIDs) }
    }

    @Test
    fun `test getUserEventLogCount`() {
        // Arrange
        val evt = UserEventLogTestData.EventNames.TEST_EVENT
        every { corestate.localDataStore.readUserEventLogCount(evt) } returns 1

        // Act
        initializeCleverTapAPI()
        val userEventLogCountActual = cleverTapAPI.getUserEventLogCount(evt)

        // Assert
        assertEquals(1, userEventLogCountActual)
        verify { corestate.localDataStore.readUserEventLogCount(evt) }
    }

    @Test
    fun `test getUserEventLog`() {
        // Arrange
        val evt = UserEventLogTestData.EventNames.TEST_EVENT
        val log = UserEventLogTestData.EventNames.sampleUserEventLogsForSameDeviceId[0]
        every { corestate.localDataStore.readUserEventLog(evt) } returns log

        // Act
        initializeCleverTapAPI()
        val userEventLogActual = cleverTapAPI.getUserEventLog(evt)

        // Assert
        assertSame(log, userEventLogActual)
        verify { corestate.localDataStore.readUserEventLog(evt) }
    }

    @Test
    fun `test getUserEventLogHistory`() {
        // Arrange
        val logs = UserEventLogTestData.EventNames.sampleUserEventLogsForSameDeviceId
        every { corestate.localDataStore.readUserEventLogs() } returns logs

        // Act
        initializeCleverTapAPI()
        val historyActual = cleverTapAPI.userEventLogHistory

        // Assert
        assertEquals(2, historyActual.size)
        assertEquals(logs[0], historyActual.values.elementAt(0))
        assertEquals(logs[1], historyActual.values.elementAt(1))
        verify { corestate.localDataStore.readUserEventLogs() }
    }

    @Test
    fun `test getUserLastVisitTs`() {
        val expectedUserLastVisitTs = UserEventLogTestData.TestTimestamps.SAMPLE_TIMESTAMP
        // Arrange
        every { corestate.sessionManager.userLastVisitTs } returns expectedUserLastVisitTs

        // Act
        initializeCleverTapAPI()
        val lastVisitTsActual = cleverTapAPI.userLastVisitTs

        // Assert
        assertEquals(expectedUserLastVisitTs, lastVisitTsActual)
        verify { corestate.sessionManager.userLastVisitTs }
    }

    @Test
    fun `test getUserAppLaunchCount`() {
        // Arrange
        every { corestate.localDataStore.readUserEventLogCount(Constants.APP_LAUNCHED_EVENT) } returns 5

        // Act
        initializeCleverTapAPI()
        val appLaunchCountActual = cleverTapAPI.userAppLaunchCount

        // Assert
        assertEquals(5, appLaunchCountActual)
        verify { corestate.localDataStore.readUserEventLogCount(Constants.APP_LAUNCHED_EVENT) }
    }

    @After
    fun tearDown() {
        CleverTapAPI.setInstances(null) // clear existing CleverTapAPI instances
    }
}