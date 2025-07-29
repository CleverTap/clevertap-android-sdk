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

        // Test with null account ID
        val resultWithNull = StorageHelper.storageKeyWithSuffix(null, key)
        assertEquals("$key:null", resultWithNull)
    }

    //=====================================================================================================================================
}