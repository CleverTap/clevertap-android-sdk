package com.clevertap.android.geofence;

import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import org.junit.*;
import org.mockito.*;

public class CTGeofenceFactoryTest extends BaseTestCase {

    @Mock
    public GoogleApiAvailability googleApiAvailability;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);
        super.setUp();
    }

    @Test
    public void testCreateGeofenceAdapterTC1() {

        // when all dependencies available

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);

            try (MockedStatic<GoogleApiAvailability> googleApiAvailabilityMockedStatic = Mockito.mockStatic(
                    GoogleApiAvailability.class)) {
                googleApiAvailabilityMockedStatic.when(GoogleApiAvailability::getInstance)
                        .thenReturn(googleApiAvailability);
                Mockito.when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SUCCESS);
                CTGeofenceAdapter geofenceAdapter = CTGeofenceFactory.createGeofenceAdapter(application);
                Assert.assertNotNull(geofenceAdapter);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateGeofenceAdapterTC2() {
        // when play service apk not available

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);
            try (MockedStatic<GoogleApiAvailability> googleApiAvailabilityMockedStatic = Mockito.mockStatic(
                    GoogleApiAvailability.class)) {
                googleApiAvailabilityMockedStatic.when(GoogleApiAvailability::getInstance)
                        .thenReturn(googleApiAvailability);
                Mockito.when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SERVICE_MISSING);
                CTGeofenceFactory.createGeofenceAdapter(application);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateGeofenceAdapterTC3() {
        // when play service apk is disabled

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);
            try (MockedStatic<GoogleApiAvailability> googleApiAvailabilityMockedStatic = Mockito.mockStatic(
                    GoogleApiAvailability.class)) {
                googleApiAvailabilityMockedStatic.when(GoogleApiAvailability::getInstance)
                        .thenReturn(googleApiAvailability);
                Mockito.when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SERVICE_DISABLED);
                CTGeofenceFactory.createGeofenceAdapter(application);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateGeofenceAdapterTC4() {
        // when fused location dependency not available

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);
            CTGeofenceFactory.createGeofenceAdapter(application);
        }
    }
}
