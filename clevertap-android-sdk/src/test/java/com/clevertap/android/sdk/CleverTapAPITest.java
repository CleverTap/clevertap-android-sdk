package com.clevertap.android.sdk;

import android.app.Activity;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class CleverTapAPITest extends BaseTestCase {

    private CleverTapAPI cleverTapAPI;

    private TestApplication application;

    @Before
    public void setUp() throws Exception {
        application = TestApplication.getApplication();
        cleverTapAPI = mock(CleverTapAPI.class);
    }

    @Test
    public void testActivity() {
        Activity activity = mock(Activity.class);
        Bundle bundle = new Bundle();
        //create
        activity.onCreate(bundle, null);
        verify(cleverTapAPI).onActivityCreated(activity, null);
    }

}
