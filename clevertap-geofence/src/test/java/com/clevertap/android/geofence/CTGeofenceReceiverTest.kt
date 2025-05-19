package com.clevertap.android.geofence

import android.content.BroadcastReceiver
import android.content.Intent
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class CTGeofenceReceiverTest : BaseTestCase() {

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    lateinit var pendingResult: BroadcastReceiver.PendingResult

    @Mock
    lateinit var logger: Logger

    @Before
    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        super.setUp()
    }

    @Test
    fun testOnReceiveWhenIntentIsNull() {
        val receiver = CTGeofenceReceiver()
        val spy = Mockito.spy(receiver)
        spy.onReceive(application, null)
        verify(spy, never()).goAsync()
    }

    @Test
    fun testOnReceiveWhenIntentNotNull() {
        val receiver = CTGeofenceReceiver()
        val spy = Mockito.spy(receiver)
        `when`(spy.goAsync()).thenReturn(pendingResult)

        val isFinished = arrayOf(false)

        doAnswer { invocation ->
            isFinished[0] = true
            null
        }.`when`(pendingResult).finish()

        mockStatic(CTGeofenceAPI::class.java).use { ctGeofenceAPIMockedStatic ->
            ctGeofenceAPIMockedStatic.`when`<Logger>(CTGeofenceAPI::getLogger).thenReturn(logger)

            val intent = Intent()
            spy.onReceive(application, intent)

            await().until { isFinished[0] }

            verify(CTGeofenceAPI.getLogger()).debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence receiver called"
            )
            verify(CTGeofenceAPI.getLogger()).debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG, "Returning from Geofence receiver"
            )
            verify(pendingResult).finish()
        }
    }
}
