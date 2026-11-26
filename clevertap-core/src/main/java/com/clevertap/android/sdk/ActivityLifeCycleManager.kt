package com.clevertap.android.sdk

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
import com.clevertap.android.sdk.utils.Clock
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
        executors.postAsyncSafelyTask<Void?>().execute("activityPaused") {
            val now = (System.currentTimeMillis() / 1000).toInt()
            if (coreMetaData.inCurrentSession()) {
                try {
                    StorageHelper.putInt(context, config.accountId, Constants.LAST_SESSION_EPOCH, now)
                    config.logger.verbose(config.accountId, "Updated session time: $now")
                } catch (t: Throwable) {
                    config.logger.verbose(config.accountId, "Failed to update session time time: ${t.message}")
                }
            }
            null
        }
    }

    //Lifecycle
    fun appBackgrounded() {
        baseEventQueueManager.flushOnAppBackground(context)
    }

    //Lifecycle
    fun activityResumed() {
        config.logger.verbose(config.accountId, "App in foreground")
        sessionManager.checkTimeoutSession()

        //Anything in this If block will run once per App Launch.
        if (!coreMetaData.isAppLaunchPushed) {
            analyticsManager.pushAppLaunchedEvent()
            analyticsManager.fetchFeatureFlags()
            inboxV2Bridge.submit(FetchTrigger.SYSTEM, null)
            pushProviders.onTokenRefresh()
            executors.postAsyncSafelyTask<Void?>().execute("HandlingInstallReferrer") {
                if (!coreMetaData.isInstallReferrerDataSent && coreMetaData.isFirstSession) {
                    handleInstallReferrerOnFirstInstall()
                }
                null
            }

            executors.ioTask<Void?>().execute("CleanUpOldGIFs") {
                Utils.cleanupOldGIFs(context, config, clock)
                null
            }

            try {
                callbackManager.geofenceCallback?.triggerLocation()
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
            if (!shouldProcess) {
                return
            }

            if (notification != null && !notification.isEmpty && notification.containsKey(Constants.NOTIFICATION_TAG)) {
                analyticsManager.pushNotificationClickedEvent(notification)
            }

            if (deepLink != null) {
                pushDeepLinkSafely(deepLink)
            }
        } catch (t: Throwable) {
            Logger.v("Throwable - ${t.localizedMessage}")
        }
    }

    private fun pushDeepLinkSafely(deepLink: Uri) {
        try {
            analyticsManager.pushDeepLink(deepLink, false)
        } catch (t: Throwable) {
            // no-op
        }
    }

    private fun handleInstallReferrerOnFirstInstall() {
        config.logger.verbose(config.accountId, "Starting to handle install referrer")
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerServiceDisconnected() {
                    if (!coreMetaData.isInstallReferrerDataSent) {
                        executors.postAsyncSafelyTask<Void?>().execute("RetryInstallReferrer") {
                            handleInstallReferrerOnFirstInstall()
                            null
                        }
                    }
                }

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            // Connection established
                            val task = executors.postAsyncSafelyTask<ReferrerDetails?>()

                            task.addOnSuccessListener { response ->
                                try {
                                    // A null response takes the same path as an NPE from the library
                                    // (Java dereferenced it; the NPE message was null).
                                    if (response == null) {
                                        onReferrerDetailsFailed(referrerClient, null)
                                        return@addOnSuccessListener
                                    }
                                    val referrerUrl = response.installReferrer
                                    coreMetaData.referrerClickTime = response.referrerClickTimestampSeconds
                                    coreMetaData.appInstallTime = response.installBeginTimestampSeconds
                                    analyticsManager.pushInstallReferrer(referrerUrl)
                                    coreMetaData.isInstallReferrerDataSent = true
                                    config.logger.debug(
                                        config.accountId,
                                        "Install Referrer data set [Referrer URL-$referrerUrl]"
                                    )
                                } catch (npe: NullPointerException) {
                                    onReferrerDetailsFailed(referrerClient, npe.message)
                                }
                            }

                            task.execute("ActivityLifeCycleManager#getInstallReferrer") {
                                try {
                                    referrerClient.installReferrer
                                } catch (e: RemoteException) {
                                    config.logger.debug(
                                        config.accountId,
                                        "Remote exception caused by Google Play Install Referrer library - ${e.message}"
                                    )
                                    referrerClient.endConnection()
                                    coreMetaData.isInstallReferrerDataSent = false
                                    null
                                }
                            }
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
                "Google Play Install Referrer's InstallReferrerClient Class not found - ${t.localizedMessage}" +
                    " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your " +
                    "build.gradle"
            )
        }
    }

    private fun onReferrerDetailsFailed(referrerClient: InstallReferrerClient, message: String?) {
        config.logger.debug(
            config.accountId,
            "Install referrer client null pointer exception caused by Google Play Install Referrer library - $message"
        )
        executors.ioTaskNonUi<Void?>().execute("EndReferrerConnection") {
            referrerClient.endConnection()
            null
        }
        coreMetaData.isInstallReferrerDataSent = false
    }
}
