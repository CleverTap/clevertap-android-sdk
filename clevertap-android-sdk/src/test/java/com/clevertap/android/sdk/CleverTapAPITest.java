package com.clevertap.android.sdk;

import android.app.Activity;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(RobolectricTestRunner.class)
public class CleverTapAPITest extends BaseTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testActivity() {
        Activity activity = mock(Activity.class);
        Bundle bundle = new Bundle();
        //create
        activity.onCreate(bundle, null);
        Mockito.verify(cleverTapAPI).onActivityCreated(activity, null);
    }

}
