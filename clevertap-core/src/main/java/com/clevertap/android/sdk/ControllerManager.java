package com.clevertap.android.sdk;

import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inbox.CTInboxController;
import com.clevertap.android.sdk.network.BatchListener;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.variables.CTVariables;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;
import org.json.JSONArray;

public class ControllerManager {

    private InAppFCManager inAppFCManager;

    private CTDisplayUnitController ctDisplayUnitController;

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    private CTFeatureFlagsController ctFeatureFlagsController;

    private CTInboxController ctInboxController;

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    private CTProductConfigController ctProductConfigController;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private InAppController inAppController;

    private PushProviders pushProviders;

    private  CTVariables ctVariables;

    public ControllerManager(
            CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager) {
        this.config = config;
        this.callbackManager = callbackManager;
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
