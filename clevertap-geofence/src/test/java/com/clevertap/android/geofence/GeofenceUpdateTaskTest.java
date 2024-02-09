package com.clevertap.android.geofence;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import com.clevertap.android.geofence.fakes.GeofenceJSON;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.model.CTGeofence;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.junit.*;
import org.mockito.*;
import org.skyscreamer.jsonassert.JSONAssert;

public class GeofenceUpdateTaskTest extends BaseTestCase {

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTGeofenceAdapter ctGeofenceAdapter;

    private MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic;

    private MockedStatic<FileUtils> fileUtilsMockedStatic;

    @Mock
    private Logger logger;

    @After
    public void cleanup() {
        ctGeofenceAPIMockedStatic.close();
        fileUtilsMockedStatic.close();
    }

    @Test
    public void executeTestTC1() throws Exception {

        // when old geofence is empty and geofence monitor count is less than new geofence list size

        when(FileUtils.getCachedDirName(application)).thenReturn("");
        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn("");

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .setGeofenceMonitoringCount(1)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        GeofenceUpdateTask updateTask = new GeofenceUpdateTask(application, GeofenceJSON.getGeofence());

        updateTask.execute();

        ArgumentCaptor<JSONObject> argumentCaptorJson = ArgumentCaptor.forClass(JSONObject.class);

        fileUtilsMockedStatic.verify(() -> FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(),
                argumentCaptorJson.capture()));

        JSONAssert.assertEquals(GeofenceJSON.getFirst(), argumentCaptorJson.getValue(), true);

        ArgumentCaptor<List<CTGeofence>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(ctGeofenceAdapter).addAllGeofence(argumentCaptor.capture(), any(OnSuccessListener.class));
        assertEquals(argumentCaptor.getValue().size(), 1);
    }

    @Test
    public void executeTestTC2() throws Exception {

        // when old geofence is empty and geofence monitor count is greater than new geofence list size

        when(FileUtils.getCachedDirName(application)).thenReturn("");
        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn("");

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .setGeofenceMonitoringCount(1)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        GeofenceUpdateTask updateTask = new GeofenceUpdateTask(application, GeofenceJSON.getEmptyGeofence());

        updateTask.execute();

        ArgumentCaptor<JSONObject> argumentCaptorJson = ArgumentCaptor.forClass(JSONObject.class);

        fileUtilsMockedStatic.verify(() -> FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(),
                argumentCaptorJson.capture()));

        JSONAssert.assertEquals(GeofenceJSON.getEmptyGeofence(), argumentCaptorJson.getValue(), true);

        ArgumentCaptor<List<CTGeofence>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(ctGeofenceAdapter).addAllGeofence(argumentCaptor.capture(), any(OnSuccessListener.class));
        assertEquals(argumentCaptor.getValue().size(), 0);
    }

    @Test
    public void executeTestTC3() throws Exception {

        // when old geofence is empty and new geofence json is invalid

        when(FileUtils.getCachedDirName(application)).thenReturn("");
        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn("");

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .setGeofenceMonitoringCount(1)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        GeofenceUpdateTask updateTask = new GeofenceUpdateTask(application, GeofenceJSON.getEmptyJson());

        updateTask.execute();

        ArgumentCaptor<JSONObject> argumentCaptorJson = ArgumentCaptor.forClass(JSONObject.class);

        fileUtilsMockedStatic.verify(() -> FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(),
                argumentCaptorJson.capture()));

        JSONAssert.assertEquals(GeofenceJSON.getEmptyJson(), argumentCaptorJson.getValue(), true);

        ArgumentCaptor<List<CTGeofence>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(ctGeofenceAdapter).addAllGeofence(argumentCaptor.capture(), any(OnSuccessListener.class));
        assertEquals(argumentCaptor.getValue().size(), 0);
    }

    @Test
    public void executeTestTC4() {

        // when old geofence is not empty and new geofence list is not empty

        when(FileUtils.getCachedDirName(application)).thenReturn("");
        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getFirst().toString());

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .setGeofenceMonitoringCount(1)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        GeofenceUpdateTask updateTask = new GeofenceUpdateTask(application, GeofenceJSON.getGeofence());

        updateTask.execute();

        ArgumentCaptor<List<String>> argumentCaptorOldGeofence = ArgumentCaptor.forClass(List.class);

        verify(ctGeofenceAdapter)
                .removeAllGeofence(argumentCaptorOldGeofence.capture(), any(OnSuccessListener.class));
        assertThat(argumentCaptorOldGeofence.getValue(), is(Arrays.asList("310001")));

    }

    @Test
    public void executeTestTC5() throws Exception {

        // when old geofence is not empty and new geofence list is null

        when(FileUtils.getCachedDirName(application)).thenReturn("");
        when(FileUtils.getCachedFullPath(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(GeofenceJSON.getGeofenceString());

        CTGeofenceSettings currentGeofenceSettings = new CTGeofenceSettings.Builder()
                .setGeofenceMonitoringCount(2)
                .build();

        when(ctGeofenceAPI.getGeofenceSettings()).thenReturn(currentGeofenceSettings);

        GeofenceUpdateTask updateTask = new GeofenceUpdateTask(application, null);

        updateTask.execute();

        ArgumentCaptor<JSONObject> argumentCaptorJson = ArgumentCaptor.forClass(JSONObject.class);

        fileUtilsMockedStatic.verify(() -> FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(),
                argumentCaptorJson.capture()));
        JSONAssert.assertEquals(GeofenceJSON.getGeofence(), argumentCaptorJson.getValue(), true);

        ArgumentCaptor<List<CTGeofence>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(ctGeofenceAdapter).addAllGeofence(argumentCaptor.capture(), any(OnSuccessListener.class));
        assertEquals(argumentCaptor.getValue().size(), 2);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        super.setUp();
        ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class);
        fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class);
        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);
        when(ctGeofenceAPI.getCtGeofenceAdapter()).thenReturn(ctGeofenceAdapter);
    }
}
