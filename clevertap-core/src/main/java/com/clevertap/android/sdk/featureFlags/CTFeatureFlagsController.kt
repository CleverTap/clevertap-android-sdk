package com.clevertap.android.sdk.featureFlags

import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.utils.FileUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.Callable

/**
 * Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 */
@Deprecated("This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
public open class CTFeatureFlagsController @Deprecated(
    "This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
) internal constructor(
    private var guidValue: String?,
    internal val config: CleverTapInstanceConfig,
    internal val callbackManager: BaseCallbackManager,
    internal val analyticsManager: BaseAnalyticsManager,
    internal val fileUtils: FileUtils
) {

    internal var initialized = false

    private val store: MutableMap<String, Boolean> = Collections.synchronizedMap(HashMap())

    init {
        init()
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun getGuid(): String? = guidValue

    /** Kotlin property syntax, equivalent to [getGuid]. */
    @Suppress("DEPRECATION")
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public val guid: String?
        @JvmSynthetic @JvmName("ktGetGuid") inline get() = getGuid()

    // -----------------------------------------------------------------------//
    // ********                        Public API                        *****//
    // -----------------------------------------------------------------------//

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun fetchFeatureFlags() {
        val task = CTExecutorFactory.executors(config).mainTask<Void?>()
        task.execute("fetchFeatureFlags") {
            try {
                analyticsManager.fetchFeatureFlags()
            } catch (e: Exception) {
                logger.verbose(logTag, e.localizedMessage)
            }
            null
        }
    }

    /**
     * Getter to return the feature flag configured at the dashboard
     *
     * @param key          - Key of the Feature flag
     * @param defaultValue - default value of the Key, in case we don't find any Feature Flag with the Key.
     * @return boolean- Value of the Feature flag.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun get(key: String?, defaultValue: Boolean): Boolean? {
        if (!initialized) {
            logger.verbose(logTag, "Controller not initialized, returning default value - $defaultValue")
            return defaultValue
        }
        logger.verbose(logTag, "Getting feature flag with key - $key and default value - $defaultValue")
        return store[key] ?: defaultValue.also {
            logger.verbose(logTag, "Feature flag not found, returning default value - $defaultValue")
        }
    }

    // -----------------------------------------------------------------------//
    // ********                        Internal API                      *****//
    // -----------------------------------------------------------------------//

    /**
     * Method to check Feature Flag has been initialized
     *
     * @return boolean- true if initialized, else false.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun isInitialized(): Boolean = initialized

    /** Kotlin property syntax, equivalent to [isInitialized]. */
    @Suppress("DEPRECATION")
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public val isInitialized: Boolean
        @JvmSynthetic @JvmName("ktIsInitialized") inline get() = isInitialized()

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun resetWithGuid(guid: String?) {
        guidValue = guid
        init()
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setGuidAndInit(cleverTapID: String?) {
        if (initialized) {
            return
        }
        guidValue = cleverTapID
        init()
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    @Synchronized
    @Throws(JSONException::class)
    public open fun updateFeatureFlags(jsonObject: JSONObject) {
        val featureFlagList = jsonObject.getJSONArray(Constants.KEY_KV)
        try {
            for (i in 0 until featureFlagList.length()) {
                val ff = featureFlagList.getJSONObject(i)
                store[ff.getString("n")] = ff.getBoolean("v")
            }
            logger.verbose(logTag, "Updating feature flags...$store")
        } catch (e: JSONException) {
            logger.verbose(logTag, "Error parsing Feature Flag array ${e.localizedMessage}")
        }
        archiveData(jsonObject)
        notifyFeatureFlagUpdate()
    }

    internal val cachedDirName: String
        get() = "${CTFeatureFlagConstants.DIR_FEATURE_FLAG}_${config.accountId}_$guidValue"

    internal val cachedFileName: String
        get() = CTFeatureFlagConstants.CACHED_FILE_NAME

    internal val cachedFullPath: String
        get() = "$cachedDirName/$cachedFileName"

    internal fun init() {
        if (guidValue.isNullOrEmpty()) {
            return
        }
        val task = CTExecutorFactory.executors(config).ioTask<Boolean>()
        task.addOnSuccessListener { init -> initialized = init }
        task.execute("initFeatureFlags", object : Callable<Boolean> {
            override fun call(): Boolean {
                // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                synchronized(this) {
                    logger.verbose(logTag, "Feature flags init is called")
                    val fileName = cachedFullPath
                    try {
                        store.clear()
                        val content = fileUtils.readFromFile(fileName)
                        if (content.isEmpty()) {
                            logger.verbose(logTag, "Feature flags file is empty-$fileName")
                            return true
                        }
                        val kvArray = JSONObject(content).getJSONArray(Constants.KEY_KV)
                        for (i in 0 until kvArray.length()) {
                            val kv = kvArray.get(i) as JSONObject? ?: continue
                            val key = kv.getString(PRODUCT_CONFIG_JSON_KEY_FOR_KEY)
                            val value = kv.getString(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE)
                            if (!key.isNullOrEmpty()) {
                                store[key] = value.toBoolean()
                            }
                        }
                        logger.verbose(logTag, "Feature flags initialized from file $fileName with configs  $store")
                    } catch (e: Exception) {
                        logger.verbose(logTag, "UnArchiveData failed file- $fileName", e)
                        return false
                    }
                    return true
                }
            }
        })
    }

    @Synchronized
    private fun archiveData(featureFlagRespObj: JSONObject?) {
        if (featureFlagRespObj == null) {
            return
        }
        try {
            fileUtils.writeJsonToFile(cachedDirName, cachedFileName, featureFlagRespObj)
            logger.verbose(logTag, "Feature flags saved into file-[$cachedFullPath]$store")
        } catch (e: Exception) {
            logger.verbose(logTag, "ArchiveData failed", e)
        }
    }

    private val logger: Logger
        get() = config.logger

    private val logTag: String
        get() = "${config.accountId}[Feature Flag]"

    private fun notifyFeatureFlagUpdate() {
        if (callbackManager.featureFlagListener == null) {
            return
        }
        val task = CTExecutorFactory.executors(config).mainTask<Void?>()
        task.execute("notifyFeatureFlagUpdate") {
            try {
                callbackManager.featureFlagListener?.featureFlagsUpdated()
            } catch (e: Exception) {
                logger.verbose(logTag, e.localizedMessage)
            }
            null
        }
    }
}
