package com.clevertap.android.geofence;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28,
        application = TestApplication.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*"})
@PrepareForTest({CTGeofenceAPI.class, CTGeofenceTaskManager.class, Utils.class})
public class CTGeofenceBootReceiverTest extends BaseTestCase {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    private Logger logger;
    @Mock
    public CTGeofenceAPI ctGeofenceAPI;
    @Mock
    public BroadcastReceiver.PendingResult pendingResult;
    @Mock
    public CTGeofenceTaskManager taskManager;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CTGeofenceAPI.class, CTGeofenceTaskManager.class, Utils.class);

        super.setUp();

        when(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI);
        logger = new Logger(Logger.DEBUG);
        when(CTGeofenceAPI.getLogger()).thenReturn(logger);

        PowerMockito.when(CTGeofenceTaskManager.getInstance()).thenReturn(taskManager);

    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC1() {

        // when initCTGeofenceApiIfRequired return true

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);
        when(spy.goAsync()).thenReturn(pendingResult);

        final Boolean[] isFinished = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                isFinished[0] = true;
                return null;
            }
        }).when(pendingResult).finish();

        PowerMockito.when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                .thenReturn(true);
        PowerMockito.when(Utils.hasBackgroundLocationPermission(application)).thenReturn(true);
        PowerMockito.when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(true);

        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);

        spy.onReceive(application, intent);

        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isFinished[0];
            }
        });

        verify(pendingResult).finish();
    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC2() {

        // when initCTGeofenceApiIfRequired return false

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);
        when(spy.goAsync()).thenReturn(pendingResult);

        final Boolean[] isFinished = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                isFinished[0] = true;
                return null;
            }
        }).when(pendingResult).finish();

        PowerMockito.when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                .thenReturn(true);
        PowerMockito.when(Utils.hasBackgroundLocationPermission(application)).thenReturn(true);
        PowerMockito.when(Utils.initCTGeofenceApiIfRequired(application)).thenReturn(false);

        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);

        spy.onReceive(application, intent);

        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isFinished[0];
            }
        });

        verify(pendingResult).finish();
    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC3() {

        // when ACCESS_FINE_LOCATION permission missing

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);

        PowerMockito.when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                .thenReturn(false);

        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);

        spy.onReceive(application, intent);

        verify(spy, never()).goAsync();

    }

    @Test
    public void testOnReceiveWhenIntentNotNullTC4() {

        // when ACCESS_BACKGROUND_LOCATION permission missing

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);

        PowerMockito.when(Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION))
                .thenReturn(true);
        PowerMockito.when(Utils.hasBackgroundLocationPermission(application))
                .thenReturn(false);

        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);

        spy.onReceive(application, intent);

        verify(spy, never()).goAsync();

    }

    @Test
    public void testOnReceiveWhenIntentIstNull() {

        CTGeofenceBootReceiver receiver = new CTGeofenceBootReceiver();
        CTGeofenceBootReceiver spy = Mockito.spy(receiver);

        spy.onReceive(application, null);

        verify(spy, never()).goAsync();

    }

}
