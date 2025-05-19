package com.clevertap.android.geofence

import android.content.Context
import android.location.Location
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.sdk.CleverTapAPI
import com.google.android.gms.location.LocationResult
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.Future

class PushLocationEventTaskTest : BaseTestCase() {

    @Mock
    private lateinit var cleverTapAPI: CleverTapAPI

    @Mock
    private lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    private lateinit var onCompleteListener: CTGeofenceTask.OnCompleteListener

    private lateinit var ctGeofenceAPIMockedStatic: MockedStatic<CTGeofenceAPI>

    private lateinit var locationResult: LocationResult

    @Mock
    private lateinit var logger: Logger

    private lateinit var utilsMockedStatic: MockedStatic<Utils>

    @After
    fun cleanup() {
        ctGeofenceAPIMockedStatic.close()
        utilsMockedStatic.close()
    }

    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        super.setUp()
        locationResult = LocationResult.create(listOf(Location("")))
        ctGeofenceAPIMockedStatic = Mockito.mockStatic<CTGeofenceAPI>(CTGeofenceAPI::class.java)
        utilsMockedStatic = Mockito.mockStatic<Utils>(Utils::class.java)
        Mockito.`when`<CTGeofenceAPI?>(CTGeofenceAPI.getInstance(application))
            .thenReturn(ctGeofenceAPI)
        Mockito.`when`<Logger?>(CTGeofenceAPI.getLogger()).thenReturn(logger)
    }

    @Test
    fun testExecuteWhenCleverTapApiIsNull() {
        Mockito.`when`<Boolean?>(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(false)
        val task = PushLocationEventTask(application, locationResult)

        // when listener null
        task.execute()
        Mockito.verifyNoMoreInteractions(onCompleteListener)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        utilsMockedStatic.verify(
            MockedStatic.Verification {
                Utils.notifyLocationUpdates(
                    ArgumentMatchers.any(
                        Context::class.java
                    ), ArgumentMatchers.any(Location::class.java)
                )
            }, Mockito.times(0)
        )
        Mockito.verify(onCompleteListener).onComplete()
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureIsNullAndListenerNotNull() {
        val task = PushLocationEventTask(application, locationResult)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        Mockito.`when`<Boolean?>(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)

        Mockito.`when`(
            cleverTapAPI.setLocationForGeofences(
                ArgumentMatchers.any(
                    Location::class.java
                ), ArgumentMatchers.anyInt()
            )
        ).thenReturn(null)

        task.execute()

        utilsMockedStatic.verify(MockedStatic.Verification {
            Utils.notifyLocationUpdates(
                ArgumentMatchers.any(
                    Context::class.java
                ), ArgumentMatchers.any(Location::class.java)
            )
        })
        Mockito.verify(logger).verbose(
            CTGeofenceAPI.GEOFENCE_LOG_TAG, "Dropping location ping event to CT server"
        )
        Mockito.verify(onCompleteListener).onComplete()
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureIsNullAndListenerNull() {
        val task = PushLocationEventTask(application, locationResult)

        // when listener null
        Mockito.`when`<Boolean?>(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)

        Mockito.`when`(
            cleverTapAPI.setLocationForGeofences(
                ArgumentMatchers.any(Location::class.java),
                ArgumentMatchers.anyInt()
            )
        ).thenReturn(null)

        task.execute()

        utilsMockedStatic.verify(MockedStatic.Verification {
            Utils.notifyLocationUpdates(
                ArgumentMatchers.any(
                    Context::class.java
                ), ArgumentMatchers.any(Location::class.java)
            )
        })
        Mockito.verify(logger).verbose(
            CTGeofenceAPI.GEOFENCE_LOG_TAG, "Dropping location ping event to CT server"
        )
        Mockito.verifyNoMoreInteractions(onCompleteListener)
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureNotNullAndListenerNotNull() {
        val future = Mockito.mock<Future<*>?>(Future::class.java)

        val task = PushLocationEventTask(application, locationResult)

        // when listener not null
        task.setOnCompleteListener(onCompleteListener)
        Mockito.`when`<Boolean?>(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)

        Mockito.`when`(
            ctGeofenceAPI.processTriggeredLocation(ArgumentMatchers.any(Location::class.java))
        ).thenReturn(future)
        task.execute()

        utilsMockedStatic.verify(MockedStatic.Verification {
            Utils.notifyLocationUpdates(
                ArgumentMatchers.any(Context::class.java),
                ArgumentMatchers.any(Location::class.java)
            )
        })
        Mockito.verify(future).get()

        Mockito.verify(logger).verbose(
            CTGeofenceAPI.GEOFENCE_LOG_TAG, "Calling future for setLocationForGeofences()"
        )
        Mockito.verify(onCompleteListener).onComplete()
    }

    @Test
    fun testExecuteWhenCleverTapApiNotNullAndFutureNotNullAndListenerNull() {
        val future = Mockito.mock(Future::class.java)

        val task = PushLocationEventTask(application, locationResult)

        // when listener null
        Mockito.`when`<Boolean?>(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true)

        Mockito.`when`(
            ctGeofenceAPI.processTriggeredLocation(ArgumentMatchers.any(Location::class.java))
        ).thenReturn(future)
        task.execute()

        utilsMockedStatic.verify(MockedStatic.Verification {
            Utils.notifyLocationUpdates(
                ArgumentMatchers.any(Context::class.java),
                ArgumentMatchers.any(Location::class.java)
            )
        })
        Mockito.verify(future).get()

        Mockito.verify(logger).verbose(
            CTGeofenceAPI.GEOFENCE_LOG_TAG, "Calling future for setLocationForGeofences()"
        )
        Mockito.verifyNoMoreInteractions(onCompleteListener)
    }
}
