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
import io.mockk.verifyOrder
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
        CleverTapAPI.setInstances(null) // clear existing CleverTapAPI instances
        corestate = MockCoreStateKotlin(cleverTapInstanceConfig)
        testClock = TestClock()
    }

    @After
    fun tearDown() {
        CleverTapAPI.setInstances(null) // clear existing CleverTapAPI instances
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

    @Test
    fun test_setOptOut_true() {
        // Setup
        initializeCleverTapAPI()

        // generate the data for a profile push to alert the server to the optOut state change
        val expectedMap = hashMapOf<String, Any>().apply {
            put(Constants.CLEVERTAP_OPTOUT, true)
            put(Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS, false)
        }

        // Act
        cleverTapAPI.setOptOut(true)

        // Assert
        verifyOrder {
            corestate.analyticsManager.pushProfile(expectedMap)
            corestate.coreMetaData.isCurrentUserOptedOut = true
            corestate.coreMetaData.enabledSystemEvents = false
            corestate.deviceInfo.saveOptOutState(true)
        }
    }

    @Test
    fun test_setOptOut_false() {
        // Setup
        initializeCleverTapAPI()

        // generate the data for a profile push to alert the server to the optOut state change
        val expectedMap = hashMapOf<String, Any>().apply {
            put(Constants.CLEVERTAP_OPTOUT, false)
            put(Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS, false)
        }

        // Act
        cleverTapAPI.setOptOut(false)

        // Assert
        verifyOrder {
            corestate.coreMetaData.isCurrentUserOptedOut = false
            corestate.coreMetaData.enabledSystemEvents = true
            corestate.analyticsManager.pushProfile(expectedMap)
            corestate.deviceInfo.saveOptOutState(false)
        }
    }

    @Test
    fun test_setOptOut_true_allowSystemEvents_false() {
        // Setup
        initializeCleverTapAPI()

        // generate the data for a profile push to alert the server to the optOut state change
        val expectedMap = hashMapOf<String, Any>().apply {
            put(Constants.CLEVERTAP_OPTOUT, true)
            put(Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS, false)
        }

        // Act
        cleverTapAPI.setOptOut(true, false)

        // Assert
        verifyOrder {
            corestate.coreMetaData.isCurrentUserOptedOut = true
            corestate.coreMetaData.enabledSystemEvents = false
            corestate.analyticsManager.pushProfile(expectedMap)
            corestate.deviceInfo.saveOptOutState(true)
        }
    }

    @Test
    fun test_setOptOut_true_allowSystemEvents_true() {
        // Setup
        initializeCleverTapAPI()

        // generate the data for a profile push to alert the server to the optOut state change
        val expectedMap = hashMapOf<String, Any>().apply {
            put(Constants.CLEVERTAP_OPTOUT, true)
            put(Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS, true)
        }

        // Act
        cleverTapAPI.setOptOut(true, true)

        // Assert
        verifyOrder {
            corestate.coreMetaData.isCurrentUserOptedOut = true
            corestate.coreMetaData.enabledSystemEvents = true
            corestate.analyticsManager.pushProfile(expectedMap)
            corestate.deviceInfo.saveOptOutState(true)
        }
    }

    @Test
    fun test_setOptOut_false_allowSystemEvents_false() {
        // Setup
        initializeCleverTapAPI()

        // generate the data for a profile push to alert the server to the optOut state change
        val expectedMap = hashMapOf<String, Any>().apply {
            put(Constants.CLEVERTAP_OPTOUT, false)
            put(Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS, false)
        }

        // Act
        cleverTapAPI.setOptOut(false, false)

        // Assert
        verifyOrder {
            corestate.coreMetaData.isCurrentUserOptedOut = false
            corestate.coreMetaData.enabledSystemEvents = true
            corestate.analyticsManager.pushProfile(expectedMap)
            corestate.deviceInfo.saveOptOutState(false)
        }
    }

    @Test
    fun test_setOptOut_false_allowSystemEvents_true() {
        // Setup
        initializeCleverTapAPI()

        // generate the data for a profile push to alert the server to the optOut state change
        val expectedMap = hashMapOf<String, Any>().apply {
            put(Constants.CLEVERTAP_OPTOUT, false)
            put(Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS, true)
        }

        // Act
        cleverTapAPI.setOptOut(false, true)

        // Assert
        verifyOrder {
            corestate.coreMetaData.isCurrentUserOptedOut = false
            corestate.coreMetaData.enabledSystemEvents = true
            corestate.analyticsManager.pushProfile(expectedMap)
            corestate.deviceInfo.saveOptOutState(false)
        }
    }

    // =========================
    // EVENT TRACKING TESTS
    // =========================

    @Test
    fun test_pushEvent_withEventNameOnly() {
        // Arrange
        val eventName = "Custom event"

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushEvent(eventName)

        // Assert
        verify {
            corestate.analyticsManager.pushEvent(eventName, null)
        }
    }

    @Test
    fun test_pushEvent_withEventNameAndActions() {
        // Arrange
        val eventName = "custom name"
        val eventData = mapOf<String, Any>(
            "Product Name" to "iPhone 15",
            "Price" to 999.99,
            "Quantity" to 1,
            "Category" to "Electronics"
        )

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushEvent(eventName, eventData)

        // Assert
        verify {
            corestate.analyticsManager.pushEvent(eventName, eventData)
        }
    }

    @Test
    fun test_pushEvent_withInvalidNames() {
        // Arrange
        initializeCleverTapAPI()

        // Act
        cleverTapAPI.pushEvent("")
        cleverTapAPI.pushEvent(null)
        cleverTapAPI.pushEvent("    ")
        cleverTapAPI.pushEvent("  ")

        // Assert
        verify(exactly = 0) {
            corestate.analyticsManager.pushEvent(any(), null)
        }
    }

    @Test
    fun test_pushChargedEvent_withValidData() {
        // Arrange
        val chargeDetails = hashMapOf<String, Any>(
            "Amount" to 299.99,
            "Currency" to "USD",
            "Payment Mode" to "Credit Card",
            "Charged ID" to "order_12345"
        )
        val items = arrayListOf<HashMap<String, Any>>(
            hashMapOf<String, Any>(
                "Product Name" to "Wireless Headphones",
                "Category" to "Electronics",
                "Price" to 149.99,
                "Quantity" to 1
            ),
            hashMapOf<String, Any>(
                "Product Name" to "Phone Case",
                "Category" to "Accessories",
                "Price" to 29.99,
                "Quantity" to 2
            )
        )

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushChargedEvent(chargeDetails, items)

        // Assert
        verify {
            corestate.analyticsManager.pushChargedEvent(chargeDetails, items)
        }
    }

    @Test
    fun test_pushError() {
        // Arrange
        val errorMessage = "Network connection failed"
        val errorCode = 500

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushError(errorMessage, errorCode)

        // Assert
        verify {
            corestate.analyticsManager.pushError(errorMessage, errorCode)
        }
    }

    @Test
    fun test_pushInstallReferrer() {
        // Arrange
        val source = "google"
        val medium = "cpc"
        val campaign = "summer_sale_2024"

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushInstallReferrer(source, medium, campaign)

        // Assert
        verify {
            corestate.analyticsManager.pushInstallReferrer(source, medium, campaign)
        }
    }

    // =========================
    // USER PROFILE MANAGEMENT TESTS
    // =========================

    @Test
    fun test_onUserLogin_withProfileOnly() {
        // Arrange
        val profile = mapOf<String, Any>(
            "Name" to "John Doe",
            "Email" to "john@example.com",
            "Age" to 30
        )

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.onUserLogin(profile)

        // Assert
        verify {
            corestate.loginController.onUserLogin(profile, null)
        }
    }

    @Test
    fun test_onUserLogin_withProfileAndCleverTapID() {
        // Arrange
        val profile = mapOf<String, Any>(
            "Identity" to "user123",
            "Email" to "user@test.com"
        )
        val cleverTapID = "custom_id_12345"

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.onUserLogin(profile, cleverTapID)

        // Assert
        verify {
            corestate.loginController.onUserLogin(profile, cleverTapID)
        }
    }

    @Test
    fun test_onUserLogin_withEmptyProfile() {
        // Arrange
        val profile = emptyMap<String, Any>()

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.onUserLogin(profile)

        // Assert
        verify {
            corestate.loginController.onUserLogin(profile, null)
        }
    }

    @Test
    fun test_pushProfile() {
        // Arrange
        val profile = mapOf<String, Any>(
            "Name" to "Jane Smith",
            "Phone" to "+1234567890",
            "Gender" to "Female",
            "DOB" to "1990-05-15"
        )

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.pushProfile(profile)

        // Assert
        verify {
            corestate.analyticsManager.pushProfile(profile)
        }
    }

    @Test
    fun test_addMultiValueForKey() {
        // Arrange
        val key = "Interests"
        val value = "Technology"

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.addMultiValueForKey(key, value)

        // Assert
        verify {
            corestate.analyticsManager.addMultiValuesForKey(key, arrayListOf(value))
        }
    }

    @Test
    fun test_addMultiValueForKey_withEmptyValue() {
        // Arrange
        val key = "Tags"
        val value = ""

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.addMultiValueForKey(key, value)

        // Assert
        verify {
            corestate.analyticsManager._generateEmptyMultiValueError(key)
        }
    }

    @Test
    fun test_addMultiValueForKey_withNullValue() {
        // Arrange
        val key = "Categories"
        val value: String? = null

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.addMultiValueForKey(key, value)

        // Assert
        verify {
            corestate.analyticsManager._generateEmptyMultiValueError(key)
        }
    }

    @Test
    fun test_addMultiValuesForKey() {
        // Arrange
        val key = "Skills"
        val values = arrayListOf("Java", "Kotlin", "Android", "Mobile Development")

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.addMultiValuesForKey(key, values)

        // Assert
        verify {
            corestate.analyticsManager.addMultiValuesForKey(key, values)
        }
    }

    @Test
    fun test_removeMultiValueForKey() {
        // Arrange
        val key = "Preferences"
        val value = "Email Notifications"

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.removeMultiValueForKey(key, value)

        // Assert
        verify {
            corestate.analyticsManager.removeMultiValuesForKey(key, arrayListOf(value))
        }
    }

    @Test
    fun test_removeMultiValueForKey_withEmptyValue() {
        // Arrange
        val key = "Tags"
        val value = ""

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.removeMultiValueForKey(key, value)

        // Assert
        verify {
            corestate.analyticsManager._generateEmptyMultiValueError(key)
        }
        verify(exactly = 0) {
            corestate.analyticsManager.removeMultiValuesForKey(any(), any())
        }
    }

    @Test
    fun test_removeMultiValueForKey_withNullValue() {
        // Arrange
        val key = "Categories"
        val value: String? = null

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.removeMultiValueForKey(key, value)

        // Assert
        verify {
            corestate.analyticsManager._generateEmptyMultiValueError(key)
        }
        verify(exactly = 0) {
            corestate.analyticsManager.removeMultiValuesForKey(any(), any())
        }
    }

    @Test
    fun test_removeMultiValuesForKey() {
        // Arrange
        val key = "OldInterests"
        val values = arrayListOf("Sports", "Movies", "Music")

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.removeMultiValuesForKey(key, values)

        // Assert
        verify {
            corestate.analyticsManager.removeMultiValuesForKey(key, values)
        }
    }

    @Test
    fun test_setMultiValuesForKey() {
        // Arrange
        val key = "Languages"
        val values = arrayListOf("English", "Spanish", "French")

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.setMultiValuesForKey(key, values)

        // Assert
        verify {
            corestate.analyticsManager.setMultiValuesForKey(key, values)
        }
    }

    @Test
    fun test_incrementValue() {
        // Arrange
        val key = "LoginCount"
        val value = 1

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.incrementValue(key, value)

        // Assert
        verify {
            corestate.analyticsManager.incrementValue(key, value)
        }
    }

    @Test
    fun test_incrementValue_withDouble() {
        // Arrange
        val key = "TotalSpent"
        val value = 25.99

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.incrementValue(key, value)

        // Assert
        verify {
            corestate.analyticsManager.incrementValue(key, value)
        }
    }

    @Test
    fun test_decrementValue() {
        // Arrange
        val key = "Credits"
        val value = 5

        // Act
        initializeCleverTapAPI()
        cleverTapAPI.decrementValue(key, value)

        // Assert
        verify {
            corestate.analyticsManager.decrementValue(key, value)
        }
    }

    @Test
    fun test_getProperty_whenPersonalizationEnabled() {
        // Arrange
        val propertyName = "UserType"
        val expectedValue = "Premium"
        corestate.config.enablePersonalization(true)
        every { corestate.localDataStore.getProfileProperty(propertyName) } returns expectedValue

        // Act
        initializeCleverTapAPI()
        val actualValue = cleverTapAPI.getProperty(propertyName)

        // Assert
        assertEquals(expectedValue, actualValue)
        verify { corestate.localDataStore.getProfileProperty(propertyName) }
    }

    @Test
    fun test_getProperty_whenPersonalizationDisabled() {
        // Arrange
        val propertyName = "UserLevel"
        corestate.config.enablePersonalization(false)

        // Act
        initializeCleverTapAPI()
        val actualValue = cleverTapAPI.getProperty(propertyName)

        // Assert
        assertEquals(null, actualValue)
        verify(exactly = 0) { corestate.localDataStore.getProfileProperty(any()) }
    }
}