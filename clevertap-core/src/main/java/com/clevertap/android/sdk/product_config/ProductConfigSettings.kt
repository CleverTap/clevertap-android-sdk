package com.clevertap.android.sdk.product_config

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_MIN_FETCH_INTERVAL_SECONDS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_NO_OF_CALLS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_WINDOW_LENGTH_MINS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.utils.FileUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 */
@Suppress("DEPRECATION")
@Deprecated("This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
open class ProductConfigSettings @Deprecated(
    "This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
) internal constructor(
    private var guidValue: String?,
    private val config: CleverTapInstanceConfig,
    private val fileUtils: FileUtils
) {

    private val settingsMap: MutableMap<String, String> = Collections.synchronizedMap(HashMap())

    private val logTag: String
        get() = ProductConfigUtil.getLogTag(config)

    init {
        initDefaults()
    }

    internal fun eraseStoredSettingsFile(fileUtils: FileUtils?) {
        requireNotNull(fileUtils) { "FileUtils can't be null" }
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.execute("ProductConfigSettings#eraseStoredSettingsFile", object : Callable<Void?> {
            override fun call(): Void? {
                // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                synchronized(this) {
                    try {
                        val fileName = fullPath
                        fileUtils.deleteFile(fileName)
                        config.logger.verbose(logTag, "Deleted settings file$fileName")
                    } catch (e: Exception) {
                        config.logger.verbose(logTag, "Error while resetting settings", e)
                    }
                    return null
                }
            }
        })
    }

    internal val dirName: String
        get() = "${CTProductConfigConstants.DIR_PRODUCT_CONFIG}_${config.accountId}_$guidValue"

    internal val fullPath: String
        get() = "$dirName/${CTProductConfigConstants.FILE_NAME_CONFIG_SETTINGS}"

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getGuid(): String? = guidValue

    internal fun setGuid(guid: String?) {
        guidValue = guid
    }

    /** Kotlin property syntax, equivalent to [getGuid]; the setter stays SDK-internal as in Java. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    var guid: String?
        @JvmSynthetic @JvmName("ktGetGuid") inline get() = getGuid()
        @JvmSynthetic @JvmName("ktSetGuid") internal inline set(value) = setGuid(value)

    internal fun getJsonObject(content: String?): JSONObject? {
        if (content.isNullOrEmpty()) {
            return null
        }
        return try {
            JSONObject(content)
        } catch (e: JSONException) {
            config.logger.verbose(logTag, "LoadSettings failed", e)
            null
        }
    }

    internal var lastFetchTimeStampInMillis: Long
        @Synchronized get() = readSetting(KEY_LAST_FETCHED_TIMESTAMP, 0L, "GetLastFetchTimeStampInMillis") {
            it.toDouble().toLong()
        }
        @Synchronized set(timeStampInMillis) {
            if (timeStampInMillis >= 0 && lastFetchTimeStampInMillis != timeStampInMillis) {
                settingsMap[KEY_LAST_FETCHED_TIMESTAMP] = timeStampInMillis.toString()
                updateConfigToFile()
            }
        }

    internal val nextFetchIntervalInSeconds: Long
        get() {
            val minFetchIntervalInSecondsSDK = minFetchIntervalInSeconds
            val minFetchIntervalInSecondsServer =
                TimeUnit.MINUTES.toSeconds((windowIntervalInMinutes / noOfCallsInAllowedWindow).toLong())
            return maxOf(minFetchIntervalInSecondsServer, minFetchIntervalInSecondsSDK)
        }

    internal fun initDefaults() {
        settingsMap[PRODUCT_CONFIG_NO_OF_CALLS] = DEFAULT_NO_OF_CALLS.toString()
        settingsMap[PRODUCT_CONFIG_WINDOW_LENGTH_MINS] = DEFAULT_WINDOW_LENGTH_MINS.toString()
        settingsMap[KEY_LAST_FETCHED_TIMESTAMP] = 0.toString()
        settingsMap[PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS] = DEFAULT_MIN_FETCH_INTERVAL_SECONDS.toString()
        config.logger.verbose(logTag, "Settings loaded with default values: $settingsMap")
    }

    /**
     * loads settings from file.
     * It's a sync call, please make sure to call this from a background thread
     */
    @Synchronized
    internal fun loadSettings(fileUtils: FileUtils?) {
        requireNotNull(fileUtils) { "fileutils can't be null" }
        try {
            populateMapWithJson(getJsonObject(fileUtils.readFromFile(fullPath)))
        } catch (e: Exception) {
            config.logger.verbose(logTag, "LoadSettings failed while reading file", e)
        }
    }

    @Synchronized
    internal fun populateMapWithJson(jsonObject: JSONObject?) {
        if (jsonObject == null) {
            return
        }
        for (key in jsonObject.keys()) {
            if (key.isNullOrEmpty()) {
                continue
            }
            val value = try {
                jsonObject.get(key).toString()
            } catch (e: Exception) {
                config.logger.verbose(logTag, "Failed loading setting for key $key", e)
                continue
            }
            if (value.isNotEmpty()) {
                settingsMap[key] = value
            }
        }
        config.logger.verbose(logTag, "LoadSettings completed with settings: $settingsMap")
    }

    internal fun reset(fileUtils: FileUtils?) {
        initDefaults()
        eraseStoredSettingsFile(fileUtils)
    }

    internal fun setARPValue(arp: JSONObject?) {
        if (arp == null) {
            return
        }
        for (key in arp.keys()) {
            try {
                if (key.isNullOrEmpty()) {
                    continue
                }
                val value = arp.get(key)
                if (value is Number &&
                    (PRODUCT_CONFIG_NO_OF_CALLS.equals(key, ignoreCase = true) ||
                        PRODUCT_CONFIG_WINDOW_LENGTH_MINS.equals(key, ignoreCase = true))
                ) {
                    setProductConfigValuesFromARP(key, value.toDouble().toInt())
                }
            } catch (e: Exception) {
                config.logger.verbose(logTag, "Product Config setARPValue failed", e)
            }
        }
    }

    @Synchronized
    internal fun setMinimumFetchIntervalInSeconds(intervalInSeconds: Long) {
        if (intervalInSeconds > 0 && minFetchIntervalInSeconds != intervalInSeconds) {
            settingsMap[PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS] = intervalInSeconds.toString()
        }
    }

    private val minFetchIntervalInSeconds: Long
        get() = readSetting(
            PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS,
            DEFAULT_MIN_FETCH_INTERVAL_SECONDS,
            "GetMinFetchIntervalInSeconds"
        ) { it.toDouble().toLong() }

    private val noOfCallsInAllowedWindow: Int
        @Synchronized get() = readSetting(PRODUCT_CONFIG_NO_OF_CALLS, DEFAULT_NO_OF_CALLS, "GetNoOfCallsInAllowedWindow") {
            it.toDouble().toInt()
        }

    @Synchronized
    private fun setNoOfCallsInAllowedWindow(callsInAllowedWindow: Int) {
        if (callsInAllowedWindow > 0 && noOfCallsInAllowedWindow != callsInAllowedWindow) {
            settingsMap[PRODUCT_CONFIG_NO_OF_CALLS] = callsInAllowedWindow.toString()
            updateConfigToFile()
        }
    }

    private val windowIntervalInMinutes: Int
        @Synchronized get() = readSetting(
            PRODUCT_CONFIG_WINDOW_LENGTH_MINS,
            DEFAULT_WINDOW_LENGTH_MINS,
            "GetWindowIntervalInMinutes"
        ) { it.toDouble().toInt() }

    @Synchronized
    private fun setWindowIntervalInMinutes(intervalInMinutes: Int) {
        if (intervalInMinutes > 0 && windowIntervalInMinutes != intervalInMinutes) {
            settingsMap[PRODUCT_CONFIG_WINDOW_LENGTH_MINS] = intervalInMinutes.toString()
            updateConfigToFile()
        }
    }

    /**
     * Parses the setting stored under [key], or returns [default] when it is missing/empty or fails to
     * parse (logged as "<[failureLogPrefix]> failed").
     */
    private inline fun <T> readSetting(key: String, default: T, failureLogPrefix: String, parse: (String) -> T): T {
        val value = settingsMap[key]
        return try {
            if (value.isNullOrEmpty()) default else parse(value)
        } catch (e: Exception) {
            config.logger.verbose(logTag, "$failureLogPrefix failed", e)
            default
        }
    }

    private fun setProductConfigValuesFromARP(key: String, value: Int) {
        when (key) {
            PRODUCT_CONFIG_NO_OF_CALLS -> setNoOfCallsInAllowedWindow(value)
            PRODUCT_CONFIG_WINDOW_LENGTH_MINS -> setWindowIntervalInMinutes(value)
        }
    }

    @Synchronized
    private fun updateConfigToFile() {
        val task = CTExecutorFactory.executors(config).ioTask<Boolean>()
        task.addOnSuccessListener { isSuccess ->
            if (isSuccess) {
                config.logger.verbose(logTag, "Product Config settings: writing Success $settingsMap")
            } else {
                config.logger.verbose(logTag, "Product Config settings: writing Failed")
            }
        }.execute("ProductConfigSettings#updateConfigToFile") {
            try {
                //Ensure that we are not saving min interval in seconds
                val toWriteMap = HashMap(settingsMap)
                toWriteMap.remove(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS)

                fileUtils.writeJsonToFile(
                    dirName,
                    CTProductConfigConstants.FILE_NAME_CONFIG_SETTINGS,
                    JSONObject(toWriteMap as Map<*, *>)
                )
                true
            } catch (e: Exception) {
                config.logger.verbose(logTag, "UpdateConfigToFile failed", e)
                false
            }
        }
    }
}
