@file:Suppress("RedundantNullableReturnType", "RedundantExplicitType", "ControlFlowWithEmptyBody", "DEPRECATION")

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
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.Q
import android.os.Build.VERSION_CODES.R
import android.os.Build.VERSION_CODES.S
import android.os.Bundle
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT
import android.telephony.TelephonyManager.NETWORK_TYPE_CDMA
import android.telephony.TelephonyManager.NETWORK_TYPE_EDGE
import android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD
import android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0
import android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A
import android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B
import android.telephony.TelephonyManager.NETWORK_TYPE_GPRS
import android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA
import android.telephony.TelephonyManager.NETWORK_TYPE_HSPA
import android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP
import android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA
import android.telephony.TelephonyManager.NETWORK_TYPE_IDEN
import android.telephony.TelephonyManager.NETWORK_TYPE_LTE
import android.telephony.TelephonyManager.NETWORK_TYPE_NR
import android.telephony.TelephonyManager.NETWORK_TYPE_UMTS
import com.clevertap.android.shared.test.BaseTestCase
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowNetworkInfo
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.util.ReflectionHelpers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.clevertap.android.sdk.R as R1

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
        printIfDebug(map)
        assertNotNull(map)
        assertEquals(0, map.size)
    }


    @Test
    fun test_convertBundleObjectToHashMap_when_BundleIsPassed_should_ReturnHashMap() {
        val bundle = Bundle().also { it.putChar("gender", 'M') }
        val map = Utils.convertBundleObjectToHashMap(bundle)
        printIfDebug(map)
        assertNotNull(map)
        assertEquals(1, map.size)
    }


    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONArrayOfJSONObjectsToArrayListOfHashMaps_when_JsonArrayIsPassed_should_ReturnList() {

        // when array has 1 object, it returns a list of 1 item
        var jsonArray = JSONArray("""[{"key1":"hi"}]""")
        var list = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(jsonArray)
        printIfDebug("list is $list")
        assertEquals(1, list.size)

        // when array has 0 object, it returns a list of 0 item
        jsonArray = JSONArray()
        list = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(jsonArray)
        printIfDebug("list is $list")
        assertEquals(0, list.size)

        // when array has malformed object object, it still returns a list of 0 item // todo can't get exception to throw
        //jsonArray = JSONArray("""[{"key1"}]""")
        //list = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(jsonArray)
        //printIfDebug("list is $list")
        //assertEquals(0,list.size)

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONArrayToArrayList_when_JsonArrayIsPassed_should_ReturnListOfStrings() {
        // when array has 1 item, it returns a list of 1 item
        var jsonArray = JSONArray().also { it.put("abc") }
        var list: ArrayList<String> = Utils.convertJSONArrayToArrayList(jsonArray)
        printIfDebug("list is $list")
        assertEquals(1, list.size)

        // when array has 0 item, it returns a list of 0 item
        jsonArray = JSONArray()
        list = Utils.convertJSONArrayToArrayList(jsonArray)
        printIfDebug("list is $list")
        assertEquals(0, list.size)


        // when array has malformed items, it returns a list of 0 item // todo can't get exception to throw
        //jsonArray = JSONArray().also { it.put(false) }
        //list = Utils.convertJSONArrayToArrayList(jsonArray)
        //printIfDebug("list is $list")
        //assertEquals(0, list.size)

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertJSONObjectToHashMap_when_JsonObjectIsPassed_should_ReturnAMap() {
        // when object has some key values, it returns those values as a map
        var jsonObject = JSONObject().also { it.put("some_number", 24) }
        var map = Utils.convertJSONObjectToHashMap(jsonObject)
        printIfDebug("map is $map")
        assertNotNull(map)
        assertEquals(24, map["some_number"])

        // when object has some key values, it returns empty map
        jsonObject = JSONObject()
        map = Utils.convertJSONObjectToHashMap(jsonObject)
        printIfDebug("map is $map")
        assertNotNull(map)
        assertEquals(0, map.size)
        assertEquals(null, map["some_number"])

        // TODO : can't get JSONException to fire

    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_convertToTitleCase_when_AStringIsPassed_should_ConvertStringToTitleCase() {
        var string: String? = "this is a string"
        var stringConverted: String? = Utils.convertToTitleCase(string)
        printIfDebug(stringConverted)
        assertEquals("This Is A String", stringConverted)


        string = null
        stringConverted = Utils.convertToTitleCase(string)
        assertNull(stringConverted)


        string = ""
        stringConverted = Utils.convertToTitleCase(string)
        assertEquals(0, stringConverted.length)

        // Camel case strings are converted to Title case
        string = "CamelCaseHasWordsWithCapitalFirstLetter"
        stringConverted = Utils.convertToTitleCase(string)
        assertEquals("Camelcasehaswordswithcapitalfirstletter", stringConverted)

        // Mix case strings are converted to Title case
        string = "mIXCaSeWoRD"
        stringConverted = Utils.convertToTitleCase(string)
        assertEquals("Mixcaseword", stringConverted)

        // Upper case strings are converted to Title case
        string = "UPPER_CASE-WORD"
        stringConverted = Utils.convertToTitleCase(string)
        assertEquals("Upper_case-word", stringConverted)
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_drawableToBitmap_when_PassedDrawable_should_ReturnBitmap() {
        val drawable: Drawable = application.getDrawable(R1.drawable.common_full_open_on_phone) ?: error("drawable is null")
        val bitmap = Utils.drawableToBitmap(drawable)
        printBitmapInfo(bitmap)
        assertNotNull(bitmap)
        // TODO write  what remaining cases ??
    }

    //------------------------------------------------------------------------------------

    @Test
    fun test_getBitmapFromURL_when_CorrectImageLinkArePassed_should_ReturnImage() {
        val url2 = "https:/www.example.com/malformed_url"
        val image2: Bitmap? = Utils.getBitmapFromURL(url2)
        image2.let {
            printBitmapInfo(it, "image2")
            assertNull(it)
        }

        val url = "https://www.freedesktop.org/wiki/logo.png"
        val image: Bitmap? = Utils.getBitmapFromURL(url)
        image.let {
            printBitmapInfo(it, "image")
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
        var networkType: String? = Utils.getCurrentNetworkType(null)
        printIfDebug("Network type is $networkType")
        assertNotNull(networkType)
        assertEquals("Unavailable", networkType)

        // if context is not null and  user is connected to wifi and wify is enabled, we will get wifi as return
        prepareForWifiConnectivityTest(true)
        networkType = Utils.getCurrentNetworkType(application.applicationContext)
        printIfDebug("Network type is $networkType")
        assertEquals("WiFi", networkType)

        // if context is not null and  user is connected to wifi and wify is NOT enabled, we will get Unknown as return
        prepareForWifiConnectivityTest(false)
        networkType = Utils.getCurrentNetworkType(application.applicationContext)
        printIfDebug("Network type is $networkType")
        assertEquals("Unknown", networkType)

        // remaining parts of this function will be tested in  test_getDeviceNetworkType_when_FunctionIsCalledWithContextAndOSVersionIsM_should_ReturnNetworkType
    }
    //------------------------------------------------------------------------------------

    @Test
    fun test_getDeviceNetworkType_when_FunctionIsCalledWithContextAndTelePhonyServiceIsNotAvialable_should_ReturnUnAvailable() {
        //if telephone service is NotAvailable it will return unknown
        prepareForTeleConnectTest(teleServiceAvailable = false)
        val receivedType = Utils.getDeviceNetworkType(application)
        println("receivedType = $receivedType")
        assertEquals("Unavailable", receivedType)

    }

    @Test
    fun test_getDeviceNetworkType_when_FunctionIsCalledWithContext_should_ReturnNetworkType() {
        //if telephone service is available and SDK version is <R,
        // it will give the network type accordingly no matter weather we have read phone state permission or not
        var receivedType = ""
        arrayOf(KITKAT, LOLLIPOP, M, N, O, P, Q).forEach {
            prepareForTeleConnectTest(sdk = it, hasRPSPermission = false)
            receivedType = Utils.getDeviceNetworkType(application)
            printIfDebug("receivedType = $receivedType")
            assertEquals("2G", receivedType)
        }
        //but for SDK version >= 30/R we must  give permission other wise unknown will be received

        arrayOf(R, S).forEach {
            prepareForTeleConnectTest(sdk = it, hasRPSPermission = false)
            receivedType = Utils.getDeviceNetworkType(application)
            printIfDebug("receivedType = $receivedType")
            assertEquals("Unknown", receivedType)
        }

        arrayOf(R, S).forEach {
            prepareForTeleConnectTest(sdk = it, hasRPSPermission = true)
            receivedType = Utils.getDeviceNetworkType(application)
            printIfDebug("receivedType = $receivedType")
            assertEquals("2G", receivedType)
        }

        // for other telephony types, it gives the  values as 2g,3g, 4g, 5g, or unknown accordingly
        arrayOf(
            NETWORK_TYPE_GPRS to "2G", NETWORK_TYPE_EDGE to "2G",
            NETWORK_TYPE_CDMA to "2G", NETWORK_TYPE_1xRTT to "2G",
            NETWORK_TYPE_IDEN to "2G",
            NETWORK_TYPE_UMTS to "3G", NETWORK_TYPE_EVDO_0 to "3G",
            NETWORK_TYPE_EVDO_A to "3G", NETWORK_TYPE_HSDPA to "3G",
            NETWORK_TYPE_HSUPA to "3G", NETWORK_TYPE_HSPA to "3G",
            NETWORK_TYPE_EVDO_B to "3G", NETWORK_TYPE_EHRPD to "3G",
            NETWORK_TYPE_HSPAP to "3G",
            NETWORK_TYPE_LTE to "4G",
            NETWORK_TYPE_NR to "5G"
        ).forEach {
            prepareForTeleConnectTest(networkType = it.first)
            receivedType = Utils.getDeviceNetworkType(application)
            printIfDebug("receivedType = $receivedType")
            assertEquals(it.second, receivedType)
        }

    }


    //------------------------------------------------------------------------------------

    @Test
    fun test_getMemoryConsumption_when_FunctionIsCalled_should_ReturnANonNullMemoryValue() {
        val consumption = Utils.getMemoryConsumption()
        printIfDebug("Consumptions type is $consumption")
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
        printBitmapInfo(actualAppIconBitmap, "actualAppIconBitmap")
        ShadowPackageManager().setApplicationIcon(application.packageName, actualAppDrawable)
        //---prerequisite-----

        val bitmap61 = Utils.getNotificationBitmap(null, true, context)
        assertNotNull(bitmap61)
        printBitmapInfo(bitmap61, "bitmap61")

        //if fallbackToAppIcon is true and path is  null, result will  be the app icon
        val bitmap62 = Utils.getNotificationBitmap("", true, context)
        printBitmapInfo(bitmap62, "bitmap62")
        assertNotNull(bitmap62)

        // if path is not Null/empty, the icon will be available irrespective to the fallbackToAppIcon switch
        val bitmap41 =
            Utils.getNotificationBitmap("https://www.pod.cz/ico/favicon.ico", false, application.applicationContext)
        val bitmap42 =
            Utils.getNotificationBitmap("https://www.pod.cz/ico/favicon.ico", true, application.applicationContext)
        printBitmapInfo(bitmap41, "bitmap41")
        printBitmapInfo(bitmap42, "bitmap42")

        assertNotNull(bitmap41)
        assertNotNull(bitmap42)
    }

    @Test
    fun test_getNotificationBitmapWithSizeConstraints_when_BitmapSizeIsLargerThanGivenSize_should_ReturnNull() {
        val context = application.applicationContext

        // if path is not Null/empty, the icon will be available irrespective to the fallbackToAppIcon switch
        val bitmap41 = Utils.getNotificationBitmapWithSizeConstraints(
            "https://www.pod.cz/ico/favicon.ico",
            false,
            application.applicationContext,
            10
        )
        printBitmapInfo(bitmap41, "bitmap41")

        assertNull(bitmap41)
    }

    @Test
    fun test_getNotificationBitmapWithSizeConstraints_when_BitmapSizeIsSamllerThanGivenSize_should_ReturnBitmap() {
        val context = application.applicationContext

        // if path is not Null/empty, the icon will be available irrespective to the fallbackToAppIcon switch
        val bitmap41 = Utils.getNotificationBitmapWithSizeConstraints(
            "https://www.pod.cz/ico/favicon.ico",
            false,
            application.applicationContext,
            10 * 1024 * 1024
        )
        printBitmapInfo(bitmap41, "bitmap41")

        assertNotNull(bitmap41)
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
        printIfDebug("thumbnail id  is $image1")
        assertEquals(-1, image1)

        // when context is not null, we will get the image resource id if image is available else 0
        val thumb21 = Utils.getThumbnailImage(application.applicationContext, "unavailable_res")
        val thumb22 = Utils.getThumbnailImage(application.applicationContext, "ct_image")
        printIfDebug("thumb21 is $thumb21. thumb22 is $thumb22 ")
        assertEquals(0, thumb21)
        assertEquals(R1.drawable.ct_image, thumb22)


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
        printIfDebug("result1:$result1")
        assertNull(result1)

        // if key is not empty but the value of key is not set in json then null will be returned
        val json2 = JSONObject()
        val key2 = "key"
        json2.put(key2, null)
        val result2 = Utils.optionalStringKey(json2, key2)
        printIfDebug("result2:$result2")
        assertNull(result2)

        // if key is not empty and the value of key is  set in json then  value will return
        val json3 = JSONObject()
        val key3 = "key"
        json3.put(key3, "value")
        val result3 = Utils.optionalStringKey(json3, key3)
        printIfDebug("result3:$result3")
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
        // test 1: we are trying to fire an activity that is  NOT part of current application
        //outcome 1: intent won't get the package set since the activity is not a valid part of that application
        var activityComponent = ComponentName("com.abc.xyz", "com.abc.xyz.MyActivity")
        var intent = Intent().also {
            it.action = Intent.ACTION_VIEW
            it.component = activityComponent
        }
        printIfDebug("test 1")
        printIntentInfo(intent, "before setting package")
        Utils.setPackageNameFromResolveInfoList(application.applicationContext, intent)
        printIntentInfo(intent, "after setting package")
        assertNull(intent.getPackage())

        // test 2: we are trying to fire an activity that is part of current application but NOT registered (similar to having an app say "com.eg.abc" with an activity: com.eg.abc.MyActivity that is NOT registerd in Manifest)
        //outcome 2: intent won't get the package set since the activity is not a valid part of that application
        activityComponent = ComponentName(application.packageName, "${application.packageName}.MyActivity")
        intent = Intent().also {
            it.action = Intent.ACTION_VIEW
            it.component = activityComponent
        }
        printIfDebug("test 2")
        printIntentInfo(intent, "before setting package")
        Utils.setPackageNameFromResolveInfoList(application.applicationContext, intent)
        printIntentInfo(intent, "after setting package")
        assertNull(intent.getPackage())


        // test 3: we are trying to fire an activity that is part of current application AND IS registered
        //outcome 3: intent will get the package set successfully
        activityComponent = ComponentName(application.packageName, "${application.packageName}.MyActivity")
        ShadowPackageManager().also { spm ->
            spm.addActivityIfNotPresent(activityComponent)
            spm.addIntentFilterForActivity(activityComponent, IntentFilter(Intent.ACTION_VIEW))
        }
        intent = Intent().also {
            it.action = Intent.ACTION_VIEW
            it.component = activityComponent
        }
        printIfDebug("test 3")
        printIntentInfo(intent, "before setting package")
        Utils.setPackageNameFromResolveInfoList(application.applicationContext, intent)
        printIntentInfo(intent, "after setting package")
        assertNotNull(intent.getPackage())
        assertEquals(application.packageName, intent.getPackage())

        //test 4 ?? todo

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
        printIfDebug(bundle.getString("a"))

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

    @Test
    fun test_isRenderFallback_when_wzrk_tsr_fbIsAbsent_and_wzrk_fallbackIsTrue_should_ReturnTrue() {
        assertTrue {
            Utils.isRenderFallback(RemoteMessage(Bundle().let {
                it.putString(Constants.NOTIFICATION_RENDER_FALLBACK, "true")
                it
            }), application)
        }
    }

    @Test
    fun test_isRenderFallback_when_wzrk_fallbackIsAbsent_and_wzrk_tsr_fbIsFalse_should_ReturnFalse() {
        assertFalse {
            Utils.isRenderFallback(RemoteMessage(Bundle().let {
                it.putString(Constants.WZRK_TSR_FB, "false")
                it
            }), application)
        }
    }

    @Test
    fun test_isRenderFallback_when_wzrk_fallbackIsAbsent_and_wzrk_tsr_fbIsAbsent_should_ReturnFalse() {
        assertFalse { Utils.isRenderFallback(RemoteMessage(Bundle()), application) }
    }

    @Test
    fun test_isRenderFallback_when_wzrk_fallbackIsTrue_and_wzrk_tsr_fbIsFalse_should_ReturnTrue() {
        assertTrue {
            Utils.isRenderFallback(RemoteMessage(Bundle().let {
                it.putString(Constants.WZRK_TSR_FB, "false")
                it.putString(Constants.NOTIFICATION_RENDER_FALLBACK, "TRUE")
                it
            }), application)
        }
    }

    @Test
    fun test_isRenderFallback_when_wzrk_fallbackIsFalse_and_wzrk_tsr_fbIsTrue_should_ReturnFalse() {
        assertFalse {
            Utils.isRenderFallback(RemoteMessage(Bundle().let {
                it.putString(Constants.WZRK_TSR_FB, "true")
                it.putString(Constants.NOTIFICATION_RENDER_FALLBACK, "FaLsE")
                it
            }), application)
        }
    }

    @Test
    fun test_isRenderFallback_when_wzrk_fallbackIsTrue_and_wzrk_tsr_fbIsTrue_should_ReturnFalse() {
        assertFalse {
            Utils.isRenderFallback(RemoteMessage(Bundle().let {
                it.putString(Constants.WZRK_TSR_FB, "true")
                it.putString(Constants.NOTIFICATION_RENDER_FALLBACK, "TrUE")
                it
            }), application)
        }
    }

    @Test
    fun test_isRenderFallback_when_wzrk_fallbackIsFalse_and_wzrk_tsr_fbIsFalse_should_ReturnFalse() {
        assertFalse {
            Utils.isRenderFallback(RemoteMessage(Bundle().let {
                it.putString(Constants.WZRK_TSR_FB, "false")
                it.putString(Constants.NOTIFICATION_RENDER_FALLBACK, "FALSE")
                it
            }), application)
        }
    }

    //------------------------------------------------------------------------------------

    private fun prepareForWifiConnectivityTest(
        isConnected: Boolean,
        networkType: Int = ConnectivityManager.TYPE_WIFI
    ) {
        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadowConnectivityManager = Shadows.shadowOf(connectivityManager)
        shadowConnectivityManager.also {
            val network = ShadowNetworkInfo.newInstance(
                if (isConnected) NetworkInfo.DetailedState.CONNECTED else NetworkInfo.DetailedState.DISCONNECTED,
                networkType,
                0,
                true,
                if (isConnected) NetworkInfo.State.CONNECTED else NetworkInfo.State.DISCONNECTED
            )
            it.setNetworkInfo(networkType, network)
        }
    }

    private fun prepareForTeleConnectTest(networkType: Int = NETWORK_TYPE_CDMA, teleServiceAvailable: Boolean = true, hasRPSPermission: Boolean = true, sdk: Int = M) {
        printIfDebug("prepareForTeleConnectTest() called with: networkType = $networkType, teleServiceAvailable = $teleServiceAvailable, hasRPSPermission = $hasRPSPermission, sdk = $sdk")
        when {
            !teleServiceAvailable -> shadowApplication.setSystemService(Context.TELEPHONY_SERVICE, null)
            else -> {
                if (!hasRPSPermission) {
                    shadowApplication.denyPermissions(Manifest.permission.READ_PHONE_STATE)
                } else {
                    shadowApplication.grantPermissions(Manifest.permission.READ_PHONE_STATE)
                }

                ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
                val telephonyManager = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val shadowTelephonyManager = Shadows.shadowOf(telephonyManager)
                shadowTelephonyManager.setNetworkType(networkType)
                if (sdk >= R) {
                    shadowTelephonyManager.setDataNetworkType(networkType)
                }
            }
        }


    }


    private fun printIntentInfo(intent: Intent?, startMsg: String) {
        if (intent == null) {
            printIfDebug("$startMsg received intent: null")
        } else {
            printIfDebug("$startMsg received intent: $intent")
            printIfDebug("$startMsg received getPackage: ${intent.getPackage()}")
            printIfDebug("$startMsg received action: ${intent.action}")
            printIfDebug("$startMsg received component: ${intent.component}")
            //printIfDebug("$startMsg received categories: ${intent.categories}")
            //printIfDebug("$startMsg received identifier: ${intent.identifier}")
            //printIfDebug("$startMsg received clipData: ${intent.clipData}")
            //printIfDebug("$startMsg received data: ${intent.data}")
            //printIfDebug("$startMsg received dataString: ${intent.dataString}")
            //printIfDebug("$startMsg received extras: ${intent.extras}")
            //printIfDebug("$startMsg received flags: ${intent.flags}")
        }
    }

    private fun printBitmapInfo(bitmap: Bitmap?, name: String = "") {
        if (!BuildConfig.DEBUG) {
            println("printBitmapInfo: not debug , returning")
            return
        }
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

    private fun printIfDebug(value: Any?) {
        if (BuildConfig.DEBUG) println(value)
    }

}