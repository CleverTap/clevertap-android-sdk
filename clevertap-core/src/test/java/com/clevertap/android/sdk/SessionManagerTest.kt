package com.clevertap.android.sdk


import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class SessionManagerTest : BaseTestCase() {

    private lateinit var sessionManagerDef: SessionManager
    private lateinit var configDef: CleverTapInstanceConfig
    private lateinit var config: CleverTapInstanceConfig
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var validator : Validator
    private lateinit var localDataStoreDef: LocalDataStore
    override fun setUp() {
        super.setUp()
        config = CleverTapInstanceConfig.createInstance(application, "id", "token", "region")


        configDef = CleverTapInstanceConfig.createDefaultInstance(application, "id", "token", "region")
        coreMetaData = CoreMetaData()
        validator = Validator()

        localDataStoreDef = LocalDataStore(application,configDef)

        sessionManagerDef = SessionManager(configDef,coreMetaData,validator,localDataStoreDef)

    }

    @Test
    fun test_checkTimeoutSession_when_FunctionIsCalledAndAppLastSeenIsGreaterThan60Mins_should_DestroySession() {
        //1. when appLastSeen is <= 0 , the function returns without any further execution. this could not be verified since checkTimeoutSession is a void function

        //2. when appLastSeen is  = timestamp that is older than current time by 60 minutes, then session gets destroyed via destroySession();
        // we verify this by spying the destroySession(); call
        var smSpy = Mockito.spy(sessionManagerDef)
        smSpy.appLastSeen = System.currentTimeMillis() - (Constants.SESSION_LENGTH_MINS * 60 * 1000 )- 1000
        smSpy.checkTimeoutSession()
        Mockito.verify(smSpy,Mockito.atMostOnce()).destroySession()

        smSpy = Mockito.spy(sessionManagerDef)
        smSpy.appLastSeen = System.currentTimeMillis()
        smSpy.checkTimeoutSession()
        Mockito.verify(smSpy,Mockito.never()).destroySession()



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