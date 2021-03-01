package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inbox.CTInboxController;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.Utils;
import java.util.concurrent.Callable;

public class ControllerManager {

    private final BaseDatabaseManager mBaseDatabaseManager;

    private CTDisplayUnitController mCTDisplayUnitController;

    private CTFeatureFlagsController mCTFeatureFlagsController;

    private CTInboxController mCTInboxController;

    private final CTLockManager mCTLockManager;

    private CTProductConfigController mCTProductConfigController;

    private final BaseCallbackManager mCallbackManager;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final DeviceInfo mDeviceInfo;

    private InAppController mInAppController;

    private PushProviders mPushProviders;

    public ControllerManager(Context context,
            CleverTapInstanceConfig config,
            CTLockManager ctLockManager,
            BaseCallbackManager callbackManager,
            DeviceInfo deviceInfo,
            BaseDatabaseManager databaseManager) {
        mConfig = config;
        mCTLockManager = ctLockManager;
        mCallbackManager = callbackManager;
        mDeviceInfo = deviceInfo;
        mContext = context;
        mBaseDatabaseManager = databaseManager;
    }

    public CTDisplayUnitController getCTDisplayUnitController() {
        return mCTDisplayUnitController;
    }

    public void setCTDisplayUnitController(
            final CTDisplayUnitController CTDisplayUnitController) {
        mCTDisplayUnitController = CTDisplayUnitController;
    }

    public CTFeatureFlagsController getCTFeatureFlagsController() {

        return mCTFeatureFlagsController;
    }

    public void setCTFeatureFlagsController(
            final CTFeatureFlagsController CTFeatureFlagsController) {
        mCTFeatureFlagsController = CTFeatureFlagsController;
    }

    public CTInboxController getCTInboxController() {
        return mCTInboxController;
    }

    public void setCTInboxController(final CTInboxController CTInboxController) {
        mCTInboxController = CTInboxController;
    }

    public CTProductConfigController getCTProductConfigController() {
        return mCTProductConfigController;
    }

    public void setCTProductConfigController(
            final CTProductConfigController CTProductConfigController) {
        mCTProductConfigController = CTProductConfigController;
    }

    public InAppController getInAppController() {
        return mInAppController;
    }

    public void setInAppController(final InAppController inAppController) {
        mInAppController = inAppController;
    }

    public PushProviders getPushProviders() {
        return mPushProviders;
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
        Task<Void> task = CTExecutorFactory.executors(mConfig).postAsyncSafelyTask();
        task.execute("initializeInbox", new Callable<Void>() {
            @Override
            public Void call() {
                _initializeInbox();
                return null;
            }
        });
    }

    // always call async
    private void _initializeInbox() {
        synchronized (mCTLockManager.getInboxControllerLock()) {
            if (getCTInboxController() != null) {
                mCallbackManager._notifyInboxInitialized();
                return;
            }
            if (mDeviceInfo.getDeviceID() != null) {
                setCTInboxController(new CTInboxController(mConfig, mDeviceInfo.getDeviceID(),
                        mBaseDatabaseManager.loadDBAdapter(mContext),
                        mCTLockManager,
                        mCallbackManager,
                        Utils.haveVideoPlayerSupport));
                mCallbackManager._notifyInboxInitialized();
            } else {
                mConfig.getLogger().info("CRITICAL : No device ID found!");
            }
        }
    }
}
