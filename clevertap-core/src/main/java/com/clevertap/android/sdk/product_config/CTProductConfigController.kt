package com.clevertap.android.sdk.product_config

import android.content.Context
import android.text.TextUtils
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
import com.clevertap.android.sdk.task.OnSuccessListener
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
open class CTProductConfigController @Deprecated(
    "This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
) internal constructor(
    private val context: Context?,
    internal val config: CleverTapInstanceConfig,
    internal val analyticsManager: BaseAnalyticsManager?,
    internal val coreMetaData: CoreMetaData?,
    internal val callbackManager: BaseCallbackManager?,
    productConfigSettings: ProductConfigSettings,
    internal val fileUtils: FileUtils
) {

    private enum class PROCESSING_STATE {
        INIT,
        FETCHED,
        ACTIVATED
    }

    //use lock for synchronization for read write
    internal val activatedConfigs: MutableMap<String, String> = Collections.synchronizedMap(HashMap())

    //use lock for synchronization for read write
    internal val defaultConfigs: MutableMap<String, String> = Collections.synchronizedMap(HashMap())

    internal var initializedFlag = AtomicBoolean(false)

    private val isFetchAndActivatingFlag = AtomicBoolean(false)

    private val settingsValue: ProductConfigSettings = productConfigSettings

    //use lock for synchronization for read write
    private val waitingTobeActivatedConfig: MutableMap<String, String> = Collections
        .synchronizedMap(HashMap())

    init {
        initAsync()
    }

    /**
     * Asynchronously activates the most recently fetched configs, so that the fetched key value pairs take effect.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun activate() {
        if (TextUtils.isEmpty(settingsValue.getGuid())) {
            return
        }
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.addOnSuccessListener(OnSuccessListener { sendCallback(PROCESSING_STATE.ACTIVATED) })
            .execute("activateProductConfigs", object : Callable<Void?> {
                override fun call(): Void? {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        try {
                            //read fetched info
                            var toWriteValues = HashMap<String, String>()
                            if (waitingTobeActivatedConfig.isNotEmpty()) {
                                toWriteValues.putAll(waitingTobeActivatedConfig)
                                waitingTobeActivatedConfig.clear()
                            } else {
                                toWriteValues = getStoredValues(activatedFullPath)
                            }

                            activatedConfigs.clear()
                            //apply default config first
                            if (defaultConfigs.isNotEmpty()) {
                                activatedConfigs.putAll(defaultConfigs)
                            }
                            activatedConfigs.putAll(toWriteValues)
                            config.logger.verbose(
                                ProductConfigUtil.getLogTag(config),
                                "Activated successfully with configs: $activatedConfigs"
                            )
                        } catch (e: Exception) {
                            config.logger.verbose(
                                ProductConfigUtil.getLogTag(config),
                                "Activate failed", e
                            )
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
    open fun fetch() {
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
    open fun fetch(minimumFetchIntervalInSeconds: Long) {
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
    open fun fetchAndActivate() {
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
    open fun fetchProductConfig() {
        val event = JSONObject()
        val notif = JSONObject()
        try {
            notif.put("t", Constants.FETCH_TYPE_PC)
            event.put("evtName", Constants.WZRK_FETCH)
            event.put("evtData", notif)
        } catch (e: JSONException) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "Error creating fetch event JSON", e
            )
        }
        analyticsManager!!.sendFetchEvent(event)
        coreMetaData!!.isProductConfigRequested = true
        config.logger
            .verbose(config.accountId, Constants.LOG_TAG_PRODUCT_CONFIG + "Fetching product config")
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
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getBoolean(Key: String?): Boolean? {
        if (initializedFlag.get() && !TextUtils.isEmpty(Key)) {
            val value: String? = activatedConfigs[Key]
            if (!TextUtils.isEmpty(value)) {
                return java.lang.Boolean.parseBoolean(value)
            }
        }
        return DEFAULT_VALUE_FOR_BOOLEAN
    }

    /**
     * Returns the parameter value for the given key as a double.
     *
     * @param Key String
     * @return Double - value of the product config,if key is not present return
     * [CTProductConfigConstants.DEFAULT_VALUE_FOR_DOUBLE]
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getDouble(Key: String?): Double? {
        if (initializedFlag.get() && !TextUtils.isEmpty(Key)) {
            try {
                val value: String? = activatedConfigs[Key]
                if (!TextUtils.isEmpty(value)) {
                    return java.lang.Double.parseDouble(value!!)
                }
            } catch (e: Exception) {
                config.logger.verbose(
                    ProductConfigUtil.getLogTag(config),
                    "Error getting Double for Key-$Key", e
                )
            }
        }
        return DEFAULT_VALUE_FOR_DOUBLE
    }

    /**
     * Returns the last fetched timestamp in millis.
     *
     * @return - long value of timestamp in millis.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getLastFetchTimeStampInMillis(): Long {
        return settingsValue.lastFetchTimeStampInMillis
    }

    /** Kotlin property syntax, equivalent to [getLastFetchTimeStampInMillis]. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    val lastFetchTimeStampInMillis: Long
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
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getLong(Key: String?): Long? {
        if (initializedFlag.get() && !TextUtils.isEmpty(Key)) {
            try {
                val value: String? = activatedConfigs[Key]
                if (!TextUtils.isEmpty(value)) {
                    return java.lang.Long.parseLong(value!!)
                }
            } catch (e: Exception) {
                config.logger.verbose(
                    ProductConfigUtil.getLogTag(config),
                    "Error getting Long for Key-$Key", e
                )
            }
        }
        return DEFAULT_VALUE_FOR_LONG
    }

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
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getString(Key: String?): String {
        if (initializedFlag.get() && !TextUtils.isEmpty(Key)) {
            val value: String? = activatedConfigs[Key]
            if (!TextUtils.isEmpty(value)) {
                return value!!
            }
        }
        return DEFAULT_VALUE_FOR_STRING
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun isInitialized(): Boolean {
        return initializedFlag.get()
    }

    /** Kotlin property syntax, equivalent to [isInitialized]. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    val isInitialized: Boolean
        @JvmSynthetic @JvmName("ktIsInitialized") inline get() = isInitialized()

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun onFetchFailed() {
        isFetchAndActivatingFlag.compareAndSet(true, false)
        config.logger.verbose(ProductConfigUtil.getLogTag(config), "Fetch Failed")
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun onFetchSuccess(kvResponse: JSONObject?) {
        if (TextUtils.isEmpty(settingsValue.getGuid())) {
            return
        }
        synchronized(this) {
            if (kvResponse != null) {
                try {
                    parseFetchedResponse(kvResponse)
                    fileUtils.writeJsonToFile(
                        productConfigDirName,
                        CTProductConfigConstants.FILE_NAME_ACTIVATED,
                        JSONObject(waitingTobeActivatedConfig as Map<*, *>)
                    )
                    config.logger
                        .verbose(
                            ProductConfigUtil.getLogTag(config), "Fetch file-[" + activatedFullPath +
                                    "] write success: " + waitingTobeActivatedConfig
                        )
                    val task = CTExecutorFactory.executors(config).mainTask<Void?>()
                    task.execute("sendPCFetchSuccessCallback", Callable<Void?> {
                        config.logger
                            .verbose(ProductConfigUtil.getLogTag(config), "Product Config: fetch Success")
                        sendCallback(PROCESSING_STATE.FETCHED)
                        null
                    })
                    if (isFetchAndActivatingFlag.getAndSet(false)) {
                        activate()
                    }
                } catch (e: Exception) {
                    config.logger.verbose(ProductConfigUtil.getLogTag(config), "Product Config: fetch Failed", e)
                    sendCallback(PROCESSING_STATE.FETCHED)
                    // set fetchAndActivating flag to false if fetch fails.
                    isFetchAndActivatingFlag.compareAndSet(true, false)
                }
            }
        }
    }

    /**
     * Deletes all activated, fetched and defaults configs as well as all Product Config settings.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun reset() {
        defaultConfigs.clear()
        activatedConfigs.clear()
        settingsValue.initDefaults()
        eraseStoredConfigFiles()
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun resetSettings() {
        settingsValue.reset(fileUtils)
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun setArpValue(arp: JSONObject?) {
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
    open fun setDefaults(resourceID: Int) {
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
    open fun setDefaults(map: HashMap<String, Any>?) {
        val task = CTExecutorFactory.executors(config).ioTask<Void?>()
        task.addOnSuccessListener(OnSuccessListener { initAsync() })
            .execute("ProductConfig#setDefaultsUsingHashMap", object : Callable<Void?> {
                override fun call(): Void? {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        if (map != null && map.isNotEmpty()) {
                            for (entry in map.entries) {
                                @Suppress("SENSELESS_COMPARISON")
                                if (entry != null) {
                                    val key = entry.key
                                    val value = entry.value
                                    try {
                                        if (!TextUtils.isEmpty(key) && ProductConfigUtil.isSupportedDataType(value)) {
                                            defaultConfigs[key] = value.toString()
                                        }
                                    } catch (e: Exception) {
                                        config.logger.verbose(
                                            ProductConfigUtil.getLogTag(config),
                                            "Product Config: setDefaults Failed for Key: $key", e
                                        )
                                    }
                                }
                            }
                        }
                        config.logger.verbose(
                            ProductConfigUtil.getLogTag(config),
                            "Product Config: setDefaults Completed with: $defaultConfigs"
                        )
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
    open fun setGuidAndInit(cleverTapID: String?) {
        if (isInitialized() || TextUtils.isEmpty(cleverTapID)) {
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
    open fun setMinimumFetchIntervalInSeconds(fetchIntervalInSeconds: Long) {
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
                        config.logger
                            .verbose(ProductConfigUtil.getLogTag(config), "Reset Deleted Dir: $dirName")
                    } catch (e: Exception) {
                        config.logger.verbose(
                            ProductConfigUtil.getLogTag(config),
                            "Reset failed", e
                        )
                    }

                    return null
                }
            }
        })
    }

    internal val activatedFullPath: String
        get() = productConfigDirName + "/" + CTProductConfigConstants.FILE_NAME_ACTIVATED

    // -----------------------------------------------------------------------//
    // ********                        Internal API                      *****//
    // -----------------------------------------------------------------------//

    internal val productConfigDirName: String
        get() = CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.accountId + "_" + settingsValue.getGuid()

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    open fun getSettings(): ProductConfigSettings {
        return settingsValue
    }

    /** Kotlin property syntax, equivalent to [getSettings]. */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    val settings: ProductConfigSettings
        @JvmSynthetic @JvmName("ktGetSettings") inline get() = getSettings()

    internal fun initAsync() {
        if (TextUtils.isEmpty(settingsValue.getGuid())) {
            return
        }
        val task = CTExecutorFactory.executors(config).ioTask<Boolean>()
        task.addOnSuccessListener(OnSuccessListener { sendCallback(PROCESSING_STATE.INIT) })
            .execute("ProductConfig#initAsync", object : Callable<Boolean> {
                override fun call(): Boolean {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        try {
                            //apply defaults
                            if (defaultConfigs.isNotEmpty()) {
                                activatedConfigs.putAll(defaultConfigs)
                            }
                            val storedConfig = getStoredValues(activatedFullPath)
                            if (storedConfig.isNotEmpty()) {
                                waitingTobeActivatedConfig.putAll(storedConfig)
                            }
                            config.logger.verbose(
                                ProductConfigUtil.getLogTag(config),
                                "Loaded configs ready to be applied: $waitingTobeActivatedConfig"
                            )
                            settingsValue.loadSettings(fileUtils)
                            initializedFlag.set(true)
                        } catch (e: Exception) {
                            config.logger.verbose(
                                ProductConfigUtil.getLogTag(config),
                                "InitAsync failed", e
                            )
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
        task.addOnSuccessListener(OnSuccessListener { initAsync() })
            .execute("PCController#setDefaultsWithXmlParser", object : Callable<Void?> {
                override fun call(): Void? {
                    // Locks on this Callable instance, exactly like the Java original's synchronized (this).
                    synchronized(this) {
                        defaultConfigs.putAll(xmlParser.getDefaultsFromXml(context!!, resourceID))
                        config.logger.verbose(
                            ProductConfigUtil.getLogTag(config),
                            "Product Config: setDefaults Completed with: $defaultConfigs"
                        )
                        return null
                    }
                }
            })
    }

    private fun canRequest(minimumFetchIntervalInSeconds: Long): Boolean {
        val validGuid = !TextUtils.isEmpty(settingsValue.getGuid())

        if (!validGuid) {
            config.logger
                .verbose(ProductConfigUtil.getLogTag(config), "Product Config: Throttled due to empty Guid")
            return false
        }

        val lastRequestTime = settingsValue.lastFetchTimeStampInMillis

        val timeDifference = (System.currentTimeMillis() - lastRequestTime) - TimeUnit.SECONDS
            .toMillis(minimumFetchIntervalInSeconds)
        val isTimeExpired = timeDifference > 0
        if (!isTimeExpired) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "Throttled since you made frequent request- [Last Request Time-" +
                        Date(lastRequestTime) + "], " +
                        "Try again in " + (-timeDifference / 1000L) + " seconds"
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
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "ConvertServerJsonToMap failed", e
            )
            return map
        }

        if (kvArray != null && kvArray.length() > 0) {
            for (i in 0 until kvArray.length()) {
                try {
                    val `object` = kvArray.get(i) as JSONObject?
                    if (`object` != null) {
                        val Key = `object`.getString(PRODUCT_CONFIG_JSON_KEY_FOR_KEY)
                        val Value = `object`.getString(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE)
                        if (!TextUtils.isEmpty(Key)) {
                            map[Key] = Value
                        }
                    }
                } catch (e: Exception) {
                    config.logger.verbose(
                        ProductConfigUtil.getLogTag(config),
                        "ConvertServerJsonToMap failed", e
                    )
                }
            }
        }
        return map
    }

    private fun getStoredValues(fullFilePath: String): HashMap<String, String> {
        val map = HashMap<String, String>()
        val content: String?
        try {
            content = fileUtils.readFromFile(fullFilePath)
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "GetStoredValues reading file success:[ $fullFilePath]--[Content]$content"
            )
        } catch (e: Exception) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "GetStoredValues reading file failed", e
            )
            return map
        }
        if (!TextUtils.isEmpty(content)) {
            val jsonObject: JSONObject
            try {
                jsonObject = JSONObject(content!!)
            } catch (e: Exception) {
                config.logger.verbose(
                    ProductConfigUtil.getLogTag(config),
                    "GetStoredValues failed due to malformed json", e
                )
                return map
            }
            val iterator = jsonObject.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                if (!TextUtils.isEmpty(key)) {
                    val value: String
                    try {
                        value = jsonObject.get(key).toString()
                    } catch (e: Exception) {
                        config.logger.verbose(
                            ProductConfigUtil.getLogTag(config),
                            "GetStoredValues failed for key $key while parsing json",
                            e
                        )
                        continue
                    }
                    if (!TextUtils.isEmpty(value)) {
                        map[key] = value
                    }
                }
            }
        }
        return map
    }

    private fun onActivated() {
        val productConfigListener = callbackManager!!.productConfigListener
        if (productConfigListener != null) {
            productConfigListener.onActivated()
        }
    }

    //Event
    private fun onFetched() {
        val productConfigListener = callbackManager!!.productConfigListener
        if (productConfigListener != null) {
            productConfigListener.onFetched()
        }
    }

    private fun onInit() {
        val productConfigListener = callbackManager!!.productConfigListener
        if (productConfigListener != null) {
            config.logger.verbose(config.accountId, "Product Config initialized")
            productConfigListener.onInit()
        }
    }

    @Synchronized
    private fun parseFetchedResponse(jsonObject: JSONObject) {
        val map = convertServerJsonToMap(jsonObject)
        waitingTobeActivatedConfig.clear()
        waitingTobeActivatedConfig.putAll(map)
        config.logger
            .verbose(ProductConfigUtil.getLogTag(config), "Product Config: Fetched response:$jsonObject")
        var timestamp: Int? = null
        try {
            timestamp = jsonObject.get(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP) as Int?
        } catch (e: Exception) {
            config.logger.verbose(
                ProductConfigUtil.getLogTag(config),
                "ParseFetchedResponse failed", e
            )
        }
        if (timestamp != null) {
            settingsValue.lastFetchTimeStampInMillis = timestamp * 1000L
        }
    }

    private fun sendCallback(state: PROCESSING_STATE?) {
        if (state != null) {
            when (state) {
                PROCESSING_STATE.INIT -> onInit()
                PROCESSING_STATE.FETCHED -> onFetched()
                PROCESSING_STATE.ACTIVATED -> onActivated()
            }
        }
    }
}
