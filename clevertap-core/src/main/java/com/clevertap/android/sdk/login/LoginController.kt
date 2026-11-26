package com.clevertap.android.sdk.login

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.LocalDataStore
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.SessionManager
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.inbox.InboxV2Bridge
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.network.fetch.FetchTrigger
import com.clevertap.android.sdk.product_config.CTProductConfigFactory
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.validation.ValidationResultStack
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class LoginController(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo,
    private val validationResultStack: ValidationResultStack,
    private val baseEventQueueManager: BaseEventQueueManager,
    private val analyticsManager: AnalyticsManager,
    private val coreMetaData: CoreMetaData,
    private val controllerManager: ControllerManager,
    private val sessionManager: SessionManager,
    private val localDataStore: LocalDataStore,
    private val callbackManager: BaseCallbackManager,
    private val dbManager: DBManager,
    private val ctLockManager: CTLockManager,
    private val loginInfoProvider: LoginInfoProvider,
    private val contentFetchManager: ContentFetchManager,
    private val inboxV2Bridge: InboxV2Bridge
) {

    private var cachedGUID: String? = null

    private val pushProviders: PushProviders? = controllerManager.pushProviders

    open fun asyncProfileSwitchUser(profile: Map<String, Any?>?, cacheGuid: String?, cleverTapID: String?) {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("resetProfile") {
            try {
                // Logs the cachedGUID field rather than the cacheGuid param, as the Java original did.
                val cachedGuidLog = if (cacheGuid != null) { cachedGUID } else { "NULL and cleverTapID $cleverTapID" }
                config.logger.verbose(
                    config.accountId,
                    "asyncProfileSwitchUser:[profile $profile with Cached GUID $cachedGuidLog"
                )
                //set optOut to false on the current user to unregister the device token
                coreMetaData.isCurrentUserOptedOut = false
                // unregister the device token on the current user
                checkNotNull(pushProviders).forcePushDeviceToken(false)

                // try and flush and then reset the queues
                baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR, null, true)
                baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED, null, true)
                // Drop any in-flight app-launch arbitration window so the previous user's buffered
                // in-app is never shown to the new user. Must run BEFORE cancelAllResponseJobs():
                // cancelling the fetch coroutine fires its completion signal, which would otherwise
                // display that buffered in-app.
                controllerManager.inAppController?.abandonAppLaunchArbitration()
                contentFetchManager.cancelAllResponseJobs()
                cancelScheduledInApps()
                dbManager.clearQueues(context)

                // clear out the old data
                CoreMetaData.setActivityCount(1)
                sessionManager.destroySession()

                // either force restore the cached GUID or generate a new one
                switchDeviceId(cacheGuid = cacheGuid, cleverTapID = cleverTapID)

                localDataStore.changeUser()
                callbackManager.notifyUserProfileInitialized(deviceInfo.deviceID)

                // Restore state of opt out and system events from storage
                deviceInfo.setCurrentUserOptOutStateFromStorage()
                deviceInfo.setSystemEventsAllowedStateFromStorage()

                // variables for new user are fetched with App Launched
                resetVariables()
                analyticsManager.forcePushAppLaunchedEvent()
                if (profile != null) {
                    analyticsManager.pushProfile(profile)
                }
                checkNotNull(pushProviders).forcePushDeviceToken(true)
                resetInbox()
                schedulePostSwitchInboxFetch()
                resetFeatureFlags()
                resetProductConfigs()
                recordDeviceIDErrors()
                resetDisplayUnits()

                notifyChangeUserCallback()

                checkNotNull(controllerManager.inAppFCManager).changeUser(deviceInfo.deviceID)
                // Native Display (ND) frequency caps (SDK-6055): repoint ND counters to the new
                // user too. Null-guarded — ndFCManager can still be null on the async device-id path.
                controllerManager.ndFCManager?.changeUser()
            } catch (t: Throwable) {
                config.logger.verbose(config.accountId, "Reset Profile error", t)
            }
            null
        }
    }

    private fun switchDeviceId(cacheGuid: String?, cleverTapID: String?) {
        when {
            cacheGuid != null -> {
                deviceInfo.forceUpdateDeviceId(cacheGuid)
                callbackManager.notifyUserProfileInitialized(cacheGuid)
            }

            config.enableCustomCleverTapId -> deviceInfo.forceUpdateCustomCleverTapID(cleverTapID)
            else -> deviceInfo.forceNewDeviceID()
        }
    }

    open fun notifyChangeUserCallback() {
        val changeUserCallbackList = callbackManager.changeUserCallbackList
        synchronized(changeUserCallbackList) {
            for (callback in changeUserCallbackList) {
                callback?.onChangeUser(checkNotNull(deviceInfo.deviceID), config.accountId)
            }
        }
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    open fun onUserLogin(profile: Map<String, Any?>?, cleverTapID: String?) {
        if (config.enableCustomCleverTapId && cleverTapID == null) {
            Logger.i(
                "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml Please call onUserlogin() and pass a custom CleverTap ID"
            )
        } else if (!config.enableCustomCleverTapId && cleverTapID != null) {
            Logger.i(
                "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml Please call CleverTapAPI.defaultInstance() without a custom CleverTap ID"
            )
        }
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("_onUserLogin") {
            onUserLoginInternal(profile, cleverTapID)
            null
        }
    }

    open fun recordDeviceIDErrors() {
        for (validationResult in deviceInfo.drainValidationResults()) {
            validationResultStack.pushValidationResult(validationResult)
        }
    }

    private fun onUserLoginInternal(profile: Map<String, Any?>?, cleverTapID: String?) {
        if (profile == null) {
            return
        }

        try {
            val currentGUID = deviceInfo.deviceID ?: return

            var haveIdentifier = false

            // check for valid identifier keys
            // use the first one we find
            val iProfileHandler = IdentityRepoFactory.getRepo(context, config, validationResultStack)
            for ((key, value) in profile) {
                if (!iProfileHandler.hasIdentity(key)) {
                    continue
                }
                try {
                    if (value !is Number && value !is String && value !is Boolean) {
                        config.logger.debug(
                            config.accountId,
                            "onUserLogin: Aborting the operation. Non-primitive value for the identifier key = $key"
                        )
                        return
                    }
                    val identifier = value.toString()
                    if (identifier.isNotEmpty()) {
                        haveIdentifier = true
                        cachedGUID = loginInfoProvider.getGUIDForIdentifier(key, identifier)
                        if (cachedGUID != null) {
                            break
                        }
                    }
                } catch (t: Throwable) {
                    // no-op
                }
            }

            // if no valid identifier provided or there are no identified users on the device; just push on the current profile
            if (!deviceInfo.isErrorDeviceId && (!haveIdentifier || loginInfoProvider.isAnonymousDevice)) {
                config.logger.debug(
                    config.accountId,
                    "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile"
                )
                analyticsManager.pushProfile(profile)
                return
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID == currentGUID) {
                config.logger.debug(
                    config.accountId,
                    "onUserLogin: $profile maps to current device id $currentGUID pushing on current profile"
                )
                analyticsManager.pushProfile(profile)
                return
            }

            config.logger.verbose(
                config.accountId,
                "onUserLogin: queuing reset profile for $profile with Cached GUID ${cachedGUID ?: "NULL"}"
            )

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID)
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "onUserLogin failed", t)
        }
    }

    /**
     * Resets the Display Units in the cache
     */
    private fun resetDisplayUnits() {
        val cache = controllerManager.displayUnitCache
        if (cache != null) {
            cache.reset()
        } else {
            config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Can't reset Display Units, DisplayUnitcontroller is null"
            )
        }
    }

    private fun resetFeatureFlags() {
        val ctFeatureFlagsController = controllerManager.ctFeatureFlagsController
        if (ctFeatureFlagsController != null && ctFeatureFlagsController.isInitialized) {
            ctFeatureFlagsController.resetWithGuid(deviceInfo.deviceID)
            ctFeatureFlagsController.fetchFeatureFlags()
        } else {
            config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Can't reset Display Units, CTFeatureFlagsController is null"
            )
        }
    }

    // always call async
    private fun resetInbox() {
        synchronized(ctLockManager.inboxControllerLock) {
            controllerManager.ctInboxController = null
        }
        controllerManager.initializeInbox()
        // New user gets an independent fetch window — clear the previous user's throttle timestamp.
        inboxV2Bridge.resetThrottle()
    }

    /**
     * Enqueues a post-user-switch inbox fetch on the PostAsyncSafely executor.
     *
     * Ordering guarantee: PostAsyncSafely is a single-thread executor.
     * By the time this task runs, the "profilePush" task queued by
     * `analyticsManager.pushProfile` will already have completed,
     * so new-user events are in the DB. Flushing synchronously here lets
     * the server return the new user's ARP *before*
     * [InboxV2Bridge.submit] builds the inbox FETCH request header.
     * Without this flush the first FETCH goes out with stale/missing ARP
     * and returns no inbox messages.
     */
    private fun schedulePostSwitchInboxFetch() {
        CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
            .execute("postSwitchInboxFetch") {
                baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR)
                inboxV2Bridge.submit(FetchTrigger.SYSTEM, null)
                null
            }
    }

    //Session
    private fun resetProductConfigs() {
        if (config.isAnalyticsOnly) {
            config.logger.debug(config.accountId, "Product Config is not enabled for this instance")
            return
        }
        controllerManager.ctProductConfigController?.resetSettings()
        controllerManager.ctProductConfigController = CTProductConfigFactory.getInstance(
            context, deviceInfo, config, analyticsManager, coreMetaData, callbackManager
        )
        config.logger.verbose(config.accountId, "Product Config reset")
    }

    private fun resetVariables() {
        controllerManager.ctVariables?.clearUserContent()
    }

    /**
     * Cancels all scheduled in-apps (delayed and in-action).
     * Called during user switch to clear previous user's scheduled in-apps.
     */
    @WorkerThread
    private fun cancelScheduledInApps() {
        controllerManager.inAppController?.let {
            it.cancelAllScheduledInApps()
            config.logger.verbose(config.accountId, "Cancelled all scheduled in-apps for user change")
        }
    }
}
