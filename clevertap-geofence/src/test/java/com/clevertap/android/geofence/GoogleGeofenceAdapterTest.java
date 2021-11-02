package com.clevertap.android.geofence;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.app.PendingIntent;
import com.clevertap.android.geofence.fakes.GeofenceJSON;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.geofence.model.CTGeofence;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.hamcrest.*;
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
@PrepareForTest({CTGeofenceAPI.class, Utils.class, CleverTapAPI.class
        , LocationServices.class, Tasks.class})
@Ignore
public class GoogleGeofenceAdapterTest extends BaseTestCase {

    @Mock
    public CleverTapAPI cleverTapAPI;

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTGeofenceAdapter ctLocationAdapter;

    @Mock
    public GeofencingClient geofencingClient;

    @Mock
    public CTGeofenceTask.OnCompleteListener onCompleteListener;

    @Mock
    public OnSuccessListener onSuccessListener;

    @Mock
    public PendingIntent pendingIntent;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    public Task<Void> task;

    private Logger logger;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CTGeofenceAPI.class, Utils.class, CleverTapAPI.class,
                LocationServices.class, Tasks.class);

        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        logger = new Logger(Logger.DEBUG);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

        WhiteboxImpl.setInternalState(ctGeofenceAPI, "ctGeofenceAdapter", ctLocationAdapter);
        WhiteboxImpl.setInternalState(ctGeofenceAPI, "cleverTapAPI", cleverTapAPI);
        PowerMockito.when(LocationServices.getGeofencingClient(application)).thenReturn(geofencingClient);

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
        geofenceAdapter.addAllGeofence(new ArrayList<CTGeofence>(), onSuccessListener);
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

        try {
            verifyStatic(Tasks.class);
            Tasks.await(task);

            verify(onSuccessListener).onSuccess(null);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetGeofencingRequest() {
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        List<CTGeofence> ctGeofences = CTGeofence.from(GeofenceJSON.getGeofence());

        try {
            List<Geofence> googleGeofences = WhiteboxImpl.invokeMethod(geofenceAdapter,
                    "getGoogleGeofences", ctGeofences);
            GeofencingRequest geofencingRequest = WhiteboxImpl.invokeMethod(geofenceAdapter,
                    "getGeofencingRequest", googleGeofences);

            assertEquals(GeofencingRequest.INITIAL_TRIGGER_ENTER, geofencingRequest.getInitialTrigger());
            MatcherAssert.assertThat(geofencingRequest.getGeofences(), CoreMatchers.is(googleGeofences));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRemoveAllGeofenceTC1() {
        // when fence list is null
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        geofenceAdapter.removeAllGeofence(null, onSuccessListener);

        verify(geofencingClient, never()).removeGeofences(ArgumentMatchers.anyList());

    }

    @Test
    public void testRemoveAllGeofenceTC2() {
        // when fence list is empty
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        geofenceAdapter.removeAllGeofence(new ArrayList<String>(), onSuccessListener);

        verify(geofencingClient, never()).removeGeofences(ArgumentMatchers.anyList());

    }

    @Test
    public void testRemoveAllGeofenceTC3() {
        // when fence list is not empty

        List<String> ctGeofenceIds = Arrays.asList("111", "222");
        GoogleGeofenceAdapter geofenceAdapter = new GoogleGeofenceAdapter(application);
        when(geofencingClient.removeGeofences(ctGeofenceIds)).thenReturn(task);
        geofenceAdapter.removeAllGeofence(ctGeofenceIds, onSuccessListener);

        try {
            verifyStatic(Tasks.class);
            Tasks.await(task);

            verify(onSuccessListener).onSuccess(null);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

        try {
            verifyStatic(Tasks.class);
            Tasks.await(task);

            verify(pendingIntent).cancel();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
