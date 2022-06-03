package com.clevertap.android.geofence;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.app.PendingIntent;
import android.content.Context;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.internal.WhiteboxImpl;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28,
        application = TestApplication.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*"})
@PrepareForTest({CTGeofenceAPI.class, Utils.class, FileUtils.class})
public class LocationUpdateTaskTest extends BaseTestCase {

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTLocationAdapter ctLocationAdapter;

    @Mock
    public CTGeofenceTask.OnCompleteListener onCompleteListener;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Logger logger;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CTGeofenceAPI.class, Utils.class, FileUtils.class);

        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        logger = new Logger(Logger.DEBUG);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

        WhiteboxImpl.setInternalState(ctGeofenceAPI, "ctLocationAdapter", ctLocationAdapter);

    }

    @Test
    public void testExecuteTC1() {
        // when pending intent is null and bgLocationUpdate is true

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        verify(ctLocationAdapter).requestLocationUpdates();

        verifyStatic(Utils.class);
        Utils.writeSettingsToFile(any(Context.class), any(CTGeofenceSettings.class));

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

        verifyStatic(Utils.class);
        Utils.writeSettingsToFile(any(Context.class), any(CTGeofenceSettings.class));

    }

    @Test
    public void testExecuteTC3() {
        // when pending intent is not null and bgLocationUpdate is false

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(false).build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        // make pending intent non-null
        PendingIntent pendingIntent = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_LOCATION, PendingIntent.FLAG_UPDATE_CURRENT);

        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        // verify that previous update is removed
        verify(ctLocationAdapter).removeLocationUpdates(any(PendingIntent.class));
        verify(ctLocationAdapter, never()).requestLocationUpdates();

        verifyStatic(Utils.class);
        Utils.writeSettingsToFile(any(Context.class), any(CTGeofenceSettings.class));

    }

    @Test
    public void testExecuteWhenLocationAdapterIsNull() {
        // when location adapter is null

        WhiteboxImpl.setInternalState(ctGeofenceAPI, "ctLocationAdapter", (Object[]) null);
        LocationUpdateTask task = new LocationUpdateTask(application);
        task.execute();

        verifyStatic(Utils.class, never());
        Utils.writeSettingsToFile(any(Context.class), any(CTGeofenceSettings.class));

    }

    @Test
    public void testIsRequestLocationTC1() throws Exception {
        // when currentBgLocationUpdate is false
//
//        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
//                .enableBackgroundLocationUpdates(false).build();
//
//        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
//
//        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder().build();
//
//        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);
//
//        LocationUpdateTask task = new LocationUpdateTask(application);
//        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation", null);
//
//        assertFalse(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC10() throws Exception {
        // when currentBgLocationUpdate is true and there is no change in settings

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .build();

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertFalse(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC2() throws Exception {
        // when currentBgLocationUpdate is true and pendingIntent is null

//        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
//                .enableBackgroundLocationUpdates(true).build();
//
//        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);
//
//        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder().build();
//
//        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);
//
//        LocationUpdateTask task = new LocationUpdateTask(application);
//        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation", null);
//
//        assertTrue(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC3() throws Exception {
        // when fetch mode is current and change in accuracy

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setLocationAccuracy(CTGeofenceSettings.ACCURACY_HIGH)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationAccuracy(CTGeofenceSettings.ACCURACY_LOW)
                .build();

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertTrue(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC4() throws Exception {
        // when fetch mode is current and change in interval

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setInterval(2000000)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setInterval(5000000)
                .build();

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertTrue(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC5() throws Exception {
        // when fetch mode is current and change in fastest interval

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setFastestInterval(2000000)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setFastestInterval(5000000)
                .build();

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertTrue(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC6() throws Exception {
        // when fetch mode is current and change in displacement

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setSmallestDisplacement(600)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setSmallestDisplacement(700)
                .build();

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertTrue(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC7() throws Exception {
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

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertTrue(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC8() throws Exception {
        // when fetch mode is current location and change in fetch mode

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .build();

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertTrue(isRequestLocation);

    }

    @Test
    public void testIsRequestLocationTC9() throws Exception {
        // when fetch mode is last location and change in fetch mode

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        CTGeofenceSettings lastGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .build();

        PowerMockito.when(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings);

        LocationUpdateTask task = new LocationUpdateTask(application);
        boolean isRequestLocation = WhiteboxImpl.invokeMethod(task, "isRequestLocation",
                mock(PendingIntent.class));

        assertTrue(isRequestLocation);

    }

}
