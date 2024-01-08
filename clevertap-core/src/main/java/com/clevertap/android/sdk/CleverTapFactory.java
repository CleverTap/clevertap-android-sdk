package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.cryption.CryptHandler;
import com.clevertap.android.sdk.cryption.CryptUtils;
import com.clevertap.android.sdk.db.DBManager;
import com.clevertap.android.sdk.events.EventMediator;
import com.clevertap.android.sdk.events.EventQueueManager;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsFactory;
import com.clevertap.android.sdk.inapp.ImpressionManager;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inapp.InAppQueue;
import com.clevertap.android.sdk.inapp.TriggerManager;
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager;
import com.clevertap.android.sdk.inapp.evaluation.LimitsMatcher;
import com.clevertap.android.sdk.inapp.evaluation.TriggersMatcher;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.login.LoginController;
import com.clevertap.android.sdk.network.AppLaunchListener;
import com.clevertap.android.sdk.network.CompositeBatchListener;
import com.clevertap.android.sdk.network.FetchInAppListener;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager;
import com.clevertap.android.sdk.response.InAppResponse;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.MainLooperHandler;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.validation.ValidationResultStack;
import com.clevertap.android.sdk.validation.Validator;
import com.clevertap.android.sdk.variables.CTVariables;
import com.clevertap.android.sdk.variables.Parser;
import com.clevertap.android.sdk.variables.VarCache;
import java.util.concurrent.Callable;

class CleverTapFactory {

    static CoreState getCoreState(Context context, CleverTapInstanceConfig cleverTapInstanceConfig,
            String cleverTapID) {
        CoreState coreState = new CoreState(context);

        StoreRegistry storeRegistry = new StoreRegistry();
        storeRegistry.setLegacyInAppStore(StoreProvider.getInstance().provideLegacyInAppStore(context, cleverTapInstanceConfig.getAccountId()));
        coreState.setStoreRegistry(storeRegistry);

        CoreMetaData coreMetaData = new CoreMetaData();
        coreState.setCoreMetaData(coreMetaData);

        Validator validator = new Validator();

        ValidationResultStack validationResultStack = new ValidationResultStack();
        coreState.setValidationResultStack(validationResultStack);

        CTLockManager ctLockManager = new CTLockManager();
        coreState.setCTLockManager(ctLockManager);

        MainLooperHandler mainLooperHandler = new MainLooperHandler();
        coreState.setMainLooperHandler(mainLooperHandler);

        CleverTapInstanceConfig config = new CleverTapInstanceConfig(cleverTapInstanceConfig);
        coreState.setConfig(config);

        DBManager baseDatabaseManager = new DBManager(config, ctLockManager);
        coreState.setDatabaseManager(baseDatabaseManager);

        CryptHandler cryptHandler = new CryptHandler(config.getEncryptionLevel(), CryptHandler.EncryptionAlgorithm.AES, config.getAccountId());
        coreState.setCryptHandler(cryptHandler);
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("migratingEncryptionLevel", () -> {
            CryptUtils.migrateEncryptionLevel(context, config, cryptHandler, baseDatabaseManager.loadDBAdapter(context));
            return null;
        });

        EventMediator eventMediator = new EventMediator(context, config, coreMetaData);
        coreState.setEventMediator(eventMediator);

        LocalDataStore localDataStore = new LocalDataStore(context, config, cryptHandler);
        coreState.setLocalDataStore(localDataStore);

        DeviceInfo deviceInfo = new DeviceInfo(context, config, cleverTapID, coreMetaData);
        coreState.setDeviceInfo(deviceInfo);

        CTPreferenceCache.getInstance(context,config);

        BaseCallbackManager callbackManager = new CallbackManager(config, deviceInfo);
        coreState.setCallbackManager(callbackManager);

        SessionManager sessionManager = new SessionManager(config, coreMetaData, validator, localDataStore);
        coreState.setSessionManager(sessionManager);

        ControllerManager controllerManager = new ControllerManager(context, config,
                ctLockManager, callbackManager, deviceInfo, baseDatabaseManager);
        coreState.setControllerManager(controllerManager);

        TriggersMatcher triggersMatcher = new TriggersMatcher();
        TriggerManager triggersManager = new TriggerManager(context, config.getAccountId(), deviceInfo);
        ImpressionManager impressionManager = new ImpressionManager(storeRegistry);
        LimitsMatcher limitsMatcher = new LimitsMatcher(impressionManager, triggersManager);

        coreState.setImpressionManager(impressionManager);

        EvaluationManager evaluationManager = new EvaluationManager(
                triggersMatcher,
                triggersManager,
                limitsMatcher,
                storeRegistry
        );
        coreState.setEvaluationManager(evaluationManager);

        final StoreProvider storeProvider = StoreProvider.getInstance();
        /*InAppStore inAppStore = storeProvider.provideInAppStore(context, cryptHandler, deviceInfo,
                config.getAccountId());*/
        //ImpressionStore impStore = storeProvider.provideImpressionStore(context, deviceInfo, config.getAccountId());

        Task<Void> taskInitStores = CTExecutorFactory.executors(config).ioTask();
        taskInitStores.execute("initStores", () -> {
            if (coreState.getDeviceInfo() != null && coreState.getDeviceInfo().getDeviceID() != null) {
                if (storeRegistry.getInAppStore() == null) {
                    InAppStore inAppStore = storeProvider.provideInAppStore(context, cryptHandler, deviceInfo,
                            config.getAccountId());
                    storeRegistry.setInAppStore(inAppStore);
                    evaluationManager.loadSuppressedCSAndEvaluatedSSInAppsIds();
                    callbackManager.addChangeUserCallback(inAppStore);
                }
                if (storeRegistry.getImpressionStore() == null) {
                    ImpressionStore impStore = storeProvider.provideImpressionStore(context, deviceInfo,
                            config.getAccountId());
                    storeRegistry.setImpressionStore(impStore);
                    callbackManager.addChangeUserCallback(impStore);
                }
                if (storeRegistry.getInAppAssetsStore() == null) {
                    InAppAssetsStore assetsStore = storeProvider.provideInAppAssetsStore(context, deviceInfo, config.getAccountId());
                    storeRegistry.setInAppAssetsStore(assetsStore);
                }
            }
            return null;
        });

        //Get device id should be async to avoid strict mode policy.
        Task<Void> taskInitFCManager = CTExecutorFactory.executors(config).ioTask();
        taskInitFCManager.execute("initFCManager", new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (coreState.getDeviceInfo() != null && coreState.getDeviceInfo().getDeviceID() != null
                        && controllerManager.getInAppFCManager() == null) {
                    coreState.getConfig().getLogger()
                            .verbose(config.getAccountId() + ":async_deviceID",
                                    "Initializing InAppFC with device Id = " + coreState.getDeviceInfo()
                                            .getDeviceID());
                    controllerManager
                            .setInAppFCManager(
                                    new InAppFCManager(context, config, coreState.getDeviceInfo().getDeviceID(),
                                            storeRegistry, impressionManager));
                }
                return null;
            }
        });

        VarCache varCache = new VarCache(config, context);
        coreState.setVarCache(varCache);

        CTVariables ctVariables = new CTVariables(varCache);
        coreState.setCTVariables(ctVariables);
        coreState.getControllerManager().setCtVariables(ctVariables);

        Parser parser = new Parser(ctVariables);
        coreState.setParser(parser);

        Task<Void> taskVariablesInit = CTExecutorFactory.executors(config).ioTask();
        taskVariablesInit.execute("initCTVariables", () -> {
            ctVariables.init();
            return null;
        });

        InAppResponse inAppResponse = new InAppResponse(
                config,
                controllerManager,
                false,
                storeRegistry,
                coreMetaData
        );

        NetworkManager networkManager = new NetworkManager(
                context,
                config,
                deviceInfo,
                coreMetaData,
                validationResultStack,
                controllerManager,
                baseDatabaseManager,
                callbackManager,
                ctLockManager,
                validator,
                localDataStore,
                cryptHandler,
                inAppResponse
        );
        coreState.setNetworkManager(networkManager);

        EventQueueManager baseEventQueueManager = new EventQueueManager(
                baseDatabaseManager,
                context,
                config,
                eventMediator,
                sessionManager,
                callbackManager,
                mainLooperHandler,
                deviceInfo,
                validationResultStack,
                networkManager,
                coreMetaData,
                ctLockManager,
                localDataStore,
                controllerManager,
                cryptHandler
        );
        coreState.setBaseEventQueueManager(baseEventQueueManager);

        InAppResponse inAppResponseForSendTestInApp = new InAppResponse(
                config,
                controllerManager,
                true,
                storeRegistry,
                coreMetaData
        );

        AnalyticsManager analyticsManager = new AnalyticsManager(
                context,
                config,
                baseEventQueueManager,
                validator,
                validationResultStack,
                coreMetaData,
                localDataStore,
                deviceInfo,
                callbackManager,
                controllerManager,
                ctLockManager,
                inAppResponseForSendTestInApp
        );
        coreState.setAnalyticsManager(analyticsManager);

        networkManager.addNetworkHeadersListener(evaluationManager);

        InAppController inAppController = new InAppController(context, config, mainLooperHandler,
                controllerManager, callbackManager, analyticsManager, coreMetaData, deviceInfo, new InAppQueue(config,
                storeRegistry), evaluationManager
        );

        coreState.setInAppController(inAppController);
        coreState.getControllerManager().setInAppController(inAppController);

        final AppLaunchListener appLaunchListener = new AppLaunchListener();
        appLaunchListener.addListener(inAppController.onAppLaunchEventSent);

        CompositeBatchListener batchListener = new CompositeBatchListener();
        batchListener.addListener(appLaunchListener);
        batchListener.addListener(new FetchInAppListener(callbackManager));
        callbackManager.setBatchListener(batchListener);

        Task<Void> taskInitFeatureFlags = CTExecutorFactory.executors(config).ioTask();
        taskInitFeatureFlags.execute("initFeatureFlags", new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                initFeatureFlags(context, controllerManager, config, deviceInfo, callbackManager, analyticsManager);
                return null;
            }
        });

        LocationManager locationManager = new LocationManager(context, config, coreMetaData, baseEventQueueManager);
        coreState.setLocationManager(locationManager);

        CTWorkManager ctWorkManager = new CTWorkManager(context,config);

        PushProviders pushProviders = PushProviders
                .load(context, config, baseDatabaseManager, validationResultStack,
                        analyticsManager, controllerManager,ctWorkManager);
        coreState.setPushProviders(pushProviders);

        ActivityLifeCycleManager activityLifeCycleManager = new ActivityLifeCycleManager(context, config,
                analyticsManager, coreMetaData, sessionManager, pushProviders, callbackManager, inAppController,
                baseEventQueueManager);
        coreState.setActivityLifeCycleManager(activityLifeCycleManager);

        LoginController loginController = new LoginController(context, config, deviceInfo,
                validationResultStack, baseEventQueueManager, analyticsManager,
                coreMetaData, controllerManager, sessionManager,
                localDataStore, callbackManager, baseDatabaseManager, ctLockManager, cryptHandler);
        coreState.setLoginController(loginController);

        return coreState;
    }

    static void initFeatureFlags(Context context, ControllerManager controllerManager, CleverTapInstanceConfig config,
            DeviceInfo deviceInfo, BaseCallbackManager callbackManager, AnalyticsManager analyticsManager) {

        config.getLogger().verbose(config.getAccountId() + ":async_deviceID",
                "Initializing Feature Flags with device Id = " + deviceInfo.getDeviceID());
        if (config.isAnalyticsOnly()) {
            config.getLogger().debug(config.getAccountId(), "Feature Flag is not enabled for this instance");
        } else {
            controllerManager.setCTFeatureFlagsController(CTFeatureFlagsFactory.getInstance(context,
                    deviceInfo.getDeviceID(),
                    config, callbackManager, analyticsManager));
            config.getLogger().verbose(config.getAccountId() + ":async_deviceID", "Feature Flags initialized");
        }

    }
}