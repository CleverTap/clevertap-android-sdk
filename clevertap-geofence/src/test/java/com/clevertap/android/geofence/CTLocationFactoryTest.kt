package com.clevertap.android.geofence

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class CTLocationFactoryTest : BaseTestCase() {

    @Mock
    lateinit var googleApiAvailability: GoogleApiAvailability

    @Before
    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        super.setUp()
    }

    @Test
    fun testCreateLocationAdapterTC1() {
        // when all dependencies available
        Mockito.mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean>(Utils::isFusedLocationApiDependencyAvailable)
                .thenReturn(true)

            Mockito.mockStatic(
                GoogleApiAvailability::class.java
            ).use { googleApiAvailabilityMockedStatic ->
                googleApiAvailabilityMockedStatic.`when`<GoogleApiAvailability>(
                    GoogleApiAvailability::getInstance
                ).thenReturn(googleApiAvailability)
                Mockito.`when`(googleApiAvailability.isGooglePlayServicesAvailable(application))
                    .thenReturn(ConnectionResult.SUCCESS)

                val locationAdapter = CTLocationFactory.createLocationAdapter(application)
                Assert.assertNotNull(locationAdapter)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateLocationAdapterTC2() {

        // when play service apk not available
        Mockito.mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean>(Utils::isFusedLocationApiDependencyAvailable)
                .thenReturn(
                    true
                )
            Mockito.mockStatic(
                GoogleApiAvailability::class.java
            ).use { googleApiAvailabilityMockedStatic ->
                googleApiAvailabilityMockedStatic.`when`<GoogleApiAvailability>(
                    GoogleApiAvailability::getInstance
                ).thenReturn(googleApiAvailability)
                Mockito.`when`(googleApiAvailability.isGooglePlayServicesAvailable(application))
                    .thenReturn(ConnectionResult.SERVICE_MISSING)
                CTLocationFactory.createLocationAdapter(application)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateLocationAdapterTC3() {

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

                    CTLocationFactory.createLocationAdapter(application)
                }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateLocationAdapterTC4() {

        // when fused location dependency not available
        Mockito.mockStatic(Utils::class.java).use { utilsMockedStatic ->
            utilsMockedStatic.`when`<Boolean>(Utils::isFusedLocationApiDependencyAvailable)
                .thenReturn(false)
            CTLocationFactory.createLocationAdapter(application)
        }
    }
}
