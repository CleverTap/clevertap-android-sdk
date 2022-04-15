@file:Suppress("RedundantNullableReturnType", "RedundantExplicitType", "ControlFlowWithEmptyBody")

package com.clevertap.android.sdk

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants.CLEVERTAP_STORAGE_TAG
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSharedPreferences
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@RunWith(RobolectricTestRunner::class)
class StorageHelperTest: BaseTestCase() {

    private val namespace = "abc"

    @Test
    fun test_getPreferences_when_ContextIsPassed_should_ReturnPreferences() {

        // when context is passed and no string is passed, a preference is created with identifier  "wizrocket"
        assertNotNull(StorageHelper.getPreferences(appCtx))

        // when context is passed and some string  is passed, a preference is created with identifier  "wizrocket_stringvalue"
        assertNotNull(StorageHelper.getPreferences(appCtx,namespace))

    }


    private fun prepareSP(action : ((pref1:SharedPreferences, prefWithNameSpace:SharedPreferences) -> Unit)? = null){
        val pref1 = appCtx.getSharedPreferences(CLEVERTAP_STORAGE_TAG, Context.MODE_PRIVATE);
        val pref2 = appCtx.getSharedPreferences(CLEVERTAP_STORAGE_TAG+"_"+namespace,Context.MODE_PRIVATE)
        action?.invoke(pref1,pref2)
    }


    @Test
    fun test_getString_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrNull() {
        val presentKey = "present"
        val absentKey= "absent"

        // todo : only string works on prefWithNameSpace , others dont , for  getString() function . @piyush

        //preparing SharedPref : adding a key-value pair  "present:value" in shared pref named "wizrocket", which is th one created as default by StorageHelper.getPreferences(appappCtx)
        prepareSP { pref1, _  -> pref1.edit().putString(presentKey,"value").commit() }

        //when correct key is passed , the correct value  is returned from default SP
        val value1 = StorageHelper.getString(appCtx,presentKey,"default")
        assertEquals("value",value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned  from default SP
        val value2 = StorageHelper.getString(appCtx,absentKey,null)
        assertNull(value2)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned  from default SP
        val value3 = StorageHelper.getString(appCtx,absentKey,"default")
        assertEquals("default",value3)

        //preparing SharedPref : adding a key-value pair  "present:value" in shared pref named "wizrocket_abc", which is th one created when namespace is passed by StorageHelper.getPreferences(appappCtx,namespace)
        prepareSP { _, prefWithNameSpace  -> prefWithNameSpace.edit().putString(presentKey,"value").commit() }

        //when correct key and namespace is passed , the correct value  is returned from namespace SP
        val value4 = StorageHelper.getString(appCtx,namespace,presentKey,"default")
        assertEquals("value",value4)

        //when incorrect key and namespace is passed is passed, the default value is returned  from namespace SP
        val value5 = StorageHelper.getString(appCtx,namespace,absentKey,null)
        assertNull(value5)

        //when incorrect key and namespace is passed is passed, the default value is returned  from namespace SP
        val value6 = StorageHelper.getString(appCtx,namespace,absentKey,"default")
        assertEquals("default",value6)


    }

    @Test
    fun test_getStringFromPrefs_when_AppropriateParamsAndConfigIsPassed_should_ReturnValueAccordingly() {
        val rawKey = "rawKey"

        // since getString(...) and storageKeyWithSuffix(...) are being tested in other functions,
        // we will be testing only the impact of CleverTapInstanceConfig param in this function

        prepareSP { pref1, _  ->
            pref1.edit().putString(rawKey,"value").commit()
            pref1.edit().putString("$rawKey:id","value-id").commit()
        }

        // if config is passed without being set as default,
        // it will request for "key:id" where key is raw key and id is accountid
        var config:CleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(appCtx,"id","token")
        val v1 = StorageHelper.getStringFromPrefs(appCtx,config, rawKey,null)
        assertEquals("value-id",v1)

        // if config is passed  AFTER being set as default,
        // it will  STILL request for value of  "key:id" where key is raw key and id is accountid
        prepareSP { pref1, _ ->  pref1.edit().putString("$rawKey:id2","value-id2").commit() }
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id2","token","eu1")
        val v2 = StorageHelper.getStringFromPrefs(appCtx,config, rawKey,null)
        assertEquals("value-id2",v2)


        // but if value for "key:id" is null, it will search for just the value of  "key"
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id3","token","eu1")
        val v3 = StorageHelper.getStringFromPrefs(appCtx,config, rawKey,null)
        assertEquals("value",v3)

    }

    @Test
    fun test_getBoolean_when_AppropriateParamsArePassed_should_ReturnValueIfAvailableOrNull() {
        val presentKey = "presentBoolKey"
        val absentKey= "absentBoolKey"

        //preparing SharedPref : adding a key-value pair  "present:true" in shared pref named "wizrocket", which is th one created as default by StorageHelper.getPreferences(appappCtx)
        prepareSP { pref1, _  -> pref1.edit().putBoolean(presentKey,true).commit() }

        //when correct key is passed , the correct value  is returned from default SP
        val value1 = StorageHelper.getBoolean(appCtx,presentKey,false)
        assertEquals(true,value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned  from default SP
        val value2 = StorageHelper.getBoolean(appCtx,absentKey,false)
        assertEquals(false,value2)

    }

    @Test
    fun test_getBooleanFromPrefs_when_AppropriateParamsAndConfigIsPassed_should_ReturnValueAccordingly() {
        val rawKey = "rawKeyBool"

        // since getBoolean(...) and storageKeyWithSuffix(...) are being tested in other functions,
        // we will be testing only the impact of CleverTapInstanceConfig param in this function

        prepareSP { pref1, _  ->
            pref1.edit().putBoolean(rawKey,false).commit()
            pref1.edit().putBoolean("$rawKey:id",true).commit()
        }

        // if config is passed without being set as default,
        // it will request for "key:id" where key is raw key and id is accountid
        var config:CleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(appCtx,"id","token")
        val v1 = StorageHelper.getBooleanFromPrefs(appCtx,config, rawKey)
        assertEquals(true,v1)

        // if config is passed  AFTER being set as default,
        // it will  STILL request for value of  "key:id" where key is raw key and id is accountid
        prepareSP { pref1, _ ->  pref1.edit().putBoolean("$rawKey:id2",true).commit() }
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id2","token","eu1")
        val v2 = StorageHelper.getBooleanFromPrefs(appCtx,config, rawKey,)
        assertEquals(true,v2)


        // but if value for "key:id" is null, it will search for just the value of  "key"
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id3","token","eu1")
        val v3 = StorageHelper.getBooleanFromPrefs(appCtx,config, rawKey)
        assertEquals(false,v3)

    }

    @Test
    fun test_getInt_when_ABC_should_XYZ() {
        val presentKey = "presentIntKey"
        val absentKey= "absentIntKey"

        //preparing SharedPref : adding a key-value pair  "present:42" in shared pref named "wizrocket", which is th one created as default by StorageHelper.getPreferences(appappCtx)
        prepareSP { pref1, _  -> pref1.edit().putInt(presentKey,42).commit() }

        //when correct key is passed , the correct value  is returned from default SP
        val value1 = StorageHelper.getInt(appCtx,presentKey,-1)
        assertEquals(42,value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned  from default SP
        val value2 = StorageHelper.getInt(appCtx,absentKey,-1)
        assertEquals(-1,value2)
    }

    @Test
    fun test_getIntFromPrefs_when_ABC_should_XYZ() {
        val rawKey = "rawKeyBool"

        // since getInt(...) and storageKeyWithSuffix(...) are being tested in other functions,
        // we will be testing only the impact of CleverTapInstanceConfig param in this function

        prepareSP { pref1, _  ->
            pref1.edit().putInt(rawKey,13).commit()
            pref1.edit().putInt("$rawKey:id",14).commit()
        }

        // if config is passed without being set as default,
        // it will request for "key:id" where key is raw key and id is accountid
        var config:CleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(appCtx,"id","token")
        val v1 = StorageHelper.getIntFromPrefs(appCtx,config, rawKey,-1)
        assertEquals(14,v1)

        // if config is passed  AFTER being set as default,
        // it will  STILL request for value of  "key:id" where key is raw key and id is accountid
        prepareSP { pref1, _ ->  pref1.edit().putInt("$rawKey:id2",15).commit() }
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id2","token","eu1")
        val v2 = StorageHelper.getIntFromPrefs(appCtx,config, rawKey,-1)
        assertEquals(15,v2)


        // but if value for "key:id" is null, it will search for just the value of  "key"
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id3","token","eu1")
        val v3 = StorageHelper.getIntFromPrefs(appCtx,config, rawKey, -1)
        assertEquals(13,v3)

    }


    @Test
    fun test_getLong_when_ABC_should_XYZ() {
        val presentKey = "presentLongKey"
        val absentKey= "absentLongKey"

        //preparing SharedPref : adding a key-value pair  "present:42" in shared pref named "wizrocket", which is th one created as default by StorageHelper.getPreferences(appappCtx)
        prepareSP { pref1, _  -> pref1.edit().putLong(presentKey,51L).commit() }

        //when correct key is passed , the correct value  is returned from default SP
        val value1 = StorageHelper.getLong(appCtx,presentKey,-1)
        assertEquals(51,value1)

        //when incorrect key (or the key that is not present already) is passed, the default value is returned  from default SP
        val value2 = StorageHelper.getLong(appCtx,absentKey,-1)
        assertEquals(-1,value2)

    }

    @Test
    fun test_getLongFromPrefs_when_ABC_should_XYZ() {
        val rawKey = "rawKeyBool"
        // todo : only long works on pref With NameSpace , others dont , for  get_x_FromPrefs? @piyush

        // since getInt(...) and storageKeyWithSuffix(...) are being tested in other functions,
        // we will be testing only the impact of CleverTapInstanceConfig param in this function

        prepareSP { _, prefWithNameSpace  ->
            prefWithNameSpace.edit().putLong(rawKey,999_999_999).commit()
            prefWithNameSpace.edit().putLong("$rawKey:id",9876543211).commit()
        }

        // if config is passed without being set as default,
        // it will request for "key:id" where key is raw key and id is accountid
        var config:CleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(appCtx,"id","token")
        val v1 = StorageHelper.getLongFromPrefs(appCtx,config, rawKey,-1,namespace)
        assertEquals(9876543211,v1)

        // if config is passed  AFTER being set as default,
        // it will  STILL request for value of  "key:id" where key is raw key and id is accountid
        prepareSP { _, prefWithNameSpace ->  prefWithNameSpace.edit().putLong("$rawKey:id2",9876543212).commit() }
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id2","token","eu1")
        val v2 = StorageHelper.getLongFromPrefs(appCtx,config, rawKey,-1,namespace)
        assertEquals(9876543212,v2)

        // but if value for "key:id" is null, it will search for just the value of  "key"
        config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id3","token","eu1")
        val v3 = StorageHelper.getLongFromPrefs(appCtx,config, rawKey, -1,namespace)
        assertEquals(999_999_999,v3)

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
        StorageHelper.putString(appCtx,"putKey","value")
        val value = StorageHelper.getString(appCtx,"putKey","fail")
        assertEquals("value",value)


        val config = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id2","token","eu1")
        StorageHelper.putString(appCtx,config,"putKey2","value2")

        val value2 = StorageHelper.getString(appCtx,"putKey2:id2","fail")
        assertEquals("value2",value2)


    }

    @Test
    fun test_putBoolean_when_ABC_should_XYZ() {
        StorageHelper.putBoolean(appCtx,"putKeyBool",true)
        val value = StorageHelper.getBoolean(appCtx,"putKeyBool",false)
        assertEquals(true,value)

        // todo  no  immediate / putX with config for everyone except string ?

    }

    @Test
    fun test_putInt_when_ABC_should_XYZ() {

        StorageHelper.putInt(appCtx,"putKeyInt",21)
        val value = StorageHelper.getInt(appCtx,"putKeyInt",-1)
        assertEquals(21,value)

    }

    @Test
    fun test_putLong_when_ABC_should_XYZ() {
        StorageHelper.putLong(appCtx,"putKeyLong",21)
        val value = StorageHelper.getLong(appCtx,"putKeyLong",-1)
        assertEquals(21,value)

    }

    //=====================================================================================================================================

    @Test
    fun test_putStringImmediate_when_ABC_should_XYZ() {
        StorageHelper.putStringImmediate(appCtx,"putKey22","value")
        val value = StorageHelper.getString(appCtx,"putKey22","fail")
        assertEquals("value",value)
        //todo correct?@piyush

    }

    @Test
    fun test_remove_when_ABC_should_XYZ() {
        prepareSP { pref1, prefWithNameSpace -> pref1.edit().putString("keyRem","value").commit() }
        StorageHelper.remove(appCtx,"keyRem")
        assertEquals(null,StorageHelper.getString(appCtx,"keyRem",null))
        //todo correct?@piyush
    }

    @Test
    fun test_removeImmediate_when_ABC_should_XYZ() {
        prepareSP { pref1, prefWithNameSpace -> pref1.edit().putString("keyRem2","value").commit() }
        StorageHelper.removeImmediate(appCtx,"keyRem2")
        assertEquals(null,StorageHelper.getString(appCtx,"keyRem2",null))
        //todo correct? @piyush
    }

    //=====================================================================================================================================



    @Test
    fun test_persist_when_ABC_should_XYZ() {
        val mock: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)
        StorageHelper.persist(mock)
        Mockito.verify(mock).apply()
    }

    @Test
    fun test_persistImmediately_when_ABC_should_XYZ() {
        val mock: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)
        StorageHelper.persistImmediately(mock)
        Mockito.verify(mock).commit()
    }

    //=====================================================================================================================================






}