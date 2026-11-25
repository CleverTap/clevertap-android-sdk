package com.clevertap.android.sdk

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inbox.InboxV2Bridge
import com.clevertap.android.sdk.network.fetch.FetchTrigger
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.OnSuccessListener
import com.clevertap.android.sdk.utils.Clock
import java.util.concurrent.Callable

internal open class ActivityLifeCycleManager(
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
    private val clock: Clock,
    private val inboxV2Bridge: InboxV2Bridge
) {

    //Lifecycle
    fun activityPaused() {
        CoreMetaData.setAppForeground(false)
        sessionManager.appLastSeen = System.currentTimeMillis()
        config.logger.verbose(config.accountId, "App in background")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute(
            "activityPaused",
            Callable<Void?> {
                val now = (System.currentTimeMillis() / 1000).toInt()
                if (coreMetaData.inCurrentSession()) {
                    try {
                        StorageHelper.putInt(context, config.accountId, Constants.LAST_SESSION_EPOCH, now)
                        config.logger.verbose(config.accountId, "Updated session time: $now")
                    } catch (t: Throwable) {
                        config.logger.verbose(config.accountId, "Failed to update session time time: " + t.message)
                    }
                }
                null
            }
        )
    }

    //Lifecycle
    fun appBackgrounded() {
        baseEventQueueManager.flushOnAppBackground(context)
    }

    //Lifecycle
    fun activityResumed(activity: Activity?) {
        config.logger.verbose(config.accountId, "App in foreground")
        sessionManager.checkTimeoutSession()

        //Anything in this If block will run once per App Launch.
        if (!coreMetaData.isAppLaunchPushed) {
            analyticsManager.pushAppLaunchedEvent()
            analyticsManager.fetchFeatureFlags()
            inboxV2Bridge.submit(FetchTrigger.SYSTEM, null)
            pushProviders.onTokenRefresh()
            val task = executors.postAsyncSafelyTask<Void?>()
            task.execute("HandlingInstallReferrer", Callable<Void?> {
                if (!coreMetaData.isInstallReferrerDataSent && coreMetaData.isFirstSession) {
                    handleInstallReferrerOnFirstInstall()
                }
                null
            })

            val cleanUpTask = executors.ioTask<Void?>()
            cleanUpTask.execute("CleanUpOldGIFs", Callable<Void?> {
                Utils.cleanupOldGIFs(context, config, clock)
                null
            })

            try {
                if (callbackManager.geofenceCallback != null) {
                    callbackManager.geofenceCallback!!.triggerLocation()
                }
            } catch (e: IllegalStateException) {
                config.logger.verbose(config.accountId, e.localizedMessage)
            } catch (e: Exception) {
                config.logger.verbose(config.accountId, "Failed to trigger location")
            }
        }
        baseEventQueueManager.pushInitialEventsAsync()
        inAppController.showNotificationIfAvailable()
    }

    fun onActivityCreated(notification: Bundle?, deepLink: Uri?, accountId: String?) {
        try {
            val shouldProcess = (accountId == null && config.isDefaultInstance) || config.accountId == accountId

            if (shouldProcess) {
                if (notification != null && !notification.isEmpty && notification
                        .containsKey(Constants.NOTIFICATION_TAG)
                ) {
                    analyticsManager.pushNotificationClickedEvent(notification)
                }

                if (deepLink != null) {
                    try {
                        analyticsManager.pushDeepLink(deepLink, false)
                    } catch (t: Throwable) {
                        // no-op
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.v("Throwable - " + t.localizedMessage)
        }
    }

    private fun handleInstallReferrerOnFirstInstall() {
        config.logger.verbose(config.accountId, "Starting to handle install referrer")
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerServiceDisconnected() {
                    if (!coreMetaData.isInstallReferrerDataSent) {
                        val retryTask = executors.postAsyncSafelyTask<Void?>()
                        retryTask.execute("RetryInstallReferrer", Callable<Void?> {
                            handleInstallReferrerOnFirstInstall()
                            null
                        })
                    }
                }

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            // Connection established
                            val task = executors.postAsyncSafelyTask<ReferrerDetails?>()

                            task.addOnSuccessListener(OnSuccessListener { response ->
                                try {
                                    // Java dereferenced a possibly-null response here; the NPE is caught below.
                                    val referrerUrl = response!!.installReferrer
                                    coreMetaData.referrerClickTime = response.referrerClickTimestampSeconds
                                    coreMetaData.appInstallTime = response.installBeginTimestampSeconds
                                    analyticsManager.pushInstallReferrer(referrerUrl)
                                    coreMetaData.isInstallReferrerDataSent = true
                                    config.logger.debug(
                                        config.accountId,
                                        "Install Referrer data set [Referrer URL-$referrerUrl]"
                                    )
                                } catch (npe: NullPointerException) {
                                    config.logger.debug(
                                        config.accountId,
                                        "Install referrer client null pointer exception caused by Google Play Install Referrer library - "
                                                + npe.message
                                    )
                                    val endTask = executors.ioTaskNonUi<Void?>()
                                    endTask.execute("EndReferrerConnection", Callable<Void?> {
                                        referrerClient.endConnection()
                                        null
                                    })
                                    coreMetaData.isInstallReferrerDataSent = false
                                }
                            })

                            task.execute("ActivityLifeCycleManager#getInstallReferrer", Callable<ReferrerDetails?> {
                                var response: ReferrerDetails? = null
                                try {
                                    response = referrerClient.installReferrer
                                } catch (e: RemoteException) {
                                    config.logger.debug(
                                        config.accountId,
                                        "Remote exception caused by Google Play Install Referrer library - " + e.message
                                    )
                                    referrerClient.endConnection()
                                    coreMetaData.isInstallReferrerDataSent = false
                                }
                                response
                            })
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                            // API not available on the current Play Store app.
                            config.logger.debug(
                                config.accountId,
                                "Install Referrer data not set, API not supported by Play Store on device"
                            )

                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE ->
                            // Connection couldn't be established.
                            config.logger.debug(
                                config.accountId,
                                "Install Referrer data not set, connection to Play Store unavailable"
                            )
                    }
                }
            })
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                "Google Play Install Referrer's InstallReferrerClient Class not found - " + t
                    .localizedMessage
                        + " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your build.gradle"
            )
        }
    }
}
