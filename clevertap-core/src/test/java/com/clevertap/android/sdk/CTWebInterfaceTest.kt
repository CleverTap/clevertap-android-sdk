package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTWebInterfaceTest : BaseTestCase() {
    // CTWebInterface is a wrapper around some functions of the clevertap api
    private lateinit var ctWebInterface: CTWebInterface
    private var ctApi:CleverTapAPI? = null
    override fun setUp() {
        super.setUp()

        ctApi = CleverTapAPI.getDefaultInstance(application)
        ctWebInterface = CTWebInterface(ctApi)

    }

    @Test
    fun test_addMultiValueForKey_when_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.addMultiValueForKey("key","value")


        // when ctApi is not null, calling this function will call ctApi's internal function
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctSpy)
        ctWebInterface.addMultiValueForKey("key2","value2")
        Mockito.verify(ctSpy,Mockito.atLeastOnce())?.addMultiValueForKey("key2","value2")


    }

    @Test
    fun test_addMultiValuesForKey_when_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.addMultiValuesForKey("key","[{'k1':'v1'},{'k2':'v2'}]")

        // when ctApi is not null, but key(or value) is null,  calling this function will do nothing.
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctSpy)
        ctWebInterface.addMultiValuesForKey(null,"[{'k1':'v1'},{'k2':'v2'}]")
        Mockito.verify(ctSpy,Mockito.never())?.addMultiValueForKey(null,"[{'k1':'v1'},{'k2':'v2'}]")

        ctWebInterface.addMultiValuesForKey("keyx",null)
        Mockito.verify(ctSpy,Mockito.never())?.addMultiValueForKey("keyx",null)


        //when ctApi and key and value  are all not null, calling this function will call ctApi's internal function
        ctWebInterface.addMultiValuesForKey("key2","[{'k1':'v1'},{'k2':'v2'}]")
        Mockito.verify(ctSpy,Mockito.atMostOnce())?.addMultiValueForKey("key2","[{'k1':'v1'},{'k2':'v2'}]")


    }

    @Test
    fun test_pushChargedEvent_when_FunctionIsCalledWithJsonAndJsonArray_should_CallClevertapApiFunctionWithData() {
        var eventName: String? = null
        var eventValues: String? = null
        ctWebInterface.pushChargedEvent(eventName, eventValues)

        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.pushChargedEvent("key", "[{'k1':'v1'},{'k2':'v2'}]")

        // when ctApi is not null, following actions will happen
        //1. if either the eventname or values is null, function will return without any action
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctApiSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctApiSpy)
        eventName = "{'event':'eventName'}"
        eventValues = "[{'k1':'v1'},{'k2':'v2'}]"

        ctWebInterface.pushChargedEvent(eventName, null)
        Mockito.verify(ctApiSpy, Mockito.never())?.pushChargedEvent(Mockito.any(), Mockito.any())

        ctWebInterface.pushChargedEvent(null, eventValues)
        Mockito.verify(ctApiSpy, Mockito.never())?.pushChargedEvent(Mockito.any(), Mockito.any())

        // if neither is null, ctApi's  function will get called

        ctWebInterface.pushChargedEvent(eventName, eventValues)
        val eventDetails = Utils.convertJSONObjectToHashMap(JSONObject(eventName))
        val eventData = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(JSONArray(eventValues))
        println("eventDetails:$eventDetails")
        println("eventData:$eventData")
        Mockito.verify(ctApiSpy, Mockito.atLeastOnce())?.pushChargedEvent(eventDetails, eventData)


    }

    @Test
    fun test_pushEvent_when_FunctionIsCalledWithEventNameAndProperties_should_CallAssocClevertapApiFunctionWithTransformedData() {
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctApiSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctApiSpy)

        ctWebInterface.pushEvent("event")
        Mockito.verify(ctApiSpy, Mockito.atLeastOnce())?.pushEvent("event")


        ctWebInterface.pushEvent("event2","{'k1':'v1'}")
        val eventData = Utils.convertJSONObjectToHashMap(JSONObject("{'k1':'v1'}"))
        Mockito.verify(ctApiSpy, Mockito.atLeastOnce())?.pushEvent("event2",eventData)

    }

    @Test
    fun test_pushProfile_when_CalledWithJsonString_should_CallAssocClevertapApiFunction() {
        // if profile is null, function returns without any changes
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctApiSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctApiSpy)
        var profile:String? = null
        ctWebInterface.pushProfile(profile)
        Mockito.verify(ctApiSpy, Mockito.never())?.pushProfile(Mockito.anyMap())

        // if profile is not null, function calls associated CT api function
        profile = "{'key1':'value1'}"
        ctWebInterface.pushProfile(profile)
        Mockito.verify(ctApiSpy, Mockito.atMostOnce())?.pushProfile(Utils.convertJSONObjectToHashMap(JSONObject(profile)))



    }

    @Test
    fun test_removeMultiValueForKey_when_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.removeMultiValueForKey("key", "value")


        // when ctApi , key and value are not null, calling this function will call ctApi's internal function
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctSpy)

        ctWebInterface.removeMultiValueForKey(null, "value21")
        Mockito.verify(ctSpy, Mockito.never())?.removeMultiValueForKey(null, "value21")

        ctWebInterface.removeMultiValueForKey("key21", null)
        Mockito.verify(ctSpy, Mockito.never())?.removeMultiValueForKey("key21", null)


        ctWebInterface.removeMultiValueForKey("key2", "value2")
        Mockito.verify(ctSpy, Mockito.atLeastOnce())?.removeMultiValueForKey("key2", "value2")
    }

    @Test
    fun test_removeMultiValuesForKey_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.removeMultiValuesForKey("key","[{'k1':'v1'},{'k2':'v2'}]")

        // when ctApi is not null, but key(or value) is null,  calling this function will do nothing.
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctSpy)
        ctWebInterface.removeMultiValuesForKey(null,"[{'k1':'v1'},{'k2':'v2'}]")
        Mockito.verify(ctSpy,Mockito.never())?.removeMultiValuesForKey(null,Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]")))

        ctWebInterface.removeMultiValuesForKey("keyx",null)
        Mockito.verify(ctSpy,Mockito.never())?.removeMultiValuesForKey("keyx",null)


        //when ctApi and key and value  are all not null, calling this function will call ctApi's internal function
        ctWebInterface.removeMultiValuesForKey("key2","[{'k1':'v1'},{'k2':'v2'}]")
        Mockito.verify(ctSpy,Mockito.atMostOnce())?.removeMultiValuesForKey("key2",Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]")))


    }

    @Test
    fun test_removeValueForKey_when_CalledWithKey_should_CallAssocClevertapApiFunction() {
        // if key is null, function returns without any changes
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctApiSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctApiSpy)
        var key:String? = null
        ctWebInterface.removeValueForKey(key)
        Mockito.verify(ctApiSpy, Mockito.never())?.removeValueForKey(key)

        // if profile is not null, function calls associated CT api function
        key = "key"
        ctWebInterface.removeValueForKey(key)
        Mockito.verify(ctApiSpy, Mockito.atMostOnce())?.removeValueForKey(key)

    }

    @Test
    fun test_setMultiValueForKey_when_ABC_should_XYZ() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.setMultiValueForKey("key","[{'k1':'v1'},{'k2':'v2'}]")

        // when ctApi is not null, but key(or value) is null,  calling this function will do nothing.
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctSpy = Mockito.spy(ctApi)
        ctWebInterface = CTWebInterface(ctSpy)
        ctWebInterface.setMultiValueForKey(null,"[{'k1':'v1'},{'k2':'v2'}]")
        Mockito.verify(ctSpy,Mockito.never())?.setMultiValuesForKey(null,Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]")))

        ctWebInterface.setMultiValueForKey("keyx",null)
        Mockito.verify(ctSpy,Mockito.never())?.setMultiValuesForKey("keyx",null)


        //when ctApi and key and value  are all not null, calling this function will call ctApi's internal function
        ctWebInterface.setMultiValueForKey("key2","[{'k1':'v1'},{'k2':'v2'}]")
        Mockito.verify(ctSpy,Mockito.atMostOnce())?.setMultiValuesForKey("key2",Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]")))
    }
}