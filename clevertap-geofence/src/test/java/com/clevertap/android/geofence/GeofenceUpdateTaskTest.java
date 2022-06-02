package com.clevertap.android.geofence;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import com.clevertap.android.geofence.fakes.GeofenceJSON;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.model.CTGeofence;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.Arrays;
import java.util.List;
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
@PrepareForTest({CTGeofenceAPI.class, FileUtils.class})
public class GeofenceUpdateTaskTest extends BaseTestCase {

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public CTGeofenceAdapter ctGeofenceAdapter;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Logger logger;

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

        verifyStatic(FileUtils.class);
        FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(), argumentCaptorJson.capture());

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

        verifyStatic(FileUtils.class);
        FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(), argumentCaptorJson.capture());

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

        verifyStatic(FileUtils.class);
        FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(), argumentCaptorJson.capture());

        JSONAssert.assertEquals(GeofenceJSON.getEmptyJson(), argumentCaptorJson.getValue(), true);

        ArgumentCaptor<List<CTGeofence>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(ctGeofenceAdapter).addAllGeofence(argumentCaptor.capture(), any(OnSuccessListener.class));
        assertEquals(argumentCaptor.getValue().size(), 0);
    }

    @Test
    public void executeTestTC4() throws Exception {

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
        assertThat(argumentCaptorOldGeofence.getValue(), is(Arrays.asList(new String[]{"310001"})));

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

        verifyStatic(FileUtils.class);
        FileUtils.writeJsonToFile(any(Context.class), anyString(), anyString(), argumentCaptorJson.capture());

        JSONAssert.assertEquals(GeofenceJSON.getGeofence(), argumentCaptorJson.getValue(), true);

        ArgumentCaptor<List<CTGeofence>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(ctGeofenceAdapter).addAllGeofence(argumentCaptor.capture(), any(OnSuccessListener.class));
        assertEquals(argumentCaptor.getValue().size(), 2);
    }

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CTGeofenceAPI.class, FileUtils.class);

        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        logger = new Logger(Logger.DEBUG);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

        WhiteboxImpl.setInternalState(ctGeofenceAPI, "ctGeofenceAdapter", ctGeofenceAdapter);

    }

}
