package com.clevertap.android.geofence;

import static org.powermock.api.mockito.PowerMockito.when;

import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
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
public class CTLocationFactoryTest extends BaseTestCase {

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
    public void testCreateLocationAdapterTC1() {

        // when all dependencies available
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(true);
        when(googleApiAvailability.isGooglePlayServicesAvailable(application)).thenReturn(ConnectionResult.SUCCESS);

        CTLocationAdapter LocationAdapter = CTLocationFactory.createLocationAdapter(application);
        Assert.assertNotNull(LocationAdapter);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateLocationAdapterTC2() {

        // when play service apk not available
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(true);
        when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                .thenReturn(ConnectionResult.SERVICE_MISSING);

        CTLocationFactory.createLocationAdapter(application);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateLocationAdapterTC3() {

        // when play service apk is disabled
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(true);
        when(googleApiAvailability.isGooglePlayServicesAvailable(application))
                .thenReturn(ConnectionResult.SERVICE_DISABLED);

        CTLocationFactory.createLocationAdapter(application);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateLocationAdapterTC4() {

        // when fused location dependency not available
        when(Utils.isFusedLocationApiDependencyAvailable()).thenReturn(false);
        CTLocationFactory.createLocationAdapter(application);
    }

}
