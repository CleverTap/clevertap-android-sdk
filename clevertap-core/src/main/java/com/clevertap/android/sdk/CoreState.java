package com.clevertap.android.sdk;

import android.content.Context;
import android.os.Handler;
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.login.LoginController;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;

//TODO move this to builder pattern & add sanity check for dependencies at the time of creation
public class CoreState extends CleverTapState {

    private BaseLocationManager baseLocationManager;

    private CleverTapInstanceConfig config;

    private CoreMetaData coreMetaData;

    private CTFeatureFlagsController ctFeatureFlagsController;

    private CTInboxController ctInboxController;

    private CTProductConfigController ctProductConfigController;

    private BaseDatabaseManager databaseManager;

    private DeviceInfo deviceInfo;

    private EventMediator eventMediator;

    private EventProcessor eventProcessor;

    private InAppFCManager inAppFCManager;

    private LocalDataStore localDataStore;

    private AnalyticsManager mAnalyticsManager;

    private BaseQueueManager mBaseEventQueueManager;

    private CTDisplayUnitController mCTDisplayUnitController;

    private CTLockManager mCTLockManager;

    private CallbackManager mCallbackManager;

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

    public AnalyticsManager getAnalyticsManager() {
        return mAnalyticsManager;
    }

    public void setAnalyticsManager(final AnalyticsManager analyticsManager) {
        mAnalyticsManager = analyticsManager;
    }

    public BaseQueueManager getBaseEventQueueManager() {
        return mBaseEventQueueManager;
    }

    void setBaseEventQueueManager(final BaseQueueManager baseEventQueueManager) {
        this.mBaseEventQueueManager = baseEventQueueManager;
    }

    public CTDisplayUnitController getCTDisplayUnitController() {
        return mCTDisplayUnitController;
    }

    public void setCTDisplayUnitController(
            final CTDisplayUnitController CTDisplayUnitController) {
        mCTDisplayUnitController = CTDisplayUnitController;
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

    public CTFeatureFlagsController getCtFeatureFlagsController() {
        return ctFeatureFlagsController;
    }

    public void setCtFeatureFlagsController(
            final CTFeatureFlagsController ctFeatureFlagsController) {
        this.ctFeatureFlagsController = ctFeatureFlagsController;
    }

    public CTInboxController getCtInboxController() {
        return ctInboxController;
    }

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

    EventProcessor getEventProcessor() {
        return eventProcessor;
    }

    void setEventProcessor(final EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    BaseLocationManager getLocationManager() {
        return baseLocationManager;
    }

    @Override
    void setLocationManager(final BaseLocationManager baseLocationManager) {
        this.baseLocationManager = baseLocationManager;
    }

    /**
     * Returns the generic handler object which is used to post
     * runnables. The returned value will never be null.
     *
     * @return The generic handler
     * @see Handler
     */

    MainLooperHandler getMainLooperHandler() {
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

    public void initializeInbox() {
        if (getConfig().isAnalyticsOnly()) {
            config.getLogger()
                    .debug(config.getAccountId(), "Instance is analytics only, not initializing Notification Inbox");
            return;
        }
        postAsyncSafelyHandler.postAsyncSafely("initializeInbox", new Runnable() {
            @Override
            public void run() {
                _initializeInbox();
            }
        });
    }

    // always call async
    private void _initializeInbox() {
        synchronized (mCTLockManager.getInboxControllerLock()) {
            if (this.ctInboxController != null) {
                mCallbackManager._notifyInboxInitialized();
                return;
            }
            if (deviceInfo.getDeviceID() != null) {
                this.ctInboxController = new CTInboxController(deviceInfo.getDeviceID(),
                        databaseManager.loadDBAdapter(context),
                        Utils.haveVideoPlayerSupport);
                mCallbackManager._notifyInboxInitialized();
            } else {
                config.getLogger().info("CRITICAL : No device ID found!");
            }
        }
    }

    private void initProductConfig() {
        Logger.v("Initializing Product Config with device Id = " + getDeviceInfo().getDeviceID());
        if (getConfig().isAnalyticsOnly()) {
            getConfig().getLogger()
                    .debug(getConfig().getAccountId(), "Product Config is not enabled for this instance");
            return;
        }
        if (ctProductConfigController == null) {
            setCtProductConfigController(new CTProductConfigController(context, getDeviceInfo().getDeviceID(),
                    getConfig(), mBaseEventQueueManager, coreMetaData, mCallbackManager));
        }
    }

}
