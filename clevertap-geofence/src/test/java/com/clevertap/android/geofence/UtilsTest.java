package com.clevertap.android.geofence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
import org.mockito.*;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;
import org.skyscreamer.jsonassert.JSONAssert;

public class UtilsTest extends BaseTestCase {

    @Mock
    private CleverTapAPI cleverTapAPI;

    @Mock
    private CTGeofenceAPI ctGeofenceAPI;

    private MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic;

    @Mock
    private Logger logger;

    private ShadowApplication shadowApplication;

    @After
    public void cleanup() {
        ctGeofenceAPIMockedStatic.close();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);

        shadowApplication = Shadows.shadowOf(application);
        ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class);

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
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
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.P);

        // when SDK Level is less than Q

        boolean actualWhenSdkIsP = Utils.hasBackgroundLocationPermission(application);
        assertTrue("hasBackgroundLocationPermission must return true when sdk int is less than Q", actualWhenSdkIsP);

        // when SDK Level is greater than P and permission denied
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.Q);

        shadowApplication.denyPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        boolean actualWhenPermissionDenied = Utils.hasBackgroundLocationPermission(application);
        assertFalse("hasBackgroundLocationPermission must return false when permission " +
                "is denied and sdk int is greater than P", actualWhenPermissionDenied);

    }

    @Test
    public void testHasPermission() {

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

        //when cleverTapApi and settings is null

        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMockedStatic.when(() -> FileUtils.readFromFile(any(Context.class), anyString())).thenReturn("");
            fileUtilsMockedStatic.when(() -> FileUtils.getCachedFullPath(any(Context.class),
                    anyString())).thenReturn("");

            boolean actualWhenSettingsAndCTApiIsNull = Utils.initCTGeofenceApiIfRequired(application);
            assertFalse("Must be false when cleverTapApi and settings is null", actualWhenSettingsAndCTApiIsNull);

            // when cleverTapApi is null and settings is not null

            fileUtilsMockedStatic.when(() -> FileUtils.readFromFile(any(Context.class),
                    anyString())).thenReturn(CTGeofenceSettingsFake.getSettingsJsonString());
            boolean actualWhenSettingsNonNullAndCTApiIsNull = Utils.initCTGeofenceApiIfRequired(application);
            assertFalse("Must be false when cleverTapApi is null and settings is not null",
                    actualWhenSettingsNonNullAndCTApiIsNull);

            // when cleverTapApi is not null and settings is not null

            try (MockedStatic<CleverTapAPI> clevertapApiMockedStatic = Mockito.mockStatic(CleverTapAPI.class)) {

                clevertapApiMockedStatic.when(() -> CleverTapAPI.getGlobalInstance(any(Context.class), anyString()))
                        .thenReturn(cleverTapAPI);

                boolean actualWhenSettingsNonNullAndCTApiNonNull = Utils.initCTGeofenceApiIfRequired(application);
                assertTrue("Must be true when cleverTapApi is not null and settings is not null",
                        actualWhenSettingsNonNullAndCTApiNonNull);

                // when cleverTapApi is not null
                boolean actualWhenCTApiNonNull = Utils.initCTGeofenceApiIfRequired(application);
                assertTrue("Must be true when cleverTapApi is not null",
                        actualWhenCTApiNonNull);
            }
        }

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
        try (MockedStatic<com.clevertap.android.sdk.Utils> coreUtilsMockedStatic = Mockito.mockStatic(
                com.clevertap.android.sdk.Utils.class)) {

            CTLocationUpdatesListener locationUpdatesListener = Mockito.mock(CTLocationUpdatesListener.class);

            when(ctGeofenceAPI.getCtLocationUpdatesListener()).thenReturn(locationUpdatesListener);

            Utils.notifyLocationUpdates(application, Mockito.mock(Location.class));

            ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);

            coreUtilsMockedStatic.verify(
                    () -> com.clevertap.android.sdk.Utils.runOnUiThread(runnableArgumentCaptor.capture()));
            runnableArgumentCaptor.getValue().run();
            Mockito.verify(locationUpdatesListener).onLocationUpdates(any(Location.class));
        }
    }

    @Test
    public void testReadSettingsFromFile() {

        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMockedStatic.when(() -> FileUtils.getCachedFullPath(any(Context.class),
                    anyString())).thenReturn("");

            // when settings in file is not blank
            fileUtilsMockedStatic.when(() -> FileUtils.readFromFile(any(Context.class),
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

        JSONArray actualSubArrayFull = Utils.subArray(geofenceArray, 0, geofenceArray.length());

        try {
            JSONAssert.assertEquals(geofenceArray, actualSubArrayFull, true);
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
                () -> Utils.subArray(geofenceArray, geofenceArray.length(), 0));

    }

    @Test
    public void testWriteSettingsToFileFailure() {

        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
            when(FileUtils.getCachedDirName(application)).thenReturn("");
            when(FileUtils.writeJsonToFile(any(), any(), any(), any())).thenReturn(false);

            Utils.writeSettingsToFile(application,
                    CTGeofenceSettingsFake.getSettings(CTGeofenceSettingsFake.getSettingsJsonObject()));
            fileUtilsMockedStatic.verify(
                    () -> FileUtils.writeJsonToFile(application, FileUtils.getCachedDirName(application),
                            CTGeofenceConstants.SETTINGS_FILE_NAME, CTGeofenceSettingsFake.getSettingsJsonObject()));
            Mockito.verify(logger).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to write new settings to file");
        }
    }

    @Test
    public void testWriteSettingsToFileSuccess() {

        try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
            when(FileUtils.getCachedDirName(application)).thenReturn("");
            when(FileUtils.writeJsonToFile(any(), any(), any(), any())).thenReturn(true);

            Utils.writeSettingsToFile(application,
                    CTGeofenceSettingsFake.getSettings(CTGeofenceSettingsFake.getSettingsJsonObject()));
            fileUtilsMockedStatic.verify(
                    () -> FileUtils.writeJsonToFile(application, FileUtils.getCachedDirName(application),
                            CTGeofenceConstants.SETTINGS_FILE_NAME, CTGeofenceSettingsFake.getSettingsJsonObject()));
            Mockito.verify(logger).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "New settings successfully written to file");
        }
    }
}
