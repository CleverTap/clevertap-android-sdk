package com.clevertap.android.shared.test;

import static com.clevertap.android.shared.test.Constant.ACC_ID;
import static com.clevertap.android.shared.test.Constant.ACC_TOKEN;
import static org.mockito.Mockito.*;

import android.os.Build;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.annotation.Config;

@Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P},
        application = TestApplication.class
)
@RunWith(AndroidJUnit4.class)
public abstract class BaseTestCase {

    protected TestApplication application;

    protected BaseCTApiListener baseCTApiListener;

    protected CleverTapAPI cleverTapAPI;

    protected CleverTapInstanceConfig cleverTapInstanceConfig;

    public static boolean areEqual(Bundle expected, Bundle actual) {
        if (expected == null) {
            return actual == null;
        }

        if (expected.size() != actual.size()) {
            return false;
        }

        for (String key : expected.keySet()) {
            if (!actual.containsKey(key)) {
                return false;
            }

            Object expectedValue = expected.get(key);
            Object actualValue = actual.get(key);

            if (expectedValue == null) {
                if (actualValue != null) {
                    return false;
                }

                continue;
            }

            if (expectedValue instanceof Bundle && actualValue instanceof Bundle) {
                if (!areEqual((Bundle) expectedValue, (Bundle) actualValue)) {
                    return false;
                }

                continue;
            }

            if (!expectedValue.equals(actualValue)) {
                return false;
            }
        }

        return true;
    }

    public static void assertBundlesEquals(String message, Bundle expected, Bundle actual) {
        if (!areEqual(expected, actual)) {
            Assert.fail(message + " <" + expected.toString() + "> is not equal to <" + actual.toString() + ">");
        }
    }

    public static void assertBundlesEquals(Bundle expected, Bundle actual) {
        assertBundlesEquals(null, expected, actual);
    }

    public TestApplication getApplication() {
        return TestApplication.getApplication();
    }

    @Before
    public void setUp() {
        application = TestApplication.getApplication();
        cleverTapAPI = Mockito.mock(CleverTapAPI.class);

        cleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(application, ACC_ID, ACC_TOKEN);

        baseCTApiListener = Mockito.mock(BaseCTApiListener.class);
        when(baseCTApiListener.context()).thenReturn(application);
        when(baseCTApiListener.config()).thenReturn(cleverTapInstanceConfig);
    }

}
