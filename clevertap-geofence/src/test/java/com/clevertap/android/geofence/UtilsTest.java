package com.clevertap.android.geofence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import com.clevertap.android.geofence.fakes.CTGeofenceSettingsFake;
import com.clevertap.android.geofence.fakes.GeofenceJSON;
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener;
import com.clevertap.android.sdk.CleverTapAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.junit.function.*;
import org.junit.runner.*;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.powermock.reflect.internal.WhiteboxImpl;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28,
        application = TestApplication.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*"})
@PrepareForTest({FileUtils.class, CTGeofenceAPI.class, CleverTapAPI.class,
        com.clevertap.android.sdk.Utils.class})
public class UtilsTest extends BaseTestCase {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private CTGeofenceAPI ctGeofenceAPI;

    private Logger logger;

    /* @Mock
     private static Logger logger;*/
    private ShadowApplication shadowApplication;

    @Before
    public void setUp() throws Exception {
        //MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CTGeofenceAPI.class);
        super.setUp();
        shadowApplication = Shadows.shadowOf(application);
        ctGeofenceAPI = Mockito.mock(CTGeofenceAPI.class);

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        logger = new Logger(Logger.DEBUG);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

    }

    @Test
    public void testEmptyIfNull() {
        //when string is null
        String actualWhenStringIsNull = Utils.emptyIfNull(null);
        assertEquals("", actualWhenStringIsNull);

        //when string is non null
        String actualWhenStringIsNonNull = Utils.emptyIfNull("1");
        assertEquals("1", actualWhenStringIsNonNull);
    }

    @Test
    public void testHasBackgroundLocationPermission() {

        // when SDK Level is less than Q
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.P);

        boolean actualWhenSdkIsP = Utils.hasBackgroundLocationPermission(application);
        assertTrue("hasBackgroundLocationPermission must return true when sdk int is less than Q", actualWhenSdkIsP);

        // when SDK Level is greater than P and permission denied
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.Q);

        shadowApplication.denyPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        boolean actualWhenPermissionDenied = Utils.hasBackgroundLocationPermission(application);
        assertFalse("hasBackgroundLocationPermission must return false when permission " +
                "is denied and sdk int is greater than P", actualWhenPermissionDenied);

    }

    @Test
    public void testHasPermission() {
        //mockStatic(ContextCompat.class);

        ShadowApplication shadowApplication = Shadows.shadowOf(application);

        // when permission is null

        boolean actual = Utils.hasPermission(application, null);
        assertFalse("hasPermission must return false when permission is null", actual);

        // when permission not null and checkSelfPermission returns permission granted

        shadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        boolean actualWhenPermissionGranted = Utils
                .hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION);
        assertTrue("hasPermission must return true when permission is granted", actualWhenPermissionGranted);

        // when permission not null and checkSelfPermission returns permission denied

        shadowApplication.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        boolean actualWhenPermissionDenied = Utils
                .hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION);
        assertFalse("hasPermission must return false when permission is denied", actualWhenPermissionDenied);
    }

    @Test
    public void testInitCTGeofenceApiIfRequired() {
        mockStatic(FileUtils.class);

        //when cleverTapApi and settings is null

        when(FileUtils.readFromFile(any(Context.class), anyString())).thenReturn("");
        when(FileUtils.getCachedFullPath(any(Context.class),
                anyString())).thenReturn("");

        boolean actualWhenSettingsAndCTApiIsNull = Utils.initCTGeofenceApiIfRequired(application);
        assertFalse("Must be false when cleverTapApi and settings is null", actualWhenSettingsAndCTApiIsNull);

        // when cleverTapApi is null and settings is not null

        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(CTGeofenceSettingsFake.getSettingsJsonString());
        boolean actualWhenSettingsNonNullAndCTApiIsNull = Utils.initCTGeofenceApiIfRequired(application);
        assertFalse("Must be false when cleverTapApi is null and settings is not null",
                actualWhenSettingsNonNullAndCTApiIsNull);

        // when cleverTapApi is not null and settings is not null

        mockStatic(CleverTapAPI.class);
        CleverTapAPI cleverTapAPI = Mockito.mock(CleverTapAPI.class);

        when(CleverTapAPI.getGlobalInstance(any(Context.class), anyString()))
                .thenReturn(cleverTapAPI);

        boolean actualWhenSettingsNonNullAndCTApiNonNull = Utils.initCTGeofenceApiIfRequired(application);
        assertTrue("Must be true when cleverTapApi is not null and settings is not null",
                actualWhenSettingsNonNullAndCTApiNonNull);

        // when cleverTapApi is not null
        WhiteboxImpl.setInternalState(ctGeofenceAPI, "cleverTapAPI", cleverTapAPI);

        boolean actualWhenCTApiNonNull = Utils.initCTGeofenceApiIfRequired(application);
        assertTrue("Must be true when cleverTapApi is not null",
                actualWhenCTApiNonNull);

    }

    @Test
    public void testJsonToGeoFenceList() {

        List<String> actualList = Utils.jsonToGeoFenceList(GeofenceJSON.getGeofence());
        List<String> expectedList = Arrays.asList("310001", "310002");

        assertThat(actualList, CoreMatchers.is(expectedList));

        List<String> actualListWhenJsonArrayIsEmpty = Utils.jsonToGeoFenceList(GeofenceJSON.getEmptyGeofence());
        List<String> expectedListWhenJsonArrayIsEmpty = new ArrayList<>();

        assertThat(actualListWhenJsonArrayIsEmpty, CoreMatchers.is(expectedListWhenJsonArrayIsEmpty));

        JSONObject emptyJson = GeofenceJSON.getEmptyJson();
        List<String> actualListWhenJsonIsEmpty = Utils.jsonToGeoFenceList(emptyJson);
        List<String> expectedListWhenJsonIsEmpty = new ArrayList<>();

        assertThat(actualListWhenJsonIsEmpty, CoreMatchers.is(expectedListWhenJsonIsEmpty));
    }

    @Test
    public void testNotifyLocationUpdates() {
        mockStatic(com.clevertap.android.sdk.Utils.class);

        CTLocationUpdatesListener locationUpdatesListener = Mockito.mock(CTLocationUpdatesListener.class);

        Mockito.when(ctGeofenceAPI.getCtLocationUpdatesListener()).thenReturn(locationUpdatesListener);

        Utils.notifyLocationUpdates(application, Mockito.mock(Location.class));

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);

        verifyStatic(com.clevertap.android.sdk.Utils.class);
        com.clevertap.android.sdk.Utils.runOnUiThread(runnableArgumentCaptor.capture());

        runnableArgumentCaptor.getValue().run();
        Mockito.verify(locationUpdatesListener).onLocationUpdates(any(Location.class));
    }

    @Test
    public void testReadSettingsFromFile() {
        mockStatic(FileUtils.class);

        when(FileUtils.getCachedFullPath(any(Context.class),
                anyString())).thenReturn("");

        // when settings in file is not blank
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn(CTGeofenceSettingsFake.getSettingsJsonString());

        CTGeofenceSettings settingsActualWhenNotEmpty = Utils.readSettingsFromFile(application);
        CTGeofenceSettings settingsExpectedWhenNotEmpty =
                CTGeofenceSettingsFake.getSettings(CTGeofenceSettingsFake.getSettingsJsonObject());

        assertThat(settingsActualWhenNotEmpty, samePropertyValuesAs(settingsExpectedWhenNotEmpty));

        // when settings in file is blank
        when(FileUtils.readFromFile(any(Context.class),
                anyString())).thenReturn("");

        CTGeofenceSettings settingsActualWhenEmpty = Utils.readSettingsFromFile(application);
        assertNull(settingsActualWhenEmpty);
    }

    @Test
    public void testSubArray() {
        final JSONArray geofenceArray = GeofenceJSON.getGeofenceArray();

        JSONArray expectedSubArrayFirst = GeofenceJSON.getFirstFromGeofenceArray();
        JSONArray actualSubArrayFirst = Utils.subArray(geofenceArray, 0, 1);

        try {
            JSONAssert.assertEquals(expectedSubArrayFirst, actualSubArrayFirst, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray expectedSubArrayFull = geofenceArray;
        JSONArray actualSubArrayFull = Utils.subArray(geofenceArray, 0, expectedSubArrayFull.length());

        try {
            JSONAssert.assertEquals(expectedSubArrayFull, actualSubArrayFull, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray expectedSubArrayLast = GeofenceJSON.getLastFromGeofenceArray();
        JSONArray actualSubArrayLast = Utils.subArray(geofenceArray, 1, geofenceArray.length());

        try {
            JSONAssert.assertEquals(expectedSubArrayLast, actualSubArrayLast, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Assert.assertThrows("IllegalArgumentException must be thrown when fromIndex is greater than lastIndex",
                IllegalArgumentException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        Utils.subArray(geofenceArray, geofenceArray.length(), 0);
                    }
                });

    }

    @Test
    public void testWriteSettingsToFile() {
        mockStatic(FileUtils.class);

        WhiteboxImpl.setInternalState(ctGeofenceAPI, "accountId", "4RW-Z6Z-485Z");
        when(FileUtils.getCachedDirName(application)).thenReturn("");

        Utils.writeSettingsToFile(application,
                CTGeofenceSettingsFake.getSettings(CTGeofenceSettingsFake.getSettingsJsonObject()));

        verifyStatic(FileUtils.class);
        FileUtils.writeJsonToFile(application, FileUtils.getCachedDirName(application),
                CTGeofenceConstants.SETTINGS_FILE_NAME, CTGeofenceSettingsFake.getSettingsJsonObject());

    }

}
