package com.clevertap.android.sdk

import android.app.NotificationManager
import android.content.res.Configuration
import android.telephony.TelephonyManager
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun `a full refresh that overlaps refreshPushStatus keeps the header's push status`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        deviceInfo.prepareDeviceStateRefresh().call()

        withPushRefreshDuringRead(deviceInfo) {
            deviceInfo.prepareDeviceStateRefresh().call()
        }

        assertEquals(DeviceState.PUSH_STATUS_DENIED, deviceInfo.getPushStatus())
    }

    @Test
    fun `an init refresh that overlaps refreshPushStatus on an empty cache keeps the header's push status`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        withPushRefreshDuringRead(deviceInfo) {
            deviceInfo.prepareDeviceStateRefresh().call()
        }

        assertEquals(DeviceState.PUSH_STATUS_DENIED, deviceInfo.getPushStatus())
    }

    @Test
    fun `a lazy read that overlaps refreshPushStatus keeps the header's push status`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        val pushStatus = withPushRefreshDuringRead(deviceInfo) {
            offMainThread { deviceInfo.getPushStatus() }
        }

        assertEquals(DeviceState.PUSH_STATUS_DENIED, pushStatus)
    }

    @Test
    fun `a full refresh queued before refreshPushStatus but read after stores its own push status`() {
        notificationShadow.setNotificationsEnabled(true)
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        deviceInfo.prepareDeviceStateRefresh().call()
        val queued = deviceInfo.prepareDeviceStateRefresh()
        notificationShadow.setNotificationsEnabled(false)
        deviceInfo.refreshPushStatus()

        notificationShadow.setNotificationsEnabled(true)
        queued.call()

        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())
    }

    @Test
    fun `a lazy read that starts after refreshPushStatus stores its own push status`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        notificationShadow.setNotificationsEnabled(false)
        deviceInfo.refreshPushStatus()

        notificationShadow.setNotificationsEnabled(true)

        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, offMainThread { deviceInfo.getPushStatus() })
    }

    @Test
    fun `getters on the main thread send a push status already read before the first full read`() {
        notificationShadow.setNotificationsEnabled(true)
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        deviceInfo.refreshPushStatus()

        assertEquals(DeviceState.PUSH_STATUS_ALLOWED, deviceInfo.getPushStatus())
        assertNull(deviceInfo.getCpuCores())
    }

    // The full read sees push allowed, then a header reads denied before the full read stores.
    private fun <T> withPushRefreshDuringRead(deviceInfo: DeviceInfo, block: () -> T): T {
        mockkObject(DeviceState.Companion)
        try {
            every { DeviceState.read(any(), any()) } answers {
                notificationShadow.setNotificationsEnabled(true)
                val older = callOriginal()
                notificationShadow.setNotificationsEnabled(false)
                deviceInfo.refreshPushStatus()
                older
            }
            return block()
        } finally {
            unmockkObject(DeviceState.Companion)
        }
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
        every { deviceInfo.liveNetworkInfo } returns DeviceInfo.LiveNetworkInfo(null, null, null, false, true)
        every { deviceInfo.isBoldText() } returns false
        every { deviceInfo.getInstallSource() } returns "com.android.vending"

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), true, false)

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
        every { deviceInfo.liveNetworkInfo } returns DeviceInfo.LiveNetworkInfo(null, null, null, null, null)
        every { deviceInfo.isBoldText() } returns null
        every { deviceInfo.getInstallSource() } returns null

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), true, false)

        listOf(
            "batt", "chg", "ram", "cpu", "metered", "lowData",
            "boldText", "instSrc"
        ).forEach {
            assertFalse(appFields.has(it), "$it should be left out")
        }
        assertEquals(true, appFields.has("dt"))
    }

    @Test
    fun `app fields leave out metered and data saver when network info reporting is off`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.liveNetworkInfo } returns DeviceInfo.LiveNetworkInfo(null, null, null, false, true)

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        assertFalse(appFields.has("metered"))
        assertFalse(appFields.has("lowData"))
    }

    @Test
    fun `app fields take wifi, radio, bluetooth, metered and data saver from one grouped read`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.liveNetworkInfo } returns DeviceInfo.LiveNetworkInfo(true, "4G", false, true, false)

        val appFields = CTJsonConverter.from(deviceInfo, CoreMetaData(), true, false)

        assertEquals(true, appFields.getBoolean("wifi"))
        assertEquals("4G", appFields.getString("Radio"))
        assertEquals(false, appFields.getBoolean("BluetoothEnabled"))
        assertEquals(true, appFields.getBoolean("metered"))
        assertEquals(false, appFields.getBoolean("lowData"))
        verify(exactly = 0) { deviceInfo.isWifiConnected() }
        verify(exactly = 0) { deviceInfo.getNetworkType() }
        verify(exactly = 0) { deviceInfo.isBluetoothEnabled() }
    }

    @Test
    fun `app fields leave out wifi, radio and bluetooth when their read did not finish or reporting is off`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.liveNetworkInfo } returns null

        val unfinished = CTJsonConverter.from(deviceInfo, CoreMetaData(), true, false)
        every { deviceInfo.liveNetworkInfo } returns DeviceInfo.LiveNetworkInfo(true, "4G", false, true, false)
        val reportingOff = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        listOf("wifi", "Radio", "BluetoothEnabled", "metered", "lowData").forEach {
            assertFalse(unfinished.has(it), "$it should be left out")
            assertFalse(reportingOff.has(it), "$it should be left out")
        }
    }

    @Test
    fun `live network info leaves out what it can't read without dropping the read`() {
        // No network monitor and no bluetooth permission here, so each value is null but the read finishes.
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        val info = deviceInfo.getLiveNetworkInfo()

        assertNotNull(info)
        assertNull(info.radio)
        assertNull(deviceInfo.getNetworkType())
    }

    @Test
    fun `app fields carry the last exit, and leave it out when there is no record`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.getLastExitReason() } returns 6
        every { deviceInfo.getLastExitStatus() } returns 0
        every { deviceInfo.getLastExitImportance() } returns 100

        val withExit = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)
        every { deviceInfo.getLastExitReason() } returns null
        every { deviceInfo.getLastExitStatus() } returns null
        every { deviceInfo.getLastExitImportance() } returns null
        val withoutExit = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        assertEquals(6, withExit.getInt("exitRsn"))
        assertEquals(0, withExit.getInt("exitStatus"))
        assertEquals(100, withExit.getInt("exitImp"))
        listOf("exitRsn", "exitStatus", "exitImp").forEach {
            assertFalse(withoutExit.has(it), "$it should be left out")
        }
    }

    @Test
    fun `app fields carry the last anr details, and leave them out when the exit was not an anr`() {
        val deviceInfo = mockk<DeviceInfo>(relaxed = true)
        every { deviceInfo.context } returns appCtx
        every { deviceInfo.getLastAnrType() } returns 2
        every { deviceInfo.getLastAnrTimeoutMs() } returns 5000L
        every { deviceInfo.isLastAnrUserPerceptible() } returns true

        val withAnr = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)
        every { deviceInfo.getLastAnrType() } returns null
        every { deviceInfo.getLastAnrTimeoutMs() } returns null
        every { deviceInfo.isLastAnrUserPerceptible() } returns null
        val withoutAnr = CTJsonConverter.from(deviceInfo, CoreMetaData(), false, false)

        assertEquals(2, withAnr.getInt("anrType"))
        assertEquals(5000L, withAnr.getLong("anrTimeout"))
        assertEquals(true, withAnr.getBoolean("anrUser"))
        listOf("anrType", "anrTimeout", "anrUser").forEach {
            assertFalse(withoutAnr.has(it), "$it should be left out")
        }
    }

    @Test
    fun `device specs are read once and then cached`() {
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)
        val first = offMainThread { deviceInfo.getCpuCores() }

        assertEquals(Runtime.getRuntime().availableProcessors(), first)
        assertEquals(first, deviceInfo.getCpuCores())
    }

    @Test
    fun `cached info reads package, telephony and window values through grouped reads`() {
        val telephony = shadowOf(appCtx.getSystemService(TelephonyManager::class.java))
        telephony.setNetworkOperatorName("Test Carrier")
        telephony.setSimCountryIso("in")
        val deviceInfo = DeviceInfo(appCtx, cleverTapInstanceConfig, null, CoreMetaData(), null)

        val packageInfo = appCtx.packageManager.getPackageInfo(appCtx.packageName, 0)
        @Suppress("DEPRECATION")
        assertEquals(packageInfo.versionCode, deviceInfo.getBuild())
        assertEquals(packageInfo.versionName, deviceInfo.getVersionName())
        assertEquals("Test Carrier", deviceInfo.getCarrier())
        assertEquals("in", deviceInfo.getCountryCode())
        assertTrue(deviceInfo.getDPI() > 0)
    }

    // Robolectric runs tests on the main looper, so off-main reads run here.
    private fun <T> offMainThread(block: () -> T): T {
        var result: Result<T>? = null
        thread { result = runCatching(block) }.join()
        return result!!.getOrThrow()
    }
}
