package com.clevertap.android.geofence;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.app.PendingIntent;
import com.clevertap.android.geofence.fakes.GeofenceJSON;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.model.CTGeofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.*;
import org.mockito.*;

public class GoogleGeofenceAdapterTest extends BaseTestCase {

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;


    @Mock
    public GeofencingClient geofencingClient;


    @Mock
    public OnSuccessListener onSuccessListener;

    @Mock
    public PendingIntent pendingIntent;

    @Mock
    public Task<Void> task;

    private MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic;

    @Mock
    private CTGeofenceAdapter ctGeofenceAdapter;

    private MockedStatic<LocationServices> locationServicesMockedStatic;

    @Mock
    private Logger logger;

    private MockedStatic<Tasks> tasksMockedStatic;

    private MockedStatic<Utils> utilsMockedStatic;

    @After
    public void cleanup() {
        ctGeofenceAPIMockedStatic.close();
        locationServicesMockedStatic.close();
        utilsMockedStatic.close();
        tasksMockedStatic.close();
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class);
        locationServicesMockedStatic = Mockito.mockStatic(LocationServices.class);
        utilsMockedStatic = Mockito.mockStatic(Utils.class);
        tasksMockedStatic = Mockito.mockStatic(Tasks.class);

        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);
        when(LocationServices.getGeofencingClient(application)).thenReturn(geofencingClient);
        when(ctGeofenceAPI.getCtGeofenceAdapter()).thenReturn(ctGeofenceAdapter);

    }

    @Test
    public void testAddAllGeofenceTC1() {

        // when fence list is null
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        geofenceAdapter.addAllGeofence(null, onSuccessListener);

        verify(geofencingClient, never()).addGeofences(any(GeofencingRequest.class), any(PendingIntent.class));

    }

    @Test
    public void testAddAllGeofenceTC2() {

        // when fence list is empty
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        geofenceAdapter.addAllGeofence(new ArrayList<>(), onSuccessListener);
        verify(geofencingClient, never()).addGeofences(any(GeofencingRequest.class), any(PendingIntent.class));

    }

    @Test
    public void testAddAllGeofenceTC3() {

        // when fence list is not empty

        List<CTGeofence> ctGeofences = CTGeofence.from(GeofenceJSON.getGeofence());
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        when(geofencingClient.addGeofences(any(GeofencingRequest.class), any(PendingIntent.class)))
                .thenReturn(task);
        geofenceAdapter.addAllGeofence(ctGeofences, onSuccessListener);

        tasksMockedStatic.verify(() -> Tasks.await(task));
        verify(onSuccessListener).onSuccess(null);

    }

    @Test
    public void testGetGeofencingRequest() {
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        List<CTGeofence> ctGeofences = CTGeofence.from(GeofenceJSON.getGeofence());

        try {
            geofenceAdapter.addAllGeofence(ctGeofences, onSuccessListener);
            ArgumentCaptor<GeofencingRequest> geofencingRequestArgumentCaptor = ArgumentCaptor.forClass(
                    GeofencingRequest.class);
            ArgumentCaptor<PendingIntent> pendingIntentArgumentCaptor = ArgumentCaptor.forClass(PendingIntent.class);

            verify(geofencingClient).addGeofences(geofencingRequestArgumentCaptor.capture(),
                    pendingIntentArgumentCaptor.capture());
            assertEquals(GeofencingRequest.INITIAL_TRIGGER_ENTER,
                    geofencingRequestArgumentCaptor.getValue().getInitialTrigger());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    @Test
    public void testRemoveAllGeofenceTC1() {
        // when fence list is null
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        geofenceAdapter.removeAllGeofence(null, onSuccessListener);

        verify(geofencingClient, never()).removeGeofences(ArgumentMatchers.anyList());

    }

    //
    @Test
    public void testRemoveAllGeofenceTC2() {
        // when fence list is empty
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        geofenceAdapter.removeAllGeofence(new ArrayList<>(), onSuccessListener);

        verify(geofencingClient, never()).removeGeofences(ArgumentMatchers.anyList());

    }

    //
    @Test
    public void testRemoveAllGeofenceTC3() {
        // when fence list is not empty

        List<String> ctGeofenceIds = Arrays.asList("111", "222");
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        when(geofencingClient.removeGeofences(ctGeofenceIds)).thenReturn(task);
        geofenceAdapter.removeAllGeofence(ctGeofenceIds, onSuccessListener);

        tasksMockedStatic.verify(() -> Tasks.await(task));
        verify(onSuccessListener).onSuccess(null);

    }

    @Test
    public void testStopGeofenceMonitoringTC1() {
        // when pending intent is null

        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        geofenceAdapter.stopGeofenceMonitoring(null);

        verify(geofencingClient, never()).removeGeofences(any(PendingIntent.class));
    }

    @Test
    public void testStopGeofenceMonitoringTC2() {
        // when pending intent is not null

        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);

        when(geofencingClient.removeGeofences(pendingIntent)).thenReturn(task);

        geofenceAdapter.stopGeofenceMonitoring(pendingIntent);

        tasksMockedStatic.verify(() -> Tasks.await(task));
        verify(pendingIntent).cancel();
    }

}
