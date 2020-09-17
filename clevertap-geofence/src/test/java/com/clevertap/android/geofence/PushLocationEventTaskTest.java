package com.clevertap.android.geofence;

import android.content.Context;
import android.location.Location;

import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.LocationResult;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.internal.WhiteboxImpl;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.emory.mathcs.backport.java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28,
        application = TestApplication.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*"})
@PrepareForTest({CTGeofenceAPI.class, CleverTapAPI.class, Utils.class, LocationResult.class})
public class PushLocationEventTaskTest extends BaseTestCase {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    private Logger logger;
    @Mock
    public CTGeofenceAPI ctGeofenceAPI;
    @Mock
    public CleverTapAPI cleverTapAPI;
    @Mock
    public CTGeofenceTask.OnCompleteListener onCompleteListener;
    private Location location;
    private LocationResult locationResult;

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

        verifyStatic(Utils.class,times(0));
        Utils.notifyLocationUpdates(any(Context.class), any(Location.class));

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
    public void testSendOnCompleteEventWhenListenerNotNull(){
        // when listener not null
        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        task.setOnCompleteListener(onCompleteListener);
        try {
            WhiteboxImpl.invokeMethod(task,"sendOnCompleteEvent");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Mockito.verify(onCompleteListener).onComplete();

    }

    @Test
    public void testSendOnCompleteEventWhenListenerIsNull(){
        // when listener is null
        PushLocationEventTask task = new PushLocationEventTask(application, locationResult);

        task.setOnCompleteListener(null);
        try {
            WhiteboxImpl.invokeMethod(task,"sendOnCompleteEvent");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Mockito.verify(onCompleteListener,times(0)).onComplete();

    }

}
