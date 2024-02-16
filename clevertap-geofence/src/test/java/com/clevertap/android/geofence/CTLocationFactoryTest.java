package com.clevertap.android.geofence;

import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import org.junit.*;
import org.mockito.*;

public class CTLocationFactoryTest extends BaseTestCase {

    @Mock
    public GoogleApiAvailability googleApiAvailability;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);
        super.setUp();
    }

    @Test
    public void testCreateLocationAdapterTC1() {

        // when all dependencies available
        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);

            try (MockedStatic<GoogleApiAvailability> googleApiAvailabilityMockedStatic = Mockito.mockStatic(
                    GoogleApiAvailability.class)) {
                googleApiAvailabilityMockedStatic.when(GoogleApiAvailability::getInstance)
                        .thenReturn(googleApiAvailability);
                Mockito.when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SUCCESS);

                CTLocationAdapter LocationAdapter = CTLocationFactory.createLocationAdapter(application);
                Assert.assertNotNull(LocationAdapter);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateLocationAdapterTC2() {

        // when play service apk not available
        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);
            try (MockedStatic<GoogleApiAvailability> googleApiAvailabilityMockedStatic = Mockito.mockStatic(
                    GoogleApiAvailability.class)) {
                googleApiAvailabilityMockedStatic.when(GoogleApiAvailability::getInstance)
                        .thenReturn(googleApiAvailability);
                Mockito.when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SERVICE_MISSING);
                CTLocationFactory.createLocationAdapter(application);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateLocationAdapterTC3() {

        // when play service apk is disabled
        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);
            try (MockedStatic<GoogleApiAvailability> googleApiAvailabilityMockedStatic = Mockito.mockStatic(
                    GoogleApiAvailability.class)) {
                googleApiAvailabilityMockedStatic.when(GoogleApiAvailability::getInstance)
                        .thenReturn(googleApiAvailability);
                Mockito.when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                        .thenReturn(ConnectionResult.SERVICE_DISABLED);

                CTLocationFactory.createLocationAdapter(application);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateLocationAdapterTC4() {

        // when fused location dependency not available
        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::isFusedLocationApiDependencyAvailable).thenReturn(true);
            CTLocationFactory.createLocationAdapter(application);
        }
    }
}
