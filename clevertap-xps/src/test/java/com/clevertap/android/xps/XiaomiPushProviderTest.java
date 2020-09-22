package com.clevertap.android.xps;

import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApplication.class)
public class XiaomiPushProviderTest extends BaseTestCase {

    private CTPushProviderListener ctPushProviderListener;
    private XiaomiPushProvider xiaomiPushProvider;
    private TestMiSdkHandler sdkHandler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        xiaomiPushProvider = new XiaomiPushProvider();
        ctPushProviderListener = mock(CTPushProviderListener.class);
        sdkHandler = new TestMiSdkHandler();
        when(ctPushProviderListener.context()).thenReturn(application);
        when(ctPushProviderListener.config()).thenReturn(cleverTapInstanceConfig);
        xiaomiPushProvider.setCTPushListener(ctPushProviderListener);
        xiaomiPushProvider.setMiSdkHandler(sdkHandler);
    }

    @Test
    public void testRequestToken() {
        xiaomiPushProvider.requestToken();
        verify(ctPushProviderListener).onNewToken(XpsTestConstants.MI_TOKEN, PushConstants.PushType.XPS);
    }

    @Test
    public void testIsAvailable() {
        sdkHandler.setAvailable(false);
        Assert.assertFalse(xiaomiPushProvider.isAvailable());
        sdkHandler.setAvailable(true);
        Assert.assertTrue(xiaomiPushProvider.isAvailable());
    }

    @Test
    public void testIsSupported() {
        Assert.assertTrue(xiaomiPushProvider.isSupported());
    }
}