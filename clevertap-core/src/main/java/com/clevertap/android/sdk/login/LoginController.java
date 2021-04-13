package com.clevertap.android.sdk.login;

import android.content.Context;
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
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.product_config.CTProductConfigFactory;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.validation.ValidationResultStack;
import java.util.Map;
import java.util.concurrent.Callable;

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

    private String processingUserLoginIdentifier = null;

    private static final Object processingUserLoginLock = new Object();

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
            CTLockManager ctLockManager) {
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
                    baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR);
                    baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED);
                    dbManager.clearQueues(context);

                    // clear out the old data
                    localDataStore.changeUser();
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
                    callbackManager.notifyUserProfileInitialized(deviceInfo.getDeviceID());
                    deviceInfo
                            .setCurrentUserOptOutStateFromStorage(); // be sure to call this after the guid is updated
                    analyticsManager.forcePushAppLaunchedEvent();
                    if (profile != null) {
                        analyticsManager.pushProfile(profile);
                    }
                    pushProviders.forcePushDeviceToken(true);
                    synchronized (processingUserLoginLock) {
                        processingUserLoginIdentifier = null;
                    }
                    resetInbox();
                    resetFeatureFlags();
                    resetProductConfigs();
                    recordDeviceIDErrors();
                    resetDisplayUnits();
                    controllerManager.getInAppFCManager().changeUser(deviceInfo.getDeviceID());
                } catch (Throwable t) {
                    config.getLogger().verbose(config.getAccountId(), "Reset Profile error", t);
                }
                return null;
            }
        });
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
        _onUserLogin(profile, cleverTapID);
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
            LoginInfoProvider loginInfoProvider = new LoginInfoProvider(context,
                    config, deviceInfo);
            // check for valid identifier keys
            // use the first one we find
            IdentityRepo iProfileHandler = IdentityRepoFactory
                    .getRepo(context, config, deviceInfo,
                            validationResultStack);
            for (String key : profile.keySet()) {
                Object value = profile.get(key);
                boolean isProfileKey = iProfileHandler.hasIdentity(key);
                if (isProfileKey) {
                    try {
                        String identifier = null;
                        if (value != null) {
                            identifier = value.toString();
                        }
                        if (identifier != null && identifier.length() > 0) {
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
                        "onUserLogin: " + profile.toString() + " maps to current device id " + currentGUID
                                + " pushing on current profile");
                analyticsManager.pushProfile(profile);
                return;
            }

            // stringify profile to use as dupe blocker
            String profileToString = profile.toString();

            // as processing happens async block concurrent onUserLogin requests with the same profile, as our cache is set async
            if (isProcessUserLoginWithIdentifier(profileToString)) {
                config.getLogger()
                        .debug(config.getAccountId(), "Already processing onUserLogin for " + profileToString);
                return;
            }

            // create new guid if necessary and reset
            // block any concurrent onUserLogin call for the same profile
            synchronized (processingUserLoginLock) {
                processingUserLoginIdentifier = profileToString;
            }

            config.getLogger()
                    .verbose(config.getAccountId(), "onUserLogin: queuing reset profile for " + profileToString
                            + " with Cached GUID " + ((cachedGUID != null) ? cachedGUID : "NULL"));

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID);

        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "onUserLogin failed", t);
        }
    }

    private boolean isProcessUserLoginWithIdentifier(String identifier) {
        synchronized (processingUserLoginLock) {
            return processingUserLoginIdentifier != null && processingUserLoginIdentifier.equals(identifier);
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
        if (controllerManager.getCTFeatureFlagsController() != null && controllerManager
                .getCTFeatureFlagsController()
                .isInitialized()) {
            controllerManager.getCTFeatureFlagsController().resetWithGuid(deviceInfo.getDeviceID());
            controllerManager.getCTFeatureFlagsController().fetchFeatureFlags();
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
}