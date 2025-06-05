package com.clevertap.android.geofence

import android.location.Location
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.sdk.CleverTapAPI
import com.google.android.gms.location.LocationResult
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Test
import java.util.concurrent.Future

class PushLocationEventTaskTest : BaseTestCase() {

    private lateinit var cleverTapAPI: CleverTapAPI
    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var onCompleteListener: CTGeofenceTask.OnCompleteListener
    private lateinit var locationResult: LocationResult
    private lateinit var logger: Logger

    override fun setUp() {
        super.setUp()

        cleverTapAPI = mockk(relaxed = true)
        ctGeofenceAPI = mockk(relaxed = true)
        onCompleteListener = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        locationResult = LocationResult.create(listOf(Location("")))

        mockkStatic(CTGeofenceAPI::class)
        mockkStatic(Utils::class)

        every { CTGeofenceAPI.getInstance(application) } returns ctGeofenceAPI
        every { CTGeofenceAPI.getLogger() } returns logger
    }

    @After
    override fun cleanUp() {
        super.cleanUp()
        unmockkStatic(CTGeofenceAPI::class)
        unmockkStatic(Utils::class)
    }

    @Test
    fun testExecuteWhenCleverTapApiIsNull() {
        every { Utils.initCTGeofenceApiIfRequired(application) } returns false
        val task = PushLocationEventTask(application, locationResult)

        // when listener null
        task.execute()
        confirmVerified(onCompleteListener)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        verify(exactly = 0) { Utils.notifyLocationUpdates(any(), any()) }
        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureIsNullAndListenerNotNull() {
        val task = PushLocationEventTask(application, locationResult)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { cleverTapAPI.setLocationForGeofences(any(), any()) } returns null
        every { ctGeofenceAPI.processTriggeredLocation(any()) } returns null

        task.execute()

        verify { Utils.notifyLocationUpdates(any(), any()) }
        verify {
            logger.verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG, "Dropping location ping event to CT server"
            )
        }
        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureIsNullAndListenerNull() {
        val task = PushLocationEventTask(application, locationResult)

        // when listener null
        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { cleverTapAPI.setLocationForGeofences(any(), any()) } returns null
        every { ctGeofenceAPI.processTriggeredLocation(any()) } returns null

        task.execute()

        verify { Utils.notifyLocationUpdates(any(), any()) }
        verify {
            logger.verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG, "Dropping location ping event to CT server"
            )
        }
        verify(exactly = 0) { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureNotNullAndListenerNotNull() {
        val future: Future<*> = mockk(relaxed = true)

        val task = PushLocationEventTask(application, locationResult)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { ctGeofenceAPI.processTriggeredLocation(any()) } returns future

        task.execute()

        verify { Utils.notifyLocationUpdates(any(), any()) }
        verify { future.get() }

        verify {
            logger.verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG, "Calling future for setLocationForGeofences()"
            )
        }
        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureNotNullAndListenerNull() {
        val future: Future<*> = mockk(relaxed = true)

        val task = PushLocationEventTask(application, locationResult)

        // when listener null
        every { Utils.initCTGeofenceApiIfRequired(application) } returns true
        every { ctGeofenceAPI.processTriggeredLocation(any()) } returns future

        task.execute()

        verify { Utils.notifyLocationUpdates(any(), any()) }
        verify { future.get() }

        verify {
            logger.verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG, "Calling future for setLocationForGeofences()"
            )
        }
        verify(exactly = 0) { onCompleteListener.onComplete() }
    }
}
