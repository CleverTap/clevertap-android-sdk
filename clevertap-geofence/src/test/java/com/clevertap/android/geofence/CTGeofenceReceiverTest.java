package com.clevertap.android.geofence;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import android.content.BroadcastReceiver;
import android.content.Intent;
import org.junit.*;
import org.mockito.*;

public class CTGeofenceReceiverTest extends BaseTestCase {

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
    public void testOnReceiveWhenIntentIsNull() {
        CTGeofenceReceiver receiver = new CTGeofenceReceiver();
        CTGeofenceReceiver spy = Mockito.spy(receiver);
        spy.onReceive(application, null);
        verify(spy, never()).goAsync();
    }

    @Test
    public void testOnReceiveWhenIntentNotNull() {
        CTGeofenceReceiver receiver = new CTGeofenceReceiver();
        CTGeofenceReceiver spy = Mockito.spy(receiver);
        when(spy.goAsync()).thenReturn(pendingResult);

        final Boolean[] isFinished = {false};

        doAnswer(invocation -> {
            isFinished[0] = true;
            return null;
        }).when(pendingResult).finish();

        try (MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class)) {
            ctGeofenceAPIMockedStatic.when(CTGeofenceAPI::getLogger).thenReturn(logger);

            Intent intent = new Intent();
            spy.onReceive(application, intent);

            await().until(() -> isFinished[0]);

            verify(CTGeofenceAPI.getLogger()).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence receiver called");
            verify(CTGeofenceAPI.getLogger()).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Returning from Geofence receiver");
            verify(pendingResult).finish();
        }
    }

}
