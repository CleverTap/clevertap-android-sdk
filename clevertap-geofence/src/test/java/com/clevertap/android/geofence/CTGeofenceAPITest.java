package com.clevertap.android.geofence;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LATITUDE;
import static com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LONGITUDE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.Manifest;
import android.app.PendingIntent;
import android.location.Location;
import com.clevertap.android.geofence.fakes.GeofenceJSON;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.interfaces.CTGeofenceEventsListener;
import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener;
import com.clevertap.android.sdk.CleverTapAPI;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import org.json.JSONObject;
import org.junit.*;
import org.mockito.*;

public class CTGeofenceAPITest extends BaseTestCase {

    @Mock
    public CleverTapAPI cleverTapAPI;

    @Mock
    public ExecutorService executorService;

    @Mock
    public CTGeofenceAdapter geofenceAdapter;

    @Mock
    public CTLocationAdapter locationAdapter;

    private MockedStatic<CTGeofenceFactory> ctGeofenceFactoryMockedStatic;

    private MockedStatic<CTLocationFactory> ctLocationFactoryMockedStatic;

    private MockedStatic<Utils> utilsMockedStatic;

    @After
    public void cleanup() throws NoSuchFieldException, IllegalAccessException {
        CTGeofenceAPI instance = CTGeofenceAPI.getInstance(application);
        Field field = CTGeofenceAPI.class.getDeclaredField("ctGeofenceAPI");
        field.setAccessible(true);
        field.set(instance, null);

        CTGeofenceTaskManager taskManagerInstance = CTGeofenceTaskManager.getInstance();
        Field fieldTaskManager = CTGeofenceTaskManager.class.getDeclaredField("taskManager");
        fieldTaskManager.setAccessible(true);
        fieldTaskManager.set(taskManagerInstance, null);

        ctLocationFactoryMockedStatic.close();
        ctGeofenceFactoryMockedStatic.close();
        utilsMockedStatic.close();

    }

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);
        super.setUp();

        ctLocationFactoryMockedStatic = mockStatic(CTLocationFactory.class);
        ctLocationFactoryMockedStatic.when(() -> CTLocationFactory.createLocationAdapter(application))
                .thenReturn(locationAdapter);

        ctGeofenceFactoryMockedStatic = mockStatic(CTGeofenceFactory.class);
        ctGeofenceFactoryMockedStatic.when(() -> CTGeofenceFactory.createGeofenceAdapter(application))
                .thenReturn(geofenceAdapter);

        utilsMockedStatic = Mockito.mockStatic(Utils.class);
    }

    @Test
    public void testDeactivate() {

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
            ctGeofenceAPI.deactivate();
            ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(executorService).submit(argumentCaptor.capture());

            PendingIntent geofenceMonitoring = PendingIntentFactory.getPendingIntent(application,
                    PendingIntentFactory.PENDING_INTENT_GEOFENCE, FLAG_UPDATE_CURRENT);
            PendingIntent locationUpdates = PendingIntentFactory.getPendingIntent(application,
                    PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_UPDATE_CURRENT);

            argumentCaptor.getValue().run();

            verify(geofenceAdapter).stopGeofenceMonitoring(geofenceMonitoring);
            verify(locationAdapter).removeLocationUpdates(locationUpdates);

            fileUtilsMockedStatic.verify(
                    () -> FileUtils.deleteDirectory(any(), FileUtils.getCachedDirName(application)));

            assertFalse(ctGeofenceAPI.isActivated());
        }
    }

    @Test
    public void testGetInstance() {
        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        Assert.assertNotNull(ctGeofenceAPI);

        CTGeofenceAPI ctGeofenceAPI1 = CTGeofenceAPI.getInstance(application);

        Assert.assertSame(ctGeofenceAPI, ctGeofenceAPI1);
    }

    @Test
    public void testGetLogger() {
        assertNotNull(CTGeofenceAPI.getLogger());
    }

    @Test
    public void testHandleGeoFencesTC1() {

        // when location access permission is not granted

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(false);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.getGeofence());

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    public void testHandleGeoFencesTC2() {

        // when background location permission is not granted

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(true);
        when(Utils.hasBackgroundLocationPermission(application)).thenReturn(false);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.getGeofence());

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    public void testHandleGeoFencesTC3() {

        // when geofence list is null

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(true);
        when(Utils.hasBackgroundLocationPermission(application)).thenReturn(true);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.handleGeoFences(null);

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    public void testHandleGeoFencesTC4() {

        // when location access permission and background location permission is granted

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(true);
        when(Utils.hasBackgroundLocationPermission(application)).thenReturn(true);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.getGeofence());

        verify(executorService).submit(any(Runnable.class));
    }

    @Test
    public void testInitBackgroundLocationUpdatesTC1() {

        // when location access permission is denied

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(false);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.initBackgroundLocationUpdates();

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testInitBackgroundLocationUpdatesTC2() {

        // when geofence init is not called

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(true);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.initBackgroundLocationUpdates();

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    public void testInitBackgroundLocationUpdatesTC3() {

        // when geofence sdk initialized and location access permission is granted

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(true);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.init(null, cleverTapAPI);

        verify(executorService).submit(any(Runnable.class));
    }

    @Test
    public void testInitDefaultConfig() {
//        CTGeofenceSettings actual = CTGeofenceAPI.getInstance(application).initDefaultConfig();
//        assertThat(actual, samePropertyValuesAs(new CTGeofenceSettings.Builder().build()));
    }

    @Test
    public void testInitTC1() {
        // when location adapter is null
        when(CTLocationFactory.createLocationAdapter(application)).thenReturn(null);
        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);

        ctGeofenceAPI.init(null, cleverTapAPI);
        Mockito.verifyNoMoreInteractions(cleverTapAPI);
    }

    @Test
    public void testInitTC2() {
        // when geofence adapter is null
        when(CTGeofenceFactory.createGeofenceAdapter(application)).thenReturn(null);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);

        ctGeofenceAPI.init(null, cleverTapAPI);
        Mockito.verifyNoMoreInteractions(cleverTapAPI);
    }

    @Test(expected = NullPointerException.class)
    public void testInitTC3() {
        // when cleverTapAPI is null

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);

        ctGeofenceAPI.init(null, null);
    }

    @Test
    public void testInitTC4() {
        // when Settings is null
//        ShadowApplication shadowApplication = Shadows.shadowOf(application);
//        shadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
//
//        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
//
//        ctGeofenceAPI.init(null, cleverTapAPI);
//        assertThat(ctGeofenceAPI.getGeofenceSettings(), samePropertyValuesAs(ctGeofenceAPI.initDefaultConfig()));
//        verify(cleverTapAPI).setGeofenceCallback(any(GeofenceCallback.class));
    }

    @Test
    public void testInitTC5() {
        // when Settings is not null

//        ShadowApplication shadowApplication = Shadows.shadowOf(application);
//        shadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
//
//        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
//
//        CTGeofenceSettings expected = new CTGeofenceSettings.Builder().setId("12345")
//                .setInterval(5000000).setFastestInterval(5000000).enableBackgroundLocationUpdates(false)
//                .setGeofenceMonitoringCount(90).build();
//
//        ctGeofenceAPI.init(expected, cleverTapAPI);
//        assertThat(ctGeofenceAPI.getGeofenceSettings(), samePropertyValuesAs(expected));
//        verify(cleverTapAPI).setGeofenceCallback(any(GeofenceCallback.class));
    }

    @Test
    public void testProcessTriggeredLocationTC1() {
        // when delta t and delta d both is satisfied

        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE);
        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE);
        GeofenceStorageHelper.putLong(application, CTGeofenceConstants.KEY_LAST_LOCATION_EP,
                System.currentTimeMillis() - 2400000);// move to past by 40 minutes

        Location location = new Location("");
        double newLat = 19.23041616;
        double newLng = 72.82488101;

        location.setLatitude(newLat);
        location.setLongitude(newLng);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        ctGeofenceAPI.init(null, cleverTapAPI);
        ctGeofenceAPI.processTriggeredLocation(location);

        double actualLat = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LATITUDE, 0);
        double actualLong = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LONGITUDE, 0);
        long actualEP = GeofenceStorageHelper.getLong(application
                , CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0);

        assertEquals(newLat, actualLat, 0);
        assertEquals(newLng, actualLong, 0);

        verify(cleverTapAPI).setLocationForGeofences(any(Location.class), anyInt());
    }

    @Test
    public void testProcessTriggeredLocationTC2() {
        // when delta t is satisfied

        double lastPingedLat = 19.23051746;
        double lastPingedLng = 72.82425874;

        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LATITUDE, lastPingedLat);
        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LONGITUDE, lastPingedLng);
        GeofenceStorageHelper.putLong(application, CTGeofenceConstants.KEY_LAST_LOCATION_EP,
                System.currentTimeMillis() - 2400000);// move to past by 40 minutes

        Location location = new Location("");
        location.setLatitude(19.23041616);
        location.setLongitude(72.82488101);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        ctGeofenceAPI.init(null, cleverTapAPI);
        ctGeofenceAPI.processTriggeredLocation(location);

        double actualLat = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LATITUDE, 0);
        double actualLong = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LONGITUDE, 0);
        long actualEP = GeofenceStorageHelper.getLong(application
                , CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0);

        assertEquals(lastPingedLat, actualLat, 0);
        assertEquals(lastPingedLng, actualLong, 0);

        verify(cleverTapAPI, never()).setLocationForGeofences(any(Location.class), anyInt());
    }

    @Test
    public void testProcessTriggeredLocationTC3() {
        // when delta d is satisfied

        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE);
        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE);
        GeofenceStorageHelper.putLong(application, CTGeofenceConstants.KEY_LAST_LOCATION_EP,
                System.currentTimeMillis() - 1500000);// move to pas by 25 minutes

        Location location = new Location("");
        location.setLatitude(19.23041616);
        location.setLongitude(72.82488101);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        ctGeofenceAPI.init(null, cleverTapAPI);
        ctGeofenceAPI.processTriggeredLocation(location);

        double actualLat = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LATITUDE, 0);
        double actualLong = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LONGITUDE, 0);
        long actualEP = GeofenceStorageHelper.getLong(application
                , CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0);

        assertEquals(DEFAULT_LATITUDE, actualLat, 0);
        assertEquals(DEFAULT_LONGITUDE, actualLong, 0);
        verify(cleverTapAPI, never()).setLocationForGeofences(any(Location.class), anyInt());
    }

    @Test
    public void testProcessTriggeredLocationTC4() {
        // when delta t is not satisfied and delta d is not satisfied

        double lastPingedLat = 19.23051746;
        double lastPingedLng = 72.82425874;

        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LATITUDE, lastPingedLat);
        GeofenceStorageHelper.putDouble(application, CTGeofenceConstants.KEY_LONGITUDE, lastPingedLng);
        GeofenceStorageHelper.putLong(application, CTGeofenceConstants.KEY_LAST_LOCATION_EP,
                System.currentTimeMillis() - 1500000);// move to pas by 25 minutes

        Location location = new Location("");
        location.setLatitude(19.23041616);
        location.setLongitude(72.82488101);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        ctGeofenceAPI.init(null, cleverTapAPI);
        ctGeofenceAPI.processTriggeredLocation(location);

        double actualLat = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LATITUDE, 0);
        double actualLong = GeofenceStorageHelper.getDouble(application,
                CTGeofenceConstants.KEY_LONGITUDE, 0);
        long actualEP = GeofenceStorageHelper.getLong(application
                , CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0);

        assertEquals(lastPingedLat, actualLat, 0);
        assertEquals(lastPingedLng, actualLong, 0);
        verify(cleverTapAPI, never()).setLocationForGeofences(any(Location.class), anyInt());
    }

    @Test
    public void testSetCtGeofenceEventsListener() {
        CTGeofenceEventsListener listener = new CTGeofenceEventsListener() {
            @Override
            public void onGeofenceEnteredEvent(JSONObject geofenceEnteredEventProperties) {

            }

            @Override
            public void onGeofenceExitedEvent(JSONObject geofenceExitedEventProperties) {

            }
        };

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        ctGeofenceAPI.setCtGeofenceEventsListener(listener);
        assertSame(listener, ctGeofenceAPI.getCtGeofenceEventsListener());
    }

    @Test
    public void testSetCtLocationUpdatesListener() {
        CTLocationUpdatesListener listener = location -> {

        };

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        ctGeofenceAPI.setCtLocationUpdatesListener(listener);
        assertSame(listener, ctGeofenceAPI.getCtLocationUpdatesListener());
    }

    @Test
    public void testTriggerLocationTC1() {

        // when location access permission is denied

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(false);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.triggerLocation();

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    public void testTriggerLocationTC2() {

        // when geofence init is not called

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(true);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.triggerLocation();

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    public void testTriggerLocationTC3() {

        // when geofence sdk initialized and location access permission is granted

        when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(true);

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(application);
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService);
        ctGeofenceAPI.init(null, cleverTapAPI);

        ctGeofenceAPI.triggerLocation();

        verify(executorService, times(2)).submit(any(Runnable.class));
    }

}
