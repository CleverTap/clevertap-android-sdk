package com.clevertap.android.sdk;

import android.content.Context;

class CleverTapFactory {

    //TODO piyush check it in builder
    static CoreState getCoreState(Context context, CleverTapInstanceConfig config, DeviceInfo deviceInfo,
            Validator validator, final ValidationResultStack remoteLogger) {
        CoreState coreState = new CoreState(context);
        coreState.setConfig(config);
        coreState.setDeviceInfo(deviceInfo);
        coreState.setCoreMetaData(new CoreMetaData());
        coreState.setNetworkManager(new NetworkManager(coreState));
        coreState.setDatabaseManager(new DBManager());
        coreState.setEventMediator(new EventMediator(coreState));
        //config

        coreState.setPostAsyncSafelyHandler(new PostAsyncSafelyHandler(coreState));
        coreState.setMainLooperHandler(new MainLooperHandler());

        coreState.setEventMediator(new EventMediator(coreState));
        coreState.setEventProcessor(new EventProcessor());
        coreState.setSessionHandler(new SessionHandler(coreState, validator));
        coreState.setRemoteLogger(remoteLogger);
        coreState.setEventQueue(new EventQueue(coreState));
        return coreState;
    }
}