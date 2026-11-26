package com.clevertap.android.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.UiModeManager
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display.DEFAULT_DISPLAY
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.inapp.InAppController.Companion.LOCAL_INAPP_COUNT
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.NetworkMonitor
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import java.lang.reflect.Method
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong
import org.json.JSONObject

// Public (restricted) because already-Kotlin public types (e.g. inapp.TriggerManager) take it in
// their signatures.
@RestrictTo(Scope.LIBRARY_GROUP)
open class DeviceInfo internal constructor(
    val context: Context,
    private val config: CleverTapInstanceConfig,
    @Suppress("UNUSED_PARAMETER") cleverTapID: String?,
    private val coreMetaData: CoreMetaData,
    private val networkMonitor: NetworkMonitor?
) {

    /** Immutable device properties, computed once (in declaration order) off the caller thread. */
    private inner class DeviceCachedInfo {

        val versionName: String? = readVersionName()

        val osName: String = OS_NAME

        val osVersion: String = Build.VERSION.RELEASE

        val manufacturer: String = Build.MANUFACTURER

        val model: String = Build.MODEL.replace(manufacturer, "")

        val carrier: String? = readCarrier()

        val build: Int = readBuild()

        val bluetoothVersion: String = readBluetoothVersion()

        val countryCode: String? = readCountryCode()

        val sdkVersion: Int = BuildConfig.VERSION_CODE

        private val windowSize: WindowSize = readWindowSize()

        val width: Double = windowSize.width

        val height: Double = windowSize.height

        val dpi: Int = windowSize.localDpi

        var localInAppCount: Int = StorageHelper.getInt(context, LOCAL_INAPP_COUNT, 0)

        val locale: String = readDeviceLocale()

        val appBucket: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) readAppBucket() else null

        private fun readWindowSize(): WindowSize {
            val wm = getWindowManager()

            if (wm == null) {
                config.logger.verbose(
                    config.accountId,
                    "WindowManager is null, returning zero dimension for width/height"
                )
                return WindowSize(0, 0.0, 0.0)
            }

            val localWidth: Int
            val localHeight: Int
            val localXDpi: Float
            val localYDpi: Float
            val localDpi: Int

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = wm.currentWindowMetrics
                val configuration = context.resources.configuration
                val insets = windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemGestures())

                localWidth = windowMetrics.bounds.width() - insets.right - insets.left
                localHeight = windowMetrics.bounds.height() - insets.top - insets.bottom

                localXDpi = configuration.densityDpi.toFloat()
                localYDpi = configuration.densityDpi.toFloat()
                localDpi = configuration.densityDpi
            } else {
                val dm = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getMetrics(dm)

                localWidth = dm.widthPixels
                localHeight = dm.heightPixels
                localXDpi = dm.xdpi
                localYDpi = dm.ydpi
                localDpi = dm.densityDpi
            }

            // legacy code -> conversion to double is copied
            // (int / float is a float division, widened to double, as in Java)
            val doubleWidth = (localWidth / localXDpi).toDouble()
            val doubleHeight = (localHeight / localYDpi).toDouble()
            return WindowSize(localDpi, toTwoPlaces(doubleWidth), toTwoPlaces(doubleHeight))
        }

        private fun readBluetoothVersion(): String = when {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> "ble"
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) -> "classic"
            else -> "none"
        }

        private fun readBuild(): Int = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            config.logger.debug(config.accountId, "Unable to get app build", e)
            0
        }

        private fun readCarrier(): String? = try {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager?
            manager?.networkOperatorName
        } catch (e: Exception) {
            // Failed to get network operator name from network
            null
        }

        private fun readCountryCode(): String? = try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager?
            if (tm != null) tm.simCountryIso else ""
        } catch (ignore: Throwable) {
            ""
        }

        /**
         * This method is used for devices above API 28
         * This method gets the standby values for app.Standby buckets are divided into the following:-
         * STANDBY_BUCKET_ACTIVE - The app was used very recently, currently in use or likely to be used very soon.
         * STANDBY_BUCKET_FREQUENT - The app was used in the last few days and/or likely to be used in the next few days.
         * STANDBY_BUCKET_RARE - The app has not be used for several days and/or is unlikely to be used for several days.
         * STANDBY_BUCKET_RESTRICTED - The app has not be used for several days, is unlikely to be used for several days, and has
         * been misbehaving in some manner.
         * STANDBY_BUCKET_WORKING_SET - The app was used recently and/or likely to be used in the next few hours.
         *
         * @return one of the possible String value of AppStandbyBucket(). If no AppBucket info is found,
         * returns empty String
         */
        @RequiresApi(api = Build.VERSION_CODES.P)
        private fun readAppBucket(): String {
            // Java dereferenced the service without a null check; a missing service still fails here.
            val usm = checkNotNull(context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager?) {
                "UsageStatsManager is not available"
            }
            return when (usm.appStandbyBucket) {
                UsageStatsManager.STANDBY_BUCKET_ACTIVE -> STANDBY_BUCKET_ACTIVE
                UsageStatsManager.STANDBY_BUCKET_FREQUENT -> STANDBY_BUCKET_FREQUENT
                UsageStatsManager.STANDBY_BUCKET_RARE -> STANDBY_BUCKET_RARE
                UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> STANDBY_BUCKET_RESTRICTED
                UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> STANDBY_BUCKET_WORKING_SET
                else -> ""
            }
        }

        private fun readVersionName(): String? = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            config.logger.debug(config.accountId, "Unable to get app version", e)
            null
        }

        private fun readDeviceLocale(): String {
            val language = Locale.getDefault().language.ifEmpty { "xx" }
            val country = Locale.getDefault().country.ifEmpty { "XX" }
            return "${language}_$country"
        }

        private fun toTwoPlaces(n: Double): Double = (n * 100).roundToLong() / 100.0
    }

    /**
     * A simple class representing the size of a window.
     * It stores the local DPI (dots per inch) along with the window's width and height.
     */
    private class WindowSize(val localDpi: Int, val width: Double, val height: Double)

    private fun getWindowManager(): WindowManager? {
        // (The Java version null-checked the context here; it is non-null in Kotlin.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val primaryDisplay = context.getSystemService(DisplayManager::class.java)?.getDisplay(DEFAULT_DISPLAY)
                if (primaryDisplay != null) {
                    val windowContext = context.createDisplayContext(primaryDisplay)
                        .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION, null)
                    return windowContext.getSystemService(WindowManager::class.java)
                }
            } catch (e: Exception) {
                config.logger.verbose(config.accountId, "Window context creation failed", e)
            }
        }

        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
    }

    /**
     * Type of a device with below possible values
     * - [DeviceInfo.SMART_PHONE]
     * - [DeviceInfo.TABLET]
     * - [DeviceInfo.TV]
     * - [DeviceInfo.UNKNOWN]
     * - [DeviceInfo.NULL]
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @IntDef(SMART_PHONE, TABLET, TV, UNKNOWN, NULL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DeviceType

    private val adIDLock = Any()

    private var adIdRun = false

    private var cachedInfo: DeviceCachedInfo? = null

    private val deviceIDLock = Any()

    private var enableNetworkInfoReporting = false

    private var googleAdIDValue: String? = null

    open var library: String? = null

    private var limitAdTracking = false

    private val validationResults = ArrayList<ValidationResult>()

    var customLocale: String? = null

    /** Log tag of the device-id resolution path. */
    private val asyncDeviceIdTag: String
        get() = "${config.accountId}:async_deviceID"

    private val deviceIdStorageKey: String
        get() = "${Constants.DEVICE_ID_TAG}:${config.accountId}"

    private val fallbackIdStorageKey: String
        get() = "${Constants.FALLBACK_ID_TAG}:${config.accountId}"

    private val fallbackDeviceID: String?
        get() = StorageHelper.getString(context, fallbackIdStorageKey, null)

    private val deviceCachedInfo: DeviceCachedInfo
        get() = cachedInfo ?: DeviceCachedInfo().also { cachedInfo = it }

    fun forceNewDeviceID() {
        forceUpdateDeviceId(generateGUID())
    }

    fun forceUpdateCustomCleverTapID(cleverTapID: String?): String? {
        if (Utils.validateCTID(cleverTapID)) {
            config.logger.info(config.accountId, "Setting CleverTap ID to custom CleverTap ID : $cleverTapID")
            val id = Constants.CUSTOM_CLEVERTAP_ID_PREFIX + cleverTapID
            forceUpdateDeviceId(id)
            return id
        }
        val fallbackId = generateFallbackDeviceID()
        removeDeviceID()
        val error = recordDeviceError(ValidationError.INVALID_CT_CUSTOM_ID, cleverTapID, fallbackDeviceID)
        config.logger.info(config.accountId, error)
        return fallbackId
    }

    /**
     * Force updates the device ID, with the ID specified.
     *
     * This is used internally by the SDK, there is no need to call this explicitly.
     *
     * @param id The new device ID
     */
    @SuppressLint("CommitPrefEdits")
    fun forceUpdateDeviceId(id: String?) {
        config.logger.verbose(config.accountId, "Force updating the device ID to $id")
        synchronized(deviceIDLock) {
            StorageHelper.putString(context, deviceIdStorageKey, id)
        }
    }

    //Event
    open val appLaunchedFields: JSONObject
        get() = try {
            val deviceIsMultiUser = googleAdID != null && LoginInfoProvider(context, config).deviceIsMultiUser()
            CTJsonConverter.from(this, coreMetaData, enableNetworkInfoReporting, deviceIsMultiUser)
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Failed to construct App Launched event", t)
            JSONObject()
        }

    open val bluetoothVersion: String?
        get() = deviceCachedInfo.bluetoothVersion

    open val build: Int
        get() = deviceCachedInfo.build

    open val carrier: String?
        get() = deviceCachedInfo.carrier

    open val countryCode: String?
        get() = deviceCachedInfo.countryCode

    open val dpi: Int
        get() = deviceCachedInfo.dpi

    open val deviceID: String?
        get() = storedDeviceID() ?: fallbackDeviceID

    open val googleAdID: String?
        get() = synchronized(adIDLock) { googleAdIDValue }

    open val height: Double
        get() = deviceCachedInfo.height

    open val manufacturer: String?
        get() = deviceCachedInfo.manufacturer

    open val appBucket: String?
        get() = deviceCachedInfo.appBucket

    open val model: String?
        get() = deviceCachedInfo.model

    open val networkType: String?
        get() = networkMonitor?.getNetworkTypeString()

    open val osName: String?
        get() = deviceCachedInfo.osName

    open val osVersion: String?
        get() = deviceCachedInfo.osVersion

    open val sdkVersion: Int
        get() = deviceCachedInfo.sdkVersion

    val localInAppCount: Int
        get() = deviceCachedInfo.localInAppCount

    fun incrementLocalInAppCount() {
        deviceCachedInfo.localInAppCount++
    }

    val deviceLocale: String?
        get() = deviceCachedInfo.locale

    val locale: String?
        // If locale is set by the client then use that, otherwise fetch it from the device
        get() = if (customLocale.isNullOrEmpty()) deviceLocale else customLocale

    /** Returns the pending device validation results and clears them. */
    fun drainValidationResults(): List<ValidationResult> {
        val pending = ArrayList(validationResults)
        validationResults.clear()
        return pending
    }

    open val versionName: String?
        get() = deviceCachedInfo.versionName

    open val width: Double
        get() = deviceCachedInfo.width

    open val isBluetoothEnabled: Boolean?
        @SuppressLint("MissingPermission")
        get() = try {
            val hasBluetoothPermission =
                context.packageManager.checkPermission(Manifest.permission.BLUETOOTH, context.packageName)
            if (hasBluetoothPermission == PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()?.isEnabled
            } else {
                null
            }
        } catch (e: Throwable) {
            // do nothing since we don't have permissions
            null
        }

    val isErrorDeviceId: Boolean
        get() = deviceID?.startsWith(Constants.ERROR_PROFILE_PREFIX) == true

    open val isLimitAdTrackingEnabled: Boolean
        get() = synchronized(adIDLock) { limitAdTracking }

    open val isWifiConnected: Boolean?
        get() = when {
            PackageManager.PERMISSION_GRANTED !=
                context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) -> null

            networkMonitor == null -> null
            networkMonitor.getNetworkType() == NetworkMonitor.NetworkType.UNDETECTED -> null
            else -> networkMonitor.isWifiConnected()
        }

    fun enableDeviceNetworkInfoReporting(value: Boolean) {
        enableNetworkInfoReporting = value
        StorageHelper.putBoolean(context, config.accountId, Constants.NETWORK_INFO, enableNetworkInfoReporting)
        config.logger.verbose(
            config.accountId,
            "Device Network Information reporting set to $enableNetworkInfoReporting"
        )
    }

    open val attributionID: String?
        get() = deviceID

    // Caches immutable device properties (model, screen size, carrier, …) off the caller thread.
    // Device-id resolution itself now happens in CleverTapFactory's device-id barrier via
    // initDeviceID(), not here (SDK-6254).
    open fun cacheDeviceInfoAsync() {
        config.logger.verbose(asyncDeviceIdTag, "DeviceInfo() called")
        CTExecutorFactory.executors(config).ioTask<Void?>().execute("getDeviceCachedInfo") {
            deviceCachedInfo
            null
        }
    }

    fun setCurrentUserOptOutStateFromStorage() {
        val key = optOutKey()
        if (key == null) {
            config.logger.verbose(
                config.accountId,
                "Unable to set current user OptOut state from storage: storage key is null"
            )
            return
        }
        val storedOptOut = StorageHelper.getBooleanFromPrefs(context, config.accountId, key)
        coreMetaData.isCurrentUserOptedOut = storedOptOut
        config.logger.verbose(
            config.accountId,
            "Set current user OptOut state from storage to: $storedOptOut for key: $key"
        )
    }

    fun saveOptOutState(userOptOut: Boolean) {
        // persist the new optOut state
        val key = optOutKey()
        if (key == null) {
            config.logger.verbose(config.accountId, "Unable to persist user OptOut state, storage key is null")
            return
        }
        StorageHelper.putBoolean(context, config.accountId, key, userOptOut)
        config.logger.verbose(config.accountId, "Set current user OptOut state to: $userOptOut")
    }

    fun setSystemEventsAllowedStateFromStorage() {
        val key = allowedSystemEventsKey()
        if (key == null) {
            config.logger.verbose(
                config.accountId,
                "Unable to set current user allowed system events and communications flag from storage: storage key is null"
            )
            return
        }
        val storedAllowedSystemEvents = StorageHelper.getBooleanFromPrefs(context, config.accountId, key)
        coreMetaData.enabledSystemEvents = storedAllowedSystemEvents
        config.logger.verbose(
            config.accountId,
            "Set current user allowed system events and communications flag state from storage to: $storedAllowedSystemEvents for key: $key"
        )
    }

    fun saveAllowedSystemEventsState(allowedSystemEvents: Boolean) {
        val key = allowedSystemEventsKey()
        if (key == null) {
            config.logger.verbose(
                config.accountId,
                "Unable to persist user allowed system events and communications flag state, storage key is null"
            )
            return
        }
        StorageHelper.putBoolean(context, config.accountId, key, allowedSystemEvents)
        config.logger.verbose(
            config.accountId,
            "Set current user allowed system events and communications flag state to: $allowedSystemEvents"
        )
    }

    private fun optOutKey(): String? = deviceID?.let { "OptOut:$it" }

    private fun allowedSystemEventsKey(): String? = deviceID?.let { "allowSystemEvents:$it" }

    fun setDeviceNetworkInfoReportingFromStorage() {
        val enabled = StorageHelper.getBooleanFromPrefs(context, config.accountId, Constants.NETWORK_INFO)
        config.logger.verbose(config.accountId, "Setting device network info reporting state from storage to $enabled")
        enableNetworkInfoReporting = enabled
    }

    private fun storedDeviceID(): String? {
        val stored = StorageHelper.getString(context, deviceIdStorageKey, null)
        return if (config.isDefaultInstance) {
            stored ?: StorageHelper.getString(context, Constants.DEVICE_ID_TAG, null)
        } else {
            stored
        }
    }

    @Synchronized
    private fun fetchGoogleAdID() {
        config.logger.verbose(asyncDeviceIdTag, "fetchGoogleAdID() called!")
        if (googleAdID != null || adIdRun) {
            return
        }
        var advertisingID: String? = null
        try {
            adIdRun = true
            val adIdClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            val getAdInfo: Method = adIdClient.getMethod("getAdvertisingIdInfo", Context::class.java)
            // A null adInfo fails here and is caught below, as in Java.
            val adInfo = checkNotNull(getAdInfo.invoke(null, context)) { "advertising id info is null" }
            val isLimitAdTracking = adInfo.javaClass.getMethod("isLimitAdTrackingEnabled")
            val limitedAdTracking = isLimitAdTracking.invoke(adInfo) as Boolean?
            synchronized(adIDLock) {
                limitAdTracking = limitedAdTracking == true
                config.logger.verbose(asyncDeviceIdTag, "limitAdTracking = $limitAdTracking")
                if (limitAdTracking) {
                    config.logger.debug(
                        config.accountId,
                        "Device user has opted out of sharing Advertising ID, falling back to random UUID for CleverTap ID generation"
                    )
                    return
                }
            }
            val getAdId = adInfo.javaClass.getMethod("getId")
            advertisingID = getAdId.invoke(adInfo) as String?
        } catch (t: Throwable) {
            val cause = t.cause
            if (cause != null) {
                config.logger.verbose(config.accountId, "Failed to get Advertising ID: $t$cause")
            } else {
                config.logger.verbose(config.accountId, "Failed to get Advertising ID: $t")
            }
        }
        if (advertisingID != null && advertisingID.trim { it <= ' ' }.length > 2) {
            synchronized(adIDLock) {
                if (advertisingID.contains("00000000")) {
                    //Device has opted out of sharing Google Advertising ID
                    config.logger.debug(
                        config.accountId,
                        "Device user has opted out of sharing Advertising ID, falling back to random UUID for CleverTap ID generation"
                    )
                    return
                }
                googleAdIDValue = advertisingID.replace("-", "")
            }
        }

        config.logger.verbose(asyncDeviceIdTag, "fetchGoogleAdID() done executing!")
    }

    @Synchronized
    private fun generateDeviceID(): String {
        config.logger.verbose(asyncDeviceIdTag, "generateDeviceID() called!")
        val generatedDeviceID = googleAdID?.let { Constants.GUID_PREFIX_GOOGLE_AD_ID + it }
            ?: synchronized(deviceIDLock) { generateGUID() }
        forceUpdateDeviceId(generatedDeviceID)
        config.logger.verbose(asyncDeviceIdTag, "generateDeviceID() done executing!")
        return generatedDeviceID
    }

    private fun generateGUID(): String = GUID_PREFIX + UUID.randomUUID().toString().replace("-", "")

    fun initDeviceID(cleverTapID: String?): String? {
        config.logger.verbose(asyncDeviceIdTag, "Called initDeviceID()")
        //Show logging as per Manifest flag
        if (config.enableCustomCleverTapId) {
            if (cleverTapID == null) {
                config.logger.info(recordDeviceError(ValidationError.USE_CUSTOM_ID_FALLBACK))
            }
        } else if (cleverTapID != null) {
            config.logger.info(recordDeviceError(ValidationError.USE_CUSTOM_ID_MISSING_IN_MANIFEST))
        }

        config.logger.verbose(asyncDeviceIdTag, "Calling _getDeviceID")
        val deviceID = storedDeviceID()
        config.logger.verbose(asyncDeviceIdTag, "Called _getDeviceID")
        if (deviceID != null && deviceID.trim { it <= ' ' }.length > 2) {
            config.logger.verbose(config.accountId, "CleverTap ID already present for profile")
            if (cleverTapID != null) {
                val error = recordDeviceError(ValidationError.UNABLE_TO_SET_CT_CUSTOM_ID, deviceID, cleverTapID)
                config.logger.info(config.accountId, error)
            }
            return deviceID
        }

        if (config.enableCustomCleverTapId) {
            return forceUpdateCustomCleverTapID(cleverTapID)
        }

        if (!config.isUseGoogleAdId) {
            config.logger.verbose(asyncDeviceIdTag, "Calling generateDeviceID()")
            val genId = generateDeviceID()
            config.logger.verbose(asyncDeviceIdTag, "Called generateDeviceID()")
            return genId
        }

        // fetch the googleAdID to generate GUID
        //has to be called on background thread
        fetchGoogleAdID()
        val genId = generateDeviceID()

        config.logger.verbose(asyncDeviceIdTag, "initDeviceID() done executing!")
        return genId
    }

    private fun recordDeviceError(error: ValidationError, vararg varargs: String?): String {
        // Null entries flow through unchanged, as they did from Java.
        @Suppress("UNCHECKED_CAST")
        val validationResult = ValidationResultFactory.create(error, *(varargs as Array<String>))
        validationResults.add(validationResult)
        // ValidationResultFactory always sets the description.
        return validationResult.errorDesc.orEmpty()
    }

    private fun removeDeviceID() {
        StorageHelper.remove(context, deviceIdStorageKey)
    }

    @Synchronized
    private fun generateFallbackDeviceID(): String = fallbackDeviceID ?: synchronized(deviceIDLock) {
        val newFallbackDeviceID = Constants.ERROR_PROFILE_PREFIX + UUID.randomUUID().toString().replace("-", "")
        updateFallbackID(newFallbackDeviceID)
        newFallbackDeviceID
    }

    private fun updateFallbackID(fallbackId: String) {
        config.logger.verbose(config.accountId, "Updating the fallback id - $fallbackId")
        StorageHelper.putString(context, fallbackIdStorageKey, fallbackId)
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    companion object {

        private const val TAG = "DeviceInfo"

        private const val STANDBY_BUCKET_ACTIVE = "active"
        private const val STANDBY_BUCKET_FREQUENT = "frequent"
        private const val STANDBY_BUCKET_RARE = "rare"
        private const val STANDBY_BUCKET_RESTRICTED = "restricted"
        private const val STANDBY_BUCKET_WORKING_SET = "working_set"

        private const val GUID_PREFIX = "__"

        private const val OS_NAME = "Android"

        /**
         * Device is a smart phone
         */
        const val SMART_PHONE: Int = 1

        /**
         * Device is a tablet
         */
        const val TABLET: Int = 2

        /**
         * Device is a television
         */
        const val TV: Int = 3

        /**
         * Device type is not known
         */
        const val UNKNOWN: Int = 0

        /**
         * Initial state of device type before determining
         */
        const val NULL: Int = -1

        /** Device type resolved once per app run by [getDeviceType]. */
        @DeviceType
        private var cachedDeviceType: Int = NULL

        /**
         * Returns the integer identifier for the default app icon.
         *
         * @param context The Android context
         * @return The integer identifier for the image resource
         */
        fun getAppIconAsIntId(context: Context): Int = context.applicationInfo.icon

        /**
         * Determines if a device is tablet, smart phone or TV
         *
         * @param context context
         * @return one of the possible value of [DeviceType]
         */
        @DeviceType
        fun getDeviceType(context: Context?): Int {
            if (cachedDeviceType != NULL) {
                return cachedDeviceType
            }
            try {
                // A null context / service fails here and is handled below, as in Java.
                val uiModeManager =
                    checkNotNull(checkNotNull(context).getSystemService(Context.UI_MODE_SERVICE) as UiModeManager?)
                if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                    cachedDeviceType = TV
                    return cachedDeviceType
                }
            } catch (e: Exception) {
                //uiModeManager or context is null
                Logger.d(TAG, "Failed to decide whether device is a TV!", e)
            }

            cachedDeviceType = try {
                if (checkNotNull(context).resources.getBoolean(R.bool.ctIsTablet)) TABLET else SMART_PHONE
            } catch (e: Exception) {
                // resource not found or context is null
                Logger.d(TAG, "Failed to decide whether device is a smart phone or tablet!", e)
                UNKNOWN
            }
            return cachedDeviceType
        }
    }
}
