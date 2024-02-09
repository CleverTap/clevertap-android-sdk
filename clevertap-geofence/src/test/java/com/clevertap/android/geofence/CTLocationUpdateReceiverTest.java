package com.clevertap.android.geofence;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.location.Location;
import com.google.android.gms.location.LocationResult;
import java.util.Arrays;
import org.junit.*;
import org.mockito.*;

public class CTLocationUpdateReceiverTest extends BaseTestCase {

    @Mock
    public CTGeofenceAPI ctGeofenceAPI;

    @Mock
    public BroadcastReceiver.PendingResult pendingResult;

    private LocationResult locationResult;

    @Mock
    private Logger logger;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        super.setUp();
        Location location = new Location("");
        locationResult = LocationResult.create(Arrays.asList(new Location[]{location}));

    }

    @Test
    public void testOnReceiveWhenLastLocationNotNull() {
        CTLocationUpdateReceiver receiver = new CTLocationUpdateReceiver();
        CTLocationUpdateReceiver spy = Mockito.spy(receiver);
        when(spy.goAsync()).thenReturn(pendingResult);

        final Boolean[] isFinished = {false};

        doAnswer(invocation -> {
            isFinished[0] = true;
            return null;
        }).when(pendingResult).finish();

        Intent intent = new Intent();
        intent.putExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT", locationResult);

        try (MockedStatic<CTGeofenceAPI> ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI.class)) {
            ctGeofenceAPIMockedStatic.when(CTGeofenceAPI::getLogger).thenReturn(logger);

            spy.onReceive(application, intent);

            await().until(() -> isFinished[0]);

            verify(CTGeofenceAPI.getLogger()).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Location updates receiver called");
            verify(CTGeofenceAPI.getLogger()).debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Returning from Location Updates Receiver");
            verify(pendingResult).finish();
        }
    }

    @Test
    public void testOnReceiveWhenLocationResultIsNull() {
        CTLocationUpdateReceiver receiver = new CTLocationUpdateReceiver();
        CTLocationUpdateReceiver spy = Mockito.spy(receiver);
        when(spy.goAsync()).thenReturn(pendingResult);

        spy.onReceive(application, null);

        verify(pendingResult).finish();
    }
}
