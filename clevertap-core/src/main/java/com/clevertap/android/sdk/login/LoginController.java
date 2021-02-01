package com.clevertap.android.sdk.login;

import android.content.Context;
import com.clevertap.android.sdk.AnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.BaseDatabaseManager;
import com.clevertap.android.sdk.BaseEventQueueManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DBManager;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.EventGroup;
import com.clevertap.android.sdk.InAppFCManager;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.PostAsyncSafelyHandler;
import com.clevertap.android.sdk.SessionManager;
import com.clevertap.android.sdk.ValidationResult;
import com.clevertap.android.sdk.ValidationResultStack;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import java.util.Map;

public class LoginController {

    private String cachedGUID = null;

    private final AnalyticsManager mAnalyticsManager;

    private final BaseEventQueueManager mBaseEventQueueManager;

    private final BaseCallbackManager mCallbackManager;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final CoreMetaData mCoreMetaData;

    private final DeviceInfo mDeviceInfo;

    private final InAppFCManager mInAppFCManager;

    private final LocalDataStore mLocalDataStore;

    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    private final PushProviders mPushProviders;

    private final SessionManager mSessionManager;

    private final ValidationResultStack mValidationResultStack;

    private final BaseDatabaseManager mDBManager;

    private String processingUserLoginIdentifier = null;

    private final Boolean processingUserLoginLock = true;

    private final ControllerManager mControllerManager;

    private final CTLockManager mCTLockManager;

    public LoginController(Context context,
            CleverTapInstanceConfig config,
            DeviceInfo deviceInfo,
            ValidationResultStack validationResultStack,
            BaseEventQueueManager eventQueueManager,
            AnalyticsManager analyticsManager,
            InAppFCManager inAppFCManager,
            PostAsyncSafelyHandler postAsyncSafelyHandler,
            CoreMetaData coreMetaData,
            ControllerManager controllerManager,
            SessionManager sessionManager,
            LocalDataStore localDataStore,
            BaseCallbackManager callbackManager,
            DBManager dbManager,
            CTLockManager ctLockManager){
        mConfig = config;
        mContext = context;
        mDeviceInfo = deviceInfo;
        mValidationResultStack = validationResultStack;
        mBaseEventQueueManager = eventQueueManager;
        mAnalyticsManager = analyticsManager;
        mInAppFCManager = inAppFCManager;
        mPostAsyncSafelyHandler = postAsyncSafelyHandler;
        mCoreMetaData = coreMetaData;
        mPushProviders = controllerManager.getPushProviders();
        mSessionManager = sessionManager;
        mLocalDataStore = localDataStore;
        mCallbackManager = callbackManager;
        mDBManager = dbManager;
        mControllerManager = controllerManager;
        mCTLockManager = ctLockManager;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void onUserLogin(final Map<String, Object> profile, final String cleverTapID) {
        if (mConfig.getEnableCustomCleverTapId()) {
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

    private void _onUserLogin(final Map<String, Object> profile, final String cleverTapID) {
        if (profile == null) {
            return;
        }

        try {
            final String currentGUID = mDeviceInfo.getDeviceID();
            if (currentGUID == null) {
                return;
            }

            boolean haveIdentifier = false;
            LoginInfoProvider loginInfoProvider = new LoginInfoProvider(mContext,
                    mConfig, mDeviceInfo);
            // check for valid identifier keys
            // use the first one we find
            IdentityRepo iProfileHandler = IdentityRepoFactory
                    .getRepo(mContext, mConfig, mDeviceInfo,
                            mValidationResultStack);
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
            if (!mDeviceInfo.isErrorDeviceId()) {
                if (!haveIdentifier || loginInfoProvider.isAnonymousDevice()) {
                    mConfig.getLogger().debug(mConfig.getAccountId(),
                            "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile");
                    mAnalyticsManager.pushProfile(profile);
                    return;
                }
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID.equals(currentGUID)) {
                mConfig.getLogger().debug(mConfig.getAccountId(),
                        "onUserLogin: " + profile.toString() + " maps to current device id " + currentGUID
                                + " pushing on current profile");
                mAnalyticsManager.pushProfile(profile);
                return;
            }

            // stringify profile to use as dupe blocker
            String profileToString = profile.toString();

            // as processing happens async block concurrent onUserLogin requests with the same profile, as our cache is set async
            if (isProcessUserLoginWithIdentifier(profileToString)) {
                mConfig.getLogger()
                        .debug(mConfig.getAccountId(), "Already processing onUserLogin for " + profileToString);
                return;
            }

            // create new guid if necessary and reset
            // block any concurrent onUserLogin call for the same profile
            synchronized (processingUserLoginLock) {
                processingUserLoginIdentifier = profileToString;
            }

            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "onUserLogin: queuing reset profile for " + profileToString
                            + " with Cached GUID " + ((cachedGUID != null) ? cachedGUID : "NULL"));

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID);

        } catch (Throwable t) {
            mConfig.getLogger().verbose(mConfig.getAccountId(), "onUserLogin failed", t);
        }
    }

    public void asyncProfileSwitchUser(final Map<String, Object> profile, final String cacheGuid,
            final String cleverTapID) {
        mPostAsyncSafelyHandler.postAsyncSafely("resetProfile", new Runnable() {
            @Override
            public void run() {
                try {
                    mConfig.getLogger().verbose(mConfig.getAccountId(), "asyncProfileSwitchUser:[profile " + profile
                            + " with Cached GUID " + ((cacheGuid != null) ? cachedGUID
                            : "NULL" + " and cleverTapID " + cleverTapID));
                    //set optOut to false on the current user to unregister the device token
                    mCoreMetaData.setCurrentUserOptedOut(false);
                    // unregister the device token on the current user
                    mPushProviders.forcePushDeviceToken(false);

                    // try and flush and then reset the queues
                    mBaseEventQueueManager.flushQueueSync(mContext, EventGroup.REGULAR);
                    mBaseEventQueueManager.flushQueueSync(mContext, EventGroup.PUSH_NOTIFICATION_VIEWED);
                    mDBManager.clearQueues(mContext);

                    // clear out the old data
                    mLocalDataStore.changeUser();
                    CoreMetaData.setActivityCount(1);
                    mSessionManager.destroySession();

                    // either force restore the cached GUID or generate a new one
                    if (cacheGuid != null) {
                        mDeviceInfo.forceUpdateDeviceId(cacheGuid);
                        mCallbackManager.notifyUserProfileInitialized(cacheGuid);
                    } else if (mConfig.getEnableCustomCleverTapId()) {
                        mDeviceInfo.forceUpdateCustomCleverTapID(cleverTapID);
                    } else {
                        mDeviceInfo.forceNewDeviceID();
                    }
                    mCallbackManager.notifyUserProfileInitialized(mDeviceInfo.getDeviceID());
                    mDeviceInfo.setCurrentUserOptOutStateFromStorage(); // be sure to call this after the guid is updated
                    mAnalyticsManager.forcePushAppLaunchedEvent();
                    if (profile != null) {
                        mAnalyticsManager.pushProfile(profile);
                    }
                    mPushProviders.forcePushDeviceToken(true);
                    synchronized (processingUserLoginLock) {
                        processingUserLoginIdentifier = null;
                    }
                    resetInbox();
                    resetFeatureFlags();
                    resetProductConfigs();
                    recordDeviceIDErrors();
                    resetDisplayUnits();
                    mInAppFCManager.changeUser(mDeviceInfo.getDeviceID());
                } catch (Throwable t) {
                    mConfig.getLogger().verbose(mConfig.getAccountId(), "Reset Profile error", t);
                }
            }
        });
    }

    private boolean isProcessUserLoginWithIdentifier(String identifier) {
        synchronized (processingUserLoginLock) {
            return processingUserLoginIdentifier != null && processingUserLoginIdentifier.equals(identifier);
        }
    }

    public void recordDeviceIDErrors() {
        for (ValidationResult validationResult : mDeviceInfo.getValidationResults()) {
            mValidationResultStack.pushValidationResult(validationResult);
        }
    }

    /**
     * Resets the Display Units in the cache
     */
    private void resetDisplayUnits() {
        if (mControllerManager.getCTDisplayUnitController() != null) {
            mControllerManager.getCTDisplayUnitController().reset();
        } else {
            mConfig.getLogger().verbose(mConfig.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, DisplayUnitcontroller is null");
        }
    }

    private void resetFeatureFlags() {
        if (mControllerManager.getCTFeatureFlagsController() != null && mControllerManager.getCTFeatureFlagsController()
                .isInitialized()) {
            mControllerManager.getCTFeatureFlagsController().resetWithGuid(mDeviceInfo.getDeviceID());
            mControllerManager.getCTFeatureFlagsController().fetchFeatureFlags();
        }
    }

    // always call async
    private void resetInbox() {
        synchronized (mCTLockManager.getInboxControllerLock()) {
            mControllerManager.setCTInboxController(null);
        }
        mControllerManager.initializeInbox();
    }
//Session

    private void resetProductConfigs() {
        if (mConfig.isAnalyticsOnly()) {
            mConfig.getLogger().debug(mConfig.getAccountId(), "Product Config is not enabled for this instance");
            return;
        }
        if (mControllerManager.getCTProductConfigController() != null) {
            mControllerManager.getCTProductConfigController().resetSettings();
        }
        CTProductConfigController ctProductConfigController = new CTProductConfigController(mContext, mDeviceInfo.getDeviceID(),
                mConfig, mAnalyticsManager, mCoreMetaData,
                mCallbackManager);
        mControllerManager.setCTProductConfigController(ctProductConfigController);
        mConfig.getLogger().verbose(mConfig.getAccountId(), "Product Config reset");
    }
}