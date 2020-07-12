package com.clevertap.android.sdk.featureFlags;

import com.clevertap.android.sdk.BaseTestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class FeatureFlagTest extends BaseTestCase {
    private static final String TAG = "FeatureFlagTest";

    @Before
    public void setUp() throws Exception {
        super.setUp();
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