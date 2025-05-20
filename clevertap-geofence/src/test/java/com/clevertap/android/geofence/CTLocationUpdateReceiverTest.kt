package com.clevertap.android.geofence

import android.content.BroadcastReceiver
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.LocationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test

class CTLocationUpdateReceiverTest : BaseTestCase() {

    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var pendingResult: BroadcastReceiver.PendingResult
    private lateinit var locationResult: LocationResult
    private lateinit var logger: Logger

    @Before
    override fun setUp() {
        super.setUp()
        ctGeofenceAPI = mockk(relaxed = true)
        pendingResult = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        val location = Location("")
        locationResult = LocationResult.create(listOf(location))
    }

    @Test
    fun testOnReceiveWhenLastLocationNotNull() {
        val receiver = CTLocationUpdateReceiver()
        val spy = spyk(receiver)
        every { spy.goAsync() } returns pendingResult

        val isFinished = arrayOf(false)

        every { pendingResult.finish() } answers {
            isFinished[0] = true
        }

        val intent = Intent()
        intent.putExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT", locationResult)

        mockkStatic(CTGeofenceAPI::class) {
            every { CTGeofenceAPI.getLogger() } returns logger

            spy.onReceive(application, intent)

            await().until { isFinished[0] }

            verifyOrder {
                logger.debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Location updates receiver called"
                )
                logger.debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Returning from Location Updates Receiver"
                )
            }
            verify { pendingResult.finish() }
        }
    }

    @Test
    fun testOnReceiveWhenLocationResultIsNull() {
        val receiver = CTLocationUpdateReceiver()
        val spy = spyk(receiver)
        every { spy.goAsync() } returns pendingResult

        spy.onReceive(application, null)

        verify { pendingResult.finish() }
    }
}
