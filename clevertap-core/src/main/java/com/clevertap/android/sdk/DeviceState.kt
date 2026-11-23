package com.clevertap.android.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Looper
import androidx.annotation.VisibleForTesting
import android.os.PowerManager
import android.provider.Settings
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor

// On the main thread, about 4x the slowest read seen on a stressed emulator (130 ms).
private const val READ_TIMEOUT_MS = 500L

// Off the main thread a read can wait longer. At a slow cold start reads queue behind each other, and
// 500 ms dropped values like window size from App Launched.
private const val WORKER_READ_TIMEOUT_MS = 3_000L

private fun defaultTimeoutMs(): Long =
    if (Looper.myLooper() == Looper.getMainLooper()) READ_TIMEOUT_MS else WORKER_READ_TIMEOUT_MS

// ApplicationExitInfo.getAnrInfo was added in API 37, Android 17.
private const val API_ANR_INFO = 37

private const val MAX_PARALLEL_READS = 4
private const val MAX_PENDING_READS = 64

// A stuck system call can't be cancelled, only abandoned. A capped view of Dispatchers.IO, so stuck reads
// hold at most MAX_PARALLEL_READS of its threads. Not CTExecutors' IO pool, since the SDK's own tasks wait
// on these reads from that pool, and a stuck read would take a thread from it for good.
@OptIn(ExperimentalCoroutinesApi::class)
private val readExecutor: Executor = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_READS).asExecutor()

// Reads started and not yet finished. Capped so reads queued behind stuck ones can't pile up.
@VisibleForTesting
internal val pendingReads = AtomicInteger()

/**
 * A read running on readExecutor. Start the slow reads of one snapshot before getting any of them,
 * so they wait out one timeout together instead of one after another.
 */
internal class DeviceRead<T> internal constructor(
    private val name: String,
    private val future: Future<T?>?,
    private val timeoutMs: Long,
    private val startNanos: Long
) {

    // True when the read timed out or was skipped, not when it failed, so the caller can try it again.
    @Volatile
    var unfinished = false
        private set

    /** Null if the read failed or ran past its timeout, counted from [startRead], so its af key is left out. */
    fun get(): T? {
        if (future == null) {
            unfinished = true
            return null
        }
        return try {
            val waitedNanos = System.nanoTime() - startNanos
            val leftNanos = (TimeUnit.MILLISECONDS.toNanos(timeoutMs) - waitedNanos).coerceAtLeast(0L)
            future.get(leftNanos, TimeUnit.NANOSECONDS)
        } catch (t: Throwable) {
            // No interrupt. The thread is Dispatchers.IO's, and the flag would stay set for its next task.
            future.cancel(false)
            when (t) {
                is TimeoutException -> {
                    unfinished = true
                    Logger.v("Reading $name for the app fields timed out after $timeoutMs ms, leaving it out")
                }
                is InterruptedException -> {
                    Thread.currentThread().interrupt()
                    Logger.v("Interrupted while reading $name for the app fields")
                }
                else -> Logger.v("Unable to read $name for the app fields", (t as? ExecutionException)?.cause ?: t)
            }
            null
        }
    }
}

/**
 * Starts [read] on readExecutor. Use it only for system service calls, which go to another process and
 * can block. Group the calls to one service in one read, since a stuck service stalls all of them.
 */
@JvmOverloads
internal fun <T> startRead(name: String, timeoutMs: Long = defaultTimeoutMs(), read: () -> T?): DeviceRead<T> {
    val startNanos = System.nanoTime()
    if (pendingReads.incrementAndGet() > MAX_PENDING_READS) {
        pendingReads.decrementAndGet()
        Logger.v("Too many device reads waiting, skipped $name for the app fields")
        return DeviceRead(name, null, timeoutMs, startNanos)
    }
    val future = FutureTask(Callable { read() })
    // Counted down here, not in the callable, so a read cancelled while queued is counted down too.
    readExecutor.execute {
        try {
            future.run()
        } finally {
            pendingReads.decrementAndGet()
        }
    }
    return DeviceRead(name, future, timeoutMs, startNanos)
}

// Null if the read fails or takes longer than its timeout, so its af key is left out.
@JvmOverloads
internal fun <T> readOrNull(name: String, timeoutMs: Long = defaultTimeoutMs(), read: () -> T?): T? =
    startRead(name, timeoutMs, read).get()

/**
 * For values af sent before these timeouts existed. On the main thread they run inline, as they always did,
 * so a busy device can't drop them from App Launched. Off it they get the timeout.
 */
internal fun <T> startExistingRead(name: String, read: () -> T?): DeviceRead<T> {
    if (Looper.myLooper() != Looper.getMainLooper()) return startRead(name, read = read)
    val done = FutureTask(Callable { read() }).apply { run() }
    return DeviceRead(name, done, READ_TIMEOUT_MS, System.nanoTime())
}

internal fun <T> existingReadOrNull(name: String, read: () -> T?): T? = startExistingRead(name, read).get()

/** Runs [read] on the calling thread, so one failing value inside a grouped read doesn't drop the others. */
internal fun <T> catchOrNull(name: String, read: () -> T?): T? =
    try {
        read()
    } catch (e: Exception) {
        Logger.v("Unable to read $name for the app fields", e)
        null
    }

/** Device state sent in the af fields, with the iOS keys. A value that can't be read is null and left out. */
internal data class DeviceState(
    val lowPower: Boolean?,
    val thermal: Int?,
    val darkMode: Boolean?,
    val textSize: Double?,
    val reduceMotion: Boolean?,
    val pushStatus: Int?,
    val batt: Int?,
    val chg: Boolean?,
    val boldText: Boolean?
) {

    /** This state with display values from [configuration]. */
    fun withDisplay(configuration: Configuration): DeviceState =
        copy(
            darkMode = darkMode(configuration),
            textSize = textSize(configuration),
            boldText = boldText(configuration)
        )

    fun withPushStatus(pushStatus: Int?): DeviceState = copy(pushStatus = pushStatus)

    companion object {

        // Same numbers as iOS UNAuthorizationStatus. Android has no not determined, provisional or ephemeral.
        const val PUSH_STATUS_DENIED = 1
        const val PUSH_STATUS_ALLOWED = 2

        /**
         * Reads every value. Makes system calls, so call it off the main thread.
         * DeviceInfo replaces the display values with the resumed activity's, see [withDisplay].
         */
        @JvmStatic
        fun read(context: Context, configuration: Configuration): DeviceState {
            val power = startRead("power state") { readPower(context) }
            val reduceMotion = startRead("animator duration scale") { readReduceMotion(context) }
            val pushStatus = startRead("notification permission") { queryPushStatus(context) }
            val battery = startRead("battery state") { readBattery(context) }

            val powerValues = power.get()
            val batteryValues = battery.get()
            return DeviceState(
                lowPower = powerValues?.lowPower,
                thermal = powerValues?.thermal,
                darkMode = darkMode(configuration),
                textSize = textSize(configuration),
                reduceMotion = reduceMotion.get(),
                pushStatus = pushStatus.get(),
                batt = batteryValues?.batt,
                chg = batteryValues?.chg,
                boldText = boldText(configuration)
            )
        }

        /** Only the display values. No system calls, so safe on the main thread. */
        @JvmStatic
        fun displayOnly(configuration: Configuration): DeviceState = DeviceState(
            lowPower = null,
            thermal = null,
            darkMode = darkMode(configuration),
            textSize = textSize(configuration),
            reduceMotion = null,
            pushStatus = null,
            batt = null,
            chg = null,
            boldText = boldText(configuration)
        )

        /** Null below API 24, where the framework can't report the notification setting. */
        @JvmStatic
        fun pushStatus(notificationsEnabled: Boolean): Int? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
            return if (notificationsEnabled) PUSH_STATUS_ALLOWED else PUSH_STATUS_DENIED
        }

        /** Maps Android's 7 thermal statuses to the iOS 0 to 3 scale (nominal, fair, serious, critical). */
        @JvmStatic
        fun thermalLevel(status: Int): Int? = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> 0
            PowerManager.THERMAL_STATUS_LIGHT,
            PowerManager.THERMAL_STATUS_MODERATE -> 1
            PowerManager.THERMAL_STATUS_SEVERE -> 2
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> 3
            else -> null
        }

        @JvmStatic
        fun darkMode(configuration: Configuration): Boolean? =
            when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> null
            }

        /** Raw font scale, rounded to two decimals since 1.15f widens to 1.149999976158142. */
        @JvmStatic
        fun textSize(configuration: Configuration): Double? =
            configuration.fontScale.takeIf { it > 0f }?.let { Math.round(it * 100) / 100.0 }

        /** True when bold text is on. API 31+ only, UNDEFINED means no value. */
        @JvmStatic
        fun boldText(configuration: Configuration): Boolean? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
            val adjustment = configuration.fontWeightAdjustment
            if (adjustment == Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) return null
            return adjustment > 0
        }

        /** Battery percent rounded down to 10s, so 47 is sent as 40. Null outside 0 to 100. */
        @JvmStatic
        fun batteryBand(level: Int): Int? = if (level in 0..100) level / 10 * 10 else null

        /** True when charging or full, same as iOS. */
        @JvmStatic
        fun chargingFromStatus(status: Int): Boolean? = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL -> true
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
            else -> null
        }

        /** True when Data Saver is on, even if this app is exempt. */
        @JvmStatic
        fun dataSaverOn(restrictBackgroundStatus: Int): Boolean? = when (restrictBackgroundStatus) {
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> false
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED,
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> true
            else -> null
        }

        private fun readPower(context: Context): PowerValues? {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
            return PowerValues(
                lowPower = catchOrNull("low power mode") { powerManager.isPowerSaveMode },
                thermal = catchOrNull("thermal status") { readThermal(powerManager) }
            )
        }

        private fun readThermal(powerManager: PowerManager): Int? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            return thermalLevel(powerManager.currentThermalStatus)
        }

        private fun readBattery(context: Context): BatteryValues = BatteryValues(
            batt = catchOrNull("battery level") {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                // Not getIntProperty, which returns 0 for an unsupported property when the app targets below API 28.
                batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    ?.takeIf { it in 0..100 }
                    ?.let { batteryBand(it.toInt()) }
            },
            chg = catchOrNull("charging state") { readCharging(context) }
        )

        // A duration scale of 0 turns animations off.
        private fun readReduceMotion(context: Context): Boolean =
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

        // Framework call, since NotificationManagerCompat is only compileOnly in core.
        private fun queryPushStatus(context: Context): Int? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return null
            return pushStatus(notificationManager.areNotificationsEnabled())
        }

        // Last battery broadcast, no receiver registered. isCharging() lagged on the emulator, this didn't.
        private fun readCharging(context: Context): Boolean? {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val battery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(null, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(null, filter)
            }
            return battery?.let { chargingFromStatus(it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) }
        }

        // Not part of the cached state. DeviceInfo reads it live with wifi, so the two can't disagree in one af.
        // Needs ACCESS_NETWORK_STATE, which the SDK doesn't declare. Lint misses this check, hence the suppression.
        @JvmStatic
        @SuppressLint("MissingPermission")
        fun readMetered(context: Context): Boolean? {
            if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) return null
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return connectivityManager?.isActiveNetworkMetered
        }

        @JvmStatic
        fun readDataSaver(context: Context): Boolean? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return connectivityManager?.let { dataSaverOn(it.restrictBackgroundStatus) }
        }

        private fun hasPermission(context: Context, permission: String): Boolean =
            context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private data class PowerValues(val lowPower: Boolean?, val thermal: Int?)

    private data class BatteryValues(val batt: Int?, val chg: Boolean?)
}

/** Device facts that can't change while the app runs, read once per launch. Null when unreadable. */
internal data class DeviceSpecs(
    val ram: Int?,
    val cpu: Int?,
    val instSrc: String?,
    val lastExit: LastExit? = null,
    // True when a read timed out, so DeviceInfo reads again instead of keeping this for the session.
    val unfinished: Boolean = false
) {

    /** How this process last died, from ApplicationExitInfo. Its REASON_ and IMPORTANCE_ codes are sent as is. */
    data class LastExit(val reason: Int, val status: Int, val importance: Int, val anr: Anr? = null)

    /** From ApplicationExitInfo.AnrInfo, read only for REASON_ANR. AnrTypes code sent as is. */
    data class Anr(val type: Int, val timeoutMs: Long, val userPerceptible: Boolean)

    companion object {

        private val RAM_TIERS_GB = intArrayOf(1, 2, 3, 4, 6, 8, 12, 16)

        /** All unknown, used to avoid system calls on the main thread. */
        @JvmField
        val UNKNOWN = DeviceSpecs(ram = null, cpu = null, instSrc = null)

        /** Makes system calls, so call it off the main thread. */
        @JvmStatic
        fun read(context: Context): DeviceSpecs {
            // Total memory and the last exit both come from ActivityManager, so one read for the two.
            val activity = startRead("activity manager") {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                activityManager?.let {
                    catchOrNull("total memory") { readRamTier(it) } to catchOrNull("last exit") { readLastExit(it) }
                }
            }
            val instSrc = startRead("install source") { readInstaller(context) }
            val activityValues = activity.get()
            val installer = instSrc.get()
            return DeviceSpecs(
                ram = activityValues?.first,
                // In process, no system service call, so no need for a read thread.
                cpu = Runtime.getRuntime().availableProcessors(),
                instSrc = installer,
                lastExit = activityValues?.second,
                unfinished = activity.unfinished || instSrc.unfinished
            )
        }

        private fun readRamTier(activityManager: ActivityManager): Int? {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return ramTier(memoryInfo.totalMem)
        }

        // The records cover every process of the app, newest first, so take the newest one for this process.
        private fun readLastExit(activityManager: ActivityManager): LastExit? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            val processName = Application.getProcessName()
            return activityManager.getHistoricalProcessExitReasons(null, 0, 0)
                .firstOrNull { it.processName == processName }
                ?.let { LastExit(it.reason, it.status, it.importance, readAnr(it)) }
        }

        private fun readAnr(exitInfo: ApplicationExitInfo): Anr? {
            if (Build.VERSION.SDK_INT < API_ANR_INFO) return null
            // The docs say it is set only for REASON_ANR, but an Android 17 emulator also set it on other exits.
            if (exitInfo.reason != ApplicationExitInfo.REASON_ANR) return null
            return exitInfo.anrInfo?.let { Anr(it.anrType, it.timeoutMillis, it.isUserPerceptible) }
        }

        /** Smallest GB tier that holds the total memory, so 7.4 GB is 8. Above 16 GB is sent as 16. */
        @JvmStatic
        fun ramTier(totalBytes: Long): Int? {
            if (totalBytes <= 0) return null
            val gb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            return RAM_TIERS_GB.firstOrNull { it >= gb } ?: RAM_TIERS_GB.last()
        }

        // Installing store's package name, null for installs like adb.
        private fun readInstaller(context: Context): String? {
            val packageManager = context.packageManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(context.packageName)
            }
        }
    }
}
