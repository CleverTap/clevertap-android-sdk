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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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

    @Test
    fun `readOrNull returns the value of a read that finishes in time`() {
        assertEquals(42, readOrNull("fast read") { 42 })
    }

    @Test
    fun `readOrNull returns null when the read throws`() {
        assertNull(readOrNull<Int>("failing read") { throw SecurityException("no permission") })
    }

    @Test
    fun `readOrNull stops waiting and returns null when the read is too slow`() {
        val release = CountDownLatch(1)
        val start = System.nanoTime()

        val result = readOrNull("stuck read", timeoutMs = 100) { release.await(); 1 }

        val waitedMs = (System.nanoTime() - start) / 1_000_000
        release.countDown()
        assertNull(result)
        assertTrue(waitedMs < 1_000, "waited $waitedMs ms")
    }

    @Test
    fun `readOrNull queues reads beyond the pool size instead of dropping them`() {
        val callers = 8
        val start = CountDownLatch(1)
        val results = arrayOfNulls<Int>(callers)
        val threads = (0 until callers).map { i ->
            Thread {
                start.await()
                results[i] = readOrNull("busy read $i") { Thread.sleep(20); i }
            }.apply { start() }
        }

        start.countDown()
        threads.forEach { it.join(5_000) }

        assertEquals((0 until callers).toList(), results.toList())
    }

    @Test
    fun `a slow read is dropped on the main thread but kept off it`() {
        // Robolectric runs the test on the main looper. 800 ms is past the main thread timeout only.
        val onMain = readOrNull("slow read") { Thread.sleep(800); 1 }
        var offMain: Int? = null
        Thread { offMain = readOrNull("slow read") { Thread.sleep(800); 2 } }.apply { start(); join(5_000) }

        assertNull(onMain)
        assertEquals(2, offMain)
    }

    @Test
    fun `a read of an existing value runs inline on the main thread, so a full queue can't drop it`() {
        val release = CountDownLatch(1)
        val stuck = (0 until 64).map { startRead("stuck read $it", timeoutMs = 10_000) { release.await(); it } }
        var readThread: Thread? = null

        // Robolectric runs the test on the main looper.
        val existing = existingReadOrNull("existing value") { readThread = Thread.currentThread(); 7 }
        val added = readOrNull("new value") { 8 }

        release.countDown()
        stuck.forEach { it.get() }
        assertEquals(7, existing)
        assertEquals(Thread.currentThread(), readThread)
        assertNull(added)
        assertNull(existingReadOrNull<Int>("failing existing value") { throw SecurityException("no permission") })
    }

    @Test
    fun `a timed out read does not leave its shared thread interrupted`() {
        val release = AtomicBoolean(false)
        val finished = CountDownLatch(1)
        var interrupted = true

        // Spins like a system call that never looks at the interrupt flag.
        val result = readOrNull("stuck read", timeoutMs = 100) {
            while (!release.get()) Thread.yield()
            interrupted = Thread.currentThread().isInterrupted
            finished.countDown()
            1
        }

        release.set(true)
        assertNull(result)
        assertTrue(finished.await(5, TimeUnit.SECONDS))
        assertFalse(interrupted)
    }

    @Test
    fun `reads beyond the pending cap are skipped until the waiting ones run`() {
        val release = CountDownLatch(1)
        val stuck = (0 until 64).map { startRead("stuck read $it", timeoutMs = 10_000) { release.await(); it } }
        var skippedReadRan = false

        val skipped = readOrNull("extra read", timeoutMs = 10_000) { skippedReadRan = true; 1 }

        release.countDown()
        assertNull(skipped)
        assertFalse(skippedReadRan)
        assertEquals((0 until 64).toList(), stuck.map { it.get() })
        assertEquals(7, readOrNull("read after the queue drained", timeoutMs = 5_000) { 7 })
    }

    @Test
    fun `reads started together wait out one timeout, not one each`() {
        val release = CountDownLatch(1)
        val start = System.nanoTime()

        val reads = (0 until 3).map { startRead("stuck read $it", timeoutMs = 300) { release.await(); it } }
        val results = reads.map { it.get() }

        val waitedMs = (System.nanoTime() - start) / 1_000_000
        release.countDown()
        assertEquals(listOf(null, null, null), results)
        assertTrue(waitedMs < 700, "waited $waitedMs ms")
    }

    @Test
    fun `catchOrNull keeps a failing value from dropping the rest of its group`() {
        assertEquals(1, catchOrNull("fine value") { 1 })
        assertNull(catchOrNull<Int>("failing value") { throw SecurityException("no permission") })
    }

    private fun configWithNight(night: Int) = Configuration().apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
    }
}
