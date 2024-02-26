package com.clevertap.android.geofence;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.location.Location;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.LocationResult;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.*;
import org.mockito.*;

public class PushLocationEventTaskTest extends BaseTestCase {

    @Mock
    public CleverTapAPI cleverTapAPI;

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTGeofenceTask.OnCompleteListener onCompleteListener;

    private MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic;

    private LocationResult locationResult;

    @Mock
    private Logger logger;

    private MockedStatic<Utils> utilsMockedStatic;

    @After
    public void cleanup() {
        ctGeofenceAPIMockedStatic.close();
        utilsMockedStatic.close();
    }

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);
        super.setUp();
        Location location = new Location("");
        locationResult = LocationResult.create(Arrays.asList(new Location[]{location}));
        ctGeofenceAPIMockedStatic = mockStatic(CTGeofenceAPI.class);
        utilsMockedStatic = mockStatic(Utils.class);
        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);
    }

    @Test
    public void testExecuteWhenCleverTapApiIsNull() {

        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(false);
        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        // when listener null
        task.execute();
        Mockito.verifyNoMoreInteractions(onCompleteListener);

        // when listener not null
        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        utilsMockedStatic.verify(() -> Utils.notifyLocationUpdates(any(Context.class), any(Location.class)),
                times(0));
        Mockito.verify(onCompleteListener).onComplete();
    }

    @Test
    public void testExecuteWhenCleverTapApiNotNullAndFutureIsNullAndListenerNotNull() {

        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        // when listener not null
        task.setOnCompleteListener(onCompleteListener);
        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);

        Mockito.when(cleverTapAPI.setLocationForGeofences(any(Location.class), anyInt())).
                thenReturn(null);

        task.execute();

        utilsMockedStatic.verify(() -> Utils.notifyLocationUpdates(any(Context.class), any(Location.class)));
        Mockito.verify(logger).verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Dropping location ping event to CT server");
        Mockito.verify(onCompleteListener).onComplete();
    }

    @Test
    public void testExecuteWhenCleverTapApiNotNullAndFutureIsNullAndListenerNull() {

        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        // when listener null
        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);

        Mockito.when(cleverTapAPI.setLocationForGeofences(any(Location.class), anyInt())).
                thenReturn(null);

        task.execute();

        utilsMockedStatic.verify(() -> Utils.notifyLocationUpdates(any(Context.class), any(Location.class)));
        Mockito.verify(logger).verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Dropping location ping event to CT server");
        Mockito.verifyNoMoreInteractions(onCompleteListener);
    }

    @Test
    public void testExecuteWhenCleverTapApiNotNullAndFutureNotNullAndListenerNotNull() {
        Future future = Mockito.mock(Future.class);

        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        // when listener not null
        task.setOnCompleteListener(onCompleteListener);
        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);

        Mockito.when(ctGeofenceAPI.processTriggeredLocation(any(Location.class))).
                thenReturn(future);
        task.execute();

        utilsMockedStatic.verify(() -> Utils.notifyLocationUpdates(any(Context.class), any(Location.class)));
        try {
            Mockito.verify(future).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        Mockito.verify(logger).verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Calling future for setLocationForGeofences()");
        Mockito.verify(onCompleteListener).onComplete();
    }

    @Test
    public void testExecuteWhenCleverTapApiNotNullAndFutureNotNullAndListenerNull() {
        Future future = Mockito.mock(Future.class);

        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        // when listener null
        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);

        Mockito.when(ctGeofenceAPI.processTriggeredLocation(any(Location.class))).
                thenReturn(future);
        task.execute();

        utilsMockedStatic.verify(() -> Utils.notifyLocationUpdates(any(Context.class), any(Location.class)));
        try {
            Mockito.verify(future).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        Mockito.verify(logger).verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Calling future for setLocationForGeofences()");
        Mockito.verifyNoMoreInteractions(onCompleteListener);
    }
}
