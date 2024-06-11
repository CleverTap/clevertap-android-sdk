package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class LocalDataStoreTest : BaseTestCase() {
    private lateinit var defConfig: CleverTapInstanceConfig
    private lateinit var config: CleverTapInstanceConfig

    private lateinit var localDataStoreWithDefConfig: LocalDataStore
    private lateinit var localDataStoreWithConfig: LocalDataStore
    private lateinit var localDataStoreWithConfigSpy: LocalDataStore
    private lateinit var cryptHandler : CryptHandler
    private lateinit var deviceInfo : DeviceInfo

    override fun setUp() {
        super.setUp()
        val metaData = CoreMetaData()
        defConfig = CleverTapInstanceConfig.createDefaultInstance(appCtx, "id", "token", "region")
        cryptHandler = CryptHandler(0, CryptHandler.EncryptionAlgorithm.AES, "id")
        deviceInfo = MockDeviceInfo(appCtx, defConfig, "id", metaData)
        localDataStoreWithDefConfig = LocalDataStore(appCtx, defConfig, cryptHandler, deviceInfo)
        config = CleverTapInstanceConfig.createInstance(appCtx, "id", "token", "region")
        localDataStoreWithConfig = LocalDataStore(appCtx, config, cryptHandler, deviceInfo)
        localDataStoreWithConfigSpy = Mockito.spy(localDataStoreWithConfig)
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
        var results: Map<String, EventDetail> = mutableMapOf()

        //if default config is used, events are stored in local_events pref file
        results = localDataStoreWithDefConfig.getEventHistory(appCtx)
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
        val contextSpy = Mockito.spy(appCtx)

        // when context is null, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, null, -1)
        Mockito.verify(contextSpy, Mockito.times(0)).getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)

        // when json is null, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, null, -1)
        Mockito.verify(contextSpy, Mockito.times(0)).getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)


        // when  type is not  Constants.RAISED_EVENT, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, JSONObject(), -1)
        Mockito.verify(contextSpy, Mockito.times(0)).getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)

        // when context and valid json object is passed type is Constants.RAISED_EVENT, but json doesn't have any value for "evtName" key, then the function calls persistEvent(ctx,jsonObject) but returns without saving anything
        val jsonObject = JSONObject()
        var jsonSpy = Mockito.spy(jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, jsonSpy, Constants.RAISED_EVENT)
        Mockito.verify(jsonSpy, Mockito.times(1)).getString("evtName")
        val prefs = contextSpy.getSharedPreferences("WizRocket_local_events:id",Context.MODE_PRIVATE)
        var valueForKey:String? = prefs.getString("someVal:id",null)
        assertEquals(null,valueForKey)


        // when context and valid json object is passed type is Constants.RAISED_EVENT, and json  have a value for "evtName" key, then the function calls persistEvent(ctx,jsonObject) and also initiates saving the value
        jsonObject.put("evtName", "someVal")
        jsonSpy = Mockito.spy(jsonObject)
        val expectedVal = "1|${System.currentTimeMillis() / 1000}|${System.currentTimeMillis() / 1000}"
        localDataStoreWithConfig.persistEvent(contextSpy, jsonSpy, Constants.RAISED_EVENT)
        Mockito.verify(jsonSpy, Mockito.times(1)).getString("evtName")
        valueForKey = prefs.getString("someVal:id","")
        assertEquals(expectedVal,valueForKey)

    }

    @Test
    fun test_setDataSyncFlag_when_FunctionIsCalledWithAJson_should_AddABooleanKeyNamedDSyncToTheJson() {
        var json = JSONObject()
        var jsonSpy = Mockito.spy(json)

        // 1. when personalisation is disabled, it will add "dsync" =  false in our object AND RETURN.
        config.enablePersonalization(false)
        localDataStoreWithConfig.setDataSyncFlag(jsonSpy)
        assertFalse { jsonSpy.getBoolean("dsync") }
        Mockito.verify(jsonSpy, Mockito.never()).getString("type")

        // 2. if personalisation is enabled, it will contrinue for other checks and modification to original json will depend on other factors
        config.enablePersonalization(true)
        localDataStoreWithConfig.setDataSyncFlag(jsonSpy)
        Mockito.verify(jsonSpy, Mockito.atLeastOnce()).getString("type")


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
}