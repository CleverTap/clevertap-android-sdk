package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.events.BaseEventQueueManager;
import com.clevertap.android.sdk.events.EventMediator;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.login.LoginController;
import com.clevertap.android.sdk.network.BaseNetworkManager;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.product_config.CTProductConfigFactory;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.task.MainLooperHandler;
import com.clevertap.android.sdk.validation.ValidationResultStack;

public class CoreState extends CleverTapState {

    private BaseLocationManager baseLocationManager;

    private CleverTapInstanceConfig config;

    private CoreMetaData coreMetaData;

    private BaseDatabaseManager databaseManager;

    private DeviceInfo deviceInfo;

    private EventMediator eventMediator;

    private LocalDataStore localDataStore;

    private ActivityLifeCycleManager activityLifeCycleManager;

    private AnalyticsManager analyticsManager;

    private BaseEventQueueManager baseEventQueueManager;

    private CTLockManager ctLockManager;

    private BaseCallbackManager callbackManager;

    private ControllerManager controllerManager;

    private InAppController inAppController;

    private LoginController loginController;

    private SessionManager sessionManager;

    private ValidationResultStack validationResultStack;

    private MainLooperHandler mainLooperHandler;

    private BaseNetworkManager networkManager;

    private PushProviders pushProviders;

    CoreState(final Context context) {
        super(context);
    }

    public ActivityLifeCycleManager getActivityLifeCycleManager() {
        return activityLifeCycleManager;
    }

    public void setActivityLifeCycleManager(final ActivityLifeCycleManager activityLifeCycleManager) {
        this.activityLifeCycleManager = activityLifeCycleManager;
    }

    public AnalyticsManager getAnalyticsManager() {
        return analyticsManager;
    }

    public void setAnalyticsManager(final AnalyticsManager analyticsManager) {
        this.analyticsManager = analyticsManager;
    }

    public BaseEventQueueManager getBaseEventQueueManager() {
        return baseEventQueueManager;
    }

    void setBaseEventQueueManager(final BaseEventQueueManager baseEventQueueManager) {
        this.baseEventQueueManager = baseEventQueueManager;
    }

    public CTLockManager getCTLockManager() {
        return ctLockManager;
    }

    public void setCTLockManager(final CTLockManager CTLockManager) {
        ctLockManager = CTLockManager;
    }

    public BaseCallbackManager getCallbackManager() {
        return callbackManager;
    }

    void setCallbackManager(final BaseCallbackManager callbackManager) {
        this.callbackManager = callbackManager;
    }

    public CleverTapInstanceConfig getConfig() {
        return config;
    }

    public void setConfig(final CleverTapInstanceConfig config) {
        this.config = config;
    }

    public ControllerManager getControllerManager() {
        return controllerManager;
    }

    public void setControllerManager(final ControllerManager controllerManager) {
        this.controllerManager = controllerManager;
    }

    public CoreMetaData getCoreMetaData() {
        return coreMetaData;
    }

    void setCoreMetaData(final CoreMetaData coreMetaData) {
        this.coreMetaData = coreMetaData;
    }

    public CTProductConfigController getCtProductConfigController() {
        initProductConfig();
        return getControllerManager().getCTProductConfigController();
    }

    @Override
    public BaseDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    void setDatabaseManager(final BaseDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(final DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public InAppController getInAppController() {
        return inAppController;
    }

    public void setInAppController(final InAppController inAppController) {
        this.inAppController = inAppController;
    }

    public LocalDataStore getLocalDataStore() {
        return localDataStore;
    }

    public void setLocalDataStore(final LocalDataStore localDataStore) {
        this.localDataStore = localDataStore;
    }

    public LoginController getLoginController() {
        return loginController;
    }

    public void setLoginController(final LoginController loginController) {
        this.loginController = loginController;
    }

    @Override
    public BaseNetworkManager getNetworkManager() {
        return networkManager;
    }

    @Override
    void setNetworkManager(final BaseNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public PushProviders getPushProviders() {
        return pushProviders;
    }

    public void setPushProviders(final PushProviders pushProviders) {
        this.pushProviders = pushProviders;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(final SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public ValidationResultStack getValidationResultStack() {
        return validationResultStack;
    }

    public void setValidationResultStack(final ValidationResultStack validationResultStack) {
        this.validationResultStack = validationResultStack;
    }

    @Override
    BaseLocationManager getLocationManager() {
        return baseLocationManager;
    }

    @Override
    void setLocationManager(final BaseLocationManager baseLocationManager) {
        this.baseLocationManager = baseLocationManager;
    }

    public EventMediator getEventMediator() {
        return eventMediator;
    }

    public void setEventMediator(final EventMediator eventMediator) {
        this.eventMediator = eventMediator;
    }

    public MainLooperHandler getMainLooperHandler() {
        return mainLooperHandler;
    }

    public void setMainLooperHandler(final MainLooperHandler mainLooperHandler) {
        this.mainLooperHandler = mainLooperHandler;
    }

    private void initProductConfig() {
        Logger.v("Initializing Product Config with device Id = " + getDeviceInfo().getDeviceID());
        if (getConfig().isAnalyticsOnly()) {
            getConfig().getLogger()
                    .debug(getConfig().getAccountId(), "Product Config is not enabled for this instance");
            return;
        }
        if (getControllerManager().getCTProductConfigController() == null) {
            CTProductConfigController ctProductConfigController = CTProductConfigFactory
                    .getInstance(context, getDeviceInfo(),
                            getConfig(), analyticsManager, coreMetaData, callbackManager);
            getControllerManager().setCTProductConfigController(ctProductConfigController);
        }
    }
}