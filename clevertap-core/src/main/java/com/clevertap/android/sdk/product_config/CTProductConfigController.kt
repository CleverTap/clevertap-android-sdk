package com.clevertap.android.sdk.product_config

import android.content.Context
import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_BOOLEAN
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_DOUBLE
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_LONG
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_STRING
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.utils.FileUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 */
@Suppress("DEPRECATION")
@Deprecated("This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
public open class CTProductConfigController @Deprecated(
    "This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
) internal constructor(
    private val context: Context,
    internal val config: CleverTapInstanceConfig,
    internal val analyticsManager: BaseAnalyticsManager,
    internal val coreMetaData: CoreMetaData,
    internal val callbackManager: BaseCallbackManager,
    private val settingsValue: ProductConfigSettings,
    internal val fileUtils: FileUtils
) {

    private enum class ProcessingState {
        INIT,
        FETCHED,
        ACTIVATED
    }

    //use lock for synchronization for read write
    internal val activatedConfigs: MutableMap<String, String> = Collections.synchronizedMap(HashMap())

    //use lock for synchronization for read write
    internal val defaultConfigs: MutableMap<String, String> = Collections.synchronizedMap(HashMap())

    internal val initializedFlag = AtomicBoolean(false)

    private val isFetchAndActivatingFlag = AtomicBoolean(false)

    //use lock for synchronization for read write
    private val waitingTobeActivatedConfig: MutableMap<String, String> = Collections.synchronizedMap(HashMap())

    private val logTag: String
        get() = ProductConfigUtil.getLogTag(config)

    init {
        initAsync()
    }

    /**
     * Asynchronously activates the most recently fetched configs, so that the fetched key value pairs take effect.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun activate() {
        if (settingsValue.getGuid().isNullOrEmpty()) {
            return
        }
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.addOnSuccessListener { sendCallback(ProcessingState.ACTIVATED) }
            .execute("activateProductConfigs", object : Callable<Void?> {
                override fun call(): Void? {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        try {
                            //read fetched info
                            val toWriteValues = if (waitingTobeActivatedConfig.isNotEmpty()) {
                                HashMap(waitingTobeActivatedConfig).also { waitingTobeActivatedConfig.clear() }
                            } else {
                                getStoredValues(activatedFullPath)
                            }

                            activatedConfigs.clear()
                            //apply default config first
                            activatedConfigs.putAll(defaultConfigs)
                            activatedConfigs.putAll(toWriteValues)
                            config.logger.verbose(logTag, "Activated successfully with configs: $activatedConfigs")
                        } catch (e: Exception) {
                            config.logger.verbose(logTag, "Activate failed", e)
                        }
                        return null
                    }
                }
            })
    }

    /**
     * Starts fetching configs, adhering to the default minimum fetch interval.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun fetch() {
        fetch(settingsValue.nextFetchIntervalInSeconds)
    }

    /**
     * Starts fetching configs, adhering to the specified minimum fetch interval in seconds.
     *
     * @param minimumFetchIntervalInSeconds - long value of seconds
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun fetch(minimumFetchIntervalInSeconds: Long) {
        if (canRequest(minimumFetchIntervalInSeconds)) {
            fetchProductConfig()
        }
    }

    /**
     * Asynchronously fetches and then activates the fetched configs.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun fetchAndActivate() {
        fetch()
        isFetchAndActivatingFlag.set(true)
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun fetchProductConfig() {
        val event = JSONObject()
        try {
            val notif = JSONObject().put("t", Constants.FETCH_TYPE_PC)
            event.put("evtName", Constants.WZRK_FETCH)
            event.put("evtData", notif)
        } catch (e: JSONException) {
            config.logger.verbose(logTag, "Error creating fetch event JSON", e)
        }
        analyticsManager.sendFetchEvent(event)
        coreMetaData.isProductConfigRequested = true
        config.logger.verbose(config.accountId, "${Constants.LOG_TAG_PRODUCT_CONFIG}Fetching product config")
    }

    /**
     * Returns the parameter value for the given key as a boolean.
     *
     * @param Key - String
     * @return Boolean - value of the product config,if key is not present return
     * [CTProductConfigConstants.DEFAULT_VALUE_FOR_BOOLEAN]
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Suppress("FunctionParameterNaming") // public API: parameter name is visible to Kotlin named-argument callers
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun getBoolean(Key: String?): Boolean? =
        activatedValue(Key)?.toBoolean() ?: DEFAULT_VALUE_FOR_BOOLEAN

    /**
     * Returns the parameter value for the given key as a double.
     *
     * @param Key String
     * @return Double - value of the product config,if key is not present return
     * [CTProductConfigConstants.DEFAULT_VALUE_FOR_DOUBLE]
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Suppress("FunctionParameterNaming") // public API: parameter name is visible to Kotlin named-argument callers
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun getDouble(Key: String?): Double? =
        parseActivatedValue(Key, "Double") { it.toDouble() } ?: DEFAULT_VALUE_FOR_DOUBLE

    /**
     * Returns the last fetched timestamp in millis.
     *
     * @return - long value of timestamp in millis.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun getLastFetchTimeStampInMillis(): Long = settingsValue.lastFetchTimeStampInMillis

    /** Kotlin property syntax, equivalent to [getLastFetchTimeStampInMillis]. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public val lastFetchTimeStampInMillis: Long
        @JvmSynthetic @JvmName("ktGetLastFetchTimeStampInMillis") inline get() = getLastFetchTimeStampInMillis()

    /**
     * Returns the parameter value for the given key as a long.
     *
     * @param Key - String
     * @return Long - value of the product config,if key is not present return
     * [CTProductConfigConstants.DEFAULT_VALUE_FOR_LONG]
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Suppress("FunctionParameterNaming") // public API: parameter name is visible to Kotlin named-argument callers
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun getLong(Key: String?): Long? =
        parseActivatedValue(Key, "Long") { it.toLong() } ?: DEFAULT_VALUE_FOR_LONG

    // -----------------------------------------------------------------------//
    // ********                        Public API                        *****//
    // -----------------------------------------------------------------------//

    /**
     * Returns the parameter value for the given key as a String.
     *
     * @param Key - String
     * @return String - value of the product config,if key is not present return
     * [CTProductConfigConstants.DEFAULT_VALUE_FOR_STRING]
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Suppress("FunctionParameterNaming") // public API: parameter name is visible to Kotlin named-argument callers
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun getString(Key: String?): String = activatedValue(Key) ?: DEFAULT_VALUE_FOR_STRING

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun isInitialized(): Boolean = initializedFlag.get()

    /** Kotlin property syntax, equivalent to [isInitialized]. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public val isInitialized: Boolean
        @JvmSynthetic @JvmName("ktIsInitialized") inline get() = isInitialized()

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun onFetchFailed() {
        isFetchAndActivatingFlag.compareAndSet(true, false)
        config.logger.verbose(logTag, "Fetch Failed")
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun onFetchSuccess(kvResponse: JSONObject?) {
        if (settingsValue.getGuid().isNullOrEmpty()) {
            return
        }
        synchronized(this) {
            if (kvResponse == null) {
                return
            }
            try {
                parseFetchedResponse(kvResponse)
                fileUtils.writeJsonToFile(
                    productConfigDirName,
                    CTProductConfigConstants.FILE_NAME_ACTIVATED,
                    JSONObject(waitingTobeActivatedConfig as Map<*, *>)
                )
                config.logger.verbose(
                    logTag,
                    "Fetch file-[$activatedFullPath] write success: $waitingTobeActivatedConfig"
                )
                val task = CTExecutorFactory.executors(config).mainTask<Void?>()
                task.execute("sendPCFetchSuccessCallback") {
                    config.logger.verbose(logTag, "Product Config: fetch Success")
                    sendCallback(ProcessingState.FETCHED)
                    null
                }
                if (isFetchAndActivatingFlag.getAndSet(false)) {
                    activate()
                }
            } catch (e: Exception) {
                config.logger.verbose(logTag, "Product Config: fetch Failed", e)
                sendCallback(ProcessingState.FETCHED)
                // set fetchAndActivating flag to false if fetch fails.
                isFetchAndActivatingFlag.compareAndSet(true, false)
            }
        }
    }

    /**
     * Deletes all activated, fetched and defaults configs as well as all Product Config settings.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun reset() {
        defaultConfigs.clear()
        activatedConfigs.clear()
        settingsValue.initDefaults()
        eraseStoredConfigFiles()
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun resetSettings() {
        settingsValue.reset(fileUtils)
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setArpValue(arp: JSONObject?) {
        settingsValue.setARPValue(arp)
    }

    /**
     * Sets default configs using an XML resource.
     *
     * @param resourceID - resource Id of the XML.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setDefaults(resourceID: Int) {
        setDefaultsWithXmlParser(resourceID, DefaultXmlParser())
    }

    /**
     * Sets default configs using the given HashMap.
     *
     * @param map - HashMap of the default configs
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setDefaults(map: HashMap<String, Any>?) {
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.addOnSuccessListener { initAsync() }
            .execute("ProductConfig#setDefaultsUsingHashMap", object : Callable<Void?> {
                override fun call(): Void? {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        map?.forEach { (key, value) ->
                            try {
                                if (!key.isNullOrEmpty() && ProductConfigUtil.isSupportedDataType(value)) {
                                    defaultConfigs[key] = value.toString()
                                }
                            } catch (e: Exception) {
                                config.logger.verbose(logTag, "Product Config: setDefaults Failed for Key: $key", e)
                            }
                        }
                        config.logger.verbose(logTag, "Product Config: setDefaults Completed with: $defaultConfigs")
                        return null
                    }
                }
            })
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setGuidAndInit(cleverTapID: String?) {
        if (isInitialized() || cleverTapID.isNullOrEmpty()) {
            return
        }
        settingsValue.setGuid(cleverTapID)
        initAsync()
    }

    /**
     * Sets the minimum interval between successive fetch calls.
     * @param fetchIntervalInSeconds- interval in seconds.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setMinimumFetchIntervalInSeconds(fetchIntervalInSeconds: Long) {
        settingsValue.setMinimumFetchIntervalInSeconds(fetchIntervalInSeconds)
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    internal fun eraseStoredConfigFiles() {
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.execute("eraseStoredConfigs", object : Callable<Void?> {
            override fun call(): Void? {
                // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                synchronized(this) {
                    try {
                        val dirName = productConfigDirName
                        fileUtils.deleteDirectory(dirName)
                        config.logger.verbose(logTag, "Reset Deleted Dir: $dirName")
                    } catch (e: Exception) {
                        config.logger.verbose(logTag, "Reset failed", e)
                    }
                    return null
                }
            }
        })
    }

    internal val activatedFullPath: String
        get() = "$productConfigDirName/${CTProductConfigConstants.FILE_NAME_ACTIVATED}"

    // -----------------------------------------------------------------------//
    // ********                        Internal API                      *****//
    // -----------------------------------------------------------------------//

    internal val productConfigDirName: String
        get() = "${CTProductConfigConstants.DIR_PRODUCT_CONFIG}_${config.accountId}_${settingsValue.getGuid()}"

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun getSettings(): ProductConfigSettings = settingsValue

    /** Kotlin property syntax, equivalent to [getSettings]. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public val settings: ProductConfigSettings
        @JvmSynthetic @JvmName("ktGetSettings") inline get() = getSettings()

    internal fun initAsync() {
        if (settingsValue.getGuid().isNullOrEmpty()) {
            return
        }
        val task = CTExecutorFactory.executors(config).ioTask<Boolean>()
        task.addOnSuccessListener { sendCallback(ProcessingState.INIT) }
            .execute("ProductConfig#initAsync", object : Callable<Boolean> {
                override fun call(): Boolean {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        try {
                            //apply defaults
                            activatedConfigs.putAll(defaultConfigs)
                            waitingTobeActivatedConfig.putAll(getStoredValues(activatedFullPath))
                            config.logger.verbose(logTag, "Loaded configs ready to be applied: $waitingTobeActivatedConfig")
                            settingsValue.loadSettings(fileUtils)
                            initializedFlag.set(true)
                        } catch (e: Exception) {
                            config.logger.verbose(logTag, "InitAsync failed", e)
                            return false
                        }
                        return true
                    }
                }
            })
    }

    internal val isFetchAndActivating: Boolean
        get() = isFetchAndActivatingFlag.get()

    internal fun setDefaultsWithXmlParser(resourceID: Int, xmlParser: DefaultXmlParser) {
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.addOnSuccessListener { initAsync() }
            .execute("PCController#setDefaultsWithXmlParser", object : Callable<Void?> {
                override fun call(): Void? {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        defaultConfigs.putAll(xmlParser.getDefaultsFromXml(context, resourceID))
                        config.logger.verbose(logTag, "Product Config: setDefaults Completed with: $defaultConfigs")
                        return null
                    }
                }
            })
    }

    /**
     * The activated value for [key], or null when not initialised, the key is empty or the value is
     * missing/empty.
     */
    private fun activatedValue(key: String?): String? {
        if (!initializedFlag.get() || key.isNullOrEmpty()) {
            return null
        }
        return activatedConfigs[key]?.takeIf { it.isNotEmpty() }
    }

    /** [activatedValue] parsed with [parse]; a parse failure is logged and yields null. */
    private inline fun <T> parseActivatedValue(key: String?, typeName: String, parse: (String) -> T): T? = try {
        activatedValue(key)?.let(parse)
    } catch (e: Exception) {
        config.logger.verbose(logTag, "Error getting $typeName for Key-$key", e)
        null
    }

    private fun canRequest(minimumFetchIntervalInSeconds: Long): Boolean {
        if (settingsValue.getGuid().isNullOrEmpty()) {
            config.logger.verbose(logTag, "Product Config: Throttled due to empty Guid")
            return false
        }

        val lastRequestTime = settingsValue.lastFetchTimeStampInMillis

        val timeDifference =
            (System.currentTimeMillis() - lastRequestTime) - TimeUnit.SECONDS.toMillis(minimumFetchIntervalInSeconds)
        if (timeDifference <= 0) {
            config.logger.verbose(
                logTag,
                "Throttled since you made frequent request- [Last Request Time-${Date(lastRequestTime)}], " +
                    "Try again in ${-timeDifference / 1000L} seconds"
            )
            return false
        }
        return true
    }

    private fun convertServerJsonToMap(jsonObject: JSONObject): HashMap<String, String> {
        val map = HashMap<String, String>()
        val kvArray = try {
            jsonObject.getJSONArray(Constants.KEY_KV)
        } catch (e: JSONException) {
            config.logger.verbose(logTag, "ConvertServerJsonToMap failed", e)
            return map
        }

        for (i in 0 until kvArray.length()) {
            try {
                val kv = kvArray.get(i) as JSONObject? ?: continue
                val key = kv.getString(PRODUCT_CONFIG_JSON_KEY_FOR_KEY)
                val value = kv.getString(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE)
                if (!key.isNullOrEmpty()) {
                    map[key] = value
                }
            } catch (e: Exception) {
                config.logger.verbose(logTag, "ConvertServerJsonToMap failed", e)
            }
        }
        return map
    }

    private fun getStoredValues(fullFilePath: String): HashMap<String, String> {
        val map = HashMap<String, String>()
        val content = try {
            fileUtils.readFromFile(fullFilePath).also {
                config.logger.verbose(logTag, "GetStoredValues reading file success:[ $fullFilePath]--[Content]$it")
            }
        } catch (e: Exception) {
            config.logger.verbose(logTag, "GetStoredValues reading file failed", e)
            return map
        }
        if (content.isEmpty()) {
            return map
        }
        val jsonObject = try {
            JSONObject(content)
        } catch (e: Exception) {
            config.logger.verbose(logTag, "GetStoredValues failed due to malformed json", e)
            return map
        }
        for (key in jsonObject.keys()) {
            if (key.isNullOrEmpty()) {
                continue
            }
            val value = try {
                jsonObject.get(key).toString()
            } catch (e: Exception) {
                config.logger.verbose(logTag, "GetStoredValues failed for key $key while parsing json", e)
                continue
            }
            if (value.isNotEmpty()) {
                map[key] = value
            }
        }
        return map
    }

    private fun onActivated() {
        callbackManager.productConfigListener?.onActivated()
    }

    //Event
    private fun onFetched() {
        callbackManager.productConfigListener?.onFetched()
    }

    private fun onInit() {
        val productConfigListener = callbackManager.productConfigListener ?: return
        config.logger.verbose(config.accountId, "Product Config initialized")
        productConfigListener.onInit()
    }

    @Synchronized
    private fun parseFetchedResponse(jsonObject: JSONObject) {
        val map = convertServerJsonToMap(jsonObject)
        waitingTobeActivatedConfig.clear()
        waitingTobeActivatedConfig.putAll(map)
        config.logger.verbose(logTag, "Product Config: Fetched response:$jsonObject")
        val timestamp = try {
            jsonObject.get(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP) as Int?
        } catch (e: Exception) {
            config.logger.verbose(logTag, "ParseFetchedResponse failed", e)
            null
        }
        if (timestamp != null) {
            settingsValue.lastFetchTimeStampInMillis = timestamp * 1000L
        }
    }

    private fun sendCallback(state: ProcessingState) {
        when (state) {
            ProcessingState.INIT -> onInit()
            ProcessingState.FETCHED -> onFetched()
            ProcessingState.ACTIVATED -> onActivated()
        }
    }
}
