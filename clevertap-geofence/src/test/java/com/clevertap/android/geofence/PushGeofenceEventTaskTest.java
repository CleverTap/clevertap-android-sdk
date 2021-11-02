package com.clevertap.android.geofence;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import com.clevertap.android.geofence.fakes.GeofenceEventFake;
import com.clevertap.android.geofence.fakes.GeofenceJSON;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.json.JSONObject;
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
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28,
        application = TestApplication.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*"})
@PrepareForTest({CTGeofenceAPI.class, CleverTapAPI.class, Utils.class, GeofencingEvent.class
        , FileUtils.class})
@Ignore
public class PushGeofenceEventTaskTest extends BaseTestCase {


    @Mock
    public CleverTapAPI cleverTapAPI;

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTGeofenceTask.OnCompleteListener onCompleteListener;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private GeofencingEvent geofencingEvent;

    private Intent intent;

    private Location location;

    private Logger logger;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CTGeofenceAPI.class, Utils.class, CleverTapAPI.class
                , GeofencingEvent.class, FileUtils.class);
        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        logger = new Logger(Logger.DEBUG);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

        location = new Location("");
        intent = new Intent();
        when(GeofencingEvent.fromIntent(intent)).thenReturn(geofencingEvent);
        WhiteboxImpl.setInternalState(ctGeofenceAPI, "cleverTapAPI", cleverTapAPI);

    }

    @Test
    public void testExecuteWhenCleverTapApiIsNull() {

        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(false);
        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        // when listener null
        task.execute();
        Mockito.verifyNoMoreInteractions(onCompleteListener);

        // when listener not null
        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        verifyStatic(GeofencingEvent.class, times(0));
        GeofencingEvent.fromIntent(intent);

        verify(onCompleteListener).onComplete();

    }

    @Test
    public void testExecuteWhenGeofenceEventNotValid() {
        // When geofence does not have error and geofence event is not valid

        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);
        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);
        Mockito.when(geofencingEvent.hasError()).thenReturn(false);
        when(geofencingEvent.getGeofenceTransition()).thenReturn(0);

        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        // when geofence event is not valid

        verify(cleverTapAPI).pushGeoFenceError(anyInt(), anyString());
        verify(onCompleteListener).onComplete();

    }

    @Test
    public void testExecuteWhenGeofenceHasError() {
        // When geofence has error

        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);
        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);
        Mockito.when(geofencingEvent.hasError()).thenReturn(true);

        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        verify(cleverTapAPI).pushGeoFenceError(anyInt(), anyString());
        verify(onCompleteListener).onComplete();
        verify(geofencingEvent, times(0)).getGeofenceTransition();

    }

    @Test
    public void testExecuteWhenTriggeredGeofenceIsNull() {
        // When geofence does not have error and geofence event is valid

        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);
        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);
        Mockito.when(geofencingEvent.hasError()).thenReturn(false);
        when(geofencingEvent.getGeofenceTransition()).thenReturn(1);
        when(geofencingEvent.getTriggeringGeofences()).thenReturn(null);

        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        // when geofence event is enter and triggered geofence is null

        verify(cleverTapAPI).pushGeoFenceError(anyInt(), anyString());
        verify(onCompleteListener).onComplete();

    }

    @Test
    public void testExecuteWhenTriggeredGeofenceNotNull() {
        // when geofence event is enter and triggered geofence is not null
        List<Geofence> geofenceList = new ArrayList<>();
        Location location = new Location("");

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);
        Mockito.when(geofencingEvent.hasError()).thenReturn(false);
        Mockito.when(geofencingEvent.getTriggeringGeofences()).thenReturn(geofenceList);
        Mockito.when(geofencingEvent.getTriggeringLocation()).thenReturn(location);
        when(geofencingEvent.getGeofenceTransition()).thenReturn(1);
        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn("");

        task.setOnCompleteListener(onCompleteListener);
        task.execute();

        verifyStatic(FileUtils.class);
        FileUtils.readFromFile(any(Context.class), anyString());

        verify(onCompleteListener).onComplete();

    }

    @Test
    public void testPushGeofenceEventsWhenEnter() {
        // When old geofence in file is not empty and triggered geofence found in file

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);
        Future future = Mockito.mock(Future.class);

        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getGeofenceString());
        when(cleverTapAPI.pushGeofenceEnteredEvent(any(JSONObject.class))).thenReturn(future);

        List<Geofence> triggeredGeofenceList = GeofenceEventFake.getSingleMatchingTriggeredGeofenceList();
        Location triggeredLocation = GeofenceEventFake.getTriggeredLocation();

        try {

            // Geofence Entered event
            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents",
                    triggeredGeofenceList,
                    triggeredLocation, Geofence.GEOFENCE_TRANSITION_ENTER);

            JSONObject firstFromGeofenceArray = GeofenceJSON.getFirstFromGeofenceArray().getJSONObject(0);
            firstFromGeofenceArray.put("triggered_lat", triggeredLocation.getLatitude());
            firstFromGeofenceArray.put("triggered_lng", triggeredLocation.getLongitude());

            ArgumentCaptor<JSONObject> objectArgumentCaptor = ArgumentCaptor.forClass(JSONObject.class);

            verify(cleverTapAPI).pushGeofenceEnteredEvent(objectArgumentCaptor.capture());
            JSONAssert.assertEquals(firstFromGeofenceArray, objectArgumentCaptor.getValue(), true);

            verify(future).get();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Test
    public void testPushGeofenceEventsWhenExit() {
        // When old geofence in file is not empty and triggered geofence found in file

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);
        Future future = Mockito.mock(Future.class);

        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getGeofenceString());
        when(cleverTapAPI.pushGeoFenceExitedEvent(any(JSONObject.class))).thenReturn(future);

        List<Geofence> triggeredGeofenceList = GeofenceEventFake.getSingleMatchingTriggeredGeofenceList();
        Location triggeredLocation = GeofenceEventFake.getTriggeredLocation();

        try {

            // Geofence Exit event
            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents",
                    triggeredGeofenceList,
                    triggeredLocation, Geofence.GEOFENCE_TRANSITION_EXIT);

            JSONObject firstFromGeofenceArray = GeofenceJSON.getFirstFromGeofenceArray().getJSONObject(0);
            firstFromGeofenceArray.put("triggered_lat", triggeredLocation.getLatitude());
            firstFromGeofenceArray.put("triggered_lng", triggeredLocation.getLongitude());

            ArgumentCaptor<JSONObject> objectArgumentCaptor = ArgumentCaptor.forClass(JSONObject.class);

            verify(cleverTapAPI).pushGeoFenceExitedEvent(objectArgumentCaptor.capture());
            JSONAssert.assertEquals(firstFromGeofenceArray, objectArgumentCaptor.getValue(), true);

            verify(future).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPushGeofenceEventsWhenMultipleExit() {
        // Test multiple geofence exit triggers

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);
        Future future = Mockito.mock(Future.class);

        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getGeofenceString());
        when(cleverTapAPI.pushGeoFenceExitedEvent(any(JSONObject.class))).thenReturn(future);

        List<Geofence> triggeredGeofenceList = GeofenceEventFake.getDoubleMatchingTriggeredGeofenceList();
        Location triggeredLocation = GeofenceEventFake.getTriggeredLocation();

        try {

            // Multiple Geofence Exit event
            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents",
                    triggeredGeofenceList,
                    triggeredLocation, Geofence.GEOFENCE_TRANSITION_EXIT);

            JSONObject firstFromGeofenceArray = GeofenceJSON.getFirstFromGeofenceArray().getJSONObject(0);
            firstFromGeofenceArray.put("triggered_lat", triggeredLocation.getLatitude());
            firstFromGeofenceArray.put("triggered_lng", triggeredLocation.getLongitude());

            ArgumentCaptor<JSONObject> objectArgumentCaptor = ArgumentCaptor.forClass(JSONObject.class);

            verify(cleverTapAPI, times(2)).pushGeoFenceExitedEvent(objectArgumentCaptor.capture());

            // assert geofence with id 310001
            JSONAssert.assertEquals(firstFromGeofenceArray, objectArgumentCaptor.getAllValues().get(0), true);

            JSONObject lastFromGeofenceArray = GeofenceJSON.getLastFromGeofenceArray().getJSONObject(0);
            lastFromGeofenceArray.put("triggered_lat", triggeredLocation.getLatitude());
            lastFromGeofenceArray.put("triggered_lng", triggeredLocation.getLongitude());

            // assert geofence with id 310002
            JSONAssert.assertEquals(lastFromGeofenceArray, objectArgumentCaptor.getAllValues().get(1), true);

            verify(future, times(2)).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPushGeofenceEventsWhenOldGeofenceIsEmpty() {
        // When old geofence in file is empty

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn("");

        List<Geofence> triggeredGeofenceList = GeofenceEventFake.getSingleMatchingTriggeredGeofenceList();
        Location triggeredLocation = GeofenceEventFake.getTriggeredLocation();

        try {

            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents",
                    triggeredGeofenceList,
                    triggeredLocation, Geofence.GEOFENCE_TRANSITION_ENTER);

            verify(cleverTapAPI, never()).pushGeofenceEnteredEvent(any(JSONObject.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPushGeofenceEventsWhenOldGeofenceJsonArrayIsEmpty() {
        // When old geofence json array in file is empty

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getEmptyGeofence().toString());

        List<Geofence> triggeredGeofenceList = GeofenceEventFake.getSingleMatchingTriggeredGeofenceList();
        Location triggeredLocation = GeofenceEventFake.getTriggeredLocation();

        try {

            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents",
                    triggeredGeofenceList,
                    triggeredLocation, Geofence.GEOFENCE_TRANSITION_ENTER);

            verify(cleverTapAPI, never()).pushGeofenceEnteredEvent(any(JSONObject.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPushGeofenceEventsWhenOldGeofenceJsonInvalid() {
        // When old geofence json content in file is invalid

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getEmptyJson().toString());

        List<Geofence> triggeredGeofenceList = GeofenceEventFake.getSingleMatchingTriggeredGeofenceList();
        Location triggeredLocation = GeofenceEventFake.getTriggeredLocation();

        try {

            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents",
                    triggeredGeofenceList,
                    triggeredLocation, Geofence.GEOFENCE_TRANSITION_ENTER);

            verify(cleverTapAPI, never()).pushGeofenceEnteredEvent(any(JSONObject.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPushGeofenceEventsWhenTriggeredGeofenceIsNotFound() {
        // When triggered geofence not found in file

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getGeofenceString());

        List<Geofence> triggeredGeofenceList = GeofenceEventFake.getNonMatchingTriggeredGeofenceList();
        Location triggeredLocation = GeofenceEventFake.getTriggeredLocation();

        try {

            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents",
                    triggeredGeofenceList,
                    triggeredLocation, Geofence.GEOFENCE_TRANSITION_ENTER);

            verify(cleverTapAPI, never()).pushGeofenceEnteredEvent(any(JSONObject.class));
            verify(cleverTapAPI).pushGeoFenceError(anyInt(), anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPushGeofenceEventsWhenTriggeredGeofenceIsNull() {
        // When triggered geofence is null

        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        try {
            WhiteboxImpl.invokeMethod(task, "pushGeofenceEvents", (Object) null,
                    null, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        verify(cleverTapAPI).pushGeoFenceError(anyInt(), anyString());

        verifyStatic(FileUtils.class, times(0));
        FileUtils.readFromFile(any(Context.class), anyString());

    }

    @Test
    public void testSendOnCompleteEventWhenListenerIsNull() {
        // when listener is null
        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

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
        PushGeofenceEventTask task = new PushGeofenceEventTask(application, intent);

        task.setOnCompleteListener(onCompleteListener);
        try {
            WhiteboxImpl.invokeMethod(task, "sendOnCompleteEvent");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Mockito.verify(onCompleteListener).onComplete();

    }
}
