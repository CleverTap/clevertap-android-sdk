package com.clevertap.android.geofence

import android.app.PendingIntent
import android.content.ComponentName
import org.junit.Assert
import org.junit.Test
import org.robolectric.Shadows

class PendingIntentFactoryTest : BaseTestCase() {
    @Test
    fun testGetPendingIntentForFlagNoCreate() {
        // when pendingIntent flag is no create

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_NO_CREATE
        )

        Assert.assertNull(actual)
    }

    @Test
    fun testGetPendingIntentForFlagUpdateCurrent() {
        // when pendingIntent flag is no create

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )

        Assert.assertNotNull(actual)
    }

    @Test
    fun testGetPendingIntentForGeofence() {
        // when pendingIntent type is geofence

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shadowPendingIntent = Shadows.shadowOf(actual)

        Assert.assertTrue(shadowPendingIntent.isBroadcastIntent)
        Assert.assertEquals(1001001, shadowPendingIntent.requestCode.toLong())
        Assert.assertEquals(
            PendingIntent.FLAG_UPDATE_CURRENT.toLong(),
            shadowPendingIntent.flags.toLong()
        )

        val actualComponentName = ComponentName(application, CTGeofenceReceiver::class.java)

        Assert.assertEquals(
            CTGeofenceConstants.ACTION_GEOFENCE_RECEIVER,
            shadowPendingIntent.savedIntent.action
        )
        Assert.assertEquals(
            actualComponentName,
            shadowPendingIntent.savedIntent.component
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPendingIntentForInvalidType() {
        // when pendingIntent type is invalid

        PendingIntentFactory.getPendingIntent(
            application,
            9, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @Test
    fun testGetPendingIntentForLocation() {
        // when pendingIntent type is location

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_LOCATION, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shadowPendingIntent = Shadows.shadowOf(actual)

        Assert.assertTrue(shadowPendingIntent.isBroadcastIntent)
        Assert.assertEquals(10100111, shadowPendingIntent.requestCode.toLong())
        Assert.assertEquals(
            PendingIntent.FLAG_UPDATE_CURRENT.toLong(),
            shadowPendingIntent.flags.toLong()
        )

        val actualComponentName = ComponentName(application, CTLocationUpdateReceiver::class.java)

        Assert.assertEquals(
            CTGeofenceConstants.ACTION_LOCATION_RECEIVER,
            shadowPendingIntent.savedIntent.action
        )
        Assert.assertEquals(
            actualComponentName,
            shadowPendingIntent.savedIntent.component
        )
    }

    @Test
    fun testGetPendingIntentWhenContextIsNull() {
        val actual = PendingIntentFactory.getPendingIntent(
            null,
            PendingIntentFactory.PENDING_INTENT_LOCATION, PendingIntent.FLAG_NO_CREATE
        )

        Assert.assertNull(actual)
    }

    @Test
    fun testGetPendingIntentWhenInstanceAlreadyExist() {
        // when pendingIntent already exists in system then it should return existing one
        // instead of creating new (update + new)

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val actual1 = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_NO_CREATE
        )

        Assert.assertSame(actual, actual1)
    }

    @Test
    fun testGetPendingIntentWhenInstanceAlreadyExist1() {
        // when pendingIntent already exists in system then it should return existing one
        // instead of creating new (update + update)

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val actual1 = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )

        Assert.assertSame(actual, actual1)
    }

    @Test
    fun testGetPendingIntentWhenInstanceCancel() {
        // when pendingIntent already exists in system and then canceled (update + new)

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )

        actual!!.cancel()

        val actual1 = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_NO_CREATE
        )

        Assert.assertNotSame(actual, actual1)
        Assert.assertNull(actual1)
    }

    @Test
    fun testGetPendingIntentWhenInstanceCancel1() {
        // when pendingIntent already exists in system and then canceled (update + update)

        val actual = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )

        actual!!.cancel()

        val actual1 = PendingIntentFactory.getPendingIntent(
            application,
            PendingIntentFactory.PENDING_INTENT_GEOFENCE, PendingIntent.FLAG_UPDATE_CURRENT
        )

        Assert.assertNotSame(actual, actual1)
    }
}
