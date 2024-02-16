package com.clevertap.android.geofence;

import static org.junit.Assert.*;

import android.app.PendingIntent;
import android.content.ComponentName;
import org.junit.*;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPendingIntent;

public class PendingIntentFactoryTest extends BaseTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGetPendingIntentForFlagNoCreate() {
        // when pendingIntent flag is no create

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_NO_CREATE);

        assertNull(actual);
    }

    @Test
    public void testGetPendingIntentForFlagUpdateCurrent() {
        // when pendingIntent flag is no create

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);

        assertNotNull(actual);
    }

    @Test
    public void testGetPendingIntentForGeofence() {
        // when pendingIntent type is geofence

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);

        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(actual);

        assertTrue(shadowPendingIntent.isBroadcastIntent());
        assertEquals(1001001, shadowPendingIntent.getRequestCode());
        assertEquals(PendingIntent.FLAG_UPDATE_CURRENT, shadowPendingIntent.getFlags());

        ComponentName actualComponentName = new ComponentName(application, CTGeofenceReceiver.class);

        assertEquals(CTGeofenceConstants.ACTION_GEOFENCE_RECEIVER,
                shadowPendingIntent.getSavedIntent().getAction());
        assertEquals(actualComponentName, shadowPendingIntent.getSavedIntent().getComponent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPendingIntentForInvalidType() {
        // when pendingIntent type is invalid

        PendingIntentFactory.getPendingIntent(application,
                9, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    @Test
    public void testGetPendingIntentForLocation() {
        // when pendingIntent type is location

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_LOCATION, PendingIntent.FLAG_UPDATE_CURRENT);

        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(actual);

        assertTrue(shadowPendingIntent.isBroadcastIntent());
        assertEquals(10100111, shadowPendingIntent.getRequestCode());
        assertEquals(PendingIntent.FLAG_UPDATE_CURRENT, shadowPendingIntent.getFlags());

        ComponentName actualComponentName = new ComponentName(application, CTLocationUpdateReceiver.class);

        assertEquals(CTGeofenceConstants.ACTION_LOCATION_RECEIVER,
                shadowPendingIntent.getSavedIntent().getAction());
        assertEquals(actualComponentName, shadowPendingIntent.getSavedIntent().getComponent());
    }

    @Test
    public void testGetPendingIntentWhenContextIsNull() {
        PendingIntent actual = PendingIntentFactory.getPendingIntent(null,
                PendingIntentFactory.PENDING_INTENT_LOCATION, PendingIntent.FLAG_NO_CREATE);

        assertNull(actual);
    }

    @Test
    public void testGetPendingIntentWhenInstanceAlreadyExist() {
        // when pendingIntent already exists in system then it should return existing one
        // instead of creating new (update + new)

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent actual1 = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_NO_CREATE);

        assertSame(actual, actual1);
    }

    @Test
    public void testGetPendingIntentWhenInstanceAlreadyExist1() {
        // when pendingIntent already exists in system then it should return existing one
        // instead of creating new (update + update)

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent actual1 = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);

        assertSame(actual, actual1);
    }

    @Test
    public void testGetPendingIntentWhenInstanceCancel() {
        // when pendingIntent already exists in system and then canceled (update + new)

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);

        actual.cancel();

        PendingIntent actual1 = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_NO_CREATE);

        assertNotSame(actual, actual1);
        assertNull(actual1);
    }

    @Test
    public void testGetPendingIntentWhenInstanceCancel1() {
        // when pendingIntent already exists in system and then canceled (update + update)

        PendingIntent actual = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);

        actual.cancel();

        PendingIntent actual1 = PendingIntentFactory.getPendingIntent(application,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT);

        assertNotSame(actual, actual1);
    }
}
