package com.clevertap.android.sdk

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivityManager
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DeviceSpecsTest : BaseTestCase() {

    private val gb = 1024L * 1024L * 1024L

    @Test
    fun `ramTier picks the smallest tier that holds the memory and caps at 16`() {
        assertEquals(8, DeviceSpecs.ramTier((7.4 * gb).toLong()))
        assertEquals(4, DeviceSpecs.ramTier(4 * gb))
        assertEquals(6, DeviceSpecs.ramTier((5.6 * gb).toLong()))
        assertEquals(1, DeviceSpecs.ramTier(gb / 2))
        assertEquals(16, DeviceSpecs.ramTier(24 * gb))
        assertNull(DeviceSpecs.ramTier(0))
    }

    @Test
    @Config(sdk = [33])
    fun `read returns the memory tier, core count and installer`() {
        val memoryInfo = ActivityManager.MemoryInfo().apply { totalMem = (11.2 * gb).toLong() }
        shadowOf(appCtx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).setMemoryInfo(memoryInfo)
        shadowOf(appCtx.packageManager).setInstallSourceInfo(appCtx.packageName, null, "com.android.vending")

        val specs = DeviceSpecs.read(appCtx)

        assertEquals(12, specs.ram)
        assertEquals(Runtime.getRuntime().availableProcessors(), specs.cpu)
        assertEquals("com.android.vending", specs.instSrc)
    }

    @Test
    @Config(sdk = [33])
    fun `read leaves out the installer when there is none`() {
        shadowOf(appCtx.packageManager).setInstallSourceInfo(appCtx.packageName, null, null)

        assertNull(DeviceSpecs.read(appCtx).instSrc)
    }

    @Test
    @Config(sdk = [33])
    fun `read returns the newest exit of this process, not of the app's other processes`() {
        val activityManager = shadowOf(appCtx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        val thisProcess = Application.getProcessName()
        activityManager.addApplicationExitInfo(exitInfo(thisProcess, ApplicationExitInfo.REASON_CRASH, 1))
        activityManager.addApplicationExitInfo(exitInfo(thisProcess, ApplicationExitInfo.REASON_ANR, 6))
        activityManager.addApplicationExitInfo(exitInfo("$thisProcess:push", ApplicationExitInfo.REASON_LOW_MEMORY, 0))

        val lastExit = DeviceSpecs.read(appCtx).lastExit

        assertEquals(DeviceSpecs.LastExit(ApplicationExitInfo.REASON_ANR, 6, IMPORTANCE_FOREGROUND), lastExit)
        // Below API 37 there are no ANR details, even for an ANR exit.
        assertNull(lastExit?.anr)
    }

    @Test
    @Config(sdk = [33])
    fun `read leaves out the last exit when there is no record`() {
        assertNull(DeviceSpecs.read(appCtx).lastExit)
    }

    @Test
    @Config(sdk = [29])
    fun `read leaves out the last exit below api 30`() {
        assertNull(DeviceSpecs.read(appCtx).lastExit)
    }

    private fun exitInfo(processName: String, reason: Int, status: Int): ApplicationExitInfo =
        ShadowActivityManager.ApplicationExitInfoBuilder.newBuilder()
            .setProcessName(processName)
            .setReason(reason)
            .setStatus(status)
            .setImportance(IMPORTANCE_FOREGROUND)
            .build()
}
