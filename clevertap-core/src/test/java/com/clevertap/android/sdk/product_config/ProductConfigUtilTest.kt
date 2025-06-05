package com.clevertap.android.sdk.product_config

import org.junit.*

class ProductConfigUtilTest {
    @Test
    fun isSupportedDataType(){
        val obj = "1212"
        Assert.assertTrue(ProductConfigUtil.isSupportedDataType(obj))

        val obj1 = 1212
        Assert.assertTrue(ProductConfigUtil.isSupportedDataType(obj1))

        val obj2 = false
        Assert.assertTrue(ProductConfigUtil.isSupportedDataType(obj2))

        val obj3 = Object()
        Assert.assertFalse(ProductConfigUtil.isSupportedDataType(obj3))

    }
}