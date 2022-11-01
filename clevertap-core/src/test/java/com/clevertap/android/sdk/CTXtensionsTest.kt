package com.clevertap.android.sdk

import android.os.Build.VERSION
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTXtensionsTest : BaseTestCase() {

    @Test
    fun test_when_sdkInt_and_targetSdkVersion_is_33_and_input_is_32_should_return_true() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 33)

        application.applicationContext.applicationInfo.targetSdkVersion = 33

        assertTrue { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_33_and_targetSdkVersion_is_32_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 33)

        application.applicationContext.applicationInfo.targetSdkVersion = 32

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_32_and_targetSdkVersion_is_33_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 32)

        application.applicationContext.applicationInfo.targetSdkVersion = 33

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_30_and_targetSdkVersion_is_30_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 30)

        application.applicationContext.applicationInfo.targetSdkVersion = 30

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }
}