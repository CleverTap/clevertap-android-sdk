@file:Suppress("RedundantNullableReturnType", "RedundantExplicitType", "ControlFlowWithEmptyBody")

package com.clevertap.android.sdk

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants.CLEVERTAP_STORAGE_TAG
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@RunWith(RobolectricTestRunner::class)
class StorageHelperTest: BaseTestCase() {

    private val namespace = "abc"
    private val accountId = "test_account_id"

    @Test
    fun test_getPreferences_when_ContextIsPassed_should_ReturnPreferences() {

        // when context is passed and no string is passed, a preference is created with identifier "wizrocket"
        assertNotNull(StorageHelper.getPreferences(appCtx))

        // when context is passed and some string is passed, a preference is created with identifier "wizrocket_stringvalue"
        assertNotNull(StorageHelper.getPreferences(appCtx, namespace))
    }

    private fun prepareSP(action: ((pref1: SharedPreferences, prefWithNameSpace: SharedPreferences) -> Unit)? = null) {
        val pref1 = appCtx.getSharedPreferences(CLEVERTAP_STORAGE_TAG, Context.MODE_PRIVATE)
        val pref2 = appCtx.getSharedPreferences(CLEVERTAP_STORAGE_TAG + "_" + namespace, Context.MODE_PRIVATE)
        action?.invoke(pref1, pref2)
    }

    @Test
    fun test_getString_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrNull() {
        val presentKey = "present"
        val absentKey = "absent"

        //preparing SharedPref : adding a key-value pair "present:value" in shared pref named "wizrocket", which is the one created as default by StorageHelper.getPreferences(appCtx)
        prepareSP { pref1, _ -> pref1.edit().putString(presentKey, "value").commit() }

        //when correct key is passed, the correct value is returned from default SP
        val value1 = StorageHelper.getString(appCtx, presentKey, "default")
        assertEquals("value", value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned from default SP
        val value2 = StorageHelper.getString(appCtx, absentKey, null)
        assertNull(value2)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned from default SP
        val value3 = StorageHelper.getString(appCtx, absentKey, "default")
        assertEquals("default", value3)

        //preparing SharedPref : adding a key-value pair "present:value" in shared pref named "wizrocket_abc", which is the one created when namespace is passed by StorageHelper.getPreferences(appCtx,namespace)
        prepareSP { _, prefWithNameSpace -> prefWithNameSpace.edit().putString(presentKey, "value").commit() }

        //when correct key and namespace is passed, the correct value is returned from namespace SP
        val value4 = StorageHelper.getString(appCtx, namespace, presentKey, "default")
        assertEquals("value", value4)

        //when incorrect key and namespace is passed, the default value is returned from namespace SP
        val value5 = StorageHelper.getString(appCtx, namespace, absentKey, null)
        assertNull(value5)

        //when incorrect key and namespace is passed, the default value is returned from namespace SP
        val value6 = StorageHelper.getString(appCtx, namespace, absentKey, "default")
        assertEquals("default", value6)
    }

    @Test
    fun test_getStringFromPrefs_when_AppropriateParamsArePassed_should_ReturnValueAccordingly() {
        val rawKey = "rawKey"

        prepareSP { pref1, _ ->
            pref1.edit().putString(rawKey, "value").commit()
            pref1.edit().putString("$rawKey:$accountId", "value-id").commit()
        }

        // Should return the account-specific value when available
        val v1 = StorageHelper.getStringFromPrefs(appCtx, accountId, rawKey, null)
        assertEquals("value-id", v1)

        // Test with different account ID - should return account-specific value
        val accountId2 = "id2"
        prepareSP { pref1, _ -> pref1.edit().putString("$rawKey:$accountId2", "value-id2").commit() }
        val v2 = StorageHelper.getStringFromPrefs(appCtx, accountId2, rawKey, null)
        assertEquals("value-id2", v2)

        // Test with non-existent account ID - should return default value
        val accountId3 = "id3"
        val v3 = StorageHelper.getStringFromPrefs(appCtx, accountId3, rawKey, "default")
        assertEquals("default", v3)
    }

    @Test
    fun test_getBoolean_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrDefault() {
        val presentKey = "presentBoolKey"
        val absentKey = "absentBoolKey"

        //preparing SharedPref : adding a key-value pair "present:true" in shared pref named "wizrocket", which is the one created as default by StorageHelper.getPreferences(appCtx)
        prepareSP { pref1, _ -> pref1.edit().putBoolean(presentKey, true).commit() }

        //when correct key is passed, the correct value is returned from default SP
        val value1 = StorageHelper.getBoolean(appCtx, presentKey, false)
        assertEquals(true, value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned from default SP
        val value2 = StorageHelper.getBoolean(appCtx, absentKey, false)
        assertEquals(false, value2)
    }

    @Test
    fun test_getBooleanFromPrefs_when_AppropriateParamsArePassed_should_ReturnValueAccordingly() {
        val rawKey = "rawKeyBool"

        prepareSP { pref1, _ ->
            pref1.edit().putBoolean(rawKey, false).commit()
            pref1.edit().putBoolean("$rawKey:$accountId", true).commit()
        }

        // Should return the account-specific value when available
        val v1 = StorageHelper.getBooleanFromPrefs(appCtx, accountId, rawKey)
        assertEquals(true, v1)

        // Test with different account ID - should return account-specific value
        val accountId2 = "id2"
        prepareSP { pref1, _ -> pref1.edit().putBoolean("$rawKey:$accountId2", true).commit() }
        val v2 = StorageHelper.getBooleanFromPrefs(appCtx, accountId2, rawKey)
        assertEquals(true, v2)

        // Test with non-existent account ID - should return default value (false)
        val accountId3 = "id3"
        val v3 = StorageHelper.getBooleanFromPrefs(appCtx, accountId3, rawKey)
        assertEquals(false, v3)
    }

    @Test
    fun test_getInt_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrDefault() {
        val presentKey = "presentIntKey"
        val absentKey = "absentIntKey"

        //preparing SharedPref : adding a key-value pair "present:42" in shared pref named "wizrocket", which is the one created as default by StorageHelper.getPreferences(appCtx)
        prepareSP { pref1, _ -> pref1.edit().putInt(presentKey, 42).commit() }

        //when correct key is passed, the correct value is returned from default SP
        val value1 = StorageHelper.getInt(appCtx, presentKey, -1)
        assertEquals(42, value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned from default SP
        val value2 = StorageHelper.getInt(appCtx, absentKey, -1)
        assertEquals(-1, value2)
    }

    @Test
    fun test_getIntFromPrefs_when_AppropriateParamsArePassed_should_ReturnValueAccordingly() {
        val rawKey = "rawKeyInt"

        prepareSP { pref1, _ ->
            pref1.edit().putInt(rawKey, 13).commit()
            pref1.edit().putInt("$rawKey:$accountId", 14).commit()
        }

        // Should return the account-specific value when available
        val v1 = StorageHelper.getIntFromPrefs(appCtx, accountId, rawKey, -1)
        assertEquals(14, v1)

        // Test with different account ID - should return account-specific value
        val accountId2 = "id2"
        prepareSP { pref1, _ -> pref1.edit().putInt("$rawKey:$accountId2", 15).commit() }
        val v2 = StorageHelper.getIntFromPrefs(appCtx, accountId2, rawKey, -1)
        assertEquals(15, v2)

        // Test with non-existent account ID - should return default value
        val accountId3 = "id3"
        val v3 = StorageHelper.getIntFromPrefs(appCtx, accountId3, rawKey, -1)
        assertEquals(-1, v3)
    }

    @Test
    fun test_getLong_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrDefault() {
        val presentKey = "presentLongKey"
        val absentKey = "absentLongKey"

        //preparing SharedPref : adding a key-value pair "present:51L" in shared pref named "wizrocket", which is the one created as default by StorageHelper.getPreferences(appCtx)
        prepareSP { pref1, _ -> pref1.edit().putLong(presentKey, 51L).commit() }

        //when correct key is passed, the correct value is returned from default SP
        val value1 = StorageHelper.getLong(appCtx, presentKey, -1)
        assertEquals(51, value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned from default SP
        val value2 = StorageHelper.getLong(appCtx, absentKey, -1)
        assertEquals(-1, value2)
    }

    @Test
    fun test_getLongFromPrefs_when_AppropriateParamsArePassed_should_ReturnValueAccordingly() {
        val rawKey = "rawKeyLong"

        prepareSP { _, prefWithNameSpace ->
            prefWithNameSpace.edit().putLong("$rawKey:$accountId", 9876543211).commit()
        }

        // Should return the account-specific value when available
        val v1 = StorageHelper.getLongFromPrefs(appCtx, accountId, rawKey, -1, namespace)
        assertEquals(9876543211, v1)

        // Test with different account ID - should return account-specific value
        val accountId2 = "id2"
        prepareSP { _, prefWithNameSpace -> prefWithNameSpace.edit().putLong("$rawKey:$accountId2", 9876543212).commit() }
        val v2 = StorageHelper.getLongFromPrefs(appCtx, accountId2, rawKey, -1, namespace)
        assertEquals(9876543212, v2)

        // Test with non-existent account ID - should return base key value
        val accountId3 = "id3"
        val v3 = StorageHelper.getLongFromPrefs(appCtx, accountId3, rawKey, -1, namespace)
        assertEquals(-1, v3)
    }

    //=====================================================================================================================================

    @Test
    fun test_putString_when_AppropriateParamsArePassed_should_StoreValueCorrectly() {
        StorageHelper.putString(appCtx, "putKey", "value")
        val value = StorageHelper.getString(appCtx, "putKey", "fail")
        assertEquals("value", value)

        // Test account-specific put
        StorageHelper.putString(appCtx, accountId, "putKey2", "value2")
        val value2 = StorageHelper.getString(appCtx, "putKey2:$accountId", "fail")
        assertEquals("value2", value2)
    }

    @Test
    fun test_putBoolean_when_AppropriateParamsArePassed_should_StoreValueCorrectly() {
        StorageHelper.putBoolean(appCtx, "putKeyBool", true)
        val value = StorageHelper.getBoolean(appCtx, "putKeyBool", false)
        assertEquals(true, value)

        // Test account-specific put
        StorageHelper.putBoolean(appCtx, accountId, "putKeyBool2", true)
        val value2 = StorageHelper.getBoolean(appCtx, "putKeyBool2:$accountId", false)
        assertEquals(true, value2)
    }

    @Test
    fun test_putInt_when_AppropriateParamsArePassed_should_StoreValueCorrectly() {
        StorageHelper.putInt(appCtx, "putKeyInt", 21)
        val value = StorageHelper.getInt(appCtx, "putKeyInt", -1)
        assertEquals(21, value)

        // Test account-specific put
        StorageHelper.putInt(appCtx, accountId, "putKeyInt2", 22)
        val value2 = StorageHelper.getInt(appCtx, "putKeyInt2:$accountId", -1)
        assertEquals(22, value2)
    }

    @Test
    fun test_putLong_when_AppropriateParamsArePassed_should_StoreValueCorrectly() {
        StorageHelper.putLong(appCtx, "putKeyLong", 21)
        val value = StorageHelper.getLong(appCtx, "putKeyLong", -1)
        assertEquals(21, value)
    }

    //=====================================================================================================================================

    @Test
    fun test_putStringImmediate_when_AppropriateParamsArePassed_should_StoreValueCorrectly() {
        StorageHelper.putStringImmediate(appCtx, "putKey22", "value")
        val value = StorageHelper.getString(appCtx, "putKey22", "fail")
        assertEquals("value", value)

        // Test account-specific putStringImmediate
        StorageHelper.putStringImmediate(appCtx, accountId, "putKey23", "value23")
        val value2 = StorageHelper.getString(appCtx, "putKey23:$accountId", "fail")
        assertEquals("value23", value2)
    }

    @Test
    fun test_putBooleanImmediate_when_AppropriateParamsArePassed_should_StoreValueCorrectly() {
        StorageHelper.putBooleanImmediate(appCtx, "putKeyBoolImm", true)
        val value = StorageHelper.getBoolean(appCtx, "putKeyBoolImm", false)
        assertEquals(true, value)
    }

    @Test
    fun test_putIntImmediate_when_AppropriateParamsArePassed_should_StoreValueCorrectly() {
        StorageHelper.putIntImmediate(appCtx, "putKeyIntImm", 99)
        val value = StorageHelper.getInt(appCtx, "putKeyIntImm", -1)
        assertEquals(99, value)
    }

    @Test
    fun test_remove_when_AppropriateParamsArePassed_should_RemoveValueCorrectly() {
        prepareSP { pref1, _ -> pref1.edit().putString("keyRem", "value").commit() }
        StorageHelper.remove(appCtx, "keyRem")
        assertEquals(null, StorageHelper.getString(appCtx, "keyRem", null))

        // Test account-specific remove
        prepareSP { pref1, _ -> pref1.edit().putString("keyRem2:$accountId", "value").commit() }
        StorageHelper.remove(appCtx, accountId, "keyRem2")
        assertEquals(null, StorageHelper.getString(appCtx, "keyRem2:$accountId", null))
    }

    @Test
    fun test_removeImmediate_when_AppropriateParamsArePassed_should_RemoveValueCorrectly() {
        prepareSP { pref1, _ -> pref1.edit().putString("keyRem3", "value").commit() }
        StorageHelper.removeImmediate(appCtx, "keyRem3")
        assertEquals(null, StorageHelper.getString(appCtx, "keyRem3", null))
    }

    //=====================================================================================================================================

    @Test
    fun test_persist_when_EditorIsPassed_should_CallApply() {
        val mock: SharedPreferences.Editor = mockk(relaxed = true)
        StorageHelper.persist(mock)
        verify {
            mock.apply()
        }
    }

    @Test
    fun test_persistImmediately_when_EditorIsPassed_should_CallCommit() {
        val mock: SharedPreferences.Editor = mockk(relaxed = true)
        StorageHelper.persistImmediately(mock)
        verify {
            mock.commit()
        }
    }

    @Test
    fun test_storageKeyWithSuffix_when_AccountIdAndKeyArePassed_should_ReturnFormattedKey() {
        val key = "testKey"
        val result = StorageHelper.storageKeyWithSuffix(accountId, key)
        assertEquals("$key:$accountId", result)
    }

    //=====================================================================================================================================

    @Test
    fun test_getStringFromPrefs_fallback_logic_for_missing_account_specific_key() {
        val rawKey = "fallbackTestKey"
        val nonExistentAccountId = "nonexistent_account"
        
        // Put value only with base key (no account suffix)
        prepareSP { pref1, _ -> 
            pref1.edit().putString(rawKey, "fallback_value").commit() 
        }
        
        // Should return null since current implementation doesn't have fallback logic
        val result = StorageHelper.getStringFromPrefs(appCtx, nonExistentAccountId, rawKey, null)
        assertNull(result)
        
        // Should return default value when specified
        val resultWithDefault = StorageHelper.getStringFromPrefs(appCtx, nonExistentAccountId, rawKey, "default_value")
        assertEquals("default_value", resultWithDefault)
    }

    @Test
    fun test_putString_with_null_values() {
        // Test putting null value
        StorageHelper.putString(appCtx, "nullKey", null)
        val value = StorageHelper.getString(appCtx, "nullKey", "default")
        assertNull(value)
        
        // Test putting null value with account ID
        StorageHelper.putString(appCtx, accountId, "nullKey2", null)
        val value2 = StorageHelper.getString(appCtx, "nullKey2:$accountId", "default")
        assertNull(value2)
    }

    @Test
    fun test_putStringImmediate_with_null_values() {
        // Test putting null value immediately
        StorageHelper.putStringImmediate(appCtx, "nullKeyImm", null)
        val value = StorageHelper.getString(appCtx, "nullKeyImm", "default")
        assertNull(value)
        
        // Test putting null value immediately with account ID
        StorageHelper.putStringImmediate(appCtx, accountId, "nullKeyImm2", null)
        val value2 = StorageHelper.getString(appCtx, "nullKeyImm2:$accountId", "default")
        assertNull(value2)
    }

    @Test
    fun test_remove_with_accountId() {
        // Setup a key with account ID suffix
        prepareSP { pref1, _ -> pref1.edit().putString("removeKey:$accountId", "value").commit() }
        
        // Remove with account ID
        StorageHelper.remove(appCtx, accountId, "removeKey")
        
        // Verify it's removed
        val value = StorageHelper.getString(appCtx, "removeKey:$accountId", "default")
        assertEquals("default", value)
    }

    @Test
    fun test_putBoolean_with_accountId() {
        StorageHelper.putBoolean(appCtx, accountId, "boolKeyWithAccount", true)
        val value = StorageHelper.getBoolean(appCtx, "boolKeyWithAccount:$accountId", false)
        assertEquals(true, value)
    }

    @Test
    fun test_putInt_with_accountId() {
        StorageHelper.putInt(appCtx, accountId, "intKeyWithAccount", 42)
        val value = StorageHelper.getInt(appCtx, "intKeyWithAccount:$accountId", -1)
        assertEquals(42, value)
    }

    @Test
    fun test_getLong_with_namespace() {
        val key = "longKeyWithNamespace"
        val value = 789L
        
        // Setup value in namespace
        prepareSP { _, prefWithNameSpace -> 
            prefWithNameSpace.edit().putLong(key, value).commit() 
        }
        
        val result = StorageHelper.getLong(appCtx, namespace, key, -1L)
        assertEquals(value, result)
        
        // Test with non-existent key
        val result2 = StorageHelper.getLong(appCtx, namespace, "nonexistent", -1L)
        assertEquals(-1L, result2)
    }

    @Test
    fun test_putLong_functionality() {
        val key = "longPutKey"
        val value = 999L
        
        StorageHelper.putLong(appCtx, key, value)
        val retrieved = StorageHelper.getLong(appCtx, key, -1L)
        assertEquals(value, retrieved)
    }

    @Test
    fun test_persist_error_handling() {
        // Create a mock that throws an exception
        val mockEditor: SharedPreferences.Editor = mockk(relaxed = true)
        io.mockk.every { mockEditor.apply() } throws RuntimeException("Test exception")
        
        // Should not throw exception, should catch and log
        try {
            StorageHelper.persist(mockEditor)
            // Test passes if no exception is thrown
        } catch (e: Exception) {
            // Should not reach here
            throw AssertionError("persist() should handle exceptions gracefully")
        }
        
        verify { mockEditor.apply() }
    }

    @Test
    fun test_persistImmediately_error_handling() {
        // Create a mock that throws an exception
        val mockEditor: SharedPreferences.Editor = mockk(relaxed = true)
        io.mockk.every { mockEditor.commit() } throws RuntimeException("Test exception")
        
        // Should not throw exception, should catch and log
        try {
            StorageHelper.persistImmediately(mockEditor)
            // Test passes if no exception is thrown
        } catch (e: Exception) {
            // Should not reach here
            throw AssertionError("persistImmediately() should handle exceptions gracefully")
        }
        
        verify { mockEditor.commit() }
    }

    @Test
    fun test_multiple_namespaces() {
        val key = "multiNamespaceKey"
        val namespace1 = "ns1"
        val namespace2 = "ns2"
        
        // Put different values in different namespaces
        val pref1 = appCtx.getSharedPreferences(CLEVERTAP_STORAGE_TAG + "_" + namespace1, Context.MODE_PRIVATE)
        val pref2 = appCtx.getSharedPreferences(CLEVERTAP_STORAGE_TAG + "_" + namespace2, Context.MODE_PRIVATE)
        
        pref1.edit().putString(key, "value1").commit()
        pref2.edit().putString(key, "value2").commit()
        
        // Verify namespace isolation
        val result1 = StorageHelper.getString(appCtx, namespace1, key, "default")
        val result2 = StorageHelper.getString(appCtx, namespace2, key, "default")
        
        assertEquals("value1", result1)
        assertEquals("value2", result2)
    }

    @Test
    fun test_preferences_with_null_namespace() {
        val prefs = StorageHelper.getPreferences(appCtx, null)
        assertNotNull(prefs)
        
        // Should be same as getPreferences(appCtx)
        val defaultPrefs = StorageHelper.getPreferences(appCtx)
        assertEquals(prefs, defaultPrefs)
    }

    @Test
    fun test_getString_with_null_namespace() {
        val key = "nullNamespaceKey"
        val value = "nullNamespaceValue"
        
        // Put value in default preferences
        prepareSP { pref1, _ -> pref1.edit().putString(key, value).commit() }
        
        // Get with null namespace should work same as no namespace
        val result = StorageHelper.getString(appCtx, null, key, "default")
        assertEquals(value, result)
    }

    @Test
    fun test_getLong_with_null_namespace() {
        val key = "nullNamespaceLongKey"
        val value = 12345L
        
        // Put value in default preferences
        prepareSP { pref1, _ -> pref1.edit().putLong(key, value).commit() }
        
        // Get with null namespace should work same as no namespace
        val result = StorageHelper.getLong(appCtx, null, key, -1L)
        assertEquals(value, result)
    }

    @Test
    fun test_edge_cases_with_empty_strings() {
        // Test with empty key
        StorageHelper.putString(appCtx, "", "value")
        val result1 = StorageHelper.getString(appCtx, "", "default")
        assertEquals("value", result1)
        
        // Test with empty account ID
        StorageHelper.putString(appCtx, "", "emptyAccountKey", "value")
        val result2 = StorageHelper.getString(appCtx, "emptyAccountKey:", "default")
        assertEquals("value", result2)
    }

}