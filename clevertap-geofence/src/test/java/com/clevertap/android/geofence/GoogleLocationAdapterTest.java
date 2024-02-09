package com.clevertap.android.geofence;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.clevertap.android.geofence.CTGeofenceConstants.TAG_WORK_LOCATION_UPDATES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;
import com.clevertap.android.geofence.fakes.GeofenceEventFake;
import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.mockito.*;


public class GoogleLocationAdapterTest extends BaseTestCase {

    @Mock
    public CleverTapAPI cleverTapAPI;

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTLocationAdapter ctLocationAdapter;

    @Mock
    public FusedLocationProviderClient providerClient;

    @Mock
    private Logger logger;

    private MockedStatic<Tasks> tasksMockedStatic;

    private MockedStatic<Utils> utilsMockedStatic;

    private MockedStatic<LocationServices> locationServicesMockedStatic;

    private MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);

        ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class);
        locationServicesMockedStatic = Mockito.mockStatic(LocationServices.class);
        utilsMockedStatic = Mockito.mockStatic(Utils.class);
        tasksMockedStatic = Mockito.mockStatic(Tasks.class);

        when(CTGeofenceAPI.getInstance(any(Context.class))).thenReturn(ctGeofenceAPI);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);
        when(ctGeofenceAPI.getCtLocationAdapter()).thenReturn(ctLocationAdapter);
        when(ctGeofenceAPI.getCleverTapApi()).thenReturn(cleverTapAPI);

        super.setUp();

        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();

        // Initialize WorkManager for unit test.
        WorkManagerTestInitHelper.initializeTestWorkManager(
                application, config);
    }

    @After
    public void cleanup() {
        ctGeofenceAPIMockedStatic.close();
        locationServicesMockedStatic.close();
        utilsMockedStatic.close();
        tasksMockedStatic.close();
    }

    @Test
    public void testApplySettings() {

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .enableBackgroundLocationUpdates(true)
                .setInterval(2000000)
                .setFastestInterval(2000000)
                .setLocationAccuracy(CTGeofenceSettings.ACCURACY_LOW)
                .setSmallestDisplacement(900)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        final GoogleLocationAdapter locationAdapter = new GoogleLocationAdapter(application);

        try {
            locationAdapter.requestLocationUpdates();
            LocationRequest actualLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_LOW_POWER,2000000)
                    .setMinUpdateIntervalMillis(2000000)
                    .setMinUpdateDistanceMeters(900)
                    .build();

            assertEquals(ctGeofenceSettings.getInterval(), actualLocationRequest.getIntervalMillis());
            assertEquals(ctGeofenceSettings.getFastestInterval(), actualLocationRequest.getMinUpdateIntervalMillis());
            assertEquals(ctGeofenceSettings.getSmallestDisplacement(),
                    actualLocationRequest.getMinUpdateDistanceMeters(), 0);
            assertEquals(Priority.PRIORITY_LOW_POWER, actualLocationRequest.getPriority());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetLastLocation() {
        final Location expectedLocation = GeofenceEventFake.getTriggeredLocation();
        when(LocationServices.getFusedLocationProviderClient(application))
                .thenReturn(providerClient);
        Task<Location> locationTask = mock(Task.class);
        try {
            when(Tasks.await(locationTask)).thenReturn(expectedLocation);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        when(providerClient.getLastLocation()).thenReturn(locationTask);
        final GoogleLocationAdapter locationAdapter = new GoogleLocationAdapter(application);

        locationAdapter.getLastLocation(location -> Assert.assertSame(expectedLocation, location));
    }

    @Test
    public void testRequestLocationUpdatesTC1() {

        // when backgroundLocationUpdates not enabled

        when(LocationServices.getFusedLocationProviderClient(application))
                .thenReturn(providerClient);

        GoogleLocationAdapter locationAdapter = new GoogleLocationAdapter(application);

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .enableBackgroundLocationUpdates(false).build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        locationAdapter.requestLocationUpdates();
        verify(cleverTapAPI).pushGeoFenceError(anyInt(), anyString());
        verify(providerClient, never()).requestLocationUpdates(
                any(LocationRequest.class), any(PendingIntent.class));
    }

    @Test
    public void testRequestLocationUpdatesTC2() throws Exception {

        // when backgroundLocationUpdates is enabled and fetch mode is current location

        when(LocationServices.getFusedLocationProviderClient(application))
                .thenReturn(providerClient);
        when(Utils.isConcurrentFuturesDependencyAvailable())
                .thenReturn(true);

        GoogleLocationAdapter locationAdapter = new GoogleLocationAdapter(application);

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .enableBackgroundLocationUpdates(true).build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        // enqueue work to verify later that work is cancelled by method under test
        WorkManager workManager = WorkManager.getInstance(application);

        PeriodicWorkRequest locationRequest = new PeriodicWorkRequest.Builder(BackgroundLocationWork.class,
                1800000, TimeUnit.MILLISECONDS,
                600000, TimeUnit.MILLISECONDS)
                .build();

        workManager.enqueueUniquePeriodicWork(TAG_WORK_LOCATION_UPDATES,
                ExistingPeriodicWorkPolicy.KEEP, locationRequest).getResult().get();

        // call method under test
        locationAdapter.requestLocationUpdates();

        // Get WorkInfo and outputData
        WorkInfo workInfo = workManager.getWorkInfoById(locationRequest.getId()).get();
        // Assert work is cancelled by locationAdapter.requestLocationUpdates()
        assertThat(workInfo.getState(), is(WorkInfo.State.CANCELLED));

        verify(providerClient).requestLocationUpdates(
                any(LocationRequest.class), any(PendingIntent.class));

        tasksMockedStatic.verify(() -> Tasks.await(null));

    }

    @Test
    public void testRequestLocationUpdatesTC3() throws Exception {

        // when backgroundLocationUpdates is enabled and fetch mode is last location

        when(LocationServices.getFusedLocationProviderClient(application))
                .thenReturn(providerClient);
        when(Utils.isConcurrentFuturesDependencyAvailable())
                .thenReturn(true);

        GoogleLocationAdapter locationAdapter = new GoogleLocationAdapter(application);

        CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .enableBackgroundLocationUpdates(true).build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(ctGeofenceSettings);

        // simulate current location request exists in OS
        PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_UPDATE_CURRENT);

        // call method under test
        locationAdapter.requestLocationUpdates();

        WorkManager workManager = WorkManager.getInstance(application);

        // Get WorkInfo and outputData
        List<WorkInfo> workInfos = workManager.getWorkInfosForUniqueWork(TAG_WORK_LOCATION_UPDATES).get();
        // Assert work is enqueued by locationAdapter.requestLocationUpdates()
        assertThat(workInfos.get(0).getState(),
                isIn(new WorkInfo.State[]{WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING}));

        verify(providerClient).removeLocationUpdates(any(PendingIntent.class));

        tasksMockedStatic.verify(() -> Tasks.await(null));

    }

}
