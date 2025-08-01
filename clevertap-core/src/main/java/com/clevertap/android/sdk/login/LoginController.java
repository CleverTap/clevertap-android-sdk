package com.clevertap.android.sdk.login;

import android.content.Context;

import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.AnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.SessionManager;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.db.DBManager;
import com.clevertap.android.sdk.events.BaseEventQueueManager;
import com.clevertap.android.sdk.events.EventGroup;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.network.ContentFetchManager;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.product_config.CTProductConfigFactory;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.validation.ValidationResultStack;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LoginController {

    private String cachedGUID = null;

    private final AnalyticsManager analyticsManager;

    private final BaseEventQueueManager baseEventQueueManager;

    private final CTLockManager ctLockManager;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final ControllerManager controllerManager;

    private final CoreMetaData coreMetaData;

    private final BaseDatabaseManager dbManager;

    private final DeviceInfo deviceInfo;

    private final LocalDataStore localDataStore;

    private final PushProviders pushProviders;

    private final SessionManager sessionManager;

    private final ValidationResultStack validationResultStack;

    private final LoginInfoProvider loginInfoProvider;

    private final ContentFetchManager contentFetchManager;

    public LoginController(Context context,
            CleverTapInstanceConfig config,
            DeviceInfo deviceInfo,
            ValidationResultStack validationResultStack,
            BaseEventQueueManager eventQueueManager,
            AnalyticsManager analyticsManager,
            CoreMetaData coreMetaData,
            ControllerManager controllerManager,
            SessionManager sessionManager,
            LocalDataStore localDataStore,
            BaseCallbackManager callbackManager,
            DBManager dbManager,
            CTLockManager ctLockManager,
            LoginInfoProvider loginInfoProvider,
            ContentFetchManager contentFetchManager
    ) {
        this.config = config;
        this.context = context;
        this.deviceInfo = deviceInfo;
        this.validationResultStack = validationResultStack;
        baseEventQueueManager = eventQueueManager;
        this.analyticsManager = analyticsManager;
        this.coreMetaData = coreMetaData;
        pushProviders = controllerManager.getPushProviders();
        this.sessionManager = sessionManager;
        this.localDataStore = localDataStore;
        this.callbackManager = callbackManager;
        this.dbManager = dbManager;
        this.controllerManager = controllerManager;
        this.ctLockManager = ctLockManager;
        this.loginInfoProvider = loginInfoProvider;
        this.contentFetchManager = contentFetchManager;
    }

    public void asyncProfileSwitchUser(final Map<String, Object> profile, final String cacheGuid,
            final String cleverTapID) {
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("resetProfile", new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    config.getLogger().verbose(config.getAccountId(), "asyncProfileSwitchUser:[profile " + profile
                            + " with Cached GUID " + ((cacheGuid != null) ? cachedGUID
                            : "NULL" + " and cleverTapID " + cleverTapID));
                    //set optOut to false on the current user to unregister the device token
                    coreMetaData.setCurrentUserOptedOut(false);
                    // unregister the device token on the current user
                    pushProviders.forcePushDeviceToken(false);

                    // try and flush and then reset the queues
                    baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR, null, true);
                    baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED, null, true);
                    contentFetchManager.cancelAllResponseJobs();
                    dbManager.clearQueues(context);

                    // clear out the old data
                    CoreMetaData.setActivityCount(1);
                    sessionManager.destroySession();

                    // either force restore the cached GUID or generate a new one
                    if (cacheGuid != null) {
                        deviceInfo.forceUpdateDeviceId(cacheGuid);
                        callbackManager.notifyUserProfileInitialized(cacheGuid);
                    } else if (config.getEnableCustomCleverTapId()) {
                        deviceInfo.forceUpdateCustomCleverTapID(cleverTapID);
                    } else {
                        deviceInfo.forceNewDeviceID();
                    }

                    localDataStore.changeUser();
                    callbackManager.notifyUserProfileInitialized(deviceInfo.getDeviceID());

                    // Restore state of opt out and system events from storage
                    deviceInfo.setCurrentUserOptOutStateFromStorage();
                    deviceInfo.setSystemEventsAllowedStateFromStorage();

                    // variables for new user are fetched with App Launched
                    resetVariables();
                    analyticsManager.forcePushAppLaunchedEvent();
                    if (profile != null) {
                        analyticsManager.pushProfile(profile);
                    }
                    pushProviders.forcePushDeviceToken(true);
                    resetInbox();
                    resetFeatureFlags();
                    resetProductConfigs();
                    recordDeviceIDErrors();
                    resetDisplayUnits();

                    notifyChangeUserCallback();

                    controllerManager.getInAppFCManager().changeUser(deviceInfo.getDeviceID());
                } catch (Throwable t) {
                    config.getLogger().verbose(config.getAccountId(), "Reset Profile error", t);
                }
                return null;
            }
        });
    }

    public void notifyChangeUserCallback() {
        final List<ChangeUserCallback> changeUserCallbackList
                = callbackManager.getChangeUserCallbackList();
        synchronized (changeUserCallbackList) {
            for (ChangeUserCallback callback : changeUserCallbackList) {
                if (callback != null) {
                    callback.onChangeUser(deviceInfo.getDeviceID(), config.getAccountId());
                }
            }
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void onUserLogin(final Map<String, Object> profile, final String cleverTapID) {
        if (config.getEnableCustomCleverTapId()) {
            if (cleverTapID == null) {
                Logger.i(
                        "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml Please call onUserlogin() and pass a custom CleverTap ID");
            }
        } else {
            if (cleverTapID != null) {
                Logger.i(
                        "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml Please call CleverTapAPI.defaultInstance() without a custom CleverTap ID");
            }
        }
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("_onUserLogin",new Callable<Void>() {
            @Override
            public Void call() {
                _onUserLogin(profile, cleverTapID);
                return null;
            }
        });

    }

    public void recordDeviceIDErrors() {
        for (ValidationResult validationResult : deviceInfo.getValidationResults()) {
            validationResultStack.pushValidationResult(validationResult);
        }
    }

    private void _onUserLogin(final Map<String, Object> profile, final String cleverTapID) {
        if (profile == null) {
            return;
        }

        try {
            final String currentGUID = deviceInfo.getDeviceID();
            if (currentGUID == null) {
                return;
            }

            boolean haveIdentifier = false;

            // check for valid identifier keys
            // use the first one we find
            IdentityRepo iProfileHandler = IdentityRepoFactory
                    .getRepo(context, config, validationResultStack);
            for (String key : profile.keySet()) {
                Object value = profile.get(key);
                boolean isProfileKey = iProfileHandler.hasIdentity(key);
                if (isProfileKey) {
                    try {
                        String identifier = null;
                        if (value != null) {
                            identifier = value.toString();
                        }
                        if (identifier != null && !identifier.isEmpty()) {
                            haveIdentifier = true;
                            cachedGUID = loginInfoProvider.getGUIDForIdentifier(key, identifier);
                            if (cachedGUID != null) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        // no-op
                    }
                }
            }

            // if no valid identifier provided or there are no identified users on the device; just push on the current profile
            if (!deviceInfo.isErrorDeviceId()) {
                if (!haveIdentifier || loginInfoProvider.isAnonymousDevice()) {
                    config.getLogger().debug(config.getAccountId(),
                            "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile");
                    analyticsManager.pushProfile(profile);
                    return;
                }
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID.equals(currentGUID)) {
                config.getLogger().debug(config.getAccountId(),
                        "onUserLogin: " + profile + " maps to current device id " + currentGUID
                                + " pushing on current profile");
                analyticsManager.pushProfile(profile);
                return;
            }

            config.getLogger()
                    .verbose(config.getAccountId(), "onUserLogin: queuing reset profile for " + profile
                            + " with Cached GUID " + ((cachedGUID != null) ? cachedGUID : "NULL"));

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID);

        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "onUserLogin failed", t);
        }
    }

    /**
     * Resets the Display Units in the cache
     */
    private void resetDisplayUnits() {
        if (controllerManager.getCTDisplayUnitController() != null) {
            controllerManager.getCTDisplayUnitController().reset();
        } else {
            config.getLogger().verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, DisplayUnitcontroller is null");
        }
    }

    private void resetFeatureFlags() {
        CTFeatureFlagsController ctFeatureFlagsController = controllerManager.getCTFeatureFlagsController();
        if (ctFeatureFlagsController != null && ctFeatureFlagsController
                .isInitialized()) {
            ctFeatureFlagsController.resetWithGuid(deviceInfo.getDeviceID());
            ctFeatureFlagsController.fetchFeatureFlags();
        }else {
            config.getLogger().verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, CTFeatureFlagsController is null");
        }
    }

    // always call async
    private void resetInbox() {
        synchronized (ctLockManager.getInboxControllerLock()) {
            controllerManager.setCTInboxController(null);
        }
        controllerManager.initializeInbox();
    }
//Session

    private void resetProductConfigs() {
        if (config.isAnalyticsOnly()) {
            config.getLogger().debug(config.getAccountId(), "Product Config is not enabled for this instance");
            return;
        }
        if (controllerManager.getCTProductConfigController() != null) {
            controllerManager.getCTProductConfigController().resetSettings();
        }
        CTProductConfigController ctProductConfigController =
                CTProductConfigFactory.getInstance(context, deviceInfo, config, analyticsManager, coreMetaData,
                        callbackManager);
        controllerManager.setCTProductConfigController(ctProductConfigController);
        config.getLogger().verbose(config.getAccountId(), "Product Config reset");
    }

    private void resetVariables() {
        if (controllerManager.getCtVariables() != null) {
            controllerManager.getCtVariables().clearUserContent();
        }
    }

}
