package com.clevertap.android.sdk.features

import android.content.Context
import android.os.Bundle
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.InAppFCManager
import com.clevertap.android.sdk.InAppNotificationButtonListener
import com.clevertap.android.sdk.InAppNotificationListener
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.PushPermissionHandler
import com.clevertap.android.sdk.PushPermissionResponseListener
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.features.callbacks.InAppCallbackManager
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.InAppNotificationInflater
import com.clevertap.android.sdk.inapp.StoreRegistryInAppQueue
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.customtemplates.system.SystemTemplates
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.evaluation.LimitsMatcher
import com.clevertap.android.sdk.inapp.evaluation.TriggersMatcher
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.response.InAppResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * In-app messaging feature
 * Manages in-app notifications, templates, evaluations, and impressions
 */
internal class InAppFeature(
    private val dataFeature: DataFeature,
    val storeRegistry: StoreRegistry,
    val storeProvider: StoreProvider
) : CleverTapFeature, InAppFeatureMethods {

    lateinit var coreContract: CoreContract
    var inAppFCManager: InAppFCManager? = null

    // Lazy-initialized InApp dependencies (initialized after coreContract is set)
    val inAppCallbackManager: InAppCallbackManager by lazy {
        InAppCallbackManager()
    }
    
    val triggerManager: TriggerManager by lazy {
        TriggerManager(
            coreContract.context(),
            coreContract.config().accountId,
            coreContract.deviceInfo()
        )
    }
    
    val impressionManager: ImpressionManager by lazy {
        ImpressionManager(storeRegistry)
    }
    
    private val triggersMatcher: TriggersMatcher by lazy {
        TriggersMatcher(dataFeature.localDataStore)
    }
    
    private val limitsMatcher: LimitsMatcher by lazy {
        LimitsMatcher(impressionManager, triggerManager)
    }
    
    private val inAppActionHandler: InAppActionHandler by lazy {
        InAppActionHandler(
            context = coreContract.context(),
            ctConfig = coreContract.config(),
            pushPermissionHandler = PushPermissionHandler(
                config = coreContract.config(),
                ctListeners = inAppCallbackManager.getPushPermissionResponseListenerList()
            )
        )
    }
    
    private val systemTemplates: Set<CustomTemplate> by lazy {
        SystemTemplates.getSystemTemplates(inAppActionHandler)
    }
    
    val templatesManager: TemplatesManager by lazy {
        TemplatesManager.createInstance(coreContract.config(), systemTemplates)
    }
    
    val evaluationManager: EvaluationManager by lazy {
        EvaluationManager(
            triggersMatcher = triggersMatcher,
            triggersManager = triggerManager,
            limitsMatcher = limitsMatcher,
            storeRegistry = storeRegistry,
            templatesManager = templatesManager
        )
    }
    
    private val inAppNotificationInflater: InAppNotificationInflater by lazy {
        InAppNotificationInflater(
            storeRegistry,
            templatesManager,
            coreContract.executors(),
            { FileResourceProvider.getInstance(coreContract.context(), coreContract.logger()) }
        )
    }
    
    val inAppController: InAppController by lazy {
        InAppController(
            context = coreContract.context(),
            config = coreContract.config(),
            executors = coreContract.executors(),
            callbackManager = inAppCallbackManager,
            analyticsManager = coreContract.analytics(),
            coreMetaData = coreContract.coreMetaData(),
            manifestInfo = ManifestInfo.getInstance(coreContract.context()),
            deviceInfo = coreContract.deviceInfo(),
            inAppQueue = StoreRegistryInAppQueue(storeRegistry, coreContract.config().accountId),
            evaluationManager = evaluationManager,
            templatesManager = templatesManager,
            inAppActionHandler = inAppActionHandler,
            inAppNotificationInflater = inAppNotificationInflater,
            clock = coreContract.clock()
        )
    }
    
    val inAppResponse: InAppResponse by lazy {
        InAppResponse(
            coreContract.config().accountId,
            coreContract.logger(),
            false,
            templatesManager
        )
    }

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
    ) {
        handleInAppResponse(response)
    }

    private fun handleInAppResponse(response: JSONObject) {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing inapp messages"
            )
            return
        }
        if (storeRegistry.isNotInitialised()) {
            // In legacy we did not process response but this case is unlikely as we init stores
            // on IO or as soon as device id is ready which is first call in sdk.
            // Keeping for coherent behaviour and can be removed after specific testing.
            return
        }
        if (response.length() == 0) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "There is no inapps data to handle"
            )
            return
        }
        // todo: fix user switch flush flag, full response flag.
        inAppResponse.processResponse(
            response, false, false,
            inAppController.getInAppFCManager(), this
        )
    }

    fun batchSent(batch: JSONArray, success: Boolean) {
        if (batch.length() == 0) {
            // picked from legacy FetchInAppListener.kt
            inAppCallbackManager.getFetchInAppsCallback()?.onInAppsFetched(success)
            return
        }
        checkAppLaunched(batch, success)
        checkWzrkFetch(batch, success)
    }

    private fun checkWzrkFetch(batch: JSONArray, success: Boolean) {
        for (i in 0 until batch.length()) {
            val batchItem = batch.optJSONObject(i) ?: JSONObject()
            val batchItemEvtData = batchItem.optJSONObject(Constants.KEY_EVT_DATA) ?: JSONObject()

            if (batchItem.optString(Constants.KEY_EVT_NAME) == Constants.WZRK_FETCH
                && batchItemEvtData.optInt(Constants.KEY_T) == Constants.FETCH_TYPE_IN_APPS
            ) {
                inAppCallbackManager.getFetchInAppsCallback()?.onInAppsFetched(success)
                break
            }
        }
    }

    private fun checkAppLaunched(batch: JSONArray, success: Boolean) {
        for (i in 0 until batch.length()) {
            val item = batch.getJSONObject(i)
            if (item.optString("evtName") == Constants.APP_LAUNCHED_EVENT && success) {
                inAppController.evaluateAppLaunchedInApps()
                break
            }
        }
    }

    override fun clearStaleInAppCache(inappStaleList: JSONArray) {
        //Stale in-app ids used to remove in-app counts and triggers
        for (i in 0..<inappStaleList.length()) {
            val inappStaleId = inappStaleList.optString(i)
            storeRegistry.impressionStore?.clear(inappStaleId)
            triggerManager.removeTriggers(inappStaleId)
        }
    }

    override fun handleAppLaunchServerSide(inapps: JSONArray) {
        try {
            inAppController.onAppLaunchServerSideInAppsResponse(
                inapps,
                coreContract.coreMetaData().locationFromUser
            )
        } catch (e: Throwable) {
            val logger = coreContract.logger()
            val accountId = coreContract.config().accountId
            logger.verbose(accountId, "InAppManager: Malformed AppLaunched ServerSide inApps")
            logger.verbose(accountId, "InAppManager: Reason: " + e.message, e)
        }
    }

    override fun displayInApp(inappNotifsArray: JSONArray) {
        // Fire the first notification, if any
        val task = coreContract.executors().postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppResponse#processResponse") {
            inAppController.addInAppNotificationsToQueue(inappNotifsArray)
        }
    }

    override fun storeServerSideInApps(jsonArray: JSONArray) {
        storeRegistry.inAppStore?.storeServerSideInApps(jsonArray)
    }

    override fun storeClientSideInApps(jsonArray: JSONArray) {
        storeRegistry.inAppStore?.storeClientSideInApps(jsonArray)
    }

    override fun setMode(mode: String) {
        storeRegistry.inAppStore?.mode = mode
    }

    override fun preloadFilesAndCache(meta: List<Pair<String, CtCacheType>>) {
        FileResourcesRepoFactory.createFileResourcesRepo(
            coreContract.context(),
            coreContract.logger(),
            storeRegistry
        ).also {
            it.preloadFilesAndCache(meta)
        }
    }

    override fun cleanupStaleFiles(files: List<String>) {
        FileResourcesRepoFactory.createFileResourcesRepo(
            coreContract.context(),
            coreContract.logger(),
            storeRegistry
        ).also {
            it.cleanupStaleFiles(files)
        }
    }

    override fun updateInAppFcManager(perDay: Int, perSession: Int, response: JSONObject) {
        inAppFCManager?.updateLimits(coreContract.context(), perDay, perSession)
        inAppFCManager?.processResponse(coreContract.context(), response)
    }

    /**
     * Called when user changes, updates the inapp stores to save data against this new user.
     */
    fun userChanged(deviceId: String) {
        storeRegistry.inAppStore?.onChangeUser(
            deviceId = deviceId,
            accountId = coreContract.config().accountId
        )
        storeRegistry.impressionStore?.onChangeUser(
            deviceId = deviceId,
            accountId = coreContract.config().accountId
        )
    }

    /**
     * Phase 2: Initialization method moved from CoreState
     * Initializes the InAppFCManager with the provided device ID
     */
    fun initInAppFCManager(deviceId: String) {
        val iam = InAppFCManager(
            coreContract.context(),
            coreContract.config(),
            deviceId,
            storeRegistry,
            impressionManager,
            coreContract.executors(),
            coreContract.clock()
        )
        coreContract.executors().postAsyncSafelyTask<Unit>().execute("initInAppFCManager") {
            iam.init(deviceId)
        }
        this.inAppFCManager = iam
        this.inAppController.setInAppFCManager(iam)
    }

    /**
     * Phase 2: Store initialization method moved from CoreState
     * Initializes InApp and Impression stores for the given device ID
     */
    @WorkerThread
    fun initInAppStores(deviceId: String?) {
        if (deviceId != null) {
            if (storeRegistry.inAppStore == null) {
                val inAppStore = storeProvider.provideInAppStore(
                    context = coreContract.context(),
                    cryptHandler = coreContract.cryptHandler(),
                    deviceId = deviceId,
                    accountId = coreContract.config().accountId
                )
                storeRegistry.inAppStore = inAppStore
                evaluationManager.loadSuppressedCSAndEvaluatedSSInAppsIds()
            }
            if (storeRegistry.impressionStore == null) {
                val impStore = storeProvider.provideImpressionStore(
                    context = coreContract.context(),
                    deviceId = deviceId,
                    accountId = coreContract.config().accountId
                )
                storeRegistry.impressionStore = impStore
            }
        }
    }

    @WorkerThread
    fun handleInAppPreview(extras: Bundle) {
        try {
            // Use requireNotNull for values that must exist, or let for safer parsing
            val payloadString = extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY)
            if (payloadString.isNullOrEmpty()) {
                Logger.v("In-app preview payload string is missing or empty.")
                return
            }

            val payloadType = extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY)
            val payloadJson = JSONObject(payloadString)

            // Determine the content based on the payload type
            val notificationContent = when (payloadType) {
                Constants.INAPP_IMAGE_INTERSTITIAL_TYPE,
                Constants.INAPP_ADVANCED_BUILDER_TYPE -> getHalfInterstitialInApp(payloadJson)

                else -> payloadJson
            }

            // Construct the final JSON response
            val notificationsArray = JSONArray().put(notificationContent)
            val apiResponseJson = JSONObject().apply {
                put(Constants.INAPP_JSON_RESPONSE_KEY, notificationsArray)
            }

            handleApiData(response = apiResponseJson)

        } catch (e: JSONException) {
            // Catch specific exceptions
            Logger.v("Failed to parse in-app preview JSON from push payload", e)
        } catch (t: Throwable) {
            // A fallback for any other unexpected errors
            Logger.v("An unexpected error occurred while handling in-app preview", t)
        }
    }

    @Throws(JSONException::class)
    private fun getHalfInterstitialInApp(inapp: JSONObject): JSONObject? {
        val inAppConfig = inapp.optString(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG)
        val htmlContent: String? = wrapImageInterstitialContent(inAppConfig)

        if (htmlContent != null) {
            inapp.put(Constants.KEY_TYPE, Constants.KEY_CUSTOM_HTML)
            val data = inapp.opt(Constants.INAPP_DATA_TAG)

            if (data is JSONObject) {
                var dataObject = data
                dataObject = JSONObject(dataObject.toString()) // Create a mutable copy
                // Update the html
                dataObject.put(Constants.INAPP_HTML_TAG, htmlContent)
                inapp.put(Constants.INAPP_DATA_TAG, dataObject)
            } else {
                // If data key is not present or it is not a JSONObject,
                // set it and overwrite it
                val newData = JSONObject()
                newData.put(Constants.INAPP_HTML_TAG, htmlContent)
                inapp.put(Constants.INAPP_DATA_TAG, newData)
            }
        } else {
            coreContract.config().getLogger()
                .debug(coreContract.config().accountId, "Failed to parse the image-interstitial notification")
            return null
        }

        return inapp
    }

    /**
     * Wraps the provided content with HTML obtained from the image-interstitial file.
     *
     * @param content The content to be wrapped within the image-interstitial HTML.
     * @return The wrapped content, or null if an error occurs during HTML retrieval or processing.
     */
    fun wrapImageInterstitialContent(content: String?): String? {
        try {
            val html = Utils.readAssetFile(coreContract.context(), Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME)
            if (html != null && content != null) {
                val parts: Array<String?> =
                    html.split(Constants.INAPP_HTML_SPLIT.toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                if (parts.size == 2) {
                    return parts[0] + content + parts[1]
                }
            }
        } catch (e: IOException) {
            coreContract.config().getLogger()
                .debug(/* suffix = */ coreContract.config().accountId, /* message = */ "Failed to read the image-interstitial HTML file")
        }
        return null
    }

    /**
     * Evaluates and potentially displays an in-app notification based on the triggered event.
     * This method is called when an event is raised, and it checks if any in-app campaigns
     * are configured to be shown for that event.
     *
     * @param context The Android context
     * @param event The [JSONObject] representing the event that was triggered
     * @param eventType The type of event (RAISED_EVENT, PROFILE_EVENT, etc.)
     */
    fun evaluateInAppForEvent(
        context: Context,
        event: JSONObject,
        eventType: Int,
        eventMediator: EventMediator
    ) {
        val eventName = eventMediator.getEventName(event)
        val userLocation = coreContract.coreMetaData().locationFromUser
        
        // Update local store for raised events
        updateLocalStore(eventName, eventType)

        when {
            eventMediator.isChargedEvent(event) -> {
                inAppController.onQueueChargedEvent(
                    eventMediator.getChargedEventDetails(event),
                    eventMediator.getChargedEventItemDetails(event),
                    userLocation
                )
            }
            
            !NetworkManager.isNetworkOnline(context) && eventMediator.isEvent(event) -> {
                // When offline, evaluate all events
                inAppController.onQueueEvent(
                    eventName!!,
                    eventMediator.getEventProperties(event),
                    userLocation
                )
            }
            
            eventType == Constants.PROFILE_EVENT -> {
                // Evaluate for user attribute changes
                val userAttributeChangedProperties = 
                    eventMediator.computeUserAttributeChangeProperties(event)
                inAppController.onQueueProfileEvent(userAttributeChangedProperties, userLocation)
            }
            
            !eventMediator.isAppLaunchedEvent(event) && eventMediator.isEvent(event) -> {
                // When online, only evaluate non-appLaunched events
                inAppController.onQueueEvent(
                    eventName!!,
                    eventMediator.getEventProperties(event),
                    userLocation
                )
            }
        }
    }

    /**
     * Updates the local store with event information for raised events
     */
    @WorkerThread
    private fun updateLocalStore(eventName: String?, type: Int) {
        if (type == Constants.RAISED_EVENT) {
            dataFeature.localDataStore.persistUserEventLog(eventName)
        }
    }

    // ========== PUBLIC API FACADE ==========
    // These methods provide direct delegation from CleverTapAPI to InApp functionality
    // Signature matches CleverTapAPI public methods for 1:1 mapping

    /**
     * Suspends display of InApp Notifications.
     * The InApp Notifications are queued once this method is called.
     */
    fun suspendInApps() {
        if (!coreContract.config().isAnalyticsOnly) {
            coreContract.logger().debug(coreContract.config().accountId, "Suspending InApp Notifications...")
            coreContract.logger().debug(
                coreContract.config().accountId,
                "Please Note - InApp Notifications will be suspended till resumeInAppNotifications() is not called again"
            )
            inAppController.suspendInApps()
        } else {
            coreContract.logger().debug(
                coreContract.config().accountId,
                "CleverTap instance is set for Analytics only! Cannot suspend InApp Notifications."
            )
        }
    }

    /**
     * Resumes display of InApp Notifications.
     * Shows all queued InApp Notifications and resumes InApp on events raised after this method.
     */
    fun resumeInApps() {
        if (!coreContract.config().isAnalyticsOnly) {
            coreContract.logger().debug(coreContract.config().accountId, "Resuming InApp Notifications...")
            inAppController.resumeInApps()
        } else {
            coreContract.logger().debug(
                coreContract.config().accountId,
                "CleverTap instance is set for Analytics only! Cannot resume InApp Notifications."
            )
        }
    }

    /**
     * Suspends display of InApp Notifications and discards any new InApp Notifications.
     * InApp Notifications will be displayed only once resumeInAppNotifications() is called.
     */
    fun discardInApps() {
        if (!coreContract.config().isAnalyticsOnly) {
            coreContract.logger().debug(coreContract.config().accountId, "Discarding InApp Notifications...")
            coreContract.logger().debug(
                coreContract.config().accountId,
                "Please Note - InApp Notifications will be dropped till resumeInAppNotifications() is not called again"
            )
            inAppController.discardInApps()
        } else {
            coreContract.logger().debug(
                coreContract.config().accountId,
                "CleverTap instance is set for Analytics only! Cannot discard InApp Notifications."
            )
        }
    }

    /**
     * Checks whether notification permission is granted or denied for Android 13 and above devices.
     */
    fun isPushPermissionGranted(): Boolean {
        return inAppController.isPushPermissionGranted()
    }

    /**
     * Calls the push primer flow for Android 13 and above devices.
     */
    fun promptPushPrimer(jsonObject: JSONObject) {
        inAppController.promptPushPrimer(jsonObject)
    }

    /**
     * Calls directly hard permission dialog, if push primer is not required.
     */
    fun promptPermission(showFallbackSettings: Boolean) {
        inAppController.promptPermission(showFallbackSettings)
    }

    /**
     * Returns the InAppNotificationListener object
     */
    fun getInAppNotificationListener(): InAppNotificationListener? {
        return inAppCallbackManager.getInAppNotificationListener()
    }

    /**
     * Sets the InAppNotificationListener
     */
    fun setInAppNotificationListener(listener: InAppNotificationListener?) {
        inAppCallbackManager.setInAppNotificationListener(listener)
    }

    /**
     * Sets the InAppNotificationButtonListener
     */
    fun setInAppNotificationButtonListener(listener: InAppNotificationButtonListener?) {
        inAppCallbackManager.setInAppNotificationButtonListener(listener)
    }

    /**
     * Registers PushPermissionResponseListener
     */
    fun registerPushPermissionResponseListener(listener: PushPermissionResponseListener?) {
        if (listener == null) {
            Logger.v("Passing null PushPermissionResponseListener to register is not allowed")
            return
        }
        inAppCallbackManager.registerPushPermissionResponseListener(listener)
    }

    /**
     * Unregisters PushPermissionResponseListener
     */
    fun unregisterPushPermissionResponseListener(listener: PushPermissionResponseListener?) {
        if (listener == null) {
            Logger.v("Passing null PushPermissionResponseListener to unregister is not allowed")
            return
        }
        inAppCallbackManager.unregisterPushPermissionResponseListener(listener)
    }

    /**
     * Retrieve a CustomTemplateContext for a template that is currently displaying.
     */
    fun getActiveContextForTemplate(templateName: String): CustomTemplateContext? {
        return templatesManager.getActiveContextForTemplate(templateName)
    }

    /**
     * Deletes all images and gifs which are preloaded for inapps in cs mode
     */
    @WorkerThread
    fun clearInAppResources(expiredOnly: Boolean) {
        val impl = FileResourcesRepoFactory.createFileResourcesRepo(
            coreContract.context(),
            coreContract.logger(),
            storeRegistry
        )

        if (expiredOnly) {
            impl.cleanupExpiredResources(CtCacheType.IMAGE)
        } else {
            impl.cleanupAllResources(CtCacheType.IMAGE)
        }
    }

    /**
     * Deletes all types of files which are preloaded for SDK features
     */
    @WorkerThread
    fun clearFileResources(expiredOnly: Boolean) {
        val impl = FileResourcesRepoFactory.createFileResourcesRepo(
            coreContract.context(),
            coreContract.logger(),
            storeRegistry
        )

        if (expiredOnly) {
            impl.cleanupExpiredResources(CtCacheType.FILES)
        } else {
            impl.cleanupAllResources(CtCacheType.FILES)
        }
    }

    /**
     * Fetches in-app notifications from the server.
     * This method is asynchronous and the result of the fetch operation
     * will be delivered to the [com.clevertap.android.sdk.FetchInAppsCallback] if it has been set.
     *
     * @param callback An optional [com.clevertap.android.sdk.FetchInAppsCallback] to be notified
     *                 of the fetch result. If provided, this callback will be triggered once,
     *                 either on success or failure. It is then discarded.
     */
    fun fetchInApps(callback: FetchInAppsCallback?) {
        if (coreContract.config().isAnalyticsOnly) {
            return
        }
        Logger.v(Constants.LOG_TAG_INAPP + " Fetching In Apps...")

        if (callback != null) {
            inAppCallbackManager.setFetchInAppsCallback(callback)
        }

        val event = AnalyticsFeature.fetchRequestAsJson(Constants.FETCH_TYPE_IN_APPS)
        coreContract.analytics().sendFetchEvent(event)
    }

    // ========== PUBLIC API FACADE END ==========
}

internal interface InAppFeatureMethods {
    fun clearStaleInAppCache(inappStaleList: JSONArray)
    fun handleAppLaunchServerSide(inapps: JSONArray)
    fun displayInApp(inappNotifsArray: JSONArray)
    fun storeServerSideInApps(jsonArray: JSONArray)
    fun storeClientSideInApps(jsonArray: JSONArray)
    fun setMode(mode: String)
    fun preloadFilesAndCache(meta: List<Pair<String, CtCacheType>> )
    fun cleanupStaleFiles(files: List<String>)
    fun updateInAppFcManager(perDay: Int, perSession: Int, response: JSONObject)
}
