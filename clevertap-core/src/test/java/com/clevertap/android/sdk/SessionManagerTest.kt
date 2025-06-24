package com.clevertap.android.sdk


import android.content.Context
import com.clevertap.android.sdk.cryption.CryptFactory
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.CryptRepository
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.sdk.usereventlogs.UserEventLog
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
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
    private lateinit var cryptHandler : CryptHandler
    private lateinit var deviceInfo : DeviceInfo
    private lateinit var baseDatabaseManager: BaseDatabaseManager

    override fun setUp() {
        super.setUp()
        config = CleverTapInstanceConfig.createInstance(application, "id", "token", "region")


        configDef = CleverTapInstanceConfig.createDefaultInstance(application, "id", "token", "region")
        coreMetaData = CoreMetaData()
        validator = Validator()
        cryptHandler = CryptHandler(
            EncryptionLevel.NONE,
            "accountId",
            mockk<CryptRepository>(relaxed = true),
            mockk<CryptFactory>(relaxed = true),
        )
        deviceInfo = MockDeviceInfo(appCtx, configDef, "id", coreMetaData)
        baseDatabaseManager = mockk<DBManager>(relaxed = true)
        localDataStoreDef = LocalDataStore(
            application,
            configDef,
            cryptHandler,
            deviceInfo,
            baseDatabaseManager
        )

        sessionManagerDef = SessionManager(configDef,coreMetaData,validator,localDataStoreDef)
    }

    override fun cleanUp() {
        super.cleanUp()
        CoreMetaData.setCurrentActivity(null)
    }

    @Test
    fun test_checkTimeoutSession_when_FunctionIsCalledAndAppLastSeenIsGreaterThan60Mins_should_DestroySession() {
        //1. when appLastSeen is <= 0 , the function returns without any further execution. this implies that destroySession() Was never called
        var smSpy = spyk(sessionManagerDef)
        smSpy.appLastSeen = 0
        verify(exactly = 0) { smSpy.destroySession() }

        //2. when appLastSeen is  = timestamp that is older than current time by 60 minutes, then session gets destroyed via destroySession();
        // we verify this by spying the destroySession(); call
        smSpy = spyk(sessionManagerDef)
        smSpy.appLastSeen = System.currentTimeMillis() - (Constants.SESSION_LENGTH_MINS * 60 * 1000 )- 1000
        smSpy.checkTimeoutSession()
        verify(exactly = 1) { smSpy.destroySession() }

        smSpy = spyk(sessionManagerDef)
        smSpy.appLastSeen = System.currentTimeMillis()
        smSpy.checkTimeoutSession()
        verify(exactly = 0) { smSpy.destroySession() }
        assertEquals(null, CoreMetaData.getCurrentActivity())

    }

    @Test
    fun test_destroySession_when_DestroySessionIsCalled_should_CallABunchOfApisFromCoreMetaData() {
        val coreMetaDataSpy = spyk(coreMetaData)
        sessionManagerDef = SessionManager(configDef,coreMetaDataSpy,validator,localDataStoreDef)

        sessionManagerDef.destroySession()
        verify(exactly = 1) { coreMetaDataSpy.currentSessionId = 0 }
        verify(exactly = 1) { coreMetaDataSpy.isAppLaunchPushed = false }
        verify(exactly = 1) { coreMetaDataSpy.clearSource() }
        verify(exactly = 1) { coreMetaDataSpy.clearMedium() }
        verify(exactly = 1) { coreMetaDataSpy.clearCampaign() }
        verify(exactly = 1) { coreMetaDataSpy.clearWzrkParams() }

        verify(exactly = 0) { coreMetaDataSpy.isFirstSession = false }
        verify(exactly = 0) { coreMetaDataSpy.isFirstSession = true }

        // if coreMetaData has first session set as true, then it will also be switched to false
        coreMetaDataSpy.isFirstSession = true
        sessionManagerDef.destroySession()
        verify(exactly = 1) { coreMetaDataSpy.isFirstSession = false }

    }

    @Test
    fun test_getAppLastSeenAndSetAppLastSeen_when_FunctionIsCalled_should_ReturnAppLastSeenValue() {
        sessionManagerDef.appLastSeen = 10
        assertEquals(10,sessionManagerDef.appLastSeen)
    }

    @Test
    fun test_getLastVisitTimeAndSetLastVisitTime_when_FunctionIsCalled_should_SetTimeOfLastAppLaunchEventFireInLocalDataStore() {
        val localDataStoreMockk = mockk<LocalDataStore>(relaxed = true)
        sessionManagerDef = SessionManager(configDef,coreMetaData,validator,localDataStoreMockk)

        // when local data store returns null for app launched event, it sets last visit time as -1
        every { localDataStoreMockk.getEventDetail(Constants.APP_LAUNCHED_EVENT) } returns null
        sessionManagerDef.setLastVisitTime()
        assertEquals(-1,sessionManagerDef.lastVisitTime)

        // when local data store returns eventDetails for app launched event, it sets last visit time as eventDetails.lastVisitTime

        every { localDataStoreMockk.getEventDetail(Constants.APP_LAUNCHED_EVENT) } returns
                EventDetail(1, 10, 20, "hi")
        sessionManagerDef.setLastVisitTime()
        assertEquals(20,sessionManagerDef.lastVisitTime)
    }

    @Test
    fun test_lazyCreateSession_when_FunctionIsCalledWithContext_should_CreateSessionIfApplicable() {
        val coreMetaDataSpy = spyk(coreMetaData)
        sessionManagerDef = SessionManager(configDef,coreMetaDataSpy,validator,localDataStoreDef)
        var ctxSpy = spyk(application)

        // when current session is going on (i.e when currentSessionId>0 ) session is not created . we verify by verifying coreMetaDataSpy call
        coreMetaDataSpy.currentSessionId = 4
        sessionManagerDef.lazyCreateSession(ctxSpy)
        verify(exactly = 0) { coreMetaDataSpy.isFirstRequestInSession = true }

        // when current session has ended (i.e when currentSessionId<=0 ) session is  created .
        // we verify by verifying coreMetaDataSpy calls
        ctxSpy = spyk(application)
        coreMetaDataSpy.currentSessionId = 0
        sessionManagerDef.lazyCreateSession(ctxSpy)
        verify(exactly = 1) { coreMetaDataSpy.isFirstRequestInSession = true }

    }

    @Test
    fun testCreateSession() {
        // when current session has ended (i.e when currentSessionId<=0 ) session is  created .
        // we verify by verifying coreMetaDataSpy calls.
        var coreMetaDataSpy = spyk(coreMetaData).also { it.currentSessionId = 0 }
        var ctxSpy = spyk(application)
        sessionManagerDef =
            spyk(SessionManager(configDef, coreMetaDataSpy, validator, localDataStoreDef))

        every { sessionManagerDef.now } returns 1000
        // when lazyCreateSession is called while coreMetaData.currentSessionId ==0 , createSession gets called and
        // 1. coreMetaData.currentSessionId to System.currentTimeMillis() / 1000
        // 2. value of lastSessionId in cache is set to value of coreMetaData.getCurrentSessionId()

        sessionManagerDef.lazyCreateSession(ctxSpy)
        val expectedSettedValue = 1000
        Thread.sleep(1000)
        verify(exactly = 1) {
            coreMetaDataSpy.currentSessionId = 0
        }//(System.currentTimeMillis() / 1000).toInt()
        val settedValue = ctxSpy.getSharedPreferences("WizRocket",Context.MODE_PRIVATE).getInt("lastSessionId:id",-1)
        assertEquals(expectedSettedValue,settedValue)

        // when lastSessionTime <= 0 ,xyz . // when lastSessionID ! = 0
        coreMetaDataSpy = spyk(coreMetaData).also { it.currentSessionId = 0 }
        ctxSpy = spyk(application)
        sessionManagerDef = SessionManager(configDef, coreMetaDataSpy, validator, localDataStoreDef)
        ctxSpy.getSharedPreferences("WizRocket", Context.MODE_PRIVATE).edit().putInt("lastSessionId:id", 0).putInt("sexe:id", 10)/*.putInt("lastSessionId",11).putInt("sexe",13)*/.commit()

        sessionManagerDef.lazyCreateSession(ctxSpy)
        verify(exactly = 1) { coreMetaDataSpy.lastSessionLength = 10 }
        verify(exactly = 1) { coreMetaDataSpy.isFirstSession = true }

    }

    @Test
    fun `test setUserLastVisitTs`(){
        val localDataStoreMockk = mockk<LocalDataStore>()
        sessionManagerDef = SessionManager(configDef,coreMetaData,validator,localDataStoreMockk)
        val appLaunchedEventLog = UserEventLog(
            Constants.APP_LAUNCHED_EVENT,
            Utils.getNormalizedName(Constants.APP_LAUNCHED_EVENT),
            0,
            1000000L,
            1,
            deviceInfo.deviceID
        )
        every { localDataStoreMockk.readUserEventLog(Constants.APP_LAUNCHED_EVENT) } returns appLaunchedEventLog
        sessionManagerDef.setUserLastVisitTs()
        assertEquals(appLaunchedEventLog.lastTs, sessionManagerDef.userLastVisitTs)
    }


}