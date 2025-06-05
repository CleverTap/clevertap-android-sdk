package com.clevertap.android.sdk

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup.REGULAR
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.login.IdentityRepo
import com.clevertap.android.sdk.login.IdentityRepoFactory
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkInfo
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EventQueueManagerTest : BaseTestCase() {

    private lateinit var corestate: MockCoreStateKotlin

    private lateinit var eventQueueManager: EventQueueManager

    private lateinit var json: JSONObject

    private lateinit var loginInfoProvider: LoginInfoProvider

    @Before
    override fun setUp() {
        super.setUp()
        corestate = MockCoreStateKotlin(cleverTapInstanceConfig)
        loginInfoProvider = mockk(relaxed = true)
        eventQueueManager = spyk(
            EventQueueManager(
                corestate.databaseManager,
                application,
                cleverTapInstanceConfig,
                corestate.eventMediator,
                corestate.sessionManager,
                corestate.callbackManager,
                corestate.mainLooperHandler,
                corestate.deviceInfo,
                corestate.validationResultStack,
                corestate.networkManager as NetworkManager,
                corestate.coreMetaData,
                corestate.ctLockManager,
                corestate.localDataStore,
                corestate.controllerManager,
                loginInfoProvider
            )
        )
            json = JSONObject()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test queueEvent when type is raised event updates local store`() {
        withMockExecutors {
            // Given
            val event = JSONObject()
            event.put("evtName", "test_event")

            every { corestate.eventMediator.getEventName(event) } returns "test_event"

        // When
        eventQueueManager.queueEvent(application, event, Constants.RAISED_EVENT)

            // Then
            verify { corestate.localDataStore.persistUserEventLog("test_event") }
        }
    }

    @Test
    fun `test queueEvent when type is not raised event does not update local store`() {
        withMockExecutors {

            // Given
            val event = JSONObject()
            event.put("evtName", "test_event")
            every { corestate.eventMediator.getEventName(event) } returns "test_event"

        // Test for different event types that are not RAISED_EVENT
        listOf(
            Constants.PROFILE_EVENT,
            Constants.FETCH_EVENT,
            Constants.DATA_EVENT,
            Constants.PING_EVENT,
            Constants.PAGE_EVENT,
            Constants.NV_EVENT
        ).forEach { eventType ->

            // When
            eventQueueManager.queueEvent(application, event, eventType)

                // Then
                verify(exactly = 0) { corestate.localDataStore.persistUserEventLog(any()) }
            }
        }
    }

    @Test
    fun test_queueEvent_will_not_add_to_queue_when_event_should_be_dropped() {
        withMockExecutors {
            every {
                corestate.eventMediator.shouldDropEvent(
                    json, Constants.PING_EVENT
                )
            } returns true
            eventQueueManager.queueEvent(application, json, Constants.PING_EVENT)
            verify(exactly = 0) {
                eventQueueManager.addToQueue(
                    application, json, Constants.PING_EVENT
                )
            }
        }
    }

    @Test
    fun test_queueEvent_will_add_to_queue_when_event_should_not_be_dropped() {
        withMockExecutors {
            every {
                corestate.eventMediator.shouldDropEvent(
                    json, Constants.FETCH_EVENT
                )
            } returns false
            every {
                eventQueueManager.addToQueue(
                    application, json, Constants.FETCH_EVENT
                )
            } just runs
            eventQueueManager.queueEvent(application, json, Constants.FETCH_EVENT)
            verify { eventQueueManager.addToQueue(application, json, Constants.FETCH_EVENT) }
        }
    }

    @Test
    fun test_queueEvent_will_process_further_and_add_to_queue_when_event_should_not_be_dropped() {
        withMockExecutors {
            every {
                corestate.eventMediator.shouldDropEvent(
                    json, Constants.PING_EVENT
                )
            } returns false
            every {
                eventQueueManager.addToQueue(
                    application, json, Constants.PING_EVENT
                )
            } just runs
            every { eventQueueManager.pushInitialEventsAsync() } just runs
            every { corestate.sessionManager.lazyCreateSession(application) } just runs

        eventQueueManager.queueEvent(application, json, Constants.PING_EVENT)

            verify { corestate.sessionManager.lazyCreateSession(application) }
            verify { eventQueueManager.pushInitialEventsAsync() }
            verify { eventQueueManager.addToQueue(application, json, Constants.PING_EVENT) }
        }
    }

    @Test
    fun test_queueEvent_will_delay_add_to_queue_when_event_processing_should_be_delayed() {
        withMockExecutors {

            every {
                corestate.eventMediator.shouldDropEvent(
                    json, Constants.PING_EVENT
                )
            } returns false

            every {
                corestate.eventMediator.shouldDeferProcessingEvent(
                    json, Constants.PING_EVENT
                )
            } returns true
            every {
                eventQueueManager.addToQueue(
                    application, json, Constants.PING_EVENT
                )
            } just runs
            every { eventQueueManager.pushInitialEventsAsync() } just runs
            every { corestate.sessionManager.lazyCreateSession(application) } just runs

        eventQueueManager.queueEvent(application, json, Constants.PING_EVENT)

            val runnableSlot = slot<Runnable>()
            verify { corestate.mainLooperHandler.postDelayed(capture(runnableSlot), any()) }

            runnableSlot.captured.run()

            verify { corestate.sessionManager.lazyCreateSession(application) }
            verify { eventQueueManager.pushInitialEventsAsync() }
            verify { eventQueueManager.addToQueue(application, json, Constants.PING_EVENT) }
        }
    }

    @Test
    fun test_queueEvent_for_profileEvent_will_process_further_and_add_to_queue_when_event_should_not_be_dropped() {
        withMockExecutors {
            val mockInAppController = mockk<InAppController>(relaxed = true)
            every {
                corestate.eventMediator.shouldDropEvent(
                    json, Constants.PROFILE_EVENT
                )
            } returns false

            every {
                corestate.eventMediator.shouldDropEvent(
                    json, Constants.PROFILE_EVENT
                )
            } returns false

            every { corestate.controllerManager.inAppController } returns mockInAppController

            every { eventQueueManager.pushInitialEventsAsync() } just runs
            every { corestate.sessionManager.lazyCreateSession(application) } just runs

            eventQueueManager.queueEvent(application, json, Constants.PROFILE_EVENT)

            verify { mockInAppController.onQueueProfileEvent(any(), any()) }
            verify { corestate.sessionManager.lazyCreateSession(application) }
            verify { eventQueueManager.pushInitialEventsAsync() }
            verify { eventQueueManager.addToQueue(application, json, Constants.PROFILE_EVENT) }
        }
    }

    @Test
    fun test_addToQueue_when_event_is_notification_viewed() {
        withMockExecutors {
            every {
                eventQueueManager.processPushNotificationViewedEvent(
                    application, json, Constants.NV_EVENT
                )
            } just runs

        eventQueueManager.addToQueue(application, json, Constants.NV_EVENT)

            verify {
                eventQueueManager.processPushNotificationViewedEvent(
                    application, json, Constants.NV_EVENT
                )
            }
            verify(exactly = 0) {
                eventQueueManager.processEvent(
                    application, json, Constants.NV_EVENT
                )
            }
        }
    }

    @Test
    fun test_addToQueue_when_event_is_not_notification_viewed() {
        withMockExecutors {
            every {
                eventQueueManager.processEvent(
                    application, json, Constants.PROFILE_EVENT
                )
            } just runs

        eventQueueManager.addToQueue(application, json, Constants.PROFILE_EVENT)

            verify(exactly = 0) {
                eventQueueManager.processPushNotificationViewedEvent(
                    application, json, Constants.PROFILE_EVENT
                )
            }
            verify { eventQueueManager.processEvent(application, json, Constants.PROFILE_EVENT) }
        }
    }

    @Test
    fun test_processPushNotificationViewedEvent_when_there_is_no_validation_error() {
        withMockExecutors {
            val runnableSlot = slot<Runnable>()
            corestate.coreMetaData.currentSessionId = 1000
            every { eventQueueManager.now } returns 7000
            every {
                eventQueueManager.flushQueueAsync(
                    application, PUSH_NOTIFICATION_VIEWED
                )
            } just runs
            every {
                eventQueueManager.initInAppEvaluation(
                    application, json, Constants.PROFILE_EVENT
                )
            } just runs

        eventQueueManager.processPushNotificationViewedEvent(application, json, Constants.PROFILE_EVENT)

        assertNull(json.optJSONObject(Constants.ERROR_KEY))
        assertEquals("event", json.getString("type"))
        assertEquals(1000, json.getInt("s"))
        assertEquals(7000, json.getInt("ep"))

            verify {
                corestate.databaseManager.queuePushNotificationViewedEventToDB(
                    application, json
                )
            }
            verify { corestate.mainLooperHandler.removeCallbacks(capture(runnableSlot)) }
            verify { corestate.mainLooperHandler.post(capture(runnableSlot)) }

            runnableSlot.captured.run()

            verify { eventQueueManager.flushQueueAsync(application, PUSH_NOTIFICATION_VIEWED) }
        }
    }

    @Test
    fun test_processPushNotificationViewedEvent_when_there_is_validation_error() {
        withMockExecutors {

            // Arrange
            val validationResult = ValidationResult()
            validationResult.errorDesc = "Fire in the hall"
            validationResult.errorCode = 999

        corestate.validationResultStack.pushValidationResult(validationResult)

            val runnableSlot = slot<Runnable>()
            corestate.coreMetaData.currentSessionId = 1000
            every { eventQueueManager.now } returns 7000
            every {
                eventQueueManager.flushQueueAsync(
                    application, PUSH_NOTIFICATION_VIEWED
                )
            } just runs
            every {
                eventQueueManager.initInAppEvaluation(
                    application, json, Constants.PROFILE_EVENT
                )
            } just runs

        // Act
        eventQueueManager.processPushNotificationViewedEvent(application, json, Constants.PROFILE_EVENT)

        // Assert
        assertEquals(validationResult.errorCode, json.getJSONObject(Constants.ERROR_KEY)["c"])
        assertEquals(validationResult.errorDesc, json.getJSONObject(Constants.ERROR_KEY)["d"])
        assertEquals("event", json.getString("type"))
        assertEquals(1000, json.getInt("s"))
        assertEquals(7000, json.getInt("ep"))

            verify {
                corestate.databaseManager.queuePushNotificationViewedEventToDB(
                    application, json
                )
            }
            verify { corestate.mainLooperHandler.removeCallbacks(capture(runnableSlot)) }
            verify { corestate.mainLooperHandler.post(capture(runnableSlot)) }

            runnableSlot.captured.run()

            verify { eventQueueManager.flushQueueAsync(application, PUSH_NOTIFICATION_VIEWED) }
        }
    }

    @Test
    fun test_pushInitialEventsAsync_does_not_pushBasicProfile_when_inCurrentSession() {
        withMockExecutors {

            corestate.coreMetaData.currentSessionId = 10000
            every { eventQueueManager.pushBasicProfile(null, false) } just runs

        eventQueueManager.pushInitialEventsAsync()

            verify(exactly = 0) { eventQueueManager.pushBasicProfile(null, false) }
        }
    }

    @Test
    fun test_pushInitialEventsAsync_pushesBasicProfile_when_not_inCurrentSession() {
        withMockExecutors {

            corestate.coreMetaData.currentSessionId = -1
            every { eventQueueManager.pushBasicProfile(null, false) } just runs

        eventQueueManager.pushInitialEventsAsync()

            verify { eventQueueManager.pushBasicProfile(null, false) }
        }
    }

    @Test
    fun test_scheduleQueueFlush() {
        withMockExecutors {

            // Arrange
            val runnableSlot = slot<Runnable>()
            every { eventQueueManager.flushQueueSync(any(), any()) } just runs

        eventQueueManager.scheduleQueueFlush(application)

            // Assert
            verify { corestate.mainLooperHandler.removeCallbacks(capture(runnableSlot)) }
            verify { corestate.mainLooperHandler.postDelayed(capture(runnableSlot), any()) }

            runnableSlot.captured.run()

            verify { eventQueueManager.flushQueueSync(application, REGULAR) }
            verify { eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED) }
        }
    }

    @Test
    fun test_flush() {
        withMockExecutors {

            every { eventQueueManager.flushQueueAsync(application, REGULAR) } just runs

        eventQueueManager.flush()

            verify { eventQueueManager.flushQueueAsync(application, REGULAR) }
        }
    }

    @Test
    fun test_flushQueueSync_when_net_is_offline() {
        withMockExecutors {

            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            shadowOfCM.setActiveNetworkInfo(null) // make offline

        eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify(exactly = 0) {
                corestate.networkManager.needsHandshakeForDomain(
                    PUSH_NOTIFICATION_VIEWED
                )
            }
        }
    }

    @Test
    fun test_flushQueueSync_when_net_is_online_and_metadata_is_offline() {
        withMockExecutors {

            corestate.coreMetaData.isOffline = true
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            val netInfo =
                ShadowNetworkInfo.newInstance(DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 1, true, true)
            shadowOfCM.setActiveNetworkInfo(netInfo) // make offline

        eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify(exactly = 0) {
                corestate.networkManager.needsHandshakeForDomain(
                    PUSH_NOTIFICATION_VIEWED
                )
            }
        }
    }

    @Test
    fun test_flushQueueSync_when_HandshakeForDomain_not_needed() {
        withMockExecutors {

            corestate.coreMetaData.isOffline = false
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            val netInfo =
                ShadowNetworkInfo.newInstance(DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 1, true, true)
            shadowOfCM.setActiveNetworkInfo(netInfo) // make offline

        eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify(exactly = 0) { corestate.networkManager.initHandshake(any(), any()) }
            verify {
                corestate.networkManager.flushDBQueue(
                    application, PUSH_NOTIFICATION_VIEWED, null
                )
            }
        }
    }

    @Test
    fun test_flushQueueSync_when_HandshakeForDomain_is_needed() {
        withMockExecutors {

            val runnableSlot = slot<Runnable>()
            corestate.coreMetaData.isOffline = false
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            val netInfo =
                ShadowNetworkInfo.newInstance(DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 1, true, true)
            shadowOfCM.setActiveNetworkInfo(netInfo) // make offline
            every { corestate.networkManager.needsHandshakeForDomain(PUSH_NOTIFICATION_VIEWED) } returns true

        eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify { corestate.networkManager.initHandshake(any(), capture(runnableSlot)) }

            runnableSlot.captured.run()

            verify {
                corestate.networkManager.flushDBQueue(
                    application, PUSH_NOTIFICATION_VIEWED, null
                )
            }
        }
    }

    @Test
    fun test_pushBasicProfile_when_input_is_null() {
        withMockExecutors {

            // Arrange
            every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedDeviceId = "device_12345"
        val expectedDeviceCarrier = "carrier_12345"
        val expectedDeviceCC = "CC_12345"
        val expectedDeviceTZ = "tz_12345"

        val expectedTZ = TimeZone.getDefault()
        expectedTZ.id = expectedDeviceTZ
        TimeZone.setDefault(expectedTZ)

            every { corestate.deviceInfo.deviceID } returns expectedDeviceId
            every { corestate.deviceInfo.carrier } returns expectedDeviceCarrier
            every { corestate.deviceInfo.countryCode } returns expectedDeviceCC

            val jsonSlot = slot<JSONObject>()
            val eventTypeSlot = slot<Int>()

        // Act
        eventQueueManager.pushBasicProfile(null, false)

            // Assert
            verify {
                eventQueueManager.queueEvent(
                    any(), capture(jsonSlot), capture(eventTypeSlot)
                )
            }
            val actualProfile = jsonSlot.captured["profile"] as JSONObject
            assertEquals(expectedDeviceCarrier, actualProfile["Carrier"])
            assertEquals(expectedDeviceCC, actualProfile["cc"])
            assertEquals(expectedDeviceTZ, actualProfile["tz"])
        }
    }

    @Test
    fun test_pushBasicProfile_when_carrier_or_cc_is_null() {
        withMockExecutors {

            // Arrange
            every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedDeviceId = "device_12345"

            every { corestate.deviceInfo.deviceID } returns expectedDeviceId
            every { corestate.deviceInfo.carrier } returns null
            every { corestate.deviceInfo.countryCode } returns null

            val jsonSlot = slot<JSONObject>()
            val eventTypeSlot = slot<Int>()

        // Act
        eventQueueManager.pushBasicProfile(null, false)

            // Assert
            verify {
                eventQueueManager.queueEvent(
                    any(), capture(jsonSlot), capture(eventTypeSlot)
                )
            }
            val actualProfile = jsonSlot.captured["profile"] as JSONObject
            assertNull(actualProfile.optString("Carrier", null))
            assertNull(actualProfile.optString("cc", null))
        }
    }

    @Test
    fun test_pushBasicProfile_when_carrier_or_cc_is_blank() {
        withMockExecutors {

            // Arrange
            every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedDeviceId = "device_12345"

            every { corestate.deviceInfo.deviceID } returns expectedDeviceId
            every { corestate.deviceInfo.carrier } returns ""
            every { corestate.deviceInfo.countryCode } returns ""

            val jsonSlot = slot<JSONObject>()
            val eventTypeSlot = slot<Int>()

        // Act
        eventQueueManager.pushBasicProfile(null, false)

            // Assert
            verify {
                eventQueueManager.queueEvent(
                    any(), capture(jsonSlot), capture(eventTypeSlot)
                )
            }
            val actualProfile = jsonSlot.captured["profile"] as JSONObject
            assertNull(actualProfile.optString("Carrier", null))
            assertNull(actualProfile.optString("cc", null))
        }
    }

    @Test
    fun test_pushBasicProfile_when_input_is_not_null() {
        withMockExecutors {

            mockkStatic(IdentityRepoFactory::class) {
                val mockIdentityRepo = mockk<IdentityRepo>(relaxed = true)
                every {
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.validationResultStack
                    )
                } returns mockIdentityRepo

                // Arrange
                every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedDeviceId = "device_12345"

                every { corestate.deviceInfo.deviceID } returns expectedDeviceId

                val jsonSlot = slot<JSONObject>()
                val eventTypeSlot = slot<Int>()

        val inputJson = JSONObject()
        val subInputJson = JSONObject()
        subInputJson.put("age", 70)
        inputJson.put("name", "abc")
        inputJson.put("details", subInputJson)

        // Act
        eventQueueManager.pushBasicProfile(inputJson, false)

                // Assert
                verify {
                    eventQueueManager.queueEvent(
                        any(), capture(jsonSlot), capture(eventTypeSlot)
                    )
                }
                val actualProfile = jsonSlot.captured["profile"] as JSONObject
                val actualDetails = actualProfile["details"] as JSONObject

        assertEquals("abc", actualProfile["name"])
        assertEquals(70, actualDetails["age"])

        unmockkStatic(IdentityRepoFactory::class)
    }

    @Test
    fun test_pushBasicProfile_when_key_is_profile_identifier() {
        withMockExecutors {

            mockkStatic(IdentityRepoFactory::class) {
                val mockIdentityRepo = mockk<IdentityRepo>(relaxed = true)
                every {
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.validationResultStack
                    )
                } returns mockIdentityRepo

                // Arrange
                every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedDeviceId = "device_12345"

                every { corestate.deviceInfo.deviceID } returns expectedDeviceId

                val jsonSlot = slot<JSONObject>()
                val eventTypeSlot = slot<Int>()

        val inputJson = JSONObject()
        val subInputJson = JSONObject()
        subInputJson.put("age", 70)
        inputJson.put("name", "abc")
        inputJson.put("details", subInputJson)

        every { mockIdentityRepo.hasIdentity("name") } returns true

        // Act
        eventQueueManager.pushBasicProfile(inputJson, false)

                // Assert
                verify { loginInfoProvider.cacheGUIDForIdentifier(expectedDeviceId, "name", "abc") }
                verify {
                    eventQueueManager.queueEvent(
                        any(), capture(jsonSlot), capture(eventTypeSlot)
                    )
                }
                val actualProfile = jsonSlot.captured["profile"] as JSONObject
                val actualDetails = actualProfile["details"] as JSONObject

        assertEquals("abc", actualProfile["name"])
        assertEquals(70, actualDetails["age"])

        unmockkStatic(IdentityRepoFactory::class)
    }

    @Test
    fun test_pushBasicProfile_when_key_is_profile_identifier_and_removeFromSharedPrefs_is_true() {
        withMockExecutors {

            mockkStatic(IdentityRepoFactory::class) {
                val mockIdentityRepo = mockk<IdentityRepo>()
                every {
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.validationResultStack
                    )
                } returns mockIdentityRepo

                //Arrange
                every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedDeviceID = "_12345789"
        val expectedDeviceCarrier = "Android"
        val expectedDeviceCC = "us"
        val expectedDeviceTZ = "Asia/Kolkata"
        val expectedTZ = TimeZone.getDefault()
        expectedTZ.id = expectedDeviceTZ
        TimeZone.setDefault(expectedTZ)

                every { corestate.deviceInfo.deviceID } returns expectedDeviceID
                every { corestate.deviceInfo.carrier } returns expectedDeviceCarrier
                every { corestate.deviceInfo.countryCode } returns expectedDeviceCC
                every { corestate.deviceInfo.isErrorDeviceId() } returns false

                val jsonSlot = slot<JSONObject>()
                val eventTypeSlot = slot<Int>()

        val inputJson = JSONObject()
        inputJson.put("Email", "abc@xyz.com")

        every { mockIdentityRepo.hasIdentity("Email") } returns true

        //Act
        eventQueueManager.pushBasicProfile(inputJson, true)

                //Assert
                verify {
                    loginInfoProvider.removeValueFromCachedGUIDForIdentifier(
                    expectedDeviceID,
                    "Email"
                    )
                }
                verify {
                    eventQueueManager.queueEvent(
                        any(), capture(jsonSlot), capture(eventTypeSlot)
                    )
                }

        val actualProfile = jsonSlot.captured["profile"] as JSONObject
        assertEquals("abc@xyz.com", actualProfile["Email"])
        assertEquals(expectedDeviceCarrier, actualProfile["Carrier"])
        assertEquals(expectedDeviceCC, actualProfile["cc"])
        assertEquals(expectedDeviceTZ, actualProfile["tz"])

        unmockkStatic(IdentityRepoFactory::class)
    }

    @Test
    fun test_pushBasicProfile_when_key_is_not_profile_identifier_and_removeFromSharedPrefs_is_false() {
        withMockExecutors {

            mockkStatic(IdentityRepoFactory::class) {
                val mockIdentityRepo = mockk<IdentityRepo>()
                every {
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.validationResultStack
                    )
                } returns mockIdentityRepo

                //Arrange
                every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedGUID = "_123456789"
        val expectedDeviceCarrier = "Android"
        val expectedDeviceCC = "us"
        val expectedDeviceTZ = "Asia/Kolkata"
        val expectedTZ = TimeZone.getDefault()
        expectedTZ.id = expectedDeviceTZ
        TimeZone.setDefault(expectedTZ)

                every { corestate.deviceInfo.deviceID } returns expectedGUID
                every { corestate.deviceInfo.carrier } returns expectedDeviceCarrier
                every { corestate.deviceInfo.countryCode } returns expectedDeviceCC

                val jsonSlot = slot<JSONObject>()
                val eventTypeSlot = slot<Int>()

        val inputJson = JSONObject()
        inputJson.put("Phone", "+919998988767")

        every { mockIdentityRepo.hasIdentity("Phone") } returns false

        //Act
        eventQueueManager.pushBasicProfile(inputJson, false)

                //Assert
                verify {
                    eventQueueManager.queueEvent(
                        any(), capture(jsonSlot), capture(eventTypeSlot)
                    )
                }

        val actualProfile = jsonSlot.captured["profile"] as JSONObject
        assertEquals("+919998988767", actualProfile["Phone"])
        assertEquals(expectedDeviceCarrier, actualProfile["Carrier"])
        assertEquals(expectedDeviceCC, actualProfile["cc"])
        assertEquals(expectedDeviceTZ, actualProfile["tz"])

        unmockkStatic(IdentityRepoFactory::class)
    }

    @Test
    fun test_pushBasicProfile_when_key_is_profile_identifier_and_removeFromSharedPrefs_is_false() {
        withMockExecutors {

            mockkStatic(IdentityRepoFactory::class) {
                val mockIdentityRepo = mockk<IdentityRepo>()
                every {
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.validationResultStack
                    )
                } returns mockIdentityRepo

                //Arrange
                every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedDeviceID = "_12345789"
        val expectedDeviceCarrier = "Android"
        val expectedDeviceCC = "us"
        val expectedDeviceTZ = "Asia/Kolkata"
        val expectedTZ = TimeZone.getDefault()
        expectedTZ.id = expectedDeviceTZ
        TimeZone.setDefault(expectedTZ)

                every { corestate.deviceInfo.deviceID } returns expectedDeviceID
                every { corestate.deviceInfo.carrier } returns expectedDeviceCarrier
                every { corestate.deviceInfo.countryCode } returns expectedDeviceCC

                val jsonSlot = slot<JSONObject>()
                val eventTypeSlot = slot<Int>()

        val inputJson = JSONObject()
        inputJson.put("Email", "abc@xyz.com")

        every { mockIdentityRepo.hasIdentity("Email") } returns true

        //Act
        eventQueueManager.pushBasicProfile(inputJson, false)

                //Assert
                verify {
                    loginInfoProvider.cacheGUIDForIdentifier(
                        expectedDeviceID, "Email", "abc@xyz.com"
                    )
                }
                verify {
                    eventQueueManager.queueEvent(
                        any(), capture(jsonSlot), capture(eventTypeSlot)
                    )
                }

        val actualProfile = jsonSlot.captured["profile"] as JSONObject
        assertEquals("abc@xyz.com", actualProfile["Email"])
        assertEquals(expectedDeviceCarrier, actualProfile["Carrier"])
        assertEquals(expectedDeviceCC, actualProfile["cc"])
        assertEquals(expectedDeviceTZ, actualProfile["tz"])

        unmockkStatic(IdentityRepoFactory::class)
    }

    @Test
    fun test_pushBasicProfile_when_key_is_not_profile_identifier_and_removeFromSharedPrefs_is_true() {
        withMockExecutors {

            mockkStatic(IdentityRepoFactory::class) {
                val mockIdentityRepo = mockk<IdentityRepo>()
                every {
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.validationResultStack
                    )
                } returns mockIdentityRepo

                //Arrange
                every { eventQueueManager.queueEvent(any(), any(), any()) } returns null

        val expectedGUID = "_123456789"
        val expectedDeviceCarrier = "Android"
        val expectedDeviceCC = "us"
        val expectedDeviceTZ = "Asia/Kolkata"
        val expectedTZ = TimeZone.getDefault()
        expectedTZ.id = expectedDeviceTZ
        TimeZone.setDefault(expectedTZ)

                every { corestate.deviceInfo.deviceID } returns expectedGUID
                every { corestate.deviceInfo.carrier } returns expectedDeviceCarrier
                every { corestate.deviceInfo.countryCode } returns expectedDeviceCC

                val jsonSlot = slot<JSONObject>()
                val eventTypeSlot = slot<Int>()

        val inputJson = JSONObject()
        inputJson.put("Phone", "+919998988767")

        every { mockIdentityRepo.hasIdentity("Phone") } returns false

        //Act
        eventQueueManager.pushBasicProfile(inputJson, true)

                //Assert
                verify {
                    eventQueueManager.queueEvent(
                        any(), capture(jsonSlot), capture(eventTypeSlot)
                    )
                }

        val actualProfile = jsonSlot.captured["profile"] as JSONObject
        assertEquals("+919998988767", actualProfile["Phone"])
        assertEquals(expectedDeviceCarrier, actualProfile["Carrier"])
        assertEquals(expectedDeviceCC, actualProfile["cc"])
        assertEquals(expectedDeviceTZ, actualProfile["tz"])

        unmockkStatic(IdentityRepoFactory::class)
    }

    @Test
    fun test_processEvent_when_type_is_page_event() {
        withMockExecutors {

            // Arrange
            val expectedScreenName = "Home Page"
            val expectedSessionId = 9898
            val expectedEpoch = 5000
            val expectedIsFirstSession = true
            val expectedLastSessionLength = 3600
            val expectedGeofenceSDKVersion = 10

        corestate.coreMetaData.setCurrentScreenName(expectedScreenName)
        corestate.coreMetaData.currentSessionId = expectedSessionId
        corestate.coreMetaData.isFirstSession = expectedIsFirstSession
        corestate.coreMetaData.lastSessionLength = expectedLastSessionLength
        corestate.coreMetaData.geofenceSDKVersion = expectedGeofenceSDKVersion
        corestate.coreMetaData.isLocationForGeofence = true
        CoreMetaData.setActivityCount(0)
        val actualEvent = JSONObject()

            every { eventQueueManager.now } returns expectedEpoch
            every { eventQueueManager.scheduleQueueFlush(application) } just runs

        // Act
        eventQueueManager.processEvent(application, actualEvent, Constants.PAGE_EVENT)

        // Assert
        // assert following mappings are present in json
        assertEquals(1, CoreMetaData.getActivityCount())
        assertEquals(expectedScreenName, actualEvent["n"])
        assertEquals(expectedSessionId, actualEvent["s"])
        assertEquals(1, actualEvent["pg"])
        assertEquals("page", actualEvent["type"])
        assertEquals(expectedEpoch, actualEvent["ep"])
        assertEquals(expectedIsFirstSession, actualEvent["f"])
        assertEquals(expectedLastSessionLength, actualEvent["lsl"])

        // assert following values are not modified
        assertEquals(expectedGeofenceSDKVersion, corestate.coreMetaData.geofenceSDKVersion)
        assertFalse(corestate.coreMetaData.isBgPing)
        assertTrue(corestate.coreMetaData.isLocationForGeofence)

        // assert following keys are absent in json
        assertNull(actualEvent.opt(Constants.ERROR_KEY))
        assertNull(actualEvent.opt("pai"))
        assertNull(actualEvent.opt("mc"))
        assertNull(actualEvent.opt("nt"))
        assertNull(actualEvent.opt("gf"))
        assertNull(actualEvent.opt("gfSDKVersion"))

        // assert following methods called
        verify { corestate.localDataStore.setDataSyncFlag(actualEvent) }
        verify { corestate.databaseManager.queueEventToDB(
                    application, actualEvent, Constants.PAGE_EVENT
                ) }
        verify(exactly = 0) { corestate.localDataStore.persistEvent(
                    application, actualEvent, Constants.PAGE_EVENT
                ) }
        verify { eventQueueManager.scheduleQueueFlush(application)}
        }
    }

    @Test
    fun test_processEvent_when_type_is_ping_event() {
        withMockExecutors {

            mockkStatic(Utils::class) {
                // Arrange
                val validationResult = ValidationResult()
                validationResult.errorDesc = "Fire in the hall"
                validationResult.errorCode = 999

        corestate.validationResultStack.pushValidationResult(validationResult)

        val expectedSessionId = 9898
        val expectedEpoch = 5000
        val expectedIsFirstSession = true
        val expectedLastSessionLength = 3600
        val expectedGeofenceSDKVersion = 0
        val expectedMemoryConsumption = 100000L
        val expectedNetworkType = "4G"
        val expectedActivityCount = 10

        corestate.coreMetaData.currentSessionId = expectedSessionId
        corestate.coreMetaData.isFirstSession = expectedIsFirstSession
        corestate.coreMetaData.lastSessionLength = expectedLastSessionLength
        corestate.coreMetaData.geofenceSDKVersion = expectedGeofenceSDKVersion
        corestate.coreMetaData.isLocationForGeofence = true
        CoreMetaData.setActivityCount(expectedActivityCount)

                every { Utils.getMemoryConsumption() } returns expectedMemoryConsumption
                every { Utils.getCurrentNetworkType(application) } returns expectedNetworkType
                every { eventQueueManager.now } returns expectedEpoch
                every { eventQueueManager.scheduleQueueFlush(application) } just runs

        val actualEvent = JSONObject()
        actualEvent.put("bk", 1)

                every { eventQueueManager.now } returns expectedEpoch
                every { eventQueueManager.scheduleQueueFlush(application) } just runs

                // Act
                eventQueueManager.processEvent(application, actualEvent, Constants.PING_EVENT)

        // Assert
        // assert mapping are as expected
        assertEquals(expectedMemoryConsumption, actualEvent["mc"])
        assertEquals(expectedNetworkType, actualEvent["nt"])
        assertTrue(corestate.coreMetaData.isBgPing)

        // assert below mapping is removed
        assertNull(actualEvent.opt("bk"))

        // assert following keys are absent in json
        assertNull(actualEvent.opt("n"))

        // assert validation error present
        assertEquals(validationResult.errorCode, actualEvent.getJSONObject(Constants.ERROR_KEY)["c"])
        assertEquals(validationResult.errorDesc, actualEvent.getJSONObject(Constants.ERROR_KEY)["d"])

        // ---------

        // assert following mappings are present in json
        assertEquals(expectedActivityCount, CoreMetaData.getActivityCount())
        assertEquals(expectedSessionId, actualEvent["s"])
        assertEquals(expectedActivityCount, actualEvent["pg"])
        assertEquals("ping", actualEvent["type"])
        assertEquals(expectedEpoch, actualEvent["ep"])
        assertEquals(expectedIsFirstSession, actualEvent["f"])
        assertEquals(expectedLastSessionLength, actualEvent["lsl"])
        assertEquals(true, actualEvent["gf"])
        assertEquals(expectedGeofenceSDKVersion, actualEvent["gfSDKVersion"])

        // assert following values are modified
        assertEquals(expectedGeofenceSDKVersion, corestate.coreMetaData.geofenceSDKVersion)
        assertFalse(corestate.coreMetaData.isLocationForGeofence)

        // assert following keys are absent in json
        assertNull(actualEvent.opt("pai"))

        // assert following methods called
        verify { corestate.localDataStore.setDataSyncFlag(actualEvent) }
        verify { corestate.databaseManager.queueEventToDB(
                        application, actualEvent, Constants.PING_EVENT
                    ) }
        verify(exactly = 0) { corestate.localDataStore.persistEvent(
                        application, actualEvent, Constants.PING_EVENT
                    ) }
        verify { eventQueueManager.scheduleQueueFlush(application) }

        unmockkStatic(Utils::class)
    }

    @Test
    fun test_processEvent_when_type_is_profile_event() {
        withMockExecutors {

            val actualEvent = JSONObject()

        eventQueueManager.processEvent(application, actualEvent, Constants.PROFILE_EVENT)

        assertEquals("profile", actualEvent["type"])
        // assert following keys are absent in json
        assertNull(actualEvent.opt("pai"))
    }

    @Test
    fun test_processEvent_when_type_is_data_event() {
        withMockExecutors {

            val actualEvent = JSONObject()

        eventQueueManager.processEvent(application, actualEvent, Constants.DATA_EVENT)

        assertEquals("data", actualEvent["type"])
        // assert following keys are absent in json
        assertNull(actualEvent.opt("pai"))
    }

    @Test
    fun test_processEvent_when_type_is_normal_event() {
        withMockExecutors {

            val actualEvent = JSONObject()
            actualEvent.put("evtName", Constants.APP_LAUNCHED_EVENT)

        eventQueueManager.processEvent(application, actualEvent, Constants.RAISED_EVENT)

        // assert following keys are present in json
        assertEquals("event", actualEvent["type"])
        assertNotNull(actualEvent.opt("pai"))
    }

    private fun withMockExecutors(block: () -> Unit) {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            block()
        }
    }
}
