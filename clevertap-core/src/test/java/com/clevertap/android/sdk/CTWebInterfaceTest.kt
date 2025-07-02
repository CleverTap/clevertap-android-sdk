package com.clevertap.android.sdk

import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment
import com.clevertap.android.sdk.inapp.InAppActionType.CLOSE
import com.clevertap.android.sdk.inapp.InAppActionType.CUSTOM_CODE
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTWebInterfaceTest : BaseTestCase() {
    // CTWebInterface is a wrapper around some functions of the clevertap api
    private lateinit var ctWebInterface: CTWebInterface
    private var ctApi:CleverTapAPI? = null
    private  val inputs: List<Number> = listOf(
        1,Int.MAX_VALUE,Int.MIN_VALUE,-1,0,
        2.5f, Float.MAX_VALUE,Float.MIN_VALUE,-2.5f,0f,
        1.5,Double.MAX_VALUE,Double.MIN_VALUE,-1.5,0.0,
        50L,Long.MAX_VALUE,Long.MIN_VALUE,-50L,0L
    )
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
        val ctMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.addMultiValueForKey("key2","value2")
        verify(exactly = 1) { ctMock.addMultiValueForKey("key2", "value2") }
    }


    @Test
    fun test_incrementValue_when_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.incrementValue("key",1.5)


        // when ctApi is not null, calling this function will call ctApi's internal function
        ctApi = CleverTapAPI.getDefaultInstance(application)

        inputs.forEach  {
            val ctMock = mockk<CleverTapAPI>(relaxed = true)
            ctWebInterface = CTWebInterface(ctMock)

            ctWebInterface.incrementValue("key2",it.toDouble())
            verify(exactly = 1) { ctMock.incrementValue("key2", it.toDouble()) }
        }
    }



    @Test
    fun test_decrementValue_when_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.decrementValue("key",1.5)


        // when ctApi is not null, calling this function will call ctApi's internal function
        ctApi = CleverTapAPI.getDefaultInstance(application)

        inputs.forEach {
            val ctMock = mockk<CleverTapAPI>(relaxed = true)
            ctWebInterface = CTWebInterface(ctMock)

            ctWebInterface.decrementValue("key2",it.toDouble())
            verify(exactly = 1) { ctMock.decrementValue("key2", it.toDouble()) }
        }
    }



    @Test
    fun test_addMultiValuesForKey_when_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.addMultiValuesForKey("key","[{'k1':'v1'},{'k2':'v2'}]")

        // when ctApi is not null, but key(or value) is null,  calling this function will do nothing.
        ctApi = CleverTapAPI.getDefaultInstance(application)
        var ctMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.addMultiValuesForKey(null,"[{'k1':'v1'},{'k2':'v2'}]")
        var valuesArray = JSONArray("[{'k1':'v1'},{'k2':'v2'}]")
        var expectedVal =  Utils.convertJSONArrayToArrayList(valuesArray)
        verify(exactly = 0) { ctMock.addMultiValuesForKey(null, expectedVal) }

        ctWebInterface.addMultiValuesForKey("keyx",null)
        verify(exactly = 0) { ctMock.addMultiValuesForKey("keyx", null) }


        //when ctApi and key and value  are all not null, calling this function will call ctApi's internal function
        ctWebInterface.addMultiValuesForKey("key2","[{'k1':'v1'},{'k2':'v2'}]")
         valuesArray = JSONArray("[{'k1':'v1'},{'k2':'v2'}]")
         expectedVal =  Utils.convertJSONArrayToArrayList(valuesArray)
        verify(exactly = 1) { ctMock.addMultiValuesForKey("key2", expectedVal) }

        // when passed json is malformed, no function will be called
        ctMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.addMultiValuesForKey("key2","[{'k1':'v1'")
        verify(exactly = 0) { ctMock.addMultiValuesForKey("key2", expectedVal) }
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
        var ctApiMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctApiMock)
        eventName = "{'event':'eventName'}"
        eventValues = "[{'k1':'v1'},{'k2':'v2'}]"

        ctWebInterface.pushChargedEvent(eventName, null)
        verify(exactly = 0) { ctApiMock.pushChargedEvent(any(), any()) }

        ctWebInterface.pushChargedEvent(null, eventValues)
        verify(exactly = 0) { ctApiMock.pushChargedEvent(any(), any()) }

        // if neither is null, ctApi's  function will get called
        ctWebInterface.pushChargedEvent(eventName, eventValues)
        val eventDetails = Utils.convertJSONObjectToHashMap(JSONObject(eventName))
        val eventData = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(JSONArray(eventValues))
        verify(exactly = 1) { ctApiMock.pushChargedEvent(eventDetails, eventData) }

       // when passed json is malformed, no function will be called
        ctApiMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.pushChargedEvent(eventName, "{'k2':'v2'}]")
        verify(exactly = 0) { ctApiMock.pushChargedEvent(eventDetails, eventData) }

    }

    @Test
    fun test_pushEvent_when_FunctionIsCalledWithEventNameAndProperties_should_CallAssocClevertapApiFunctionWithTransformedData() {
        ctApi = CleverTapAPI.getDefaultInstance(application)
        var ctApiMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.pushEvent("event")
        verify(exactly = 1) { ctApiMock.pushEvent("event") }

        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.pushEvent("event2","{'k1':'v1'}")
        val eventData = Utils.convertJSONObjectToHashMap(JSONObject("{'k1':'v1'}"))
        verify(exactly = 1) { ctApiMock.pushEvent("event2", eventData) }

        // if actions are null, not assoc api function will be called
        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.pushEvent("event3",null)
        verify(exactly = 0) { ctApiMock.pushEvent("event3", eventData) }

        // if json is malformed, not assoc api function will be called
        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.pushEvent("event3","{''v1'}")
        verify(exactly = 0) { ctApiMock.pushEvent("event3", eventData) }
    }

    @Test
    fun test_pushProfile_when_CalledWithJsonString_should_CallAssocClevertapApiFunction() {
        // if profile is null, function returns without any changes
        ctApi = CleverTapAPI.getDefaultInstance(application)
        var ctApiMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.pushProfile(null)
        verify(exactly = 0) { ctApiMock.pushProfile(any()) }

        // if profile is not null, function calls associated CT api function
        ctWebInterface = CTWebInterface(ctApiMock)
        var profile = "{'key1':'value1'}"
        ctWebInterface.pushProfile(profile)
        verify(exactly = 1) {
            ctApiMock.pushProfile(
                Utils.convertJSONObjectToHashMap(
                    JSONObject(
                        profile
                    )
                )
            )
        }

        // if json is malformed, not assoc api function will be called
        ctWebInterface = CTWebInterface(ctApiMock)
        profile = "{'key2':'value2'}"
        ctWebInterface.pushProfile(":'value1'}")
        verify(exactly = 0) {
            ctApiMock.pushProfile(
                Utils.convertJSONObjectToHashMap(
                    JSONObject(
                        profile
                    )
                )
            )
        }

    }

    @Test
    fun test_onUserLogin_when_CalledWithJsonString_should_CallAssocClevertapApiFunction() {
        // if profile is null, function returns without any changes
        ctApi = CleverTapAPI.getDefaultInstance(application)
        var ctApiMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.onUserLogin(null)
        verify(exactly = 0) { ctApiMock.onUserLogin(any()) }

        // if profile is not null, function calls associated CT api function
        ctWebInterface = CTWebInterface(ctApiMock)
        var profile = "{'key1':'value1'}"
        ctWebInterface.onUserLogin(profile)
        verify(exactly = 1) {
            ctApiMock.onUserLogin(
                Utils.convertJSONObjectToHashMap(
                    JSONObject(
                        profile
                    )
                )
            )
        }

        // if json is malformed, not assoc api function will be called
        ctWebInterface = CTWebInterface(ctApiMock)
        profile = "{'key2':'value2'}"
        ctWebInterface.onUserLogin(":'value1'}")
        verify(exactly = 0) {
            ctApiMock.onUserLogin(
                Utils.convertJSONObjectToHashMap(
                    JSONObject(
                        profile
                    )
                )
            )
        }
    }



    @Test
    fun test_removeMultiValueForKey_when_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.removeMultiValueForKey("key", "value")


        // when ctApi , key and value are not null, calling this function will call ctApi's internal function
        ctApi = CleverTapAPI.getDefaultInstance(application)
        val ctMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctMock)

        ctWebInterface.removeMultiValueForKey(null, "value21")
        verify(exactly = 0) { ctMock.removeMultiValueForKey(null, "value21") }

        ctWebInterface.removeMultiValueForKey("key21", null)
        verify(exactly = 0) { ctMock.removeMultiValueForKey("key21", null) }


        ctWebInterface.removeMultiValueForKey("key2", "value2")
        verify(exactly = 1) { ctMock.removeMultiValueForKey("key2", "value2") }
    }

    @Test
    fun test_removeMultiValuesForKey_CalledWithKeyAndValue_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.removeMultiValuesForKey("key","[{'k1':'v1'},{'k2':'v2'}]")

        // when ctApi is not null, but key(or value) is null,  calling this function will do nothing.
        ctApi = CleverTapAPI.getDefaultInstance(application)
        var ctMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.removeMultiValuesForKey(null,"[{'k1':'v1'},{'k2':'v2'}]")
        verify(exactly = 0) {
            ctMock.removeMultiValuesForKey(
                null,
                Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]"))
            )
        }

        ctWebInterface.removeMultiValuesForKey("keyx",null)
        verify(exactly = 0) { ctMock.removeMultiValuesForKey("keyx", null) }


        //when ctApi and key and value  are all not null, calling this function will call ctApi's internal function
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.removeMultiValuesForKey("key2","[{'k1':'v1'},{'k2':'v2'}]")
        verify(exactly = 1) {
            ctMock.removeMultiValuesForKey(
                "key2",
                Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]"))
            )
        }


        // when passed json is malformed, no function will be called
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.removeMultiValuesForKey("key22","'k1':'v1'},{'k2':'v2'}]")
        verify(exactly = 0) {
            ctMock.removeMultiValuesForKey(
                "key22",
                Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]"))
            )
        }



    }

    @Test
    fun test_removeValueForKey_when_CalledWithKey_should_CallAssocClevertapApiFunction() {
        // if key is null, function returns without any changes
        ctApi = CleverTapAPI.getDefaultInstance(application)
        var ctApiMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctApiMock)
        ctWebInterface.removeValueForKey(null)
        verify(exactly = 0) { ctApiMock.removeValueForKey(null) }

        // if profile is not null, function calls associated CT api function
        val key = "key"
        ctWebInterface.removeValueForKey(key)
        verify(exactly = 1) { ctApiMock.removeValueForKey(key) }

    }

    @Test
    fun test_setMultiValueForKey_when_KeyAndJsonArrayStringIsPassed_should_CallAssocClevertapApiFunction() {
        // when ctApi is null, calling this function will do nothing.
        ctApi = null
        ctWebInterface = CTWebInterface(ctApi)
        ctWebInterface.setMultiValueForKey("key","[{'k0':'v0'},{'k20':'v20'}]")

        ctApi = CleverTapAPI.getDefaultInstance(application)
        var ctMock = mockk<CleverTapAPI>(relaxed = true)

        // when ctApi is not null, but key(or value) is null,  calling this function will do nothing.
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.setMultiValueForKey(null,"[{'k1':'v1'},{'k2':'v2'}]")
        verify(exactly = 0) {
            ctMock.setMultiValuesForKey(
                null,
                Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]"))
            )
        }

        ctMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.setMultiValueForKey("keyx",null)
        verify(exactly = 0) { ctMock.setMultiValuesForKey("keyx", null) }


        //when ctApi and key and value  are all not null, calling this function will call ctApi's internal function
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.setMultiValueForKey("key2","[{'k1':'v1'},{'k2':'v2'}]")
        verify(exactly = 1) {
            ctMock.setMultiValuesForKey(
                "key2",
                Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]"))
            )
        }

        // when passed json is malformed, no function will be called
        ctMock = mockk<CleverTapAPI>(relaxed = true)
        ctWebInterface = CTWebInterface(ctMock)
        ctWebInterface.setMultiValueForKey("key2","'k1':'v1',{'k2':'v2'}]")
        verify(exactly = 0) {
            ctMock.setMultiValuesForKey(
                "key2",
                Utils.convertJSONArrayToArrayList(JSONArray("[{'k1':'v1'},{'k2':'v2'}]"))
            )
        }
    }

    @Test
    fun `triggerAction should call InAppBaseFragment when provided correct parameters`() {
        val fragmentMock = mockk<CTInAppBaseFragment>(relaxed = true)
        val webInterface = CTWebInterface(mockk<CleverTapAPI>(relaxed = true), fragmentMock)

        val closeActionJson = """{
            "type":"$CLOSE",
            "templateId": "__close_notification",
            "android": "",
            "ios": "",
            "vars": {},
            "kv": {}
        }"""

        webInterface.triggerInAppAction(closeActionJson, "close", null)
        verify { fragmentMock.triggerAction(match { it.type == CLOSE }, "close", any()) }
        clearMocks(fragmentMock)

        val customTemplateAction = """{
            "type": "$CUSTOM_CODE",
            "templateId": "660598688e5e1e44f417e91e",
            "vars": {
                "var1": true,
                "var2": "Text",
                "var3": 123
            },
            "templateName": "function-a",
            "templateDescription": "Description"
        }"""
        webInterface.triggerInAppAction(customTemplateAction, "function-a", "buttonId")
        verify { fragmentMock.triggerAction(match { it.type == CUSTOM_CODE }, "function-a", any()) }
    }

    @Test
    fun `triggerAction should not call InAppBaseFragment when provided invalid params`() {
        val fragmentMock = mockk<CTInAppBaseFragment>(relaxed = true)
        val webInterface = CTWebInterface(mockk<CleverTapAPI>(relaxed = true), fragmentMock)

        webInterface.triggerInAppAction("close", "action", null)
        verify { fragmentMock wasNot called }

        webInterface.triggerInAppAction(null, null, null)
        verify { fragmentMock wasNot called }
    }

    @Test
    fun `triggerAction should do nothing when CleverTapAPI or InAppBaseFragment is null`() {
        CTWebInterface(null, null).triggerInAppAction(null, null, null)
        CTWebInterface(mockk(), null).triggerInAppAction(null, null, null)

        val fragmentMock = mockk<CTInAppBaseFragment>(relaxed = true)
        CTWebInterface(null, fragmentMock).triggerInAppAction(null, null, null)
        verify { fragmentMock wasNot called }
    }

}
