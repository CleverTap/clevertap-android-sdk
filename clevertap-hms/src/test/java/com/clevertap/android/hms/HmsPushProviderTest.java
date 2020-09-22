package com.clevertap.android.hms;

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
@Config(sdk = 28,
        application = TestApplication.class
)
public class HmsPushProviderTest extends BaseTestCase {

    private CTPushProviderListener ctPushProviderListener;
    private HmsPushProvider pushProvider;
    private TestHmsSdkHandler sdkHandler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        pushProvider = new HmsPushProvider();
        ctPushProviderListener = mock(CTPushProviderListener.class);
        sdkHandler = new TestHmsSdkHandler();
        when(ctPushProviderListener.context()).thenReturn(application);
        when(ctPushProviderListener.config()).thenReturn(cleverTapInstanceConfig);
        pushProvider.setCTPushListener(ctPushProviderListener);
        pushProvider.setHmsSdkHandler(sdkHandler);
    }

    @Test
    public void testRequestToken() {
        pushProvider.requestToken();
        verify(ctPushProviderListener).onNewToken(HmsTestConstants.HMS_TOKEN, PushConstants.PushType.HPS);
    }

    @Test
    public void testIsAvailable() {
        sdkHandler.setAvailable(false);
        Assert.assertFalse(pushProvider.isAvailable());
        sdkHandler.setAvailable(true);
        Assert.assertTrue(pushProvider.isAvailable());
    }

    @Test
    public void testIsSupported() {
        sdkHandler.setSupported(true);
        Assert.assertTrue(pushProvider.isSupported());
        sdkHandler.setSupported(false);
        Assert.assertFalse(pushProvider.isSupported());
    }
}