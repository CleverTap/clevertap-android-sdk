package com.clevertap.android.sdk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.BatteryManager
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DeviceSignalsTest : BaseTestCase() {

    @Test
    fun `batteryBand rounds down to a band of 10 and drops values outside 0 to 100`() {
        assertEquals(40, DeviceState.batteryBand(47))
        assertEquals(0, DeviceState.batteryBand(9))
        assertEquals(100, DeviceState.batteryBand(100))
        assertNull(DeviceState.batteryBand(Int.MIN_VALUE))
        assertNull(DeviceState.batteryBand(101))
    }

    @Test
    fun `chargingFromStatus counts charging and full as charging`() {
        assertEquals(true, DeviceState.chargingFromStatus(BatteryManager.BATTERY_STATUS_CHARGING))
        assertEquals(true, DeviceState.chargingFromStatus(BatteryManager.BATTERY_STATUS_FULL))
        assertEquals(false, DeviceState.chargingFromStatus(BatteryManager.BATTERY_STATUS_DISCHARGING))
        assertEquals(false, DeviceState.chargingFromStatus(BatteryManager.BATTERY_STATUS_NOT_CHARGING))
        assertNull(DeviceState.chargingFromStatus(BatteryManager.BATTERY_STATUS_UNKNOWN))
    }

    @Test
    fun `dataSaverOn is true when Data Saver is on, also for an exempt app`() {
        assertEquals(false, DeviceState.dataSaverOn(ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED))
        assertEquals(true, DeviceState.dataSaverOn(ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED))
        assertEquals(true, DeviceState.dataSaverOn(ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED))
        assertNull(DeviceState.dataSaverOn(0))
    }

    @Test
    @Config(sdk = [33])
    fun `boldText reads the font weight adjustment and ignores the undefined value`() {
        assertEquals(true, DeviceState.boldText(Configuration().apply { fontWeightAdjustment = 300 }))
        assertEquals(false, DeviceState.boldText(Configuration().apply { fontWeightAdjustment = 0 }))
        assertNull(
            DeviceState.boldText(
                Configuration().apply { fontWeightAdjustment = Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED }
            )
        )
    }

    @Test
    @Config(sdk = [30])
    fun `boldText is left out below API 31`() {
        assertNull(DeviceState.boldText(Configuration()))
    }

    @Test
    @Config(sdk = [33])
    fun `read returns battery, data saver and bold text`() {
        val battery = shadowOf(appCtx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        battery.setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 47)
        appCtx.sendStickyBroadcast(
            Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_FULL)
        )
        shadowOf(appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .setRestrictBackgroundStatus(ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED)

        val state = DeviceState.read(appCtx, Configuration().apply { fontWeightAdjustment = 300 })

        assertEquals(40, state.batt)
        assertEquals(true, state.chg)
        assertEquals(true, state.lowData)
        assertEquals(true, state.boldText)
    }

    @Test
    @Config(sdk = [33])
    fun `metered is left out without ACCESS_NETWORK_STATE and read with it`() {
        shadowApplication.denyPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        assertNull(DeviceState.read(appCtx, Configuration()).metered)

        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        assertNotNull(DeviceState.read(appCtx, Configuration()).metered)
    }

    @Test
    @Config(sdk = [33])
    fun `withDisplay takes bold text from the activity configuration`() {
        val state = DeviceState.read(appCtx, Configuration().apply { fontWeightAdjustment = 0 })

        assertEquals(true, state.withDisplay(Configuration().apply { fontWeightAdjustment = 300 }).boldText)
    }
}
