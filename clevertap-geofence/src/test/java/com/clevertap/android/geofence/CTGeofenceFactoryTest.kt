package com.clevertap.android.geofence

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CTGeofenceFactoryTest : BaseTestCase() {

    private lateinit var googleApiAvailability: GoogleApiAvailability

    @Before
    override fun setUp() {
        super.setUp()
        googleApiAvailability = mockk(relaxed = true)
    }

    @Test
    fun testCreateGeofenceAdapterTC1() {
        // when all dependencies available

        mockkStatic(Utils::class) {
            every { Utils.isFusedLocationApiDependencyAvailable() } returns true

            mockkStatic(GoogleApiAvailability::class) {
                every { GoogleApiAvailability.getInstance() } returns googleApiAvailability
                every { googleApiAvailability.isGooglePlayServicesAvailable(application) } returns ConnectionResult.SUCCESS

                val geofenceAdapter = CTGeofenceFactory.createGeofenceAdapter(application)
                Assert.assertNotNull(geofenceAdapter)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateGeofenceAdapterTC2() {
        // when play service apk not available

        mockkStatic(Utils::class) {
            every { Utils.isFusedLocationApiDependencyAvailable() } returns true

            mockkStatic(GoogleApiAvailability::class) {
                every { GoogleApiAvailability.getInstance() } returns googleApiAvailability
                every { googleApiAvailability.isGooglePlayServicesAvailable(application) } returns ConnectionResult.SERVICE_MISSING

                CTGeofenceFactory.createGeofenceAdapter(application)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateGeofenceAdapterTC3() {
        // when play service apk is disabled

        mockkStatic(Utils::class) {
            every { Utils.isFusedLocationApiDependencyAvailable() } returns true

            mockkStatic(GoogleApiAvailability::class) {
                every { GoogleApiAvailability.getInstance() } returns googleApiAvailability
                every { googleApiAvailability.isGooglePlayServicesAvailable(application) } returns ConnectionResult.SERVICE_DISABLED

                CTGeofenceFactory.createGeofenceAdapter(application)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateGeofenceAdapterTC4() {
        // when fused location dependency not available

        mockkStatic(Utils::class) {
            every { Utils.isFusedLocationApiDependencyAvailable() } returns false

            CTGeofenceFactory.createGeofenceAdapter(application)
        }
    }
}
