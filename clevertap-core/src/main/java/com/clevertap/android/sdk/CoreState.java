package com.clevertap.android.sdk;

import android.content.Context;
import android.os.Handler;
import com.clevertap.android.sdk.pushnotification.PushProviders;

//TODO move this to builder pattern & add sanity check for dependencies at the time of creation
public class CoreState extends CleverTapState {

    private CleverTapInstanceConfig config;

    private CoreMetaData coreMetaData;

    private BaseDatabaseManager databaseManager;

    private DeviceInfo deviceInfo;

    private EventMediator eventMediator;

    private EventProcessor eventProcessor;

    private InAppFCManager inAppFCManager;

    private LocalDataStore localDataStore;

    private BaseQueueManager mBaseEventQueueManager;

    private CTLockManager mCTLockManager;

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

    public CTLockManager getCTLockManager() {
        return mCTLockManager;
    }

    public void setCTLockManager(final CTLockManager CTLockManager) {
        mCTLockManager = CTLockManager;
    }

    public CleverTapInstanceConfig getConfig() {
        return config;
    }

    public void setConfig(final CleverTapInstanceConfig config) {
        this.config = config;
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

    public BaseQueueManager getBaseEventQueueManager() {
        return mBaseEventQueueManager;
    }

    void setBaseEventQueueManager(final BaseQueueManager baseEventQueueManager) {
        this.mBaseEventQueueManager = baseEventQueueManager;
    }

    public CoreMetaData getCoreMetaData() {
        return coreMetaData;
    }

    void setCoreMetaData(final CoreMetaData coreMetaData) {
        this.coreMetaData = coreMetaData;
    }

    @Override
    BaseDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    void setDatabaseManager(final BaseDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
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
    BaseNetworkManager getNetworkManager() {
        return networkManager;
    }

    @Override
    void setNetworkManager(final BaseNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    PostAsyncSafelyHandler getPostAsyncSafelyHandler() {
        return postAsyncSafelyHandler;
    }

    @Override
    void setPostAsyncSafelyHandler(final PostAsyncSafelyHandler postAsyncSafelyHandler) {
        this.postAsyncSafelyHandler = postAsyncSafelyHandler;
    }

}
