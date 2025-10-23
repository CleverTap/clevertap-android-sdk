package com.clevertap.android.sdk

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import androidx.annotation.RestrictTo
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.clevertap.android.sdk.StorageHelper.putInt
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock

/**
 * Manages the activity lifecycle events for the CleverTap SDK.
 *
 * This class is responsible for handling various lifecycle callbacks of an Android application,
 * such as when an activity is created, resumed, or paused. It orchestrates actions like
 * session management, pushing "App Launched" events, handling install referrers,
 * processing deep links and push notification clicks, and triggering in-app messages.
 *
 * This is an internal class and is not meant to be used directly by developers.
 *
 * @property context The application context.
 * @property config The instance-specific configuration.
 * @property analyticsManager The manager for handling analytics events.
 * @property coreMetaData The manager for core metadata like session and device info.
 * @property sessionManager The manager for user sessions.
 * @property pushProviders The manager for push notification services.
 * @property callbackManager The manager for various SDK callbacks.
 * @property inAppController The controller for in-app notifications.
 * @property baseEventQueueManager The manager for the event queue.
 * @property executors The task executor for running background operations.
 * @property clock A clock instance for time-related operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ActivityLifeCycleManager internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val analyticsManager: AnalyticsManager,
    private val coreMetaData: CoreMetaData,
    private val sessionManager: SessionManager,
    private val pushProviders: PushProviders,
    private val callbackManager: BaseCallbackManager,
    private val inAppController: InAppController,
    private val baseEventQueueManager: BaseEventQueueManager,
    private val executors: CTExecutors,
    private val clock: Clock
) {
    //Lifecycle
    fun activityPaused() {
        CoreMetaData.setAppForeground(false)
        sessionManager.appLastSeen = clock.currentTimeMillis()
        config.getLogger().verbose(config.accountId, "App in background")
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute(
            "activityPaused"
        ) {
            val now = clock.currentTimeSecondsInt()
            if (coreMetaData.inCurrentSession()) {
                try {
                    putInt(
                        context,
                        config.accountId,
                        Constants.LAST_SESSION_EPOCH,
                        now
                    )
                    config.getLogger()
                        .verbose(config.accountId, "Updated session time: $now")
                } catch (t: Throwable) {
                    config.getLogger().verbose(
                        config.accountId,
                        "Failed to update session time time: " + t.message
                    )
                }
            }
        }
    }

    //Lifecycle
    fun activityResumed(activity: Activity?) {
        config.getLogger().verbose(config.accountId, "App in foreground")
        sessionManager.checkTimeoutSession()

        //Anything in this If block will run once per App Launch.
        if (!coreMetaData.isAppLaunchPushed) {
            analyticsManager.pushAppLaunchedEvent()
            analyticsManager.fetchFeatureFlags()
            pushProviders.onTokenRefresh()
            val task = executors.postAsyncSafelyTask<Unit>()
            task.execute("HandlingInstallReferrer") {
                if (!coreMetaData.isInstallReferrerDataSent && coreMetaData
                        .isFirstSession
                ) {
                    handleInstallReferrerOnFirstInstall()
                }
            }

            val cleanUpTask = executors.ioTask<Unit>()
            cleanUpTask.execute("CleanUpOldGIFs") {
                Utils.cleanupOldGIFs(context, config, clock)
            }

            try {
                if (callbackManager.getGeofenceCallback() != null) {
                    callbackManager.getGeofenceCallback().triggerLocation()
                }
            } catch (e: IllegalStateException) {
                config.getLogger().verbose(config.accountId, e.localizedMessage)
            } catch (_: Exception) {
                config.getLogger().verbose(config.accountId, "Failed to trigger location")
            }
        }
        baseEventQueueManager.pushInitialEventsAsync()
        inAppController.showNotificationIfAvailable()
    }

    fun onActivityCreated(notification: Bundle?, deepLink: Uri?, accountId: String?) {
        try {
            val shouldProcess =
                (accountId == null && config.isDefaultInstance) || config.accountId == accountId

            if (shouldProcess) {
                if (notification != null && !notification.isEmpty() && notification
                        .containsKey(Constants.NOTIFICATION_TAG)
                ) {
                    analyticsManager.pushNotificationClickedEvent(notification)
                }

                if (deepLink != null) {
                    try {
                        analyticsManager.pushDeepLink(deepLink, false)
                    } catch (_: Throwable) {
                        // no-op
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.v("Throwable - " + t.localizedMessage)
        }
    }

    private fun handleInstallReferrerOnFirstInstall() {
        config.getLogger().verbose(config.accountId, "Starting to handle install referrer")
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerServiceDisconnected() {
                    if (!coreMetaData.isInstallReferrerDataSent) {
                        handleInstallReferrerOnFirstInstall()
                    }
                }

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            // Connection established
                            val task = executors.postAsyncSafelyTask<ReferrerDetails?>()

                            task.addOnSuccessListener { response: ReferrerDetails? ->
                                try {
                                    val referrerUrl = response!!.installReferrer
                                    coreMetaData.referrerClickTime =
                                        response.referrerClickTimestampSeconds
                                    coreMetaData.appInstallTime =
                                        response.installBeginTimestampSeconds
                                    analyticsManager.pushInstallReferrer(referrerUrl)
                                    coreMetaData.isInstallReferrerDataSent = true
                                    config.getLogger().debug(
                                        config.accountId,
                                        "Install Referrer data set [Referrer URL-$referrerUrl]"
                                    )
                                } catch (npe: NullPointerException) {
                                    config.getLogger().debug(
                                        config.accountId,
                                        "Install referrer client null pointer exception caused by Google Play Install Referrer library - "
                                                + npe
                                            .message
                                    )
                                    referrerClient.endConnection()
                                    coreMetaData.isInstallReferrerDataSent = false
                                }
                            }

                            task.execute("ActivityLifeCycleManager#getInstallReferrer") {
                                var response: ReferrerDetails? = null
                                try {
                                    response = referrerClient.installReferrer
                                } catch (e: RemoteException) {
                                    config.getLogger().debug(
                                        config.accountId,
                                        "Remote exception caused by Google Play Install Referrer library - " + e
                                            .message
                                    )
                                    referrerClient.endConnection()
                                    coreMetaData.isInstallReferrerDataSent = false
                                }
                                response
                            }
                        }

                        // API not available on the current Play Store app.
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                            config.getLogger().debug(
                                config.accountId,
                                "Install Referrer data not set, API not supported by Play Store on device"
                            )

                        // Connection couldn't be established.
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE ->
                            config.getLogger().debug(
                                config.accountId,
                                "Install Referrer data not set, connection to Play Store unavailable"
                            )
                    }
                }
            })
        } catch (t: Throwable) {
            config.getLogger().verbose(
                config.accountId,
                ("Google Play Install Referrer's InstallReferrerClient Class not found - " + t
                    .localizedMessage
                        + " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your build.gradle")
            )
        }
    }
}