package com.clevertap.android.sdk;

import android.content.Context;

//TODO move this to builder pattern & add sanity check for dependencies at the time of creation
class CoreState extends CleverTapState {

    private CleverTapInstanceConfig config;

    private CoreMetaData coreMetaData;

    private BaseDatabaseManager databaseManager;

    private DeviceInfo deviceInfo;

    private EventMediator eventMediator;

    private EventProcessor eventProcessor;

    private EventQueue eventQueue;

    private MainLooperHandler mainLooperHandler;

    private BaseNetworkManager networkManager;

    private PostAsyncSafelyHandler postAsyncSafelyHandler;

    private ValidationResultStack remoteLogger;

    private SessionHandler sessionHandler;

    CoreState(final Context context) {
        super(context);
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

    public ValidationResultStack getRemoteLogger() {
        return remoteLogger;
    }

    public void setRemoteLogger(final ValidationResultStack remoteLogger) {
        this.remoteLogger = remoteLogger;
    }

    public SessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public void setSessionHandler(final SessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    CoreMetaData getCoreMetaData() {
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

    EventQueue getEventQueue() {
        return eventQueue;
    }

    void setEventQueue(final EventQueue eventQueue) {
        this.eventQueue = eventQueue;
    }

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
