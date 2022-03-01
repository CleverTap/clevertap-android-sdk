@file:Suppress("RedundantNullableReturnType", "RedundantExplicitType", "ControlFlowWithEmptyBody")

package com.clevertap.android.sdk

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants.CLEVERTAP_STORAGE_TAG
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@RunWith(RobolectricTestRunner::class)
class StorageHelperTest: BaseTestCase() {

    private val namespace = "abc"

    @Test
    fun test_getPreferences_when_ContextIsPassed_should_ReturnPreferences() {
        val context = application.applicationContext

        // when context is passed and no string is passed, a preference is created with identifier  "wizrocket"
        assertNotNull(StorageHelper.getPreferences(context))

        // when context is passed and some string "key" is passed, a preference is created with identifier  "wizrocket-key"
        assertNotNull(StorageHelper.getPreferences(context,"key"))

    }


    private fun prepareTest(action : ((pref1:SharedPreferences,prefWithNameSpace:SharedPreferences) -> Unit)? = null){
        val context = application.applicationContext
        val pref1 = context.getSharedPreferences(CLEVERTAP_STORAGE_TAG, Context.MODE_PRIVATE);
        val pref2 = context.getSharedPreferences(CLEVERTAP_STORAGE_TAG+"_"+namespace,Context.MODE_PRIVATE)
        action?.invoke(pref1,pref2)
    }


    @Test
    fun test_getString_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrNull() {
        val presentKey = "present"
        val absentKey= "absent"
        val ctx = application.applicationContext

        prepareTest { pref1, _  -> pref1.edit().putString(presentKey,"value").commit() }

        val value1 = StorageHelper.getString(ctx,presentKey,"default")
        assertEquals("value",value1)

        val value2 = StorageHelper.getString(ctx,absentKey,null)
        assertNull(value2)

        val value3 = StorageHelper.getString(ctx,absentKey,"default")
        assertEquals("default",value3)

        prepareTest { _, prefWithNameSpace  -> prefWithNameSpace.edit().putString(presentKey,"value").commit() }

        val value4 = StorageHelper.getString(ctx,namespace,presentKey,"default")
        assertEquals("value",value4)

        val value5 = StorageHelper.getString(ctx,namespace,absentKey,null)
        assertNull(value5)

        val value6 = StorageHelper.getString(ctx,namespace,absentKey,"default")
        assertEquals("default",value6)


    }

    @Test
    fun test_getStringFromPrefs_when_AppropriateParamsAndConfigIsPassed_should_ReturnValueAccordingly() {
        val rawKey = "rawKey"
        val ctx = application.applicationContext

        // since getString(...) and storageKeyWithSuffix(...) are being tested in other functions,
        // we will be testing only the impact of CleverTapInstanceConfig param in this function

        prepareTest { pref1, _  ->
            pref1.edit().putString(rawKey,"value").commit()
            pref1.edit().putString("$rawKey:id","value-id").commit()
        }

        // if config is passed without being set as default,
        // it will request for "key:id" where key is raw key and id is accountid
        var config:CleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(ctx,"id","token")
        val v1 = StorageHelper.getStringFromPrefs(ctx,config, rawKey,null)
        assertEquals("value-id",v1)

        // if config is passed  AFTER being set as default,
        // it will  STILL request for value of  "key:id" where key is raw key and id is accountid
        prepareTest { pref1, _ ->  pref1.edit().putString("$rawKey:id2","value-id2").commit() }
        config = CleverTapInstanceConfig.createDefaultInstance(ctx,"id2","token","eu1")
        val v2 = StorageHelper.getStringFromPrefs(ctx,config, rawKey,null)
        assertEquals("value-id2",v2)


        // but if value for "key:id" is null, it will search for just the value of  "key"
        config = CleverTapInstanceConfig.createDefaultInstance(ctx,"id3","token","eu1")
        val v3 = StorageHelper.getStringFromPrefs(ctx,config, rawKey,null)
        assertEquals("value",v3)

    }

    @Test
    fun test_getBoolean_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrNull() {
    }

    @Test
    fun test_getBooleanFromPrefs_when_AppropriateParamsAndConfigIsPassed_should_ReturnValueAccordingly() {
    }

    @Test
    fun test_getInt_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getIntFromPrefs_when_ABC_should_XYZ() {
    }


    @Test
    fun test_getLong_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getLongFromPrefs_when_ABC_should_XYZ() {
    }

    //=====================================================================================================================================

    @Test
    fun test_storageKeyWithSuffix_when_ConfigAndKeyIsPassed_should_ReturnAConcatenatedStringOfKeyAndAccountID() {
        val result = StorageHelper.storageKeyWithSuffix(
            CleverTapInstanceConfig.createInstance(application,"id","token"),
            "key"
        )
        assertEquals("key:id",result)
    }

    //=====================================================================================================================================


    @Test
    fun test_putString_when_ABC_should_XYZ() {
    }

    @Test
    fun test_putStringImmediate_when_ABC_should_XYZ() {
    }

    @Test
    fun test_putBoolean_when_ABC_should_XYZ() {
    }

    @Test
    fun test_putInt_when_ABC_should_XYZ() {
    }

    @Test
    fun test_putLong_when_ABC_should_XYZ() {
    }

    //=====================================================================================================================================


    @Test
    fun test_remove_when_ABC_should_XYZ() {
    }

    @Test
    fun test_removeImmediate_when_ABC_should_XYZ() {
    }

    //=====================================================================================================================================



    @Test
    fun test_persist_when_ABC_should_XYZ() {
    }

    @Test
    fun test_persistImmediately_when_ABC_should_XYZ() {
    }

    //=====================================================================================================================================






}