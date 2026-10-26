package com.clevertap.android.sdk

import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DeviceStateTest : BaseTestCase() {

    @Test
    fun `thermalLevel maps the seven android statuses onto the four ios states`() {
        assertEquals(0, DeviceState.thermalLevel(PowerManager.THERMAL_STATUS_NONE))
        assertEquals(1, DeviceState.thermalLevel(PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals(1, DeviceState.thermalLevel(PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals(2, DeviceState.thermalLevel(PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals(3, DeviceState.thermalLevel(PowerManager.THERMAL_STATUS_CRITICAL))
        assertEquals(3, DeviceState.thermalLevel(PowerManager.THERMAL_STATUS_EMERGENCY))
        assertEquals(3, DeviceState.thermalLevel(PowerManager.THERMAL_STATUS_SHUTDOWN))
        assertNull(DeviceState.thermalLevel(99))
    }

    @Test
    fun `darkMode reads the night bits and is null when undefined`() {
        assertEquals(true, DeviceState.darkMode(configWithNight(Configuration.UI_MODE_NIGHT_YES)))
        assertEquals(false, DeviceState.darkMode(configWithNight(Configuration.UI_MODE_NIGHT_NO)))
        assertNull(DeviceState.darkMode(configWithNight(Configuration.UI_MODE_NIGHT_UNDEFINED)))
    }

    @Test
    fun `textSize is the font scale rounded to two decimals`() {
        assertEquals(1.15, DeviceState.textSize(Configuration().apply { fontScale = 1.15f }))
        assertEquals(1.0, DeviceState.textSize(Configuration().apply { fontScale = 1f }))
        assertNull(DeviceState.textSize(Configuration().apply { fontScale = 0f }))
    }

    @Test
    @Config(sdk = [33])
    fun `read returns the current power, animation and notification state`() {
        val powerManager = appCtx.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIsPowerSaveMode(true)
        shadowOf(powerManager).setCurrentThermalStatus(PowerManager.THERMAL_STATUS_SEVERE)
        Settings.Global.putFloat(appCtx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        shadowOf(appCtx.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)

        val state = DeviceState.read(appCtx, configWithNight(Configuration.UI_MODE_NIGHT_YES))

        assertEquals(true, state.lowPower)
        assertEquals(2, state.thermal)
        assertEquals(true, state.darkMode)
        assertEquals(true, state.reduceMotion)
        assertEquals(DeviceState.PUSH_STATUS_DENIED, state.pushStatus)
    }

    @Test
    @Config(sdk = [33])
    fun `read reports allowed notifications and animations on by default`() {
        shadowOf(appCtx.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)

        val state = DeviceState.read(appCtx, Configuration())

        assertEquals(false, state.lowPower)
        assertEquals(0, state.thermal)
        assertEquals(false, state.reduceMotion)
        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, state.pushStatus)
    }

    @Test
    @Config(sdk = [23])
    fun `read leaves out thermal and push status below the apis that provide them`() {
        val state = DeviceState.read(appCtx, Configuration())

        assertNull(state.thermal)
        assertNull(state.pushStatus)
    }

    private fun configWithNight(night: Int) = Configuration().apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
    }
}
