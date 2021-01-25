package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.login.LoginController;
import com.clevertap.android.sdk.pushnotification.PushProviders;

class CleverTapFactory {

    //TODO piyush check it in builder
    static CoreState getCoreState(Context context, CleverTapInstanceConfig cleverTapInstanceConfig,
            String cleverTapID) {
        CoreState coreState = new CoreState(context);

        CoreMetaData coreMetaData = new CoreMetaData();
        coreState.setCoreMetaData(coreMetaData);

        Validator validator = new Validator();
        coreState.setValidator(validator);

        ValidationResultStack validationResultStack = new ValidationResultStack();
        coreState.setValidationResultStack(validationResultStack);

        CTLockManager ctLockManager = new CTLockManager();
        coreState.setCTLockManager(ctLockManager);

        MainLooperHandler mainLooperHandler = new MainLooperHandler();
        coreState.setMainLooperHandler(mainLooperHandler);

        CleverTapInstanceConfig config = new CleverTapInstanceConfig(cleverTapInstanceConfig);
        coreState.setConfig(config);

        EventMediator eventMediator = new EventMediator(context, config, coreMetaData);
        coreState.setEventMediator(eventMediator);

        PostAsyncSafelyHandler postAsyncSafelyHandler = new PostAsyncSafelyHandler(config);
        coreState.setPostAsyncSafelyHandler(postAsyncSafelyHandler);

        LocalDataStore localDataStore = new LocalDataStore(context, config);
        coreState.setLocalDataStore(localDataStore);

        DeviceInfo deviceInfo = new DeviceInfo(context, config, cleverTapID, coreMetaData);
        coreState.setDeviceInfo(deviceInfo);

        CallbackManager callbackManager = new CallbackManager(config, deviceInfo);
        coreState.setCallbackManager(callbackManager);

        SessionManager sessionManager = new SessionManager(config, coreMetaData, validator, localDataStore);
        coreState.setSessionManager(sessionManager);

        InAppFCManager inAppFCManager = null;
        if (coreState.getDeviceInfo() != null && coreState.getDeviceInfo().getDeviceID() != null) {
            inAppFCManager = new InAppFCManager(context, config, coreState.getDeviceInfo().getDeviceID());
            coreState.getConfig().getLogger()
                    .verbose("Initializing InAppFC with device Id = " + coreState.getDeviceInfo().getDeviceID());
            coreState.setInAppFCManager(inAppFCManager);
        }

        DBManager baseDatabaseManager = new DBManager(config, ctLockManager);
        coreState.setDatabaseManager(baseDatabaseManager);

        //config

        // initializing feature flag so that feature flag will automatically gets initialized
        coreState.getCtFeatureFlagsController();

        NetworkManager networkManager = new NetworkManager(context, config, deviceInfo, coreMetaData,
                validationResultStack, pushProviders, inAppFCManager, baseDatabaseManager, ctLockManager,
                postAsyncSafelyHandler, validator);
        coreState.setNetworkManager(networkManager);

        EventQueueManager baseEventQueueManager = new EventQueueManager(baseDatabaseManager, context, config,
                eventMediator,
                sessionManager, callbackManager,
                mainLooperHandler, postAsyncSafelyHandler, deviceInfo, validationResultStack,
                networkManager, baseDatabaseManager, coreMetaData, ctLockManager, localDataStore);
        coreState.setBaseEventQueueManager(baseEventQueueManager);

        AnalyticsManager analyticsManager = new AnalyticsManager(context, config, baseEventQueueManager, validator,
                validationResultStack, coreMetaData, postAsyncSafelyHandler, localDataStore, deviceInfo,
                mainLooperHandler, callbackManager);
        coreState.setAnalyticsManager(analyticsManager);

        InAppController inAppController = new InAppController(context, config, mainLooperHandler,
                postAsyncSafelyHandler, inAppFCManager, callbackManager, analyticsManager, coreMetaData);
        coreState.setInAppController(inAppController);


        LocationManager locationManager = new LocationManager(context, config, coreMetaData, baseEventQueueManager);
        coreState.setLocationManager(locationManager);

        PushProviders pushProviders = PushProviders
                .load(context, config, baseDatabaseManager,  postAsyncSafelyHandler, validationResultStack,
                        analyticsManager);
        coreState.setPushProviders(pushProviders);


        ActivityLifeCycleManager activityLifeCycleManager = new ActivityLifeCycleManager(context, config,
                analyticsManager, coreMetaData, sessionManager, pushProviders, callbackManager, inAppController,
                baseEventQueueManager, postAsyncSafelyHandler);
        coreState.setActivityLifeCycleManager(activityLifeCycleManager);

        LoginController loginController = new LoginController(coreState);
        coreState.setLoginController(loginController);
        return coreState;
    }
}