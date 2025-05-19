package com.clevertap.android.geofence

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import com.clevertap.android.geofence.CTGeofenceAPI.GEOFENCE_LOG_TAG
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class CTGeofenceBootReceiverTest : BaseTestCase() {

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
    fun testOnReceiveWhenIntentIstNull() {
        val receiver = CTGeofenceBootReceiver()
        val spy = Mockito.spy(receiver)

        spy.onReceive(application, null)

        verify(spy, never()).goAsync()
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC1() {
        // when initCTGeofenceApiIfRequired return true

        val receiver = CTGeofenceBootReceiver()
        val spy = Mockito.spy(receiver)
        `when`(spy.goAsync()).thenReturn(pendingResult)

        val isFinished = arrayOf(false)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        doAnswer { invocation ->
            isFinished[0] = true
            null
        }.`when`(pendingResult).finish()

        mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean> {
                Utils.hasPermission(
                    application, Manifest.permission.ACCESS_FINE_LOCATION
                )
            }.thenReturn(true)
            utilsMockedStatic.`when`<Boolean> { Utils.hasBackgroundLocationPermission(application) }
                .thenReturn(true)
            utilsMockedStatic.`when`<Boolean> { Utils.initCTGeofenceApiIfRequired(application) }
                .thenReturn(true)

            mockStatic(CTGeofenceAPI::class.java).use { ctGeofenceAPIMockedStatic ->
                ctGeofenceAPIMockedStatic.`when`<Logger>(CTGeofenceAPI::getLogger)
                    .thenReturn(logger)

                spy.onReceive(application, intent)
                await().until { isFinished[0] }
                verify(CTGeofenceAPI.getLogger()).debug(
                    GEOFENCE_LOG_TAG, "onReceive called after " + "device reboot"
                )
                verify(pendingResult).finish()
            }
        }
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC2() {
        // when initCTGeofenceApiIfRequired return false
        val receiver = CTGeofenceBootReceiver()
        val spy = Mockito.spy(receiver)
        `when`(spy.goAsync()).thenReturn(pendingResult)

        val isFinished = arrayOf(false)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        doAnswer { invocation ->
            isFinished[0] = true
            null
        }.`when`(pendingResult).finish()

        mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean> {
                Utils.hasPermission(
                    application, Manifest.permission.ACCESS_FINE_LOCATION
                )
            }.thenReturn(true)
            utilsMockedStatic.`when`<Boolean> { Utils.hasBackgroundLocationPermission(application) }
                .thenReturn(true)
            utilsMockedStatic.`when`<Boolean> { Utils.initCTGeofenceApiIfRequired(application) }
                .thenReturn(false)

            mockStatic(CTGeofenceAPI::class.java).use { ctGeofenceAPIMockedStatic ->
                ctGeofenceAPIMockedStatic.`when`<Logger>(CTGeofenceAPI::getLogger)
                    .thenReturn(logger)
                spy.onReceive(application, intent)
                await().until { isFinished[0] }

                verify(CTGeofenceAPI.getLogger()).debug(
                    GEOFENCE_LOG_TAG, "onReceive called after " + "device reboot"
                )
                verifyNoMoreInteractions(CTGeofenceAPI.getLogger())
                verify(pendingResult).finish()
            }
        }
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC3() {
        // when ACCESS_FINE_LOCATION permission missing
        val receiver = CTGeofenceBootReceiver()
        val spy = Mockito.spy(receiver)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean> {
                Utils.hasPermission(
                    application, Manifest.permission.ACCESS_FINE_LOCATION
                )
            }.thenReturn(false)
            mockStatic(CTGeofenceAPI::class.java).use { ctGeofenceAPIMockedStatic ->
                ctGeofenceAPIMockedStatic.`when`<Logger>(CTGeofenceAPI::getLogger)
                    .thenReturn(logger)
                spy.onReceive(application, intent)
                verify(CTGeofenceAPI.getLogger()).debug(
                    GEOFENCE_LOG_TAG, "onReceive called after " + "device reboot"
                )
                verify(CTGeofenceAPI.getLogger()).debug(
                    GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_FINE_LOCATION permission! Not registering " + "geofences and location updates after device reboot"
                )
                verify(spy, never()).goAsync()
            }
        }
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC4() {
        // when ACCESS_BACKGROUND_LOCATION permission missing
        val receiver = CTGeofenceBootReceiver()
        val spy = Mockito.spy(receiver)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean> {
                Utils.hasPermission(
                    application, Manifest.permission.ACCESS_FINE_LOCATION
                )
            }.thenReturn(true)
            utilsMockedStatic.`when`<Boolean> { Utils.hasBackgroundLocationPermission(application) }
                .thenReturn(false)
            mockStatic(CTGeofenceAPI::class.java).use { ctGeofenceAPIMockedStatic ->
                ctGeofenceAPIMockedStatic.`when`<Logger>(CTGeofenceAPI::getLogger)
                    .thenReturn(logger)
                spy.onReceive(application, intent)
                verify(CTGeofenceAPI.getLogger()).debug(
                    GEOFENCE_LOG_TAG, "onReceive called after " + "device reboot"
                )
                verify(CTGeofenceAPI.getLogger()).debug(
                    GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_BACKGROUND_LOCATION permission! not registering " + "geofences and location updates after device reboot"
                )
                spy.onReceive(application, intent)
                verify(spy, never()).goAsync()
            }
        }
    }
}
