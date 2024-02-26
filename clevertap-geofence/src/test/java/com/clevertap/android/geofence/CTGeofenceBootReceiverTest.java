package com.clevertap.android.geofence;

import static com.clevertap.android.geofence.CTGeofenceAPI.GEOFENCE_LOG_TAG;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import org.junit.*;
import org.mockito.*;

public class CTGeofenceBootReceiverTest extends BaseTestCase {

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public BroadcastReceiver.PendingResult pendingResult;

    @Mock
    private Logger logger;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        super.setUp();
    }

    @Test
    public void testOnReceiveWhenIntentIstNull() {

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);

        spy.onReceive(application, null);

        verify(spy, never()).goAsync();

    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC1() {

        // when initCTGeofenceApiIfRequired return true

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);
        when(spy.goAsync()).thenReturn(pendingResult);

        final Boolean[] isFinished = {false};
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);

        doAnswer(invocation -> {
            isFinished[0] = true;
            return null;
        }).when(pendingResult).finish();

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(() -> Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                    .thenReturn(true);
            utilsMockedStatic.when(() -> Utils.hasBackgroundLocationPermission(application))
                    .thenReturn(true);
            utilsMockedStatic.when(() -> Utils.initCTGeofenceApiIfRequired(application))
                    .thenReturn(true);

            try (MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class)) {
                ctGeofenceAPIMockedStatic.when(CTGeofenceAPI::getLogger).thenReturn(logger);

                spy.onReceive(application, intent);
                await().until(() -> isFinished[0]);
                verify(CTGeofenceAPI.getLogger()).debug(GEOFENCE_LOG_TAG,
                        "onReceive called after " + "device reboot");
                verify(pendingResult).finish();
            }
        }
    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC2() {

        // when initCTGeofenceApiIfRequired return false

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);
        when(spy.goAsync()).thenReturn(pendingResult);

        final Boolean[] isFinished = {false};
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        doAnswer(invocation -> {
            isFinished[0] = true;
            return null;
        }).when(pendingResult).finish();

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(() -> Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                    .thenReturn(true);
            utilsMockedStatic.when(() -> Utils.hasBackgroundLocationPermission(application))
                    .thenReturn(true);
            utilsMockedStatic.when(() -> Utils.initCTGeofenceApiIfRequired(application))
                    .thenReturn(false);

            try (MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class)) {
                ctGeofenceAPIMockedStatic.when(CTGeofenceAPI::getLogger).thenReturn(logger);
                spy.onReceive(application, intent);
                await().until(() -> isFinished[0]);

                verify(CTGeofenceAPI.getLogger()).debug(GEOFENCE_LOG_TAG,
                        "onReceive called after " + "device reboot");
                verifyNoMoreInteractions(CTGeofenceAPI.getLogger());
                verify(pendingResult).finish();
            }
        }
    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC3() {
        // when ACCESS_FINE_LOCATION permission missing
        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(() -> Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                    .thenReturn(false);
            try (MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class)) {
                ctGeofenceAPIMockedStatic.when(CTGeofenceAPI::getLogger).thenReturn(logger);
                spy.onReceive(application, intent);
                verify(CTGeofenceAPI.getLogger()).debug(GEOFENCE_LOG_TAG,
                        "onReceive called after " + "device reboot");
                verify(CTGeofenceAPI.getLogger()).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "We don't have ACCESS_FINE_LOCATION permission! Not registering "
                                + "geofences and location updates after device reboot");
                verify(spy, never()).goAsync();
            }
        }
    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC4() {
        // when ACCESS_BACKGROUND_LOCATION permission missing
        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);

        try (MockedStatic<Utils> utilsMockedStatic = Mockito.mockStatic(Utils.class)) {
            utilsMockedStatic.when(() -> Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                    .thenReturn(true);
            utilsMockedStatic.when(() -> Utils.hasBackgroundLocationPermission(application))
                    .thenReturn(false);
            try (MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class)) {
                ctGeofenceAPIMockedStatic.when(CTGeofenceAPI::getLogger).thenReturn(logger);
                spy.onReceive(application, intent);
                verify(CTGeofenceAPI.getLogger()).debug(GEOFENCE_LOG_TAG,
                        "onReceive called after " + "device reboot");
                verify(CTGeofenceAPI.getLogger()).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "We don't have ACCESS_BACKGROUND_LOCATION permission! not registering "
                                + "geofences and location updates after device reboot");
                spy.onReceive(application, intent);
                verify(spy, never()).goAsync();
            }
        }
    }
}
