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
import com.clevertap.android.sdk.validation.Validator;

//TODO move this to builder pattern & add sanity check for dependencies at the time of creation
public class CoreState extends CleverTapState {

    private BaseLocationManager baseLocationManager;

    private CleverTapInstanceConfig config;

    private CoreMetaData coreMetaData;

    private BaseDatabaseManager databaseManager;

    private DeviceInfo deviceInfo;

    private EventMediator eventMediator;

    private LocalDataStore localDataStore;

    private ActivityLifeCycleManager mActivityLifeCycleManager;

    private AnalyticsManager mAnalyticsManager;

    private BaseEventQueueManager mBaseEventQueueManager;

    private CTLockManager mCTLockManager;

    private BaseCallbackManager mCallbackManager;

    private ControllerManager mControllerManager;

    private InAppController mInAppController;

    private LoginController mLoginController;

    private SessionManager mSessionManager;

    private ValidationResultStack mValidationResultStack;

    private MainLooperHandler mainLooperHandler;

    private BaseNetworkManager networkManager;

    private PushProviders pushProviders;

    CoreState(final Context context) {
        super(context);
    }

    public ActivityLifeCycleManager getActivityLifeCycleManager() {
        return mActivityLifeCycleManager;
    }

    public void setActivityLifeCycleManager(final ActivityLifeCycleManager activityLifeCycleManager) {
        mActivityLifeCycleManager = activityLifeCycleManager;
    }

    public AnalyticsManager getAnalyticsManager() {
        return mAnalyticsManager;
    }

    public void setAnalyticsManager(final AnalyticsManager analyticsManager) {
        mAnalyticsManager = analyticsManager;
    }

    public BaseEventQueueManager getBaseEventQueueManager() {
        return mBaseEventQueueManager;
    }

    void setBaseEventQueueManager(final BaseEventQueueManager baseEventQueueManager) {
        this.mBaseEventQueueManager = baseEventQueueManager;
    }

    public CTLockManager getCTLockManager() {
        return mCTLockManager;
    }

    public void setCTLockManager(final CTLockManager CTLockManager) {
        mCTLockManager = CTLockManager;
    }

    public BaseCallbackManager getCallbackManager() {
        return mCallbackManager;
    }

    void setCallbackManager(final BaseCallbackManager callbackManager) {
        mCallbackManager = callbackManager;
    }

    public CleverTapInstanceConfig getConfig() {
        return config;
    }

    public void setConfig(final CleverTapInstanceConfig config) {
        this.config = config;
    }

    public ControllerManager getControllerManager() {
        return mControllerManager;
    }

    public void setControllerManager(final ControllerManager controllerManager) {
        mControllerManager = controllerManager;
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
        return mInAppController;
    }

    public void setInAppController(final InAppController inAppController) {
        mInAppController = inAppController;
    }

    public LocalDataStore getLocalDataStore() {
        return localDataStore;
    }

    public void setLocalDataStore(final LocalDataStore localDataStore) {
        this.localDataStore = localDataStore;
    }

    public LoginController getLoginController() {
        return mLoginController;
    }

    public void setLoginController(final LoginController loginController) {
        mLoginController = loginController;
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
        return mSessionManager;
    }

    public void setSessionManager(final SessionManager sessionManager) {
        this.mSessionManager = sessionManager;
    }

    public ValidationResultStack getValidationResultStack() {
        return mValidationResultStack;
    }

    public void setValidationResultStack(final ValidationResultStack validationResultStack) {
        this.mValidationResultStack = validationResultStack;
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
                            getConfig(), mAnalyticsManager, coreMetaData, mCallbackManager);
            getControllerManager().setCTProductConfigController(ctProductConfigController);
        }
    }
}