package com.clevertap.android.geofence;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.location.Location;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.LocationResult;
import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
@PrepareForTest({CTGeofenceAPI.class, CleverTapAPI.class, Utils.class, LocationResult.class})
@Ignore
public class PushLocationEventTaskTest extends BaseTestCase {

    @Mock
    public CleverTapAPI cleverTapAPI;

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTGeofenceTask.OnCompleteListener onCompleteListener;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Location location;

    private LocationResult locationResult;

    private Logger logger;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CTGeofenceAPI.class, Utils.class, CleverTapAPI.class);
        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        logger = new Logger(Logger.DEBUG);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

        location = new Location("");
        locationResult = LocationResult.create(Arrays.asList(new Location[]{location}));

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

        verifyStatic(Utils.class, times(0));
        Utils.notifyLocationUpdates(any(Context.class), any(Location.class));

        Mockito.verify(onCompleteListener).onComplete();

    }

    @Test
    public void testExecuteWhenCleverTapApiNotNullAndFutureIsNull() {

        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        // when listener not null
        task.setOnCompleteListener(onCompleteListener);
        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);

        WhiteboxImpl.setInternalState(ctGeofenceAPI, "cleverTapAPI", cleverTapAPI);
        WhiteboxImpl.setInternalState(ctGeofenceAPI, "context", application);

        Mockito.when(cleverTapAPI.setLocationForGeofences(any(Location.class), anyInt())).
                thenReturn(null);

        task.execute();

        verifyStatic(Utils.class);
        Utils.notifyLocationUpdates(any(Context.class), any(Location.class));
        Mockito.verify(cleverTapAPI).setLocationForGeofences(any(Location.class), anyInt());
        Mockito.verify(onCompleteListener).onComplete();

    }

    @Test
    public void testExecuteWhenCleverTapApiNotNullAndFutureNotNull() {
        Future future = Mockito.mock(Future.class);

        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        // when listener not null
        task.setOnCompleteListener(onCompleteListener);
        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);

        WhiteboxImpl.setInternalState(ctGeofenceAPI, "cleverTapAPI", cleverTapAPI);
        WhiteboxImpl.setInternalState(ctGeofenceAPI, "context", application);

        Mockito.when(cleverTapAPI.setLocationForGeofences(any(Location.class), anyInt())).
                thenReturn(future);
        task.execute();

        verifyStatic(Utils.class);
        Utils.notifyLocationUpdates(any(Context.class), any(Location.class));

        try {
            Mockito.verify(future).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Mockito.verify(onCompleteListener).onComplete();

    }

    @Test
    public void testSendOnCompleteEventWhenListenerIsNull() {
        // when listener is null
        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        task.setOnCompleteListener(null);
        try {
            WhiteboxImpl.invokeMethod(task, "sendOnCompleteEvent");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Mockito.verify(onCompleteListener, times(0)).onComplete();

    }

    @Test
    public void testSendOnCompleteEventWhenListenerNotNull() {
        // when listener not null
        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        task.setOnCompleteListener(onCompleteListener);
        try {
            WhiteboxImpl.invokeMethod(task, "sendOnCompleteEvent");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Mockito.verify(onCompleteListener).onComplete();

    }

}
