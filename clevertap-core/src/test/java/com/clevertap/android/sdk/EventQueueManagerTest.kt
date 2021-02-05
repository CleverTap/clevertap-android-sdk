package com.clevertap.android.sdk

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo.DetailedState
import com.clevertap.android.sdk.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.EventGroup.REGULAR
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkInfo
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class EventQueueManagerTest : BaseTestCase() {

    private lateinit var corestate: MockCoreState
    private lateinit var eventQueueManager: EventQueueManager
    private lateinit var json: JSONObject

    @Before
    override fun setUp() {
        super.setUp()

        corestate = MockCoreState(application, cleverTapInstanceConfig)
        corestate.postAsyncSafelyHandler = MockPostAsyncSafelyHandler(cleverTapInstanceConfig)

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
                    corestate.postAsyncSafelyHandler,
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

    @Test
    fun test_queueEvent_will_not_add_to_queue_when_event_should_be_dropped() {

        `when`(corestate.eventMediator.shouldDropEvent(json, Constants.PING_EVENT))
            .thenReturn(true)
        eventQueueManager.queueEvent(application, json, Constants.PING_EVENT)
        verify(eventQueueManager, never()).addToQueue(application, json, Constants.PING_EVENT)
    }

    @Test
    fun test_queueEvent_will_add_to_queue_when_event_should_not_be_dropped() {

        `when`(corestate.eventMediator.shouldDropEvent(json, Constants.FETCH_EVENT))
            .thenReturn(false)
        doNothing().`when`(eventQueueManager).addToQueue(application, json, Constants.FETCH_EVENT)
        eventQueueManager.queueEvent(application, json, Constants.FETCH_EVENT)
        verify(eventQueueManager).addToQueue(application, json, Constants.FETCH_EVENT)
    }

    @Test
    fun test_queueEvent_will_process_further_and_add_to_queue_when_event_should_not_be_dropped() {

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

    @Test
    fun test_queueEvent_will_delay_add_to_queue_when_event_processing_should_be_delayed() {

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

    @Test
    fun test_addToQueue_when_event_is_notification_viewed() {
        doNothing().`when`(eventQueueManager).processPushNotificationViewedEvent(application, json)

        eventQueueManager.addToQueue(application, json, Constants.NV_EVENT)

        verify(eventQueueManager).processPushNotificationViewedEvent(application, json)
        verify(eventQueueManager, never()).processEvent(application, json, Constants.NV_EVENT)
    }

    @Test
    fun test_addToQueue_when_event_is_not_notification_viewed() {
        doNothing().`when`(eventQueueManager).processEvent(application, json, Constants.PROFILE_EVENT)

        eventQueueManager.addToQueue(application, json, Constants.PROFILE_EVENT)

        verify(eventQueueManager, never()).processPushNotificationViewedEvent(application, json)
        verify(eventQueueManager).processEvent(application, json, Constants.PROFILE_EVENT)
    }

    @Test
    fun test_processPushNotificationViewedEvent_when_there_is_no_validation_error() {
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

    @Test
    fun test_processPushNotificationViewedEvent_when_there_is_validation_error() {
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

    @Test
    fun test_pushInitialEventsAsync_does_not_pushBasicProfile_when_inCurrentSession() {
        corestate.coreMetaData.currentSessionId = 10000
        doNothing().`when`(eventQueueManager).pushBasicProfile(null)

        eventQueueManager.pushInitialEventsAsync()

        verify(eventQueueManager, never()).pushBasicProfile(null)
    }

    @Test
    fun test_pushInitialEventsAsync_pushesBasicProfile_when_not_inCurrentSession() {
        corestate.coreMetaData.currentSessionId = -1
        doNothing().`when`(eventQueueManager).pushBasicProfile(null)

        eventQueueManager.pushInitialEventsAsync()

        verify(eventQueueManager).pushBasicProfile(null)
    }

    @Test
    fun test_scheduleQueueFlush() {
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

    @Test
    fun test_flush() {
        doNothing().`when`(eventQueueManager).flushQueueAsync(application, REGULAR)

        eventQueueManager.flush()

        verify(eventQueueManager).flushQueueAsync(application, REGULAR)
    }

    @Test
    fun test_flushQueueSync_when_net_is_offline() {
        val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadowOfCM = shadowOf(cm)
        shadowOfCM.setActiveNetworkInfo(null) // make offline

        eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

        verify(corestate.networkManager, never()).needsHandshakeForDomain(PUSH_NOTIFICATION_VIEWED)
    }

    @Test
    fun test_flushQueueSync_when_net_is_online_and_metadata_is_offline() {
        corestate.coreMetaData.isOffline = true
        val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadowOfCM = shadowOf(cm)
        val netInfo =
            ShadowNetworkInfo.newInstance(DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 1, true, true)
        shadowOfCM.setActiveNetworkInfo(netInfo) // make offline

        eventQueueManager.flushQueueSync(application, PUSH_NOTIFICATION_VIEWED)

        verify(corestate.networkManager, never()).needsHandshakeForDomain(PUSH_NOTIFICATION_VIEWED)
    }

    @Test
    fun test_flushQueueSync_when_HandshakeForDomain_not_needed() {
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

    @Test
    fun test_flushQueueSync_when_HandshakeForDomain_is_needed() {
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