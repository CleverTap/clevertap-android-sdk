package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inbox.CTInboxController;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;

public class ControllerManager {

    private final CleverTapInstanceConfig mConfig;
    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;
    private final CTLockManager mCTLockManager;
    private final CallbackManager mCallbackManager;
    private final DeviceInfo mDeviceInfo;
    private final Context mContext;
    private final BaseDatabaseManager mBaseDatabaseManager;

    public ControllerManager(Context context,
            CleverTapInstanceConfig config,
            PostAsyncSafelyHandler postAsyncSafelyHandler,
            CTLockManager ctLockManager,
            CallbackManager callbackManager,
            DeviceInfo deviceInfo,
            BaseDatabaseManager databaseManager) {
        mConfig = config;
        mPostAsyncSafelyHandler = postAsyncSafelyHandler;
        mCTLockManager = ctLockManager;
        mCallbackManager = callbackManager;
        mDeviceInfo = deviceInfo;
        mContext = context;
        mBaseDatabaseManager = databaseManager;
    }

    private PushProviders mPushProviders;

    private InAppController mInAppController;

    private CTDisplayUnitController mCTDisplayUnitController;

    private CTInboxController mCTInboxController;

    private CTProductConfigController mCTProductConfigController;

    private CTFeatureFlagsController mCTFeatureFlagsController;

    public CTDisplayUnitController getCTDisplayUnitController() {
        return mCTDisplayUnitController;
    }

    public CTFeatureFlagsController getCTFeatureFlagsController() {

        return mCTFeatureFlagsController;
    }

    public CTInboxController getCTInboxController() {
        return mCTInboxController;
    }

    public CTProductConfigController getCTProductConfigController() {
        return mCTProductConfigController;
    }

    public InAppController getInAppController() {
        return mInAppController;
    }

    public PushProviders getPushProviders() {
        return mPushProviders;
    }

    public void setCTDisplayUnitController(
            final CTDisplayUnitController CTDisplayUnitController) {
        mCTDisplayUnitController = CTDisplayUnitController;
    }

    public void setCTFeatureFlagsController(
            final CTFeatureFlagsController CTFeatureFlagsController) {
        mCTFeatureFlagsController = CTFeatureFlagsController;
    }

    public void setCTInboxController(final CTInboxController CTInboxController) {
        mCTInboxController = CTInboxController;
    }

    public void setCTProductConfigController(
            final CTProductConfigController CTProductConfigController) {
        mCTProductConfigController = CTProductConfigController;
    }

    public void setInAppController(final InAppController inAppController) {
        mInAppController = inAppController;
    }

    public void setPushProviders(final PushProviders pushProviders) {
        mPushProviders = pushProviders;
    }

    public void initializeInbox() {
        if (mConfig.isAnalyticsOnly()) {
            mConfig.getLogger()
                    .debug(mConfig.getAccountId(), "Instance is analytics only, not initializing Notification Inbox");
            return;
        }
        mPostAsyncSafelyHandler.postAsyncSafely("initializeInbox", new Runnable() {
            @Override
            public void run() {
                _initializeInbox();
            }
        });
    }

    // always call async
    private void _initializeInbox() {
        synchronized (mCTLockManager.getInboxControllerLock()) {
            if (getInAppController() != null) {
                mCallbackManager._notifyInboxInitialized();
                return;
            }
            if (mDeviceInfo.getDeviceID() != null) {
                setCTInboxController(new CTInboxController(mDeviceInfo.getDeviceID(),
                        mBaseDatabaseManager.loadDBAdapter(mContext),
                        mCTLockManager,
                        mPostAsyncSafelyHandler,
                        mCallbackManager,
                        Utils.haveVideoPlayerSupport));
                mCallbackManager._notifyInboxInitialized();
            } else {
                mConfig.getLogger().info("CRITICAL : No device ID found!");
            }
        }
    }
}
