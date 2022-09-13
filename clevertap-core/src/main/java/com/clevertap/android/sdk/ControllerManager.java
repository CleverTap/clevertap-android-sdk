package com.clevertap.android.sdk;

import android.content.Context;
import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inbox.CTInboxController;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.util.concurrent.Callable;

public class ControllerManager {

    private InAppFCManager inAppFCManager;

    private final BaseDatabaseManager baseDatabaseManager;

    private CTDisplayUnitController ctDisplayUnitController;

    private CTFeatureFlagsController ctFeatureFlagsController;

    private CTInboxController ctInboxController;

    private final CTLockManager ctLockManager;

    private CTProductConfigController ctProductConfigController;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final DeviceInfo deviceInfo;

    private InAppController inAppController;

    private PushProviders pushProviders;

    public ControllerManager(Context context,
            CleverTapInstanceConfig config,
            CTLockManager ctLockManager,
            BaseCallbackManager callbackManager,
            DeviceInfo deviceInfo,
            BaseDatabaseManager databaseManager) {
        this.config = config;
        this.ctLockManager = ctLockManager;
        this.callbackManager = callbackManager;
        this.deviceInfo = deviceInfo;
        this.context = context;
        baseDatabaseManager = databaseManager;
    }

    public CTDisplayUnitController getCTDisplayUnitController() {
        return ctDisplayUnitController;
    }

    public void setCTDisplayUnitController(
            final CTDisplayUnitController CTDisplayUnitController) {
        ctDisplayUnitController = CTDisplayUnitController;
    }

    public CTFeatureFlagsController getCTFeatureFlagsController() {

        return ctFeatureFlagsController;
    }

    public void setCTFeatureFlagsController(
            final CTFeatureFlagsController CTFeatureFlagsController) {
        ctFeatureFlagsController = CTFeatureFlagsController;
    }

    public CTInboxController getCTInboxController() {
        return ctInboxController;
    }

    public void setCTInboxController(final CTInboxController CTInboxController) {
        ctInboxController = CTInboxController;
    }

    public CTProductConfigController getCTProductConfigController() {
        return ctProductConfigController;
    }

    public void setCTProductConfigController(
            final CTProductConfigController CTProductConfigController) {
        ctProductConfigController = CTProductConfigController;
    }

    public CleverTapInstanceConfig getConfig() {
        return config;
    }

    public InAppController getInAppController() {
        return inAppController;
    }

    public void setInAppController(final InAppController inAppController) {
        this.inAppController = inAppController;
    }

    public InAppFCManager getInAppFCManager() {
        return inAppFCManager;
    }

    public void setInAppFCManager(final InAppFCManager inAppFCManager) {
        this.inAppFCManager = inAppFCManager;
    }

    public PushProviders getPushProviders() {
        return pushProviders;
    }

    public void setPushProviders(final PushProviders pushProviders) {
        this.pushProviders = pushProviders;
    }

    @AnyThread
    public void initializeInbox() {
        if (config.isAnalyticsOnly()) {
            config.getLogger()
                    .debug(config.getAccountId(), "Instance is analytics only, not initializing Notification Inbox");
            return;
        }
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("initializeInbox", new Callable<Void>() {
            @Override
            public Void call() {
                _initializeInbox();
                return null;
            }
        });
    }

    // always call async
    @WorkerThread
    private void _initializeInbox() {
        synchronized (ctLockManager.getInboxControllerLock()) {
            if (getCTInboxController() != null) {
                callbackManager._notifyInboxInitialized();
                return;
            }
            if (deviceInfo.getDeviceID() != null) {
                setCTInboxController(new CTInboxController(config, deviceInfo.getDeviceID(),
                        baseDatabaseManager.loadDBAdapter(context),
                        ctLockManager,
                        callbackManager,
                        Utils.haveVideoPlayerSupport));
                callbackManager._notifyInboxInitialized();
            } else {
                config.getLogger().info("CRITICAL : No device ID found!");
            }
        }
    }
}
