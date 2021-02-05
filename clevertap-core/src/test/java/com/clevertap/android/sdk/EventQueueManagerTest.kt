package com.clevertap.android.sdk

import com.clevertap.android.sdk.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
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
}