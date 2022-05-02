package com.clevertap.android.shared.test

import android.content.Context
import android.os.Build.VERSION_CODES
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication


/**
 * Naming Convention for Testing
 * 1. Classes : <Name of Class to be Test> + Test.kt
 *      e.g CTProductConfigControllerTest.kt for CTProductConfigController.java
 *
 * 2. Methods : test_<methodName>_<inputCondition>_<expectedBehavior>
 *     e.g test_constructor_whenFeatureFlagIsNotSave_InitShouldReturnTrue
 */
@Config(manifest = Config.NONE, sdk = [VERSION_CODES.P], application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
abstract class BaseTestCase {

    protected lateinit var application: TestApplication
    protected lateinit var shadowApplication: ShadowApplication
    protected lateinit var cleverTapAPI: CleverTapAPI
    protected lateinit var cleverTapInstanceConfig: CleverTapInstanceConfig
    protected lateinit var activityController: ActivityController<TestActivity>
    protected lateinit var appCtx:Context

    @Before
    open fun setUp() {
        application = TestApplication.application
        shadowApplication = Shadows.shadowOf(application)
        cleverTapAPI = Mockito.mock(CleverTapAPI::class.java)
        cleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(application, Constant.ACC_ID, Constant.ACC_TOKEN)
        activityController = Robolectric.buildActivity(TestActivity::class.java)
        appCtx = application.applicationContext

    }


    fun getSampleJsonArrayOfJsonObjects(totalJsonObjects: Int = 0, start:Int=1, printGenArray:Boolean=false): JSONArray {

        val range = JSONArray()
        (start until start+totalJsonObjects).forEach {
            val obj = JSONObject()
            obj.put("key$it", it)
            range.put(obj)
        }
        return range.also { if(printGenArray)println("generated : $range") }
    }

    fun getSampleJsonArrayOfStrings(totalJsonObjects: Int = 0, start:Int=1, printGenArray:Boolean=false): JSONArray {

        val range = JSONArray()
        (start until start+totalJsonObjects).forEach {
            range.put("value$it")
        }
        return range.also { if(printGenArray)println("generated : $range") }
    }

}