package com.clevertap.android.sdk

import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest : BaseTestCase() {
    private lateinit var configMock: CleverTapInstanceConfig
    private lateinit var coreMetaDataMock: CoreMetaData
    private lateinit var validatorMock: Validator
    private lateinit var localDataStoreMock: LocalDataStore

    private lateinit var sessionManagerMadeWithMocks: SessionManager
    private lateinit var sessionManagerMock: SessionManager
    private lateinit var sessionManager: SessionManager


    override fun setUp() {
        super.setUp()

        configMock = Mockito.mock(CleverTapInstanceConfig::class.java)
        coreMetaDataMock = Mockito.mock(CoreMetaData::class.java)
        validatorMock = Mockito.mock(Validator::class.java)
        localDataStoreMock = Mockito.mock(LocalDataStore::class.java)
        sessionManagerMadeWithMocks = SessionManager(configMock, coreMetaDataMock, validatorMock, localDataStoreMock)

        sessionManagerMock = Mockito.mock(SessionManager::class.java)

        val config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id","token","eu1")
        sessionManager = SessionManager(config, CoreMetaData(), Validator(),LocalDataStore(appCtx,config))

    }

    @Test
    fun test_checkTimeoutSession_when_ABC_should_XYZ() {
        //when appLastSeen is <=0, it does not call anything
        sessionManagerMadeWithMocks.appLastSeen = 0
        sessionManagerMadeWithMocks.checkTimeoutSession()


        //when appLastSeem is>0


    }

    @Test
    fun test_destroySession_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getAppLastSeen_when_ABC_should_XYZ() {
    }

    @Test
    fun test_setAppLastSeen_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getLastVisitTime_when_ABC_should_XYZ() {
    }

    @Test
    fun test_lazyCreateSession_when_ABC_should_XYZ() {
    }

    @Test
    fun test_setLastVisitTime_when_ABC_should_XYZ() {
    }
}