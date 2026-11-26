package com.clevertap.android.sdk

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.location.Location
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Utils.getSCDomain
import com.clevertap.android.sdk.Utils.runOnUiThread
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.DisplayUnitCache
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController
import com.clevertap.android.sdk.inapp.CTLocalInApp
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter
import com.clevertap.android.sdk.inapp.customtemplates.JsonTemplatesProducer
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter
import com.clevertap.android.sdk.inapp.customtemplates.TemplateProducer
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory
import com.clevertap.android.sdk.inbox.CTInboxActivity
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.interfaces.NotificationHandler
import com.clevertap.android.sdk.interfaces.NotificationRenderedListener
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener
import com.clevertap.android.sdk.interfaces.SCDomainListener
import com.clevertap.android.sdk.network.fetch.FetchTrigger
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.product_config.CTProductConfigListener
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.CoreNotificationRenderer
import com.clevertap.android.sdk.pushnotification.ICleverTapLiveUpdateNotificationFactory
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.LiveUpdateRouter
import com.clevertap.android.sdk.pushnotification.NotificationInfo
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushType
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.sdk.usereventlogs.UserEventLog
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.utils.UriHelper
import com.clevertap.android.sdk.validation.ManifestValidator
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.variables.CTVariableUtils
import com.clevertap.android.sdk.variables.Var
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.Future
import org.json.JSONException
import org.json.JSONObject

/**
 * <h1>CleverTapAPI</h1>
 * This is the main CleverTapAPI class that manages the SDK instances
 */
public open class CleverTapAPI internal constructor(
    private val context: Context,
    config: CleverTapInstanceConfig,
    coreState: CoreState,
    clock: Clock
) : CTInboxActivity.InboxActivityListener {

    /**
     * Implement to get called back when the device push token is refreshed
     */
    public fun interface DevicePushTokenRefreshListener {

        /**
         * @param token the device token
         * @param type  the token type com.clevertap.android.sdk.PushType (FCM)
         */
        public fun devicePushTokenDidRefresh(token: String?, type: PushType?)
    }

    /**
     * Implement to get called back when the device push token is received
     */
    @RestrictTo(Scope.LIBRARY)
    public fun interface RequestDevicePushTokenListener {

        /**
         * @param token the device token
         * @param type  the token type com.clevertap.android.sdk.PushType (FCM)
         */
        public fun onDevicePushToken(token: String?, type: PushType?)
    }

    /**
     * SDK log levels, from least to most verbose. Set the global level with
     * `CleverTapAPI.setDebugLevel` or an instance's level with [CleverTapInstanceConfig.setDebugLevel].
     */
    public enum class LogLevel(private val value: Int) {
        OFF(-1),
        INFO(0),
        DEBUG(2),
        VERBOSE(3);

        /**
         * Returns the integer value of this level: -1 ([OFF]), 0 ([INFO]), 2 ([DEBUG]) or 3 ([VERBOSE]).
         *
         * @return the integer value
         */
        public fun intValue(): Int = value
    }

    /**
     * SDK-internal handle to this instance's state. Kotlin `internal` because [CoreState] is an
     * internal type; the JVM name stays `getCoreState` for the SDK's Java callers. Other SDK
     * modules use [getInstanceConfig] instead.
     */
    @get:JvmName("getCoreState")
    @set:JvmName("setCoreState")
    internal var coreState: CoreState = coreState

    private var inboxMessageButtonListener = WeakReference<InboxMessageButtonListener?>(null)

    private var inboxMessageListener = WeakReference<InboxMessageListener?>(null)

    // Initialize
    private constructor(
        context: Context,
        config: CleverTapInstanceConfig,
        cleverTapID: String?
    ) : this(
        // Retain only the application context (never an Activity/Fragment) to avoid leaks — see
        // the note in CleverTapFactory.getCoreState. SDK-6254.
        context.applicationContext,
        config,
        CleverTapFactory.getCoreState(context, config, cleverTapID),
        Clock.SYSTEM
    )

    init {
        clevertapClock = clock
        configLogger.verbose("${config.accountId}:async_deviceID", "CoreState is set")
        asyncStartup()
        Logger.i(
            "CleverTap SDK initialized with accountId: ${config.accountId} accountToken: ${config.accountToken} " +
                "accountRegion: ${config.accountRegion}"
        )
    }

    private fun asyncStartup() {
        // Runs after getCoreState() has already posted the device-id barrier (SDK-6254) to this
        // same per-account serial queue, so these startup tasks — and every later app call — are
        // ordered behind device-id resolution. See the ordering contract in
        // CleverTapFactory.getCoreState().
        coreState.executors.postAsyncSafelyTask<Void?>().execute("CleverTapAPI#initializeDeviceInfo") {
            if (config.isDefaultInstance) {
                ManifestValidator.validate(context, coreState.deviceInfo, coreState.pushProviders)
            }
            null
        }

        val now = clevertapClock.currentTimeSecondsInt()
        if (now - CoreMetaData.getInitialAppEnteredForegroundTime() > CREATED_POST_APP_LAUNCH_AFTER_SECONDS) {
            coreState.config.setCreatedPostAppLaunch()
        }

        coreState.executors.postAsyncSafelyTask<Void?>().execute("setStatesAsync") {
            coreState.sessionManager.updateLastVisitTime()
            coreState.sessionManager.updateUserLastVisitTs()
            coreState.deviceInfo.setDeviceNetworkInfoReportingFromStorage()
            coreState.deviceInfo.setCurrentUserOptOutStateFromStorage()
            coreState.deviceInfo.setSystemEventsAllowedStateFromStorage()
            null
        }

        coreState.executors.postAsyncSafelyTask<Void?>().execute("saveConfigtoSharedPrefs") {
            val configJson = config.toJSONString()
            if (configJson == null) {
                Logger.v("Unable to save config to SharedPrefs, config Json is null")
                return@execute null
            }
            StorageHelper.putString(context, config.accountId, "instance", configJson)
            null
        }
        coreState.executors.postAsyncSafelyTask<Void?>().execute("recordDeviceIDErrors") {
            if (coreState.deviceInfo.deviceID != null) {
                coreState.loginController.recordDeviceIDErrors()
            }
            null
        }
    }

    /**
     * Retrieve a [CustomTemplateContext] for a template that is currently displaying. If the provided template
     * name is not of a currently active template this method returns `null`.
     */
    public open fun getActiveContextForTemplate(templateName: String): CustomTemplateContext? =
        coreState.templatesManager.getActiveContextForTemplate(templateName)

    /**
     * Sync all currently registered templates (through [registerCustomInAppTemplates] to the
     * backend. Use this method to transfer template definitions from the SDK to the CT dashboard. This method can
     * only be called from a debug build of the application and the current logged-in user must be marked as a
     * test profile through the web dashboard.
     */
    public open fun syncRegisteredInAppTemplates() {
        if (!isDevelopmentMode) {
            configLogger.debug(
                "CustomTemplates",
                "Your app is NOT in development mode, templates will not be synced"
            )
            return
        }

        val templatesManager = coreState.templatesManager
        val networkManager = coreState.networkManager

        getCleverTapID { _ ->
            // getCleverTapID is executed on the main thread
            coreState.executors.postAsyncSafelyTask<Void?>().execute("DefineTemplates") {
                networkManager.defineTemplates(templatesManager.getAllRegisteredTemplates())
                null
            }
        }
    }

    /**
     * Checks whether notification permission is granted or denied for Android 13 and above devices.
     * @return boolean Returns true/false based on whether permission is granted or denied.
     */
    public open fun isPushPermissionGranted(): Boolean = coreState.inAppController.isPushPermissionGranted()

    /** Kotlin property syntax, equivalent to [isPushPermissionGranted]. */
    public val isPushPermissionGranted: Boolean
        @JvmSynthetic @JvmName("ktIsPushPermissionGranted") inline get() = isPushPermissionGranted()

    /**
     * Calls the push primer flow for Android 13 and above devices.
     * @param jsonObject JSONObject - Accepts jsonObject created by [CTLocalInApp] object
     */
    public open fun promptPushPrimer(jsonObject: JSONObject?) {
        coreState.inAppController.promptPushPrimer(jsonObject!!)
    }

    /**
     * Calls directly hard permission dialog, if push primer is not required.
     * @param showFallbackSettings - boolean - If `showFallbackSettings` is true then we show a alert
     *                             dialog which routes to app's notification settings page.
     */
    public open fun promptForPushPermission(showFallbackSettings: Boolean) {
        coreState.inAppController.promptPermission(showFallbackSettings)
    }

    /**
     * Add a unique value to a multi-value user profile property
     * If the property does not exist it will be created
     *
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     *
     * If the key currently contains a scalar value, the key will be promoted to a multi-value property
     * with the current value cast to a string and the new value(s) added
     *
     * @param key   String
     * @param value String
     */
    public open fun addMultiValueForKey(key: String?, value: String?) {
        addMultiValuesForKey(key, arrayListOf(value))
    }

    /**
     * Add a collection of unique values to a multi-value user profile property
     * If the property does not exist it will be created
     *
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     *
     * If the key currently contains a scalar value, the key will be promoted to a multi-value property
     * with the current value cast to a string and the new value(s) added
     *
     * @param key    String
     * @param values [ArrayList] with String values
     */
    public open fun addMultiValuesForKey(key: String?, values: ArrayList<out String?>?) {
        @Suppress("UNCHECKED_CAST")
        coreState.analyticsManager.addMultiValuesForKey(key, values as ArrayList<String?>?)
    }

    /**
     * Deletes the given [CTInboxMessage] object
     *
     * @param message [CTInboxMessage] public object of inbox message
     */
    public open fun deleteInboxMessage(message: CTInboxMessage?) {
        inboxControllerOrLog()?.deleteInboxMessage(message)
    }

    /**
     * Deletes the [CTInboxMessage] object for given messageId
     *
     * @param messageId String - messageId of [CTInboxMessage] public object of inbox message
     */
    public open fun deleteInboxMessage(messageId: String?) {
        deleteInboxMessage(getInboxMessageForId(messageId))
    }

    /**
     * Deletes multiple [CTInboxMessage] objects for given list of messageIDs
     *
     * @param messageIDs [ArrayList] with String values - list of messageIDs of [CTInboxMessage] public object of inbox message
     */
    public open fun deleteInboxMessagesForIDs(messageIDs: ArrayList<out String?>?) {
        @Suppress("UNCHECKED_CAST")
        inboxControllerOrLog()?.deleteInboxMessagesForIDs(messageIDs as ArrayList<String>?)
    }

    /**
     * Disables the Profile/Events Read and Synchronization API
     * Personalization is enabled by default
     */
    public open fun disablePersonalization() {
        coreState.config.enablePersonalization(false)
    }

    /**
     * Suspends the display of InApp Notifications and discards any new InApp Notifications to be shown
     * after this method is called.
     * The InApp Notifications will be displayed only once resumeInAppNotifications() is called.
     */
    public open fun discardInAppNotifications() {
        discardInAppNotifications(false)
    }

    /**
     * Suspends the display of InApp Notifications and discards any new InApp Notifications to be shown
     * after this method is called.
     *
     * The InApp Notifications will be displayed only once resumeInAppNotifications() is called.
     *
     * @param dismissInAppIfVisible if true, dismisses the currently visible InApp (if any) immediately;
     *                              if false, allows the currently visible InApp to remain displayed
     */
    public open fun discardInAppNotifications(dismissInAppIfVisible: Boolean) {
        if (!coreState.config.isAnalyticsOnly) {
            configLogger.debug(accountId, "Discarding InApp Notifications...")
            configLogger.debug(
                accountId,
                "Please Note - InApp Notifications will be dropped till resumeInAppNotifications() is not called again"
            )
            coreState.inAppController.discardInApps(dismissInAppIfVisible)
        } else {
            configLogger.debug(
                accountId,
                "CleverTap instance is set for Analytics only! Cannot discard InApp Notifications."
            )
        }
    }

    /**
     * Use this method to enable device network-related information tracking, including IP address.
     * This reporting is disabled by default.  To re-disable tracking call this method with enabled set to false.
     *
     * @param value boolean Whether device network info reporting should be enabled/disabled.
     */
    public open fun enableDeviceNetworkInfoReporting(value: Boolean) {
        coreState.deviceInfo.enableDeviceNetworkInfoReporting(value)
    }

    /**
     * Enables the Profile/Events Read and Synchronization API
     * Personalization is enabled by default
     */
    public open fun enablePersonalization() {
        coreState.config.enablePersonalization(true)
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     * @return object of [CTFeatureFlagsController]
     * Handler to get the feature flag values
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun featureFlag(): CTFeatureFlagsController? {
        if (config.isAnalyticsOnly) {
            config.logger
                .debug(accountId, "Feature flag is not supported with analytics only configuration")
        }
        return coreState.controllerManager.ctFeatureFlagsController
    }

    /**
     * Sends all the events in the event queue.
     */
    public open fun flush() {
        coreState.baseEventQueueManager.flush()
    }

    /**
     * This method is used to set the SCDomain callback.
     *
     * Register to handle the domain related events from CleverTap
     * This is to be used only by clevertap-signedcall-sdk
     *
     * @param scDomainListener - the [SCDomainListener] instance
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @WorkerThread
    public open fun setSCDomainListener(scDomainListener: SCDomainListener?) {
        coreState.callbackManager.scDomainListener = scDomainListener

        val domain = coreState.networkManager.getDomain(EventGroup.REGULAR) ?: return
        // Java dereferenced the listener here too: a null listener with a known domain is an NPE.
        scDomainListener!!.onSCDomainAvailable(getSCDomain(domain))
    }

    /**
     * Returns the CleverTap account ID of this instance.
     *
     * @return the account ID
     */
    public open fun getAccountId(): String = coreState.config.accountId

    /** Kotlin property syntax, equivalent to [getAccountId]. */
    public val accountId: String
        @JvmSynthetic @JvmName("ktGetAccountId") inline get() = getAccountId()

    /**
     * Getter for retrieving all the Display Units.
     *
     * @return ArrayList<CleverTapDisplayUnit> - could be null, if there is no Display Unit campaigns
     */
    public open fun getAllDisplayUnits(): ArrayList<CleverTapDisplayUnit>? {
        val cache = coreState.controllerManager.displayUnitCache
        if (cache == null) {
            configLogger.verbose(accountId, "${Constants.FEATURE_DISPLAY_UNIT}Failed to get all Display Units")
            return null
        }
        return cache.getAllDisplayUnits()
    }

    /** Kotlin property syntax, equivalent to [getAllDisplayUnits]. */
    public val allDisplayUnits: ArrayList<CleverTapDisplayUnit>?
        @JvmSynthetic @JvmName("ktGetAllDisplayUnits") inline get() = getAllDisplayUnits()

    /**
     * Returns an ArrayList of all [CTInboxMessage] objects
     *
     * @return ArrayList of [CTInboxMessage] of Inbox Messages
     */
    public open fun getAllInboxMessages(): ArrayList<CTInboxMessage> {
        Logger.d("CleverTapAPI:getAllInboxMessages: called")
        synchronized(coreState.cTLockManager.inboxControllerLock) {
            //return empty list to avoid null pointer exceptions
            val inboxController = inboxControllerOrLog() ?: return ArrayList()
            return inboxController.messages.mapTo(ArrayList()) { messageDAO ->
                Logger.v("CTMessage Dao - ${messageDAO.toJSON()}")
                CTInboxMessage(messageDAO.toJSON())
            }
        }
    }

    /** Kotlin property syntax, equivalent to [getAllInboxMessages]. */
    public val allInboxMessages: ArrayList<CTInboxMessage>
        @JvmSynthetic @JvmName("ktGetAllInboxMessages") inline get() = getAllInboxMessages()

    /**
     * The CTInboxListener object.
     *
     * Getter returns the [CTInboxListener] object; setter sets the CTInboxListener.
     */
    public open fun getCTNotificationInboxListener(): CTInboxListener? = coreState.callbackManager.inboxListener

    /**
     * Sets the listener for App Inbox initialisation and updates.
     *
     * @param notificationInboxListener the listener, or `null` to remove it
     * @see CTInboxListener
     */
    public open fun setCTNotificationInboxListener(notificationInboxListener: CTInboxListener?) {
        coreState.callbackManager.inboxListener = notificationInboxListener
    }

    /** Kotlin property syntax, equivalent to [getCTNotificationInboxListener] / [setCTNotificationInboxListener]. */
    public var ctNotificationInboxListener: CTInboxListener?
        @JvmSynthetic @JvmName("ktGetCTNotificationInboxListener") inline get() = getCTNotificationInboxListener()
        @JvmSynthetic @JvmName("ktSetCTNotificationInboxListener")
        inline set(value) = setCTNotificationInboxListener(value)

    //Debug

    /**
     * The CTPushAmpListener object.
     *
     * Getter returns the [CTPushAmpListener] object; setter sets the CTPushAmpListener.
     */
    public open fun getCTPushAmpListener(): CTPushAmpListener? = coreState.callbackManager.pushAmpListener

    /**
     * Sets the listener that receives Push Amplification payloads. It is called on a CleverTap
     * background thread while the server response is processed.
     *
     * @param pushAmpListener the listener, or `null` to remove it
     */
    public open fun setCTPushAmpListener(pushAmpListener: CTPushAmpListener?) {
        coreState.callbackManager.pushAmpListener = pushAmpListener
    }

    /** Kotlin property syntax, equivalent to [getCTPushAmpListener] / [setCTPushAmpListener]. */
    public var ctPushAmpListener: CTPushAmpListener?
        @JvmSynthetic @JvmName("ktGetCTPushAmpListener") inline get() = getCTPushAmpListener()
        @JvmSynthetic @JvmName("ktSetCTPushAmpListener") inline set(value) = setCTPushAmpListener(value)

    /**
     * The CTPushNotificationListener object.
     *
     * Getter returns the [CTPushNotificationListener] object; setter sets the CTPushNotificationListener.
     */
    public open fun getCTPushNotificationListener(): CTPushNotificationListener? =
        coreState.callbackManager.pushNotificationListener

    /**
     * Sets the listener that receives the payload of clicked CleverTap push notifications. It is called
     * synchronously on the thread that reports the click, for example the main thread when the
     * launched activity is created.
     *
     * @param pushNotificationListener the listener, or `null` to remove it
     */
    public open fun setCTPushNotificationListener(pushNotificationListener: CTPushNotificationListener?) {
        coreState.callbackManager.pushNotificationListener = pushNotificationListener
    }

    /** Kotlin property syntax, equivalent to [getCTPushNotificationListener] / [setCTPushNotificationListener]. */
    public var ctPushNotificationListener: CTPushNotificationListener?
        @JvmSynthetic @JvmName("ktGetCTPushNotificationListener") inline get() = getCTPushNotificationListener()
        @JvmSynthetic @JvmName("ktSetCTPushNotificationListener")
        inline set(value) = setCTPushNotificationListener(value)

    /**
     * Returns a unique CleverTap identifier suitable for use with install attribution providers.
     *
     * @return The attribution identifier currently being used to identify this user.
     *
     * <p><br><span style="background:#ffcc99" >&#9888; this method may take a long time to return,
     * so you should not call it from the application main thread</span></p>
     *
     * <p style="color:red;font-size: 25px;margin-left:10px">&#9760;</p>
     * <b><span style="color:#4d2e00;background:#ffcc99" >Deprecated as of version <code>4.2.0</code> and will be
     * removed in future versions</span>
     * </b><br>
     * <code>Use [CleverTapAPI.getCleverTapID] with an [OnInitCleverTapIDListener] instead</code>
     */
    @Deprecated("Deprecated as of version 4.2.0 and will be removed in future versions. Use " +
        "CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) instead")
    @WorkerThread
    public open fun getCleverTapAttributionIdentifier(): String? = coreState.deviceInfo.attributionID

    /** Kotlin property syntax, equivalent to [getCleverTapAttributionIdentifier]. */
    @Deprecated("Deprecated as of version 4.2.0 and will be removed in future versions. Use " +
        "CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) instead")
    public val cleverTapAttributionIdentifier: String?
        @JvmSynthetic @JvmName("ktGetCleverTapAttributionIdentifier") inline get() = getCleverTapAttributionIdentifier()

    /**
     * Returns a unique identifier by which CleverTap identifies this user.
     *
     * @return The user identifier currently being used to identify this user.
     *
     * <p><br><span style="color:red;background:#ffcc99" >&#9888; this method may take a long time to return,
     * so you should not call it from the application main thread</span></p>
     */
    @WorkerThread
    public open fun getCleverTapID(): String? = coreState.deviceInfo.deviceID

    /** Kotlin property syntax, equivalent to [getCleverTapID]. */
    public val cleverTapID: String?
        @JvmSynthetic @JvmName("ktGetCleverTapID") inline get() = getCleverTapID()

    /**
     * This method is used to decrement the given value
     * Number should be in positive range
     *
     * @param key   String
     * @param value Number
     */
    public open fun decrementValue(key: String?, value: Number?) {
        coreState.analyticsManager.decrementValue(key, value)
    }

    /**
     * Cross-module accessor for this instance's [CleverTapInstanceConfig] (used by other SDK
     * modules, which cannot see the internal `CoreState`).
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public fun getInstanceConfig(): CleverTapInstanceConfig = coreState.config

    /**
     * Returns the total count of the specified event
     *
     * @param event The event for which you want to get the total count
     * @return Total count in int
     *
     * @deprecated since `v7.1.0`. Use [getUserEventLogCount] instead.
     * getUserEventLogCount() provides user-specific event counts.
     */
    @Deprecated("since v7.1.0. Use getUserEventLogCount(String) instead. getUserEventLogCount() provides " +
        "user-specific event counts.")
    public open fun getCount(event: String?): Int = coreState.localDataStore.getEventDetail(event)?.count ?: -1

    /**
     * Retrieves the count of logged events for a specific event name associated with the current
     * user/[CleverTap ID][CleverTapAPI.getCleverTapID].
     * This operation involves a database query and should be called from a background thread.
     *
     * Example usage:
     * ```
     * // Call from background thread
     * int itemSelectedCount = getUserEventLogCount("item_selected")
     * ```
     *
     * @param eventName Name of the event to get the count for (e.g., "navigation_clicked", "item_selected")
     * @return The number of times the specified event has occurred for current user, or -1 if there was an error
     */
    @WorkerThread
    public open fun getUserEventLogCount(eventName: String?): Int {
        if (!config.isPersonalizationEnabled) {
            return -1
        }
        return coreState.localDataStore.readUserEventLogCount(eventName)
    }

    /**
     * Returns an EventDetail object for the particular event passed. EventDetail consists of event name, count, first
     * time
     * and last time timestamp of the event.
     *
     * @param event The event name for which you want the Event details
     * @return The [EventDetail] object
     * @deprecated since `v7.1.0`. Use [getUserEventLog] instead.
     * getUserEventLog() provides user-specific event log.
     */
    @Deprecated(
        "since v7.1.0. Use getUserEventLog(String) instead. getUserEventLog() provides user-specific event log."
    )
    public open fun getDetails(event: String?): EventDetail? = coreState.localDataStore.getEventDetail(event)

    /**
     * Retrieves user-specific event log associated with the current user/
     * [CleverTap ID][CleverTapAPI.getCleverTapID].
     * This operation involves a database query and should be called from a background thread.
     *
     * Example usage:
     * ```
     * // Call from background thread
     * UserEventLog log = getUserEventLog("navigation_clicked")
     * long firstOccurrence = log.firstTs
     * ```
     *
     * @param eventName Name of the event to get the log for (e.g., "navigation_clicked", "item_selected")
     * @return [UserEventLog] or null if the event log does not exist or there was an error
     */
    @WorkerThread
    public open fun getUserEventLog(eventName: String?): UserEventLog? {
        if (!config.isPersonalizationEnabled) {
            return null
        }
        return coreState.localDataStore.readUserEventLog(eventName)
    }

    /**
     * Returns the device push token or null
     *
     * @param type com.clevertap.android.sdk.PushType (FCM)
     * @return String device token or null
     * NOTE: on initial install calling getDevicePushToken may return null, as the device token is
     * not yet available
     * Implement CleverTapAPI.DevicePushTokenRefreshListener to get a callback once the token is
     * available
     */
    // Java dereferenced a null type too (NPE); the parameter stays nullable for Kotlin source compatibility.
    public open fun getDevicePushToken(type: PushType?): String? = coreState.pushProviders.getCachedToken(type!!)

    /**
     * The DevicePushTokenRefreshListener.
     *
     * Getter returns the [DevicePushTokenRefreshListener] object; setter sets it.
     */
    public open fun getDevicePushTokenRefreshListener(): DevicePushTokenRefreshListener? =
        coreState.pushProviders.getDevicePushTokenRefreshListener()

    /**
     * Sets the listener notified when a push token is refreshed. It is called on the thread that
     * delivers the new token, for example the FCM service thread.
     *
     * @param tokenRefreshListener the listener, or `null` to remove it
     */
    public open fun setDevicePushTokenRefreshListener(tokenRefreshListener: DevicePushTokenRefreshListener?) {
        coreState.pushProviders.setDevicePushTokenRefreshListener(tokenRefreshListener)
    }

    /** Kotlin property syntax, equivalent to [getDevicePushTokenRefreshListener] / [setDevicePushTokenRefreshListener]. */
    public var devicePushTokenRefreshListener: DevicePushTokenRefreshListener?
        @JvmSynthetic @JvmName("ktGetDevicePushTokenRefreshListener") inline get() = getDevicePushTokenRefreshListener()
        @JvmSynthetic @JvmName("ktSetDevicePushTokenRefreshListener")
        inline set(value) = setDevicePushTokenRefreshListener(value)

    /**
     * This method is used to set the RequestDevicePushTokenListener object
     *
     * @param requestTokenListener The [RequestDevicePushTokenListener] object
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public open fun setRequestDevicePushTokenListener(requestTokenListener: RequestDevicePushTokenListener) {
        try {
            Logger.v(LOG_TAG, "${FCM_LOG_TAG}Requesting FCM token using googleservices.json")
            FirebaseMessaging
                .getInstance()
                .token
                // Anonymous object, not a lambda: a lambda body would become a CleverTapAPI method
                // taking the gms Task type, and then reflecting on CleverTapAPI (e.g. mocking it)
                // would need play-services-tasks on the classpath. Java's anonymous class avoided that.
                .addOnCompleteListener(object : OnCompleteListener<String> {
                    override fun onComplete(task: com.google.android.gms.tasks.Task<String>) {
                        if (!task.isSuccessful) {
                            Logger.v(
                                LOG_TAG,
                                "${FCM_LOG_TAG}FCM token using googleservices.json failed",
                                task.exception
                            )
                            requestTokenListener.onDevicePushToken(null, FCM)
                            return
                        }
                        val token = task.result
                        Logger.v(LOG_TAG, "${FCM_LOG_TAG}FCM token using googleservices.json - $token")
                        requestTokenListener.onDevicePushToken(token, FCM)
                    }
                })
        } catch (t: Throwable) {
            Logger.v(LOG_TAG, "${FCM_LOG_TAG}Error requesting FCM token", t)
            requestTokenListener.onDevicePushToken(null, FCM)
        }
    }

    //Util

    /**
     * Getter for retrieving Display Unit using the unitID
     *
     * @param unitID - unitID of the Display Unit [CleverTapDisplayUnit.getUnitID]
     * @return CleverTapDisplayUnit - could be null, if there is no Display Unit campaign with the identifier
     */
    public open fun getDisplayUnitForId(unitID: String?): CleverTapDisplayUnit? {
        val cache = coreState.controllerManager.displayUnitCache
        if (cache == null) {
            configLogger.verbose(
                accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Failed to get Display Unit for id: $unitID"
            )
            return null
        }
        return cache.getDisplayUnitForID(unitID)
    }

    /**
     * Returns the timestamp of the first time the given event was raised
     *
     * @param event The event name for which you want the first time timestamp
     * @return The timestamp in int
     * @deprecated since `v7.1.0`. Use [getUserEventLog] instead.
     * It provides user-specific event log with first occurrence timestamp.
     */
    @Deprecated("since v7.1.0. Use getUserEventLog(String) instead. It provides user-specific event log with first " +
        "occurrence timestamp.")
    public open fun getFirstTime(event: String?): Int = coreState.localDataStore.getEventDetail(event)?.firstTime ?: -1

    /**
     * The GeofenceCallback used by clevertap-geofence-sdk to handle geofence responses from CleverTap.
     * SDK-internal (`@RestrictTo(LIBRARY_GROUP)`); not part of the public API.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public open fun getGeofenceCallback(): GeofenceCallback? = coreState.callbackManager.geofenceCallback

    @RestrictTo(Scope.LIBRARY_GROUP)
    public open fun setGeofenceCallback(geofenceCallback: GeofenceCallback?) {
        coreState.callbackManager.geofenceCallback = geofenceCallback
    }

    /** Kotlin property syntax, equivalent to [getGeofenceCallback] / [setGeofenceCallback]. */
    public var geofenceCallback: GeofenceCallback?
        @JvmSynthetic @JvmName("ktGetGeofenceCallback") inline get() = getGeofenceCallback()
        @JvmSynthetic @JvmName("ktSetGeofenceCallback") inline set(value) = setGeofenceCallback(value)

    /**
     * Returns a Map of event names and corresponding event details of all the events raised
     *
     * @return A Map of Event Name and its corresponding EventDetail object
     * @deprecated since `v7.1.0`. Use [userEventLogHistory] instead.
     * getUserEventLogHistory() provides user-specific event logs.
     */
    @Deprecated("since v7.1.0. Use getUserEventLogHistory() instead. getUserEventLogHistory() provides user-specific " +
        "event logs.")
    public open fun getHistory(): Map<String, EventDetail>? = coreState.localDataStore.getEventHistory(context)

    /** Kotlin property syntax, equivalent to [getHistory]. */
    @Deprecated("since v7.1.0. Use getUserEventLogHistory() instead. getUserEventLogHistory() provides user-specific " +
        "event logs.")
    public val history: Map<String, EventDetail>?
        @JvmSynthetic @JvmName("ktGetHistory") inline get() = getHistory()

    /**
     * Retrieves history of all event logs associated with the current user/[CleverTap ID][CleverTapAPI.getCleverTapID] in the ascending order of lastTs.
     * This operation involves a database query and should be called from a background thread.
     *
     * Example usage:
     * ```
     * // Call from background thread
     * Map<String, UserEventLog> history = getUserEventLogHistory()
     * ```
     *
     * @return Map of event name to [UserEventLog] for all events by current user, or empty map if there was an error
     */
    @WorkerThread
    public open fun getUserEventLogHistory(): Map<String, UserEventLog> {
        if (!config.isPersonalizationEnabled) {
            return LinkedHashMap()
        }
        return coreState.localDataStore.readUserEventLogs().associateByTo(LinkedHashMap()) { it.eventName }
    }

    /** Kotlin property syntax, equivalent to [getUserEventLogHistory]. */
    public val userEventLogHistory: Map<String, UserEventLog>
        @JvmSynthetic @JvmName("ktGetUserEventLogHistory") inline get() = getUserEventLogHistory()

    /**
     * The InAppNotificationListener.
     *
     * Getter returns the [InAppNotificationListener] object; setter sets it.
     */
    public open fun getInAppNotificationListener(): InAppNotificationListener? =
        coreState.callbackManager.inAppNotificationListener

    /**
     * Sets the listener for in-app notification lifecycle callbacks.
     *
     * @param inAppNotificationListener the listener, or `null` to remove it
     * @see InAppNotificationListener
     */
    public open fun setInAppNotificationListener(inAppNotificationListener: InAppNotificationListener?) {
        coreState.callbackManager.inAppNotificationListener = inAppNotificationListener
    }

    /** Kotlin property syntax, equivalent to [getInAppNotificationListener] / [setInAppNotificationListener]. */
    public var inAppNotificationListener: InAppNotificationListener?
        @JvmSynthetic @JvmName("ktGetInAppNotificationListener") inline get() = getInAppNotificationListener()
        @JvmSynthetic @JvmName("ktSetInAppNotificationListener") inline set(value) = setInAppNotificationListener(value)

    /**
     * This method unregisters the given instance of the PushPermissionResponseListener if
     * previously registered.
     *
     * Use this method to stop observing the push permission result.
     *
     * @param pushPermissionResponseListener An [PushPermissionResponseListener] object
     */
    public open fun unregisterPushPermissionNotificationResponseListener(
        pushPermissionResponseListener: PushPermissionResponseListener?
    ) {
        coreState.callbackManager.unregisterPushPermissionResponseListener(pushPermissionResponseListener)
    }

    /**
     * This method registers the PushPermissionNotificationResponseListener.
     *
     * Call this method only from the onCreate() of the activity/fragment and unregister the
     * listener from the onDestroy() method using the
     * [unregisterPushPermissionNotificationResponseListener]
     *
     * @param pushPermissionResponseListener An [PushPermissionResponseListener] object
     */
    public open fun registerPushPermissionNotificationResponseListener(
        pushPermissionResponseListener: PushPermissionResponseListener?
    ) {
        coreState.callbackManager.registerPushPermissionResponseListener(pushPermissionResponseListener)
    }

    /**
     * Returns the count of all inbox messages for the user
     *
     * @return int - count of all inbox messages
     */
    public open fun getInboxMessageCount(): Int = synchronized(coreState.cTLockManager.inboxControllerLock) {
        inboxControllerOrLog()?.count() ?: -1
    }

    /** Kotlin property syntax, equivalent to [getInboxMessageCount]. */
    public val inboxMessageCount: Int
        @JvmSynthetic @JvmName("ktGetInboxMessageCount") inline get() = getInboxMessageCount()

    /**
     * Returns the [CTInboxMessage] object that belongs to the given message id
     *
     * @param messageId String - unique id of the inbox message
     * @return [CTInboxMessage] public object of inbox message
     */
    public open fun getInboxMessageForId(messageId: String?): CTInboxMessage? {
        Logger.d("CleverTapAPI:getInboxMessageForId() called with: messageId = [$messageId]")
        return synchronized(coreState.cTLockManager.inboxControllerLock) {
            inboxControllerOrLog()?.getMessageForId(messageId)?.let { CTInboxMessage(it.toJSON()) }
        }
    }

    /**
     * Returns the count of total number of unread inbox messages for the user
     *
     * @return int - count of all unread messages
     */
    public open fun getInboxMessageUnreadCount(): Int = synchronized(coreState.cTLockManager.inboxControllerLock) {
        inboxControllerOrLog()?.unreadCount() ?: -1
    }

    /** Kotlin property syntax, equivalent to [getInboxMessageUnreadCount]. */
    public val inboxMessageUnreadCount: Int
        @JvmSynthetic @JvmName("ktGetInboxMessageUnreadCount") inline get() = getInboxMessageUnreadCount()

    /**
     * Returns the timestamp of the last time the given event was raised
     *
     * @param event The event name for which you want the last time timestamp
     * @return The timestamp in int
     * @deprecated since `v7.1.0`. Use [getUserEventLog] instead.
     * It provides user-specific event log with last occurrence timestamp.
     */
    @Deprecated("since v7.1.0. Use getUserEventLog(String) instead. It provides user-specific event log with last " +
        "occurrence timestamp.")
    public open fun getLastTime(event: String?): Int = coreState.localDataStore.getEventDetail(event)?.lastTime ?: -1

    /**
     * The current device location / the user profile location in CleverTap.
     *
     * Getter: get the current device location.
     * Requires Location Permission in AndroidManifest e.g. "android.permission.ACCESS_COARSE_LOCATION".
     * You can then use the returned Location value to update the user profile location in CleverTap
     * via the setter.
     *
     * Setter: set the user profile location in CleverTap;
     * location can then be used for geo-segmentation etc.
     */
    public open fun getLocation(): Location? = coreState.locationManager.findBestLocation()

    /**
     * Sets the user's location in CleverTap, for example for geo-segmentation. A `null` location is
     * ignored. The location is sent to CleverTap while the app is in the foreground (or when it comes
     * from the Geofence SDK), at most once every 10 seconds.
     *
     * @param location the location
     */
    public open fun setLocation(location: Location?) {
        coreState.locationManager.updateLocation(location)
    }

    /** Kotlin property syntax, equivalent to [getLocation] / [setLocation]. */
    public var location: Location?
        @JvmSynthetic @JvmName("ktGetLocation") inline get() = getLocation()
        @JvmSynthetic @JvmName("ktSetLocation") inline set(value) = setLocation(value)

    /**
     * Returns the timestamp of the previous visit
     *
     * @return Timestamp of previous visit in int
     * @deprecated since `v7.1.0`. Use [userLastVisitTs] instead.
     * getUserLastVisitTs() provides user-specific last visit timestamp.
     */
    @Deprecated("since v7.1.0. Use getUserLastVisitTs() instead. getUserLastVisitTs() provides user-specific last " +
        "visit timestamp.")
    public open fun getPreviousVisitTime(): Int = coreState.sessionManager.lastVisitTime

    /** Kotlin property syntax, equivalent to [getPreviousVisitTime]. */
    @Deprecated("since v7.1.0. Use getUserLastVisitTs() instead. getUserLastVisitTs() provides user-specific last " +
        "visit timestamp.")
    public val previousVisitTime: Int
        @JvmSynthetic @JvmName("ktGetPreviousVisitTime") inline get() = getPreviousVisitTime()

    /**
     * Retrieves timestamp of last visit by current user/[CleverTap ID][CleverTapAPI.getCleverTapID].
     *
     * Example usage:
     * ```
     * long lastVisitTs = getUserLastVisitTs()
     * ```
     *
     * @return Timestamp of last visit by current user, or -1 if there was an error
     */
    public open fun getUserLastVisitTs(): Long {
        if (!config.isPersonalizationEnabled) {
            return -1
        }
        return coreState.sessionManager.userLastVisitTs
    }

    /** Kotlin property syntax, equivalent to [getUserLastVisitTs]. */
    public val userLastVisitTs: Long
        @JvmSynthetic @JvmName("ktGetUserLastVisitTs") inline get() = getUserLastVisitTs()

    /**
     * Retrieves the value of a specific user profile property.
     *
     * Returns null if personalization is disabled in the CleverTap configuration.
     *
     * **Note on Date Properties:** Date-related property values are returned as Unix timestamps
     * (seconds since January 1, 1970, 00:00:00 GMT) prefixed with "$D_". For example, a date property
     * might be returned as "$D_1609459200".
     *
     * @param name The name of the profile property to retrieve
     * @return The value of the specified property, or null if the property doesn't exist or
     *         personalization is disabled. The return type can be String, Number, Boolean, or
     *         a date string with "$D_" prefix.
     */
    @WorkerThread
    public open fun getProperty(name: String?): Any? {
        if (!coreState.config.isPersonalizationEnabled) {
            return null
        }
        return coreState.localDataStore.getProfileProperty(name)
    }

    /**
     * Retrieves the complete user profile as a JSON object for the current user.
     *
     * Returns a copy of the entire profile containing all stored properties for the current user.
     * Returns null if personalization is disabled in the CleverTap configuration.
     *
     * **Note on Date Properties:** Date-related property values are returned as Unix timestamps
     * (seconds since January 1, 1970, 00:00:00 GMT) prefixed with "$D_". For example, a date property
     * might appear as `"birthDate": "$D_1609459200"` in the returned JSON.
     *
     * **Example usage:**
     * ```
     * JSONObject profile = clevertap.getProfile();
     * if (profile != null) {
     *     String email = profile.optString("Email");
     *     String name = profile.optString("Name");
     * }
     * ```
     *
     * @return A JSONObject containing all user profile properties, or null if personalization is disabled.
     */
    @WorkerThread
    public open fun getProfile(): JSONObject? {
        if (!coreState.config.isPersonalizationEnabled) {
            return null
        }
        return coreState.localDataStore.profile
    }

    /** Kotlin property syntax, equivalent to [getProfile]. */
    public val profile: JSONObject?
        @JvmSynthetic @JvmName("ktGetProfile") inline get() = getProfile()

    /**
     * Returns the token for a particular push type
     */
    public open fun getPushToken(pushType: PushType): String? = coreState.pushProviders.getCachedToken(pushType)

    /**
     * Returns the number of screens which have been displayed by the app
     *
     * @return Total number of screens which have been displayed by the app
     */
    public open fun getScreenCount(): Int = CoreMetaData.getActivityCount()

    /** Kotlin property syntax, equivalent to [getScreenCount]. */
    public val screenCount: Int
        @JvmSynthetic @JvmName("ktGetScreenCount") inline get() = getScreenCount()

    /**
     * The SyncListener.
     *
     * Getter returns the [SyncListener] object; setter sets it.
     */
    @Suppress("DEPRECATION")
    public open fun getSyncListener(): SyncListener? = coreState.callbackManager.syncListener

    /**
     * Sets the deprecated [SyncListener].
     *
     * @param syncListener the listener, or `null` to remove it
     */
    @Suppress("DEPRECATION")
    public open fun setSyncListener(syncListener: SyncListener?) {
        coreState.callbackManager.syncListener = syncListener
    }

    /** Kotlin property syntax, equivalent to [getSyncListener] / [setSyncListener]. */
    @Suppress("DEPRECATION")
    public var syncListener: SyncListener?
        @JvmSynthetic @JvmName("ktGetSyncListener") inline get() = getSyncListener()
        @JvmSynthetic @JvmName("ktSetSyncListener") inline set(value) = setSyncListener(value)

    /**
     * Returns the time elapsed by the user on the app
     *
     * @return Time elapsed by user on the app in int
     */
    public open fun getTimeElapsed(): Int {
        val currentSession = coreState.coreMetaData.currentSessionId
        if (currentSession == 0) {
            return -1
        }
        return clevertapClock.currentTimeSecondsInt() - currentSession
    }

    /** Kotlin property syntax, equivalent to [getTimeElapsed]. */
    public val timeElapsed: Int
        @JvmSynthetic @JvmName("ktGetTimeElapsed") inline get() = getTimeElapsed()

    /**
     * Returns the total number of times the app has been launched
     *
     * @return Total number of app launches in int
     * @deprecated since `v7.1.0`. Use [userAppLaunchCount] instead.
     * getUserAppLaunchCount() provides user-specific app launch count.
     */
    @Deprecated("since v7.1.0. Use getUserAppLaunchCount() instead. getUserAppLaunchCount() provides user-specific " +
        "app launch count.")
    public open fun getTotalVisits(): Int =
        coreState.localDataStore.getEventDetail(Constants.APP_LAUNCHED_EVENT)?.count ?: 0

    /** Kotlin property syntax, equivalent to [getTotalVisits]. */
    @Deprecated("since v7.1.0. Use getUserAppLaunchCount() instead. getUserAppLaunchCount() provides user-specific " +
        "app launch count.")
    public val totalVisits: Int
        @JvmSynthetic @JvmName("ktGetTotalVisits") inline get() = getTotalVisits()

    /**
     * Retrieves number of times app launched by current user/[CleverTap ID][CleverTapAPI.getCleverTapID].
     * This operation involves a database query and should be called from a background thread.
     *
     * Example usage:
     * ```
     * // Call from background thread
     * int launchCount = getUserAppLaunchCount()
     * ```
     *
     * @return Number of times app launched by current user, or -1 if there was an error
     */
    @WorkerThread
    public open fun getUserAppLaunchCount(): Int {
        if (!config.isPersonalizationEnabled) {
            return -1
        }
        return coreState.localDataStore.readUserEventLogCount(Constants.APP_LAUNCHED_EVENT)
    }

    /** Kotlin property syntax, equivalent to [getUserAppLaunchCount]. */
    public val userAppLaunchCount: Int
        @JvmSynthetic @JvmName("ktGetUserAppLaunchCount") inline get() = getUserAppLaunchCount()

    /**
     * Returns a UTMDetail object which consists of UTM parameters like source, medium & campaign
     *
     * @return The [UTMDetail] object
     */
    public open fun getUTMDetails(): UTMDetail = UTMDetail().apply {
        source = coreState.coreMetaData.source
        medium = coreState.coreMetaData.medium
        campaign = coreState.coreMetaData.campaign
    }

    /** Kotlin property syntax, equivalent to [getUTMDetails]. */
    public val utmDetails: UTMDetail
        @JvmSynthetic @JvmName("ktGetUTMDetails") inline get() = getUTMDetails()

    /**
     * Returns an ArrayList of unread [CTInboxMessage] objects
     *
     * @return ArrayList of [CTInboxMessage] of unread Inbox Messages
     */
    public open fun getUnreadInboxMessages(): ArrayList<CTInboxMessage> {
        synchronized(coreState.cTLockManager.inboxControllerLock) {
            //return empty list to avoid null pointer exceptions
            val inboxController = inboxControllerOrLog() ?: return ArrayList()
            return inboxController.unreadMessages.mapTo(ArrayList()) { CTInboxMessage(it.toJSON()) }
        }
    }

    /** Kotlin property syntax, equivalent to [getUnreadInboxMessages]. */
    public val unreadInboxMessages: ArrayList<CTInboxMessage>
        @JvmSynthetic @JvmName("ktGetUnreadInboxMessages") inline get() = getUnreadInboxMessages()

    /**
     * Initializes the inbox controller and sends a callback to the [CTInboxListener]
     * This method needs to be called separately for each instance of [CleverTapAPI]
     */
    public open fun initializeInbox() {
        coreState.controllerManager.initializeInbox()
    }

    /**
     * Requests an on-demand refresh of the App Inbox from the CleverTap servers.
     *
     * Throttled to one call per 5 minutes (absolute, across app sessions).
     * Calls made inside that window silently return without hitting the network.
     *
     * Safe to call from any thread. Does not block the caller.
     */
    public open fun fetchInbox() {
        fetchInbox(null)
    }

    /**
     * Same as [fetchInbox], but invokes the callback when the fetch
     * completes (or is dropped by the throttle / session-disable flag).
     *
     * **Thread note:** the callback fires on the SDK's network
     * dispatcher thread, not the main thread. Post back to main yourself if
     * the callback touches UI.
     *
     * @param callback notified with `true` on success, `false`
     *                 otherwise. May be `null`.
     */
    public open fun fetchInbox(callback: FetchInboxCallback?) {
        if (coreState.controllerManager.ctInboxController == null) {
            configLogger.debug(
                accountId,
                "Notification Inbox not initialized — call initializeInbox() first"
            )
            callback?.let { coreState.inboxV2Bridge.submitFailure(it) }
            return
        }
        coreState.inboxV2Bridge.submit(FetchTrigger.USER_INITIATED, callback)
    }

    /**
     * Returns `true` if the V2 inbox endpoint has been session-disabled (HTTP 403).
     * When true, pull-to-refresh should be hidden — further fetch calls will short-circuit.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public open fun isInboxFetchDisabledForSession(): Boolean = coreState.inboxV2Bridge.isInboxFetchDisabledForSession

    /** Kotlin property syntax, equivalent to [isInboxFetchDisabledForSession]. */
    public val isInboxFetchDisabledForSession: Boolean
        @JvmSynthetic @JvmName("ktIsInboxFetchDisabledForSession") inline get() = isInboxFetchDisabledForSession()

    /**
     * Marks the given [CTInboxMessage] object as read
     *
     * @param message [CTInboxMessage] public object of inbox message
     */
    //marks the message as read
    public open fun markReadInboxMessage(message: CTInboxMessage?) {
        inboxControllerOrLog()?.markReadInboxMessage(message)
    }

    /**
     * Marks the given messageId of [CTInboxMessage] object as read
     *
     * @param messageId String - messageId of [CTInboxMessage] public object of inbox message
     */
    public open fun markReadInboxMessage(messageId: String?) {
        markReadInboxMessage(getInboxMessageForId(messageId))
    }

    /**
     * Marks multiple [CTInboxMessage] objects as read for given list of messageIDs
     *
     * @param messageIDs [ArrayList] with String values - list of messageIDs of [CTInboxMessage] public object of inbox message
     */
    public open fun markReadInboxMessagesForIDs(messageIDs: ArrayList<out String?>?) {
        @Suppress("UNCHECKED_CAST")
        inboxControllerOrLog()?.markReadInboxMessagesForIDs(messageIDs as ArrayList<String>?)
    }

    override fun messageDidClick(
        ctInboxActivity: CTInboxActivity?,
        contentPageIndex: Int,
        inboxMessage: CTInboxMessage?,
        data: Bundle?,
        keyValue: HashMap<String, String>?,
        buttonIndex: Int
    ) {
        // CTInboxActivity always passes the clicked message; Java threw an NPE for null as well.
        coreState.analyticsManager.pushInboxMessageStateEvent(true, inboxMessage!!, data)

        Logger.v("clicked inbox notification.")
        //notify the onInboxItemClicked callback if the listener is set.
        inboxMessageListener.get()?.onInboxItemClicked(inboxMessage, contentPageIndex, buttonIndex)

        if (!keyValue.isNullOrEmpty()) {
            Logger.v("clicked button of an inbox notification.")
            //notify the onInboxButtonClick callback if the listener is set.
            inboxMessageButtonListener.get()?.onInboxButtonClick(keyValue)
        }
    }

    //Session

    override fun messageDidShow(ctInboxActivity: CTInboxActivity?, inboxMessage: CTInboxMessage?, data: Bundle?) {
        coreState.executors.postAsyncSafelyTask<Void?>().execute("handleMessageDidShow") {
            // Nothing to record without a message (Java failed this task with an NPE instead).
            val messageId = inboxMessage?.messageId ?: return@execute null
            Logger.d("CleverTapAPI:messageDidShow() called  in async with: messageId = [$messageId]")

            val message = getInboxMessageForId(messageId) ?: return@execute null
            if (!message.isRead) {
                // Use the freshly-fetched `message` for analytics. The viewholder's
                // markItemAsRead mutates `inboxMessage.setRead(true)` on the UI thread
                // before this async body runs, which would trip the T2.3 cross-device
                // gate inside pushInboxMessageStateEvent. The fresh instance reflects
                // the controller's DAO state and isolates analytics from the UI mirror.
                coreState.analyticsManager.pushInboxMessageStateEvent(false, message, data)
                markReadInboxMessage(message)
            }
            null
        }
    }

    /**
     * Creates a separate and distinct user profile identified by one or more of Identity,
     * Email, FBID or GPID values,
     * and populated with the key-values included in the profile map argument.
     *
     * If your app is used by multiple users, you can use this method to assign them each a
     * unique profile to track them separately.
     *
     * If instead you wish to assign multiple Identity, Email, FBID and/or GPID values to the same
     * user profile,
     * use profile.push rather than this method.
     *
     * If none of Identity, Email, FBID or GPID is included in the profile map,
     * all profile map values will be associated with the current user profile.
     *
     * When initially installed on this device, your app is assigned an "anonymous" profile.
     * The first time you identify a user on this device (whether via onUserLogin or profilePush),
     * the "anonymous" history on the device will be associated with the newly identified user.
     *
     * Then, use this method to switch between subsequent separate identified users.
     *
     * Please note that switching from one identified user to another is a costly operation
     * in that the current session for the previous user is automatically closed
     * and data relating to the old user removed, and a new session is started
     * for the new user and data for that user refreshed via a network call to CleverTap.
     * In addition, any global frequency caps are reset as part of the switch.
     *
     * @param profile     The map keyed by the type of identity, with the value as the identity
     * @param cleverTapID Custom CleverTap ID passed by the App
     */
    public open fun onUserLogin(profile: Map<String, Any?>?, cleverTapID: String?) {
        coreState.loginController.onUserLogin(profile, cleverTapID)
    }

    /**
     * Creates a separate and distinct user profile identified by one or more of Identity,
     * Email, FBID or GPID values,
     * and populated with the key-values included in the profile map argument.
     *
     * If your app is used by multiple users, you can use this method to assign them each a
     * unique profile to track them separately.
     *
     * If instead you wish to assign multiple Identity, Email, FBID and/or GPID values to the same
     * user profile,
     * use profile.push rather than this method.
     *
     * If none of Identity, Email, FBID or GPID is included in the profile map,
     * all profile map values will be associated with the current user profile.
     *
     * When initially installed on this device, your app is assigned an "anonymous" profile.
     * The first time you identify a user on this device (whether via onUserLogin or profilePush),
     * the "anonymous" history on the device will be associated with the newly identified user.
     *
     * Then, use this method to switch between subsequent separate identified users.
     *
     * Please note that switching from one identified user to another is a costly operation
     * in that the current session for the previous user is automatically closed
     * and data relating to the old user removed, and a new session is started
     * for the new user and data for that user refreshed via a network call to CleverTap.
     * In addition, any global frequency caps are reset as part of the switch.
     *
     * @param profile The map keyed by the type of identity, with the value as the identity
     */
    public open fun onUserLogin(profile: Map<String, Any?>?) {
        onUserLogin(profile, null)
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     * The handle for product config functionalities(fetch/activate etc.)
     *
     * @return - the instance of [CTProductConfigController]
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun productConfig(): CTProductConfigController? {
        if (config.isAnalyticsOnly) {
            config.logger.debug(
                accountId,
                "Product config is not supported with analytics only configuration"
            )
        }
        @Suppress("DEPRECATION")
        return coreState.getCtProductConfigController(context)
    }

    /**
     * Push Charged event, which describes a purchase made.
     *
     * @param chargeDetails A [HashMap], with keys as strings, and values as [String],
     *                      [Integer], [Long], [Boolean], [Float], [Double],
     *                      [java.util.Date], or [Character]
     * @param items         An [ArrayList] which contains up to 15 [HashMap] objects,
     *                      where each HashMap object describes a particular item purchased
     */
    public open fun pushChargedEvent(
        chargeDetails: HashMap<String, out Any?>?,
        items: ArrayList<out HashMap<String, out Any?>>?
    ) {
        @Suppress("UNCHECKED_CAST")
        coreState.analyticsManager.pushChargedEvent(
            chargeDetails as HashMap<String, Any>?,
            items as ArrayList<HashMap<String, Any>>?
        )
    }

    /**
     * Use this method to pass the deeplink with UTM parameters to track installs
     *
     * @param uri URI of the deeplink
     */
    public open fun pushDeepLink(uri: Uri?) {
        coreState.analyticsManager.pushDeepLink(uri, false)
    }

    /**
     * Replaces the SDK's display-unit cache with the supplied implementation.
     * Pass `null` to clear the reference (subsequent server responses
     * will lazily install a fresh default
     * [com.clevertap.android.sdk.displayunits.CTDisplayUnitController]).
     *
     * The new instance receives subsequent `updateDisplayUnits`
     * calls (e.g. from server responses) and serves all lookup sites:
     * [getDisplayUnitForId], [allDisplayUnits],
     * [pushDisplayUnitViewedEventForID], and
     * [pushDisplayUnitClickedEventForID].
     *
     * Implementations must be thread-safe. The display-unit listener
     * registered via [setDisplayUnitListener] fires only for
     * server-pipeline activity — replacing the cache or mutating its
     * contents from outside the SDK does not synthesise a listener fire.
     *
     * @since 8.3.0
     */
    public open fun setDisplayUnitCache(cache: DisplayUnitCache?) {
        coreState.controllerManager.displayUnitCache = cache
    }

    /**
     * Raises the Display Unit Clicked event
     *
     * @param unitID - unitID of the Display Unit[CleverTapDisplayUnit.getUnitID]
     */
    public open fun pushDisplayUnitClickedEventForID(unitID: String?) {
        coreState.analyticsManager.pushDisplayUnitClickedEventForID(unitID)
    }

    /**
     * Raises a Native Display element click event for the given unit + element.
     *
     * Element-level analog of [pushDisplayUnitClickedEventForID] — for
     * Native Display units that host multiple interactive child elements (buttons,
     * images, etc.), this method records which child element was clicked alongside
     * the existing wzrk_* campaign attribution.
     *
     * `evtData` is assembled in two layers (later layers win on key collision):
     * 1. Caller's `additionalProperties`, merged verbatim — for a per-item event
     *    pass [CleverTapDisplayUnit.getMetaDataForContent], which carries
     *    that item's `wzrk_element_id` and `wzrk_index` from the
     *    `metadata` object the BE attaches to each `content[]` item,
     *    alongside `action`.
     * 2. Cached unit's `wzrk_*` fields layered on top — so server-controlled
     *    attribution always wins over same-named caller-supplied keys.
     *
     * @param unitID               the unitID of the Display Unit
     *                             ([CleverTapDisplayUnit.getUnitID])
     * @param additionalProperties per-click context including `wzrk_element_id`
     *                             and other `wzrk_*` fields from BE action metadata
     */
    public open fun pushDisplayUnitElementClickedEventForID(
        unitID: String?,
        additionalProperties: HashMap<String, out Any?>?
    ) {
        @Suppress("UNCHECKED_CAST")
        coreState.analyticsManager.pushDisplayUnitElementClickedEventForID(
            unitID, additionalProperties as HashMap<String, Any>?
        )
    }

    /**
     * Raises a Native Display element view event for the given unit and element.
     * Merges caller properties with cached unit `wzrk_*` fields, with cached
     * values taking precedence.
     *
     * @param unitID               the Display Unit ID
     * @param additionalProperties per-element context, typically from
     *                             [CleverTapDisplayUnit.getMetaDataForContent]
     */
    public open fun pushDisplayUnitElementViewedEventForID(
        unitID: String?,
        additionalProperties: HashMap<String, out Any?>?
    ) {
        @Suppress("UNCHECKED_CAST")
        coreState.analyticsManager.pushDisplayUnitElementViewedEventForID(
            unitID, additionalProperties as HashMap<String, Any>?
        )
    }

    /**
     * Raises the Display Unit Viewed event
     *
     * @param unitID - unitID of the Display Unit[CleverTapDisplayUnit.getUnitID]
     */
    public open fun pushDisplayUnitViewedEventForID(unitID: String?) {
        coreState.analyticsManager.pushDisplayUnitViewedEventForID(unitID)
    }

    /**
     * Internally records an "Error Occurred" event, which can be viewed in the dashboard.
     *
     * @param errorMessage The error message
     * @param errorCode    The error code
     */
    public open fun pushError(errorMessage: String?, errorCode: Int) {
        coreState.analyticsManager.pushError(errorMessage, errorCode)
    }

    /**
     * Pushes a basic event.
     *
     * @param eventName The name of the event
     */
    public open fun pushEvent(eventName: String?) {
        if (eventName.isNullOrBlank()) {
            return
        }
        pushEvent(eventName, null)
    }

    /**
     * Push an event with a set of attribute pairs.
     *
     * @param eventName    The name of the event
     * @param eventActions A [HashMap], with keys as strings, and values as [String],
     *                     [Integer], [Long], [Boolean], [Float], [Double],
     *                     [java.util.Date], or [Character]
     */
    public open fun pushEvent(eventName: String?, eventActions: Map<String, Any?>?) {
        coreState.analyticsManager.pushEvent(eventName, eventActions)
    }

    /**
     * Sends the FCM registration ID to CleverTap.
     *
     * @param fcmId    The FCM registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    public open fun pushFcmRegistrationId(fcmId: String?, register: Boolean) {
        coreState.pushProviders.handleToken(fcmId, PushConstants.FCM, register)
    }

    /**
     * Pushes a Signed Call event to CleverTap with a set of attribute pairs.
     *
     * @param eventName    The name of the event
     * @param eventProperties The [JSONObject] object that contains the
     *                           event properties regarding Signed Call event
     */
    @Deprecated("Deprecated")
    public open fun pushSignedCallEvent(eventName: String?, eventProperties: JSONObject?): Future<*>? =
        coreState.analyticsManager.raiseEventForSignedCall(eventName, eventProperties)

    /**
     * Used to record errors of the Geofence module
     *
     * @param errorCode    - int - predefined error code for geofences
     * @param errorMessage - String - error message
     */
    public open fun pushGeoFenceError(errorCode: Int, errorMessage: String?) {
        coreState.validationResultStack.pushValidationResult(ValidationResult(errorCode, errorMessage))
    }

    /**
     * Pushes the Geofence Cluster Exited event to CleverTap.
     *
     * @param geoFenceProperties The [JSONObject] object that contains the
     *                           event properties regarding GeoFence Cluster Exited event
     */
    // Java dereferenced null properties too (NPE); the parameter stays nullable for Kotlin source compatibility.
    public open fun pushGeoFenceExitedEvent(geoFenceProperties: JSONObject?): Future<*>? =
        coreState.analyticsManager.raiseEventForGeofences(Constants.GEOFENCE_EXITED_EVENT_NAME, geoFenceProperties!!)

    /**
     * Pushes the Geofence Cluster Entered event to CleverTap.
     *
     * @param geofenceProperties The [JSONObject] object that contains the
     *                           event properties regarding GeoFence Cluster Entered event
     */
    // Java dereferenced null properties too (NPE); the parameter stays nullable for Kotlin source compatibility.
    public open fun pushGeofenceEnteredEvent(geofenceProperties: JSONObject?): Future<*>? =
        coreState.analyticsManager.raiseEventForGeofences(Constants.GEOFENCE_ENTERED_EVENT_NAME, geofenceProperties!!)

    /**
     * Pushes the Notification Clicked event for App Inbox to CleverTap.
     *
     * @param messageId String - messageId of [CTInboxMessage]
     */
    public open fun pushInboxNotificationClickedEvent(messageId: String?) {
        Logger.v("CleverTapAPI:pushInboxNotificationClickedEvent() called with: messageId = [$messageId]")
        // An unknown id has no message; Java threw an NPE further down for it as well.
        coreState.analyticsManager.pushInboxMessageStateEvent(true, getInboxMessageForId(messageId)!!, null)
    }

    /**
     * Pushes the Notification Viewed event for App Inbox to CleverTap.
     *
     * @param messageId String - messageId of [CTInboxMessage]
     */
    public open fun pushInboxNotificationViewedEvent(messageId: String?) {
        Logger.v("CleverTapAPI:pushInboxNotificationViewedEvent() called with: messageId = [$messageId]")
        // An unknown id has no message; Java threw an NPE further down for it as well.
        coreState.analyticsManager.pushInboxMessageStateEvent(false, getInboxMessageForId(messageId)!!, null)
    }

    /**
     * This method is used to push install referrer via url String
     *
     * @param url A String with the install referrer parameters
     */
    public open fun pushInstallReferrer(url: String?) {
        coreState.analyticsManager.pushInstallReferrer(url)
    }

    /**
     * This method is used to push install referrer via UTM source, medium & campaign parameters
     *
     * @param source   The UTM source parameter
     * @param medium   The UTM medium parameter
     * @param campaign The UTM campaign parameter
     */
    @Synchronized
    public open fun pushInstallReferrer(source: String?, medium: String?, campaign: String?) {
        coreState.analyticsManager.pushInstallReferrer(source, medium, campaign)
    }

    /**
     * Pushes the Notification Clicked event to CleverTap.
     *
     * @param extras The [Bundle] object that contains the
     *               notification details
     */
    public open fun pushNotificationClickedEvent(extras: Bundle?) {
        coreState.analyticsManager.pushNotificationClickedEvent(extras)
    }

    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The [Bundle] object that contains the
     *               notification details
     */
    public open fun pushNotificationViewedEvent(extras: Bundle?) {
        coreState.analyticsManager.pushNotificationViewedEvent(extras)
    }

    /**
     * Push a profile update.
     *
     * @param profile A [Map], with keys as strings, and values as [String],
     *                [Integer], [Long], [Boolean], [Float], [Double],
     *                [java.util.Date], or [Character]
     */
    public open fun pushProfile(profile: Map<String, Any?>?) {
        coreState.analyticsManager.pushProfile(profile)
    }

    /**
     * Record a Screen View event
     *
     * @param screenName String, the name of the screen
     */
    public open fun recordScreen(screenName: String?) {
        val currentScreenName = coreState.coreMetaData.screenName
        if (screenName == null || (!currentScreenName.isNullOrEmpty() && currentScreenName == screenName)) {
            return
        }
        configLogger.debug(accountId, "Screen changed to $screenName")
        coreState.coreMetaData.setCurrentScreenName(screenName)
        coreState.analyticsManager.recordPageEventWithExtras(null)
    }

    /**
     * Remove a unique value from a multi-value user profile property
     *
     * If the key currently contains a scalar value, prior to performing the remove operation
     * the key will be promoted to a multi-value property with the current value cast to a string.
     * If the multi-value property is empty after the remove operation, the key will be removed.
     *
     * @param key   String
     * @param value String
     */
    public open fun removeMultiValueForKey(key: String?, value: String?) {
        removeMultiValuesForKey(key, arrayListOf(value))
    }

    /**
     * Remove a collection of unique values from a multi-value user profile property
     *
     * If the key currently contains a scalar value, prior to performing the remove operation
     * the key will be promoted to a multi-value property with the current value cast to a string.
     *
     * If the multi-value property is empty after the remove operation, the key will be removed.
     *
     * @param key    String
     * @param values [ArrayList] with String values
     */
    public open fun removeMultiValuesForKey(key: String?, values: ArrayList<out String?>?) {
        @Suppress("UNCHECKED_CAST")
        coreState.analyticsManager.removeMultiValuesForKey(key, values as ArrayList<String?>?)
    }

    /**
     * Remove the user profile property value specified by key from the user profile. Alternatively this method
     * can also be used to remove PII data (for eg. Email,Name,Phone), locally from database and shared prefs
     *
     * @param key String
     */
    public open fun removeValueForKey(key: String?) {
        coreState.analyticsManager.removeValueForKey(key)
    }

    /**
     * Resumes display of InApp Notifications.
     *
     * If suspendInAppNotifications() was called previously, calling this method will instantly show
     * all queued InApp Notifications and also resume InApp Notifications on events raised after this
     * method is called.
     *
     * If discardInAppNotifications() was called previously, calling this method will only resume
     * InApp Notifications on events raised after this method is called.
     */
    public open fun resumeInAppNotifications() {
        if (!coreState.config.isAnalyticsOnly) {
            configLogger.debug(accountId, "Resuming InApp Notifications...")
            coreState.inAppController.resumeInApps()
        } else {
            configLogger.debug(
                accountId,
                "CleverTap instance is set for Analytics only! Cannot resume InApp Notifications."
            )
        }
    }

    /**
     * This method is used to increment the given value.
     *
     * Number should be in positive range.
     *
     * @param key   String
     * @param value Number
     */
    public open fun incrementValue(key: String?, value: Number?) {
        coreState.analyticsManager.incrementValue(key, value)
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     *
     * This method is used to set the CTFeatureFlagsListener
     * Register to receive feature flag callbacks
     *
     * @param featureFlagsListener The [CTFeatureFlagsListener] object
     */
    @Suppress("DEPRECATION")
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setCTFeatureFlagsListener(featureFlagsListener: CTFeatureFlagsListener?) {
        coreState.callbackManager.featureFlagListener = featureFlagsListener
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     * This method is used to set the product config listener
     * Register to receive callbacks
     *
     * @param listener The [CTProductConfigListener] instance
     */
    @Suppress("DEPRECATION")
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public open fun setCTProductConfigListener(listener: CTProductConfigListener?) {
        coreState.callbackManager.productConfigListener = listener
    }

    /**
     * Sets the listener to get the list of currently running Display Campaigns via callback
     *
     * @param listener [DisplayUnitListener]
     */
    public open fun setDisplayUnitListener(listener: DisplayUnitListener?) {
        coreState.callbackManager.setDisplayUnitListener(listener)
    }

    //Listener

    /**
     * Sets the listener that receives the key-value pairs of clicked in-app buttons.
     *
     * @param listener the listener, or `null` to remove it
     * @see InAppNotificationButtonListener
     */
    public open fun setInAppNotificationButtonListener(listener: InAppNotificationButtonListener?) {
        coreState.callbackManager.inAppNotificationButtonListener = listener
    }

    /**
     * Sets the listener that receives the key-value pairs of clicked App Inbox buttons. The SDK only
     * keeps a weak reference to it, so keep a strong reference yourself.
     *
     * @param listener the listener, or `null` to remove it
     * @see InboxMessageButtonListener
     */
    public open fun setInboxMessageButtonListener(listener: InboxMessageButtonListener?) {
        inboxMessageButtonListener = WeakReference(listener)
    }

    /**
     * Sets the listener notified of App Inbox item and CTA clicks. The SDK only keeps a weak reference
     * to it, so keep a strong reference yourself.
     *
     * @param listener the listener, or `null` to remove it
     * @see InboxMessageListener
     */
    public open fun setCTInboxMessageListener(listener: InboxMessageListener?) {
        inboxMessageListener = WeakReference(listener)
    }

    /**
     * Not to be used by developers. This is used internally to help CleverTap know which library is wrapping the
     * native SDK
     *
     * @param library [String] library name
     */
    public open fun setLibrary(library: String?) {
        coreState.deviceInfo.library = library
    }

    /**
     * Sets the location in CleverTap to get updated GeoFences
     *
     * @param location android.location.Location
     */
    public open fun setLocationForGeofences(location: Location?, sdkVersion: Int): Future<*>? {
        coreState.coreMetaData.isLocationForGeofence = true
        coreState.coreMetaData.geofenceSDKVersion = sdkVersion
        return coreState.locationManager.updateLocation(location)
    }

    /**
     * Set a collection of unique values as a multi-value user profile property, any existing value will be
     * overwritten.
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     *
     * @param key    String
     * @param values [ArrayList] with String values
     */
    public open fun setMultiValuesForKey(key: String?, values: ArrayList<out String?>?) {
        @Suppress("UNCHECKED_CAST")
        coreState.analyticsManager.setMultiValuesForKey(key, values as ArrayList<String?>?)
    }

    /**
     * If you want to stop recorded events from being sent to the server, use this method to set the SDK instance to
     * offline.
     * Once offline, events will be recorded and queued locally but will not be sent to the server until offline is
     * disabled.
     * Calling this method again with offline set to false will allow events to be sent to server and the SDK instance
     * will immediately attempt to send events that have been queued while offline.
     *
     * @param value boolean, true sets the sdk offline, false sets the sdk back online
     */
    public open fun setOffline(value: Boolean) {
        coreState.coreMetaData.isOffline = value
        if (value) {
            configLogger
                .debug(accountId, "CleverTap Instance has been set to offline, won't send events queue")
        } else {
            configLogger
                .debug(accountId, "CleverTap Instance has been set to online, sending events queue")
            flush()
        }
    }

    /**
     * Clears any active mute state set by the backend, allowing the SDK to resume
     * normal event tracking and network operations immediately.
     *
     * The CleverTap backend can mute a client for a set duration (e.g., during a
     * detected abuse scenario). Call this method to override that mute and restore
     * normal SDK operation without waiting for the mute period to expire.
     */
    public open fun unmute() {
        coreState.networkManager.unmute()
    }

    /**
     * Use this method to opt the current user out of all event/profile tracking.
     * You must call this method separately for each active user profile (e.g. when switching user profiles using
     * onUserLogin).
     * Once enabled, no events will be saved remotely or locally for the current user. To re-enable tracking call this
     * method with enabled set to false.
     *
     * @param userOptOut boolean Whether tracking opt out should be enabled/disabled.
     */
    public open fun setOptOut(userOptOut: Boolean) {
        // this is legacy behavior, on opt out we should not allow system events
        // The second flag is set to support legacy method with correctness out of the box.
        setOptOut(userOptOut, !userOptOut)
    }

    /**
     * Use this method to manage the user's consent for event and profile tracking.
     * You must call this method separately for each active user profile (e.g., when switching user profiles using
     * onUserLogin).
     * This method supports the following consent management scenarios:
     *
     * 1. **Complete Opt-Out (userOptOut = true, allowSystemEvents = false):**
     *    No events (neither custom nor system) will be saved remotely or locally for the current user.
     *    This provides the highest level of privacy.
     * 2. **Full Opt-In (userOptOut = false, allowSystemEvents = true):**
     *    All events (custom and system) will be tracked and saved. This is the default behavior
     *    if no opt-out preferences are set.
     * 3. **Partial Opt-In (userOptOut = true, allowSystemEvents = true):**
     *    Only system-level CleverTap events (e.g., app launch, notification viewed) will be tracked.
     *    Custom events raised by the client application will be filtered out.
     *
     * Note: The case where `userOptOut = false` and `allowSystemEvents = false` is invalid.
     * If this combination is provided, the SDK will default to the "Full Opt-In" behavior
     * (userOptOut = false, allowSystemEvents = true).
     *
     * To re-enable full tracking after any form of opt-out, call this method with `userOptOut = false` and `allowSystemEvents = true`.
     *
     * @param userOptOut        boolean: Set to `true` to opt the user out of custom event tracking.
     *                          Set to `false` to opt the user into custom event tracking.
     * @param allowSystemEvents boolean: Set to `true` to allow system-level CleverTap events.
     *                          Set to `false` to disallow system-level CleverTap events (only if `userOptOut` is also `true`).
     */
    public open fun setOptOut(userOptOut: Boolean, allowSystemEvents: Boolean) {
        coreState.executors.postAsyncSafelyTask<Void?>().execute("setOptOut") {
            // Handle case with userOptOut = false, allowSystemEvents = false
            val resolvedAllowSystemEvents = !userOptOut || allowSystemEvents

            // generate the data for a profile push to alert the server to the optOut state change
            val optOutMap = hashMapOf<String, Any?>(
                Constants.CLEVERTAP_OPTOUT to userOptOut,
                Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS to resolvedAllowSystemEvents
            )

            coreState.coreMetaData.isCurrentUserOptedOut = false
            coreState.analyticsManager.pushProfile(optOutMap)
            coreState.coreMetaData.isCurrentUserOptedOut = userOptOut
            coreState.coreMetaData.enabledSystemEvents = resolvedAllowSystemEvents
            // persist the new optOut state
            coreState.deviceInfo.saveOptOutState(userOptOut)
            coreState.deviceInfo.saveAllowedSystemEventsState(resolvedAllowSystemEvents)
            null
        }
    }

    /**
     * Opens [CTInboxActivity] to display Inbox Messages
     *
     * @param styleConfig [CTInboxStyleConfig] configuration of various style parameters for the
     *                    [CTInboxActivity]
     */
    public open fun showAppInbox(styleConfig: CTInboxStyleConfig?) {
        synchronized(coreState.cTLockManager.inboxControllerLock) {
            if (inboxControllerOrLog() == null) {
                return
            }
        }

        // make styleConfig immutable. Java dereferenced a null styleConfig too (NPE); the parameter
        // stays nullable for Kotlin source compatibility.
        val immutableStyleConfig = CTInboxStyleConfig(styleConfig!!)

        val intent = Intent(context, CTInboxActivity::class.java)
            .putExtra("styleConfig", immutableStyleConfig)
            .putExtra("configBundle", Bundle().apply { putParcelable("config", config) })
        try {
            val currentActivity = CoreMetaData.getCurrentActivity()
                ?: error("Current activity reference not found")
            currentActivity.startActivity(intent)
            Logger.d("Displaying Notification Inbox")
        } catch (t: Throwable) {
            Logger.v("Please verify the integration of your app. It is not setup to support Notification Inbox yet.", t)
        }
    }

    /**
     * Dismisses the App Inbox Activity if already opened
     */
    public open fun dismissAppInbox() {
        try {
            val appInboxActivity = coreState.coreMetaData.appInboxActivity
                ?: error("AppInboxActivity reference not found")
            if (!appInboxActivity.isFinishing) {
                configLogger.verbose(accountId, "Finishing the App Inbox")
                appInboxActivity.finish()
            }
        } catch (t: Throwable) {
            configLogger.verbose(
                accountId,
                "Can't dismiss AppInbox, please ensure to call this method after the usage of " +
                    "cleverTapApiInstance.showAppInbox(). \n$t"
            )
        }
    }

    /**
     * Opens [CTInboxActivity] to display Inbox Messages with default [CTInboxStyleConfig] object
     */
    public open fun showAppInbox() {
        showAppInbox(CTInboxStyleConfig())
    }

    /**
     * Suspends display of InApp Notifications.
     * The InApp Notifications are queued once this method is called
     * and will be displayed once resumeInAppNotifications() is called.
     */
    public open fun suspendInAppNotifications() {
        if (!coreState.config.isAnalyticsOnly) {
            configLogger.debug(accountId, "Suspending InApp Notifications...")
            configLogger.debug(
                accountId,
                "Please Note - InApp Notifications will be suspended till resumeInAppNotifications() is not called " +
                "again"
            )
            coreState.inAppController.suspendInApps()
        } else {
            configLogger.debug(
                accountId,
                "CleverTap instance is set for Analytics only! Cannot suspend InApp Notifications."
            )
        }
    }

    /**
     * Dismisses the currently visible Picture-in-Picture (PIP) InApp Notification, if any.
     *
     * This is a no-op when no PIP is visible. Other InApp Notification types are never
     * affected. Safe to call from any thread.
     *
     * Note: dismissing releases the in-app display slot, so the next queued InApp (if any)
     * may show immediately. To keep the screen free of all in-apps, pair this with
     * [suspendInAppNotifications] on screen entry and
     * [resumeInAppNotifications] on exit.
     */
    public open fun dismissPipInApp() {
        if (!coreState.config.isAnalyticsOnly) {
            configLogger.debug(accountId, "Dismissing PIP InApp Notification if visible...")
            coreState.inAppController.dismissPipInApp()
        } else {
            configLogger.debug(
                accountId,
                "CleverTap instance is set for Analytics only! Cannot dismiss PIP InApp Notification."
            )
        }
    }

    // CoreMetaData keys custom SDK versions by a non-null name; a null name is an NPE.
    @RestrictTo(Scope.LIBRARY_GROUP)
    public open fun getCustomSdkVersion(customSdkName: String?): Int =
        coreState.coreMetaData.getCustomSdkVersion(customSdkName!!)

    @RestrictTo(Scope.LIBRARY_GROUP)
    public open fun setCustomSdkVersion(customSdkName: String?, customSdkVersion: Int) {
        coreState.coreMetaData.setCustomSdkVersion(customSdkName!!, customSdkVersion)
    }

    //To be called from DeviceInfo AdID GUID generation

    private val config: CleverTapInstanceConfig
        get() = coreState.config

    private val configLogger: Logger
        get() = config.logger

    private val isErrorDeviceId: Boolean
        get() = coreState.deviceInfo.isErrorDeviceId

    /** The inbox controller, or `null` after logging that the inbox has not been initialized. */
    private fun inboxControllerOrLog(): CTInboxController? =
        coreState.controllerManager.ctInboxController.also {
            if (it == null) {
                configLogger.debug(accountId, "Notification Inbox not initialized")
            }
        }

    /**
     * Subscribe to receive a unique identifier by which CleverTap identifies this user. The listener is called on the
     * main thread and it is invoked each time the id changes until the listener is removed through
     * [removeCleverTapIDListener]
     *
     * @param onInitCleverTapIDListener non-null callback to retrieve identifier on main thread.
     */
    public open fun getCleverTapID(onInitCleverTapIDListener: OnInitCleverTapIDListener) {
        coreState.executors.ioTask<Void?>().execute("getCleverTapID") {
            coreState.deviceInfo.deviceID?.let { deviceID ->
                runOnUiThread { onInitCleverTapIDListener.onInitCleverTapID(deviceID) }
            }
            coreState.callbackManager.addOnInitCleverTapIDListener(onInitCleverTapIDListener)
            null
        }
    }

    /**
     * Remove a previously subscribed OnInitCleverTapIDListener.
     * See [getCleverTapID]
     *
     * @param onInitCleverTapIDListener The same listener instance passed to
     *                                  [getCleverTapID] that should be removed.
     */
    public open fun removeCleverTapIDListener(onInitCleverTapIDListener: OnInitCleverTapIDListener) {
        coreState.callbackManager.removeOnInitCleverTapIDListener(onInitCleverTapIDListener)
    }

    /**
     * Renders a push notification with the given renderer, asynchronously on a CleverTap background
     * thread (serialised with other push rendering).
     *
     * @param iNotificationRenderer the renderer that builds the notification
     * @param context a context; must not be `null`
     * @param extras the push payload
     * @return a [Future] for the rendering task, or `null` if it could not be scheduled
     */
    //TODO: start synchronizing entire flow from here
    public open fun renderPushNotification(
        iNotificationRenderer: INotificationRenderer,
        context: Context?,
        extras: Bundle?
    ): Future<*>? {
        val config = coreState.config
        return try {
            coreState.executors.postAsyncSafelyTask<Void?>().submit("CleverTapAPI#renderPushNotification") {
                synchronized(coreState.pushProviders.getPushRenderingLock()) {
                    createNotificationWith(iNotificationRenderer, context, extras)
                }
                null
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "Failed to process renderPushNotification()", t)
            null
        }
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public open fun renderPushNotificationOnCallerThread(
        iNotificationRenderer: INotificationRenderer,
        context: Context?,
        extras: Bundle?
    ) {
        val config = coreState.config

        try {
            synchronized(coreState.pushProviders.getPushRenderingLock()) {
                config.logger.verbose(
                    config.accountId,
                    "rendering push on caller thread with id = ${Thread.currentThread().id}"
                )
                createNotificationWith(iNotificationRenderer, context, extras)
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "Failed to process renderPushNotification()", t)
        }
    }

    /** Renders [extras] with [renderer]; callers hold the push rendering lock. */
    private fun createNotificationWith(renderer: INotificationRenderer, context: Context?, extras: Bundle?) {
        coreState.pushProviders.pushNotificationRenderer = renderer
        val notificationId = if (extras != null && extras.containsKey(Constants.PT_NOTIF_ID)) {
            extras.getInt(Constants.PT_NOTIF_ID)
        } else {
            Constants.EMPTY_NOTIFICATION_ID
        }
        // Java passed a null context on, failing with an NPE in the renderer; the caller logs it.
        coreState.pushProviders._createNotification(context!!, extras, notificationId)
    }

    /**
     * Check if your app is in development mode.
     *
     * Some functions (like [CleverTapAPI.syncVariables] and [CleverTapAPI.syncRegisteredInAppTemplates])
     * will only work if the app is in development mode and the current user profile is set as a test profile in CT
     * Dashboard.
     *
     * @return `true` if the app is in development mode, `false` otherwise.
     */
    public open fun isDevelopmentMode(): Boolean = isDevelopmentMode(context)

    /** Kotlin property syntax, equivalent to [isDevelopmentMode]. */
    public val isDevelopmentMode: Boolean
        @JvmSynthetic @JvmName("ktIsDevelopmentMode") inline get() = isDevelopmentMode()

    /**
     * Defines a new variable. If the default vale is null it won't resolve the type properly. In
     * that case it is better to use the @Variable annotation instead of this method.
     *
     * @param name Name of the variable.
     * @param defaultValue Default value of variable, used when resolving the underlying value type.
     * @param <T> Type of value.
     * @return Returns the Var instance.
     */
    // Var.define returns null for an invalid name or a null non-file default, as it did in Java.
    public open fun <T> defineVariable(name: String?, defaultValue: T): Var<T>? =
        Var.define(name, defaultValue, coreState.cTVariables)

    /**
     * Defines a new file variable. Disclaimer: cannot be used with @Variable annotation.
     *
     * @param name Name of the variable.
     * @return Returns the Var instance.
     */
    // Var.define returns null for an invalid name, as it did in Java.
    @Suppress("UNCHECKED_CAST")
    public open fun defineFileVariable(name: String?): Var<String>? =
        Var.define<String?>(name, null, CTVariableUtils.FILE, coreState.cTVariables) as Var<String>?

    /**
     * Parses the @Variable annotated fields from a given instance or multiple instances.
     *
     * @param instances Instance or instances to parse.
     */
    public open fun parseVariables(vararg instances: Any?) {
        coreState.parser.parseVariables(*instances)
    }

    /**
     * Parses the @Variable annotated static fields from a given class or multiple classes.
     *
     * @param classes Class object or objects to parse.
     */
    public open fun parseVariablesForClasses(vararg classes: Class<*>?) {
        coreState.parser.parseVariablesForClasses(*classes)
    }

    /**
     * Get a copy of the current value of a variable or a group.
     *
     * @param name The name of the variable or the group.
     * @return The value of the variable or the group.
     */
    public open fun getVariableValue(name: String?): Any? = name?.let { coreState.varCache.getMergedValue(it) }

    /**
     * Get an instance of a variable or a group.
     *
     * @param name The name of the variable or the group.
     * @return The instance of the variable or the group, or null if not created yet.
     */
    public open fun <T> getVariable(name: String?): Var<T>? = name?.let { coreState.varCache.getVariable(it) }

    /**
     * Fetches variable values from server.
     */
    public open fun fetchVariables() {
        fetchVariables(null)
    }

    /**
     * Returns information about the active variants for the current user. Each variant will contain
     * an "id" key mapping to the numeric ID of the variant.
     */
    public open fun variants(): List<Map<String, Any>> {
        // An unmodifiable copy: Java callers get UnsupportedOperationException on mutation, as before.
        @Suppress("UNCHECKED_CAST")
        return Collections.unmodifiableList(coreState.varCache.variants().toList()) as List<Map<String, Any>>
    }

    /**
     * Fetches In Apps from server.
     *
     * @param callback Callback instance [FetchInAppsCallback] to be invoked when fetching is done.
     */
    public open fun fetchInApps(callback: FetchInAppsCallback?) {
        if (coreState.config.isAnalyticsOnly) {
            return
        }
        Logger.v("${Constants.LOG_TAG_INAPP} Fetching In Apps...")
        callback?.let { coreState.callbackManager.fetchInAppsCallback = it }
        coreState.analyticsManager.sendFetchEvent(getFetchRequestAsJson(Constants.FETCH_TYPE_IN_APPS))
    }

    /**
     * Requests a fresh Native Display (ND) advanced-rule metadata bundle mid-session (SDK-6055).
     * Sends a `wzrk_fetch` event with `t = FETCH_TYPE_ND_META`; the server responds with
     * `adUnit_notifs_ss` (see [com.clevertap.android.sdk.response.DisplayUnitResponse]). Old
     * SDKs/servers ignore the unknown fetch type, so this is forward/backward compatible.
     */
    public open fun fetchNativeDisplayMeta() {
        if (coreState.config.isAnalyticsOnly) {
            return
        }
        Logger.v("${Constants.FEATURE_DISPLAY_UNIT}Fetching Native Display metadata...")
        coreState.analyticsManager.sendFetchEvent(getFetchRequestAsJson(Constants.FETCH_TYPE_ND_META))
    }

    @JvmName("getFetchRequestAsJson")
    internal fun getFetchRequestAsJson(fetchType: Int): JSONObject {
        val event = JSONObject()
        try {
            val notif = JSONObject().put(Constants.KEY_T, fetchType)
            event.put(Constants.KEY_EVT_NAME, Constants.WZRK_FETCH)
            event.put(Constants.KEY_EVT_DATA, notif)
        } catch (e: JSONException) {
            Logger.v(
                Constants.CLEVERTAP_LOG_TAG,
                "Failed while parsing fetch request as json:", e
            )
        }
        return event
    }

    /**
     * Fetches variable values from server.
     * Note that SDK keeps only one registered callback, if you call that method again it would
     * override the callback.
     *
     * @param callback Callback instance to be invoked when fetching is done.
     */
    public open fun fetchVariables(callback: FetchVariablesCallback?) {
        if (coreState.config.isAnalyticsOnly) {
            return
        }
        Logger.v("variables", "Fetching  variables")
        callback?.let { coreState.callbackManager.fetchVariablesCallback = it }
        coreState.analyticsManager.sendFetchEvent(getFetchRequestAsJson(Constants.FETCH_TYPE_VARIABLES))
    }

    /**
     * Uploads variables to server.
     */
    public open fun syncVariables() {
        if (isDevelopmentMode) {
            Logger.v("variables", "syncVariables: waiting for id to be available")
            getCleverTapID { _ ->
                val js = coreState.varCache.getDefineVarsData()
                Logger.v("variables", "syncVariables: sending following vars to server:$js")
                coreState.analyticsManager.pushDefineVarsEvent(js)
            }
        } else {
            Logger.v("variables", "Your app is NOT in development mode, variables data will not be sent to server")
        }
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values.
     * Will be called each time new values are fetched.
     *
     * @param callback Callback to register.
     */
    public open fun addVariablesChangedCallback(callback: VariablesChangedCallback) {
        coreState.cTVariables.addVariablesChangedCallback(callback)
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values. Will be
     * called only once and then removed.
     *
     * @param callback Callback to register.
     */
    public open fun addOneTimeVariablesChangedCallback(callback: VariablesChangedCallback) {
        coreState.cTVariables.addOneTimeVariablesChangedCallback(callback)
    }

    /**
     * Removes previously registered callback.
     *
     * @param callback Callback to remove.
     */
    public open fun removeVariablesChangedCallback(callback: VariablesChangedCallback) {
        coreState.cTVariables.removeVariablesChangedCallback(callback)
    }

    /**
     * Removes previously registered callback.
     *
     * @param callback Callback to remove.
     */
    public open fun removeOneTimeVariablesChangedCallback(callback: VariablesChangedCallback) {
        coreState.cTVariables.removeOneTimeVariablesChangedHandler(callback)
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values and file
     * downloads are also completed for file type variables (if any) registered with
     * [defineFileVariable].
     * Will be called each time new values are fetched.
     *
     * @param callback Callback to register.
     */
    public open fun onVariablesChangedAndNoDownloadsPending(callback: VariablesChangedCallback) {
        coreState.cTVariables.onVariablesChangedAndNoDownloadsPending(callback)
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values and file
     * downloads are also completed for file type variables (if any) registered with
     * [defineFileVariable].
     * Will be called only once and then removed.
     *
     * @param callback Callback to register.
     */
    public open fun onceVariablesChangedAndNoDownloadsPending(callback: VariablesChangedCallback) {
        coreState.cTVariables.onceVariablesChangedAndNoDownloadsPending(callback)
    }

    /**
     * Removes all previously registered callbacks.
     */
    public open fun removeAllVariablesChangedCallbacks() {
        coreState.cTVariables.removeAllVariablesChangedCallbacks()
    }

    /**
     * Removes all previously registered one time callbacks.
     */
    public open fun removeAllOneTimeVariablesChangedCallbacks() {
        coreState.cTVariables.removeAllOneTimeVariablesChangedCallbacks()
    }

    /**
     * The custom locale for the current CleverTap instance.
     *
     * Getter returns the customLocale string value; the setter sets a custom locale for the
     * current CleverTap instance (an empty value is ignored).
     */
    public open fun getLocale(): String? = coreState.deviceInfo.customLocale

    /**
     * Sets a custom locale for this instance. A `null` or empty locale is ignored.
     *
     * @param locale the locale, for example `en_US`
     */
    public open fun setLocale(locale: String?) {
        if (locale.isNullOrEmpty()) {
            Logger.i("Empty Locale provided for setLocale, not setting it")
            return
        }
        coreState.deviceInfo.customLocale = locale
    }

    /** Kotlin property syntax, equivalent to [getLocale] / [setLocale]. */
    public var locale: String?
        @JvmSynthetic @JvmName("ktGetLocale") inline get() = getLocale()
        @JvmSynthetic @JvmName("ktSetLocale") inline set(value) = setLocale(value)

    /**
     * Deletes all images and gifs which are preloaded for inapps in cs mode
     *
     * @param expiredOnly to clear only assets which will not be needed further for inapps
     */
    @WorkerThread
    public open fun clearInAppResources(expiredOnly: Boolean) {
        clearResources(expiredOnly, CtCacheType.IMAGE)
    }

    /**
     * Deletes all types of files which are preloaded for SDK features like custom in-app templates, app functions and
     * variables etc.
     *
     * @param expiredOnly to clear only files which will not be needed further for SDK features like custom in-app
     *                    templates, app functions and variables etc.
     */
    @WorkerThread
    public open fun clearFileResources(expiredOnly: Boolean) {
        clearResources(expiredOnly, CtCacheType.FILES)
    }

    private fun clearResources(expiredOnly: Boolean, cacheType: CtCacheType) {
        val repo = FileResourcesRepoFactory.createFileResourcesRepo(context, configLogger, coreState.storeRegistry)
        if (expiredOnly) {
            repo.cleanupExpiredResources(cacheType)
        } else {
            repo.cleanupAllResources(cacheType)
        }
    }

    /**
     * Sends push registration token for the given push type
     *
     * @param token    The token
     * @param pushType Push Provider type for which the token is registered
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    public open fun pushRegistrationToken(token: String?, pushType: PushType?, register: Boolean) {
        coreState.pushProviders.handleToken(token, pushType, register)
    }

    public companion object {

        /** The push payload key that marks a notification as sent by CleverTap. */
        public const val NOTIFICATION_TAG: String = "wzrk_pn"

        /** An instance created this long after the app entered the foreground is "created post app launch". */
        private const val CREATED_POST_APP_LAUNCH_AFTER_SECONDS = 5

        /** Length of the supported sound file extensions (".mp3", ".ogg", ".wav"). */
        private const val SOUND_EXTENSION_LENGTH = 4

        private const val MAX_BITMAP_DOWNLOAD_TIMEOUT_MILLIS = 20000

        private var debugLevel = LogLevel.INFO.intValue()

        @JvmField
        internal var defaultConfig: CleverTapInstanceConfig? = null

        @Suppress("unused")
        private var sdkVersion: String? = null // For Google Play Store/Android Studio analytics

        private var registeredNotificationHandler: NotificationHandler? = null

        private var registeredSignedCallNotificationHandler: NotificationHandler? = null

        @Volatile
        private var liveUpdateNotificationFactory: ICleverTapLiveUpdateNotificationFactory? = null

        private val notificationRenderedListeners = HashMap<String?, NotificationRenderedListener?>()

        private var clevertapClock: Clock = Clock.SYSTEM

        /**
         * This method is used to change the credentials of CleverTap account Id and token programmatically
         *
         * @param accountID CleverTap Account Id
         * @param token     CleverTap Account Token
         */
        @JvmStatic
        public fun changeCredentials(accountID: String?, token: String?) {
            changeCredentials(accountID, token, null)
        }

        /**
         * This method is used to change the credentials of CleverTap account Id, token and region programmatically
         * Once the SDK is initialized with a default instance, subsequent calls to this method will be ignored.
         *
         * @param accountID CleverTap Account Id
         * @param token     CleverTap Account Token
         * @param region    Clever Tap Account Region
         */
        @JvmStatic
        public fun changeCredentials(accountID: String?, token: String?, region: String?) {
            val defaultConfig = defaultConfig
            if (defaultConfig != null) {
                Logger.i(
                    "CleverTap SDK already initialized with accountID:${defaultConfig.accountId} and " +
                        "token:${defaultConfig.accountToken}. Cannot change credentials to $accountID and $token"
                )
                return
            }

            ManifestInfo.changeCredentials(accountID, token, region)
        }

        /**
         * This method is used to change the credentials of CleverTap account Id, token, proxyDomain and spikyProxyDomain programmatically
         *
         * @param accountID         CleverTap Account Id
         * @param token             CleverTap Account Token
         * @param proxyDomain       CleverTap Proxy Domain
         * @param spikyProxyDomain  CleverTap Spiky Proxy Domain
         */
        @JvmStatic
        public fun changeCredentials(
            accountID: String?,
            token: String?,
            proxyDomain: String?,
            spikyProxyDomain: String?
        ) {
            val defaultConfig = defaultConfig
            if (defaultConfig != null) {
                Logger.i(
                    "CleverTap SDK already initialized with accountID:${defaultConfig.accountId}, " +
                        "token:${defaultConfig.accountToken}, proxyDomain: ${defaultConfig.proxyDomain} and " +
                        "spikyDomain: ${defaultConfig.spikyProxyDomain}. Cannot change credentials to " +
                        "accountID: $accountID, token: $token, proxyDomain: ${proxyDomain}and " +
                        "spikyProxyDomain: $spikyProxyDomain"
                )
                return
            }

            ManifestInfo.changeCredentials(accountID, token, proxyDomain, spikyProxyDomain)
        }

        /**
         * This method is used to change the credentials of CleverTap account Id, token, proxyDomain, spikyProxyDomain programmatically
         *
         * @param accountID         CleverTap Account Id
         * @param token             CleverTap Account Token
         * @param proxyDomain       CleverTap Proxy Domain
         * @param spikyProxyDomain  CleverTap Spiky Proxy Domain
         * @param customHandshakeDomain  Custom handshake Domain
         */
        @JvmStatic
        public fun changeCredentials(
            accountID: String?,
            token: String?,
            proxyDomain: String?,
            spikyProxyDomain: String?,
            customHandshakeDomain: String?
        ) {
            val defaultConfig = defaultConfig
            if (defaultConfig != null) {
                Logger.i(
                    "CleverTap SDK already initialized with accountID:${defaultConfig.accountId}, " +
                        "token:${defaultConfig.accountToken}, proxyDomain: ${defaultConfig.proxyDomain}, " +
                        "spikyDomain: ${defaultConfig.spikyProxyDomain}, " +
                        "handshakeDomain: ${defaultConfig.customHandshakeDomain}. Cannot change credentials to " +
                        "accountID: $accountID, token: $token, proxyDomain: $proxyDomain, " +
                        "spikyProxyDomain: ${spikyProxyDomain}and customHandshakeDomain: $customHandshakeDomain"
                )
                return
            }

            ManifestInfo.changeCredentials(accountID, token, proxyDomain, spikyProxyDomain, customHandshakeDomain)
        }

        /**
         * Launches an asynchronous task to download the notification icon from CleverTap,
         * and create the Android notification.
         *
         * If your app is using CleverTap SDK's built in FCM message handling,
         * this method does not need to be called explicitly.
         *
         * Use this method when implementing your own FCM handling mechanism. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * This entry renders core push notifications and client-factory (Mode A) Live Updates. For
         * SDK-rendered Push Templates — including the `pt_progress` (Mode B) Live Update — call
         * `CTFcmMessageHandler().createNotification(context, remoteMessage)` instead, which routes
         * through the template renderer.
         *
         * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
         * Note: Starting from core v5.1.0, this method runs on the caller's thread. Make sure to call it
         * in onMessageReceive() of messaging service.
         * </p>
         *
         * @param context        A reference to an Android context
         * @param extras         The [Bundle] object received by the broadcast receiver
         * @param notificationId A custom id to build a notification
         */
        @JvmStatic
        public fun createNotification(context: Context?, extras: Bundle?, notificationId: Int) {
            val instance = fromBundle(context, extras) ?: return
            val coreState = instance.coreState
            val config = coreState.config

            try {
                synchronized(coreState.pushProviders.getPushRenderingLock()) {
                    coreState.pushProviders.pushNotificationRenderer = CoreNotificationRenderer()
                    // a null context fails here as it did in Java; the catch below logs it
                    coreState.pushProviders._createNotification(context!!, extras, notificationId)
                }
            } catch (t: Throwable) {
                config.logger.debug(config.accountId, "Failed to process createNotification()", t)
            }
        }

        /**
         * Returns the instance for an account. When no instance exists yet, it is created from the
         * default config (for a `null` account ID) or from the config stored for that account.
         *
         * @param context a context, needed to create an instance
         * @param _accountId the account ID, or `null` for the default instance
         * @return the instance, or `null` if there is none and it cannot be created
         */
        @JvmStatic
        public fun getGlobalInstance(context: Context?, _accountId: String?): CleverTapAPI? =
            fromAccountId(context, _accountId)

        /**
         * Pass Push Notification Payload to CleverTap for smooth functioning of Pull Notifications
         *
         * @param context - Application Context
         * @param extras  - Bundle received via FCM/Pull Notifications
         */
        @JvmStatic
        public fun processPushNotification(context: Context?, extras: Bundle?) {
            val instance = fromBundle(context, extras) ?: return
            // fromBundle already required non-null extras
            instance.coreState.pushProviders.processCustomPushNotification(extras!!)
        }

        /**
         * Launches an asynchronous task to download the notification icon from CleverTap,
         * and create the Android notification.
         *
         * Use this method when implementing your own FCM handling mechanism. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
         *      Note: Starting from core v5.1.0, this method runs on the caller's thread. Make sure to call it
         *      in onMessageReceive() of messaging service.
         * </p>
         *
         * @param context A reference to an Android context
         * @param extras  The [Bundle] object received by the broadcast receiver
         */
        @JvmStatic
        public fun createNotification(context: Context?, extras: Bundle?) {
            createNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID)
        }

        /**
         * Launches an asynchronous task to create the notification channel from CleverTap
         *
         * Use this method when implementing your own FCM handling mechanism. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * @param context            A reference to an Android context
         * @param channelId          A String for setting the id of the notification channel
         * @param channelName        A String for setting the name of the notification channel
         * @param channelDescription A String for setting the description of the notification channel
         * @param importance         An Integer value setting the importance of the notifications sent in this channel
         * @param showBadge          An boolean value as to whether this channel shows a badge
         */
        @JvmStatic
        public fun createNotificationChannel(
            context: Context?, channelId: String?,
            channelName: CharSequence?, channelDescription: String?, importance: Int,
            showBadge: Boolean
        ) {
            createChannel(
                context, channelId, channelName, channelDescription, importance,
                groupId = null, showBadge = showBadge, customSound = false, sound = null,
                taskName = "createNotificationChannel"
            )
        }

        /**
         * Launches an asynchronous task to create the notification channel from CleverTap
         *
         * Use this method when implementing your own FCM handling mechanism and creating
         * notification channel groups. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * @param context            A reference to an Android context
         * @param channelId          A String for setting the id of the notification channel
         * @param channelName        A String for setting the name of the notification channel
         * @param channelDescription A String for setting the description of the notification channel
         * @param importance         An Integer value setting the importance of the notifications sent in this
         *                           channel
         * @param groupId            A String for setting the notification channel as a part of a notification
         *                           group
         * @param showBadge          An boolean value as to whether this channel shows a badge
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @JvmStatic
        public fun createNotificationChannel(
            context: Context?, channelId: String?,
            channelName: CharSequence?, channelDescription: String?, importance: Int,
            groupId: String?, showBadge: Boolean
        ) {
            createChannel(
                context, channelId, channelName, channelDescription, importance,
                groupId = groupId, showBadge = showBadge, customSound = false, sound = null,
                taskName = "creatingNotificationChannel"
            )
        }

        /**
         * Launches an asynchronous task to create the notification channel from CleverTap
         *
         * Use this method when implementing your own FCM handling mechanism. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * @param context            A reference to an Android context
         * @param channelId          A String for setting the id of the notification channel
         * @param channelName        A String for setting the name of the notification channel
         * @param channelDescription A String for setting the description of the notification channel
         * @param importance         An Integer value setting the importance of the notifications sent in this channel
         * @param showBadge          An boolean value as to whether this channel shows a badge
         * @param sound              A String denoting the custom sound raw file for this channel
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @JvmStatic
        public fun createNotificationChannel(
            context: Context?, channelId: String?,
            channelName: CharSequence?, channelDescription: String?, importance: Int,
            showBadge: Boolean, sound: String?
        ) {
            createChannel(
                context, channelId, channelName, channelDescription, importance,
                groupId = null, showBadge = showBadge, customSound = true, sound = sound,
                taskName = "createNotificationChannel"
            )
        }

        /**
         * Launches an asynchronous task to create the notification channel from CleverTap
         *
         * Use this method when implementing your own FCM handling mechanism and creating
         * notification channel groups. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * @param context            A reference to an Android context
         * @param channelId          A String for setting the id of the notification channel
         * @param channelName        A String for setting the name of the notification channel
         * @param channelDescription A String for setting the description of the notification channel
         * @param importance         An Integer value setting the importance of the notifications sent in this
         *                           channel
         * @param groupId            A String for setting the notification channel as a part of a notification
         *                           group
         * @param showBadge          An boolean value as to whether this channel shows a badge
         * @param sound              A String denoting the custom sound raw file for this channel
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @JvmStatic
        public fun createNotificationChannel(
            context: Context?, channelId: String?,
            channelName: CharSequence?, channelDescription: String?, importance: Int,
            groupId: String?, showBadge: Boolean, sound: String?
        ) {
            createChannel(
                context, channelId, channelName, channelDescription, importance,
                groupId = groupId, showBadge = showBadge, customSound = true, sound = sound,
                taskName = "creatingNotificationChannel"
            )
        }

        /**
         * Launches an asynchronous task to create the notification channel group from CleverTap
         *
         * Use this method when implementing your own FCM handling mechanism. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * @param context   A reference to an Android context
         * @param groupId   A String for setting the id of the notification channel group
         * @param groupName A String for setting the name of the notification channel group
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @JvmStatic
        public fun createNotificationChannelGroup(
            context: Context?, groupId: String?,
            groupName: CharSequence?
        ) {
            withNotificationManager(
                context,
                apiName = "createNotificationChannelGroup",
                taskName = "creatingNotificationChannelGroup",
                failureMessage = "Failure creating Notification Channel Group"
            ) { instance, notificationManager ->
                notificationManager.createNotificationChannelGroup(NotificationChannelGroup(groupId, groupName))
                instance.configLogger.info(
                    instance.accountId,
                    "Notification channel group $groupName has been created"
                )
            }
        }

        /**
         * Launches an asynchronous task to delete the notification channel from CleverTap
         *
         * Use this method when implementing your own FCM handling mechanism. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * @param context   A reference to an Android context
         * @param channelId A String for setting the id of the notification channel
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @JvmStatic
        public fun deleteNotificationChannel(context: Context?, channelId: String?) {
            withNotificationManager(
                context,
                apiName = "deleteNotificationChannel",
                taskName = "deletingNotificationChannel",
                failureMessage = "Failure deleting Notification Channel"
            ) { instance, notificationManager ->
                notificationManager.deleteNotificationChannel(channelId)
                instance.configLogger.info(instance.accountId, "Notification channel $channelId has been deleted")
            }
        }

        /**
         * Launches an asynchronous task to delete the notification channel from CleverTap
         *
         * Use this method when implementing your own FCM handling mechanism. Refer to the
         * SDK documentation for usage scenarios and examples.
         *
         * @param context A reference to an Android context
         * @param groupId A String for setting the id of the notification channel group
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @JvmStatic
        public fun deleteNotificationChannelGroup(context: Context?, groupId: String?) {
            withNotificationManager(
                context,
                apiName = "deleteNotificationChannelGroup",
                taskName = "deletingNotificationChannelGroup",
                failureMessage = "Failure deleting Notification Channel Group"
            ) { instance, notificationManager ->
                notificationManager.deleteNotificationChannelGroup(groupId)
                instance.configLogger.info(instance.accountId, "Notification channel group $groupId has been deleted")
            }
        }

        private fun createChannel(
            context: Context?,
            channelId: String?,
            channelName: CharSequence?,
            channelDescription: String?,
            importance: Int,
            groupId: String?,
            showBadge: Boolean,
            customSound: Boolean,
            sound: String?,
            taskName: String
        ) {
            withNotificationManager(
                context,
                apiName = "createNotificatonChannel",
                taskName = taskName,
                failureMessage = "Failure creating Notification Channel",
                action = @RequiresApi(api = Build.VERSION_CODES.O) { instance, notificationManager ->
                    val soundUri = if (customSound) { soundUri(context, instance, sound) } else { null }
                    val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
                        description = channelDescription
                        group = groupId // null (the default) for the overloads without a group
                        setShowBadge(showBadge)
                    }
                    when {
                        soundUri != null -> notificationChannel.setSound(
                            soundUri,
                            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                        )

                        customSound -> instance.configLogger.debug(
                            instance.accountId,
                            "Sound file not found, notification channel will be created without custom sound"
                        )
                    }
                    notificationManager.createNotificationChannel(notificationChannel)
                    instance.configLogger.info(instance.accountId, "Notification channel $channelName has been created")
                }
            )
        }

        /** The `android.resource` uri of a raw sound file such as `chime.mp3`, or `null` if unsupported. */
        private fun soundUri(context: Context?, instance: CleverTapAPI, sound: String?): Uri? {
            // Java dereferenced a null sound name here too; the task logs the failure.
            val fileName = sound!!
            if (fileName.isEmpty()) {
                return null
            }
            if (!(fileName.contains(".mp3") || fileName.contains(".ogg") || fileName.contains(".wav"))) {
                instance.configLogger.debug(instance.accountId, "Sound file name not supported")
                return null
            }
            val rawName = fileName.substring(0, fileName.length - SOUND_EXTENSION_LENGTH)
            if (rawName.isEmpty()) {
                return null
            }
            return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context?.packageName}/raw/$rawName")
        }

        /**
         * Runs [action] with the [NotificationManager] on the SDK's async queue (Android O+), logging
         * through the default instance, or the first other instance.
         */
        private fun withNotificationManager(
            context: Context?,
            apiName: String,
            taskName: String,
            failureMessage: String,
            action: (CleverTapAPI, NotificationManager) -> Unit
        ) {
            val instance = getDefaultInstanceOrFirstOther(context)
            if (instance == null) {
                Logger.v("No CleverTap Instance found in CleverTapAPI#$apiName")
                return
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    instance.coreState.executors.postAsyncSafelyTask<Void?>().execute(taskName) {
                        // Java dereferenced a null context here too; the task logs the failure.
                        val notificationManager =
                            context!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
                            ?: return@execute null
                        action(instance, notificationManager)
                        null
                    }
                }
            } catch (t: Throwable) {
                instance.configLogger.verbose(instance.accountId, failureMessage, t)
            }
        }

        /*
        This method was generating the token using Custom FCM Sender ID. The token was overriden from onNewToken
        in the presence of Custom FCM Sender ID present in the AndroidManifest.xml.
        Deprecated now as we can directly call tokenRefresh method from onNewToken implementation
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Deprecated("Deprecated now as we can directly call tokenRefresh method from onNewToken implementation")
        @JvmStatic
        public fun fcmTokenRefresh(context: Context?, token: String?) {
            for (instance in getAvailableInstances(context)) {
                if (instance.coreState.config.isAnalyticsOnly) {
                    Logger.d("Instance is Analytics Only not processing device token")
                    continue
                }
                instance.coreState.pushProviders.doTokenRefresh(token, PushConstants.FCM)
            }
        }

        /**
         * Returns the log level set for CleverTapAPI
         *
         * @return The [LogLevel] int value
         */
        @JvmStatic
        public fun getDebugLevel(): Int = debugLevel

        /**
         * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
         * Debug messages are tagged as CleverTap.
         *
         * @param level Can be one of the following:  -1 (disables all debugging), 0 (default, shows minimal SDK
         *              integration related logging),
         *              1(shows debug output), 3(shows verbose output)
         */
        @JvmStatic
        public fun setDebugLevel(level: Int) {
            debugLevel = level
        }

        /**
         * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
         * Debug messages are tagged as CleverTap.
         *
         * @param level Can be one of the following: LogLevel.OFF (disables all debugging), LogLevel.INFO (default, shows
         *              minimal SDK integration related logging),
         *              LogLevel.DEBUG(shows debug output),LogLevel.VERBOSE(shows verbose output)
         */
        @JvmStatic
        public fun setDebugLevel(level: LogLevel) {
            debugLevel = level.intValue()
        }

        /**
         * Returns the default shared instance of the CleverTap SDK.
         *
         * @param context     The Android context
         * @param cleverTapID Custom CleverTapID passed by the app
         * @return The [CleverTapAPI] object
         */
        @JvmStatic
        public fun getDefaultInstance(
            context: Context?,
            cleverTapID: String?
        ): CleverTapAPI? {
            // For Google Play Store/Android Studio tracking
            sdkVersion = BuildConfig.SDK_VERSION_STRING

            val config = defaultConfig ?: getDefaultConfig(context).also { defaultConfig = it }
            return config?.let { instanceWithConfig(context, it, cleverTapID) }
        }

        /**
         * Returns the default shared instance of the CleverTap SDK.
         *
         * @param context The Android context
         * @return The [CleverTapAPI] object
         */
        @JvmStatic
        public fun getDefaultInstance(context: Context?): CleverTapAPI? = getDefaultInstance(context, null)

        /**
         * The instance for [accountId] (the default instance when it is null), creating it when no
         * instance exists yet; `null` when there is none.
         */
        private fun fromAccountId(context: Context?, accountId: String?): CleverTapAPI? {
            if (CleverTapInstanceStore.isEmpty()) {
                return createInstanceIfAvailable(context, accountId)
            }
            return CleverTapInstanceStore.all().firstOrNull { instance ->
                (accountId == null && instance.coreState.config.isDefaultInstance) || instance.accountId == accountId
            }
        }

        /**
         * Returns a copy of all CleverTap instances created so far, keyed by account ID.
         *
         * @return the instances; changing the map does not affect the SDK
         */
        @JvmStatic
        public fun getInstances(): HashMap<String, CleverTapAPI> = CleverTapInstanceStore.snapshot()

        /**
         * Replaces the SDK's registry of instances.
         *
         * @param instances the instances keyed by account ID, or `null` to clear the registry
         */
        @JvmStatic
        public fun setInstances(instances: HashMap<String, CleverTapAPI>?) {
            CleverTapInstanceStore.replaceAll(instances)
        }

        /**
         * Checks whether this notification is from CleverTap.
         *
         * @param extras The payload from the FCM intent
         * @return See [NotificationInfo]
         */
        @JvmStatic
        public fun getNotificationInfo(extras: Bundle?): NotificationInfo {
            if (extras == null) {
                return NotificationInfo(false, false)
            }

            val fromCleverTap = extras.containsKey(Constants.NOTIFICATION_TAG)
            val shouldRender = fromCleverTap && extras.containsKey("nm")
            return NotificationInfo(fromCleverTap, shouldRender)
        }

        // other static handlers
        @RestrictTo(Scope.LIBRARY)
        @JvmStatic
        public fun handleNotificationClicked(context: Context?, notification: Bundle?) {
            if (notification == null) {
                return
            }

            fromAccountId(context, accountIdOf(notification))?.pushNotificationClickedEvent(notification)
        }

        /**
         * Raises the "Live Activity" lifecycle event with state `Dismissed` for the given push
         * bundle. Call this from your own delete intent when you render a Live Update with custom
         * dismiss handling and want CleverTap to record the dismissal.
         *
         * No-op unless `notification` is a CleverTap Live Update push, so it can never raise a
         * Dismissed event for a normal notification.
         */
        @JvmStatic
        public fun handleLiveUpdateDismissed(context: Context?, notification: Bundle?) {
            if (notification == null) {
                return
            }

            if (!LiveUpdateRouter.isLiveUpdate(notification)) {
                Logger.v("handleLiveUpdateDismissed called with a non Live Update bundle; ignoring.")
                return
            }

            // Reuse the shared instance-resolution (default vs multi-instance) instead of re-implementing
            // the registry lookup — same path handleNotificationClicked and friends use.
            val instance = fromAccountId(context, accountIdOf(notification)) ?: return

            // Dismissal is delivered on the BroadcastReceiver's main thread; raise the event on a worker
            // so the analytics/DB queue work (raiseLiveUpdateLifecycleEvent is @WorkerThread) never runs
            // on the main thread.
            instance.coreState.executors.postAsyncSafelyTask<Void?>().execute("handleLiveUpdateDismissed") {
                instance.coreState.analyticsManager
                    .raiseLiveUpdateLifecycleEvent(notification, Constants.LIVE_UPDATE_STATE_DISMISSED)
                null
            }
        }

        /**
         * Returns an instance of the CleverTap SDK using CleverTapInstanceConfig.
         *
         * @param context The Android context
         * @param config  The [CleverTapInstanceConfig] object
         * @return The [CleverTapAPI] object, or `null` if [config] is null
         */
        @JvmStatic
        public fun instanceWithConfig(context: Context?, config: CleverTapInstanceConfig?): CleverTapAPI? =
            instanceWithConfig(context, config, null)

        /**
         * Returns an instance of the CleverTap SDK using CleverTapInstanceConfig.
         *
         * @param context The Android context
         * @param config  The [CleverTapInstanceConfig] object
         * @return The [CleverTapAPI] object, or `null` if [config] is null
         */
        @JvmStatic
        public fun instanceWithConfig(
            context: Context?,
            config: CleverTapInstanceConfig?,
            cleverTapID: String?
        ): CleverTapAPI? {
            if (config == null) {
                Logger.v("CleverTapInstanceConfig cannot be null")
                return null
            }
            var created = false
            val instance = CleverTapInstanceStore.getOrCreate(config.accountId) {
                created = true
                // Java dereferenced a null context when creating an instance too.
                CleverTapAPI(context!!, config, cleverTapID)
            }
            if (!created && instance.config.enableCustomCleverTapId &&
                Utils.validateCTID(cleverTapID) && instance.isErrorDeviceId
            ) {
                instance.coreState.loginController.asyncProfileSwitchUser(null, null, cleverTapID)
            }
            Logger.v("${config.accountId}:async_deviceID", "CleverTapAPI instance = $instance")
            return instance
        }

        /**
         * Returns whether or not the app is in the foreground.
         *
         * @return The foreground status
         */
        @JvmStatic
        public fun isAppForeground(): Boolean = CoreMetaData.isAppForeground()

        /**
         * Use this method to notify CleverTap that the app is in foreground
         *
         * @param appForeground boolean true/false
         */
        @JvmStatic
        public fun setAppForeground(appForeground: Boolean) {
            CoreMetaData.setAppForeground(appForeground)
        }

        /**
         * Notifies all instances that the current activity was paused. Called by
         * [ActivityLifecycleCallback]; call it from your activities' `onPause` only if you don't
         * register the lifecycle callback.
         */
        @JvmStatic
        public fun onActivityPaused() {
            for (instance in CleverTapInstanceStore.all()) {
                try {
                    instance.coreState.activityLifeCycleManager.activityPaused()
                } catch (t: Throwable) {
                    // Ignore
                }
            }
        }

        /**
         * Notifies the SDK that an activity was resumed. Same as `onActivityResumed(activity, null)`.
         *
         * @param activity the resumed activity
         * @see onActivityPaused
         */
        @JvmStatic
        public fun onActivityResumed(activity: Activity?) {
            onActivityResumed(activity, null)
        }

        /**
         * Notifies the SDK that an activity was resumed: marks the app as in the foreground, creates the
         * default instance if none exists yet, and lets every instance handle the resume. Called by
         * [ActivityLifecycleCallback]; call it from your activities' `onResume` only if you don't
         * register the lifecycle callback.
         *
         * @param activity the resumed activity
         * @param cleverTapID a custom CleverTap ID used if the default instance is created here
         */
        @JvmStatic
        public fun onActivityResumed(activity: Activity?, cleverTapID: String?) {
            if (CleverTapInstanceStore.isEmpty()) {
                // Java dereferenced a null activity here too.
                createInstanceIfAvailable(activity!!.applicationContext, null, cleverTapID)
            }

            CoreMetaData.setAppForeground(true)

            if (CleverTapInstanceStore.isEmpty()) {
                Logger.v("Instances is null in onActivityResumed!")
                return
            }

            val currentActivityName = CoreMetaData.getCurrentActivityName()
            CoreMetaData.setCurrentActivity(activity)
            if (currentActivityName == null || currentActivityName != activity!!.localClassName) {
                CoreMetaData.incrementActivityCount()
            }

            if (CoreMetaData.getInitialAppEnteredForegroundTime() <= 0) {
                CoreMetaData.setInitialAppEnteredForegroundTime(clevertapClock.currentTimeSecondsInt())
            }

            for (instance in CleverTapInstanceStore.all()) {
                try {
                    instance.coreState.activityLifeCycleManager.activityResumed()
                } catch (t: Throwable) {
                    Logger.v("Throwable - ${t.localizedMessage}")
                }
            }
        }

        // The push entry points have always required a payload: Java dereferenced a null one too.
        private fun fromBundle(context: Context?, extras: Bundle?): CleverTapAPI? =
            fromAccountId(context, extras!!.getString(Constants.WZRK_ACCT_ID_KEY))

        /** The account id a CleverTap push / notification bundle was sent for, if readable. */
        private fun accountIdOf(notification: Bundle): String? = try {
            notification.getString(Constants.WZRK_ACCT_ID_KEY)
        } catch (t: Throwable) {
            null
        }

        private fun isDevelopmentMode(context: Context): Boolean =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        @RestrictTo(Scope.LIBRARY)
        @JvmStatic
        public fun runJobWork(context: Context?) {
            if (CleverTapInstanceStore.isEmpty()) {
                val instance = getDefaultInstance(context) ?: return
                if (instance.config.isBackgroundSync) {
                    // an instance exists only for a non-null context
                    instance.coreState.pushProviders.runPushAmpWork(context!!)
                } else {
                    Logger.d("Instance doesn't allow Background sync, not running the Job")
                }
                return
            }
            for (instance in CleverTapInstanceStore.all()) {
                if (instance.config.isAnalyticsOnly) {
                    Logger.d(instance.accountId, "Instance is Analytics Only not running the Job")
                    continue
                }
                if (!instance.config.isBackgroundSync) {
                    Logger.d(instance.accountId, "Instance doesn't allow Background sync, not running the Job")
                    continue
                }
                instance.coreState.pushProviders.runPushAmpWork(context!!)
            }
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun tokenRefresh(context: Context?, token: String?, pushType: PushType?) {
            for (instance in getAvailableInstances(context)) {
                instance.coreState.pushProviders.doTokenRefresh(token, pushType)
            }
        }

        /**
         * Register [CustomTemplates][com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate] through a
         * [TemplateProducer]. See [CustomTemplate.Builder][com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate.Builder].
         * Templates must be registered before the [CleverTapAPI] instance, that would use
         * them, is created. A common place for this initialization is in [Application.onCreate]. If your
         * application uses multiple [CleverTapAPI] instances, use the [CleverTapInstanceConfig] within the
         * TemplateProducer to differentiate which templates should be registered to which [CleverTapAPI]
         * instances.This method can be called multiple times with different TemplateProducers, however all of the
         * produced templates must have unique names.
         *
         * Example usage:
         *
         * Java:
         * ```
         * CleverTapAPI.registerCustomInAppTemplates(ctConfig -> CustomTemplatesExtKt.templatesSet(
         *                 new CustomTemplate.TemplateBuilder()
         *                         .name("template")
         *                         .presenter()
         *                         .stringArgument("string", "Text")
         *                         .build()));
         * ```
         * Kotlin:
         * ```
         * CleverTapAPI.registerCustomInAppTemplates {
         *     setOf(
         *         template {
         *             name("template")
         *             presenter()
         *             stringArgument("string", "Text")
         *         })
         * }
         * ```
         * @param producer The [TemplateProducer] that would create a set of templates
         */
        @JvmStatic
        public fun registerCustomInAppTemplates(producer: TemplateProducer) {
            synchronized(CleverTapAPI::class.java) {
                TemplatesManager.register(producer)
            }
        }

        /**
         * Register [CustomTemplates][com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate] through a
         * json definition. Templates must be registered before the [CleverTapAPI] instance, that would use
         * them, is created. A common place for this initialization is in [Application.onCreate]. If your
         * application uses multiple [CleverTapAPI] instances, extend [JsonTemplatesProducer]
         * with definitions for each instance and register using [registerCustomInAppTemplates].
         * Use the [CleverTapInstanceConfig] in [JsonTemplatesProducer.defineTemplates] to control
         * for which instance the templates should be registered.
         *
         * This method can be called multiple times with different json definitions and presenters,
         * however all of the templates must have unique names.
         *
         * @param templatesJson A string with the json definitions of templates. See
         *                      [JsonTemplatesProducer] for the json format.
         * @param templatesPresenter A presenter for all templates in the json definitions.
         *                           Required if there is at least one template with type "template".
         * @param functionsPresenter A presenter for all functions in the json definitions.
         *                           Required if there is at least one template with type "function".
         */
        @JvmStatic
        public fun registerCustomInAppTemplates(
            templatesJson: String,
            templatesPresenter: TemplatePresenter?,
            functionsPresenter: FunctionPresenter?
        ) {
            synchronized(CleverTapAPI::class.java) {
                TemplatesManager.register(JsonTemplatesProducer(templatesJson, templatesPresenter, functionsPresenter))
            }
        }

        /**
         * Returns the handler set with [setNotificationHandler].
         *
         * @return the handler, or `null` if none is set
         */
        @JvmStatic
        public fun getNotificationHandler(): NotificationHandler? = registeredNotificationHandler

        /**
         * Returns the handler set with [setSignedCallNotificationHandler].
         *
         * @return the handler, or `null` if none is set
         */
        @JvmStatic
        public fun getSignedCallNotificationHandler(): NotificationHandler? = registeredSignedCallNotificationHandler

        @RestrictTo(Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun addNotificationRenderedListener(
            id: String?,
            notificationRenderedListener: NotificationRenderedListener?
        ) {
            notificationRenderedListeners[id] = notificationRenderedListener
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun getNotificationRenderedListener(id: String?): NotificationRenderedListener? =
            notificationRenderedListeners[id]

        @RestrictTo(Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun removeNotificationRenderedListener(id: String?): NotificationRenderedListener? =
            notificationRenderedListeners.remove(id)

        // @RestrictTo keeps the BCV dump clean: @JvmName makes BCV treat this internal member as public.
        @RestrictTo(Scope.LIBRARY)
        @JvmStatic
        @JvmName("onActivityCreated")
        internal fun onActivityCreated(activity: Activity) {
            onActivityCreated(activity, null)
        }

        // static lifecycle callbacks
        // @RestrictTo keeps the BCV dump clean: @JvmName makes BCV treat this internal member as public.
        @RestrictTo(Scope.LIBRARY)
        @JvmStatic
        @JvmName("onActivityCreated")
        internal fun onActivityCreated(activity: Activity, cleverTapID: String?) {
            // make sure we have at least the default instance created here.
            if (CleverTapInstanceStore.isEmpty()) {
                createInstanceIfAvailable(activity.applicationContext, null, cleverTapID)
            }

            if (CleverTapInstanceStore.isEmpty()) {
                Logger.v("Instances is null in onActivityCreated!")
                return
            }

            // check for launch deep link
            val deepLink: Uri? = try {
                activity.intent.data
            } catch (t: Throwable) {
                null
            }
            var accountId: String? = deepLink?.let {
                try {
                    UriHelper.getAllKeyValuePairs(it.toString(), true).getString(Constants.WZRK_ACCT_ID_KEY)
                } catch (t: Throwable) {
                    null
                }
            }

            // check for launch via notification click
            val notification: Bundle? = try {
                activity.intent.extras
            } catch (t: Throwable) {
                null
            }
            var alreadyProcessedByCleverTap = false
            try {
                if (notification != null && !notification.isEmpty) {
                    @Suppress("DEPRECATION")
                    alreadyProcessedByCleverTap = notification.containsKey(Constants.WZRK_FROM_KEY) &&
                        Constants.WZRK_FROM == notification.get(Constants.WZRK_FROM_KEY)
                    if (alreadyProcessedByCleverTap) {
                        Logger.v(
                            "ActivityLifecycleCallback: Notification Clicked already processed for " +
                                "$notification, dropping duplicate."
                        )
                    }
                    if (notification.containsKey(Constants.WZRK_ACCT_ID_KEY)) {
                        @Suppress("DEPRECATION")
                        accountId = notification.get(Constants.WZRK_ACCT_ID_KEY) as String?
                    }
                }
            } catch (t: Throwable) {
                // Ignore
            }

            if (alreadyProcessedByCleverTap && deepLink == null) {
                return
            }

            try {
                for (instance in CleverTapInstanceStore.all()) {
                    instance.coreState.activityLifeCycleManager.onActivityCreated(notification, deepLink, accountId)
                }
            } catch (t: Throwable) {
                Logger.v("Throwable - ${t.localizedMessage}")
            }
        }

        private fun createInstanceIfAvailable(context: Context?, accountId: String?): CleverTapAPI? =
            createInstanceIfAvailable(context, accountId, null)

        private fun createInstanceIfAvailable(
            context: Context?,
            accountId: String?,
            cleverTapID: String?
        ): CleverTapAPI? = try {
            if (accountId == null) {
                defaultInstanceOrNull { getDefaultInstance(context, cleverTapID) }
            } else {
                // Without a context or stored config there is nothing to create (Java hit an NPE and
                // returned null from the catch below).
                val appContext = context ?: return null
                val configJson = StorageHelper.getString(appContext, "instance:$accountId", "") ?: return null
                if (configJson.isNotEmpty()) {
                    val config = CleverTapInstanceConfig.createInstanceFromJson(configJson)
                    Logger.v("Inflated Instance Config: $configJson")
                    config?.let { instanceWithConfig(appContext, it, cleverTapID) }
                } else {
                    defaultInstanceOrNull {
                        getDefaultInstance(appContext)?.takeIf { it.coreState.config.accountId == accountId }
                    }
                }
            }
        } catch (t: Throwable) {
            null
        }

        private inline fun defaultInstanceOrNull(create: () -> CleverTapAPI?): CleverTapAPI? = try {
            create()
        } catch (t: Throwable) {
            Logger.v("Error creating shared Instance: ", t.cause)
            null
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun getAvailableInstances(context: Context?): ArrayList<CleverTapAPI> {
            if (CleverTapInstanceStore.isEmpty()) {
                return ArrayList<CleverTapAPI>().apply { getDefaultInstance(context)?.let { add(it) } }
            }
            return ArrayList(CleverTapInstanceStore.all())
        }

        /**
         * Creates default [CleverTapInstanceConfig] object and returns it
         *
         * @param context The Android context
         * @return The [CleverTapInstanceConfig] object
         */
        private fun getDefaultConfig(context: Context?): CleverTapInstanceConfig? {
            val manifest = ManifestInfo.getInstance(context)
            val accountId = manifest.accountId
            val accountToken = manifest.accountToken
            val accountRegion = manifest.accountRegion
            val proxyDomain = manifest.proxyDomain
            val spikyProxyDomain = manifest.spikeyProxyDomain
            val handshakeDomain = manifest.handshakeDomain
            if (accountId == null || accountToken == null) {
                Logger.i(
                    "Account ID or Account token is missing from AndroidManifest.xml, unable to create default instance"
                )
                return null
            }
            if (accountRegion == null) {
                Logger.i("Account Region not specified in the AndroidManifest - using default region")
            }

            // todo : pass manifest info here
            // Same as the protected CleverTapInstanceConfig.createDefaultInstance (not visible from Kotlin here).
            val defaultInstanceConfig = CleverTapInstanceConfig.createInstanceWithManifest(
                ManifestInfo.getInstance(context), accountId, accountToken, accountRegion, true
            )

            // todo : check if these re-assignments are needed, already added in manifest parsing
            if (!proxyDomain.isNullOrBlank()) {
                defaultInstanceConfig.proxyDomain = proxyDomain
            }
            if (!spikyProxyDomain.isNullOrBlank()) {
                defaultInstanceConfig.spikyProxyDomain = spikyProxyDomain
            }
            if (!handshakeDomain.isNullOrBlank()) {
                defaultInstanceConfig.customHandshakeDomain = handshakeDomain
            }
            return defaultInstanceConfig
        }

        private fun getDefaultInstanceOrFirstOther(context: Context?): CleverTapAPI? =
            getDefaultInstance(context) ?: CleverTapInstanceStore.all().firstOrNull()

        /**
         * Sets the handler that receives Push Template payloads (those with a `pt_id`), for example
         * `PushTemplateNotificationHandler`.
         *
         * @param notificationHandler the handler, or `null` to remove it
         */
        @JvmStatic
        public fun setNotificationHandler(notificationHandler: NotificationHandler?) {
            registeredNotificationHandler = notificationHandler
        }

        /**
         * Sets a factory that lets the client fully build the notification for CleverTap
         * **Live Update** pushes — those carrying `wzrk_la`. When set, the SDK invokes
         * [ICleverTapLiveUpdateNotificationFactory.onCreateNotification] for such pushes instead of
         * its built-in renderers; pushes with a `pt_id` are still rendered by Push Templates (when
         * delivered through `CTFcmMessageHandler`), and ordinary pushes are unaffected.
         *
         * The client builds the [android.app.Notification]. The SDK still owns display, the
         * notification channel (created if missing), the notification id (derived from
         * `wzrk_activityId` for in-place updates), lifecycle/viewed analytics, and deduplication.
         *
         * Call this in [Application.onCreate]: a push can start the app without any Activity.
         *
         * @param factory The factory implementation, or `null` to revert to default rendering.
         */
        @JvmStatic
        public fun setLiveUpdateNotificationFactory(factory: ICleverTapLiveUpdateNotificationFactory?) {
            liveUpdateNotificationFactory = factory
        }

        /**
         * Returns the factory set with [setLiveUpdateNotificationFactory].
         *
         * @return the factory, or `null` if none is set
         */
        @JvmStatic
        public fun getLiveUpdateNotificationFactory(): ICleverTapLiveUpdateNotificationFactory? =
            liveUpdateNotificationFactory

        /**
         * Sets the handler that receives Signed Call push payloads. Used by the Signed Call SDK.
         *
         * @param notificationHandler the handler, or `null` to remove it
         */
        @JvmStatic
        public fun setSignedCallNotificationHandler(notificationHandler: NotificationHandler?) {
            registeredSignedCallNotificationHandler = notificationHandler
        }

        /**
         * Retrieves a notification bitmap with a specified timeout and size constraint.
         *
         * @param context           The context of the application. Must be non null.
         * @param bundle            The [Bundle] object received by the push receiver. Must be non null.
         * @param bitmapSrcUrl      The URL of the bitmap to download.
         * @param fallbackToAppIcon Specifies whether to fallback to the app icon if the bitmap is not available.
         * @param timeoutInMillis   The timeout duration for the bitmap download in milliseconds.  Must be in range of 1 - 20000.
         * @param sizeInBytes       The maximum size of the bitmap in bytes. Must be greater than 0.
         * @return The downloaded bitmap or null if it couldn't be downloaded or doesn't exist.
         *
         * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
         *      Note: This method must be called on background thread.
         * </p>
         */
        @JvmStatic
        public fun getNotificationBitmapWithTimeoutAndSize(
            context: Context?, bundle: Bundle?, bitmapSrcUrl: String?,
            fallbackToAppIcon: Boolean, timeoutInMillis: Long, sizeInBytes: Int
        ): Bitmap? {
            if (checkNotificationBitmapRequestInvalid(context, bundle, timeoutInMillis)) {
                return null
            }

            if (sizeInBytes < 1) {
                Logger.v("Given sizeInBytes is less than 1 bytes. Not downloading bitmap!")
                return null
            }

            val cleverTapAPI = fromBundle(context, bundle)
            if (cleverTapAPI == null) {
                Logger.v("cleverTapAPI is null. Not downloading bitmap!")
                return null
            }

            if (!cleverTapAPI.coreState.networkMonitor.isNetworkOnline()) {
                Logger.v("Network unavailable. Not downloading notification bitmap!")
                return null
            }

            return Utils.getNotificationBitmapWithTimeoutAndSize(
                bitmapSrcUrl, fallbackToAppIcon, context, cleverTapAPI.config,
                timeoutInMillis, sizeInBytes
            ).bitmap
        }

        private fun checkNotificationBitmapRequestInvalid(
            context: Context?,
            bundle: Bundle?,
            timeoutInMillis: Long
        ): Boolean {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Logger.v("Notification Bitmap Download is not allowed on main thread")
                return true
            }
            if (context == null) {
                Logger.v("Given Context is null. Not downloading bitmap!")
                return true
            }
            if (bundle == null) {
                Logger.v("Given Bundle is null. Not downloading bitmap!")
                return true
            }
            if (timeoutInMillis < 1) {
                Logger.v("Given timeoutInMillis is less than 1 millis. Not downloading bitmap!")
                return true
            }
            if (timeoutInMillis > MAX_BITMAP_DOWNLOAD_TIMEOUT_MILLIS) {
                Logger.v("Given timeoutInMillis exceeds 20 secs limit. Not downloading bitmap!")
                return true
            }
            return false
        }

        /**
         * Retrieves a notification bitmap with a specified timeout.
         *
         * @param context           The context of the application. Must be non null.
         * @param bundle            The [Bundle] object received by the push receiver. Must be non null.
         * @param bitmapSrcUrl      The URL of the bitmap to download.
         * @param fallbackToAppIcon Specifies whether to fallback to the app icon if the bitmap is not available.
         * @param timeoutInMillis   The timeout duration for the bitmap download in milliseconds.  Must be in range of 1 - 20000.
         * @return The downloaded bitmap or null if it couldn't be downloaded or doesn't exist.
         *
         * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
         * Note: This method must be called on background thread.
         * </p>
         */
        @JvmStatic
        public fun getNotificationBitmapWithTimeout(
            context: Context?, bundle: Bundle?, bitmapSrcUrl: String?,
            fallbackToAppIcon: Boolean, timeoutInMillis: Long
        ): Bitmap? {
            if (checkNotificationBitmapRequestInvalid(context, bundle, timeoutInMillis)) {
                return null
            }

            val cleverTapAPI = fromBundle(context, bundle)
            if (cleverTapAPI == null) {
                Logger.v("cleverTapAPI is null. Not downloading bitmap!")
                return null
            }

            if (!cleverTapAPI.coreState.networkMonitor.isNetworkOnline()) {
                Logger.v("Network unavailable. Not downloading notification bitmap!")
                return null
            }

            return Utils.getNotificationBitmapWithTimeout(
                bitmapSrcUrl, fallbackToAppIcon, context, cleverTapAPI.config,
                timeoutInMillis
            ).bitmap
        }
    }
}
