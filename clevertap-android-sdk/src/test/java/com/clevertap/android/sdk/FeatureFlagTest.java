package com.clevertap.android.sdk;

import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.clevertap.android.sdk.Constant.ACC_ID;
import static com.clevertap.android.sdk.Constant.ACC_TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class FeatureFlagTest extends BaseTestCase {
    private static final String TAG = "FeatureFlagTest";
    private CleverTapAPI cleverTapAPI;

    private TestApplication application;
    private CleverTapInstanceConfig cleverTapInstanceConfig;

    @Before
    public void setUp() throws Exception {
        application = TestApplication.getApplication();
        cleverTapAPI = mock(CleverTapAPI.class);
        cleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(application, ACC_ID, ACC_TOKEN);
    }

    @Test
    public void testFetch() {
        when(cleverTapAPI.featureFlag()).thenReturn(new CTFeatureFlagsController(application, "12121", cleverTapInstanceConfig, cleverTapAPI));
        cleverTapAPI.featureFlag().fetchFeatureFlags();
        verify(cleverTapAPI).fetchFeatureFlags();
    }

    @Test
    public void testGet() {
        CTFeatureFlagsController ctFeatureFlagsController = mock(CTFeatureFlagsController.class);
        when(cleverTapAPI.featureFlag()).thenReturn(ctFeatureFlagsController);
        when(ctFeatureFlagsController.get("isFeatureA", true)).thenReturn(false);
        Assert.assertFalse(cleverTapAPI.featureFlag().get("isFeatureA", true));
    }

}