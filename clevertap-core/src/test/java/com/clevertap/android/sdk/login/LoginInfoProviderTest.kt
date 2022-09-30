package com.clevertap.android.sdk.login

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LoginInfoProviderTest: BaseTestCase() {

    private lateinit var defConfig: CleverTapInstanceConfig
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var coreMetaData: CoreMetaData

    private lateinit var loginInfoProvider: LoginInfoProvider
    private lateinit var loginInfoProviderSpy: LoginInfoProvider

    override fun setUp() {
        super.setUp()
        coreMetaData = CoreMetaData()
        defConfig =  CleverTapInstanceConfig.createInstance(appCtx, "id", "token", "region")
        deviceInfo = Mockito.mock(DeviceInfo::class.java)


        loginInfoProvider = LoginInfoProvider(appCtx,defConfig,deviceInfo)
        loginInfoProviderSpy = Mockito.spy(loginInfoProvider)
    }

    @Test
    fun test_cacheGUIDForIdentifier_when_all_keys_are_correct_all_values_are_saved() {
        val guid = "__1234567"
        val key = "email"
        val identifier = "abc@gmail.com"

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)


        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)


        assertEquals("{\"email_abc@gmail.com\":\"__1234567\"}",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_cacheGUIDForIdentifier_when_key_is_empty_value_is_saved_without_key() {
        val guid = "__1234567"
        val key = ""
        val identifier = "abc@gmail.com"

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        assertEquals("{\"_abc@gmail.com\":\"__1234567\"}",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_cacheGUIDForIdentifier_when_identifier_is_empty_value_is_saved_without_identifier() {
        val guid = "__1234567"
        val key = "email"
        val identifier = ""

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        assertEquals("{\"email_\":\"__1234567\"}",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_cacheGUIDForIdentifier_when_guid_is_empty_value_is_saved_without_guid() {
        val guid = ""
        val key = "email"
        val identifier = "abc@gmail.com"

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)


        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)


        assertEquals("{\"email_abc@gmail.com\":\"\"}",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_when_cache_data_contains_email_and_identity_remove_provided_key() {
        val guid = "__1234567"
        val key = "Email"

        val jsonObj = JSONObject()
        jsonObj.put("Email_donjoe2862@gmail.com","__1234567")
        jsonObj.put("Identity_00002","__1234567")

        Mockito.`when`(loginInfoProviderSpy.cachedGUIDs).thenReturn(
            jsonObj)

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)
        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        //Assert
        assertEquals("{\"Identity_00002\":\"__1234567\"}",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_when_cache_data_contains_email_removes_cached_shared_prefs_key() {
        val guid = "__1234567"
        val key = "Email"

        val jsonObj = JSONObject()
        jsonObj.put("Email_donjoe2862@gmail.com","__1234567")

        Mockito.`when`(loginInfoProviderSpy.cachedGUIDs).thenReturn(
            jsonObj)

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)
        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        //Assert
        assertEquals("",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_when_cache_data_contains_lowercase_email_removes_cached_shared_prefs_key() {
        val guid = "__1234567"
        val key = "email"

        val jsonObj = JSONObject()
        jsonObj.put("Email_donjoe2862@gmail.com","__1234567")

        Mockito.`when`(loginInfoProviderSpy.cachedGUIDs).thenReturn(
            jsonObj)

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)
        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        //Assert
        assertEquals("",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_when_cache_data_contains_lowercase_email_removes_cached_shared_prefs_lowercase_key() {
        val guid = "__1234567"
        val key = "Email"

        val jsonObj = JSONObject()
        jsonObj.put("email_donjoe2862@gmail.com","__1234567")

        Mockito.`when`(loginInfoProviderSpy.cachedGUIDs).thenReturn(
            jsonObj)

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)
        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        //Assert
        assertEquals("",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_when_cache_data_contains_lowercase_email_and_identity_removes_cached_shared_prefs_lowercase_key(){
        val guid = "__1234567"
        val key = "Email"

        val jsonObj = JSONObject()
        jsonObj.put("email_donjoe2862@gmail.com","__1234567")
        jsonObj.put("identity_00002","__1234567")

        Mockito.`when`(loginInfoProviderSpy.cachedGUIDs).thenReturn(
            jsonObj)

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)
        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        //Assert
        assertEquals("{\"identity_00002\":\"__1234567\"}",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_when_cache_data_contains_Email_and_lowercase_identity_removes_cached_shared_prefs_key(){
        val guid = "__1234567"
        val key = "Email"

        val jsonObj = JSONObject()
        jsonObj.put("Email_donjoe2862@gmail.com","__1234567")
        jsonObj.put("identity_00002","__1234567")

        Mockito.`when`(loginInfoProviderSpy.cachedGUIDs).thenReturn(
            jsonObj)

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)
        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)

        //Assert
        assertEquals("{\"identity_00002\":\"__1234567\"}",
            sharedPreferences.getString("cachedGUIDsKey:id",""))
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_when_cache_data_contains_random_key_and_Identity_removes_cached_shared_prefs_key(){
        val guid = "__1234567"
        val key = "abcxyz"

        val jsonObj = JSONObject()
        jsonObj.put("Identity_00002","__1234567")
        jsonObj.put("Email_donjoe2862@gmail.com","__1234567")

        Mockito.`when`(loginInfoProviderSpy.cachedGUIDs).thenReturn(
            jsonObj)

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)

        //Assert
        assertEquals("{\"Identity_00002\":\"__1234567\",\"Email_donjoe2862@gmail.com\":\"__1234567\"}",
            jsonObj.toString())
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_key_is_null_and_guid_has_value_should_do_nothing(){
        val guid = "__1234567"
        val key = null

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)

        //Assert
        Mockito.verify(loginInfoProviderSpy,Mockito.never()).cachedGUIDs
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_key_has_value_and_guid_is_null_should_do_nothing(){
        val guid = null
        val key = "Email"

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)

        //Assert
        Mockito.verify(loginInfoProviderSpy,Mockito.never()).cachedGUIDs
    }

    @Test
    fun test_removeValueFromCachedGUIDForIdentifier_key_is_null_and_guid_is_null_should_do_nothing(){
        val guid = null
        val key = null

        //Act
        loginInfoProviderSpy.removeValueFromCachedGUIDForIdentifier(guid, key)

        //Assert
        Mockito.verify(loginInfoProviderSpy,Mockito.never()).cachedGUIDs
    }
}