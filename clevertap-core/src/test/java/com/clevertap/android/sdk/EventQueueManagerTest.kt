package com.clevertap.android.sdk

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo.DetailedState
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup.REGULAR
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.login.IdentityRepo
import com.clevertap.android.sdk.login.IdentityRepoFactory
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
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

    private lateinit var corestate: MockCoreState
    private lateinit var eventQueueManager: EventQueueManager
    private lateinit var json: JSONObject

    @Before
    override fun setUp() {
        super.setUp()
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            corestate = MockCoreState(application, cleverTapInstanceConfig)
            eventQueueManager =
                spy(
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
                        corestate.localDataStore
                    )
                )
            json = JSONObject()
        }
    }

    @Test
    fun test_queueEvent_will_not_add_to_queue_when_event_should_be_dropped() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            `when`(corestate.eventMediator.shouldDropEvent(json, Constants.PING_EVENT))
                .thenReturn(true)
            eventQueueManager.queueEvent(application, json, Constants.PING_EVENT)
            verify(eventQueueManager, never()).addToQueue(application, json, Constants.PING_EVENT)
        }
    }

    @Test
    fun test_queueEvent_will_add_to_queue_when_event_should_not_be_dropped() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            `when`(corestate.eventMediator.shouldDropEvent(json, Constants.FETCH_EVENT))
                .thenReturn(false)
            doNothing().`when`(eventQueueManager).addToQueue(application, json, Constants.FETCH_EVENT)
            eventQueueManager.queueEvent(application, json, Constants.FETCH_EVENT)
            verify(eventQueueManager).addToQueue(application, json, Constants.FETCH_EVENT)
        }
    }

    @Test
    fun test_queueEvent_will_process_further_and_add_to_queue_when_event_should_not_be_dropped() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            `when`(corestate.eventMediator.shouldDropEvent(json, Constants.PROFILE_EVENT))
                .thenReturn(false)
            doNothing().`when`(eventQueueManager).addToQueue(application, json, Constants.PROFILE_EVENT)
            doNothing().`when`(eventQueueManager).pushInitialEventsAsync()
            doNothing().`when`(corestate.sessionManager).lazyCreateSession(application)

            eventQueueManager.queueEvent(application, json, Constants.PROFILE_EVENT)

            verify(corestate.sessionManager).lazyCreateSession(application)
            verify(eventQueueManager).pushInitialEventsAsync()
            verify(eventQueueManager).addToQueue(application, json, Constants.PROFILE_EVENT)
        }
    }

    @Test
    fun test_queueEvent_will_delay_add_to_queue_when_event_processing_should_be_delayed() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val captor = ArgumentCaptor.forClass(Runnable::class.java)
            `when`(corestate.eventMediator.shouldDropEvent(json, Constants.PROFILE_EVENT))
                .thenReturn(false)

            `when`(corestate.eventMediator.shouldDeferProcessingEvent(json, Constants.PROFILE_EVENT))
                .thenReturn(true)
            doNothing().`when`(eventQueueManager).addToQueue(application, json, Constants.PROFILE_EVENT)
            doNothing().`when`(eventQueueManager).pushInitialEventsAsync()
            doNothing().`when`(corestate.sessionManager).lazyCreateSession(application)

            eventQueueManager.queueEvent(application, json, Constants.PROFILE_EVENT)

            verify(corestate.mainLooperHandler).postDelayed(captor.capture(), ArgumentMatchers.anyLong())

            captor.value.run()

            verify(corestate.sessionManager).lazyCreateSession(application)
            verify(eventQueueManager).pushInitialEventsAsync()
            verify(eventQueueManager).addToQueue(application, json, Constants.PROFILE_EVENT)
        }
    }

    @Test
    fun test_addToQueue_when_event_is_notification_viewed() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            doNothing().`when`(eventQueueManager).processPushNotificationViewedEvent(application, json)

            eventQueueManager.addToQueue(application, json, Constants.NV_EVENT)

            verify(eventQueueManager).processPushNotificationViewedEvent(application, json)
            verify(eventQueueManager, never()).processEvent(application, json, Constants.NV_EVENT)
        }
    }

    @Test
    fun test_addToQueue_when_event_is_not_notification_viewed() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            doNothing().`when`(eventQueueManager).processEvent(application, json, Constants.PROFILE_EVENT)

            eventQueueManager.addToQueue(application, json, Constants.PROFILE_EVENT)

            verify(eventQueueManager, never()).processPushNotificationViewedEvent(application, json)
            verify(eventQueueManager).processEvent(application, json, Constants.PROFILE_EVENT)
        }
    }

    @Test
    fun test_processPushNotificationViewedEvent_when_there_is_no_validation_error() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val captor = ArgumentCaptor.forClass(Runnable::class.java)
            corestate.coreMetaData.currentSessionId = 1000
            `when`(eventQueueManager.now).thenReturn(7000)
            doNothing().`when`(eventQueueManager).flushQueueAsync(application, PUSH_NOTIFICATION_VIEWED)

            eventQueueManager.processPushNotificationViewedEvent(application, json)

            assertNull(json.optJSONObject(Constants.ERROR_KEY))
            assertEquals("event", json.getString("type"))
            assertEquals(1000, json.getInt("s"))
            assertEquals(7000, json.getInt("ep"))

            verify(corestate.databaseManager).queuePushNotificationViewedEventToDB(application, json)
            verify(corestate.mainLooperHandler).removeCallbacks(captor.capture())
            verify(corestate.mainLooperHandler).post(captor.capture())

            captor.value.run()

            verify(eventQueueManager).flushQueueAsync(application, PUSH_NOTIFICATION_VIEWED)
        }
    }

    @Test
    fun test_processPushNotificationViewedEvent_when_there_is_validation_error() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            // Arrange
            val validationResult = ValidationResult()
            validationResult.errorDesc = "Fire in the hall"
            validationResult.errorCode = 999

            corestate.validationResultStack.pushValidationResult(validationResult)

            val captor = ArgumentCaptor.forClass(Runnable::class.java)
            corestate.coreMetaData.currentSessionId = 1000
            `when`(eventQueueManager.now).thenReturn(7000)
            doNothing().`when`(eventQueueManager).flushQueueAsync(application, PUSH_NOTIFICATION_VIEWED)

            // Act
            eventQueueManager.processPushNotificationViewedEvent(application, json)

            // Assert
            assertEquals(validationResult.errorCode, json.getJSONObject(Constants.ERROR_KEY)["c"])
            assertEquals(validationResult.errorDesc, json.getJSONObject(Constants.ERROR_KEY)["d"])
            assertEquals("event", json.getString("type"))
            assertEquals(1000, json.getInt("s"))
            assertEquals(7000, json.getInt("ep"))

            verify(corestate.databaseManager).queuePushNotificationViewedEventToDB(application, json)
            verify(corestate.mainLooperHandler).removeCallbacks(captor.capture())
            verify(corestate.mainLooperHandler).post(captor.capture())

            captor.value.run()

            verify(eventQueueManager).flushQueueAsync(application, PUSH_NOTIFICATION_VIEWED)
        }
    }

    @Test
    fun test_pushInitialEventsAsync_does_not_pushBasicProfile_when_inCurrentSession() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            corestate.coreMetaData.currentSessionId = 10000
            doNothing().`when`(eventQueueManager).pushBasicProfile(null)

            eventQueueManager.pushInitialEventsAsync()

            verify(eventQueueManager, never()).pushBasicProfile(null)
        }
    }

    @Test
    fun test_pushInitialEventsAsync_pushesBasicProfile_when_not_inCurrentSession() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            corestate.coreMetaData.currentSessionId = -1
            doNothing().`when`(eventQueueManager).pushBasicProfile(null)

            eventQueueManager.pushInitialEventsAsync()

            verify(eventQueueManager).pushBasicProfile(null)
        }
    }

    @Test
    fun test_scheduleQueueFlush() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            // Arrange

            val captor = ArgumentCaptor.forClass(Runnable::class.java)
            doNothing().`when`(eventQueueManager).flushQueueSync(ArgumentMatchers.any(), ArgumentMatchers.any())

            // Act
            eventQueueManager.scheduleQueueFlush(application)

            // Assert
            verify(corestate.mainLooperHandler).removeCallbacks(captor.capture())
            verify(corestate.mainLooperHandler).postDelayed(captor.capture(), ArgumentMatchers.anyLong())

            captor.value.run()

            verify(eventQueueManager).flushQueueSync(application, REGULAR)
            verify(eventQueueManager).flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)
        }
    }

    @Test
    fun test_flush() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            doNothing().`when`(eventQueueManager).flushQueueAsync(application, REGULAR)

            eventQueueManager.flush()

            verify(eventQueueManager).flushQueueAsync(application, REGULAR)
        }
    }

    @Test
    fun test_flushQueueSync_when_net_is_offline() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            shadowOfCM.setActiveNetworkInfo(null) // make offline

            eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify(corestate.networkManager, never()).needsHandshakeForDomain(PUSH_NOTIFICATION_VIEWED)
        }
    }

    @Test
    fun test_flushQueueSync_when_net_is_online_and_metadata_is_offline() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            corestate.coreMetaData.isOffline = true
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            val netInfo =
                ShadowNetworkInfo.newInstance(DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 1, true, true)
            shadowOfCM.setActiveNetworkInfo(netInfo) // make offline

            eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify(corestate.networkManager, never()).needsHandshakeForDomain(PUSH_NOTIFICATION_VIEWED)
        }
    }

    @Test
    fun test_flushQueueSync_when_HandshakeForDomain_not_needed() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            corestate.coreMetaData.isOffline = false
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            val netInfo =
                ShadowNetworkInfo.newInstance(DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 1, true, true)
            shadowOfCM.setActiveNetworkInfo(netInfo) // make offline

            eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify(corestate.networkManager, never()).initHandshake(ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(corestate.networkManager).flushDBQueue(application, PUSH_NOTIFICATION_VIEWED)
        }
    }

    @Test
    fun test_flushQueueSync_when_HandshakeForDomain_is_needed() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val captor = ArgumentCaptor.forClass(Runnable::class.java)
            corestate.coreMetaData.isOffline = false
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowOfCM = shadowOf(cm)
            val netInfo =
                ShadowNetworkInfo.newInstance(DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 1, true, true)
            shadowOfCM.setActiveNetworkInfo(netInfo) // make offline
            `when`(corestate.networkManager.needsHandshakeForDomain(PUSH_NOTIFICATION_VIEWED)).thenReturn(true)

            eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

            verify(corestate.networkManager).initHandshake(ArgumentMatchers.any(), captor.capture())

            captor.value.run()

            verify(corestate.networkManager).flushDBQueue(application, PUSH_NOTIFICATION_VIEWED)
        }
    }

    @Test
    fun test_pushBasicProfile_when_input_is_null() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            // Arrange
            doReturn(null).`when`(eventQueueManager).queueEvent(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyInt()
            )

            val expectedDeviceId = "device_12345"
            val expectedDeviceCarrier = "carrier_12345"
            val expectedDeviceCC = "CC_12345"
            val expectedDeviceTZ = "tz_12345"

            val expectedTZ = TimeZone.getDefault()
            expectedTZ.id = expectedDeviceTZ
            TimeZone.setDefault(expectedTZ)

            `when`(corestate.deviceInfo.deviceID).thenReturn(expectedDeviceId)
            `when`(corestate.deviceInfo.carrier).thenReturn(expectedDeviceCarrier)
            `when`(corestate.deviceInfo.countryCode).thenReturn(expectedDeviceCC)

            val captor = ArgumentCaptor.forClass(JSONObject::class.java)
            val captorEventType = ArgumentCaptor.forClass(Int::class.java)

            // Act
            eventQueueManager.pushBasicProfile(null)

            // Assert
            verify(eventQueueManager).queueEvent(ArgumentMatchers.any(), captor.capture(), captorEventType.capture())
            val actualProfile = captor.value["profile"] as JSONObject
            assertEquals(expectedDeviceCarrier, actualProfile["Carrier"])
            assertEquals(expectedDeviceCC, actualProfile["cc"])
            assertEquals(expectedDeviceTZ, actualProfile["tz"])
        }
    }

    @Test
    fun test_pushBasicProfile_when_carrier_or_cc_is_null() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            // Arrange
            doReturn(null).`when`(eventQueueManager).queueEvent(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyInt()
            )

            val expectedDeviceId = "device_12345"

            `when`(corestate.deviceInfo.deviceID).thenReturn(expectedDeviceId)
            `when`(corestate.deviceInfo.carrier).thenReturn(null)
            `when`(corestate.deviceInfo.countryCode).thenReturn(null)

            val captor = ArgumentCaptor.forClass(JSONObject::class.java)
            val captorEventType = ArgumentCaptor.forClass(Int::class.java)

            // Act
            eventQueueManager.pushBasicProfile(null)

            // Assert
            verify(eventQueueManager).queueEvent(ArgumentMatchers.any(), captor.capture(), captorEventType.capture())
            val actualProfile = captor.value["profile"] as JSONObject
            assertNull(actualProfile.optString("Carrier", null))
            assertNull(actualProfile.optString("cc", null))
        }
    }

    @Test
    fun test_pushBasicProfile_when_carrier_or_cc_is_blank() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            // Arrange
            doReturn(null).`when`(eventQueueManager).queueEvent(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyInt()
            )

            val expectedDeviceId = "device_12345"

            `when`(corestate.deviceInfo.deviceID).thenReturn(expectedDeviceId)
            `when`(corestate.deviceInfo.carrier).thenReturn("")
            `when`(corestate.deviceInfo.countryCode).thenReturn("")

            val captor = ArgumentCaptor.forClass(JSONObject::class.java)
            val captorEventType = ArgumentCaptor.forClass(Int::class.java)

            // Act
            eventQueueManager.pushBasicProfile(null)

            // Assert
            verify(eventQueueManager).queueEvent(ArgumentMatchers.any(), captor.capture(), captorEventType.capture())
            val actualProfile = captor.value["profile"] as JSONObject
            assertNull(actualProfile.optString("Carrier", null))
            assertNull(actualProfile.optString("cc", null))
        }
    }

    @Test
    fun test_pushBasicProfile_when_input_is_not_null() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(IdentityRepoFactory::class.java).use {
                val mockIdentityRepo = mock(IdentityRepo::class.java)
                `when`(
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.deviceInfo,
                        corestate.validationResultStack
                    )
                ).thenReturn(mockIdentityRepo)

                // Arrange
                doReturn(null).`when`(eventQueueManager).queueEvent(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.anyInt()
                )

                val expectedDeviceId = "device_12345"

                `when`(corestate.deviceInfo.deviceID).thenReturn(expectedDeviceId)

                val captor = ArgumentCaptor.forClass(JSONObject::class.java)
                val captorEventType = ArgumentCaptor.forClass(Int::class.java)

                val inputJson = JSONObject()
                val subInputJson = JSONObject()
                subInputJson.put("age", 70)
                inputJson.put("name", "abc")
                inputJson.put("details", subInputJson)

                // Act
                eventQueueManager.pushBasicProfile(inputJson)

                // Assert
                verify(eventQueueManager).queueEvent(
                    ArgumentMatchers.any(),
                    captor.capture(),
                    captorEventType.capture()
                )
                val actualProfile = captor.value["profile"] as JSONObject
                val actualDetails = actualProfile["details"] as JSONObject

                assertEquals("abc", actualProfile["name"])
                assertEquals(70, actualDetails["age"])
            }
        }
    }

    @Test
    fun test_pushBasicProfile_when_key_is_profile_identifier() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(IdentityRepoFactory::class.java).use {
                val mockIdentityRepo = mock(IdentityRepo::class.java)
                val mockLoginInfoProvider = mock(LoginInfoProvider::class.java)
                `when`(
                    IdentityRepoFactory.getRepo(
                        application,
                        corestate.config,
                        corestate.deviceInfo,
                        corestate.validationResultStack
                    )
                ).thenReturn(mockIdentityRepo)

                // Arrange
                doReturn(null).`when`(eventQueueManager).queueEvent(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.anyInt()
                )

                val expectedDeviceId = "device_12345"

                `when`(corestate.deviceInfo.deviceID).thenReturn(expectedDeviceId)

                val captor = ArgumentCaptor.forClass(JSONObject::class.java)
                val captorEventType = ArgumentCaptor.forClass(Int::class.java)

                val inputJson = JSONObject()
                val subInputJson = JSONObject()
                subInputJson.put("age", 70)
                inputJson.put("name", "abc")
                inputJson.put("details", subInputJson)

                `when`(eventQueueManager.loginInfoProvider).thenReturn(mockLoginInfoProvider)
                `when`(mockIdentityRepo.hasIdentity("name")).thenReturn(true)

                // Act
                eventQueueManager.pushBasicProfile(inputJson)

                // Assert
                verify(mockLoginInfoProvider).cacheGUIDForIdentifier(expectedDeviceId, "name", "abc")
                verify(eventQueueManager).queueEvent(
                    ArgumentMatchers.any(),
                    captor.capture(),
                    captorEventType.capture()
                )
                val actualProfile = captor.value["profile"] as JSONObject
                val actualDetails = actualProfile["details"] as JSONObject

                assertEquals("abc", actualProfile["name"])
                assertEquals(70, actualDetails["age"])
            }
        }
    }

    @Test
    fun test_processEvent_when_type_is_page_event() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
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

            `when`(eventQueueManager.now).thenReturn(expectedEpoch)
            doNothing().`when`(eventQueueManager).scheduleQueueFlush(application)

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
            verify(corestate.localDataStore).setDataSyncFlag(actualEvent)
            verify(corestate.databaseManager).queueEventToDB(application, actualEvent, Constants.PAGE_EVENT)
            verify(corestate.localDataStore, never()).persistEvent(application, actualEvent, Constants.PAGE_EVENT)
            verify(eventQueueManager).scheduleQueueFlush(application)
        }
    }

    @Test
    fun test_processEvent_when_type_is_ping_event() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            mockStatic(Utils::class.java).use {

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

                `when`(Utils.getMemoryConsumption()).thenReturn(expectedMemoryConsumption)
                `when`(Utils.getCurrentNetworkType(application)).thenReturn(expectedNetworkType)
                `when`(eventQueueManager.now).thenReturn(expectedEpoch)
                doNothing().`when`(eventQueueManager).scheduleQueueFlush(application)

                val actualEvent = JSONObject()
                actualEvent.put("bk", 1)

                `when`(eventQueueManager.now).thenReturn(expectedEpoch)
                doNothing().`when`(eventQueueManager).scheduleQueueFlush(application)

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
                verify(corestate.localDataStore).setDataSyncFlag(actualEvent)
                verify(corestate.databaseManager).queueEventToDB(application, actualEvent, Constants.PING_EVENT)
                verify(corestate.localDataStore, never()).persistEvent(application, actualEvent, Constants.PING_EVENT)
                verify(eventQueueManager).scheduleQueueFlush(application)
            }
        }
    }

    @Test
    fun test_processEvent_when_type_is_profile_event() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )

            val actualEvent = JSONObject()

            eventQueueManager.processEvent(application, actualEvent, Constants.PROFILE_EVENT)

            assertEquals("profile", actualEvent["type"])
            // assert following keys are absent in json
            assertNull(actualEvent.opt("pai"))
        }
    }

    @Test
    fun test_processEvent_when_type_is_data_event() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )

            val actualEvent = JSONObject()

            eventQueueManager.processEvent(application, actualEvent, Constants.DATA_EVENT)

            assertEquals("data", actualEvent["type"])
            // assert following keys are absent in json
            assertNull(actualEvent.opt("pai"))
        }
    }

    @Test
    fun test_processEvent_when_type_is_normal_event() {
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(cleverTapInstanceConfig)).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )

            val actualEvent = JSONObject()
            actualEvent.put("evtName", Constants.APP_LAUNCHED_EVENT)

            eventQueueManager.processEvent(application, actualEvent, Constants.RAISED_EVENT)

            // assert following keys are present in json
            assertEquals("event", actualEvent["type"])
            assertNotNull(actualEvent.opt("pai"))
        }
    }
}