package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.cryption.CryptFactory
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.CryptRepository
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.sdk.usereventlogs.UserEventLogDAO
import com.clevertap.android.sdk.usereventlogs.UserEventLogDAOImpl
import com.clevertap.android.sdk.usereventlogs.UserEventLogTestData
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalDataStoreTest : BaseTestCase() {
    private lateinit var userEventLogDaoMock: UserEventLogDAO
    private lateinit var baseDatabaseManager: BaseDatabaseManager
    private lateinit var defConfig: CleverTapInstanceConfig
    private lateinit var config: CleverTapInstanceConfig

    private lateinit var localDataStoreWithDefConfig: LocalDataStore
    private lateinit var localDataStoreWithConfig: LocalDataStore
    private lateinit var localDataStoreWithConfigSpy: LocalDataStore
    private lateinit var cryptHandler : CryptHandler
    private lateinit var deviceInfo : DeviceInfo
    private lateinit var dbAdapter: DBAdapter
    val eventName = UserEventLogTestData.EventNames.TEST_EVENT
    private val normalizedEventName = UserEventLogTestData.EventNames.eventNameToNormalizedMap[eventName]!!
    private val eventNames = UserEventLogTestData.EventNames.eventNames
    private val setOfActualAndNormalizedEventNamePair = UserEventLogTestData.EventNames.setOfActualAndNormalizedEventNamePair


    override fun setUp() {
        super.setUp()
        val metaData = CoreMetaData()
        defConfig = CleverTapInstanceConfig.createDefaultInstance(appCtx, "id", "token", "region")
        cryptHandler = CryptHandler(
            EncryptionLevel.NONE,
            "accountId",
            mockk<CryptRepository>(relaxed = true),
            mockk<CryptFactory>(relaxed = true),
        )
        deviceInfo = MockDeviceInfo(appCtx, defConfig, "id", metaData)
        baseDatabaseManager = mockk<DBManager>(relaxed = true)
        dbAdapter = mockk<DBAdapter>(relaxed = true)
        userEventLogDaoMock = mockk<UserEventLogDAOImpl>(relaxed = true)
        localDataStoreWithDefConfig = LocalDataStore(
            appCtx,
            defConfig,
            cryptHandler,
            deviceInfo,
            baseDatabaseManager
        )
        config = CleverTapInstanceConfig.createInstance(appCtx, "id", "token", "region")
        localDataStoreWithConfig = LocalDataStore(
            appCtx,
            config,
            cryptHandler,
            deviceInfo,
            baseDatabaseManager
        )
        localDataStoreWithConfigSpy = spyk(localDataStoreWithConfig)
        every { baseDatabaseManager.loadDBAdapter(appCtx) } returns dbAdapter
        every { dbAdapter.userEventLogDAO() } returns userEventLogDaoMock
    }

    @Test
    fun test_changeUser() {
        //localDataStoreWithDefConfig.changeUser()
        // since changeUser() is a void function calls resetLocalProfileSync() which is a private function,
        // we can't further test it or verify its calling
        assertTrue { true }

    }

    @Test
    fun test_getEventDetail_when_EventNameIsPassed_should_ReturnEventDetail() {
        // if event name is null, exception would occur and we will get null in return
        localDataStoreWithDefConfig.getEventDetail(null).let { it->
            println("Event: ${it?.name}|${it?.count}|${it?.firstTime}|${it?.lastTime}")
            assertNull(it)
        }
        println("============================================================")

        //if configuration has personalisation disabled, getEventDetail() will return null
        defConfig.enablePersonalization(false)
        localDataStoreWithDefConfig.getEventDetail("eventName").let {
            println("Event: ${it?.name}|${it?.count}|${it?.firstTime}|${it?.lastTime}")
            assertNull(it)
        }

        println("============================================================")

        //resetting personalisation for further tests :
        defConfig.enablePersonalization(true)

        // when calling with default config, it will look for event's value in pref named "WizRocket_local_events" with key as <eventname>. when found, it will return the value as EventDetail Object
        appCtx.getSharedPreferences("WizRocket_local_events",Context.MODE_PRIVATE).edit().putString("eventName","123|456|789").commit()
        localDataStoreWithDefConfig.getEventDetail("eventName").let {
            println("Event: ${it?.name}|${it?.count}|${it?.firstTime}|${it?.lastTime}")
            assertEquals("eventName",it?.name)
            assertEquals(123,it?.count)
            assertEquals(456,it?.firstTime)
            assertEquals(789,it?.lastTime)

        }
        println("============================================================")
        // when calling with NON default config, it will look for event's value in pref named "WizRocket_local_events:<id>" with key as <eventname>:<id>. when found, it will return the value as EventDetail Object
        appCtx.getSharedPreferences("WizRocket_local_events:id",Context.MODE_PRIVATE).edit().putString("eventName22:id","111|222|333").commit()
        localDataStoreWithConfig.getEventDetail("eventName22").let {
            println("Event: ${it?.name}|${it?.count}|${it?.firstTime}|${it?.lastTime}")
            assertEquals("eventName22",it.name)
            assertEquals(111,it?.count)
            assertEquals(222,it?.firstTime)
            assertEquals(333,it?.lastTime)
        }
    }

    @Test
    fun test_getEventHistory_when_FunctionIsCalled_should_ReturnAMapOfEventNameAndDetails() {
        // if context is null,exception happens and null is returnd
        assertNull(localDataStoreWithDefConfig.getEventHistory(null))

        //if default config is used, events are stored in local_events pref file
        var results: Map<String, EventDetail> = localDataStoreWithDefConfig.getEventHistory(appCtx)
        assertTrue { results.isEmpty() }
        assertNull(results["event"])

        StorageHelper.getPreferences(appCtx, "local_events").edit().putString("event", "2|1648192535|1648627865").commit()
        results = localDataStoreWithDefConfig.getEventHistory(appCtx)
        assertTrue { results.isNotEmpty() }
        assertNotNull(results["event"])
        assertTrue { results["event"] is EventDetail }
        assertEquals(2, results["event"]?.count)
        assertEquals(1648192535, results["event"]?.firstTime)
        assertEquals(1648627865, results["event"]?.lastTime)


        //if  default instance is not used, events are stored in "local_events:$accountID" pref file
        results = localDataStoreWithConfig.getEventHistory(appCtx)
        assertTrue { results.isEmpty() }
        assertNull(results["event"])

        StorageHelper.getPreferences(appCtx, "local_events:id").edit().putString("event2", "33|1234|2234").commit()
        results = localDataStoreWithConfig.getEventHistory(appCtx)
        assertTrue { results.isNotEmpty() }
        assertNotNull(results["event2"])
        assertTrue { results["event2"] is EventDetail }
        assertEquals(33, results["event2"]?.count)
        assertEquals(1234, results["event2"]?.firstTime)
        assertEquals(2234, results["event2"]?.lastTime)

        // if shared pref is empty, should regturn empty map
        StorageHelper.getPreferences(appCtx, "local_events:id").edit().clear().commit()
        results = localDataStoreWithConfig.getEventHistory(appCtx)
        println("results="+results)
        assertTrue { results.isEmpty() }
        assertNull(results["event"])

    }

    @Test
    fun test_getProfileProperty_when_FunctionIsCalledWithSomeKey_should_ReturnAssociatedValue() {
        localDataStoreWithConfig.updateProfileFields(mapOf("key" to "val"))
        assertEquals("val", localDataStoreWithConfig.getProfileProperty("key"))
    }

    @Test
    fun test_getProfileProperty_when_FunctionIsCalledWithNullKey_should_ReturnNull() {
        localDataStoreWithConfig.getProfileProperty(null).let {
            assertNull(it)
        }
    }

    @Test
    fun test_getProfileProperty_when_FunctionIsCalledWithIncorrectKey_should_ReturnNull() {
        localDataStoreWithConfig.updateProfileFields(mapOf("key" to "val"))

        assertNull(localDataStoreWithConfig.getProfileProperty("key1"))
    }


    @Test
    fun test_getProfileProperty_when_FunctionIsCalledWithCorrectKeyAndEncryptedValue_should_ReturnNull() {
        localDataStoreWithConfig.updateProfileFields(mapOf("key" to "[abcd]"))

        assertNull(localDataStoreWithConfig.getProfileProperty("key"))
    }

    @Test
    fun test_persistEvent_when_ContextAndJsonAndTypeIsPassed_should_SaveDataToSharedPref() {
        val contextSpy = spyk(appCtx)

        // when context is null, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, null, -1)
        verify(exactly = 0) { contextSpy.getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE) }

        // when json is null, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, null, -1)
        verify(exactly = 0) { contextSpy.getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE) }


        // when  type is not  Constants.RAISED_EVENT, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, JSONObject(), -1)
        verify(exactly = 0) { contextSpy.getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE) }

        // when context and valid json object is passed type is Constants.RAISED_EVENT, but json doesn't have any value for "evtName" key, then the function calls persistEvent(ctx,jsonObject) but returns without saving anything
        val jsonObject = JSONObject()
        var jsonSpy = spyk(jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, jsonSpy, Constants.RAISED_EVENT)
        verify(exactly = 1) { jsonSpy.getString("evtName") }
        val prefs = contextSpy.getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)
        var valueForKey: String? = prefs.getString("someVal:id", null)
        assertEquals(null, valueForKey)


        // when context and valid json object is passed type is Constants.RAISED_EVENT, and json  have a value for "evtName" key, then the function calls persistEvent(ctx,jsonObject) and also initiates saving the value
        jsonObject.put("evtName", "someVal")
        jsonSpy = spyk(jsonObject)
        val expectedVal = "1|${System.currentTimeMillis() / 1000}|${System.currentTimeMillis() / 1000}"
        localDataStoreWithConfig.persistEvent(contextSpy, jsonSpy, Constants.RAISED_EVENT)
        verify(exactly = 1) { jsonSpy.getString("evtName") }
        valueForKey = prefs.getString("someVal:id", "")
        assertEquals(expectedVal, valueForKey)

    }

    @Test
    fun test_setDataSyncFlag_when_FunctionIsCalledWithAJson_should_AddABooleanKeyNamedDSyncToTheJson() {
        var json = JSONObject()
        var jsonSpy = spyk(json)

        // 1. when personalisation is disabled, it will add "dsync" =  false in our object AND RETURN.
        config.enablePersonalization(false)
        localDataStoreWithConfig.setDataSyncFlag(jsonSpy)
        assertFalse { jsonSpy.getBoolean("dsync") }
        verify(exactly = 0) { jsonSpy.getString("type") }

        // 2. if personalisation is enabled, it will contrinue for other checks and modification to original json will depend on other factors
        config.enablePersonalization(true)
        localDataStoreWithConfig.setDataSyncFlag(jsonSpy)
        verify(atLeast = 1) { jsonSpy.getString("type") }


        //3. if if json.get("type") is "profile"  , dsync will always be true
        json = JSONObject().also { it.put("type", "profile") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertTrue { json.getBoolean("dsync") }


        //4. if if json.get("type") is "event" and json.get("event") is  "App Launched" , dsync will always be true
        json = JSONObject().also {
            it.put("type", "profile")
            it.put("event", "App Launched")
        }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertTrue { json.getBoolean("dsync") }

        //5. if json.get("type") is null or ANYTHING ELSE APART FROM "event" or "profile", then it will
        //   add  "dsync" =  true or false based on current time and value of "local_cache_last_update"
        //   in cache. if the difference between current time and cache value is <20 mins dsync = true
        //   is added else dsync = false is added


        //assuming last update was just done now. then local_cache_last_update+20 mins > current time. therefore value of dsync should be false
        var lastUpdateTime = (System.currentTimeMillis() / 1000).toInt()
        StorageHelper.putInt(appCtx, "local_cache_last_update:id", lastUpdateTime)
        json = JSONObject().also { it.put("type", "other") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertFalse { json.getBoolean("dsync") }

        //assuming last update was 5 mins ago. then local_cache_last_update+20 mins > current time. therefore value of dsync should be false
        lastUpdateTime = (System.currentTimeMillis() / 1000).toInt() - (5 * 60)
        StorageHelper.putInt(appCtx, "local_cache_last_update:id", lastUpdateTime)
        json = JSONObject().also { it.put("type", "other") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertFalse { json.getBoolean("dsync") }

        //assuming last update was 50 mins ago. then local_cache_last_update+20 mins < current time. therefore value of dsync should be true
        lastUpdateTime = (System.currentTimeMillis() / 1000).toInt() - (50 * 60)
        StorageHelper.putInt(appCtx, "local_cache_last_update:id", lastUpdateTime)
        json = JSONObject().also { it.put("type", "other") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertTrue { json.getBoolean("dsync") }
    }

    @Test
    fun test_setProfileFields_when_MapsIsPassed_should_SetTheKeysInPROFILE_FIELDS_IN_THIS_SESSIONMap() {
        val fieldMap = mapOf(
            "key1" to "value1",
            "key2" to true,
            "key3" to null,
            "key4" to 2
        )

        localDataStoreWithConfig.updateProfileFields(fieldMap)

        assertEquals("value1", localDataStoreWithConfig.getProfileProperty("key1"))
        assertEquals(true, localDataStoreWithConfig.getProfileProperty("key2"))
        assertNull(localDataStoreWithConfig.getProfileProperty("key3"))
        assertEquals(2, localDataStoreWithConfig.getProfileProperty("key4"))
    }

    @Test
    fun `test persistUserEventLog when event name is null returns false`() {
        // When
        val result = localDataStoreWithConfig.persistUserEventLog(null)

        // Then
        assertFalse(result)
        verify(exactly = 0) { dbAdapter.userEventLogDAO() }
        verify(exactly = 0) { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(any(), any()) }
    }

    @Test
    fun `test persistUserEventLog when event exists updates event successfully`() {
        // Given
        every { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns true
        every { userEventLogDaoMock.updateEventByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns true

        // When
        val result = localDataStoreWithConfig.persistUserEventLog(eventName)

        // Then
        assertTrue(result)
        verify { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
        verify { userEventLogDaoMock.updateEventByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
        verify(exactly = 0) { userEventLogDaoMock.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `test persistUserEventLog when event exists but update fails returns false`() {
        // Given
        every { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns true
        every { userEventLogDaoMock.updateEventByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns false

        // When
        val result = localDataStoreWithConfig.persistUserEventLog(eventName)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test persistUserEventLog when event does not exist inserts successfully`() {
        // Given
        every { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns false
        every { userEventLogDaoMock.insertEvent(deviceInfo.deviceID, eventName, normalizedEventName) } returns 1L

        // When
        val result = localDataStoreWithConfig.persistUserEventLog(eventName)

        // Then
        assertTrue(result)
        verify { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
        verify { userEventLogDaoMock.insertEvent(deviceInfo.deviceID, eventName, normalizedEventName) }
        verify(exactly = 0) { userEventLogDaoMock.updateEventByDeviceIdAndNormalizedEventName(any(), any()) }
    }

    @Test
    fun `test persistUserEventLog when event does not exist and insert fails returns false`() {
        // Given
        every { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns false
        every { userEventLogDaoMock.insertEvent(deviceInfo.deviceID, eventName, normalizedEventName) } returns -1L

        // When
        val result = localDataStoreWithConfig.persistUserEventLog(eventName)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test persistUserEventLog when exception occurs returns false`() {
        // Given
        every { userEventLogDaoMock.eventExistsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } throws RuntimeException("DB Error")

        // When
        val result = localDataStoreWithConfig.persistUserEventLog(eventName)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test persistUserEventLogsInBulk success`() {
        // Given
        every { userEventLogDaoMock.upsertEventsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, setOfActualAndNormalizedEventNamePair) } returns true

        // When
        val result = localDataStoreWithConfig.persistUserEventLogsInBulk(eventNames)

        // Then
        assertTrue(result)
        verify { userEventLogDaoMock.upsertEventsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, setOfActualAndNormalizedEventNamePair) }
    }

    @Test
    fun `test persistUserEventLogsInBulk when operation fails returns false`() {
        // Given
        every { userEventLogDaoMock.upsertEventsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, setOfActualAndNormalizedEventNamePair) } returns false

        // When
        val result = localDataStoreWithConfig.persistUserEventLogsInBulk(eventNames)

        // Then
        assertFalse(result)
        verify { userEventLogDaoMock.upsertEventsByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, setOfActualAndNormalizedEventNamePair) }
    }

    @Test
    fun `test isUserEventLogFirstTime when count is 1 returns true`() {
        // Given
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns 1

        // When
        val result = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertTrue(result)
        verify { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test isUserEventLogFirstTime when count is greater than 1 returns false`() {
        // Given
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns 2

        // When
        val result = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertFalse(result)
        verify { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test isUserEventLogFirstTime caches result for subsequent calls`() {
        // Given
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns 2

        // When
        val firstCall = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)
        val secondCall = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertFalse(firstCall)
        assertFalse(secondCall)
        // Should only call readEventCountByDeviceID once as result is cached
        verify(exactly = 1) { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test isUserEventLogFirstTime when count is 0 returns false`() {
        // Given
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns 0

        // When
        val result = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertFalse(result)
        verify { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test isUserEventLogFirstTime when count is -1 returns false`() {
        // Given
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns -1

        // When
        val result = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertFalse(result)
        verify { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test isUserEventLogFirstTime behavior with changing event counts`() {
        // Given

        // First call setup - count 0
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns 0

        // When - First call
        val firstCallResult = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertFalse(firstCallResult)
        verify { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }

        // Given - Second call setup - count 1
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns 1

        // When - Second call
        val secondCallResult = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertTrue(secondCallResult)
        verify(exactly = 2) { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }

        // Given - Third call setup - count 2
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns 2

        // When - Third call
        val thirdCallResult = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertFalse(thirdCallResult)
        verify(exactly = 3) { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }

        // When - Fourth call (should use cached result)
        val fourthCallResult = localDataStoreWithConfig.isUserEventLogFirstTime(eventName)

        // Then
        assertFalse(fourthCallResult)
        // Should not make additional DB call as result is now cached
        verify(exactly = 3) { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test cleanUpExtraEvents success`() {
        // Given
        val threshold = 5
        val numberOfRowsToCleanup = 2
        every { userEventLogDaoMock.cleanUpExtraEvents(threshold, numberOfRowsToCleanup) } returns true

        // When
        val result = localDataStoreWithConfig.cleanUpExtraEvents(threshold, numberOfRowsToCleanup)

        // Then
        assertTrue(result)
        verify { userEventLogDaoMock.cleanUpExtraEvents(threshold, numberOfRowsToCleanup) }
    }

    @Test
    fun `test readUserEventLog success`() {
        // Given
        val userEventLog = UserEventLogTestData.EventNames.sampleUserEventLogsForSameDeviceId[0]
        every { userEventLogDaoMock.readEventByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns userEventLog

        // When
        val result = localDataStoreWithConfig.readUserEventLog(eventName)

        // Then
        assertNotNull(result)
        assertEquals(userEventLog, result)
        verify { userEventLogDaoMock.readEventByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test readUserEventLogCount returns correct count when event exists`() {
        // Given
        val expectedCount = 5
        every { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) } returns expectedCount

        // When
        val result = localDataStoreWithConfig.readUserEventLogCount(eventName)

        // Then
        assertEquals(expectedCount, result)
        verify { userEventLogDaoMock.readEventCountByDeviceIdAndNormalizedEventName(deviceInfo.deviceID, normalizedEventName) }
    }

    @Test
    fun `test readUserEventLogs returns correct event list for device`() {
        // Given
        val expectedLogs = UserEventLogTestData.EventNames.sampleUserEventLogsForSameDeviceId
        every { userEventLogDaoMock.allEventsByDeviceID(deviceInfo.deviceID) } returns expectedLogs

        // When
        val result = localDataStoreWithConfig.readUserEventLogs()

        // Then
        assertEquals(expectedLogs, result)
        verify { userEventLogDaoMock.allEventsByDeviceID(deviceInfo.deviceID) }
    }

    @Test
    fun `test readEventLogsForAllUsers returns correct event list`() {
        // Given
        val expectedLogs = UserEventLogTestData.EventNames.sampleUserEventLogsForMixedDeviceId
        every { userEventLogDaoMock.allEvents() } returns expectedLogs

        // When
        val result = localDataStoreWithConfig.readEventLogsForAllUsers()

        // Then
        assertEquals(expectedLogs, result)
        verify { userEventLogDaoMock.allEvents() }
    }
}
