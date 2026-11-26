package com.clevertap.android.sdk.product_config

import org.junit.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductConfigUtilTest {
    @Test
    fun isSupportedDataType(){
        val obj = "1212"
        assertTrue(ProductConfigUtil.isSupportedDataType(obj))

        val obj1 = 1212
        assertTrue(ProductConfigUtil.isSupportedDataType(obj1))

        val obj2 = false
        assertTrue(ProductConfigUtil.isSupportedDataType(obj2))

        val obj3 = Object()
        assertFalse(ProductConfigUtil.isSupportedDataType(obj3))

    }
}