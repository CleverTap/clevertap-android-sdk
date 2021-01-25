package com.clevertap.android.sdk;

import android.content.Context;
import android.os.Handler;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.login.LoginController;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;

//TODO move this to builder pattern & add sanity check for dependencies at the time of creation
public class CoreState extends CleverTapState {

    private ControllerManager mControllerManager;

    private BaseLocationManager baseLocationManager;

    private CleverTapInstanceConfig config;

    private CoreMetaData coreMetaData;

    private CTFeatureFlagsController ctFeatureFlagsController;

    private CTInboxController ctInboxController;

    private CTProductConfigController ctProductConfigController;

    private BaseDatabaseManager databaseManager;

    private DeviceInfo deviceInfo;

    private EventMediator eventMediator;

    private InAppFCManager inAppFCManager;

    private LocalDataStore localDataStore;

    private ActivityLifeCycleManager mActivityLifeCycleManager;

    private AnalyticsManager mAnalyticsManager;

    private BaseEventQueueManager mBaseEventQueueManager;

    private CTDisplayUnitController mCTDisplayUnitController;

    private CTLockManager mCTLockManager;

    private CallbackManager mCallbackManager;

    private InAppController mInAppController;

    private LoginController mLoginController;

    private SessionManager mSessionManager;

    private ValidationResultStack mValidationResultStack;

    private Validator mValidator;

    private MainLooperHandler mainLooperHandler;

    private BaseNetworkManager networkManager;

    private PostAsyncSafelyHandler postAsyncSafelyHandler;

    private PushProviders pushProviders;

    CoreState(final Context context) {
        super(context);
    }

    public ActivityLifeCycleManager getActivityLifeCycleManager() {
        return mActivityLifeCycleManager;
    }

    public ControllerManager getControllerManager() {
        return mControllerManager;
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

    public void setControllerManager(final ControllerManager controllerManager) {
        mControllerManager = controllerManager;
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

    public CallbackManager getCallbackManager() {
        return mCallbackManager;
    }

    void setCallbackManager(final CallbackManager callbackManager) {
        mCallbackManager = callbackManager;
    }

    public CleverTapInstanceConfig getConfig() {
        return config;
    }

    public void setConfig(final CleverTapInstanceConfig config) {
        this.config = config;
    }

    public CoreMetaData getCoreMetaData() {
        return coreMetaData;
    }

    void setCoreMetaData(final CoreMetaData coreMetaData) {
        this.coreMetaData = coreMetaData;
    }

    public CTInboxController getCtInboxController() {
        return ctInboxController;
    } //TODO move to Controller Manager

    public void setCtInboxController(final CTInboxController ctInboxController) {
        this.ctInboxController = ctInboxController;
    }

    public CTProductConfigController getCtProductConfigController() {
        initProductConfig();
        return ctProductConfigController;
    }

    public void setCtProductConfigController(
            final CTProductConfigController ctProductConfigController) {
        this.ctProductConfigController = ctProductConfigController;
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

    public InAppFCManager getInAppFCManager() {
        return inAppFCManager;
    }

    public void setInAppFCManager(final InAppFCManager inAppFCManager) {
        this.inAppFCManager = inAppFCManager;
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

    /**
     * Returns the generic handler object which is used to post
     * runnables. The returned value will never be null.
     *
     * @return The generic handler
     * @see Handler
     */

    public MainLooperHandler getMainLooperHandler() {
        return mainLooperHandler;
    }

    void setMainLooperHandler(final MainLooperHandler mainLooperHandler) {
        this.mainLooperHandler = mainLooperHandler;
    }

    @Override
    public BaseNetworkManager getNetworkManager() {
        return networkManager;
    }

    @Override
    void setNetworkManager(final BaseNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public PostAsyncSafelyHandler getPostAsyncSafelyHandler() {
        return postAsyncSafelyHandler;
    }

    @Override
    void setPostAsyncSafelyHandler(final PostAsyncSafelyHandler postAsyncSafelyHandler) {
        this.postAsyncSafelyHandler = postAsyncSafelyHandler;
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

    public Validator getValidator() {
        return mValidator;
    }

    public void setValidator(final Validator validator) {
        mValidator = validator;
    }



    EventMediator getEventMediator() {
        return eventMediator;
    }

    void setEventMediator(final EventMediator eventMediator) {
        this.eventMediator = eventMediator;
    }

    @Override
    BaseLocationManager getLocationManager() {
        return baseLocationManager;
    }

    @Override
    void setLocationManager(final BaseLocationManager baseLocationManager) {
        this.baseLocationManager = baseLocationManager;
    }



    private void initProductConfig() {
        Logger.v("Initializing Product Config with device Id = " + getDeviceInfo().getDeviceID());
        if (getConfig().isAnalyticsOnly()) {
            getConfig().getLogger()
                    .debug(getConfig().getAccountId(), "Product Config is not enabled for this instance");
            return;
        }
        if (ctProductConfigController == null) {
            CTProductConfigController ctProductConfigController = new CTProductConfigController(context, getDeviceInfo().getDeviceID(),
                    getConfig(), mAnalyticsManager, coreMetaData, mCallbackManager);
            getControllerManager().setCTProductConfigController(ctProductConfigController);
        }
    }
}