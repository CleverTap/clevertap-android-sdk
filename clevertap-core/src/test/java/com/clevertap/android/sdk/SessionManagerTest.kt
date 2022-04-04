package com.clevertap.android.sdk


import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals


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
    fun test_destroySession_when_DestroySessionIsCalled_should_CallABunchOfApisFromCoreMetaData() {
        val coreMetaDataSpy = Mockito.spy(coreMetaData)
        sessionManagerDef = SessionManager(configDef,coreMetaDataSpy,validator,localDataStoreDef)

        sessionManagerDef.destroySession()
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).currentSessionId = 0
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).isAppLaunchPushed =false
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).clearSource()
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).clearMedium()
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).clearCampaign()
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).clearWzrkParams()

        Mockito.verify(coreMetaDataSpy,Mockito.never()).isFirstSession = false
        Mockito.verify(coreMetaDataSpy,Mockito.never()).isFirstSession = true

        // if coreMetaData has first session set as true, then it will also be switched to false
        coreMetaDataSpy.isFirstSession = true
        sessionManagerDef.destroySession()
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).isFirstSession = false

    }

    @Test
    fun test_getAppLastSeen_when_FunctionIsCalled_should_ReturnAppLastSeenValue() {
        sessionManagerDef.appLastSeen = 10
        assertEquals(10,sessionManagerDef.appLastSeen)
    }

    @Test
    fun test_setAppLastSeen_when_FunctionIsCalled_should_ReturnAppLastSeenValue() {
        sessionManagerDef.appLastSeen = 10
        assertEquals(10,sessionManagerDef.appLastSeen)
    }

    @Test
    fun test_setLastVisitTime_when_FunctionIsCalled_should_SetTimeOfLastAppLaunchEventFireInLocalDataStore() {
        val localDataStoreMockk = Mockito.mock(LocalDataStore::class.java)
        sessionManagerDef = SessionManager(configDef,coreMetaData,validator,localDataStoreMockk)

        // when local data store returns null for app launched event, it sets last visit time as -1
        Mockito.`when`(localDataStoreMockk.getEventDetail(Constants.APP_LAUNCHED_EVENT)).thenReturn(null)
        sessionManagerDef.setLastVisitTime()
        assertEquals(-1,sessionManagerDef.lastVisitTime)

        // when local data store returns eventDetails for app launched event, it sets last visit time as eventDetails.lastVisitTime

        Mockito.`when`(localDataStoreMockk.getEventDetail(Constants.APP_LAUNCHED_EVENT)).thenReturn(EventDetail(1,10,20,"hi"))
        sessionManagerDef.setLastVisitTime()
        assertEquals(20,sessionManagerDef.lastVisitTime)

    }

    @Test
    fun test_lazyCreateSession_when_FunctionIsCalledWithContext_should_CreateSessionIfApplicable() {
        val coreMetaDataSpy = Mockito.spy(coreMetaData)
        sessionManagerDef = SessionManager(configDef,coreMetaDataSpy,validator,localDataStoreDef)
        val ctxSpy = Mockito.spy(application)

        // when current session is going on (i.e when currentSessionId>0 ) session is not created . we verify by verifying coreMetaDataSpy call
        coreMetaDataSpy.currentSessionId = 4
        sessionManagerDef.lazyCreateSession(ctxSpy)
        Mockito.verify(coreMetaDataSpy,Mockito.never()).isFirstRequestInSession = true

        // when current session has ended (i.e when currentSessionId<=0 ) session is  created .
        // we verify by verifying coreMetaDataSpy calls
        coreMetaDataSpy.currentSessionId = 0
        sessionManagerDef.lazyCreateSession(ctxSpy)
        Mockito.verify(coreMetaDataSpy,Mockito.atMostOnce()).isFirstRequestInSession = true
        Mockito.verify(coreMetaDataSpy,Mockito.atLeastOnce()).currentSessionId = Mockito.anyInt()

    }

    @Test
    fun test_getLastVisitTime_when_FunctionIsCalled_should_GetValuefLastVisitTime() {
        val localDataStoreMockk = Mockito.mock(LocalDataStore::class.java)
        sessionManagerDef = SessionManager(configDef,coreMetaData,validator,localDataStoreMockk)

        // when local data store returns null for app launched event, it sets last visit time as -1
        Mockito.`when`(localDataStoreMockk.getEventDetail(Constants.APP_LAUNCHED_EVENT)).thenReturn(null)
        sessionManagerDef.setLastVisitTime()
        assertEquals(-1,sessionManagerDef.lastVisitTime)

        // when local data store returns eventDetails for app launched event, it sets last visit time as eventDetails.lastVisitTime

        Mockito.`when`(localDataStoreMockk.getEventDetail(Constants.APP_LAUNCHED_EVENT)).thenReturn(EventDetail(1,10,20,"hi"))
        sessionManagerDef.setLastVisitTime()
        assertEquals(20,sessionManagerDef.lastVisitTime)

    }

}