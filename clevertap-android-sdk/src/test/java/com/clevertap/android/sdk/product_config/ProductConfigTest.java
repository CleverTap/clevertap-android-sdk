package com.clevertap.android.sdk.product_config;

import com.clevertap.android.sdk.BaseTestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductConfigTest extends BaseTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testFetch() {
        when(cleverTapAPI.productConfig()).thenReturn(new CTProductConfigController(application, "12121", cleverTapInstanceConfig, cleverTapAPI));
        cleverTapAPI.productConfig().fetch();
        verify(cleverTapAPI).fetchProductConfig();
    }

    @Test
    public void testGetBoolean() {
        CTProductConfigController ctProductConfigController = mock(CTProductConfigController.class);
        when(cleverTapAPI.productConfig()).thenReturn(ctProductConfigController);
        when(ctProductConfigController.getBoolean("testBool")).thenReturn(false);
        Assert.assertFalse(cleverTapAPI.productConfig().getBoolean("testBool"));
    }

    @Test
    public void testGetLong() {
        CTProductConfigController ctProductConfigController = mock(CTProductConfigController.class);
        when(cleverTapAPI.productConfig()).thenReturn(ctProductConfigController);
        when(ctProductConfigController.getLong("testLong")).thenReturn(122212121L);
        Assert.assertNotEquals(new Long(12), cleverTapAPI.productConfig().getLong("testLong"));
        Assert.assertEquals(new Long(122212121), cleverTapAPI.productConfig().getLong("testLong"));
    }

    @Test
    public void testGetDouble() {
        CTProductConfigController ctProductConfigController = mock(CTProductConfigController.class);
        when(cleverTapAPI.productConfig()).thenReturn(ctProductConfigController);
        when(ctProductConfigController.getDouble("testDouble")).thenReturn(122.21d);
        Assert.assertNotEquals(12d, cleverTapAPI.productConfig().getDouble("testDouble"));
        Assert.assertEquals(new Double(122.21), cleverTapAPI.productConfig().getDouble("testDouble"));
    }

    @Test
    public void testGetString() {
        CTProductConfigController ctProductConfigController = mock(CTProductConfigController.class);
        when(cleverTapAPI.productConfig()).thenReturn(ctProductConfigController);
        when(ctProductConfigController.getString("testString")).thenReturn("Testing String");
        Assert.assertNotEquals("Wrong Value", cleverTapAPI.productConfig().getString("testString"));
        Assert.assertEquals("Testing String", cleverTapAPI.productConfig().getString("testString"));
    }

    @Test
    public void testActivate() {
//        CTProductConfigController ctProductConfigController = mock(CTProductConfigController.class);
//        when(cleverTapAPI.productConfig()).thenReturn(ctProductConfigController);
//        Assert.assertEquals("Testing String", cleverTapAPI.productConfig().getString("testString"));
    }
}