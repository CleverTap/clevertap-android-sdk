package com.clevertap.android.geofence;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import android.app.PendingIntent;
import android.content.Context;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import org.junit.*;
import org.mockito.*;

public class LocationUpdateTaskTest extends BaseTestCase {

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTLocationAdapter ctLocationAdapter;

    @Mock
    public CTGeofenceTask.OnCompleteListener onCompleteListener;

    @Mock
    private Logger logger;

    @Mock
    PendingIntent pendingIntent;

    private MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic;

    private MockedStatic<FileUtils> fileUtilsMockedStatic;
    private MockedStatic<Utils> utilsMockedStatic;

    private MockedStatic<PendingIntentFactory> pendingIntentFactoryMockedStatic;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);

        ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class);
        fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class);
        utilsMockedStatic = Mockito.mockStatic(Utils.class);
        pendingIntentFactoryMockedStatic = Mockito.mockStatic(PendingIntentFactory.class);
        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

        when(ctGeofenceAPI.getCtLocationAdapter()).thenReturn(ctLocationAdapter);

    }

    @After
    public void cleanup() {
        ctGeofenceAPIMockedStatic.close();
        fileUtilsMockedStatic.close();
        utilsMockedStatic.close();
        pendingIntentFactoryMockedStatic.close();
    }

    @Test
    public void testExecuteTC1() {
        // when pending intent is null and bgLocationUpdate is true

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true).build();
        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();

        utilsMockedStatic.verify(() -> Utils.writeSettingsToFile(any(Context.class), any(CTGeofenceSettings.class)));

        verify(onCompleteListener).onComplete();
    }

    @Test
    public void testExecuteTC2() {
        // when pending intent is null and bgLocationUpdate is false

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(false).build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter, never()).requestLocationUpdates();
        verify(ctLocationAdapter, never()).removeLocationUpdates(any(PendingIntent.class));

        utilsMockedStatic.verify(() -> Utils.writeSettingsToFile(any(Context.class), any(CTGeofenceSettings.class)));

    }

    @Test
    public void testExecuteTC3() {
        // when pending intent is not null and bgLocationUpdate is false

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(false).build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        // make pending intent non-null
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        // verify that previous update is removed
        verify(ctLocationAdapter).removeLocationUpdates(any(PendingIntent.class));
        verify(ctLocationAdapter, never()).requestLocationUpdates();

        utilsMockedStatic.verify(() -> Utils.writeSettingsToFile(any(Context.class), any(CTGeofenceSettings.class)));

    }

    @Test
    public void testExecuteWhenLocationAdapterIsNull() {
        // when location adapter is null

        when(ctGeofenceAPI.getCtLocationAdapter()).thenReturn(null);
        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        utilsMockedStatic.verifyNoInteractions();

    }

    @Test
    public void testIsRequestLocationTC1() {
        // when currentBgLocationUpdate is false
//
//        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
//                .enableBackgroundLocationUpdates(false).build();
//
//        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
//
//        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder().build();
//
//        LocationUpdateTask task = new LocationUpdateTask(application);
//        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation", null);
//
//        assertFalse(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC10() {
        // when currentBgLocationUpdate is true and there is no change in settings

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(logger).verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Dropping duplicate location update request");

    }

    @Test
    public void testIsRequestLocationTC2() {
        // when currentBgLocationUpdate is true and pendingIntent is null

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true).build();

        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(null);
        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder().build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();

    }

    @Test
    public void testIsRequestLocationTC3() {
        // when fetch mode is current and change in accuracy

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setLocationAccuracy(CTGeofenceSettings.ACCURACY_HIGH)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);


        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationAccuracy(CTGeofenceSettings.ACCURACY_LOW)
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();
    }

    @Test
    public void testIsRequestLocationTC4() {
        // when fetch mode is current and change in interval

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setInterval(2000000)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);


        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setInterval(5000000)
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();
    }

    @Test
    public void testIsRequestLocationTC5() {
        // when fetch mode is current and change in fastest interval

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setFastestInterval(2000000)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setFastestInterval(5000000)
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();

    }

    @Test
    public void testIsRequestLocationTC6() {
        // when fetch mode is current and change in displacement

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setSmallestDisplacement(600)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);


        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setSmallestDisplacement(700)
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();
    }

    @Test
    public void testIsRequestLocationTC7() {
        // when fetch mode is last location and change in interval

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .setInterval(2000000)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setInterval(5000000)
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();
    }

    @Test
    public void testIsRequestLocationTC8() {
        // when fetch mode is current location and change in fetch mode

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();
    }

    @Test
    public void testIsRequestLocationTC9() {
        // when fetch mode is last location and change in fetch mode

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .build();

        when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);
        when(PendingIntentFactory.getPendingIntent(any(Context.class),any(int.class),any(int.class))).thenReturn(pendingIntent);


        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();
    }

}
