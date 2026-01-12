package com.clevertap.android.sdk.inapp


import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.InAppNotificationActivity
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeAlert
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeCover
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeCoverHTML
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeCoverImageOnly
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeCustomCodeTemplate
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeFooter
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeFooterHTML
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeHalfInterstitial
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeHalfInterstitialHTML
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeHalfInterstitialImageOnly
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeHeader
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeHeaderHTML
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeInterstitial
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeInterstitialHTML
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypeInterstitialImageOnly
import com.clevertap.android.sdk.inapp.CTLocalInApp.Companion.FALLBACK_TO_NOTIFICATION_SETTINGS
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter
import com.clevertap.android.sdk.inapp.delay.DelayedInAppResult
import com.clevertap.android.sdk.inapp.delay.InActionResult
import com.clevertap.android.sdk.inapp.delay.InAppScheduler
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlFooterFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlHeaderFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeFooterFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeHeaderFragment
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Collections

internal class InAppController(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val executors: CTExecutors,
    private val controllerManager: ControllerManager,
    private val callbackManager: BaseCallbackManager,
    private val analyticsManager: AnalyticsManager,
    private val coreMetaData: CoreMetaData,
    manifestInfo: ManifestInfo,
    private val deviceInfo: DeviceInfo,
    private val inAppQueue: InAppQueue,
    private val evaluationManager: EvaluationManager,
    private val templatesManager: TemplatesManager,
    private val inAppActionHandler: InAppActionHandler,
    private val inAppNotificationInflater: InAppNotificationInflater,
    private val inAppDelayManager: InAppScheduler<DelayedInAppResult>,
    private val inAppInActionManager: InAppScheduler<InActionResult>,
    private val clock: Clock
) : InAppListener {

    private enum class InAppState {
        DISCARDED,
        SUSPENDED,
        RESUMED
    }

    private var inAppDisplayListener: WeakReference<InAppDisplayListener>? = null

    fun registerInAppDisplayListener(display: InAppDisplayListener) {
        inAppDisplayListener = WeakReference(display)
    }

    fun unregisterInAppDisplayListener() {
        logger.verbose("Unregistering InAppDisplay Listener")
        inAppDisplayListener = null
    }

    companion object {
        const val LOCAL_INAPP_COUNT = "local_in_app_count"
        const val IS_FIRST_TIME_PERMISSION_REQUEST = "firstTimeRequest"

        private val pendingNotifications =
            Collections.synchronizedList(ArrayList<CTInAppNotification>())

        @VisibleForTesting
        @Volatile
        internal var currentlyDisplayingInApp: CTInAppNotification? = null
            private set

        @VisibleForTesting
        internal fun clearCurrentlyDisplayingInApp() {
            currentlyDisplayingInApp = null
        }
    }

    val onAppLaunchEventSent: () -> Unit = {
        val appLaunchedProperties = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)
        val clientSideInAppsToDisplay =
            evaluationManager.evaluateOnAppLaunchedClientSide(
                appLaunchedProperties, coreMetaData.locationFromUser
            )
        if (clientSideInAppsToDisplay.first.isNotEmpty()) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay.first)
        }
        if (clientSideInAppsToDisplay.second.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(clientSideInAppsToDisplay.second)
        }
    }

    private val logger = config.logger
    private val defaultLogTag = config.accountId

    @Volatile
    private var inAppState = InAppState.RESUMED

    private val inAppExcludedActivityNames = getExcludedActivitiesSet(manifestInfo)

    /**
     * Schedule multiple delayed in-apps for display after their respective delays
     */
    @WorkerThread
    fun scheduleDelayedInAppsForAllModes(delayedInApps: List<JSONObject>) {
        logger.verbose(
            config.accountId,
            "InAppController: Scheduling ${delayedInApps.size} delayed in-apps"
        )

        inAppDelayManager.schedule(delayedInApps) { result ->
            when (result) {
                is DelayedInAppResult.Success -> {
                    logger.verbose(
                        config.accountId,
                        "InAppController: Successfully retrieved delayed in-app ${result.inAppId}"
                    )

                    val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
                    task.execute("InAppController#executeDelayedInAppCallback-${result.inAppId}") {
                        logger.verbose(config.accountId,"updating ttl L")
                        //result.inApp.put(Constants.WZRK_TIME_TO_LIVE_OFFSET,60L)// 60 sec ttl for testing
                        //Calculate fresh TTL after delay completes
                        evaluationManager.updateTTL(result.inApp)

                        // Add to display queue30
                        addInAppNotificationInFrontOfQueue(result.inApp)
                    }
                }

                is DelayedInAppResult.Error -> {
                    logger.verbose(
                        config.accountId,
                        "InAppController: Error for delayed in-app ${result.inAppId}: ${result.reason}",
                        result.throwable
                    )
                }

                is DelayedInAppResult.Discarded -> {
                    logger.verbose(
                        config.accountId,
                        "InAppController: in-app discarded ${result.id}: ${result.reason}"
                    )
                }
            }
        }
    }

    @WorkerThread
    fun scheduleInActionInApps(inActionMetadata: List<JSONObject>) {
        logger.verbose(
            config.accountId,
            "[InAppController]: Scheduling ${inActionMetadata.size} in-action in-apps"
        )

        inAppInActionManager.schedule(inActionMetadata) { result ->
            when (result) {
                is InActionResult.ReadyToFetch -> {
                    // After inaction duration expires, fetch content from backend
                    logger.verbose(
                        defaultLogTag,
                        "[InAppController]: In-action duration expired for targetId: ${result.targetId}, calling fetch API"
                    )
                    fetchInActionInApp(result.targetId)
                }
                is InActionResult.Error -> {
                    logger.verbose(
                        defaultLogTag,
                        "[InAppController]Error scheduling in-action in-app: ${result.message} for targetId: ${result.targetId}"
                    )
                }
                is InActionResult.Cancelled -> {
                    logger.verbose(
                        defaultLogTag,
                        "[InAppController]In-action in-app cancelled for targetId: ${result.targetId}"
                    )
                }

                is InActionResult.Discarded -> {
                    logger.verbose(
                        defaultLogTag,
                        "[InAppController]In-action: in-app discarded ${result.id}: ${result.reason}"
                    )
                }
            }
        }
    }

    /**
     * Get count of currently active delayed in-apps
     */
    fun getActiveDelayedInAppsCount(): Int {
        return inAppDelayManager.getActiveCount()
    }

    fun promptPushPrimer(jsonObject: JSONObject) {
        jsonObject.put(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION, true)
        val fallbackToSettings = jsonObject.optBoolean(FALLBACK_TO_NOTIFICATION_SETTINGS, false)
        // always show the primer when fallback to settings is enabled
        inAppActionHandler.launchPushPermissionPrompt(
            fallbackToSettings,
            fallbackToSettings //alwaysRequestIfNotGranted
        )
        { activity -> prepareNotificationForDisplay(jsonObject) }
    }

    fun promptPermission(showFallbackSettings: Boolean) {
        inAppActionHandler.launchPushPermissionPrompt(showFallbackSettings)
    }

    fun isPushPermissionGranted(): Boolean {
        return inAppActionHandler.arePushNotificationsEnabled()
    }

    override fun inAppNotificationActionTriggered(
        inAppNotification: CTInAppNotification,
        action: CTInAppAction,
        callToAction: String,
        additionalData: Bundle?,
        activityContext: Context?
    ): Bundle {
        val data = if (additionalData != null) {
            Bundle(additionalData)
        } else {
            Bundle()
        }
        data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.campaignId)
        data.putString(Constants.KEY_C2A, callToAction)

        // send clicked event
        if (!inAppNotification.isLocalInApp) {
            analyticsManager.pushInAppNotificationStateEvent(true, inAppNotification, data)
        }

        val type = action.type
        if (type == null) {
            logger.debug("Triggered in-app action without type")
            return data
        }

        when (type) {
            InAppActionType.CUSTOM_CODE -> {
                triggerCustomTemplateAction(inAppNotification, action.customTemplateInAppData)
            }

            InAppActionType.CLOSE -> {
                if (CTInAppTypeCustomCodeTemplate == inAppNotification.inAppType) {
                    templatesManager.closeTemplate(inAppNotification)
                }
                // SDK In-Apps are dismissed in CTInAppBaseFragment::handleButtonClick or CTInAppNotificationActivity
            }

            InAppActionType.OPEN_URL -> {
                val actionUrl = action.actionUrl
                if (actionUrl != null) {
                    inAppActionHandler.openUrl(actionUrl, activityContext)
                } else {
                    logger.debug("Cannot trigger open url action without url value")
                }
            }

            InAppActionType.KEY_VALUES -> {
                val keyValues = action.keyValues
                if (keyValues?.isNotEmpty() == true) {
                    if (callbackManager.getInAppNotificationButtonListener() != null) {
                        callbackManager.getInAppNotificationButtonListener()
                            .onInAppButtonClick(keyValues)
                    }
                }
            }

            else -> {
                // do nothing
            }
        }

        return data
    }

    override fun inAppNotificationDidClick(
        inAppNotification: CTInAppNotification,
        button: CTInAppNotificationButton,
        activityContext: Context?
    ): Bundle? {
        val action = button.action
        if (action == null) {
            return null
        }
        return inAppNotificationActionTriggered(
            inAppNotification,
            action,
            button.text,
            null,
            activityContext
        )
    }

    override fun inAppNotificationDidDismiss(
        inAppNotification: CTInAppNotification,
        formData: Bundle?
    ) {

        if (controllerManager.inAppFCManager != null) {
            val templateName = inAppNotification.customTemplateData?.templateName ?: ""
            logger.verbose(
                defaultLogTag,
                "InApp Dismissed: ${inAppNotification.campaignId} $templateName"
            )
        } else {
            logger.verbose(
                defaultLogTag,
                "Not calling InApp Dismissed: ${inAppNotification.campaignId} because InAppFCManager is null"
            )
        }
        try {
            val listener = callbackManager.getInAppNotificationListener()
            if (listener != null) {
                val notifKVS = if (inAppNotification.customExtras != null) {
                    Utils.convertJSONObjectToHashMap(inAppNotification.customExtras)
                } else {
                    HashMap<String, Any>()
                }

                logger.verbose("Calling the in-app listener on behalf of ${coreMetaData.source}")

                if (formData != null) {
                    listener.onDismissed(notifKVS, Utils.convertBundleObjectToHashMap(formData))
                } else {
                    listener.onDismissed(notifKVS, null)
                }
            }
        } catch (t: Throwable) {
            logger.verbose(defaultLogTag, "Failed to call the in-app notification listener", t)
        }

        // Fire the next one, if any
        val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InappController#inAppNotificationDidDismiss") {
            inAppDidDismiss(inAppNotification)
            _showNotificationIfAvailable()
        }
    }

    override fun inAppNotificationDidShow(
        inAppNotification: CTInAppNotification,
        formData: Bundle?
    ) {
        controllerManager.inAppFCManager?.didShow(context, inAppNotification)
        analyticsManager.pushInAppNotificationStateEvent(false, inAppNotification, formData)

        //Fire onShow() callback when InApp is shown.
        try {
            callbackManager.getInAppNotificationListener()?.onShow(inAppNotification)
        } catch (t: Throwable) {
            logger.verbose(defaultLogTag, "Failed to call the in-app notification listener", t)
        }
    }

    fun discardInApps(hideInAppIfVisible: Boolean) {
        inAppState = InAppState.DISCARDED
        logger.verbose(defaultLogTag, "InAppState is DISCARDED")

        if (hideInAppIfVisible) {
            logger.verbose(defaultLogTag, "Hiding InApp if visible")
            Utils.runOnUiThread { hideCurrentlyDisplayingInApp() }
        }
    }

    @MainThread
    private fun hideCurrentlyDisplayingInApp() {
        val inApp = currentlyDisplayingInApp ?: return

        logger.verbose(defaultLogTag, "Hiding currently displaying InApp: ${inApp.campaignId}")
        inAppDisplayListener?.get()?.hideInApp()
    }

    fun resumeInApps() {
        inAppState = InAppState.RESUMED
        logger.verbose(defaultLogTag, "InAppState is RESUMED")
        logger.verbose(defaultLogTag, "Resuming InApps by calling showInAppNotificationIfAny()")
        showNotificationIfAvailable()
    }

    fun suspendInApps() {
        inAppState = InAppState.SUSPENDED
        logger.verbose(defaultLogTag, "InAppState is SUSPENDED")
    }

    @WorkerThread
    fun addInAppNotificationsToQueue(inappNotifs: List<JSONObject>) {
        try {
            val filteredNotifs = filterNonRegisteredCustomTemplates(inappNotifs)
            inAppQueue.enqueueAll(filteredNotifs)

            // Fire the first notification, if any
            showNotificationIfAvailable()
        } catch (e: Exception) {
            logger.debug(defaultLogTag, "InAppController: : InApp notification handling error.", e)
        }
    }


    @WorkerThread
    fun onQueueEvent(
        eventName: String,
        eventProperties: Map<String, Any>,
        userLocation: Location?
    ) {
        val appFieldsWithEventProperties = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)
        appFieldsWithEventProperties.putAll(eventProperties)

        // Returns Triple: (immediateCS, delayedCS, inActionSS)
        val evaluatedInApps = evaluationManager.evaluateOnEvent(
            eventName,
            appFieldsWithEventProperties,
            userLocation
        )

        // Handle immediate CS in-apps
        if (evaluatedInApps.first.isNotEmpty()) {
            addInAppNotificationsToQueue(evaluatedInApps.first)
        }

        // Handle delayed CS in-apps
        if (evaluatedInApps.second.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(evaluatedInApps.second)
        }

        // Handle in-action SS metadata
        if (evaluatedInApps.third.isNotEmpty()) {
            scheduleInActionInApps(evaluatedInApps.third)
        }
    }

    @WorkerThread
    fun onQueueChargedEvent(
        chargeDetails: Map<String, Any>,
        items: List<Map<String, Any>>,
        userLocation: Location?
    ) {
        val appFieldsWithChargedEventProperties =
            JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)
        appFieldsWithChargedEventProperties.putAll(chargeDetails)

        // Returns Triple: (immediateCS, delayedCS, inActionSS)
        val evaluatedInApps = evaluationManager.evaluateOnChargedEvent(
            appFieldsWithChargedEventProperties,
            items,
            userLocation
        )

        // Handle immediate CS in-apps
        if (evaluatedInApps.first.isNotEmpty()) {
            addInAppNotificationsToQueue(evaluatedInApps.first)
        }

        // Handle delayed CS in-apps
        if (evaluatedInApps.second.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(evaluatedInApps.second)
        }

        // Handle in-action SS metadata
        if (evaluatedInApps.third.isNotEmpty()) {
            scheduleInActionInApps(evaluatedInApps.third)
        }
    }

    @WorkerThread
    fun onQueueProfileEvent(
        userAttributeChangedProperties: Map<String, Map<String, Any>>,
        location: Location?
    ) {
        val appFields = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)

        // Returns Triple: (immediateCS, delayedCS, inActionSS)
        val evaluatedInApps = evaluationManager.evaluateOnUserAttributeChange(
            userAttributeChangedProperties,
            location,
            appFields
        )

        // Handle immediate CS in-apps
        if (evaluatedInApps.first.isNotEmpty()) {
            addInAppNotificationsToQueue(evaluatedInApps.first)
        }

        // Handle delayed CS in-apps
        if (evaluatedInApps.second.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(evaluatedInApps.second)
        }

        // Handle in-action SS metadata
        if (evaluatedInApps.third.isNotEmpty()) {
            scheduleInActionInApps(evaluatedInApps.third)
        }
    }

    fun onAppLaunchServerSideInAppsResponse(
        appLaunchServerSideInApps: List<JSONObject>,
        userLocation: Location?
    ) {
        val appLaunchedProperties = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)
        //val appLaunchSsInAppList = Utils.toJSONObjectList(appLaunchServerSideInApps)
        val serverSideInAppsToDisplayImmediate =
            evaluationManager.evaluateOnAppLaunchedServerSide(
                appLaunchServerSideInApps, appLaunchedProperties, userLocation
            )

        if (serverSideInAppsToDisplayImmediate.isNotEmpty()) {
            addInAppNotificationsToQueue(serverSideInAppsToDisplayImmediate)
        }

    }

    fun onAppLaunchServerSideInactionInAppsResponse(
        appLaunchServerSideInactionInApps: List<JSONObject>,
        userLocation: Location?
    ) {
        val appLaunchedProperties = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)
        val serverSideInactionInAppsToDisplay =
            evaluationManager.evaluateOnAppLaunchedServerSide(
                appLaunchServerSideInactionInApps, appLaunchedProperties, userLocation
            )

        if (serverSideInactionInAppsToDisplay.isNotEmpty()) {
            scheduleInActionInApps(serverSideInactionInAppsToDisplay)
        }
    }

    fun onAppLaunchServerSideDelayedInAppsResponse(
        appLaunchServerSideDelayedInApps:  List<JSONObject>,
        userLocation: Location?,
    ) {
        val appLaunchedProperties = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)
        //val appLaunchSsDelayedInAppList = Utils.toJSONObjectList(appLaunchServerSideDelayedInApps)

        val serverSideInAppsToDisplayDelayed =
            evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
                appLaunchServerSideDelayedInApps, appLaunchedProperties, userLocation
            )

        if (serverSideInAppsToDisplayDelayed.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(serverSideInAppsToDisplayDelayed)
        }
    }

    fun showNotificationIfAvailable() {
        if (!config.isAnalyticsOnly) {
            val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
            task.execute("InappController#showNotificationIfAvailable") {
                _showNotificationIfAvailable()
            }
        }
    }

    private fun _showNotificationIfAvailable() {
        try {
            if (!canShowInAppOnCurrentActivity()) {
                logger.verbose("Not showing notification on blacklisted activity")
                return
            }

            if (this.inAppState == InAppState.SUSPENDED) {
                logger.debug(
                    defaultLogTag,
                    "InApp Notifications are set to be suspended, not showing the InApp Notification"
                )
                return
            }

            // see if we have any pending notifications
            if (checkPendingNotifications()) {
                return
            }

            val inapp = inAppQueue.dequeue()
            if (inapp == null) {
                return
            }

            if (this.inAppState != InAppState.DISCARDED) {
                prepareNotificationForDisplay(inapp)
            } else {
                logger.debug(
                    defaultLogTag,
                    "InApp Notifications are set to be discarded, dropping the InApp Notification"
                )
            }
        } catch (t: Throwable) {
            // We won't get here
            logger.verbose(defaultLogTag, "InApp: Couldn't parse JSON array string from prefs", t)
        }
    }

    private fun addInAppNotificationInFrontOfQueue(inApp: JSONObject) {
        if (isNonRegisteredCustomTemplate(inApp)) {
            return
        }
        inAppQueue.insertInFront(inApp)
        showNotificationIfAvailable()
    }

    private fun canShowInAppOnActivity(activity: Activity?): Boolean {
        if (activity == null) {
            return true
        }

        val activityName = activity.getLocalClassName()
        for (blacklistedActivity in inAppExcludedActivityNames) {
            if (activityName.contains(blacklistedActivity)) {
                return false
            }
        }

        return true
    }

    private fun canShowInAppOnCurrentActivity(): Boolean {
        return canShowInAppOnActivity(CoreMetaData.getCurrentActivity())
    }

    private fun displayNotification(inAppNotification: CTInAppNotification) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            executors.mainTask<Unit>().execute("InAppController:displayNotification") {
                displayNotification(inAppNotification)
            }
            return
        }

        if (inAppNotification.isRequestForPushPermission && inAppActionHandler.arePushNotificationsEnabled()) {
            logger.verbose(
                defaultLogTag,
                "Not showing push permission request, permission is already granted"
            )
            inAppActionHandler.notifyPushPermissionListeners()
            showNotificationIfAvailable()
            return
        }

        checkLimitsBeforeShowing(inAppNotification)
        incrementLocalInAppCountInPersistentStore(context, inAppNotification)
    }

    private fun notificationReady(inAppNotification: CTInAppNotification) {
        if (inAppNotification.error != null) {
            logger.debug(
                defaultLogTag,
                "Unable to process inapp notification ${inAppNotification.error}"
            )
            return
        }
        val templateData = inAppNotification.customTemplateData
        val template = templateData?.templateName?.let { templatesManager.getTemplate(it) }

        logger.debug(defaultLogTag, "Notification ready: ${inAppNotification.jsonDescription}")
        if (template != null && !template.isVisual) {
            presentTemplate(inAppNotification)
        } else {
            displayNotification(inAppNotification)
        }
    }

    private fun prepareNotificationForDisplay(jsonObject: JSONObject) {
        logger.debug(defaultLogTag, "Preparing In-App for display: $jsonObject")
        inAppNotificationInflater.inflate(
            jsonObject,
            "InappController#prepareNotificationForDisplay",
            this::notificationReady
        )
    }

    private fun getExcludedActivitiesSet(manifestInfo: ManifestInfo): Set<String> {
        val inAppActivityExclude = mutableSetOf<String>()
        val activitiesString = manifestInfo.excludedActivities
        if (activitiesString != null) {
            val split = activitiesString.split(",")
            for (activityName in split) {
                val trimmed = activityName.trim()
                if (trimmed.isNotBlank()) {
                    inAppActivityExclude.add(trimmed)
                }
            }
        }
        logger.debug(
            defaultLogTag,
            "In-app notifications will not be shown on ${inAppActivityExclude.joinToString()}"
        )
        return inAppActivityExclude
    }

    private fun checkPendingNotifications(): Boolean {
        logger.verbose(defaultLogTag, "checking Pending Notifications")
        synchronized(pendingNotifications) {
            if (pendingNotifications.isEmpty()) {
                return false
            } else {
                val notification = pendingNotifications.removeAt(0)
                checkLimitsBeforeShowing(notification)
                return true
            }
        }
    }

    private fun inAppDidDismiss(inAppNotification: CTInAppNotification) {
        logger.verbose(defaultLogTag, "Running inAppDidDismiss")
        if (currentlyDisplayingInApp != null && (currentlyDisplayingInApp?.campaignId == inAppNotification.campaignId)) {
            currentlyDisplayingInApp = null
            checkPendingNotifications()
        }
    }

    private fun incrementLocalInAppCountInPersistentStore(
        context: Context,
        inAppNotification: CTInAppNotification
    ) {
        if (inAppNotification.isLocalInApp) {
            deviceInfo.incrementLocalInAppCount()//update cache
            val task = executors.ioTask<Unit>()
            task.execute("InAppController#incrementLocalInAppCountInPersistentStore") {
                StorageHelper.putIntImmediate(
                    context,
                    LOCAL_INAPP_COUNT,
                    deviceInfo.localInAppCount
                )// update disk with cache
            }
        }
    }

    private fun checkLimitsBeforeShowing(inAppNotification: CTInAppNotification) {
        val task = executors.ioTask<Boolean>()
        task.addOnSuccessListener { canShow ->
            if (canShow) {
                showInApp(inAppNotification)
            } else {
                showNotificationIfAvailable()
            }
        }

        task.execute("checkLimitsBeforeShowing") {
            val inAppFCManager = controllerManager.inAppFCManager
            if (inAppFCManager != null) {
                val hasInAppFrequencyLimitsMaxedOut: (JSONObject, String) -> Boolean =
                    { inAppJSON, inAppId ->
                        val listOfWhenLimits = InAppResponseAdapter.getListOfWhenLimits(inAppJSON)
                        !evaluationManager.matchWhenLimitsBeforeDisplay(
                            listOfWhenLimits,
                            inAppId
                        )
                    }

                if (!inAppFCManager.canShow(inAppNotification, hasInAppFrequencyLimitsMaxedOut)) {
                    logger.verbose(
                        defaultLogTag,
                        "InApp has been rejected by FC, not showing ${inAppNotification.campaignId}"
                    )
                    return@execute false
                }
            } else {
                logger.verbose(
                    defaultLogTag,
                    "InAppFCManager() is null, not showing ${inAppNotification.campaignId}"
                )
                return@execute false
            }
            return@execute true
        }
    }

    private fun checkBeforeShowApprovalBeforeDisplay(inAppNotification: CTInAppNotification): Boolean {
        val listener = callbackManager.getInAppNotificationListener()

        return if (listener != null) {
            val kvs = if (inAppNotification.customExtras != null) {
                Utils.convertJSONObjectToHashMap(inAppNotification.customExtras)
            } else {
                HashMap<String, Any>()
            }

            listener.beforeShow(kvs)
        } else {
            true
        }
    }

    @MainThread
    private fun showInApp(inAppNotification: CTInAppNotification) {
        val activity = CoreMetaData.getCurrentActivity()
        val goFromListener = checkBeforeShowApprovalBeforeDisplay(inAppNotification)
        if (!goFromListener) {
            logger.verbose(
                defaultLogTag,
                "Application has decided to not show this in-app notification: ${inAppNotification.campaignId}"
            )
            showNotificationIfAvailable()
            return
        }

        if (inAppState == InAppState.DISCARDED) {
            logger.verbose(
                defaultLogTag,
                "InApp Notifications are set to be discarded at main thread check, not showing the InApp Notification"
            )
            return
        }

        if (!CoreMetaData.isAppForeground()) {
            pendingNotifications.add(inAppNotification)
            logger.verbose(defaultLogTag, "Not in foreground, queueing this In App")
            return
        }

        if (currentlyDisplayingInApp != null) {
            pendingNotifications.add(inAppNotification)
            logger.verbose(defaultLogTag, "In App already displaying, queueing this In App")
            return
        }

        if (!canShowInAppOnActivity(activity)) {
            pendingNotifications.add(inAppNotification)
            logger.verbose(
                defaultLogTag,
                "Not showing In App on blacklisted activity, queuing this In App"
            )
            return
        }

        if (inAppState == InAppState.SUSPENDED) {
            pendingNotifications.add(inAppNotification)
            logger.verbose(
                defaultLogTag,
                "InApp Notifications are set to be suspended at main thread check, queuing the In App"
            )
            return
        }

        if ((clock.currentTimeMillis() / 1000) > inAppNotification.timeToLive) {
            logger.debug("InApp has elapsed its time to live, not showing the InApp")
            return
        }

        val isHtmlType = Constants.KEY_CUSTOM_HTML == inAppNotification.type
        if (isHtmlType && !NetworkManager.isNetworkOnline(context)) {
            logger.debug(
                defaultLogTag,
                "Not showing HTML InApp due to no internet. An active internet connection is required to display the HTML InApp"
            )
            showNotificationIfAvailable()
            return
        }

        logger.verbose(defaultLogTag, "Attempting to show next In-App")

        currentlyDisplayingInApp = inAppNotification

        var inAppFragment: CTInAppBaseFragment? = null
        val type = inAppNotification.inAppType
        when (type) {
            CTInAppTypeCoverHTML,
            CTInAppTypeInterstitialHTML,
            CTInAppTypeHalfInterstitialHTML,
            CTInAppTypeCover,
            CTInAppTypeHalfInterstitial,
            CTInAppTypeInterstitial,
            CTInAppTypeAlert,
            CTInAppTypeInterstitialImageOnly,
            CTInAppTypeHalfInterstitialImageOnly,
            CTInAppTypeCoverImageOnly -> {

                try {
                    if (activity == null) {
                        throw IllegalStateException("Current activity reference not found")
                    }
                    logger.debug("Displaying In-App: ${inAppNotification.jsonDescription}")
                    InAppNotificationActivity.launchForInAppNotification(
                        activity,
                        inAppNotification,
                        config
                    )
                } catch (t: Throwable) {
                    logger.verbose(
                        "Please verify the integration of your app. It is not setup to support in-app notifications yet.",
                        t
                    )
                    currentlyDisplayingInApp = null
                    return
                }
            }

            CTInAppTypeFooterHTML -> {
                inAppFragment = CTInAppHtmlFooterFragment()
            }

            CTInAppTypeHeaderHTML -> {
                inAppFragment = CTInAppHtmlHeaderFragment()
            }

            CTInAppTypeFooter -> {
                inAppFragment = CTInAppNativeFooterFragment()
            }

            CTInAppTypeHeader -> {
                inAppFragment = CTInAppNativeHeaderFragment()
            }

            CTInAppTypeCustomCodeTemplate -> {
                presentTemplate(inAppNotification)
                return
            }

            else -> {
                logger.debug(defaultLogTag, "Unknown InApp Type found: $type")
                currentlyDisplayingInApp = null
                return
            }
        }

        if (inAppFragment != null) {
            logger.debug("Displaying In-App: ${inAppNotification.jsonDescription}")
            val showFragmentSuccess = CTInAppBaseFragment.showOnActivity(
                inAppFragment,
                activity,
                inAppNotification,
                config,
                defaultLogTag
            )
            if (!showFragmentSuccess) {
                currentlyDisplayingInApp = null
            }
        }
    }

    private fun presentTemplate(inAppNotification: CTInAppNotification) {
        templatesManager.presentTemplate(
            inAppNotification,
            this,
            FileResourceProvider.getInstance(context, logger)
        )
    }

    private fun filterNonRegisteredCustomTemplates(inAppNotifications: List<JSONObject>): List<JSONObject> {
        return inAppNotifications.filter { jsonObject ->
            !isNonRegisteredCustomTemplate(
                jsonObject
            )
        }
    }

    private fun isNonRegisteredCustomTemplate(inApp: JSONObject): Boolean {
        val templateName = CustomTemplateInAppData.createFromJson(inApp)?.templateName
        val isNonRegistered =
            templateName != null && !templatesManager.isTemplateRegistered(templateName)

        if (isNonRegistered) {
            logger.info(
                "CustomTemplates",
                "Template with name \"$templateName\" is not registered and cannot be presented"
            )
        }

        return isNonRegistered
    }

    private fun triggerCustomTemplateAction(
        notification: CTInAppNotification,
        templateInAppData: CustomTemplateInAppData?
    ) {
        val templateName = templateInAppData?.templateName
        if (templateName != null) {
            val template = templatesManager.getTemplate(templateName)
            if (template != null) {
                // When a custom in-app template is triggered as an action we need to present it.
                // Since all related methods operate with either CTInAppNotification or its json representation, here
                // we create a new notification from the one that initiated the triggering and add the action as its
                // template data.
                val actionTemplateData = templateInAppData.copy()
                actionTemplateData.isAction = true
                val notificationFromAction =
                    notification.createNotificationForAction(actionTemplateData)
                if (notificationFromAction == null) {
                    logger.debug("Failed to present custom template with name: $templateName")
                    return
                }
                if (template.isVisual) {
                    addInAppNotificationInFrontOfQueue(notificationFromAction.jsonDescription)
                } else {
                    prepareNotificationForDisplay(notificationFromAction.jsonDescription)
                }
            } else {
                logger.debug("Cannot present non-registered template with name: $templateName")
            }
        } else {
            logger.debug("Cannot present template without name.")
        }
    }

    /**
     * Fetch in-action in-app content from backend after inactionDuration expires
     * Sends wzrk_fetch event with t=6 and target ID
     *
     * @param targetId The campaign ID (ti) to fetch content for
     */
    @WorkerThread
    private fun fetchInActionInApp(targetId: Long) {
        logger.verbose(
            defaultLogTag,
            "Fetching in-action in-app content for targetId: $targetId"
        )

        val fetchEvent = createInActionFetchRequest(targetId)
        analyticsManager.sendFetchEvent(fetchEvent)
    }

    private fun createInActionFetchRequest(targetId: Long): JSONObject {
        return JSONObject().apply {
            put(Constants.KEY_EVT_NAME, Constants.WZRK_FETCH)
            put(Constants.KEY_EVT_DATA, JSONObject().apply {
                put(Constants.KEY_T, Constants.FETCH_TYPE_IN_ACTION_IN_APPS) // t=6
                put("tgtId", targetId)
            })
        }
    }
}
