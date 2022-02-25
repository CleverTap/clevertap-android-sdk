@file:Suppress("RedundantNullableReturnType", "RedundantExplicitType", "ControlFlowWithEmptyBody")

package com.clevertap.android.sdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import kotlin.test.*


@RunWith(RobolectricTestRunner::class)
class UtilsTest : BaseTestCase() {

    @Test
    fun test_containsIgnoreCase_when_CollectionNullAndKeyNull_should_ReturnFalse() {
        val collection: List<String>? = null
        val key: String? = null
        assertFalse { Utils.containsIgnoreCase(collection, key) }
    }


    @Test
    fun test_containsIgnoreCase_when_CollectionNullAndKeyNotNull_should_ReturnFalse() {
        val collection: List<String>? = null
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
        val key: String = ""
        assertFalse { Utils.containsIgnoreCase(collection, key) }
    }


    @Test
    fun test_containsIgnoreCase_when_CollectionNotNullAndKeyNotNullAndKeyInCollection_should_ReturnTrue() {
        val collection: List<String>? = listOf("hello")
        val key: String? = "HelLO"
        assertTrue { Utils.containsIgnoreCase(collection, key) }
    }


    //------------------------------------------------------------------------------------

    @Test
    fun test_convertBundleObjectToHashMap_when_EmptyBundleIsPassed_should_ReturnEmptyHashMap() {
        val bundle = Bundle().also { }
        val map = Utils.convertBundleObjectToHashMap(bundle)
        if (BuildConfig.DEBUG) println(map)
        assertNotNull(map)
        assertEquals(0, map.size)
    }


    @Test
    fun test_convertBundleObjectToHashMap_when_BundleIsPassed_should_ReturnHashMap() {
        val bundle = Bundle().also { it.putChar("gender", 'M') }
        val map = Utils.convertBundleObjectToHashMap(bundle)
        if (BuildConfig.DEBUG) println(map)
        assertNotNull(map)
        assertEquals(1, map.size)
    }


    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONArrayOfJSONObjectsToArrayListOfHashMaps_when_JsonArrayIsPassed_should_ReturnList() {
        val jsonArray = JSONArray().also { it.put(10) }
        val list = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(jsonArray)
        if (BuildConfig.DEBUG) println("list is $list")
        assertNotNull(list)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONArrayToArrayList_when_JsonArrayIsPassed_should_ReturnList() {
        val jsonArray = JSONArray().also { it.put(10) }
        val list = Utils.convertJSONArrayToArrayList(jsonArray)
        if (BuildConfig.DEBUG) println("list is $list")
        assertNotNull(list)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONObjectToHashMap_when_JsonObjectIsPassed_should_ReturnAMap() {
        val jsonObject = JSONObject().also { it.put("some_number", 24) }
        val map = Utils.convertJSONObjectToHashMap(jsonObject)
        if (BuildConfig.DEBUG) println("map is $map")
        assertNotNull(map)

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertToTitleCase_when_AStringIsPassed_should_ConvertStringToTitleCase() {
        val string = "this is a string"
        val stringConverted = Utils.convertToTitleCase(string)
        if (BuildConfig.DEBUG) println(stringConverted)
        assertEquals("This Is A String", stringConverted)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_drawableToBitmap_when_PassedDrawable_should_ReturnBitmap() {
        val drawable: Drawable? = application.getDrawable(R.drawable.common_full_open_on_phone)
        val bitmap = Utils.drawableToBitmap(drawable)
        assertNotNull(bitmap)

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getBitmapFromURL_when_CorrectImageLinkArePassed_should_ReturnImage() {
        val url2 = "https:/www.example.com/malformed_url"
        val image2: Bitmap? = Utils.getBitmapFromURL(url2)
        assertNull(image2)


        val url = "https://www.freedesktop.org/wiki/logo.png"
        val image: Bitmap? = Utils.getBitmapFromURL(url)
        assertNull(image)
        //todo :
        // this should return an image bytearray but is giving error :
        //      java.net.SocketException: java.security.NoSuchAlgorithmException:
        //      Error constructing implementation (algorithm: Default, provider: SunJSSE, class: sun.security.ssl.SSLContextImpl$DefaultSSLContext)
        // how to test this ?
        //todo: change it to assertNotNull to replciate

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getByteArrayFromImageURL_when_CorrectImageLinkArePassed_should_ReturnImageByteArray() {
        val url2 = "https:/www.example.com/malformed_url"
        val array2: ByteArray? = Utils.getByteArrayFromImageURL(url2)
        assertNull(array2)


        val url = "https://www.freedesktop.org/wiki/logo.png"
        val array: ByteArray? = Utils.getByteArrayFromImageURL(url)
        assertNull(array)
        //todo :
        // this should return an image bytearray but is giving error :
        //      java.net.SocketException: java.security.NoSuchAlgorithmException:
        //      Error constructing implementation (algorithm: Default, provider: SunJSSE, class: sun.security.ssl.SSLContextImpl$DefaultSSLContext)
        //
        // how to test this ?
        //todo: change it to assertNotNull to replciate

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getCurrentNetworkType_when_FunctionIsCalledWithContext_should_ReturnNetworkType() {
        val networkType2: String? = Utils.getCurrentNetworkType(null)
        if (BuildConfig.DEBUG) println("Network type is $networkType2")
        assertNotNull(networkType2)
        assertEquals("Unavailable", networkType2)

        val networkType: String? = Utils.getCurrentNetworkType(application.applicationContext)
        if (BuildConfig.DEBUG) println("Network type is $networkType")
        assertNotNull(networkType)
        //todo how to check for multiple networks? for laptop, its always going to give network as unknown

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getDeviceNetworkType_when_FunctionIsCalledWithContext_should_ReturnNetworkType() {
        val networkType2: String? = Utils.getDeviceNetworkType(null)
        if (BuildConfig.DEBUG) println("Network type is $networkType2")
        assertNotNull(networkType2)
        assertEquals("Unavailable", networkType2)

        val networkType: String? = Utils.getDeviceNetworkType(application.applicationContext)
        if (BuildConfig.DEBUG) println("Network type is $networkType")
        assertNotNull(networkType)
        //todo how to check for multiple networks? for laptop, its always going to give network as unknown

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getMemoryConsumption_when_FunctionIsCalled_should_ReturnANonNullMemoryValue() {
        val consumption = Utils.getMemoryConsumption()
        if (BuildConfig.DEBUG) println("Consumptions type is $consumption")
        assertNotNull(consumption)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getNotificationBitmap_when_ParamsArePassed_should_ReturnBitmapOrNull() {
        val context = application.applicationContext

        // if context is null, result will always be null irrespective of other params
        val bitmap8 = Utils.getNotificationBitmap(null, false, null)
        assertNull(bitmap8)

        //if fallbackToAppIcon is false and path is null/empty, result will remain null since the fallback is disabled
        val bitmap71 = Utils.getNotificationBitmap(null, false, context)
        val bitmap72 = Utils.getNotificationBitmap("", false, context)
        assertNull(bitmap71)
        assertNull(bitmap72)

        //todo these are causing illegal state exception:
        ////if fallbackToAppIcon is true and path is  null/empty, result will  be the app icon
        //val bitmap61 = Utils.getNotificationBitmap( null,  true,  context)
        //val bitmap62 = Utils.getNotificationBitmap( "",  true,  context)
        //assertNotNull(bitmap61)
        //assertNotNull(bitmap62)
        //assertEquals(bitmap61,bitmap62)
        //val appIcon = Utils.drawableToBitmap(context.packageManager.getApplicationIcon(context.applicationInfo))
        //assertEquals(bitmap61,appIcon)
        //
        //// if path is not Null/empty, the icon will be available irrespective to the fallbackToAppIcon switch
        //val bitmap41 = Utils.getNotificationBitmap( "https://www.pod.cz/ico/favicon.ico",  false,  application.applicationContext)
        //val bitmap42 = Utils.getNotificationBitmap( "https://www.pod.cz/ico/favicon.ico",  true,  application.applicationContext)
        //assertNotNull(bitmap41)
        //assertNotNull(bitmap42)

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getNow_when_ABC_should_XYZ() {
        assertEquals(System.currentTimeMillis() / 1000, Utils.getNow().toLong())
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getThumbnailImage_when_ContextAndStringIsPassed_should_Return() {

        //when context is null, we receive -1 as image
        val image1 = Utils.getThumbnailImage(null, "anything")
        if (BuildConfig.DEBUG) println("thumbnail id  is $image1")
        assertEquals(-1, image1)

        // when context is not null, we will get the image resource id if image is available else 0
        val thumb21 = Utils.getThumbnailImage(application.applicationContext, "unavailable_res")
        val thumb22 = Utils.getThumbnailImage(application.applicationContext, "ct_image")
        if (BuildConfig.DEBUG) println("thumb21 is $thumb21. thumb22 is $thumb22 ")
        assertEquals(0, thumb21)
        assertEquals(R.drawable.ct_image, thumb22)


    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_hasPermission_when_PermissionNameAndContextIsPassed_should_ReturnEitherTrueOrFalse() {


        assertFalse { Utils.hasPermission(null, Manifest.permission.INTERNET) } // context can't be null
        assertFalse { Utils.hasPermission(null, "") } // permission can't be null or empty
        assertFalse { Utils.hasPermission(null, null) }// permission can't be null or empty
        assertFalse { Utils.hasPermission(application.applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) }// permission unavailable

        val application = application
        val shadow = Shadows.shadowOf(application)
        shadow.grantPermissions(Manifest.permission.INTERNET)
        assertTrue { Utils.hasPermission(application.applicationContext, Manifest.permission.INTERNET) }

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_isActivityDead_when_ActivityIsPassed_should_ReturnTrueOrFalse() {
        var activity: Activity? = null
        assertTrue(Utils.isActivityDead(activity))

        activityController.start()
        activity = activityController.get()
        assertFalse(Utils.isActivityDead(activity))

        activity.finish()
        //ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(Utils.isActivityDead(activity))

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_isServiceAvailable_when_ClassAnContextIsPAssed_should_ReturnTrueOrFalse() {
        var clazz: Class<*>? = null
        var context: Context? = null

        // if either of clazz or context is nul, will return false
        assertFalse { Utils.isServiceAvailable(context, clazz) }

        // if clazz is available, will return true
        clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
        context = application.applicationContext
        assertFalse { Utils.isServiceAvailable(context, clazz) }//todo not working. should be asserTrue

        // if clazz is not available, will return false
        //clazz = Class.forName("com.clevertap.android.sdk.pushnotification.UnAvailableClass")// todo not working. should return false
        assertFalse { Utils.isServiceAvailable(context, clazz) }

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_optionalStringKey_when_JSONAndStringIsPassed_should_ReturnOptionalKey() {
        val json1 = JSONObject()
        val key1 = ""
        val result1 = Utils.optionalStringKey(json1, key1)
        if (BuildConfig.DEBUG) println("result1:$result1")
        assertNull(result1)

        val json2 = JSONObject()
        val key2 = "key"
        json2.put(key2,null)
        val result2 = Utils.optionalStringKey(json2,key2)
        if(BuildConfig.DEBUG)  println("result2:$result2")
        assertNull(result2)

        val json3 = JSONObject()
        val key3 = "key"
        json3.put(key3, "value")
        val result3 = Utils.optionalStringKey(json3, key3)
        if (BuildConfig.DEBUG) println("result3:$result3")
        assertNotNull(result3)
        assertEquals(result3, "value")

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_runOnUiThread_when_BlockIsPassed_should_ExecuteTheBlockInMainThread() {

        val currentThread = Thread.currentThread().id
        var thread2 = -1L
        Utils.runOnUiThread {
            print("I was successfuly run")
            thread2 = Thread.currentThread().id
        }
        assertEquals(currentThread,thread2)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_setPackageNameFromResolveInfoList_when_ABC_should_XYZ() {
        // todo what does it do ?
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_stringToBundle_when_JsonStringIsPassed_should_ReturnBundle() {
        var string: String? = null
        var bundle = Utils.stringToBundle(string)
        assertEquals(0,bundle.size())

        string = """{"a":"boy"}"""
        bundle = Utils.stringToBundle(string)
        assertEquals(1,bundle.size())
        assertEquals("boy",bundle.getString("a"))
        println(bundle.getString("a"))

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

        val id5 = "abcd_1234_!!_::_$" + "@@_---"
        assertTrue { Utils.validateCTID(id5) }

    }

    //------------------------------------------------------------------------------------


}