package com.clevertap.android.sdk.product_config

import android.text.TextUtils
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_MIN_FETCH_INTERVAL_SECONDS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_NO_OF_CALLS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_WINDOW_LENGTH_MINS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.OnSuccessListener
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

    init {
        initDefaults()
    }

    internal fun eraseStoredSettingsFile(fileUtils: FileUtils?) {
        if (fileUtils == null) {
            throw IllegalArgumentException("FileUtils can't be null")
        }
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.execute("ProductConfigSettings#eraseStoredSettingsFile", object : Callable<Void?> {
            override fun call(): Void? {
                // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                synchronized(this) {
                    try {
                        val fileName = fullPath
                        fileUtils.deleteFile(fileName)
                        config.logger
                            .verbose(ProductConfigUtil.getLogTag(config), "Deleted settings file$fileName")
                    } catch (e: Exception) {
                        config.logger.verbose(
                            ProductConfigUtil.getLogTag(config),
                            "Error while resetting settings", e
                        )
                    }
                    return null
                }
            }
        })
    }

    internal val dirName: String
        get() = CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.accountId + "_" + guidValue

    internal val fullPath: String
        get() = dirName + "/" + CTProductConfigConstants.FILE_NAME_CONFIG_SETTINGS

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getGuid(): String? {
        return guidValue
    }

    internal fun setGuid(guid: String?) {
        this.guidValue = guid
    }

    /** Kotlin property syntax, equivalent to [getGuid]; the setter stays SDK-internal as in Java. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    var guid: String?
        @JvmSynthetic @JvmName("ktGetGuid") inline get() = getGuid()
        @JvmSynthetic @JvmName("ktSetGuid") internal inline set(value) = setGuid(value)

    internal fun getJsonObject(content: String?): JSONObject? {
        if (!TextUtils.isEmpty(content)) {
            try {
                return JSONObject(content!!)
            } catch (e: JSONException) {
                config.logger.verbose(
                    ProductConfigUtil.getLogTag(config),
                    "LoadSettings failed", e
                )
            }
        }
        return null
    }

    internal var lastFetchTimeStampInMillis: Long
        @Synchronized get() {
            var lastFetchedTimeStamp = 0L
            val value = settingsMap[KEY_LAST_FETCHED_TIMESTAMP]
            try {
                if (!TextUtils.isEmpty(value)) {
                    lastFetchedTimeStamp = value!!.toDouble().toLong()
                }
            } catch (e: Exception) {
                config.logger.verbose(
                    ProductConfigUtil.getLogTag(config),
                    "GetLastFetchTimeStampInMillis failed", e
                )
            }
            return lastFetchedTimeStamp
        }
        @Synchronized set(timeStampInMillis) {
            val lastFetchTimeStampInMillis = lastFetchTimeStampInMillis
            if (timeStampInMillis >= 0 && lastFetchTimeStampInMillis != timeStampInMillis) {
                settingsMap[KEY_LAST_FETCHED_TIMESTAMP] = timeStampInMillis.toString()
                updateConfigToFile()
            }
        }

    internal val nextFetchIntervalInSeconds: Long
        get() {
            val minFetchIntervalInSecondsSDK = getMinFetchIntervalInSeconds()
            val minFetchIntervalInSecondsServer = TimeUnit.MINUTES
                .toSeconds((getWindowIntervalInMinutes() / getNoOfCallsInAllowedWindow()).toLong())
            return Math.max(minFetchIntervalInSecondsServer, minFetchIntervalInSecondsSDK)
        }

    internal fun initDefaults() {
        settingsMap[PRODUCT_CONFIG_NO_OF_CALLS] = DEFAULT_NO_OF_CALLS.toString()
        settingsMap[PRODUCT_CONFIG_WINDOW_LENGTH_MINS] = DEFAULT_WINDOW_LENGTH_MINS.toString()
        settingsMap[KEY_LAST_FETCHED_TIMESTAMP] = 0.toString()
        settingsMap[PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS] = DEFAULT_MIN_FETCH_INTERVAL_SECONDS.toString()
        config.logger
            .verbose(
                ProductConfigUtil.getLogTag(config),
                "Settings loaded with default values: $settingsMap"
            )
    }

    /**
     * loads settings from file.
     * It's a sync call, please make sure to call this from a background thread
     */
    @Synchronized
    internal fun loadSettings(fileUtils: FileUtils?) {
        if (fileUtils == null) {
            throw IllegalArgumentException("fileutils can't be null")
        }
        try {
            val content = fileUtils.readFromFile(fullPath)
            val jsonObject = getJsonObject(content)
            populateMapWithJson(jsonObject)
        } catch (e: Exception) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "LoadSettings failed while reading file", e
            )
        }
    }

    @Synchronized
    internal fun populateMapWithJson(jsonObject: JSONObject?) {
        if (jsonObject == null) {
            return
        }
        val iterator = jsonObject.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (!TextUtils.isEmpty(key)) {
                val value: String
                try {
                    val obj = jsonObject.get(key)
                    value = obj.toString()
                } catch (e: Exception) {
                    config.logger.verbose(
                        ProductConfigUtil.getLogTag(config),
                        "Failed loading setting for key $key", e
                    )
                    continue
                }
                if (!TextUtils.isEmpty(value)) {
                    settingsMap[key] = value
                }
            }
        }
        config.logger.verbose(
            ProductConfigUtil.getLogTag(config),
            "LoadSettings completed with settings: $settingsMap"
        )
    }

    internal fun reset(fileUtils: FileUtils?) {
        initDefaults()
        eraseStoredSettingsFile(fileUtils)
    }

    internal fun setARPValue(arp: JSONObject?) {
        if (arp != null) {
            val keys = arp.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                try {
                    if (!TextUtils.isEmpty(key)) {
                        val `object` = arp.get(key)
                        if (`object` is Number) {
                            val update = `object`.toDouble().toInt()
                            if (CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS.equals(key, ignoreCase = true) ||
                                CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS
                                    .equals(key, ignoreCase = true)
                            ) {
                                setProductConfigValuesFromARP(key, update)
                            }
                        }
                    }
                } catch (e: Exception) {
                    config.logger.verbose(
                        ProductConfigUtil.getLogTag(config),
                        "Product Config setARPValue failed", e
                    )
                }
            }
        }
    }

    @Synchronized
    internal fun setMinimumFetchIntervalInSeconds(intervalInSeconds: Long) {
        val minFetchIntervalInSeconds = getMinFetchIntervalInSeconds()
        if (intervalInSeconds > 0 && minFetchIntervalInSeconds != intervalInSeconds) {
            settingsMap[PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS] = intervalInSeconds.toString()
        }
    }

    private fun getMinFetchIntervalInSeconds(): Long {
        var minInterVal = DEFAULT_MIN_FETCH_INTERVAL_SECONDS
        val value = settingsMap[PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS]
        try {
            if (!TextUtils.isEmpty(value)) {
                minInterVal = value!!.toDouble().toLong()
            }
        } catch (e: Exception) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "GetMinFetchIntervalInSeconds failed", e
            )
        }
        return minInterVal
    }

    @Synchronized
    private fun getNoOfCallsInAllowedWindow(): Int {
        var noCallsAllowedInWindow = DEFAULT_NO_OF_CALLS
        val value = settingsMap[PRODUCT_CONFIG_NO_OF_CALLS]
        try {
            if (!TextUtils.isEmpty(value)) {
                noCallsAllowedInWindow = value!!.toDouble().toInt()
            }
        } catch (e: Exception) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "GetNoOfCallsInAllowedWindow failed", e
            )
        }
        return noCallsAllowedInWindow
    }

    @Synchronized
    private fun setNoOfCallsInAllowedWindow(callsInAllowedWindow: Int) {
        val noOfCallsInAllowedWindow = getNoOfCallsInAllowedWindow().toLong()
        if (callsInAllowedWindow > 0 && noOfCallsInAllowedWindow != callsInAllowedWindow.toLong()) {
            settingsMap[PRODUCT_CONFIG_NO_OF_CALLS] = callsInAllowedWindow.toString()
            updateConfigToFile()
        }
    }

    @Synchronized
    private fun getWindowIntervalInMinutes(): Int {
        var windowIntervalInMinutes = DEFAULT_WINDOW_LENGTH_MINS
        val value = settingsMap[PRODUCT_CONFIG_WINDOW_LENGTH_MINS]
        try {
            if (!TextUtils.isEmpty(value)) {
                windowIntervalInMinutes = value!!.toDouble().toInt()
            }
        } catch (e: Exception) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "GetWindowIntervalInMinutes failed", e
            )
        }
        return windowIntervalInMinutes
    }

    @Synchronized
    private fun setWindowIntervalInMinutes(intervalInMinutes: Int) {
        val windowIntervalInMinutes = getWindowIntervalInMinutes()
        if (intervalInMinutes > 0 && windowIntervalInMinutes != intervalInMinutes) {
            settingsMap[PRODUCT_CONFIG_WINDOW_LENGTH_MINS] = intervalInMinutes.toString()
            updateConfigToFile()
        }
    }

    private fun setProductConfigValuesFromARP(key: String, value: Int) {
        when (key) {
            CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS -> setNoOfCallsInAllowedWindow(value)
            CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS -> setWindowIntervalInMinutes(value)
        }
    }

    @Synchronized
    private fun updateConfigToFile() {
        val task = CTExecutorFactory.executors(config).ioTask<Boolean>()
        task.addOnSuccessListener(OnSuccessListener { isSuccess ->
            if (isSuccess) {
                config.logger.verbose(
                    ProductConfigUtil.getLogTag(config),
                    "Product Config settings: writing Success $settingsMap"
                )
            } else {
                config.logger
                    .verbose(
                        ProductConfigUtil.getLogTag(config),
                        "Product Config settings: writing Failed"
                    )
            }
        }).execute("ProductConfigSettings#updateConfigToFile", Callable {
            try {
                //Ensure that we are not saving min interval in seconds
                val toWriteMap = HashMap(settingsMap)
                toWriteMap.remove(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS)

                fileUtils.writeJsonToFile(
                    dirName,
                    CTProductConfigConstants.FILE_NAME_CONFIG_SETTINGS, JSONObject(toWriteMap as Map<*, *>)
                )
            } catch (e: Exception) {
                config.logger.verbose(
                    ProductConfigUtil.getLogTag(config),
                    "UpdateConfigToFile failed", e
                )
                return@Callable false
            }
            true
        })
    }
}
