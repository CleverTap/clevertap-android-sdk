package com.clevertap.android.sdk.events

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.FailureFlushListener
import com.clevertap.android.sdk.LocalDataStore
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.SessionManager
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.FlattenedEventData.EventProperties
import com.clevertap.android.sdk.events.FlattenedEventData.NoData
import com.clevertap.android.sdk.events.FlattenedEventData.ProfileChanges
import com.clevertap.android.sdk.login.IdentityRepoFactory
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.network.NetworkMonitor
import com.clevertap.android.sdk.profile.ProfileStateTraverser.Companion.toNestedMap
import com.clevertap.android.sdk.task.CTExecutorFactory.executors
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.validation.ValidationResultStack
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.TimeZone
import java.util.concurrent.Future

internal class EventQueueManager(
    private val baseDatabaseManager: BaseDatabaseManager,
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val eventMediator: EventMediator,
    private val sessionManager: SessionManager,
    callbackManager: BaseCallbackManager,
    private val mainLooperHandler: MainLooperHandler,
    private val deviceInfo: DeviceInfo,
    private val validationResultStack: ValidationResultStack,
    private val networkManager: NetworkManager,
    private val cleverTapMetaData: CoreMetaData,
    private val ctLockManager: CTLockManager,
    private val localDataStore: LocalDataStore,
    private val controllerManager: ControllerManager,
    private val loginInfoProvider: LoginInfoProvider, private val networkMonitor: NetworkMonitor
) : BaseEventQueueManager(), FailureFlushListener {
    private var commsRunnable: Runnable? = null

    private val logger: Logger = config.logger


    private var pushNotificationViewedRunnable: Runnable? = null


    init {
        callbackManager.setFailureFlushListener(this)
        logger.verbose(config.accountId, "EventQueueManager: registering network restore callback")
        networkManager.setNetworkRestoreCallback {
            logger.debug(config.accountId, "EventQueueManager: network restored, triggering flush for all event groups")
            flushQueueAsync(context, EventGroup.REGULAR)
            logger.verbose(config.accountId, "EventQueueManager: flush triggered for REGULAR events")
            flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED)
            logger.verbose(config.accountId, "EventQueueManager: flush triggered for PUSH_NOTIFICATION_VIEWED events")
        }
        logger.verbose(config.accountId, "EventQueueManager: network restore callback registered")
    }

    // only call async
    override fun addToQueue(
        context: Context,
        event: JSONObject,
        eventType: Int,
        flattenedEventData: FlattenedEventData
    ) {
        when (eventType) {
            Constants.NV_EVENT -> {
                config.logger.verbose(
                    config.accountId,
                    "Pushing Notification Viewed event onto separate queue"
                )
                processPushNotificationViewedEvent(context, event, eventType, flattenedEventData)
            }
            Constants.DEFINE_VARS_EVENT -> processDefineVarsEvent(context, event)
            else -> processEvent(context, event, eventType, flattenedEventData)
        }
    }

    @WorkerThread
    private fun processDefineVarsEvent(context: Context, event: JSONObject?) {
        sendImmediately(context, EventGroup.VARIABLES, event)
    }

    override fun failureFlush(context: Context) {
        scheduleQueueFlush(context)
    }

    override fun flush() {
        flushQueueAsync(context, EventGroup.REGULAR)
    }

    override fun flushQueueAsync(context: Context, eventGroup: EventGroup) {
        val task = executors(config).postAsyncSafelyTask<Void?>()
        task.execute("CommsManager#flushQueueAsync") {
                if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
                    logger.verbose(config.accountId, "Pushing Notification Viewed event onto queue flush sync")
                } else {
                    logger.verbose(config.accountId, "Pushing event onto queue flush sync")
                }
                flushQueueSync(context, eventGroup)
                 null
            }
    }

    /**
     * Flushes the events queue synchronously with a default null value for the caller.
     * This is an overloaded method that internally calls [EventQueueManager.flushQueueSync].
     *
     * @param context     The Context object.
     * @param eventGroup  The EventGroup for which the queue needs to be flushed.
     */
    override fun flushQueueSync(context: Context, eventGroup: EventGroup) {
        flushQueueSync(context, eventGroup, null)
    }

    /**
     * Flushes the events queue synchronously, checking network connectivity, offline mode, and performing handshake if necessary.
     *
     * @param context     The Context object.
     * @param eventGroup  The EventGroup for which the queue needs to be flushed.
     * @param caller      The optional caller identifier.
     */
    override fun flushQueueSync(context: Context, eventGroup: EventGroup, caller: String?) {
        flushQueueSync(context, eventGroup, caller, false)
    }

    override fun flushQueueSync(
        context: Context,
        eventGroup: EventGroup,
        caller: String?,
        isUserSwitchFlush: Boolean
    ) {
        // Check if network connectivity is available
        if (!networkManager.isNetworkOnline()) {
            logger.verbose(
                config.accountId,
                "Network connectivity unavailable. Will retry later"
            )
            controllerManager.invokeCallbacksForNetworkError()
            controllerManager.invokeBatchListener(JSONArray(), false)
            return
        }

        // Check if CleverTap instance is set to offline mode
        if (cleverTapMetaData.isOffline) {
            logger.debug(
                config.accountId,
                "CleverTap Instance has been set to offline, won't send events queue"
            )
            controllerManager.invokeCallbacksForNetworkError()
            controllerManager.invokeBatchListener(JSONArray(), false)
            return
        }

        // Check if handshake is required for the domain associated with the event group
        if (networkManager.needsHandshakeForDomain(eventGroup)) {
            networkManager.initHandshake(eventGroup) {
                networkManager.flushDBQueue(context, eventGroup, caller, isUserSwitchFlush)
            }
        } else {
            logger.verbose(
                config.accountId,
                "Pushing Notification Viewed event onto queue DB flush"
            )

            // No handshake required, directly flush the DB queue
            networkManager.flushDBQueue(context, eventGroup, caller, isUserSwitchFlush)
        }
    }

    /**
     * This method is currently used only for syncing of variables. If you find it appropriate you
     * can add handling of network error similar to flushQueueSync, also check return value of
     * sendQueue for success.
     */
    override fun sendImmediately(context: Context, eventGroup: EventGroup, eventData: JSONObject?) {
        if (!networkManager.isNetworkOnline()) {
            logger.verbose(
                config.accountId,
                "Network connectivity unavailable. Event won't be sent."
            )
            return
        }

        if (cleverTapMetaData.isOffline) {
            logger.debug(
                config.accountId,
                "CleverTap Instance has been set to offline, won't send event"
            )
            return
        }

        val singleEventQueue = JSONArray().put(eventData)

        if (networkManager.needsHandshakeForDomain(eventGroup)) {
            networkManager.initHandshake(eventGroup) {
                networkManager.sendQueue(context, eventGroup, singleEventQueue, null, false)
            }
        } else {
            networkManager.sendQueue(context, eventGroup, singleEventQueue, null, false)
        }
    }

    val now: Int
        get() = (System.currentTimeMillis() / 1000).toInt()

    fun processEvent(
        context: Context,
        event: JSONObject,
        eventType: Int,
        flattenedEventData: FlattenedEventData
    ) {
        synchronized(ctLockManager.eventLock) {
            try {
                if (CoreMetaData.getActivityCount() == 0) {
                    CoreMetaData.setActivityCount(1)
                }
                val type: String?
                if (eventType == Constants.PAGE_EVENT) {
                    type = "page"
                } else if (eventType == Constants.PING_EVENT) {
                    type = "ping"
                    attachMeta(event, context)
                    if (event.has("bk")) {
                        cleverTapMetaData.isBgPing = true
                        event.remove("bk")
                    }

                    //Add a flag to denote, PING event is for geofence
                    if (cleverTapMetaData.isLocationForGeofence) {
                        event.put("gf", true)
                        cleverTapMetaData.isLocationForGeofence = false
                        event.put("gfSDKVersion", cleverTapMetaData.geofenceSDKVersion)
                        cleverTapMetaData.geofenceSDKVersion = 0
                    }
                } else if (eventType == Constants.PROFILE_EVENT) {
                    type = "profile"
                } else if (eventType == Constants.DATA_EVENT) {
                    type = "data"
                } else {
                    type = "event"
                }

                // Complete the received event with the other params
                val currentActivityName = cleverTapMetaData.screenName
                if (currentActivityName != null) {
                    event.put("n", currentActivityName)
                }

                val session = cleverTapMetaData.currentSessionId
                event.put("s", session)
                event.put("pg", CoreMetaData.getActivityCount())
                event.put("type", type)
                event.put("ep", this.now)
                event.put("f", cleverTapMetaData.isFirstSession)
                event.put("lsl", cleverTapMetaData.lastSessionLength)
                attachPackageNameIfRequired(context, event)

                // Report any pending validation error
                val vr = validationResultStack.popValidationResult()
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                }
                localDataStore.setDataSyncFlag(event)
                baseDatabaseManager.queueEventToDB(context, event, eventType)

                initInAppEvaluation(context, event, eventType, flattenedEventData)

                scheduleQueueFlush(context)
            } catch (e: Throwable) {
                config.logger.verbose(config.accountId, "Failed to queue event: $event", e)
            }
        }
    }

    fun initInAppEvaluation(
        context: Context?,
        event: JSONObject,
        eventType: Int,
        flattenedEventData: FlattenedEventData
    ) {
        val eventName = eventMediator.getEventName(event)
        val userLocation = cleverTapMetaData.locationFromUser
        updateLocalStore(eventName, eventType)

        config.logger.verbose(config.accountId, "FlattenedEventData : $flattenedEventData")

        // Early return for no-data scenarios
        if (flattenedEventData is NoData) {
            return
        }

        val inAppController = controllerManager.inAppController

        // Handle charged events (highest priority, independent of network state)
        if (eventMediator.isChargedEvent(event)) {
            inAppController.onQueueChargedEvent(
                eventMediator.getChargedEventDetails(event),
                eventMediator.getChargedEventItemDetails(event),
                userLocation
            )
            return
        }

        // Handle profile changes
        if (flattenedEventData is ProfileChanges) {
            val flattenedProfileChanges = flattenedEventData.changes
            val userAttributeChangedProperties = flattenedProfileChanges.toNestedMap()
            inAppController.onQueueProfileEvent(userAttributeChangedProperties, userLocation)
            return
        }

        val flattenedEventProps = (flattenedEventData as EventProperties).properties
        val isOffline = !networkMonitor.isNetworkOnline()
        val isRegularEvent = eventMediator.isEvent(event)
        val isAppLaunchedEvent = eventMediator.isAppLaunchedEvent(event)

        // Queue event if: (offline AND is regular event) OR (online AND NOT app launched event)
        if ((isOffline && isRegularEvent) || (!isOffline && !isAppLaunchedEvent)) {
            inAppController.onQueueEvent(eventName, flattenedEventProps, userLocation)
        }
    }

    fun processPushNotificationViewedEvent(
        context: Context,
        event: JSONObject,
        eventType: Int,
        flattenedEventData: FlattenedEventData
    ) {
        synchronized(ctLockManager.eventLock) {
            try {
                val session = cleverTapMetaData.currentSessionId
                event.put("s", session)
                event.put("type", "event")
                event.put("ep", this.now)
                // Report any pending validation error
                val vr = validationResultStack.popValidationResult()
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                }
                config.logger
                    .verbose(config.accountId, "Pushing Notification Viewed event onto DB")
                baseDatabaseManager.queuePushNotificationViewedEventToDB(context, event)
                initInAppEvaluation(context, event, eventType, flattenedEventData)
                config.logger
                    .verbose(
                        config.accountId,
                        "Pushing Notification Viewed event onto queue flush"
                    )
                schedulePushNotificationViewedQueueFlush(context)
            } catch (t: Throwable) {
                config.logger.verbose(config.accountId, "Failed to queue notification viewed event: $event", t)
            }
        }
    }

    //Profile
    override fun pushBasicProfile(
        baseProfile: JSONObject?,
        removeFromSharedPrefs: Boolean,
        profileChanges: FlattenedEventData
    ) {
        try {
            val guid = this.cleverTapID

            val profileEvent = JSONObject()

            if (baseProfile != null && baseProfile.length() > 0) {
                val i = baseProfile.keys()
                val iProfileHandler = IdentityRepoFactory
                    .getRepo(context, config, validationResultStack)
                while (i.hasNext()) {
                    val next = i.next()

                    // need to handle command-based JSONObject props here now
                    var value: Any? = null
                    try {
                        value = baseProfile.getJSONObject(next)
                    } catch (_: Throwable) {
                        try {
                            value = baseProfile.get(next)
                        } catch (_: JSONException) {
                            //no-op
                        }
                    }

                    if (value != null) {
                        profileEvent.put(next, value)

                        // cache the valid identifier: guid pairs
                        val isProfileKey = iProfileHandler.hasIdentity(next)

                        /*If key is present in IdentitySet and removeFromSharedPrefs is true then
                        proceed to removing PII key(Email) from shared prefs*/
                        if (isProfileKey && !deviceInfo.isErrorDeviceId()) {
                            try {
                                if (removeFromSharedPrefs) {
                                    // Remove the value associated with the GUID
                                    loginInfoProvider.removeValueFromCachedGUIDForIdentifier(
                                        guid,
                                        next
                                    )
                                } else {
                                    // Cache the new value for the GUID
                                    loginInfoProvider.cacheGUIDForIdentifier(
                                        guid,
                                        next,
                                        value.toString()
                                    )
                                }
                            } catch (_: Throwable) {
                                // Log or handle the exception if needed; currently no-op
                            }
                        }
                    }
                }
            }

            try {
                val carrier = deviceInfo.carrier
                if (carrier != null && carrier != "") {
                    profileEvent.put("Carrier", carrier)
                }

                val cc = deviceInfo.countryCode
                if (cc != null && cc != "") {
                    profileEvent.put("cc", cc)
                }

                profileEvent.put("tz", TimeZone.getDefault().id)

                val event = JSONObject()
                event.put("profile", profileEvent)
                queueEvent(context, event, Constants.PROFILE_EVENT, profileChanges)
            } catch (_: JSONException) {
                config.logger
                    .verbose(
                        config.accountId,
                        "FATAL: Creating basic profile update event failed!"
                    )
            }
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Basic profile sync", t)
        }
    }

    override fun pushInitialEventsAsync() {
        if (!cleverTapMetaData.inCurrentSession()) {
            val task = executors(config).postAsyncSafelyTask<Void?>()
            task.execute("CleverTapAPI#pushInitialEventsAsync") {
                try {
                    config.logger.verbose(config.accountId, "Queuing daily events")
                    pushBasicProfile(null, false, NoData)
                } catch (t: Throwable) {
                    config.logger
                        .verbose(config.accountId, "Daily profile sync failed", t)
                }
                null
            }
        }
    }

    /**
     * Adds a new event to the queue, to be sent later.
     *
     * @param context   The Android context
     * @param event     The event to be queued
     * @param eventType The type of event to be queued
     */
    override fun queueEvent(context: Context, event: JSONObject, eventType: Int): Future<*>? {
        return queueEvent(context, event, eventType, NoData)
    }

    /**
     * Adds a new event to the queue, to be sent later.
     *
     * @param context   The Android context
     * @param event     The event to be queued
     * @param eventType The type of event to be queued
     * @param flattenedEventData eventData to be used for InApp evaluation
     */
    override fun queueEvent(
        context: Context,
        event: JSONObject,
        eventType: Int,
        flattenedEventData: FlattenedEventData
    ): Future<*>? {
        val task = executors(config).postAsyncSafelyTask<Void?>()
        return task.submit("queueEvent") {
                if (eventMediator.shouldDropEvent(event, eventType)) {
                    return@submit null
                }
                if (eventMediator.shouldDeferProcessingEvent(event, eventType)) {
                    config.logger.debug(
                        config.accountId,
                        "App Launched not yet processed, re-queuing event " + event + "after 2s"
                    )
                    mainLooperHandler.postDelayed( {
                        val task1 = executors(config).postAsyncSafelyTask<Void?>()
                        task1.execute("queueEventWithDelay") {
                                sessionManager.lazyCreateSession(context)
                                pushInitialEventsAsync()
                                addToQueue(context, event, eventType, flattenedEventData)
                                null
                        }
                    }, 2000)
                } else {
                    if (eventType == Constants.FETCH_EVENT || eventType == Constants.NV_EVENT) {
                        addToQueue(context, event, eventType, flattenedEventData)
                    } else {
                        sessionManager.lazyCreateSession(context)
                        pushInitialEventsAsync()
                        addToQueue(context, event, eventType, flattenedEventData)
                    }
                }
                null
            }
    }

    override fun scheduleQueueFlush(context: Context) {
        if (commsRunnable == null) {
            commsRunnable = Runnable {
                    flushQueueAsync(context, EventGroup.REGULAR)
                    flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED)
            }
        }
        // Cancel any outstanding send runnable, and issue a new delayed one
        mainLooperHandler.removeCallbacks(commsRunnable!!)

        mainLooperHandler.postDelayed(commsRunnable!!, networkManager.getDelayFrequency().toLong())

        logger.verbose(config.accountId, "Scheduling delayed queue flush on main event loop")
    }

    /**
     * Attaches meta info about the current state of the device to an event.
     * Typically, this meta is added only to the ping event.
     */
    private fun attachMeta(o: JSONObject, context: Context?) {
        // Memory consumption
        try {
            o.put("mc", Utils.getMemoryConsumption())
        } catch (_: Throwable) {
            // Ignore
        }

        // Attach the network type
        try {
            val networkType = networkMonitor.getNetworkTypeString()
            if (networkType != null) {
                o.put("nt", networkType)
            }
        } catch (_: Throwable) {
            // Ignore
        }
    }

    //Session
    private fun attachPackageNameIfRequired(context: Context, event: JSONObject) {
        try {
            val type = event.getString("type")
            // Send it only for app launched events
            if ("event" == type && Constants.APP_LAUNCHED_EVENT == event.getString("evtName")) {
                event.put("pai", context.packageName)
            }
        } catch (_: Throwable) {
            // Ignore
        }
    }

    private val cleverTapID: String?
        get() = deviceInfo.deviceID

    private fun schedulePushNotificationViewedQueueFlush(context: Context) {
        if (pushNotificationViewedRunnable == null) {
            pushNotificationViewedRunnable = Runnable {
                    config.logger.verbose(
                            config.accountId,
                            "Pushing Notification Viewed event onto queue flush async"
                        )
                    flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED)
            }
        }
        mainLooperHandler.removeCallbacks(pushNotificationViewedRunnable!!)
        mainLooperHandler.post(pushNotificationViewedRunnable!!)
    }

    @WorkerThread
    private fun updateLocalStore(eventName: String?, type: Int) {
        if (type == Constants.RAISED_EVENT) {
            localDataStore.persistUserEventLog(eventName)
        }
    }
}