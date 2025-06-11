package com.clevertap.android.sdk;

import android.content.Context;

import com.clevertap.android.sdk.cryption.CryptHandler;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.events.BaseEventQueueManager;
import com.clevertap.android.sdk.events.EventMediator;
import com.clevertap.android.sdk.inapp.ImpressionManager;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager;
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.login.LoginController;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.product_config.CTProductConfigFactory;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.MainLooperHandler;
import com.clevertap.android.sdk.validation.ValidationResultStack;
import com.clevertap.android.sdk.variables.CTVariables;
import com.clevertap.android.sdk.variables.Parser;
import com.clevertap.android.sdk.variables.VarCache;

public class CoreState {

    private final BaseLocationManager baseLocationManager;

    private final CleverTapInstanceConfig config;

    private final CoreMetaData coreMetaData;

    private final BaseDatabaseManager databaseManager;

    private final DeviceInfo deviceInfo;

    private final EventMediator eventMediator;

    private final LocalDataStore localDataStore;

    private final ActivityLifeCycleManager activityLifeCycleManager;

    private final AnalyticsManager analyticsManager;

    private final BaseEventQueueManager baseEventQueueManager;

    private final CTLockManager ctLockManager;

    private final BaseCallbackManager callbackManager;

    private final ControllerManager controllerManager;

    private final InAppController inAppController;

    private final EvaluationManager evaluationManager;

    private final ImpressionManager impressionManager;

    private final LoginController loginController;

    private final SessionManager sessionManager;

    private final ValidationResultStack validationResultStack;

    private final MainLooperHandler mainLooperHandler;

    private final NetworkManager networkManager;

    private final PushProviders pushProviders;

    private final VarCache varCache;

    private final Parser parser;

    private final CryptHandler cryptHandler;

    private final StoreRegistry storeRegistry;

    private final TemplatesManager templatesManager;

    private final ProfileValueHandler profileValueHandler;

    public CTVariables getCTVariables() {
        return ctVariables;
    }

    public ImpressionManager getImpressionManager() {
        return impressionManager;
    }

    public StoreRegistry getStoreRegistry() {
        return storeRegistry;
    }

    public void setCTVariables(final CTVariables CTVariables) {
        ctVariables = CTVariables;
    }

    private CTVariables ctVariables;

    public CoreState(
            BaseLocationManager baseLocationManager,
            CleverTapInstanceConfig config,
            CoreMetaData coreMetaData,
            BaseDatabaseManager databaseManager,
            DeviceInfo deviceInfo,
            EventMediator eventMediator,
            LocalDataStore localDataStore,
            ActivityLifeCycleManager activityLifeCycleManager,
            AnalyticsManager analyticsManager,
            BaseEventQueueManager baseEventQueueManager,
            CTLockManager ctLockManager,
            BaseCallbackManager callbackManager,
            ControllerManager controllerManager,
            InAppController inAppController,
            EvaluationManager evaluationManager,
            ImpressionManager impressionManager,
            LoginController loginController,
            SessionManager sessionManager,
            ValidationResultStack validationResultStack,
            MainLooperHandler mainLooperHandler,
            NetworkManager networkManager,
            PushProviders pushProviders,
            VarCache varCache,
            Parser parser,
            CryptHandler cryptHandler,
            StoreRegistry storeRegistry,
            TemplatesManager templatesManager,
            ProfileValueHandler profileValueHandler,
            CTVariables ctVariables
    ) {
        this.baseLocationManager = baseLocationManager;
        this.config = config;
        this.coreMetaData = coreMetaData;
        this.databaseManager = databaseManager;
        this.deviceInfo = deviceInfo;
        this.eventMediator = eventMediator;
        this.localDataStore = localDataStore;
        this.activityLifeCycleManager = activityLifeCycleManager;
        this.analyticsManager = analyticsManager;
        this.baseEventQueueManager = baseEventQueueManager;
        this.ctLockManager = ctLockManager;
        this.callbackManager = callbackManager;
        this.controllerManager = controllerManager;
        this.inAppController = inAppController;
        this.evaluationManager = evaluationManager;
        this.impressionManager = impressionManager;
        this.loginController = loginController;
        this.sessionManager = sessionManager;
        this.validationResultStack = validationResultStack;
        this.mainLooperHandler = mainLooperHandler;
        this.networkManager = networkManager;
        this.pushProviders = pushProviders;
        this.varCache = varCache;
        this.parser = parser;
        this.cryptHandler = cryptHandler;
        this.storeRegistry = storeRegistry;
        this.templatesManager = templatesManager;
        this.profileValueHandler = profileValueHandler;
        this.ctVariables = ctVariables;
    }

    public Parser getParser() {
        return parser;
    }

    public ActivityLifeCycleManager getActivityLifeCycleManager() {
        return activityLifeCycleManager;
    }

    public AnalyticsManager getAnalyticsManager() {
        return analyticsManager;
    }

    public BaseEventQueueManager getBaseEventQueueManager() {
        return baseEventQueueManager;
    }

    public CTLockManager getCTLockManager() {
        return ctLockManager;
    }

    public BaseCallbackManager getCallbackManager() {
        return callbackManager;
    }

    public CleverTapInstanceConfig getConfig() {
        return config;
    }

    public ControllerManager getControllerManager() {
        return controllerManager;
    }

    public CoreMetaData getCoreMetaData() {
        return coreMetaData;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public CTProductConfigController getCtProductConfigController(Context context) {
        initProductConfig(context);
        return getControllerManager().getCTProductConfigController();
    }

    public BaseDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public InAppController getInAppController() {
        return inAppController;
    }

    public EvaluationManager getEvaluationManager() {
        return evaluationManager;
    }

    public LocalDataStore getLocalDataStore() {
        return localDataStore;
    }

    public LoginController getLoginController() {
        return loginController;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public PushProviders getPushProviders() {
        return pushProviders;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ValidationResultStack getValidationResultStack() {
        return validationResultStack;
    }

    BaseLocationManager getLocationManager() {
        return baseLocationManager;
    }

    public EventMediator getEventMediator() {
        return eventMediator;
    }

    public MainLooperHandler getMainLooperHandler() {
        return mainLooperHandler;
    }

    public VarCache getVarCache() {
        return varCache;
    }

    public CryptHandler getCryptHandler() {
        return cryptHandler;
    }

    public TemplatesManager getTemplatesManager() {
        return templatesManager;
    }

    public ProfileValueHandler getProfileValueHandler() {
        return profileValueHandler;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    private void initProductConfig(Context context) {
        if (getConfig().isAnalyticsOnly()) {
            getConfig().getLogger()
                    .debug(getConfig().getAccountId(), "Product Config is not enabled for this instance");
            return;
        }
        if (getControllerManager().getCTProductConfigController() == null) {
            getConfig().getLogger().verbose(config.getAccountId() + ":async_deviceID",
                    "Initializing Product Config with device Id = " + getDeviceInfo().getDeviceID());
            CTProductConfigController ctProductConfigController = CTProductConfigFactory
                    .getInstance(context, getDeviceInfo(),
                            getConfig(), analyticsManager, coreMetaData, callbackManager);
            getControllerManager().setCTProductConfigController(ctProductConfigController);
        }
    }
}