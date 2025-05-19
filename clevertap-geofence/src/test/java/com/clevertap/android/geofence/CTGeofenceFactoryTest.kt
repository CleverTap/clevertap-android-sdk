package com.clevertap.android.geofence

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class CTGeofenceFactoryTest : BaseTestCase() {

    @Mock
    lateinit var googleApiAvailability: GoogleApiAvailability

    @Before
    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        super.setUp()
    }

    @Test
    fun testCreateGeofenceAdapterTC1() {
        // when all dependencies available

        Mockito.mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean>(Utils::isFusedLocationApiDependencyAvailable)
                .thenReturn(true)

            Mockito.mockStatic(GoogleApiAvailability::class.java)
                .use { googleApiAvailabilityMockedStatic ->
                    googleApiAvailabilityMockedStatic.`when`<GoogleApiAvailability>(
                        GoogleApiAvailability::getInstance
                    ).thenReturn(googleApiAvailability)
                    Mockito.`when`(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SUCCESS)
                    val geofenceAdapter = CTGeofenceFactory.createGeofenceAdapter(application)
                    Assert.assertNotNull(geofenceAdapter)
                }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateGeofenceAdapterTC2() {
        // when play service apk not available

        Mockito.mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean>(Utils::isFusedLocationApiDependencyAvailable)
                .thenReturn(true)
            Mockito.mockStatic(GoogleApiAvailability::class.java)
                .use { googleApiAvailabilityMockedStatic ->
                    googleApiAvailabilityMockedStatic.`when`<GoogleApiAvailability>(
                        GoogleApiAvailability::getInstance
                    ).thenReturn(googleApiAvailability)
                    Mockito.`when`(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SERVICE_MISSING)
                    CTGeofenceFactory.createGeofenceAdapter(application)
                }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateGeofenceAdapterTC3() {
        // when play service apk is disabled

        Mockito.mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean>(Utils::isFusedLocationApiDependencyAvailable)
                .thenReturn(true)
            Mockito.mockStatic(GoogleApiAvailability::class.java)
                .use { googleApiAvailabilityMockedStatic ->
                    googleApiAvailabilityMockedStatic.`when`<GoogleApiAvailability>(
                        GoogleApiAvailability::getInstance
                    ).thenReturn(googleApiAvailability)
                    Mockito.`when`(
                        googleApiAvailability.isGooglePlayServicesAvailable(
                            application
                        )
                    ).thenReturn(ConnectionResult.SERVICE_DISABLED)
                    CTGeofenceFactory.createGeofenceAdapter(application)
                }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateGeofenceAdapterTC4() {
        // when fused location dependency not available
        Mockito.mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean>(Utils::isFusedLocationApiDependencyAvailable)
                .thenReturn(false)
            CTGeofenceFactory.createGeofenceAdapter(application)
        }
    }
}
