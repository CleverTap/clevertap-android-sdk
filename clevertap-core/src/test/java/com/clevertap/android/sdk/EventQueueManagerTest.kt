package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

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
            Mockito.spy(
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
}