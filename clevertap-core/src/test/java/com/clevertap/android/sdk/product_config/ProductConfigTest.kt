package com.clevertap.android.sdk.product_config

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.mockito.*

class ProductConfigTest : BaseTestCase() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun testFetch() {
        Mockito.`when`(cleverTapAPI!!.productConfig())
            .thenReturn(CTProductConfigController(application, "12121", cleverTapInstanceConfig, cleverTapAPI))
        cleverTapAPI!!.productConfig().fetch()
        Mockito.verify(cleverTapAPI)!!.fetchProductConfig()
    }

    @Test
    fun testGetBoolean() {
        val ctProductConfigController = Mockito.mock(CTProductConfigController::class.java)
        Mockito.`when`(cleverTapAPI!!.productConfig()).thenReturn(ctProductConfigController)
        Mockito.`when`(ctProductConfigController.getBoolean("testBool")).thenReturn(false)
        Assert.assertFalse(cleverTapAPI!!.productConfig().getBoolean("testBool"))
    }

    @Test
    fun testGetLong() {
        val ctProductConfigController = Mockito.mock(CTProductConfigController::class.java)
        Mockito.`when`(cleverTapAPI!!.productConfig()).thenReturn(ctProductConfigController)
        Mockito.`when`(ctProductConfigController.getLong("testLong")).thenReturn(122212121L)
        Assert.assertNotEquals(12, cleverTapAPI!!.productConfig().getLong("testLong"))
        Assert.assertEquals(122212121, cleverTapAPI!!.productConfig().getLong("testLong"))
    }

    @Test
    fun testGetDouble() {
        val ctProductConfigController = Mockito.mock(CTProductConfigController::class.java)
        Mockito.`when`(cleverTapAPI!!.productConfig()).thenReturn(ctProductConfigController)
        Mockito.`when`(ctProductConfigController.getDouble("testDouble")).thenReturn(122.21)
        Assert.assertNotEquals(12.0, cleverTapAPI!!.productConfig().getDouble("testDouble"))
        Assert.assertEquals(122.21, cleverTapAPI!!.productConfig().getDouble("testDouble"), 0.0)
    }

    @Test
    fun testGetString() {
        val ctProductConfigController = Mockito.mock(CTProductConfigController::class.java)
        Mockito.`when`(cleverTapAPI!!.productConfig()).thenReturn(ctProductConfigController)
        Mockito.`when`(ctProductConfigController.getString("testString")).thenReturn("Testing String")
        Assert.assertNotEquals("Wrong Value", cleverTapAPI!!.productConfig().getString("testString"))
        Assert.assertEquals("Testing String", cleverTapAPI!!.productConfig().getString("testString"))
    }

    @Test
    fun testActivate() {
//        CTProductConfigController ctProductConfigController = mock(CTProductConfigController.class);
//        when(cleverTapAPI.productConfig()).thenReturn(ctProductConfigController);
//        Assert.assertEquals("Testing String", cleverTapAPI.productConfig().getString("testString"));
    }
}