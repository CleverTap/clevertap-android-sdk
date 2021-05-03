package com.clevertap.android.sdk;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.clevertap.android.sdk.events.BaseEventQueueManager;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.util.concurrent.Callable;

class ActivityLifeCycleManager {

    private final AnalyticsManager analyticsManager;

    private final BaseEventQueueManager baseEventQueueManager;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final CoreMetaData coreMetaData;

    private final InAppController inAppController;

    private final PushProviders pushProviders;

    private final SessionManager sessionManager;

    ActivityLifeCycleManager(Context context,
            CleverTapInstanceConfig config,
            AnalyticsManager analyticsManager,
            CoreMetaData coreMetaData,
            SessionManager sessionManager,
            PushProviders pushProviders,
            BaseCallbackManager callbackManager,
            InAppController inAppController,
            BaseEventQueueManager baseEventQueueManager) {
        this.context = context;
        this.config = config;
        this.analyticsManager = analyticsManager;
        this.coreMetaData = coreMetaData;
        this.sessionManager = sessionManager;
        this.pushProviders = pushProviders;
        this.callbackManager = callbackManager;
        this.inAppController = inAppController;
        this.baseEventQueueManager = baseEventQueueManager;
    }

    //Lifecycle
    public void activityPaused() {
        CoreMetaData.setAppForeground(false);
        sessionManager.setAppLastSeen(System.currentTimeMillis());
        config.getLogger().verbose(config.getAccountId(), "App in background");
        final int now = (int) (System.currentTimeMillis() / 1000);
        if (coreMetaData.inCurrentSession()) {
            try {
                StorageHelper
                        .putInt(context,
                                StorageHelper.storageKeyWithSuffix(config, Constants.LAST_SESSION_EPOCH),
                                now);
                config.getLogger().verbose(config.getAccountId(), "Updated session time: " + now);
            } catch (Throwable t) {
                config.getLogger()
                        .verbose(config.getAccountId(), "Failed to update session time time: " + t.getMessage());
            }
        }
    }

    //Lifecycle
    public void activityResumed(Activity activity) {
        config.getLogger().verbose(config.getAccountId(), "App in foreground");
        sessionManager.checkTimeoutSession();
        //Anything in this If block will run once per App Launch.
        //Will not run for Apps which disable App Launched event
        if (!coreMetaData.isAppLaunchPushed()) {

            analyticsManager.pushAppLaunchedEvent();
            analyticsManager.fetchFeatureFlags();
            pushProviders.onTokenRefresh();
            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
            task.execute("HandlingInstallReferrer",new Callable<Void>() {
                @Override
                public Void call() {
                    if (!coreMetaData.isInstallReferrerDataSent() && coreMetaData
                            .isFirstSession()) {
                        handleInstallReferrerOnFirstInstall();
                    }
                    return null;
                }
            });

            try {
                if (callbackManager.getGeofenceCallback() != null) {
                    callbackManager.getGeofenceCallback().triggerLocation();
                }
            } catch (IllegalStateException e) {
                config.getLogger().verbose(config.getAccountId(), e.getLocalizedMessage());
            } catch (Exception e) {
                config.getLogger().verbose(config.getAccountId(), "Failed to trigger location");
            }
        }
        baseEventQueueManager.pushInitialEventsAsync();
        inAppController.checkExistingInAppNotifications(activity);
        inAppController.checkPendingInAppNotifications(activity);
    }

    public void onActivityCreated(final Bundle notification, final Uri deepLink) {
        try {
            boolean shouldProcess = config.isDefaultInstance();

            if (shouldProcess) {
                if (notification != null && !notification.isEmpty() && notification
                        .containsKey(Constants.NOTIFICATION_TAG)) {
                    analyticsManager.pushNotificationClickedEvent(notification);
                }

                if (deepLink != null) {
                    try {
                        analyticsManager.pushDeepLink(deepLink, false);
                    } catch (Throwable t) {
                        // no-op
                    }
                }
            }
        } catch (Throwable t) {
            Logger.v("Throwable - " + t.getLocalizedMessage());
        }
    }

    private void handleInstallReferrerOnFirstInstall() {
        config.getLogger().verbose(config.getAccountId(), "Starting to handle install referrer");
        try {
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerServiceDisconnected() {
                    if (!coreMetaData.isInstallReferrerDataSent()) {
                        handleInstallReferrerOnFirstInstall();
                    }
                }

                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            // Connection established.
                            ReferrerDetails response;
                            try {
                                response = referrerClient.getInstallReferrer();
                                String referrerUrl = response.getInstallReferrer();
                                coreMetaData
                                        .setReferrerClickTime(response.getReferrerClickTimestampSeconds());
                                coreMetaData
                                        .setAppInstallTime(response.getInstallBeginTimestampSeconds());
                                analyticsManager.pushInstallReferrer(referrerUrl);
                                coreMetaData.setInstallReferrerDataSent(true);
                                config.getLogger().debug(config.getAccountId(),
                                        "Install Referrer data set [Referrer URL-" + referrerUrl + "]");
                            } catch (RemoteException e) {
                                config.getLogger().debug(config.getAccountId(),
                                        "Remote exception caused by Google Play Install Referrer library - " + e
                                                .getMessage());
                                referrerClient.endConnection();
                                coreMetaData.setInstallReferrerDataSent(false);
                            }catch (NullPointerException npe){
                                config.getLogger().debug(config.getAccountId(),
                                        "Install referrer client null pointer exception caused by Google Play Install Referrer library - " + npe
                                                .getMessage());
                                referrerClient.endConnection();
                                coreMetaData.setInstallReferrerDataSent(false);
                            }
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                            // API not available on the current Play Store app.
                            config.getLogger().debug(config.getAccountId(),
                                    "Install Referrer data not set, API not supported by Play Store on device");
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            // Connection couldn't be established.
                            config.getLogger().debug(config.getAccountId(),
                                    "Install Referrer data not set, connection to Play Store unavailable");
                            break;
                    }
                }
            });
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(),
                    "Google Play Install Referrer's InstallReferrerClient Class not found - " + t
                            .getLocalizedMessage()
                            + " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your build.gradle");
        }
    }
}