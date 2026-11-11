package com.clevertap.android.sdk.pushnotification.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.clevertap.android.sdk.Constants.FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.isPackageAndOsTargetsAbove
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * SDK-6153: [CTWorkManager] gets WorkManager through [CTWorkManagerAccessor] and must never let a
 * WorkManager failure escape. Example: an app pins WorkManager 2.9.1, where a Kotlin call would
 * throw NoClassDefFoundError for WorkManager$Companion.
 */
@RunWith(RobolectricTestRunner::class)
class CTWorkManagerTest : BaseTestCase() {

    @Before
    override fun setUp() {
        super.setUp()
        // Make init() take the scheduling path: Android 8.0+ target and the main process.
        mockkStatic("com.clevertap.android.sdk.CTXtensions")
        every { any<Context>().isPackageAndOsTargetsAbove(any()) } returns true
        mockkStatic(Utils::class)
        every { Utils.isMainProcess(any(), any()) } returns true
        mockkStatic(CTWorkManagerAccessor::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init enqueues the impression flush work through the accessor`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        every { CTWorkManagerAccessor.getInstance(any()) } returns workManager

        CTWorkManager(appCtx, cleverTapInstanceConfig).init()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `init does not throw when WorkManager is missing the Kotlin companion class`() {
        every { CTWorkManagerAccessor.getInstance(any()) } throws
            NoClassDefFoundError("androidx/work/WorkManager\$Companion")

        CTWorkManager(appCtx, cleverTapInstanceConfig).init()
    }

    @Test
    fun `init does not throw when WorkManager fails to start with NoSuchMethodError`() {
        every { CTWorkManagerAccessor.getInstance(any()) } throws
            NoSuchMethodError("No virtual method forNamespace(Ljava/lang/String;)")

        CTWorkManager(appCtx, cleverTapInstanceConfig).init()
    }
}
