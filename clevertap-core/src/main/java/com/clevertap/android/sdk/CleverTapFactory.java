package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.login.LoginController;
import com.clevertap.android.sdk.pushnotification.PushProviders;

class CleverTapFactory {

    //TODO piyush check it in builder
    static CoreState getCoreState(Context context, CleverTapInstanceConfig config, String cleverTapID) {
        CoreState coreState = new CoreState(context);
        coreState.setConfig(new CleverTapInstanceConfig(config));
        coreState.setValidator(new Validator());
        coreState.setValidationResultStack(new ValidationResultStack());
        coreState.setLocalDataStore(new LocalDataStore(context, config));
        coreState.setDeviceInfo(new DeviceInfo(context, config, cleverTapID));
        coreState.setCTLockManager(new CTLockManager());
        if (coreState.getDeviceInfo() != null && coreState.getDeviceInfo().getDeviceID() != null) {
            coreState.getConfig().getLogger()
                    .verbose("Initializing InAppFC with device Id = " + coreState.getDeviceInfo().getDeviceID());
            coreState.setInAppFCManager(new InAppFCManager(context, config, coreState.getDeviceInfo().getDeviceID()));
        }
        coreState.setPushProviders(PushProviders.load(coreState));
        coreState.setCTLockManager(new CTLockManager());
        coreState.setCoreMetaData(new CoreMetaData());
        coreState.setNetworkManager(new NetworkManager(coreState));
        coreState.setDatabaseManager(new DBManager(coreState));
        coreState.setEventMediator(new EventMediator(coreState));
        //config

        coreState.setPostAsyncSafelyHandler(new PostAsyncSafelyHandler(coreState));
        coreState.setMainLooperHandler(new MainLooperHandler());

        coreState.setEventMediator(new EventMediator(coreState));
        coreState.setEventProcessor(new EventProcessor(coreState));
        coreState.setSessionManager(new SessionManager(coreState));
        coreState.setCallbackManager(new CallbackManager(coreState));
        coreState.setBaseEventQueueManager(new EventQueueManager(coreState));
        coreState.setAnalyticsManager(new AnalyticsManager(coreState));
        coreState.setLoginController(new LoginController(coreState));
        coreState.setInAppController(new InAppController(coreState));
        return coreState;
    }
}