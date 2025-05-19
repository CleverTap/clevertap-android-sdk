package com.clevertap.android.geofence

import android.content.Context
import android.content.Intent
import android.location.Location
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getDoubleMatchingTriggeredGeofenceList
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getNonMatchingTriggeredGeofenceList
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getSingleMatchingTriggeredGeofenceList
import com.clevertap.android.geofence.fakes.GeofenceEventFake.getTriggeredLocation
import com.clevertap.android.geofence.fakes.GeofenceJSON
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.sdk.CleverTapAPI
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.skyscreamer.jsonassert.JSONAssert
import java.util.concurrent.Future


class PushGeofenceEventTaskTest : BaseTestCase() {

    @Mock
    lateinit var cleverTapAPI: CleverTapAPI

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    lateinit var onCompleteListener: CTGeofenceTask.OnCompleteListener

    @Mock
    private lateinit var geofencingEvent: GeofencingEvent

    private lateinit var intent: Intent

    private lateinit var location: Location

    @Mock
    private lateinit var logger: Logger

    private lateinit var utilsMockedStatic: MockedStatic<Utils>

    private lateinit var fileUtilsMockedStatic: MockedStatic<FileUtils>

    private lateinit var geofencingEventMockedStatic: MockedStatic<GeofencingEvent>

    private lateinit var ctGeofenceAPIMockedStatic: MockedStatic<CTGeofenceAPI>

    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        location = Location("")
        intent = Intent()

        ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI::class.java)
        utilsMockedStatic = Mockito.mockStatic(Utils::class.java)
        fileUtilsMockedStatic = Mockito.mockStatic(FileUtils::class.java)
        geofencingEventMockedStatic = Mockito.mockStatic(GeofencingEvent::class.java)

        Mockito.`when`(
            CTGeofenceAPI.getInstance(
                ArgumentMatchers.any(
                    Context::class.java
                )
            )
        ).thenReturn(ctGeofenceAPI)
        Mockito.`when`(CTGeofenceAPI.getLogger()).thenReturn(logger)
        Mockito.`when`(GeofencingEvent.fromIntent(intent))
            .thenReturn(geofencingEvent)

        Mockito.`when`(ctGeofenceAPI.cleverTapApi).thenReturn(cleverTapAPI)

        super.setUp()
    }

    @After
    fun cleanup() {
        ctGeofenceAPIMockedStatic.close()
        fileUtilsMockedStatic.close()
        utilsMockedStatic.close()
        geofencingEventMockedStatic.close()
    }

    @Test
    fun testExecuteWhenCleverTapApiIsNull() {
        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(false)
        val task = PushGeofenceEventTask(application, intent)

        // when listener null
        task.execute()
        Mockito.verifyNoMoreInteractions(onCompleteListener)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        geofencingEventMockedStatic.verify(MockedStatic.Verification {
            GeofencingEvent.fromIntent(
                intent
            )
        }, Mockito.times(0))
        Mockito.verify(onCompleteListener).onComplete()
    }

    @Test
    fun testExecuteWhenGeofenceEventNotValid() {
        // When geofence does not have error and geofence event is not valid

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        val task = PushGeofenceEventTask(application, intent)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(0)

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        // when geofence event is not valid
        Mockito.verify(cleverTapAPI)
            .pushGeoFenceError(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())
        Mockito.verify(onCompleteListener).onComplete()
    }

    @Test
    fun testExecuteWhenGeofenceHasError() {
        // When geofence has error

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        val task = PushGeofenceEventTask(application, intent)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(true)

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        Mockito.verify(cleverTapAPI)
            .pushGeoFenceError(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())
        Mockito.verify(onCompleteListener).onComplete()
        Mockito.verify(geofencingEvent, Mockito.times(0)).geofenceTransition
    }

    @Test
    fun testExecuteWhenTriggeredGeofenceIsNull() {
        // When geofence does not have error and geofence event is valid

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        val task = PushGeofenceEventTask(application, intent)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(1)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(null)

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        // when geofence event is enter and triggered geofence is null
        Mockito.verify(cleverTapAPI)
            .pushGeoFenceError(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())
        Mockito.verify(onCompleteListener).onComplete()
    }

    @Test
    fun testExecuteWhenTriggeredGeofenceNotNull() {
        // when geofence event is enter and triggered geofence is not null
        val geofenceList: MutableList<Geofence?> = ArrayList<Geofence?>()
        val location = Location("")

        val task = PushGeofenceEventTask(application, intent)

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(geofenceList)
        Mockito.`when`(geofencingEvent.triggeringLocation).thenReturn(location)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(1)
        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        fileUtilsMockedStatic.verify(MockedStatic.Verification {
            FileUtils.readFromFile(
                ArgumentMatchers.any(
                    Context::class.java
                ), ArgumentMatchers.anyString()
            )
        })

        Mockito.verify(onCompleteListener).onComplete()
    }

    @Test
    fun testPushGeofenceEventsWhenEnter() {
        // When old geofence in file is not empty and triggered geofence found in file

        val triggeredLocation = getTriggeredLocation()
        val task = PushGeofenceEventTask(application, intent)
        val future = Mockito.mock(Future::class.java)

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(getSingleMatchingTriggeredGeofenceList())
        Mockito.`when`(geofencingEvent.triggeringLocation).thenReturn(triggeredLocation)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(1)

        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn(GeofenceJSON.GEOFENCE_JSON_STRING)
        Mockito.`when`(
            cleverTapAPI.pushGeofenceEnteredEvent(
                ArgumentMatchers.any(
                    JSONObject::class.java
                )
            )
        ).thenReturn(future)

        task.execute()

        val firstFromGeofenceArray = GeofenceJSON.firstFromGeofenceArray.getJSONObject(0)
        firstFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
        firstFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

        val objectArgumentCaptor =
            ArgumentCaptor.forClass<JSONObject?, JSONObject?>(JSONObject::class.java)

        Mockito.verify(cleverTapAPI).pushGeofenceEnteredEvent(objectArgumentCaptor.capture())
        JSONAssert.assertEquals(firstFromGeofenceArray, objectArgumentCaptor.getValue(), true)

        Mockito.verify(future).get()
    }

    @Test
    fun testPushGeofenceEventsWhenExit() {
        // When old geofence in file is not empty and triggered geofence found in file

        val task = PushGeofenceEventTask(application, intent)
        val future = Mockito.mock(Future::class.java)
        val triggeredLocation = getTriggeredLocation()

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(getSingleMatchingTriggeredGeofenceList())
        Mockito.`when`(geofencingEvent.triggeringLocation)
            .thenReturn(triggeredLocation)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(2)

        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn(GeofenceJSON.GEOFENCE_JSON_STRING)
        Mockito.`when`(
            cleverTapAPI.pushGeoFenceExitedEvent(
                ArgumentMatchers.any(
                    JSONObject::class.java
                )
            )
        ).thenReturn(future)

        task.execute()
        val firstFromGeofenceArray = GeofenceJSON.firstFromGeofenceArray.getJSONObject(0)
        firstFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
        firstFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

        val objectArgumentCaptor =
            ArgumentCaptor.forClass<JSONObject?, JSONObject?>(JSONObject::class.java)

        Mockito.verify(cleverTapAPI).pushGeoFenceExitedEvent(objectArgumentCaptor.capture())
        JSONAssert.assertEquals(firstFromGeofenceArray, objectArgumentCaptor.getValue(), true)

        Mockito.verify(future).get()
    }

    @Test
    fun testPushGeofenceEventsWhenMultipleExit() {
        // Test multiple geofence exit triggers

        val task = PushGeofenceEventTask(application, intent)
        val future = Mockito.mock(Future::class.java)
        val triggeredLocation = getTriggeredLocation()

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(getDoubleMatchingTriggeredGeofenceList())
        Mockito.`when`(geofencingEvent.triggeringLocation).thenReturn(triggeredLocation)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(2)

        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn(GeofenceJSON.GEOFENCE_JSON_STRING)
        Mockito.`when`(
            cleverTapAPI.pushGeoFenceExitedEvent(
                ArgumentMatchers.any(
                    JSONObject::class.java
                )
            )
        ).thenReturn(future)
        task.execute()

        try {
            // Multiple Geofence Exit event

            val firstFromGeofenceArray = GeofenceJSON.firstFromGeofenceArray.getJSONObject(0)
            firstFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
            firstFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

            val objectArgumentCaptor =
                ArgumentCaptor.forClass<JSONObject?, JSONObject?>(JSONObject::class.java)

            Mockito.verify(cleverTapAPI, Mockito.times(2))
                .pushGeoFenceExitedEvent(objectArgumentCaptor.capture())

            // assert geofence with id 310001
            JSONAssert.assertEquals(
                firstFromGeofenceArray, objectArgumentCaptor.getAllValues()[0], true
            )

            val lastFromGeofenceArray = GeofenceJSON.lastFromGeofenceArray.getJSONObject(0)
            lastFromGeofenceArray.put("triggered_lat", triggeredLocation.latitude)
            lastFromGeofenceArray.put("triggered_lng", triggeredLocation.longitude)

            // assert geofence with id 310002
            JSONAssert.assertEquals(
                lastFromGeofenceArray, objectArgumentCaptor.getAllValues()[1], true
            )

            Mockito.verify(future, Mockito.times(2)).get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun testPushGeofenceEventsWhenOldGeofenceIsEmpty() {
        // When old geofence in file is empty

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(getSingleMatchingTriggeredGeofenceList())
        Mockito.`when`(geofencingEvent.triggeringLocation)
            .thenReturn(triggeredLocation)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(2)

        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")

        task.execute()
        try {
            Mockito.verify(cleverTapAPI, Mockito.never()).pushGeofenceEnteredEvent(
                ArgumentMatchers.any(
                    JSONObject::class.java
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun testPushGeofenceEventsWhenOldGeofenceJsonArrayIsEmpty() {
        // When old geofence json array in file is empty

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(getSingleMatchingTriggeredGeofenceList())
        Mockito.`when`(geofencingEvent.triggeringLocation)
            .thenReturn(triggeredLocation)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(2)

        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn(GeofenceJSON.emptyGeofence.toString())

        task.execute()
        try {
            Mockito.verify(cleverTapAPI, Mockito.never()).pushGeofenceEnteredEvent(
                ArgumentMatchers.any(
                    JSONObject::class.java
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //
    @Test
    fun testPushGeofenceEventsWhenOldGeofenceJsonInvalid() {
        // When old geofence json content in file is invalid

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(getSingleMatchingTriggeredGeofenceList())
        Mockito.`when`(geofencingEvent.triggeringLocation)
            .thenReturn(triggeredLocation)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(2)

        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn(GeofenceJSON.emptyJson.toString())

        task.execute()
        try {
            Mockito.verify(cleverTapAPI, Mockito.never()).pushGeofenceEnteredEvent(
                ArgumentMatchers.any(
                    JSONObject::class.java
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun testPushGeofenceEventsWhenTriggeredGeofenceIsNotFound() {
        // When triggered geofence not found in file

        val task = PushGeofenceEventTask(application, intent)
        val triggeredLocation = getTriggeredLocation()

        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(getNonMatchingTriggeredGeofenceList())
        Mockito.`when`(geofencingEvent.triggeringLocation)
            .thenReturn(triggeredLocation)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(2)

        Mockito.`when`(
            FileUtils.getCachedFullPath(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn("")
        Mockito.`when`(
            FileUtils.readFromFile(
                ArgumentMatchers.any(Context::class.java), ArgumentMatchers.anyString()
            )
        ).thenReturn(GeofenceJSON.GEOFENCE_JSON_STRING)

        task.execute()
        try {
            Mockito.verify(cleverTapAPI, Mockito.never()).pushGeofenceEnteredEvent(
                ArgumentMatchers.any(
                    JSONObject::class.java
                )
            )
            Mockito.verify(cleverTapAPI)
                .pushGeoFenceError(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun testPushGeofenceEventsWhenTriggeredGeofenceIsNull() {
        // When triggered geofence is null

        val task = PushGeofenceEventTask(application, intent)
        Mockito.`when`(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)
        Mockito.`when`(geofencingEvent.hasError()).thenReturn(false)
        Mockito.`when`(geofencingEvent.triggeringGeofences)
            .thenReturn(null)
        Mockito.`when`(geofencingEvent.triggeringLocation).thenReturn(location)
        Mockito.`when`(geofencingEvent.geofenceTransition).thenReturn(2)

        task.execute()
        Mockito.verify(cleverTapAPI)
            .pushGeoFenceError(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())

        fileUtilsMockedStatic.verify(MockedStatic.Verification {
            FileUtils.readFromFile(
                ArgumentMatchers.any(
                    Context::class.java
                ), ArgumentMatchers.anyString()
            )
        }, Mockito.times(0))
    }

    @Test
    fun testSendOnCompleteEventWhenListenerIsNull() {
        // when listener is null
        val task = PushGeofenceEventTask(application, intent)

        task.execute()
        Mockito.verify(onCompleteListener, Mockito.times(0)).onComplete()
    }

    @Test
    fun testSendOnCompleteEventWhenListenerNotNull() {
        // when listener not null
        val task = PushGeofenceEventTask(application, intent)

        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        Mockito.verify(onCompleteListener).onComplete()
    }
}
