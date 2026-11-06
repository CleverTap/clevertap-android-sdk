package com.clevertap.android.sdk.inapp


import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
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
import com.clevertap.android.sdk.inapp.CTInAppType.CTInAppTypePIP
import com.clevertap.android.sdk.inapp.CTLocalInApp.Companion.FALLBACK_TO_NOTIFICATION_SETTINGS
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter
import com.clevertap.android.sdk.inapp.delay.DelayedInAppResult
import com.clevertap.android.sdk.inapp.delay.InActionResult
import com.clevertap.android.sdk.inapp.delay.InAppScheduler
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.evaluation.EventAdapter
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlFooterFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlHeaderFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeFooterFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeHeaderFragment
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPManager
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.store.db.DisplayedInAppStore
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.network.ContentFetchItem
import com.clevertap.android.sdk.network.NetworkMonitor
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.variables.JsonUtil
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.TimeUnit

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
    private val clock: Clock,
    private val networkMonitor: NetworkMonitor,
    private val pipManager: PIPManager,
    private val validationResultStack: ValidationResultStack,
    private val displayedInAppStoreProvider: () -> DisplayedInAppStore? = { null },
) : InAppListener, PIPShowFailureHandler, ChangeUserCallback {

    /**
     * Gets the result of [restoreDisplayedInApp] on the main thread: the restored In-App, or null
     * when it cannot be restored. Return true when the In-App is shown, false to drop it.
     */
    fun interface RestoredInAppListener {
        @MainThread
        fun onRestoreResult(inApp: CTInAppNotification?): Boolean
    }

    // Strong reference: InAppNotificationInflater holds its listener weakly. Only the latest restore
    // is kept: a new one starts only from a new In-App screen, and the old screen is gone by then.
    @Volatile
    private var restoreReadyListener: InAppNotificationInflater.InAppNotificationReadyListener? = null

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

        // UX bound: show the /a1 winner by now. Hard 3s constant; correctness comes
        // from the closed-suppressing phase, not this timeout.
        private const val APP_LAUNCH_ARBITRATION_TIMEOUT_MS = 3_000L

        // Hard backstop: the window self-tears-down by this bound even if completion is never
        // delivered. Must sit beyond the content-fetch request's own worst case — connect (10s) +
        // read (10s) = 20s per UrlConnectionHttpClient — so a slow-but-successful /content always
        // fires completion and tears the window down normally before this fires. Tearing down
        // earlier would reset to passthrough and let a late /content show a second app-launch in-app.
        private const val APP_LAUNCH_ARBITRATION_MAX_LIFETIME_MS = 25_000L

        // The saved in-app is kept as {"inApp": <in-app JSON as received>, "ttl": <time to live>}, so
        // the in-app JSON, and the wzrk_ fields its events copy from it, stay unchanged.
        private const val DISPLAYED_INAPP_JSON = "inApp"
        private const val DISPLAYED_INAPP_TTL = "ttl"

        private val pendingNotifications =
            Collections.synchronizedList(ArrayList<CTInAppNotification>())

        @VisibleForTesting
        @Volatile
        internal var currentlyDisplayingInApp: CTInAppNotification? = null
            private set

        /**
         * Returns the in-app on screen when its campaign id matches [inAppId], else null.
         *
         * In-app screens and fragments receive only this id instead of the whole
         * [CTInAppNotification], so large HTML is never sent through Binder
         * (TransactionTooLargeException, SDK-6299). It is null after the app process was killed.
         */
        @JvmStatic
        fun getDisplayingInApp(inAppId: String?): CTInAppNotification? {
            if (inAppId == null) return null
            return currentlyDisplayingInApp?.takeIf { it.campaignId == inAppId }
        }

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
        if (clientSideInAppsToDisplay.immediateInApps.isNotEmpty()) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay.immediateInApps)
        }
        if (clientSideInAppsToDisplay.delayedInApps.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(clientSideInAppsToDisplay.delayedInApps)
        }
    }

    private val logger = config.logger
    private val defaultLogTag = config.accountId

    // Holds the /a1 app-launch winner until the /content winner is known, then shows at most one.
    private val appLaunchArbitrator = AppLaunchInAppArbitrator(
        logger = logger,
        logTag = defaultLogTag,
        timeoutMs = APP_LAUNCH_ARBITRATION_TIMEOUT_MS,
        hardTeardownMs = APP_LAUNCH_ARBITRATION_MAX_LIFETIME_MS,
        sortByPriority = evaluationManager::sortByPriority,
        showWinner = { winner -> addInAppNotificationsToQueue(listOf(winner)) }
    )

    @Volatile
    private var inAppState = InAppState.RESUMED

    private val inAppExcludedActivityNames = getExcludedActivitiesSet(manifestInfo)

    // Opt-in (default off) for rendering custom-html header/footer in-apps via a WindowManager
    // overlay when the host Activity is not a FragmentActivity. Enabled by the gaming wrapper SDKs.
    private val fragmentlessInAppBannersEnabled = manifestInfo.isFragmentlessInAppBannersEnabled

    /**
     * Schedule multiple delayed in-apps for display after their respective delays
     */
    @WorkerThread
    fun scheduleDelayedInAppsForAllModes(
        delayedInApps: List<JSONObject>,
        shouldUpdateTTL: Boolean = true
    ) {
        logger.verbose(
            config.accountId,
            "[InAppController]: Scheduling ${delayedInApps.size} delayed in-apps"
        )

        inAppDelayManager.schedule(delayedInApps) { result ->
            when (result) {
                is DelayedInAppResult.Success -> {
                    logger.verbose(
                        config.accountId,
                        "[InAppController]: Successfully retrieved delayed in-app ${result.inAppId}"
                    )

                    val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
                    task.execute("InAppController#executeDelayedInAppCallback-${result.inAppId}") {

                        if (shouldUpdateTTL) {
                            logger.verbose(config.accountId,"updating ttl for delayed in-apps")
                            //result.inApp.put(Constants.WZRK_TIME_TO_LIVE_OFFSET,60L)// 60 sec ttl for testing
                            //Calculate fresh TTL after delay completes
                            evaluationManager.updateTTL(result.inApp)
                        }

                        // Add to display queue30
                        addInAppNotificationInFrontOfQueue(result.inApp)
                    }
                }

                is DelayedInAppResult.Error -> {
                    logger.verbose(
                        config.accountId,
                        "[InAppController]: Error for delayed in-app ${result.inAppId}: ${result.reason}",
                        result.throwable
                    )
                }

                is DelayedInAppResult.Discarded -> {
                    logger.verbose(
                        config.accountId,
                        "[InAppController]: in-app discarded ${result.id}: ${result.reason}"
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
                        "[InAppController]In-action: in-app discarded ${result.targetId}: ${result.reason}"
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

        // Extract and add deep link for attribution
        val deepLink = extractDeepLink(action)
        if (!deepLink.isNullOrEmpty()) {
            data.putString(Constants.DEEP_LINK_KEY, deepLink)
        }

        // Media preload failures from the bundled advanced-builder template (delivered as HTML content)
        // arrive as a synthetic close carrying a reserved wzrk_c2a. Report them as a structured wzrk_error
        // and DO NOT raise a click or viewed event. Scoped to HTML in-apps only: aspect ratio isn't a
        // reliable signal, and gating on the HTML type excludes native templates so a native CTA's
        // wzrk_c2a is never misread as a media error.
        if (inAppNotification.isHtml() && reportMediaErrorIfAny(callToAction)) {
            return data
        }

        val type = action.type
        // Enrich the clicked event with the action descriptors (wzrk_action/wzrk_data) at this single
        // choke point, derived from the action being triggered.
        addActionDescriptors(data, action, type)

        // send clicked event
        if (!inAppNotification.isLocalInApp) {
            analyticsManager.pushInAppNotificationStateEvent(true, inAppNotification, data)
        }

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

    /**
     * Maps a media-error synthetic-close [callToAction] to a structured [ValidationResult] and pushes it
     * onto the validation stack, so it rides along on the next queued event as the top-level `wzrk_error`.
     * No event is raised here. Returns true if the callToAction was a media-error descriptor.
     */
    private fun reportMediaErrorIfAny(callToAction: String): Boolean {
        val validationResult = when (callToAction) {
            Constants.INAPP_CTA_IMAGE_ERROR_DISMISS -> ValidationResult(
                Constants.INAPP_IMAGE_LOAD_FAILED_ERROR_CODE,
                Constants.INAPP_IMAGE_LOAD_FAILED_ERROR_MSG
            )

            Constants.INAPP_CTA_VIDEO_ERROR_DISMISS -> ValidationResult(
                Constants.INAPP_VIDEO_LOAD_FAILED_ERROR_CODE,
                Constants.INAPP_VIDEO_LOAD_FAILED_ERROR_MSG
            )

            else -> return false
        }
        logger.debug("InApp media failed to load, reporting wzrk_error ${validationResult.errorCode}")
        validationResultStack.pushValidationResult(validationResult)
        return true
    }

    /**
     * Adds `wzrk_action` (the action type) and `wzrk_data` (the action payload) to the clicked-event
     * extras. `wzrk_data` for a key-values action is a nested object.
     */
    private fun addActionDescriptors(data: Bundle, action: CTInAppAction, type: InAppActionType?) {
        if (type == null) {
            return
        }
        data.putString(Constants.KEY_WZRK_ACTION, type.toString())
        when (type) {
            InAppActionType.OPEN_URL ->
                action.actionUrl?.takeIf { it.isNotEmpty() }
                    ?.let { data.putString(Constants.KEY_WZRK_DATA, it) }

            InAppActionType.CLOSE ->
                data.putString(Constants.KEY_WZRK_DATA, Constants.INAPP_WZRK_DATA_CLOSE)

            InAppActionType.CUSTOM_CODE ->
                action.customTemplateInAppData?.templateName?.takeIf { it.isNotEmpty() }
                    ?.let { data.putString(Constants.KEY_WZRK_DATA, it) }

            InAppActionType.KEY_VALUES -> {
                val keyValues = action.keyValues
                if (!keyValues.isNullOrEmpty()) {
                    // Nested payload carried as a Serializable map; analytics emits it as a nested JSON object.
                    data.putSerializable(Constants.KEY_WZRK_DATA, HashMap(keyValues))
                }
            }

            else -> {
                // no wzrk_data payload for other action types
            }
        }
    }

    override fun inAppNotificationDidClick(
        inAppNotification: CTInAppNotification,
        button: CTInAppNotificationButton,
        buttonIndex: Int,
        activityContext: Context?
    ): Bundle? {
        val action = button.action
        if (action == null) {
            return null
        }
        // Tag the clicked element for CTA buttons that don't route through the fragment's
        // handleButtonClickAtIndex (e.g. alert-template buttons). The caller passes the exact clicked
        // index (buttons are built from a JSON array and CTInAppNotificationButton.equals is value-based,
        // so indexOf would resolve a duplicate CTA payload to the wrong slot). 1-based, matching iOS.
        val additionalData = if (buttonIndex >= 0) {
            val elementId = if (inAppNotification.isImageOnlyInApp()) {
                Constants.INAPP_ELEMENT_ID_IMAGE
            } else {
                Constants.INAPP_ELEMENT_ID_BUTTON_PREFIX + (buttonIndex + 1)
            }
            Bundle().apply { putString(Constants.KEY_WZRK_ELEMENT_ID, elementId) }
        } else {
            null
        }
        return inAppNotificationActionTriggered(
            inAppNotification,
            action,
            button.text,
            additionalData,
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
        if (inAppNotification.isRestored) {
            // Counted, tracked and reported to the listener when it was first shown.
            logger.verbose(
                defaultLogTag,
                "In-app ${inAppNotification.campaignId} restored after process kill, not counting it again"
            )
            return
        }
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
        if (inApp.inAppType == CTInAppTypePIP) {
            pipManager.dismiss()
        } else {
            inAppDisplayListener?.get()?.hideInApp()
        }
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

    /**
     * Dismisses the currently visible PIP in-app, if any. Safe to call from any thread.
     * No-op when no PIP is showing — other in-app types are never affected.
     * Raises the API-dismiss "Notification Clicked" (wzrk_element_id=dismissApi)
     */
    fun dismissPipInApp() {
        logger.verbose(defaultLogTag, "dismissPipInApp() called by app")
        pipManager.dismissFromApi()
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

        // Returns (immediateCS, delayedCS, inActionSS)
        val evaluatedInApps = evaluationManager.evaluateOnEvent(
            eventName,
            appFieldsWithEventProperties,
            userLocation
        )

        // Handle immediate CS in-apps
        if (evaluatedInApps.immediateClientSideInApps.isNotEmpty()) {
            addInAppNotificationsToQueue(evaluatedInApps.immediateClientSideInApps)
        }

        // Handle delayed CS in-apps
        if (evaluatedInApps.delayedClientSideInApps.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(evaluatedInApps.delayedClientSideInApps)
        }

        // Handle in-action SS metadata
        if (evaluatedInApps.serverSideInActionInApps.isNotEmpty()) {
            scheduleInActionInApps(evaluatedInApps.serverSideInActionInApps)
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

        // Returns (immediateCS, delayedCS, inActionSS)
        val evaluatedInApps = evaluationManager.evaluateOnChargedEvent(
            appFieldsWithChargedEventProperties,
            items,
            userLocation
        )

        // Handle immediate CS in-apps
        if (evaluatedInApps.immediateClientSideInApps.isNotEmpty()) {
            addInAppNotificationsToQueue(evaluatedInApps.immediateClientSideInApps)
        }

        // Handle delayed CS in-apps
        if (evaluatedInApps.delayedClientSideInApps.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(evaluatedInApps.delayedClientSideInApps)
        }

        // Handle in-action SS metadata
        if (evaluatedInApps.serverSideInActionInApps.isNotEmpty()) {
            scheduleInActionInApps(evaluatedInApps.serverSideInActionInApps)
        }
    }

    @WorkerThread
    fun onQueueProfileEvent(
        userAttributeChangedProperties: Map<String, Map<String, Any?>>,
        location: Location?
    ) {
        val appFields = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)

        // Returns (immediateCS, delayedCS, inActionSS)
        val evaluatedInApps = evaluationManager.evaluateOnUserAttributeChange(
            userAttributeChangedProperties,
            location,
            appFields
        )

        // Handle immediate CS in-apps
        if (evaluatedInApps.immediateClientSideInApps.isNotEmpty()) {
            addInAppNotificationsToQueue(evaluatedInApps.immediateClientSideInApps)
        }

        // Handle delayed CS in-apps
        if (evaluatedInApps.delayedClientSideInApps.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(evaluatedInApps.delayedClientSideInApps)
        }

        // Handle in-action SS metadata
        if (evaluatedInApps.serverSideInActionInApps.isNotEmpty()) {
            scheduleInActionInApps(evaluatedInApps.serverSideInActionInApps)
        }
    }

    fun onAppLaunchServerSideInAppsResponse(
        appLaunchServerSideInApps: List<JSONObject>,
        contentFetchItems: List<ContentFetchItem>,
        userLocation: Location?
    ) {
        val appLaunchedProperties = JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields)
        val serverSideInAppsToDisplayImmediate =
            evaluationManager.evaluateOnAppLaunchedServerSide(
                appLaunchServerSideInApps, appLaunchedProperties, userLocation
            )

        // Option 2 fast path: show the /a1 winner now when waiting can't change the outcome.
        if (tryAppLaunchFastPath(serverSideInAppsToDisplayImmediate, contentFetchItems, appLaunchedProperties, userLocation)) {
            return
        }

        // Option 1: no window -> show now; OPEN -> buffer; CLOSED -> drop.
        val toShow = appLaunchArbitrator.routeWinners(serverSideInAppsToDisplayImmediate)
        if (toShow.isNotEmpty()) {
            addInAppNotificationsToQueue(toShow)
        }
    }

    /**
     * Option 2 prediction. Returns true (and shows [a1Winner] now, closing the window) when waiting
     * for `/content` cannot change the outcome — no synthetic candidate qualifies, or the `/a1`
     * winner tops the merged sort. Returns false to fall back to Option 1 (wait). The synthetics are
     * derived on the spot from this same `/a1` response's [contentFetchItems]; non-empty synthetics
     * imply a window was opened (both key off the same directive), so Option 2 is dormant unless every
     * app-launch content_fetch item carries `priority`.
     */
    private fun tryAppLaunchFastPath(
        a1Winner: List<JSONObject>,
        contentFetchItems: List<ContentFetchItem>,
        appLaunchedProperties: Map<String, Any>,
        userLocation: Location?
    ): Boolean {
        val synthetics = appLaunchSynthetics(contentFetchItems)
        if (synthetics.isEmpty()) {
            return false // Option 1 (no priority / no content fetch): wait or show now via routeWinners
        }
        if (a1Winner.isEmpty()) {
            return false // nothing to show now; wait for content
        }

        val event = EventAdapter(
            eventName = Constants.APP_LAUNCHED_EVENT,
            eventProperties = appLaunchedProperties,
            userLocation = userLocation
        )
        val eligibleContent = evaluationManager.evaluateDryRun(event, synthetics)

        val a1WinnerTops = if (eligibleContent.isEmpty()) {
            true // no content candidate can qualify -> waiting cannot change the outcome
        } else {
            // Merge the /a1 winner with the predicted content and ask, by reference identity, whether
            // the top came from the /a1 set. a1Winner and eligibleContent are distinct instances and
            // sortByPriority preserves references, so this needs no marker on the JSON.
            val top = evaluationManager.sortByPriority(a1Winner + eligibleContent).firstOrNull()
            top != null && a1Winner.any { it === top }
        }
        if (!a1WinnerTops) {
            return false // a content candidate could win -> fall back to Option 1
        }

        appLaunchArbitrator.closeForFastPath(a1Winner.first())
        addInAppNotificationsToQueue(a1Winner)
        return true
    }

    /**
     * Opens an app-launch arbitration window when this `/a1` response carries a `content_fetch` that
     * can produce an app-launch in-app, so the winner is held and merged with the `/content` winner
     * instead of shown twice. Called on `/a1` only.
     */
    fun openAppLaunchArbitrationWindowIfNeeded(contentFetchItems: List<ContentFetchItem>) {
        if (contentFetchItems.any { it.isAppLaunchInApp() }) {
            appLaunchArbitrator.openWindow()
        }
    }

    // Synthetic selection-rule candidates for the app-launch content_fetch items on a `/a1` response,
    // used by the Option 2 fast path. All-or-nothing: empty unless every app-launch item carries
    // `priority` (otherwise fall back to Option 1 / wait).
    private fun appLaunchSynthetics(contentFetchItems: List<ContentFetchItem>): List<JSONObject> {
        val appLaunchItems = contentFetchItems.filter { it.isAppLaunchInApp() }
        if (appLaunchItems.isEmpty()) {
            return emptyList()
        }
        val synthetics = appLaunchItems.map { it.syntheticInAppPayload() }
        return if (synthetics.all { it != null }) synthetics.filterNotNull() else emptyList()
    }

    /**
     * Completion signal for the content-fetch batch (fired exactly once, on success/error/timeout/
     * user-switch). Closes the window, shows the merged winner if not already shown, and tears down.
     */
    fun onAppLaunchContentFetchComplete() {
        appLaunchArbitrator.onContentFetchComplete()
    }

    /**
     * Discards any open app-launch arbitration window WITHOUT showing its buffered winner. Called on
     * user switch so the previous user's held app-launch in-app is never shown to the new user.
     */
    fun abandonAppLaunchArbitration() {
        appLaunchArbitrator.abandon()
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

        val serverSideInAppsToDisplayDelayed =
            evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
                appLaunchServerSideDelayedInApps, appLaunchedProperties, userLocation
            )

        if (serverSideInAppsToDisplayDelayed.isNotEmpty()) {
            scheduleDelayedInAppsForAllModes(serverSideInAppsToDisplayDelayed, false)
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

    /**
     * Shows again the full-screen In-App [inAppId] that was on screen when the app process was
     * killed. Android recreates the In-App screen with only the id, and memory is empty, so the
     * In-App is read from the database on a background thread.
     *
     * The result goes to [listener] on the main thread. It is null when nothing is stored for
     * [inAppId], the In-App's own time to live has passed, In-Apps are suspended or discarded,
     * an HTML In-App has no internet, or another In-App is on screen.
     */
    @MainThread
    fun restoreDisplayedInApp(inAppId: String, listener: RestoredInAppListener) {
        // The listener is the In-App screen. It is held weakly, so a screen that is closed before
        // the result comes back (timeout, or the inflate never calls back) is not kept alive.
        val listenerRef = WeakReference(listener)
        // On the in-app thread, like save and the delete on dismiss, so the saved in-app is read,
        // saved and deleted in order.
        val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppController#restoreDisplayedInApp") {
            val inAppJson = readDisplayedInAppForRestore(inAppId)
            if (inAppJson == null) {
                executors.mainTask<Unit>().execute("InAppController#restoreDisplayedInApp") {
                    listenerRef.get()?.onRestoreResult(null)
                }
                return@execute
            }
            val readyListener = object : InAppNotificationInflater.InAppNotificationReadyListener {
                override fun onNotificationReady(notification: CTInAppNotification) {
                    if (restoreReadyListener === this) {
                        restoreReadyListener = null
                    }
                    val screen = listenerRef.get()
                    if (screen == null) {
                        logger.verbose(defaultLogTag, "Not restoring in-app $inAppId: the in-app screen is gone")
                        return
                    }
                    onRestoredInAppReady(notification, screen)
                }
            }
            restoreReadyListener = readyListener
            inAppNotificationInflater.inflate(inAppJson, "InAppController#restoreDisplayedInApp", readyListener)
        }
    }

    @WorkerThread
    private fun readDisplayedInAppForRestore(inAppId: String): JSONObject? {
        val store = displayedInAppStoreProvider()
        val displayedInApp = store?.get(inAppId)
        val inAppJson = displayedInApp?.optJSONObject(DISPLAYED_INAPP_JSON)
        val timeToLive = displayedInApp?.optLong(DISPLAYED_INAPP_TTL) ?: 0L
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(clock.currentTimeMillis())
        return when {
            store == null -> {
                logger.verbose(defaultLogTag, "Cannot restore in-app $inAppId: there is no displayed in-app store")
                null
            }

            inAppJson == null -> {
                logger.verbose(defaultLogTag, "Cannot restore in-app $inAppId: it is not stored or cannot be read")
                // Removes a row that is there but cannot be read (too large or cannot be decrypted).
                removeSavedDisplayedInAppNow(inAppId)
                null
            }

            // Same time to live check as showInApp.
            nowSeconds > timeToLive -> {
                logger.verbose(defaultLogTag, "Not restoring in-app $inAppId: it has elapsed its time to live")
                removeSavedDisplayedInAppNow(inAppId)
                null
            }

            else -> inAppJson
        }
    }

    @MainThread
    private fun onRestoredInAppReady(inApp: CTInAppNotification, listener: RestoredInAppListener) {
        // Same rules as showInApp: respect suspendInApps/discardInApps, and HTML needs internet.
        // In those cases the saved in-app is removed: showInApp discards it when in-apps are discarded,
        // and a suspended in-app is not kept for resumeInApps, to keep the restore simple.
        val inAppsStopped = inAppState != InAppState.RESUMED
        val htmlWithoutInternet = Constants.KEY_CUSTOM_HTML == inApp.type && !networkMonitor.isNetworkOnline()
        val pushPermissionGranted = inApp.isRequestForPushPermission && inAppActionHandler.arePushNotificationsEnabled()
        val notRestoredReason = when {
            inApp.error != null -> inApp.error
            inAppsStopped -> "InAppState is $inAppState"
            htmlWithoutInternet -> "an active internet connection is required to display the HTML InApp"
            pushPermissionGranted -> "push permission is already granted"
            currentlyDisplayingInApp != null -> "in-app ${currentlyDisplayingInApp?.campaignId} is on screen"
            else -> null
        }
        if (notRestoredReason != null) {
            logger.verbose(defaultLogTag, "Not restoring in-app ${inApp.campaignId}: $notRestoredReason")
            if (inAppsStopped || htmlWithoutInternet || pushPermissionGranted) {
                removeSavedDisplayedInApp(inApp.campaignId)
            }
            listener.onRestoreResult(null)
            return
        }

        inApp.isRestored = true
        // Set before the listener shows it: the In-App screen and fragment read it from here.
        currentlyDisplayingInApp = inApp
        if (listener.onRestoreResult(inApp)) {
            logger.verbose(defaultLogTag, "Restored in-app ${inApp.campaignId} after process kill")
        } else {
            // The In-App screen is closing or being recreated (e.g. rotation during the restore).
            // Free the slot right away on the main thread, so a new screen can restore it. The saved
            // in-app is kept: the user did not close it. No dismiss callbacks.
            currentlyDisplayingInApp = null
            val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
            task.execute("InAppController#onRestoredInAppReady") {
                checkPendingNotifications()
                _showNotificationIfAvailable()
            }
        }
    }

    /**
     * Keeps the full-screen In-App on screen in the database, so [restoreDisplayedInApp] can show it
     * again after the app process is killed. Only one In-App is kept. Its time to live is stored
     * beside the In-App JSON: parsing the JSON again could give it a new default one.
     *
     * Runs on the in-app thread, like dismiss, so a save and the delete on dismiss run in order.
     */
    private fun saveDisplayedInApp(inAppNotification: CTInAppNotification) {
        val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppController#saveDisplayedInApp") {
            if (currentlyDisplayingInApp !== inAppNotification) {
                return@execute // already dismissed
            }
            val inAppId = inAppNotification.campaignId
            if (inAppId.isNullOrEmpty()) {
                return@execute // the In-App screen finds in-apps by id, so this one cannot be restored
            }
            val store = displayedInAppStoreProvider() ?: return@execute
            val displayedInApp = JSONObject()
                .put(DISPLAYED_INAPP_JSON, inAppNotification.jsonDescription)
                .put(DISPLAYED_INAPP_TTL, inAppNotification.timeToLive)
            val saved = store.save(inAppId, displayedInApp)
            logger.verbose(
                defaultLogTag,
                "Saved in-app $inAppId to show it again after a process kill: $saved"
            )
        }
    }

    private fun removeSavedDisplayedInApp(inAppId: String?) {
        inAppId ?: return
        val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppController#removeSavedDisplayedInApp") {
            removeSavedDisplayedInAppNow(inAppId)
        }
    }

    /**
     * The saved In-App belongs to the previous user, so it is not shown again after a process kill.
     * Runs on the in-app thread, after any save already queued for it.
     */
    override fun onChangeUser(deviceId: String, accountId: String) {
        val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppController#onChangeUser") {
            val cleared = displayedInAppStoreProvider()?.clear() ?: return@execute
            logger.verbose(defaultLogTag, "Removed the saved in-app on user change: $cleared")
        }
    }

    @WorkerThread
    private fun removeSavedDisplayedInAppNow(inAppId: String) {
        val removed = displayedInAppStoreProvider()?.remove(inAppId) ?: return
        logger.verbose(defaultLogTag, "Removed saved in-app $inAppId: $removed")
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
            // Nothing is on screen now, so there is nothing to restore after a process kill.
            // Called on the in-app background thread, so the database is used directly.
            inAppNotification.campaignId?.let { removeSavedDisplayedInAppNow(it) }
            checkPendingNotifications()
        }
    }

    override fun onPIPShowFailed(inAppNotification: CTInAppNotification, mediaType: PIPMediaType) {
        logger.verbose(defaultLogTag, "PIP failed to show: ${inAppNotification.campaignId}")
        reportPipMediaError(mediaType)
        // Same threading pattern as inAppNotificationDidDismiss: clear the lock and advance
        // the queue atomically on the async in-app thread. This avoids a race where
        // currentlyDisplayingInApp is cleared on main but _showNotificationIfAvailable
        // hasn't run yet on the async thread.
        val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppController#onPIPShowFailed") {
            inAppDidDismiss(inAppNotification)
            _showNotificationIfAvailable()
        }
    }

    /**
     * Reports a PIP all-media-failed as a structured [ValidationResult] on the validation stack,
     * riding out as top-level `wzrk_error` on the next queued event — mirroring how the bundled
     * advanced-builder HTML template reports its media failures. No click or viewed event is
     * raised: the PIP never showed. One distinct code per PIP media type.
     */
    private fun reportPipMediaError(mediaType: PIPMediaType) {
        val validationResult = when (mediaType) {
            PIPMediaType.IMAGE -> ValidationResult(
                Constants.INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_CODE,
                Constants.INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_MSG
            )

            PIPMediaType.VIDEO -> ValidationResult(
                Constants.INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_CODE,
                Constants.INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_MSG
            )

            PIPMediaType.GIF -> ValidationResult(
                Constants.INAPP_PIP_GIF_LOAD_FAILED_ERROR_CODE,
                Constants.INAPP_PIP_GIF_LOAD_FAILED_ERROR_MSG
            )
        }
        logger.debug("PIP media failed to load, reporting wzrk_error ${validationResult.errorCode}")
        validationResultStack.pushValidationResult(validationResult)
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

        val activity = CoreMetaData.getCurrentActivity()
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
        if (isHtmlType && !networkMonitor.isNetworkOnline()) {
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
                    saveDisplayedInApp(inAppNotification)
                } catch (t: Throwable) {
                    logger.verbose(
                        "Please verify the integration of your app. It is not setup to support in-app notifications yet.",
                        t
                    )
                    currentlyDisplayingInApp = null
                }
                return
            }

            CTInAppTypeFooterHTML -> {
                if (activity is FragmentActivity) {
                    inAppFragment = CTInAppHtmlFooterFragment()
                } else if (activity != null && fragmentlessInAppBannersEnabled) {
                    // Non-FragmentActivity host (e.g. game engines) with the opt-in enabled:
                    // fall back to the WindowManager overlay.
                    showHtmlBannerOverlay(inAppNotification, activity)
                    return
                }
            }

            CTInAppTypeHeaderHTML -> {
                if (activity is FragmentActivity) {
                    inAppFragment = CTInAppHtmlHeaderFragment()
                } else if (activity != null && fragmentlessInAppBannersEnabled) {
                    // Non-FragmentActivity host (e.g. game engines) with the opt-in enabled:
                    // fall back to the WindowManager overlay.
                    showHtmlBannerOverlay(inAppNotification, activity)
                    return
                }
            }

            CTInAppTypeFooter -> {
                inAppFragment = CTInAppNativeFooterFragment()
            }

            CTInAppTypeHeader -> {
                inAppFragment = CTInAppNativeHeaderFragment()
            }

            CTInAppTypePIP -> {
                try {
                    if (activity == null) {
                        throw IllegalStateException("Current activity reference not found for PIP")
                    }
                    val bridge = PIPInAppCallbacksBridge(inAppNotification, this, this, logger)
                    val pipConfig = PIPConfigFactory.create(inAppNotification, bridge, logger)
                    if (pipConfig == null) {
                        logger.debug(defaultLogTag, "Failed to create PIPConfig, skipping")
                        currentlyDisplayingInApp = null
                        showNotificationIfAvailable()
                        return
                    }
                    logger.debug("Displaying PIP In-App: ${inAppNotification.campaignId}")
                    pipManager.show(activity, pipConfig, null)
                } catch (t: Throwable) {
                    logger.verbose("Failed to show PIP in-app", t)
                    currentlyDisplayingInApp = null
                    showNotificationIfAvailable()
                }
                return
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

        if (inAppFragment == null || activity == null) {
            logger.debug("Unable to display In-App: Activity/Fragment is null")
            currentlyDisplayingInApp = null
            return
        }

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

    private fun showHtmlBannerOverlay(inAppNotification: CTInAppNotification, activity: Activity) {
        // Defensive guard: the overlay renders an HTML WebView, so it only supports the custom-html
        // header/footer types. Today the when-dispatch only routes those two types here, but this
        // keeps the invariant explicit and safe if the dispatch is ever refactored.
        if (!CTInAppHtmlBannerOverlay.canDisplay(inAppNotification.inAppType)) {
            logger.debug(
                "Overlay banner not supported for type ${inAppNotification.inAppType}; skipping"
            )
            currentlyDisplayingInApp = null
            return
        }
        logger.debug("Displaying In-App as overlay: ${inAppNotification.jsonDescription}")
        val bridge = CTHtmlBannerCallbacksBridge(inAppNotification, config, this)
        val overlay = CTInAppHtmlBannerOverlay(inAppNotification, config, bridge, activity)
        bridge.overlay = overlay
        // Allow the overlay to be hidden externally (discardInApps/suspend), like the fragment path.
        registerInAppDisplayListener(bridge)
        overlay.show()
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

    @WorkerThread
    fun cancelAllScheduledInApps() {
        try {
            runBlocking {
                inAppDelayManager.cancelAllScheduling()
                logger.verbose(defaultLogTag, "[InAppController]: Cancelled all delayed in-apps")
                inAppInActionManager.cancelAllScheduling()
                logger.verbose(defaultLogTag, "[InAppController]: Cancelled all in-action in-apps")
            }
        } catch (e: Exception) {
            logger.verbose(defaultLogTag, "[InAppController]: Error cancelling scheduled in-apps", e)
        }
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

    /**
     * Extracts the deep link URL from an InApp action for wzrk_dl attribution.
     *
     * The deep link is taken from the action URL when the action type is OPEN_URL.
     * This covers button clicks in both native and HTML InApps (via the JS bridge).
     *
     * @param action The action being triggered
     * @return The deep link URL, or null if the action has no URL
     */
    private fun extractDeepLink(action: CTInAppAction): String? {
        if (action.type == InAppActionType.OPEN_URL) {
            val actionUrl = action.actionUrl
            if (!actionUrl.isNullOrEmpty()) {
                return actionUrl
            }
        }
        return null
    }
}
