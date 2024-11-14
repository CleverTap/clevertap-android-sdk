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
import org.mockito.*

// todo lp check usages and eliminate context setup
class MockCoreState(cleverTapInstanceConfig: CleverTapInstanceConfig) : CoreState() {

    init {
        config = cleverTapInstanceConfig
        deviceInfo = Mockito.mock(DeviceInfo::class.java)
        pushProviders = Mockito.mock(PushProviders::class.java)
        sessionManager = Mockito.mock(SessionManager::class.java)
        locationManager = Mockito.mock(LocationManager::class.java)
        coreMetaData = CoreMetaData()
        callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
        validationResultStack = Mockito.mock(ValidationResultStack::class.java)
        analyticsManager = Mockito.mock(AnalyticsManager::class.java)
        eventMediator = Mockito.mock(EventMediator::class.java)
        databaseManager = Mockito.mock(DBManager::class.java)
        validationResultStack = ValidationResultStack()
        mainLooperHandler = Mockito.mock(MainLooperHandler::class.java)
        networkManager = Mockito.mock(NetworkManager::class.java)
        ctLockManager = CTLockManager()
        localDataStore = Mockito.mock(LocalDataStore::class.java)
        baseEventQueueManager = Mockito.mock(EventQueueManager::class.java)
        inAppController = Mockito.mock(InAppController::class.java)
        parser = Mockito.mock(Parser::class.java)
        ctVariables = Mockito.mock(CTVariables::class.java)
        varCache = Mockito.mock(VarCache::class.java)
        controllerManager = Mockito.mock(ControllerManager::class.java)
    }
}
