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
import android.text.TextUtils
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
import java.util.ArrayList
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Callable
import org.json.JSONObject

// Public (restricted) because already-Kotlin public types (e.g. inapp.TriggerManager) take it in
// their signatures.
@RestrictTo(Scope.LIBRARY_GROUP)
open class DeviceInfo internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    @Suppress("UNUSED_PARAMETER") cleverTapID: String?,
    private val mCoreMetaData: CoreMetaData,
    private val networkMonitor: NetworkMonitor?
) {

    // Fields are @JvmField so they do not clash with the same-named private compute functions.
    private inner class DeviceCachedInfo {

        @JvmField
        val bluetoothVersion: String

        @JvmField
        val build: Int

        @JvmField
        val carrier: String?

        @JvmField
        val countryCode: String?

        @JvmField
        val dpi: Int

        @JvmField
        val height: Double

        @JvmField
        val manufacturer: String

        @JvmField
        val model: String

        @JvmField
        val osName: String

        @JvmField
        val osVersion: String

        @JvmField
        val sdkVersion: Int

        @JvmField
        val versionName: String?

        @JvmField
        val width: Double

        @JvmField
        val appBucket: String?

        @JvmField
        var localInAppCount: Int

        @JvmField
        val locale: String

        init {
            versionName = getVersionName()
            osName = getOsName()
            osVersion = getOsVersion()
            manufacturer = getManufacturer()
            model = getModel()
            carrier = getCarrier()
            build = getBuild()
            bluetoothVersion = getBluetoothVersion()
            countryCode = getCountryCode()
            sdkVersion = getSdkVersion()

            val result = getWindowSizeData()

            width = result.width
            height = result.height
            dpi = result.localDpi

            localInAppCount = getLocalInAppCountFromPreference()
            locale = getDeviceLocale()
            appBucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getAppBucket()
            } else {
                null
            }
        }

        private fun getWindowSizeData(): WindowSize {
            val wm = getWindowManager()

            if (wm == null) {
                getConfigLogger().verbose(
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

                localWidth = windowMetrics.bounds.width() -
                        insets.right - insets.left
                localHeight = windowMetrics.bounds.height() -
                        insets.top - insets.bottom

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
            return WindowSize(
                localDpi,
                toTwoPlaces(doubleWidth),
                toTwoPlaces(doubleHeight)
            )
        }

        private fun getBluetoothVersion(): String {
            var bluetoothVersion = "none"
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothVersion = "ble"
            } else if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                bluetoothVersion = "classic"
            }

            return bluetoothVersion
        }

        private fun getBuild(): Int {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                @Suppress("DEPRECATION")
                return packageInfo.versionCode
            } catch (e: PackageManager.NameNotFoundException) {
                getConfigLogger().debug(config.accountId, "Unable to get app build", e)
            }
            return 0
        }

        private fun getCarrier(): String? {
            try {
                val manager = context
                    .getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager?
                if (manager != null) {
                    return manager.networkOperatorName
                }
            } catch (e: Exception) {
                // Failed to get network operator name from network
            }
            return null
        }

        private fun getCountryCode(): String? {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager?
                if (tm != null) {
                    return tm.simCountryIso
                }
            } catch (ignore: Throwable) {
                return ""
            }
            return ""
        }

        private fun getManufacturer(): String {
            return Build.MANUFACTURER
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
        private fun getAppBucket(): String {
            // Java dereferenced the service without a null check; keep the same NPE.
            val usm = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager?
            return when (usm!!.appStandbyBucket) {
                UsageStatsManager.STANDBY_BUCKET_ACTIVE -> STANDBY_BUCKET_ACTIVE
                UsageStatsManager.STANDBY_BUCKET_FREQUENT -> STANDBY_BUCKET_FREQUENT
                UsageStatsManager.STANDBY_BUCKET_RARE -> STANDBY_BUCKET_RARE
                UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> STANDBY_BUCKET_RESTRICTED
                UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> STANDBY_BUCKET_WORKING_SET
                else -> ""
            }
        }

        private fun getModel(): String {
            var model = Build.MODEL
            model = model.replace(getManufacturer(), "")
            return model
        }

        private fun getOsName(): String {
            return OS_NAME
        }

        private fun getOsVersion(): String {
            return Build.VERSION.RELEASE
        }

        private fun getSdkVersion(): Int {
            return BuildConfig.VERSION_CODE
        }

        private fun getVersionName(): String? {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return packageInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                getConfigLogger().debug(config.accountId, "Unable to get app version", e)
            }
            return null
        }

        private fun getDeviceLocale(): String {
            var language = Locale.getDefault().language
            if ("" == language) {
                language = "xx"
            }
            var country = Locale.getDefault().country
            if ("" == country) {
                country = "XX"
            }
            return language + "_" + country
        }

        private fun toTwoPlaces(n: Double): Double {
            var result = n * 100
            result = Math.round(result).toDouble()
            result = result / 100
            return result
        }
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
                val displayManager = context.getSystemService(DisplayManager::class.java)
                if (displayManager != null) {
                    val primaryDisplay = displayManager.getDisplay(DEFAULT_DISPLAY)
                    if (primaryDisplay != null) {
                        val windowContext = context.createDisplayContext(primaryDisplay)
                            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION, null)
                        return windowContext.getSystemService(WindowManager::class.java)
                    }
                }
            } catch (e: Exception) {
                getConfigLogger().verbose(config.accountId, "Window context creation failed", e)
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

    fun forceNewDeviceID() {
        val deviceID = generateGUID()
        forceUpdateDeviceId(deviceID)
    }

    fun forceUpdateCustomCleverTapID(cleverTapID: String?): String? {
        if (Utils.validateCTID(cleverTapID)) {
            getConfigLogger()
                .info(config.accountId, "Setting CleverTap ID to custom CleverTap ID : $cleverTapID")
            val id = Constants.CUSTOM_CLEVERTAP_ID_PREFIX + cleverTapID
            forceUpdateDeviceId(id)
            return id
        } else {
            val fallbackId = generateFallbackDeviceID()
            removeDeviceID()
            val error = recordDeviceError(ValidationError.INVALID_CT_CUSTOM_ID, cleverTapID, getFallBackDeviceID())
            getConfigLogger().info(config.accountId, error)
            return fallbackId
        }
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
        getConfigLogger().verbose(this.config.accountId, "Force updating the device ID to $id")
        synchronized(deviceIDLock) {
            StorageHelper.putString(context, getDeviceIdStorageKey(), id)
        }
    }

    //Event
    open val appLaunchedFields: JSONObject
        get() {
            try {
                var deviceIsMultiUser = false
                if (googleAdID != null) {
                    deviceIsMultiUser = LoginInfoProvider(context, config).deviceIsMultiUser()
                }
                return CTJsonConverter.from(
                    this, mCoreMetaData, enableNetworkInfoReporting,
                    deviceIsMultiUser
                )
            } catch (t: Throwable) {
                config.logger.verbose(config.accountId, "Failed to construct App Launched event", t)
                return JSONObject()
            }
        }

    open val bluetoothVersion: String?
        get() = getDeviceCachedInfo().bluetoothVersion

    open val build: Int
        get() = getDeviceCachedInfo().build

    open val carrier: String?
        get() = getDeviceCachedInfo().carrier

    open fun getContext(): Context {
        return context
    }

    open val countryCode: String?
        get() = getDeviceCachedInfo().countryCode

    // Kept as a function: an overridable property cannot carry @JvmName("getDPI").
    open fun getDPI(): Int {
        return getDeviceCachedInfo().dpi
    }

    open val deviceID: String?
        get() {
            val currentId = _getDeviceID()
            return currentId ?: getFallBackDeviceID()
        }

    open val googleAdID: String?
        get() {
            synchronized(adIDLock) {
                return googleAdIDValue
            }
        }

    open val height: Double
        get() = getDeviceCachedInfo().height

    open val manufacturer: String?
        get() = getDeviceCachedInfo().manufacturer

    open val appBucket: String?
        get() = getDeviceCachedInfo().appBucket

    open val model: String?
        get() = getDeviceCachedInfo().model

    open val networkType: String?
        get() {
            if (networkMonitor == null) return null
            return networkMonitor.getNetworkTypeString()
        }

    open val osName: String?
        get() = getDeviceCachedInfo().osName

    open val osVersion: String?
        get() = getDeviceCachedInfo().osVersion

    open val sdkVersion: Int
        get() = getDeviceCachedInfo().sdkVersion

    val localInAppCount: Int
        get() = getDeviceCachedInfo().localInAppCount

    fun incrementLocalInAppCount() {
        getDeviceCachedInfo().localInAppCount++
    }

    val deviceLocale: String?
        get() = getDeviceCachedInfo().locale

    val locale: String?
        get() =
            // If locale is set by the client then use that, otherwise fetch it from the device
            if (TextUtils.isEmpty(customLocale)) deviceLocale else customLocale

    // Kept as a function: reading it also clears the pending results.
    fun getValidationResults(): ArrayList<ValidationResult> {
        @Suppress("UNCHECKED_CAST")
        val tempValidationResults = validationResults.clone() as ArrayList<ValidationResult>
        validationResults.clear()
        return tempValidationResults
    }

    open val versionName: String?
        get() = getDeviceCachedInfo().versionName

    open val width: Double
        get() = getDeviceCachedInfo().width

    open val isBluetoothEnabled: Boolean?
        @SuppressLint("MissingPermission")
        get() {
            var isBluetoothEnabled: Boolean? = null
            try {
                val pm = context.packageManager
                val hasBluetoothPermission = pm.checkPermission(Manifest.permission.BLUETOOTH, context.packageName)
                if (hasBluetoothPermission == PackageManager.PERMISSION_GRANTED) {
                    @Suppress("DEPRECATION")
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    if (bluetoothAdapter != null) {
                        isBluetoothEnabled = bluetoothAdapter.isEnabled
                    }
                }
            } catch (e: Throwable) {
                // do nothing since we don't have permissions
            }
            return isBluetoothEnabled
        }

    val isErrorDeviceId: Boolean
        get() {
            val deviceID = deviceID
            return deviceID != null && deviceID.startsWith(Constants.ERROR_PROFILE_PREFIX)
        }

    open val isLimitAdTrackingEnabled: Boolean
        get() {
            synchronized(adIDLock) {
                return limitAdTracking
            }
        }

    open val isWifiConnected: Boolean?
        get() {
            if (PackageManager.PERMISSION_GRANTED != context
                    .checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            ) {
                return null
            }
            if (networkMonitor == null) {
                return null
            }
            if (networkMonitor.getNetworkType() == NetworkMonitor.NetworkType.UNDETECTED) {
                return null
            }
            return networkMonitor.isWifiConnected()
        }

    fun enableDeviceNetworkInfoReporting(value: Boolean) {
        enableNetworkInfoReporting = value
        StorageHelper.putBoolean(context, config.accountId, Constants.NETWORK_INFO, enableNetworkInfoReporting)
        config.logger
            .verbose(
                config.accountId,
                "Device Network Information reporting set to $enableNetworkInfoReporting"
            )
    }

    open val attributionID: String?
        get() = deviceID

    @WorkerThread
    private fun getLocalInAppCountFromPreference(): Int {
        return StorageHelper.getInt(context, LOCAL_INAPP_COUNT, 0)
    }

    // Caches immutable device properties (model, screen size, carrier, …) off the caller thread.
    // Device-id resolution itself now happens in CleverTapFactory's device-id barrier via
    // initDeviceID(), not here (SDK-6254).
    open fun cacheDeviceInfoAsync() {
        getConfigLogger().verbose(config.accountId + ":async_deviceID", "DeviceInfo() called")
        val taskDeviceCachedInfo = CTExecutorFactory.executors(config).ioTask<Void?>()
        taskDeviceCachedInfo.execute("getDeviceCachedInfo", Callable<Void?> {
            getDeviceCachedInfo()
            null
        })
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
        mCoreMetaData.isCurrentUserOptedOut = storedOptOut
        config.logger.verbose(
            config.accountId,
            "Set current user OptOut state from storage to: $storedOptOut for key: $key"
        )
    }

    fun saveOptOutState(userOptOut: Boolean) {
        // persist the new optOut state
        val key = optOutKey()
        if (key == null) {
            getConfigLogger()
                .verbose(config.accountId, "Unable to persist user OptOut state, storage key is null")
            return
        }
        StorageHelper.putBoolean(context, config.accountId, key, userOptOut)
        getConfigLogger().verbose(config.accountId, "Set current user OptOut state to: $userOptOut")
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
        mCoreMetaData.enabledSystemEvents = storedAllowedSystemEvents
        config.logger.verbose(
            config.accountId,
            "Set current user allowed system events and communications flag state from storage to: $storedAllowedSystemEvents for key: $key"
        )
    }

    fun saveAllowedSystemEventsState(allowedSystemEvents: Boolean) {
        val key = allowedSystemEventsKey()
        if (key == null) {
            getConfigLogger()
                .verbose(
                    config.accountId,
                    "Unable to persist user allowed system events and communications flag state, storage key is null"
                )
            return
        }
        StorageHelper.putBoolean(context, config.accountId, key, allowedSystemEvents)
        getConfigLogger().verbose(
            config.accountId,
            "Set current user allowed system events and communications flag state to: $allowedSystemEvents"
        )
    }

    private fun optOutKey(): String? {
        val guid = deviceID ?: return null
        return "OptOut:$guid"
    }

    private fun allowedSystemEventsKey(): String? {
        val guid = deviceID ?: return null
        return "allowSystemEvents:$guid"
    }

    fun setDeviceNetworkInfoReportingFromStorage() {
        val enabled = StorageHelper.getBooleanFromPrefs(context, config.accountId, Constants.NETWORK_INFO)
        config.logger
            .verbose(
                config.accountId,
                "Setting device network info reporting state from storage to $enabled"
            )
        enableNetworkInfoReporting = enabled
    }

    private fun _getDeviceID(): String? {
        val _new = StorageHelper.getString(this.context, getDeviceIdStorageKey(), null)

        return if (this.config.isDefaultInstance) {
            _new ?: StorageHelper.getString(this.context, Constants.DEVICE_ID_TAG, null)
        } else {
            _new
        }
    }

    @Synchronized
    private fun fetchGoogleAdID() {
        getConfigLogger().verbose(config.accountId + ":async_deviceID", "fetchGoogleAdID() called!")
        if (googleAdID == null && !adIdRun) {
            var advertisingID: String? = null
            try {
                adIdRun = true
                val adIdClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                val getAdInfo: Method = adIdClient.getMethod("getAdvertisingIdInfo", Context::class.java)
                // A null adInfo throws an NPE here, caught below as in Java.
                val adInfo = getAdInfo.invoke(null, context)!!
                val isLimitAdTracking = adInfo.javaClass.getMethod("isLimitAdTrackingEnabled")
                val limitedAdTracking = isLimitAdTracking.invoke(adInfo) as Boolean?
                synchronized(adIDLock) {
                    limitAdTracking = limitedAdTracking != null && limitedAdTracking
                    getConfigLogger().verbose(
                        config.accountId + ":async_deviceID",
                        "limitAdTracking = $limitAdTracking"
                    )
                    if (limitAdTracking) {
                        getConfigLogger().debug(
                            config.accountId,
                            "Device user has opted out of sharing Advertising ID, falling back to random UUID for CleverTap ID generation"
                        )
                        return
                    }
                }
                val getAdId = adInfo.javaClass.getMethod("getId")
                advertisingID = getAdId.invoke(adInfo) as String?
            } catch (t: Throwable) {
                if (t.cause != null) {
                    getConfigLogger().verbose(
                        config.accountId,
                        "Failed to get Advertising ID: " + t + t.cause.toString()
                    )
                } else {
                    getConfigLogger().verbose(config.accountId, "Failed to get Advertising ID: $t")
                }
            }
            if (advertisingID != null && advertisingID.trim { it <= ' ' }.length > 2) {
                synchronized(adIDLock) {
                    if (advertisingID.contains("00000000")) {
                        //Device has opted out of sharing Google Advertising ID
                        getConfigLogger().debug(
                            config.accountId,
                            "Device user has opted out of sharing Advertising ID, falling back to random UUID for CleverTap ID generation"
                        )
                        return
                    }
                    googleAdIDValue = advertisingID.replace("-", "")
                }
            }

            getConfigLogger().verbose(config.accountId + ":async_deviceID", "fetchGoogleAdID() done executing!")
        }
    }

    @Synchronized
    private fun generateDeviceID(): String {
        getConfigLogger().verbose(config.accountId + ":async_deviceID", "generateDeviceID() called!")
        val generatedDeviceID: String
        val adId = googleAdID
        if (adId != null) {
            generatedDeviceID = Constants.GUID_PREFIX_GOOGLE_AD_ID + adId
        } else {
            synchronized(deviceIDLock) {
                generatedDeviceID = generateGUID()
            }
        }
        forceUpdateDeviceId(generatedDeviceID)
        getConfigLogger().verbose(config.accountId + ":async_deviceID", "generateDeviceID() done executing!")
        return generatedDeviceID
    }

    private fun generateGUID(): String {
        return GUID_PREFIX + UUID.randomUUID().toString().replace("-", "")
    }

    private fun getConfigLogger(): Logger {
        return this.config.logger
    }

    private fun getDeviceCachedInfo(): DeviceCachedInfo {
        if (cachedInfo == null) {
            cachedInfo = DeviceCachedInfo()
        }
        return cachedInfo!!
    }

    private fun getDeviceIdStorageKey(): String {
        return Constants.DEVICE_ID_TAG + ":" + this.config.accountId
    }

    private fun getFallBackDeviceID(): String? {
        return StorageHelper.getString(this.context, getFallbackIdStorageKey(), null)
    }

    private fun getFallbackIdStorageKey(): String {
        return Constants.FALLBACK_ID_TAG + ":" + this.config.accountId
    }

    fun initDeviceID(cleverTapID: String?): String? {
        getConfigLogger().verbose(config.accountId + ":async_deviceID", "Called initDeviceID()")
        //Show logging as per Manifest flag
        if (config.enableCustomCleverTapId) {
            if (cleverTapID == null) {
                val error = recordDeviceError(ValidationError.USE_CUSTOM_ID_FALLBACK)
                config.logger.info(error)
            }
        } else {
            if (cleverTapID != null) {
                val error = recordDeviceError(ValidationError.USE_CUSTOM_ID_MISSING_IN_MANIFEST)
                config.logger.info(error)
            }
        }

        getConfigLogger().verbose(config.accountId + ":async_deviceID", "Calling _getDeviceID")
        val deviceID = _getDeviceID()
        getConfigLogger().verbose(config.accountId + ":async_deviceID", "Called _getDeviceID")
        if (deviceID != null && deviceID.trim { it <= ' ' }.length > 2) {
            getConfigLogger().verbose(config.accountId, "CleverTap ID already present for profile")
            if (cleverTapID != null) {
                val error = recordDeviceError(ValidationError.UNABLE_TO_SET_CT_CUSTOM_ID, deviceID, cleverTapID)
                getConfigLogger().info(config.accountId, error)
            }
            return deviceID
        }

        if (this.config.enableCustomCleverTapId) {
            return forceUpdateCustomCleverTapID(cleverTapID)
        }

        if (!this.config.isUseGoogleAdId) {
            getConfigLogger().verbose(config.accountId + ":async_deviceID", "Calling generateDeviceID()")
            val genId = generateDeviceID()
            getConfigLogger().verbose(config.accountId + ":async_deviceID", "Called generateDeviceID()")
            return genId
        }

        // fetch the googleAdID to generate GUID
        //has to be called on background thread
        fetchGoogleAdID()
        val genId = generateDeviceID()

        getConfigLogger().verbose(config.accountId + ":async_deviceID", "initDeviceID() done executing!")
        return genId
    }

    private fun recordDeviceError(error: ValidationError, vararg varargs: String?): String {
        // Null entries flow through unchanged, as they did from Java.
        @Suppress("UNCHECKED_CAST")
        val validationResult = ValidationResultFactory.create(error, *(varargs as Array<String>))
        validationResults.add(validationResult)
        return validationResult.errorDesc!!
    }

    private fun removeDeviceID() {
        StorageHelper.remove(this.context, getDeviceIdStorageKey())
    }

    @Synchronized
    private fun generateFallbackDeviceID(): String {
        val existingId = getFallBackDeviceID()

        if (existingId != null) {
            return existingId
        }

        synchronized(deviceIDLock) {
            val fallbackDeviceID = Constants.ERROR_PROFILE_PREFIX + UUID.randomUUID().toString()
                .replace("-", "")
            updateFallbackID(fallbackDeviceID)
            return fallbackDeviceID
        }
    }

    private fun updateFallbackID(fallbackId: String) {
        getConfigLogger().verbose(this.config.accountId, "Updating the fallback id - $fallbackId")
        StorageHelper.putString(context, getFallbackIdStorageKey(), fallbackId)
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

        @JvmField
        @field:DeviceType
        var sDeviceType: Int = NULL

        /**
         * Returns the integer identifier for the default app icon.
         *
         * @param context The Android context
         * @return The integer identifier for the image resource
         */
        @JvmStatic
        fun getAppIconAsIntId(context: Context): Int {
            val ai = context.applicationInfo
            return ai.icon
        }

        /**
         * Determines if a device is tablet, smart phone or TV
         *
         * @param context context
         * @return one of the possible value of [DeviceType]
         */
        @JvmStatic
        @DeviceType
        fun getDeviceType(context: Context?): Int {
            if (sDeviceType == NULL) {
                try {
                    // A null context / service throws here and is handled below, as in Java.
                    val uiModeManager = context!!.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager?
                    if (uiModeManager!!.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                        sDeviceType = TV
                        return sDeviceType
                    }
                } catch (e: Exception) {
                    //uiModeManager or context is null
                    Logger.d(TAG, "Failed to decide whether device is a TV!", e)
                }

                try {
                    sDeviceType = if (context!!.resources.getBoolean(R.bool.ctIsTablet)) TABLET else SMART_PHONE
                } catch (e: Exception) {
                    // resource not found or context is null
                    Logger.d(TAG, "Failed to decide whether device is a smart phone or tablet!", e)
                    sDeviceType = UNKNOWN
                }
            }
            return sDeviceType
        }
    }
}
