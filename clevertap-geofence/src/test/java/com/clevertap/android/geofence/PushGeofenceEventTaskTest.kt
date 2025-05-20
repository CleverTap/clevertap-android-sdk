package com.clevertap.android.geofence

import android.content.Intent
import android.location.Location
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getDoubleMatchingTriggeredGeofenceList
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getNonMatchingTriggeredGeofenceList
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getSingleMatchingTriggeredGeofenceList
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getTriggeredLocation
import com.clevertap.android.geofence.fakes.GeofenceJSON
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.sdk.CleverTapAPI
import com.google.android.gms.location.GeofencingEvent
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.util.concurrent.Future

class PushGeofenceEventTaskTest : BaseTestCase() {

    private lateinit var cleverTapAPI: CleverTapAPI
    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var onCompleteListener: CTGeofenceTask.OnCompleteListener
    private lateinit var geofencingEvent: GeofencingEvent
    private lateinit var intent: Intent
    private lateinit var location: Location
    private lateinit var logger: Logger

    override fun setUp() {
        super.setUp()

        cleverTapAPI = mockk(relaxed = true)
        ctGeofenceAPI = mockk(relaxed = true)
        onCompleteListener = mockk(relaxed = true)
        geofencingEvent = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        location = Location("")
        intent = Intent()

        mockkStatic(CTGeofenceAPI::class)
        mockkStatic(Utils::class)
        mockkStatic(FileUtils::class)
        mockkStatic(GeofencingEvent::class)

        every { CTGeofenceAPI.getInstance(any()) } returns ctGeofenceAPI
        every { CTGeofenceAPI.getLogger() } returns logger
        every { GeofencingEvent.fromIntent(intent) } returns geofencingEvent
        every { ctGeofenceAPI.cleverTapApi } returns cleverTapAPI
    }

    @After
    override fun cleanUp() {
        super.cleanUp()
        unmockkStatic(CTGeofenceAPI::class)
        unmockkStatic(FileUtils::class)
        unmockkStatic(Utils::class)
        unmockkStatic(GeofencingEvent::class)
    }

    @Test
    fun testExecuteWhenCleverTapApiIsNull() {
        every { Utils.initCTGeofenceApiIfRequired(application) } returns false
        val task = PushGeofenceEventTask(application, intent)

        // when listener null
        task.execute()
        confirmVerified(onCompleteListener)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        verify(exactly = 0) { GeofencingEvent.fromIntent(intent) }
        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteWhenGeofenceEventNotValid() {
        // When geofence does not have error and geofence event is not valid

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        val task = PushGeofenceEventTask(application, intent)
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.geofenceTransition } returns 0

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        // when geofence event is not valid
        verify { cleverTapAPI.pushGeoFenceError(any(), any()) }
        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteWhenGeofenceHasError() {
        // When geofence has error

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        val task = PushGeofenceEventTask(application, intent)
        every { geofencingEvent.hasError() } returns true

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        verify { cleverTapAPI.pushGeoFenceError(any(), any()) }
        verify { onCompleteListener.onComplete() }
        verify(exactly = 0) { geofencingEvent.geofenceTransition }
    }

    @Test
    fun testExecuteWhenTriggeredGeofenceIsNull() {
        // When geofence does not have error and geofence event is valid

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        val task = PushGeofenceEventTask(application, intent)
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.geofenceTransition } returns 1
        every { geofencingEvent.triggeringGeofences } returns null

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        // when geofence event is enter and triggered geofence is null
        verify { cleverTapAPI.pushGeoFenceError(any(), any()) }
        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteWhenTriggeredGeofenceNotNull() {
        // when geofence event is enter and triggered geofence is not null
        val geofenceList = mutableListOf<com.google.android.gms.location.Geofence>()
        val location = Location("")

        val task = PushGeofenceEventTask(application, intent)

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns geofenceList
        every { geofencingEvent.triggeringLocation } returns location
        every { geofencingEvent.geofenceTransition } returns 1
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns ""

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        verify { FileUtils.readFromFile(any(), any()) }
        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testPushGeofenceEventsWhenEnter() {
        // When old geofence in file is not empty and triggered geofence found in file

        val triggeredLocation = getTriggeredLocation()
        val task = PushGeofenceEventTask(application, intent)
        val future: Future<*> = mockk(relaxed = true)

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns getSingleMatchingTriggeredGeofenceList()
        every { geofencingEvent.triggeringLocation } returns triggeredLocation
        every { geofencingEvent.geofenceTransition } returns 1
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.GEOFENCE_JSON_STRING
        every { cleverTapAPI.pushGeofenceEnteredEvent(any()) } returns future

        task.execute()

        val firstFromGeofenceArray = GeofenceJSON.firstFromGeofenceArray.getJSONObject(0)
        firstFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
        firstFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

        val jsonSlot = slot<JSONObject>()

        verify { cleverTapAPI.pushGeofenceEnteredEvent(capture(jsonSlot)) }
        JSONAssert.assertEquals(firstFromGeofenceArray, jsonSlot.captured, true)

        verify { future.get() }
    }

    @Test
    fun testPushGeofenceEventsWhenExit() {
        // When old geofence in file is not empty and triggered geofence found in file

        val task = PushGeofenceEventTask(application, intent)
        val future: Future<*> = mockk(relaxed = true)
        val triggeredLocation = getTriggeredLocation()

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns getSingleMatchingTriggeredGeofenceList()
        every { geofencingEvent.triggeringLocation } returns triggeredLocation
        every { geofencingEvent.geofenceTransition } returns 2
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.GEOFENCE_JSON_STRING
        every { cleverTapAPI.pushGeoFenceExitedEvent(any()) } returns future

        task.execute()
        val firstFromGeofenceArray = GeofenceJSON.firstFromGeofenceArray.getJSONObject(0)
        firstFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
        firstFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

        val jsonSlot = slot<JSONObject>()

        verify { cleverTapAPI.pushGeoFenceExitedEvent(capture(jsonSlot)) }
        JSONAssert.assertEquals(firstFromGeofenceArray, jsonSlot.captured, true)

        verify { future.get() }
    }

    @Test
    fun testPushGeofenceEventsWhenMultipleExit() {
        // Test multiple geofence exit triggers

        val task = PushGeofenceEventTask(application, intent)
        val future: Future<*> = mockk(relaxed = true)
        val triggeredLocation = getTriggeredLocation()

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns getDoubleMatchingTriggeredGeofenceList()
        every { geofencingEvent.triggeringLocation } returns triggeredLocation
        every { geofencingEvent.geofenceTransition } returns 2

        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.GEOFENCE_JSON_STRING
        every { cleverTapAPI.pushGeoFenceExitedEvent(any()) } returns future

        task.execute()

        // Multiple Geofence Exit event
        val jsonSlots = mutableListOf<JSONObject>()

        verify(exactly = 2) { cleverTapAPI.pushGeoFenceExitedEvent(capture(jsonSlots)) }

        // First call assertion
        val firstFromGeofenceArray = GeofenceJSON.firstFromGeofenceArray.getJSONObject(0)
        firstFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
        firstFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

        // assert geofence with id 310001
        JSONAssert.assertEquals(firstFromGeofenceArray, jsonSlots[0], true)

        val lastFromGeofenceArray = GeofenceJSON.lastFromGeofenceArray.getJSONObject(0)
        lastFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
        lastFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

        // assert geofence with id 310002
        JSONAssert.assertEquals(lastFromGeofenceArray, jsonSlots[1], true)

        verify(exactly = 2) { future.get() }
    }

    @Test
    fun testPushGeofenceEventsWhenOldGeofenceIsEmpty() {
        // When old geofence in file is empty

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns getSingleMatchingTriggeredGeofenceList()
        every { geofencingEvent.triggeringLocation } returns triggeredLocation
        every { geofencingEvent.geofenceTransition } returns 2

        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns ""

        task.execute()
        verify(exactly = 0) { cleverTapAPI.pushGeofenceEnteredEvent(any()) }
    }

    @Test
    fun testPushGeofenceEventsWhenOldGeofenceJsonArrayIsEmpty() {
        // When old geofence json array in file is empty

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns getSingleMatchingTriggeredGeofenceList()
        every { geofencingEvent.triggeringLocation } returns triggeredLocation
        every { geofencingEvent.geofenceTransition } returns 2

        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.emptyGeofence.toString()

        task.execute()
        verify(exactly = 0) { cleverTapAPI.pushGeofenceEnteredEvent(any()) }
    }

    @Test
    fun testPushGeofenceEventsWhenOldGeofenceJsonInvalid() {
        // When old geofence json content in file is invalid

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns getSingleMatchingTriggeredGeofenceList()
        every { geofencingEvent.triggeringLocation } returns triggeredLocation
        every { geofencingEvent.geofenceTransition } returns 2

        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.emptyJson.toString()

        task.execute()

        verify(exactly = 0) { cleverTapAPI.pushGeofenceEnteredEvent(any()) }
    }

    @Test
    fun testPushGeofenceEventsWhenTriggeredGeofenceIsNotFound() {
        // When triggered geofence not found in file

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns getNonMatchingTriggeredGeofenceList()
        every { geofencingEvent.triggeringLocation } returns triggeredLocation
        every { geofencingEvent.geofenceTransition } returns 2

        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.GEOFENCE_JSON_STRING

        task.execute()

        verify(exactly = 0) { cleverTapAPI.pushGeofenceEnteredEvent(any()) }
        verify { cleverTapAPI.pushGeoFenceError(any(), any()) }
    }

    @Test
    fun testPushGeofenceEventsWhenTriggeredGeofenceIsNull() {
        // When triggered geofence is null

        val task = PushGeofenceEventTask(application, intent)
        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.triggeringGeofences } returns null
        every { geofencingEvent.triggeringLocation } returns location
        every { geofencingEvent.geofenceTransition } returns 2

        task.execute()
        verify { cleverTapAPI.pushGeoFenceError(any(), any()) }

        verify(exactly = 0) { FileUtils.readFromFile(any(), any()) }
    }

    @Test
    fun testSendOnCompleteEventWhenListenerIsNull() {
        // when listener is null
        val task = PushGeofenceEventTask(application, intent)

        task.execute()
        verify(exactly = 0) { onCompleteListener.onComplete() }
    }

    @Test
    fun testSendOnCompleteEventWhenListenerNotNull() {
        // when listener not null
        val task = PushGeofenceEventTask(application, intent)

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        verify { onCompleteListener.onComplete() }
    }
}
