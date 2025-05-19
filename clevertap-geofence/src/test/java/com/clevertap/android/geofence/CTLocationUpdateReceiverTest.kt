package com.clevertap.android.geofence

import android.content.BroadcastReceiver
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.LocationResult
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class CTLocationUpdateReceiverTest : BaseTestCase() {

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    lateinit var pendingResult: BroadcastReceiver.PendingResult

    private lateinit var locationResult: LocationResult

    @Mock
    lateinit var logger: Logger

    @Before
    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        super.setUp()
        val location = Location("")
        locationResult = LocationResult.create(listOf(location))
    }

    @Test
    fun testOnReceiveWhenLastLocationNotNull() {
        val receiver = CTLocationUpdateReceiver()
        val spy = Mockito.spy(receiver)
        `when`(spy.goAsync()).thenReturn(pendingResult)

        val isFinished = arrayOf(false)

        doAnswer { invocation ->
            isFinished[0] = true
            null
        }.`when`(pendingResult).finish()

        val intent = Intent()
        intent.putExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT", locationResult)

        Mockito.mockStatic(CTGeofenceAPI::class.java).use { ctGeofenceAPIMockedStatic ->
            ctGeofenceAPIMockedStatic.`when`<Logger>(CTGeofenceAPI::getLogger)
                .thenReturn(logger)

            spy.onReceive(application, intent)

            await().until { isFinished[0] }

            verify(CTGeofenceAPI.getLogger()).debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Location updates receiver called"
            )
            verify(CTGeofenceAPI.getLogger()).debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Returning from Location Updates Receiver"
            )
            verify(pendingResult).finish()
        }
    }

    @Test
    fun testOnReceiveWhenLocationResultIsNull() {
        val receiver = CTLocationUpdateReceiver()
        val spy = Mockito.spy(receiver)
        `when`(spy.goAsync()).thenReturn(pendingResult)

        spy.onReceive(application, null)

        verify(pendingResult).finish()
    }
}
