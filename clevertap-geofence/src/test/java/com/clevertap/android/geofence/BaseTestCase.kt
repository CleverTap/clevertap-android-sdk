package com.clevertap.android.geofence

import android.app.Application
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.clearAllMocks
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [28], application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
abstract class BaseTestCase {

    protected lateinit var application: Application

    @Before
    open fun setUp() {
        application = TestApplication.application
    }

    @After
    open fun cleanUp() {
        clearAllMocks()
    }

    companion object {
        fun areEqual(expected: Bundle?, actual: Bundle?): Boolean {
            if (expected == null) {
                return actual == null
            }

            if (actual == null) {
                return false
            }

            if (expected.size() != actual.size()) {
                return false
            }

            for (key in expected.keySet()) {
                if (!actual.containsKey(key)) {
                    return false
                }

                val expectedValue = expected.get(key)
                val actualValue = actual.get(key)

                if (expectedValue == null) {
                    if (actualValue != null) {
                        return false
                    }

                    continue
                }

                if (expectedValue is Bundle && actualValue is Bundle) {
                    if (!areEqual(expectedValue, actualValue)) {
                        return false
                    }

                    continue
                }

                if (expectedValue != actualValue) {
                    return false
                }
            }

            return true
        }

        fun assertBundlesEquals(message: String?, expected: Bundle, actual: Bundle) {
            if (!areEqual(expected, actual)) {
                Assert.fail("$message <$expected> is not equal to <$actual>")
            }
        }

        fun assertBundlesEquals(expected: Bundle, actual: Bundle) {
            assertBundlesEquals(null, expected, actual)
        }
    }
}
