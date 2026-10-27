package com.clevertap.android.sdk

import android.app.ActivityManager
import android.content.Context
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
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
}
