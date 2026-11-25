package com.clevertap.android.sdk.featureFlags

import android.text.TextUtils
import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.OnSuccessListener
import com.clevertap.android.sdk.utils.FileUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.Callable

/**
 * Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 */
@Deprecated("This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
open class CTFeatureFlagsController @Deprecated(
    "This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
) internal constructor(
    guid: String?,
    internal val config: CleverTapInstanceConfig,
    callbackManager: BaseCallbackManager?,
    analyticsManager: BaseAnalyticsManager?,
    fileUtils: FileUtils
) {

    internal var guidValue: String? = guid

    internal var initialized = false

    internal val mAnalyticsManager: BaseAnalyticsManager? = analyticsManager

    internal val mCallbackManager: BaseCallbackManager? = callbackManager

    internal var mFileUtils: FileUtils = fileUtils

    private val store: MutableMap<String, Boolean> = Collections.synchronizedMap(HashMap())

    init {
        init()
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getGuid(): String? {
        return guidValue
    }

    /** Kotlin property syntax, equivalent to [getGuid]. */
    @Suppress("DEPRECATION")
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    val guid: String?
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
    open fun fetchFeatureFlags() {
        val task = CTExecutorFactory.executors(config).mainTask<Void?>()
        task.execute("fetchFeatureFlags", Callable<Void?> {
            try {
                mAnalyticsManager!!.fetchFeatureFlags()
            } catch (e: Exception) {
                getConfigLogger().verbose(getLogTag(), e.localizedMessage)
            }
            null
        })
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
    open fun get(key: String?, defaultValue: Boolean): Boolean? {
        if (!initialized) {
            getConfigLogger()
                .verbose(getLogTag(), "Controller not initialized, returning default value - $defaultValue")
            return defaultValue
        }
        getConfigLogger().verbose(
            getLogTag(),
            "Getting feature flag with key - $key and default value - $defaultValue"
        )
        val value = store[key]
        return if (value != null) {
            value
        } else {
            getConfigLogger()
                .verbose(getLogTag(), "Feature flag not found, returning default value - $defaultValue")
            defaultValue
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
    open fun isInitialized(): Boolean {
        return initialized
    }

    /** Kotlin property syntax, equivalent to [isInitialized]. */
    @Suppress("DEPRECATION")
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    val isInitialized: Boolean
        @JvmSynthetic @JvmName("ktIsInitialized") inline get() = isInitialized()

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun resetWithGuid(guid: String?) {
        this.guidValue = guid
        init()
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun setGuidAndInit(cleverTapID: String?) {
        if (initialized) {
            return
        }
        this.guidValue = cleverTapID
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
    open fun updateFeatureFlags(jsonObject: JSONObject) {
        val featureFlagList = jsonObject.getJSONArray(Constants.KEY_KV)
        try {
            for (i in 0 until featureFlagList.length()) {
                val ff = featureFlagList.getJSONObject(i)
                store[ff.getString("n")] = ff.getBoolean("v")
            }
            getConfigLogger().verbose(getLogTag(), "Updating feature flags...$store")
        } catch (e: JSONException) {
            getConfigLogger().verbose(getLogTag(), "Error parsing Feature Flag array " + e.localizedMessage)
        }
        archiveData(jsonObject)
        notifyFeatureFlagUpdate()
    }

    internal fun getCachedDirName(): String {
        return CTFeatureFlagConstants.DIR_FEATURE_FLAG + "_" + config.accountId + "_" + guidValue
    }

    internal fun getCachedFileName(): String {
        return CTFeatureFlagConstants.CACHED_FILE_NAME
    }

    internal fun getCachedFullPath(): String {
        return getCachedDirName() + "/" + getCachedFileName()
    }

    internal fun init() {
        if (TextUtils.isEmpty(guidValue)) {
            return
        }
        val task = CTExecutorFactory.executors(config).ioTask<Boolean>()
        task.addOnSuccessListener(OnSuccessListener { init -> initialized = init })
        task.execute("initFeatureFlags", object : Callable<Boolean> {
            override fun call(): Boolean {
                // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                synchronized(this) {
                    getConfigLogger().verbose(getLogTag(), "Feature flags init is called")
                    val fileName = getCachedFullPath()
                    try {
                        store.clear()
                        val content = mFileUtils.readFromFile(fileName)
                        if (!TextUtils.isEmpty(content)) {
                            val jsonObject = JSONObject(content)
                            val kvArray = jsonObject.getJSONArray(Constants.KEY_KV)

                            if (kvArray != null && kvArray.length() > 0) {
                                for (i in 0 until kvArray.length()) {
                                    val `object` = kvArray.get(i) as JSONObject?
                                    if (`object` != null) {
                                        val Key = `object`.getString(PRODUCT_CONFIG_JSON_KEY_FOR_KEY)
                                        val Value = `object`.getString(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE)
                                        if (!TextUtils.isEmpty(Key)) {
                                            store[Key] = java.lang.Boolean.parseBoolean(Value)
                                        }
                                    }
                                }
                            }
                            getConfigLogger().verbose(
                                getLogTag(), "Feature flags initialized from file " + fileName +
                                        " with configs  " + store
                            )
                        } else {
                            getConfigLogger().verbose(getLogTag(), "Feature flags file is empty-$fileName")
                        }
                    } catch (e: Exception) {
                        getConfigLogger().verbose(getLogTag(), "UnArchiveData failed file- $fileName", e)
                        return false
                    }
                    return true
                }
            }
        })
    }

    @Synchronized
    private fun archiveData(featureFlagRespObj: JSONObject?) {
        if (featureFlagRespObj != null) {
            try {
                mFileUtils.writeJsonToFile(
                    getCachedDirName(), getCachedFileName(),
                    featureFlagRespObj
                )
                getConfigLogger()
                    .verbose(getLogTag(), "Feature flags saved into file-[" + getCachedFullPath() + "]" + store)
            } catch (e: Exception) {
                getConfigLogger().verbose(getLogTag(), "ArchiveData failed", e)
            }
        }
    }

    private fun getConfigLogger(): Logger {
        return config.logger
    }

    private fun getLogTag(): String {
        return config.accountId + "[Feature Flag]"
    }

    private fun notifyFeatureFlagUpdate() {
        if (mCallbackManager!!.featureFlagListener != null) {
            val task = CTExecutorFactory.executors(config).mainTask<Void?>()
            task.execute("notifyFeatureFlagUpdate", Callable<Void?> {
                try {
                    val featureFlagListener = mCallbackManager.featureFlagListener
                    if (featureFlagListener != null) {
                        featureFlagListener.featureFlagsUpdated()
                    }
                } catch (e: Exception) {
                    getConfigLogger().verbose(getLogTag(), e.localizedMessage)
                }
                null
            })
        }
    }
}
