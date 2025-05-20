package com.clevertap.android.geofence

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import com.clevertap.android.geofence.CTGeofenceAPI.GEOFENCE_LOG_TAG
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test

class CTGeofenceBootReceiverTest : BaseTestCase() {

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
    fun testOnReceiveWhenIntentIstNull() {
        val receiver = CTGeofenceBootReceiver()
        val spy = spyk(receiver)

        spy.onReceive(application, null)

        verify(exactly = 0) { spy.goAsync() }
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC1() {
        // when initCTGeofenceApiIfRequired return true

        val receiver = CTGeofenceBootReceiver()
        val spy = spyk(receiver)
        every { spy.goAsync() } returns pendingResult

        val isFinished = arrayOf(false)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        every { pendingResult.finish() } answers {
            isFinished[0] = true
        }

        mockkStatic(Utils::class) {
            every {
                Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
            } returns true
            every { Utils.hasBackgroundLocationPermission(application) } returns true
            every { Utils.initCTGeofenceApiIfRequired(application) } returns true

            mockkStatic(CTGeofenceAPI::class) {
                every { CTGeofenceAPI.getLogger() } returns logger

                spy.onReceive(application, intent)
                await().until { isFinished[0] }

                verify {
                    logger.debug(GEOFENCE_LOG_TAG, "onReceive called after device reboot")
                }
                verify { pendingResult.finish() }
            }
        }
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC2() {
        // when initCTGeofenceApiIfRequired return false
        val receiver = CTGeofenceBootReceiver()
        val spy = spyk(receiver)
        every { spy.goAsync() } returns pendingResult

        val isFinished = arrayOf(false)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        every { pendingResult.finish() } answers {
            isFinished[0] = true
        }

        mockkStatic(Utils::class) {
            every {
                Utils.hasPermission(
                    application, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } returns true
            every { Utils.hasBackgroundLocationPermission(application) } returns true
            every { Utils.initCTGeofenceApiIfRequired(application) } returns false

            mockkStatic(CTGeofenceAPI::class) {
                every { CTGeofenceAPI.getLogger() } returns logger

                spy.onReceive(application, intent)
                await().until { isFinished[0] }

                verify {
                    logger.debug(GEOFENCE_LOG_TAG, "onReceive called after device reboot")
                }

                verify { pendingResult.finish() }
            }
        }
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC3() {
        // when ACCESS_FINE_LOCATION permission missing
        val receiver = CTGeofenceBootReceiver()
        val spy = spyk(receiver)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        mockkStatic(Utils::class) {
            every {
                Utils.hasPermission(
                    application, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } returns false

            mockkStatic(CTGeofenceAPI::class) {
                every { CTGeofenceAPI.getLogger() } returns logger

                spy.onReceive(application, intent)

                verifyOrder {
                    logger.debug(GEOFENCE_LOG_TAG, "onReceive called after device reboot")
                    logger.debug(
                        GEOFENCE_LOG_TAG,
                        "We don't have ACCESS_FINE_LOCATION permission! Not registering geofences and location updates after device reboot"
                    )
                }
                verify(exactly = 0) { spy.goAsync() }
            }
        }
    }

    @Test
    fun testOnReceiveWhenIntentNotNullTC4() {
        // when ACCESS_BACKGROUND_LOCATION permission missing
        val receiver = CTGeofenceBootReceiver()
        val spy = spyk(receiver)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        mockkStatic(Utils::class) {
            every {
                Utils.hasPermission(
                    application, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } returns true
            every { Utils.hasBackgroundLocationPermission(application) } returns false

            mockkStatic(CTGeofenceAPI::class) {
                every { CTGeofenceAPI.getLogger() } returns logger

                spy.onReceive(application, intent)

                verifyOrder {
                    logger.debug(
                        GEOFENCE_LOG_TAG, "onReceive called after device reboot"
                    )
                    logger.debug(
                        GEOFENCE_LOG_TAG,
                        "We don't have ACCESS_BACKGROUND_LOCATION permission! not registering geofences and location updates after device reboot"
                    )
                }

                spy.onReceive(application, intent)
                verify(exactly = 0) { spy.goAsync() }
            }
        }
    }
}
