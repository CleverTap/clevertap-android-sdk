package com.clevertap.android.sdk

import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache
import io.mockk.mockk

class MockCoreStateKotlin(cleverTapInstanceConfig: CleverTapInstanceConfig) : CoreState() {

    init {
        config = cleverTapInstanceConfig
        deviceInfo = mockk<DeviceInfo>(relaxed = true)
        pushProviders = mockk<PushProviders>(relaxed = true)
        sessionManager = mockk<SessionManager>(relaxed = true)
        locationManager = mockk<LocationManager>(relaxed = true)
        coreMetaData = CoreMetaData()
        callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
        validationResultStack = mockk<ValidationResultStack>(relaxed = true)
        analyticsManager = mockk<AnalyticsManager>(relaxed = true)
        eventMediator = mockk<EventMediator>(relaxed = true)
        databaseManager = mockk<DBManager>(relaxed = true)
        validationResultStack = ValidationResultStack()
        mainLooperHandler = mockk<MainLooperHandler>(relaxed = true)
        networkManager = mockk<NetworkManager>(relaxed = true)
        ctLockManager = CTLockManager()
        localDataStore = mockk<LocalDataStore>(relaxed = true)
        baseEventQueueManager = mockk<EventQueueManager>(relaxed = true)
        inAppController = mockk<InAppController>(relaxed = true)
        parser = mockk<Parser>(relaxed = true)
        ctVariables = mockk<CTVariables>(relaxed = true)
        varCache = mockk<VarCache>(relaxed = true)
        controllerManager = mockk<ControllerManager>(relaxed = true)
    }
}
