package com.clevertap.android.geofence;


import static org.powermock.api.mockito.PowerMockito.when;

import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Ignore
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28,
        application = TestApplication.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*"})
@PrepareForTest({Utils.class, GoogleApiAvailability.class})
public class CTGeofenceFactoryTest extends BaseTestCase {

    @Mock
    public GoogleApiAvailability googleApiAvailability;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Utils.class, GoogleApiAvailability.class);
        super.setUp();
        when(GoogleApiAvailability.getInstance()).thenReturn(googleApiAvailability);
    }

    @Test
    public void testCreateGeofenceAdapterTC1() {

        // when all dependencies available
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(true);
        when(googleApiAvailability.isGooglePlayServicesAvailable(application)).thenReturn(ConnectionResult.SUCCESS);

        CTGeofenceAdapter geofenceAdapter = CTGeofenceFactory.createGeofenceAdapter(application);
        Assert.assertNotNull(geofenceAdapter);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateGeofenceAdapterTC2() {

        // when play service apk not available
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(true);
        when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                .thenReturn(ConnectionResult.SERVICE_MISSING);

        CTGeofenceFactory.createGeofenceAdapter(application);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateGeofenceAdapterTC3() {

        // when play service apk is disabled
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(true);
        when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                .thenReturn(ConnectionResult.SERVICE_DISABLED);

        CTGeofenceFactory.createGeofenceAdapter(application);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateGeofenceAdapterTC4() {

        // when fused location dependency not available
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(false);
        CTGeofenceFactory.createGeofenceAdapter(application);
    }

}
