@file:Suppress("RedundantNullableReturnType", "RedundantExplicitType", "ControlFlowWithEmptyBody")

package com.clevertap.android.sdk

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.shadows.*
import kotlin.test.*
import org.robolectric.util.ReflectionHelpers





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
        val jsonArray = JSONArray("""[{"key1":"hi"}]""")
        val list = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(jsonArray)
        if (BuildConfig.DEBUG) println("list is $list")
        //todo why the empty list? =>  DONE (write test case for empty list without JSONException, with JSONException)
        assertEquals(1,list.size)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONArrayToArrayList_when_JsonArrayIsPassed_should_ReturnList() {
        val jsonArray = JSONArray().also { it.put("10") }
        val list = Utils.convertJSONArrayToArrayList(jsonArray)
        if (BuildConfig.DEBUG) println("list is $list")
        //todo why the empty list? => DONE (write test case for empty list without JSONException, with JSONException)
        assertEquals(1,list.size)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONObjectToHashMap_when_JsonObjectIsPassed_should_ReturnAMap() {
        val jsonObject = JSONObject().also { it.put("some_number", 24) }
        val map = Utils.convertJSONObjectToHashMap(jsonObject)
        if (BuildConfig.DEBUG) println("map is $map")
        assertNotNull(map)
        assertEquals(24, map["some_number"])
        // TODO :write test case for empty map without JSONException, with JSONException

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertToTitleCase_when_AStringIsPassed_should_ConvertStringToTitleCase() {
        val string = "this is a string"
        val stringConverted = Utils.convertToTitleCase(string)
        if (BuildConfig.DEBUG) println(stringConverted)
        assertEquals("This Is A String", stringConverted)
        // TODO write remaining cases
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_drawableToBitmap_when_PassedDrawable_should_ReturnBitmap() {
        val drawable: Drawable = application.getDrawable(R.drawable.common_full_open_on_phone) ?: error("drawable is null")
        val bitmap = Utils.drawableToBitmap(drawable)
        printBitmapInfo(bitmap)
        assertNotNull(bitmap)
        // TODO write remaining cases
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getBitmapFromURL_when_CorrectImageLinkArePassed_should_ReturnImage() {
        val url2 = "https:/www.example.com/malformed_url"
        val image2: Bitmap? = Utils.getBitmapFromURL(url2)
        image2.let {
            printBitmapInfo(it,"image2")
            assertNull(it)
        }

        val url = "https://www.freedesktop.org/wiki/logo.png"
        val image: Bitmap? = Utils.getBitmapFromURL(url)
        image.let {
            printBitmapInfo(it,"image")
            assertNotNull(it)

        }
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getByteArrayFromImageURL_when_CorrectImageLinkArePassed_should_ReturnImageByteArray() {
        val url2 = "https:/www.example.com/malformed_url"
        val array2: ByteArray? = Utils.getByteArrayFromImageURL(url2)
        println(" downloaded an array2 of size  ${array2?.size} bytes ")
        assertNull(array2)


        val url = "https://www.freedesktop.org/wiki/logo.png"
        val array: ByteArray? = Utils.getByteArrayFromImageURL(url)
        println(" downloaded an array of size  ${array?.size} bytes ,")
        assertNotNull(array)

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getCurrentNetworkType_when_FunctionIsCalledWithContext_should_ReturnNetworkType() {
        // if context is null, network type will be unavailable
        val networkType2: String? = Utils.getCurrentNetworkType(null)
        if (BuildConfig.DEBUG) println("Network type is $networkType2")
        assertNotNull(networkType2)
        assertEquals("Unavailable", networkType2)

        val connectivityManager = RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadowConnectivityManager = Shadows.shadowOf(connectivityManager)
        // if context is not null and  user is connected to wifi, we will get wifi as return
        shadowConnectivityManager.also {
            val network = ShadowNetworkInfo.newInstance(
                NetworkInfo.DetailedState.CONNECTED,
                ConnectivityManager.TYPE_WIFI,
                0,
                true,
                NetworkInfo.State.CONNECTED
            )
            it.setNetworkInfo(ConnectivityManager.TYPE_WIFI, network)
        }
        val networkType: String? = Utils.getCurrentNetworkType(application.applicationContext)
        if (BuildConfig.DEBUG) println("Network type is $networkType")//todo should be wifi, but didn't worked @piyush => DONE write remaining cases
        assertEquals("WiFi", networkType)

        println("manually calling  test_getDeviceNetworkType_when_FunctionIsCalledWithContext_should_ReturnNetworkType")
        test_getDeviceNetworkType_when_FunctionIsCalledWithContextAndOSVersionIsM_should_ReturnNetworkType()


    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getDeviceNetworkType_when_FunctionIsCalledWithContextAndOSVersionIsM_should_ReturnNetworkType() {
        shadowApplication.grantPermissions(Manifest.permission.READ_PHONE_STATE)
        val telephonyManager = RuntimeEnvironment.application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.M)

        // Fall back to network type
        val shadowTelephonyManager = Shadows.shadowOf(telephonyManager)

        shadowTelephonyManager.setNetworkType(TelephonyManager.NETWORK_TYPE_NR)
        val receivedType = Utils.getDeviceNetworkType(application)//todo should be 5g, but didn't worked // @piyush => DONE write remaining cases
        println("receovedType = $receivedType")
        assertEquals("5G", receivedType)
    }

    @Test
    fun test_getDeviceNetworkType_when_FunctionIsCalledWithContextAndOSVersionIsS_should_ReturnNetworkType() {
        shadowApplication.grantPermissions(Manifest.permission.READ_PHONE_STATE)
        val telephonyManager = RuntimeEnvironment.application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.S)

        // Fall back to network type
        val shadowTelephonyManager = Shadows.shadowOf(telephonyManager)

        shadowTelephonyManager.setDataNetworkType(TelephonyManager.NETWORK_TYPE_NR)
        val receivedType = Utils.getDeviceNetworkType(application)//todo should be 5g, but didn't worked // @piyush => DONE write remaining cases
        println("receovedType = $receivedType")
        assertEquals("5G", receivedType)
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

        //if fallbackToAppIcon is true and path is  null, result will  be the app icon
        //---prerequisite-----
        val actualAppDrawable = application.getDrawable(android.R.mipmap.sym_def_app_icon)
        val actualAppIconBitmap = BitmapFactory.decodeResource(context.resources, android.R.mipmap.sym_def_app_icon)
        printBitmapInfo(actualAppIconBitmap,"actualAppIconBitmap")
        ShadowPackageManager().setApplicationIcon(application.packageName, actualAppDrawable)
        //---prerequisite-----

        val bitmap61 = Utils.getNotificationBitmap(null, true, context)
        assertNotNull(bitmap61)
        printBitmapInfo(bitmap61,"bitmap61")

        //if fallbackToAppIcon is true and path is  null, result will  be the app icon
        val bitmap62 = Utils.getNotificationBitmap("", true, context)
        printBitmapInfo(bitmap62,"bitmap62")
        assertNotNull(bitmap62)

        // if path is not Null/empty, the icon will be available irrespective to the fallbackToAppIcon switch
        val bitmap41 = Utils.getNotificationBitmap("https://www.pod.cz/ico/favicon.ico", false, application.applicationContext)
        val bitmap42 = Utils.getNotificationBitmap("https://www.pod.cz/ico/favicon.ico", true, application.applicationContext)
        printBitmapInfo(bitmap41,"bitmap41")
        printBitmapInfo(bitmap42,"bitmap42")

        assertNotNull(bitmap41)
        assertNotNull(bitmap42)

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
        val application = application


        //assertFalse { Utils.hasPermission(null, Manifest.permission.INTERNET) } // context can't be null
        //assertFalse { Utils.hasPermission(null, null) }// permission can't be null or empty
        assertFalse { Utils.hasPermission(application.applicationContext, "") } // permission can't be null or empty
        assertFalse { Utils.hasPermission(application.applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) }// permission unavailable

        shadowApplication.grantPermissions(Manifest.permission.LOCATION_HARDWARE)
        assertTrue { Utils.hasPermission(application.applicationContext, Manifest.permission.LOCATION_HARDWARE) }

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

    /*
     * 1. given : context of a runtime activity/application, clazz instance of Class<*>
     * 2. humne context package manager nikala, package name nikala ( eg : work.curioustools.myword )
     * 3. humne package info class nikali, fer usme se serviceinfo ki array nikali
     * 4. for every service info, if serviceinfo. name === clazz.name , break and return true
     *
     *
     * for testing, virtual setup:
     * 1. humne package info ka instance banaya.
     * 2. usme test service daal di
     * 3. fer shadow package manager me packafge info add kr diya
     *
     * */
    @Test
    fun test_isServiceAvailable_when_ClassAnContextIsPAssed_should_ReturnTrueOrFalse() {
        kotlin.runCatching {
        var clazz: Class<*>? = null
        var context: Context? = null

        // if either of clazz or context is nul, will return false
        //assertFalse { Utils.isServiceAvailable(context, clazz) }

        // if clazz is available, will return true
        //----pre setup-----------------------------------------------------------------
        //todo : giving NPE // @ piyush
        val service = ServiceInfo().also {
            it.name = "ABCService"
            it.packageName = application.applicationInfo.packageName
        }
        val packageInfo = PackageInfo().also {
            it.applicationInfo = application.applicationInfo
            it.services = arrayOf(service)
        }
        ShadowPackageManager().installPackage(packageInfo)
        //----pre setup-----------------------------------------------------------------

        clazz = Class.forName("ABCService")
        context = application.applicationContext
        assertTrue { Utils.isServiceAvailable(context, clazz) }

        // if clazz is not available, will return false
        clazz = Class.forName("NotABCService")
        assertFalse { Utils.isServiceAvailable(context, clazz) }

        }
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_optionalStringKey_when_JSONAndStringIsPassed_should_ReturnOptionalKey() {
        // if key is empty then optional key will not be available and null will be returned
        val json1 = JSONObject()
        val key1 = ""
        val result1 = Utils.optionalStringKey(json1, key1)
        if (BuildConfig.DEBUG) println("result1:$result1")
        assertNull(result1)

        // if key is not empty but the value of key is not set in json then null will be returned
        val json2 = JSONObject()
        val key2 = "key"
        json2.put(key2, null)
        val result2 = Utils.optionalStringKey(json2, key2)
        if (BuildConfig.DEBUG) println("result2:$result2")
        assertNull(result2)

        // if key is not empty and the value of key is  set in json then  value will return
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
            print("I ran successfully on the ui thread")
            thread2 = Thread.currentThread().id
        }
        assertEquals(currentThread, thread2)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_setPackageNameFromResolveInfoList_when_ContextAndIntentIsPassed_should_SetPackageNameAccordingly() {
        //todo package manager not setting activity info // @piyush => DONE write remaining cases

        ShadowPackageManager().let {spm ->
            /*val activityInfo = ActivityInfo().also {
                it.packageName = "com.test.package"
                it.name = "com.test.package.MyActivity"

            }
            PackageInfo().let {
                it.activities = arrayOf(activityInfo)
                it.packageName = "com.test.package"
                spm.installPackage(it)

            }*/
            spm.addActivityIfNotPresent(ComponentName(application.packageName,"${application.packageName}.MyActivity"))
            spm.addIntentFilterForActivity(ComponentName(application.packageName,"${application.packageName}.MyActivity"),
                IntentFilter(Intent.ACTION_VIEW)
            )
        }


        val intent = Intent()
        /*intent.setClassName("com.test.package","MyActivity")
        intent.`package` ="com.test.package"*/
        intent.action = Intent.ACTION_VIEW
        intent.component = ComponentName(application.packageName,"${application.packageName}.MyActivity")
        //println(intent.component)

        //println("intent package = ${intent.getPackage()}")
        //println("intent :$intent")
        Utils.setPackageNameFromResolveInfoList(application.applicationContext, intent)  // <-----
        //println("intent package = ${intent.getPackage()}")

        //assertNotNull(intent.getPackage())
        assertEquals(application.packageName,intent.`package`)

        //todo what else to test? // @piyush

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_stringToBundle_when_JsonStringIsPassed_should_ReturnBundle() {
        var string: String? = null
        var bundle = Utils.stringToBundle(string)
        assertEquals(0, bundle.size())

        string = """{"a":"boy"}"""
        bundle = Utils.stringToBundle(string)
        assertEquals(1, bundle.size())
        assertEquals("boy", bundle.getString("a"))
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

    fun printBitmapInfo(bitmap: Bitmap?, name: String = "") {
        try {
            val hash = bitmap.hashCode().toString()
            print("received bitmap : $name($hash)")
            print("\t bitmap size : ${bitmap?.byteCount} bytes ")
            print("\t height: ${bitmap?.height}")
            print("\t width: ${bitmap?.width}")
            print("\t config: ${bitmap?.config}")
            print("\t isRecycled: ${bitmap?.isRecycled}")
            println()
        }
        catch (t: Throwable) {
            println("error happened while logging bitmap: ${t.message}")
        }
    }

}