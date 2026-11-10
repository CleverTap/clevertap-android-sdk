package com.clevertap.android.sdk.pushnotification

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.pushnotification.preview.PushPreviewStore
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.pushnotification.work.CTWorkManagerAccessor
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * SDK-6153: Push Amp scheduling in [PushProviders] gets WorkManager through
 * [CTWorkManagerAccessor] and catches Throwable.
 *
 * Why Throwable matters: WorkManager failures here are Errors (NoClassDefFoundError when an app pins
 * WorkManager 2.9.1 and the call needs WorkManager$Companion; NoSuchMethodError forNamespace on
 * Google's relabelled Android 13 test image). The executor Task catches only Exception, so an
 * escaped Error kills the app. [MockCTExecutors] runs the real Task synchronously, so an escaped
 * Error fails these tests.
 */
@RunWith(RobolectricTestRunner::class)
class PushProvidersPushAmpTest : BaseTestCase() {

    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    override fun setUp() {
        super.setUp()
        cleverTapInstanceConfig.isBackgroundSync = true
        mockkStatic(CTExecutorFactory::class)
        every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
        mockkStatic(CTWorkManagerAccessor::class)
        every { CTWorkManagerAccessor.getInstance(any()) } returns workManager
    }

    @After
    fun tearDown() {
        unmockkStatic(CTExecutorFactory::class)
        unmockkStatic(CTWorkManagerAccessor::class)
    }

    /**
     * The constructor already runs Push Amp set-up (initPushAmp), which schedules work with the
     * default ping frequency. Reset the stored work name and the recorded calls afterwards, so
     * each test sees only the call it makes.
     */
    private fun buildSut(): PushProviders {
        val sut = PushProviders(
            appCtx, cleverTapInstanceConfig, mockk<BaseDatabaseManager>(relaxed = true),
            mockk<ValidationResultStack>(relaxed = true), mockk<AnalyticsManager>(relaxed = true),
            mockk<CTWorkManager>(relaxed = true), mockk<Clock>(relaxed = true),
            mockk<PushPreviewStore>(relaxed = true)
        )
        StorageHelper.putString(appCtx, PF_WORK_ID, "")
        clearMocks(workManager, answers = false)
        return sut
    }

    private fun storedWorkName(): String? = StorageHelper.getString(appCtx, PF_WORK_ID, "")

    @Test
    fun `a new ping frequency enqueues the periodic work and stores its name`() {
        buildSut().updatePingFrequencyIfNeeded(appCtx, 15)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                cleverTapInstanceConfig.accountId,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>()
            )
        }
        assertEquals(cleverTapInstanceConfig.accountId, storedWorkName())
    }

    @Test
    fun `scheduling does not throw when WorkManager is missing the Kotlin companion class`() {
        val sut = buildSut()
        every { CTWorkManagerAccessor.getInstance(any()) } throws
            NoClassDefFoundError("androidx/work/WorkManager\$Companion")

        sut.updatePingFrequencyIfNeeded(appCtx, 15)

        assertEquals("", storedWorkName())
    }

    @Test
    fun `scheduling does not throw when WorkManager fails to start with NoSuchMethodError`() {
        val sut = buildSut()
        every { CTWorkManagerAccessor.getInstance(any()) } throws
            NoSuchMethodError("No virtual method forNamespace(Ljava/lang/String;)")

        sut.updatePingFrequencyIfNeeded(appCtx, 15)

        assertEquals("", storedWorkName())
    }

    @Test
    fun `a zero ping frequency cancels the existing work and clears its name`() {
        val sut = buildSut()
        StorageHelper.putString(appCtx, PF_WORK_ID, "existing-work")

        sut.updatePingFrequencyIfNeeded(appCtx, 0)

        verify(exactly = 1) { workManager.cancelUniqueWork("existing-work") }
        assertEquals("", storedWorkName())
    }

    @Test
    fun `cancelling does not throw when WorkManager fails and keeps the work name`() {
        val sut = buildSut()
        StorageHelper.putString(appCtx, PF_WORK_ID, "existing-work")
        every { CTWorkManagerAccessor.getInstance(any()) } throws
            NoSuchMethodError("No virtual method forNamespace(Ljava/lang/String;)")

        sut.updatePingFrequencyIfNeeded(appCtx, 0)

        assertEquals("existing-work", storedWorkName())
    }

    private companion object {
        // Mirrors PushProviders.PF_WORK_ID, which is private.
        const val PF_WORK_ID = "pfworkid"
    }
}
