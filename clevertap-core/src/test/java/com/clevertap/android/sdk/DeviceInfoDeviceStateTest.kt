package com.clevertap.android.sdk

import android.app.NotificationManager
import android.content.res.Configuration
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeviceInfoDeviceStateTest : BaseTestCase() {

    private val notificationShadow
        get() = shadowOf(appCtx.getSystemService(NotificationManager::class.java))

    @Test
    fun `getters off the main thread read the state on first use when nothing has refreshed it yet`() {
        notificationShadow.setNotificationsEnabled(true)
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, offMainThread { deviceInfo.getPushStatus() })
        // Cached, so the main thread gets it too.
        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())
    }

    @Test
    fun `getters on the main thread make no reads before the first refresh, only display values are sent`() {
        notificationShadow.setNotificationsEnabled(true)
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        assertNull(deviceInfo.getPushStatus())
        assertNull(deviceInfo.getCpuCores())
        assertNull(deviceInfo.getRamTier())
        assertEquals(false, deviceInfo.isDarkMode())

        // Nothing was cached, so the refresh stores a full read.
        deviceInfo.prepareDeviceStateRefresh().call()
        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())
    }

    @Test
    fun `values stay cached until refreshDeviceState reads them again`() {
        notificationShadow.setNotificationsEnabled(true)
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        deviceInfo.prepareDeviceStateRefresh().call()
        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())

        notificationShadow.setNotificationsEnabled(false)
        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())

        deviceInfo.prepareDeviceStateRefresh().call()

        assertEquals(DeviceState.PUSH_STATUS_DENIED, deviceInfo.getPushStatus())
    }

    @Test
    fun `an older refresh that finishes after a newer one is dropped`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        val older = deviceInfo.prepareDeviceStateRefresh()
        val newer = deviceInfo.prepareDeviceStateRefresh()

        notificationShadow.setNotificationsEnabled(true)
        newer.call()
        notificationShadow.setNotificationsEnabled(false)
        older.call()

        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())
    }

    @Test
    fun `display values from the resumed activity apply to a state stored before them`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        deviceInfo.prepareDeviceStateRefresh().call()
        assertEquals(false, deviceInfo.isDarkMode())

        deviceInfo.updateDisplayState(darkActivityConfiguration())

        assertEquals(true, deviceInfo.isDarkMode())
        assertEquals(1.3, deviceInfo.getTextSize())
    }

    @Test
    fun `display values from the resumed activity survive an app wide read that lands later`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        val initRead = deviceInfo.prepareDeviceStateRefresh()

        deviceInfo.updateDisplayState(darkActivityConfiguration())
        initRead.call()

        assertEquals(true, deviceInfo.isDarkMode())
        assertEquals(1.3, deviceInfo.getTextSize())
    }

    @Test
    fun `display values from the resumed activity apply before anything has been read`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        deviceInfo.updateDisplayState(darkActivityConfiguration())

        assertEquals(true, deviceInfo.isDarkMode())
    }

    @Test
    fun `refreshPushStatus updates only the push status`() {
        notificationShadow.setNotificationsEnabled(true)
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        deviceInfo.prepareDeviceStateRefresh().call()
        deviceInfo.updateDisplayState(darkActivityConfiguration())
        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())

        notificationShadow.setNotificationsEnabled(false)
        deviceInfo.refreshPushStatus()

        assertEquals(DeviceState.PUSH_STATUS_DENIED, deviceInfo.getPushStatus())
        assertEquals(true, deviceInfo.isDarkMode())
    }

    private fun darkActivityConfiguration() = Configuration().apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        fontScale = 1.3f
    }

    @Test
    fun `app fields carry the device state keys`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.isLowPowerMode() } returns true
        every { deviceInfo.getThermalLevel() } returns 3
        every { deviceInfo.isDarkMode() } returns false
        every { deviceInfo.getTextSize() } returns 1.3
        every { deviceInfo.isReduceMotion() } returns true
        every { deviceInfo.getPushStatus() } returns DeviceState.PUSH_STATUS_ALLOWED

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        assertEquals(true, appFields.get("lowPower"))
        assertEquals(3, appFields.get("thermal"))
        assertEquals(false, appFields.get("darkMode"))
        assertEquals(1.3, appFields.get("textSize"))
        assertEquals(true, appFields.get("reduceMotion"))
        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, appFields.get("pushStatus"))
    }

    @Test
    fun `app fields leave out device state values that are unknown`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.isLowPowerMode() } returns null
        every { deviceInfo.getThermalLevel() } returns null
        every { deviceInfo.isDarkMode() } returns null
        every { deviceInfo.getTextSize() } returns null
        every { deviceInfo.isReduceMotion() } returns null
        every { deviceInfo.getPushStatus() } returns null

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        listOf("lowPower", "thermal", "darkMode", "textSize", "reduceMotion", "pushStatus").forEach {
            assertFalse(appFields.has(it), "$it should be left out")
        }
        // Other keys are still written.
        assertEquals(true, appFields.has("dt"))
    }

    @Test
    fun `app fields carry the battery, network, accessibility and device spec keys`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.getBatteryLevel() } returns 40
        every { deviceInfo.isCharging() } returns true
        every { deviceInfo.getRamTier() } returns 8
        every { deviceInfo.getCpuCores() } returns 8
        every { deviceInfo.isMeteredNetwork() } returns false
        every { deviceInfo.isDataSaverOn() } returns true
        every { deviceInfo.isBoldText() } returns false
        every { deviceInfo.getInstallSource() } returns "com.android.vending"

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        assertEquals(40, appFields.get("batt"))
        assertEquals(true, appFields.get("chg"))
        assertEquals(8, appFields.get("ram"))
        assertEquals(8, appFields.get("cpu"))
        assertEquals(false, appFields.get("metered"))
        assertEquals(true, appFields.get("lowData"))
        assertEquals(false, appFields.get("boldText"))
        assertFalse(appFields.has("screenReader"))
        assertEquals("com.android.vending", appFields.get("instSrc"))
    }

    @Test
    fun `app fields leave out battery, network, accessibility and device spec values that are unknown`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.getBatteryLevel() } returns null
        every { deviceInfo.isCharging() } returns null
        every { deviceInfo.getRamTier() } returns null
        every { deviceInfo.getCpuCores() } returns null
        every { deviceInfo.isMeteredNetwork() } returns null
        every { deviceInfo.isDataSaverOn() } returns null
        every { deviceInfo.isBoldText() } returns null
        every { deviceInfo.getInstallSource() } returns null

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        listOf(
            "batt", "chg", "ram", "cpu", "metered", "lowData",
            "boldText", "instSrc"
        ).forEach {
            assertFalse(appFields.has(it), "$it should be left out")
        }
        assertEquals(true, appFields.has("dt"))
    }

    @Test
    fun `device specs are read once and then cached`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        val first = offMainThread { deviceInfo.getCpuCores() }

        assertEquals(Runtime.getRuntime().availableProcessors(), first)
        assertEquals(first, deviceInfo.getCpuCores())
    }

    // Robolectric runs tests on the main looper, so off-main reads run here.
    private fun <T> offMainThread(block: () -> T): T {
        var result: Result<T>? = null
        thread { result = runCatching(block) }.join()
        return result!!.getOrThrow()
    }
}
