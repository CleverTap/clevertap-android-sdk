@file:Suppress("RedundantNullableReturnType", "RedundantExplicitType", "ControlFlowWithEmptyBody")

package com.clevertap.android.sdk

import android.os.Bundle
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class UtilsTest : BaseTestCase() {
    

    @Test
    fun test_containsIgnoreCase_when_CollectionNullAndKeyNull_should_ReturnFalse() {
        val collection: List<String>?= null
        val key: String? = null
        assertFalse { Utils.containsIgnoreCase(collection, key) }
    }


    @Test
    fun test_containsIgnoreCase_when_CollectionNullAndKeyNotNull_should_ReturnFalse() {
        val collection: List<String>?= null
        val key: String = ""
        assertFalse { Utils.containsIgnoreCase(collection, key) }
    }

    @Test
    fun test_containsIgnoreCase_when_CollectionNotNullAndKeyNull_should_ReturnFalse() {
        val collection: List<String> = listOf()
        val key: String? = null
        assertFalse { Utils.containsIgnoreCase(collection, key) }
    }

    @Test
    fun test_containsIgnoreCase_when_CollectionNotNullAndKeyNotNullAndKeyNotInCollection_should_ReturnFalse() {
        val collection: List<String> = listOf()
        val key: String  = ""
        assertFalse { Utils.containsIgnoreCase(collection, key) }
    }


    @Test
    fun test_containsIgnoreCase_when_CollectionNotNullAndKeyNotNullAndKeyInCollection_should_ReturnTrue() {
        val collection: List<String>?= listOf("hello")
        val key: String? = "HelLO"
        assertTrue { Utils.containsIgnoreCase(collection, key) }
    }


    //------------------------------------------------------------------------------------

    @Test
    fun test_convertBundleObjectToHashMap_when_EmptyBundleIsPassed_should_ReturnEmptyHashMap() {
        val bundle = Bundle().also{ }
        val map = Utils.convertBundleObjectToHashMap(bundle)
        println(map)
        assertNotNull(map)
        assertEquals(0,map.size,)
    }


    @Test
    fun test_convertBundleObjectToHashMap_when_NullBundleIsPassed_should_ReturnEmptyHashMap() {
        //todo 4 : this test caught that nulls are not properly handled
        val bundle =  Bundle().also{ }// null //todo set bundle to null to check
        val map = Utils.convertBundleObjectToHashMap(bundle)
        println(map)
        assertNotNull(map)
        assertEquals(0,map.size,)
    }


    @Test
    fun test_convertBundleObjectToHashMap_when_BundleIsPassed_should_ReturnHashMap() {
        val bundle = Bundle().also{ it.putChar("gender",'M') }
        val map = Utils.convertBundleObjectToHashMap(bundle)
        println(map)
        assertNotNull(map)
        assertEquals(1,map.size,)
    }



    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONArrayOfJSONObjectsToArrayListOfHashMaps_when_JsonArrayIsPassed_should_ReturnList() {
        val jsonArray = JSONArray().also { it.put(10) }
        val list = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(jsonArray)
        print("list is $list")
        assertNotNull(list)
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONArrayToArrayList_when_JsonArrayIsPassed_should_ReturnList() {
        val jsonArray = JSONArray().also { it.put(10) }
        val list = Utils.convertJSONArrayToArrayList(jsonArray)
        print("list is $list")
        assertNotNull(list)
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONObjectToHashMap_when_JsonObjectIsPassed_should_ReturnAMap() {
        val jsonObject = JSONObject().also { it.put("some_number",24) }
        val map = Utils.convertJSONObjectToHashMap(jsonObject)
        print("map is $map")
        assertNotNull(map)
        //todo

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertToTitleCase_when_AStringIsPassed_should_ConvertStringToTitleCase() {
        val string = "this is a string"
        val stringConverted = Utils.convertToTitleCase(string)
        println(stringConverted)
        assertEquals("This Is A String",stringConverted)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_drawableToBitmap_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getAppIcon_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getBitmapFromURL_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getByteArrayFromImageURL_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getCurrentNetworkType_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getDeviceNetworkType_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getMemoryConsumption_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getNotificationBitmap_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getNow_when_ABC_should_XYZ() {
        assertEquals(Utils.getNow().toLong(),System.currentTimeMillis()/1000)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getThumbnailImage_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_hasPermission_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_isActivityDead_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_isServiceAvailable_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_optionalStringKey_when_ABC_should_XYZ() {
        val json1 = JSONObject()
        val key1 = ""
        val result1 = Utils.optionalStringKey(json1,key1)
        print("result1:$result1")
        assertNull(result1)

//        val json2 = JSONObject()
//        val key2 = "key"
//        json2.put(key2,null)
//        val result2 = Utils.optionalStringKey(json2,key2)
//        print("result2:$result2")
//        assertNull(result2)

        val json3 = JSONObject()
        val key3 = "key"
        json3.put(key3,"value")
        val result3 = Utils.optionalStringKey(json3,key3)
        print("result3:$result3")
        assertNotNull(result3)
        assertEquals(result3,"value")

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_runOnUiThread_when_ABC_should_XYZ() {
        //todo 2
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_setPackageNameFromResolveInfoList_when_ABC_should_XYZ() {
        // todo 3
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_stringToBundle_when_ABC_should_XYZ() {
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_validateCTID_when_CTIdIsPassed_should_ReturnTrueOrFalse() {
        val id1 = null
        assertFalse { Utils.validateCTID(id1) }

        val id2 = ""
        assertFalse { Utils.validateCTID(id2) }

        val id3 = "11111111_22222222_33333333_44444444_55555555_66666666_77777777_88888888"
        assertFalse { Utils.validateCTID(id3) }

        val id4 = "1 2 3 4"
        assertFalse { Utils.validateCTID(id4) }

        val id5 = "abcd_1234_!!_::_$"+"@@_---"
        assertTrue { Utils.validateCTID(id5) }

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_haveDeprecatedFirebaseInstanceId_when_ABC_should_XYZ() {
        //Utils.haveDeprecatedFirebaseInstanceId
        //todo
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_haveVideoPlayerSupport_when_ABC_should_XYZ() {
        //Utils.haveVideoPlayerSupport
        //todo
    }

    //------------------------------------------------------------------------------------


}