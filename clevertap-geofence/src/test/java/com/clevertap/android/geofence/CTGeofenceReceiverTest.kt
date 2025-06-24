package com.clevertap.android.geofence

import android.content.BroadcastReceiver
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test

class CTGeofenceReceiverTest : BaseTestCase() {

    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var pendingResult: BroadcastReceiver.PendingResult
    private lateinit var logger: Logger

    @Before
    override fun setUp() {
        super.setUp()
        ctGeofenceAPI = mockk(relaxed = true)
        pendingResult = mockk(relaxed = true)
        logger = mockk(relaxed = true)
    }

    @Test
    fun testOnReceiveWhenIntentIsNull() {
        val receiver = CTGeofenceReceiver()
        val spy = spyk(receiver)

        spy.onReceive(application, null)

        verify(exactly = 0) { spy.goAsync() }
    }

    @Test
    fun testOnReceiveWhenIntentNotNull() {
        val receiver = CTGeofenceReceiver()
        val spy = spyk(receiver)
        every { spy.goAsync() } returns pendingResult

        val isFinished = arrayOf(false)

        every { pendingResult.finish() } answers {
            isFinished[0] = true
        }

        mockkStatic(CTGeofenceAPI::class) {
            every { CTGeofenceAPI.getLogger() } returns logger

            val intent = Intent()
            spy.onReceive(application, intent)

            await().until { isFinished[0] }

            verifyOrder {
                logger.debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence receiver called"
                )
                logger.debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG, "Returning from Geofence receiver"
                )
            }
            verify { pendingResult.finish() }
        }
    }
}
