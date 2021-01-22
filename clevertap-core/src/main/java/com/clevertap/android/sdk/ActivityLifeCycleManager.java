package com.clevertap.android.sdk;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.pushnotification.PushProviders;

class ActivityLifeCycleManager {

    private final AnalyticsManager mAnalyticsManager;

    private final BaseQueueManager mBaseQueueManager;

    private final CallbackManager mCallbackManager;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final CoreMetaData mCoreMetaData;

    private final InAppController mInAppController;

    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    private final PushProviders mPushProviders;

    private final SessionManager mSessionManager;

    ActivityLifeCycleManager(CoreState coreState) {
        mConfig = coreState.getConfig();
        mAnalyticsManager = coreState.getAnalyticsManager();
        mCoreMetaData = coreState.getCoreMetaData();
        mContext = coreState.getContext();
        mSessionManager = coreState.getSessionManager();
        mPushProviders = coreState.getPushProviders();
        mCallbackManager = coreState.getCallbackManager();
        mInAppController = coreState.getInAppController();
        mBaseQueueManager = coreState.getBaseEventQueueManager();
        mPostAsyncSafelyHandler = coreState.getPostAsyncSafelyHandler();
    }

    //Lifecycle
    public void activityPaused() {
        CoreMetaData.setAppForeground(false);
        mSessionManager.setAppLastSeen(System.currentTimeMillis());
        mConfig.getLogger().verbose(mConfig.getAccountId(), "App in background");
        final int now = (int) (System.currentTimeMillis() / 1000);
        if (mCoreMetaData.inCurrentSession()) {
            try {
                StorageHelper
                        .putInt(mContext,
                                StorageHelper.storageKeyWithSuffix(mConfig, Constants.LAST_SESSION_EPOCH),
                                now);
                mConfig.getLogger().verbose(mConfig.getAccountId(), "Updated session time: " + now);
            } catch (Throwable t) {
                mConfig.getLogger()
                        .verbose(mConfig.getAccountId(), "Failed to update session time time: " + t.getMessage());
            }
        }
    }

    //Lifecycle
    public void activityResumed(Activity activity) {
        mConfig.getLogger().verbose(mConfig.getAccountId(), "App in foreground");
        mSessionManager.checkTimeoutSession();
        //Anything in this If block will run once per App Launch.
        //Will not run for Apps which disable App Launched event
        if (!mCoreMetaData.isAppLaunchPushed()) {

            mAnalyticsManager.pushAppLaunchedEvent();
            mAnalyticsManager.fetchFeatureFlags();
            mPushProviders.onTokenRefresh();
            mPostAsyncSafelyHandler.postAsyncSafely("HandlingInstallReferrer", new Runnable() {
                @Override
                public void run() {
                    if (!mCoreMetaData.isInstallReferrerDataSent() && mCoreMetaData
                            .isFirstSession()) {
                        handleInstallReferrerOnFirstInstall();
                    }
                }
            });

            try {
                if (mCallbackManager.getGeofenceCallback() != null) {
                    mCallbackManager.getGeofenceCallback().triggerLocation();
                }
            } catch (IllegalStateException e) {
                mConfig.getLogger().verbose(mConfig.getAccountId(), e.getLocalizedMessage());
            } catch (Exception e) {
                mConfig.getLogger().verbose(mConfig.getAccountId(), "Failed to trigger location");
            }
        }
        if (!mCoreMetaData.inCurrentSession()) {
            mBaseQueueManager.pushInitialEventsAsync();
        }
        mInAppController.checkExistingInAppNotifications(activity);
        mInAppController.checkPendingInAppNotifications(activity);
    }

    public void onActivityCreated(final Bundle notification, final Uri deepLink) {
        try {
            boolean shouldProcess = mConfig.isDefaultInstance();

            if (shouldProcess) {
                if (notification != null && !notification.isEmpty() && notification
                        .containsKey(Constants.NOTIFICATION_TAG)) {
                    mAnalyticsManager.pushNotificationClickedEvent(notification);
                }

                if (deepLink != null) {
                    try {
                        mAnalyticsManager.pushDeepLink(deepLink, false);
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
        mConfig.getLogger().verbose(mConfig.getAccountId(), "Starting to handle install referrer");
        try {
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(mContext).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerServiceDisconnected() {
                    if (!mCoreMetaData.isInstallReferrerDataSent()) {
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
                                mCoreMetaData
                                        .setReferrerClickTime(response.getReferrerClickTimestampSeconds());
                                mCoreMetaData
                                        .setAppInstallTime(response.getInstallBeginTimestampSeconds());
                                mAnalyticsManager.pushInstallReferrer(referrerUrl);
                                mCoreMetaData.setInstallReferrerDataSent(true);
                                mConfig.getLogger().debug(mConfig.getAccountId(),
                                        "Install Referrer data set [Referrer URL-" + referrerUrl + "]");
                            } catch (RemoteException e) {
                                mConfig.getLogger().debug(mConfig.getAccountId(),
                                        "Remote exception caused by Google Play Install Referrer library - " + e
                                                .getMessage());
                                referrerClient.endConnection();
                                mCoreMetaData.setInstallReferrerDataSent(false);
                            }
                            referrerClient.endConnection();
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                            // API not available on the current Play Store app.
                            mConfig.getLogger().debug(mConfig.getAccountId(),
                                    "Install Referrer data not set, API not supported by Play Store on device");
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            // Connection couldn't be established.
                            mConfig.getLogger().debug(mConfig.getAccountId(),
                                    "Install Referrer data not set, connection to Play Store unavailable");
                            break;
                    }
                }
            });
        } catch (Throwable t) {
            mConfig.getLogger().verbose(mConfig.getAccountId(),
                    "Google Play Install Referrer's InstallReferrerClient Class not found - " + t
                            .getLocalizedMessage()
                            + " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your build.gradle");
        }
    }
}