package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.APP_ID_KEY;
import static com.clevertap.android.hms.HmsConstants.HCM_SCOPE;
import static com.clevertap.android.hms.HmsTestConstants.HMS_APP_ID;
import static com.clevertap.android.hms.HmsTestConstants.HMS_TOKEN;
import static org.mockito.Mockito.*;

import com.clevertap.android.sdk.Application;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.shared.test.BaseTestCase;
import com.clevertap.android.shared.test.Constant;
import com.clevertap.android.shared.test.TestApplication;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.HuaweiApiAvailability;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApplication.class)
public class HmsHandlerTest extends BaseTestCase {

    private AGConnectServicesConfig config;

    private HuaweiApiAvailability huaweiApi;

    private HmsInstanceId instance;

    private Application mApplication;

    private HmsSdkHandler sdkHandler;

    @Before
    public void setUp() {
        mApplication = TestApplication.Companion.getApplication();
        CleverTapInstanceConfig mCleverTapInstanceConfig = CleverTapInstanceConfig
                .createInstance(TestApplication.Companion.getApplication(), Constant.ACC_ID, Constant.ACC_TOKEN);

        final CTPushProviderListener ctPushProviderListener = Mockito.mock(CTPushProviderListener.class);
        sdkHandler = new HmsSdkHandler(context, config);
        Mockito.when(ctPushProviderListener.context()).thenReturn(TestApplication.Companion.getApplication());
        Mockito.when(ctPushProviderListener.config()).thenReturn(mCleverTapInstanceConfig);

        instance = Mockito.mock(HmsInstanceId.class);
        config = Mockito.mock(AGConnectServicesConfig.class);
        huaweiApi = Mockito.mock(HuaweiApiAvailability.class);
    }

    @Test
    public void testAppId_Invalid() {
        Mockito.when(config.getString(APP_ID_KEY)).thenThrow(new RuntimeException("Something went wrong"));
        try (MockedStatic<AGConnectServicesConfig> mocked = mockStatic(AGConnectServicesConfig.class)) {
            Mockito.when(AGConnectServicesConfig.fromContext(mApplication)).thenReturn(config);
            String appId = sdkHandler.appId();
            Assert.assertNull(appId);
        }
    }

    @Test
    public void testAppId_Valid() {
        Mockito.when(config.getString(APP_ID_KEY)).thenReturn(HMS_APP_ID);
        try (MockedStatic<AGConnectServicesConfig> mocked = mockStatic(AGConnectServicesConfig.class)) {
            Mockito.when(AGConnectServicesConfig.fromContext(mApplication)).thenReturn(config);
            String appId = sdkHandler.appId();
            Assert.assertNotNull(appId);
        }
    }

    @Test
    public void testIsAvailable_Returns_False() {
        Mockito.when(config.getString(APP_ID_KEY)).thenThrow(new RuntimeException("Something Went Wrong"));
        try (MockedStatic<AGConnectServicesConfig> mocked = mockStatic(AGConnectServicesConfig.class)) {
            Mockito.when(AGConnectServicesConfig.fromContext(mApplication)).thenReturn(config);
            Assert.assertFalse(sdkHandler.isAvailable());
        }
    }

    @Test
    public void testIsAvailable_Returns_True() {
        AGConnectServicesConfig config = Mockito.mock(AGConnectServicesConfig.class);
        Mockito.when(config.getString(APP_ID_KEY)).thenReturn(HMS_APP_ID);
        try (MockedStatic<AGConnectServicesConfig> mocked = mockStatic(AGConnectServicesConfig.class)) {
            Mockito.when(AGConnectServicesConfig.fromContext(mApplication)).thenReturn(config);
            Assert.assertTrue(sdkHandler.isAvailable());
        }
    }

    @Test
    public void testIsSupported_Returns_False() {
        huaweiApi = Mockito.mock(HuaweiApiAvailability.class);
        Mockito.when(huaweiApi.isHuaweiMobileNoticeAvailable(mApplication)).thenReturn(1);
        try (MockedStatic<HuaweiApiAvailability> mocked = mockStatic(HuaweiApiAvailability.class)) {
            Mockito.when(HuaweiApiAvailability.getInstance()).thenReturn(huaweiApi);
            Assert.assertFalse(sdkHandler.isSupported());
        }
    }

    @Test
    public void testIsSupported_Returns_False_Exception() {
        huaweiApi = Mockito.mock(HuaweiApiAvailability.class);
        Mockito.when(huaweiApi.isHuaweiMobileNoticeAvailable(mApplication))
                .thenThrow(new RuntimeException("Something went wrong"));
        try (MockedStatic<HuaweiApiAvailability> mocked = mockStatic(HuaweiApiAvailability.class)) {
            Mockito.when(HuaweiApiAvailability.getInstance()).thenReturn(huaweiApi);
            Assert.assertFalse(sdkHandler.isSupported());
        }
    }

    @Test
    public void testIsSupported_Returns_True() {
        huaweiApi = Mockito.mock(HuaweiApiAvailability.class);
        Mockito.when(huaweiApi.isHuaweiMobileNoticeAvailable(mApplication)).thenReturn(0);
        try (MockedStatic<HuaweiApiAvailability> mocked = mockStatic(HuaweiApiAvailability.class)) {
            Mockito.when(HuaweiApiAvailability.getInstance()).thenReturn(huaweiApi);
            Assert.assertTrue(sdkHandler.isSupported());
        }
    }

    @Test
    public void testNewToken_Exception() {
        try {
            Mockito.when(config.getString(APP_ID_KEY)).thenReturn(HMS_APP_ID);
            Mockito.when(instance.getToken(HMS_APP_ID, HCM_SCOPE))
                    .thenThrow(new RuntimeException("Something went wrong"));
            try (MockedStatic<HmsInstanceId> mocked = mockStatic(HmsInstanceId.class)) {
                Mockito.when(HmsInstanceId.getInstance(mApplication)).thenReturn(instance);
                try (MockedStatic<AGConnectServicesConfig> mocked1 = mockStatic(AGConnectServicesConfig.class)) {
                    Mockito.when(AGConnectServicesConfig.fromContext(mApplication)).thenReturn(config);
                    String token = sdkHandler.onNewToken();
                    Assert.assertNull(token);
                }
            }
        } catch (Exception e) {

        }
    }

    @Test
    public void testNewToken_Invalid_AppId() {
        Mockito.when(config.getString(APP_ID_KEY)).thenReturn(null);
        try (MockedStatic<AGConnectServicesConfig> mocked1 = mockStatic(AGConnectServicesConfig.class)) {
            Mockito.when(AGConnectServicesConfig.fromContext(mApplication)).thenReturn(config);
            String token = sdkHandler.onNewToken();
            Assert.assertNull(token);
        }
    }

    @Test
    public void testNewToken_Success() {
        try {
            Mockito.when(config.getString(APP_ID_KEY)).thenReturn(HMS_APP_ID);
            Mockito.when(instance.getToken(HMS_APP_ID, HCM_SCOPE)).thenReturn(HMS_TOKEN);

            try (MockedStatic<HmsInstanceId> mocked = mockStatic(HmsInstanceId.class)) {
                Mockito.when(HmsInstanceId.getInstance(mApplication)).thenReturn(instance);
                try (MockedStatic<AGConnectServicesConfig> mocked1 = mockStatic(AGConnectServicesConfig.class)) {
                    Mockito.when(AGConnectServicesConfig.fromContext(mApplication)).thenReturn(config);
                    String token = sdkHandler.onNewToken();
                    Assert.assertEquals(token, HMS_TOKEN);
                }
            }
        } catch (Exception e) {
            //do nothing
        }
    }
}