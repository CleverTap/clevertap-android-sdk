package com.clevertap.android.sdk;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

//@RunWith(MockitoJUnitRunner.class)
@RunWith(RobolectricTestRunner.class)
public class CleverTapAPITest extends BaseTestCase {

    private CleverTapAPI cleverTapAPI;

    private TestApplication application;

    @Before
    public void setUp() throws Exception {
        application = TestApplication.getApplication();
        cleverTapAPI = CleverTapAPI.getDefaultInstance(application);
    }

    @Test
    public void test() {
        Assert.assertEquals(1, 1);
    }
}
