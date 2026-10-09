package com.clevertap.android.pushtemplates.styles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeProgressApiTest {

    @Test
    fun `methods are found only when the classpath has androidx core 1_17 ProgressStyle`() {
        // Holds whichever androidx.core the test classpath ships (today < 1.17.0, so null).
        val coreHasProgressStyle = runCatching {
            Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle")
        }.isSuccess

        if (coreHasProgressStyle) assertNotNull(NativeProgressApi.methods) else assertNull(NativeProgressApi.methods)
    }

    @Test
    fun `a failed lookup is cached as null and never retried`() {
        var calls = 0
        val cached = NativeProgressApi.cachedLookup {
            calls++
            throw ClassNotFoundException("androidx.core.app.NotificationCompat\$ProgressStyle")
        }

        assertNull(cached.value) // the failure is swallowed, not thrown
        assertNull(cached.value)
        assertEquals(1, calls)
    }
}
