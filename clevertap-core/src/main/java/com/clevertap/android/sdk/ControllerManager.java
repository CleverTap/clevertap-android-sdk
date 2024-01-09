package com.clevertap.android.sdk;

import android.content.Context;
import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inbox.CTInboxController;
import com.clevertap.android.sdk.network.BatchListener;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.variables.CTVariables;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;
import java.util.concurrent.Callable;
import org.json.JSONArray;

public class ControllerManager {

    private InAppFCManager inAppFCManager;

    private final BaseDatabaseManager baseDatabaseManager;

    private CTDisplayUnitController ctDisplayUnitController;

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    private CTFeatureFlagsController ctFeatureFlagsController;

    private CTInboxController ctInboxController;

    private final CTLockManager ctLockManager;

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    private CTProductConfigController ctProductConfigController;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final DeviceInfo deviceInfo;

    private InAppController inAppController;

    private PushProviders pushProviders;

    private  CTVariables ctVariables;

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

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public CTFeatureFlagsController getCTFeatureFlagsController() {

        return ctFeatureFlagsController;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
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

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public CTProductConfigController getCTProductConfigController() {
        return ctProductConfigController;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public void setCTProductConfigController(
            final CTProductConfigController CTProductConfigController) {
        ctProductConfigController = CTProductConfigController;
    }

    public CTVariables getCtVariables() {
        return ctVariables;
    }
    public void setCtVariables(CTVariables ctVariables) {
        this.ctVariables = ctVariables;
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

    public void invokeCallbacksForNetworkError() {

        // Variables
        if (ctVariables != null) {
            FetchVariablesCallback fetchCallback = callbackManager.getFetchVariablesCallback();
            callbackManager.setFetchVariablesCallback(null);

            ctVariables.handleVariableResponseError(fetchCallback);
        }

        // Add more callbacks if necessary
    }

    /**
     * Invokes the batch listener callback to notify about the completion of a batch operation.
     *
     * @param requestQueue The JSON array representing the batch request sent to server.
     * @param success      true when the batch operation was successful, false when no network or CleverTap
     *                     instance is set to offline mode or request is failed.
     */
    public void invokeBatchListener(JSONArray requestQueue, boolean success) {
        BatchListener listener = callbackManager.getBatchListener();
        if (listener != null) {
            listener.onBatchSent(requestQueue, success);
        }
    }
}
