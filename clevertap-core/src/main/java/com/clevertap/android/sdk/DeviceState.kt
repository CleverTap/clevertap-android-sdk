package com.clevertap.android.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

// Log why a read failed, since its af key is left out.
private inline fun <T> readOrNull(name: String, read: () -> T?): T? =
    runCatching(read)
        .onFailure { Logger.v("Unable to read $name for the app fields", it) }
        .getOrNull()

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
    val metered: Boolean?,
    val lowData: Boolean?,
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
            val powerManager = readOrNull("power manager") {
                context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            }
            val batteryManager = readOrNull("battery manager") {
                context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            }
            return DeviceState(
                lowPower = readOrNull("low power mode") { powerManager?.isPowerSaveMode },
                thermal = readOrNull("thermal status") { readThermal(powerManager) },
                darkMode = darkMode(configuration),
                textSize = textSize(configuration),
                reduceMotion = readOrNull("animator duration scale") { readReduceMotion(context) },
                pushStatus = readPushStatus(context),
                batt = readOrNull("battery level") {
                    batteryManager?.let { batteryBand(it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)) }
                },
                chg = readOrNull("charging state") { readCharging(context) },
                metered = readOrNull("metered network") { readMetered(context) },
                lowData = readOrNull("data saver") { readDataSaver(context) },
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
            metered = null,
            lowData = null,
            boldText = boldText(configuration)
        )

        /** Reads only push permission. Makes a system call, so call it off the main thread. */
        @JvmStatic
        fun readPushStatus(context: Context): Int? =
            readOrNull("notification permission") { queryPushStatus(context) }

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

        private fun readThermal(powerManager: PowerManager?): Int? {
            if (powerManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            return thermalLevel(powerManager.currentThermalStatus)
        }

        // A duration scale of 0 turns animations off.
        private fun readReduceMotion(context: Context): Boolean =
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

        // Framework call, since NotificationManagerCompat is only compileOnly in core.
        private fun queryPushStatus(context: Context): Int? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return null
            return if (notificationManager.areNotificationsEnabled()) PUSH_STATUS_ALLOWED else PUSH_STATUS_DENIED
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

        // Needs ACCESS_NETWORK_STATE, which the SDK doesn't declare. Lint misses this check, hence the suppression.
        @SuppressLint("MissingPermission")
        private fun readMetered(context: Context): Boolean? {
            if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) return null
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return connectivityManager?.isActiveNetworkMetered
        }

        private fun readDataSaver(context: Context): Boolean? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return connectivityManager?.let { dataSaverOn(it.restrictBackgroundStatus) }
        }

        private fun hasPermission(context: Context, permission: String): Boolean =
            context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}

/** Device facts that can't change while the app runs, read once per launch. Null when unreadable. */
internal data class DeviceSpecs(
    val ram: Int?,
    val cpu: Int?,
    val instSrc: String?
) {

    companion object {

        private val RAM_TIERS_GB = intArrayOf(1, 2, 3, 4, 6, 8, 12, 16)

        /** All unknown, used to avoid system calls on the main thread. */
        @JvmField
        val UNKNOWN = DeviceSpecs(ram = null, cpu = null, instSrc = null)

        /** Makes system calls, so call it off the main thread. */
        @JvmStatic
        fun read(context: Context): DeviceSpecs = DeviceSpecs(
            ram = readOrNull("total memory") {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                activityManager?.let {
                    val memoryInfo = ActivityManager.MemoryInfo()
                    it.getMemoryInfo(memoryInfo)
                    ramTier(memoryInfo.totalMem)
                }
            },
            cpu = readOrNull("processor count") { Runtime.getRuntime().availableProcessors() },
            instSrc = readOrNull("install source") { readInstaller(context) }
        )

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
