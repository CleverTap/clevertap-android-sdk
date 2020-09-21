package com.clevertap.android.xps;

import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.shared.test.BaseTestCase;
import com.xiaomi.mipush.sdk.MiPushClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*"})
@PrepareForTest({MiPushClient.class})
public class XiaomiPushProviderTest extends BaseTestCase {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    private MiPushClient miPushClient;
    private CTPushProviderListener ctPushProviderListener;
    private XiaomiPushProvider xiaomiPushProvider;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        xiaomiPushProvider = new XiaomiPushProvider();
    }

    void tearDown() {
    }

    @Test
    public void testRequestToken() {
        miPushClient = mock(MiPushClient.class);
        ctPushProviderListener = mock(CTPushProviderListener.class);
        when(ctPushProviderListener.context()).thenReturn(application);
        when(ctPushProviderListener.config()).thenReturn(cleverTapInstanceConfig);
        String token = "abc";
        when(MiPushClient.getRegId(application)).thenReturn(token);
        xiaomiPushProvider.requestToken();
//        verify(ctPushProviderListener).onNewToken(token, PushConstants.PushType.XPS);
    }

    void isAvailable() {

    }
}