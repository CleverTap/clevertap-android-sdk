import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SDK-6153: the SDK schedules its work through CTWorkManagerAccessor (a Java call). These tests run
 * on a real device with a real test WorkManager and read back what the SDK actually scheduled, so
 * they check the real constraints and intervals, not mocked calls.
 *
 * The failure cases (NoClassDefFoundError for WorkManager$Companion, NoSuchMethodError forNamespace)
 * cannot happen on a real device with WorkManager 2.10+, so they are covered by the unit tests
 * PushProvidersPushAmpTest and CTWorkManagerTest.
 */
@RunWith(AndroidJUnit4::class)
class WorkManagerAccessorInstrumentationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CleverTapAPI.setDebugLevel(VERBOSE)
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .setExecutor(SynchronousExecutor())
            .build()
        // Fresh, empty WorkManager for every test.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        // Push Amp keeps its work name and ping frequency in shared prefs across runs. Clear them
        // so the SDK schedules from scratch instead of trusting work from an older run.
        StorageHelper.remove(context, PF_WORK_ID)
        StorageHelper.remove(context, PING_FREQUENCY)
    }

    @Test
    fun impressionFlushWorkIsScheduledWithNetworkAndCharging() {
        val config = CleverTapInstanceConfig.createInstance(context, "TEST-6153", "TEST")

        CTWorkManager(contextTargeting(35), config).init()

        val info = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME).get().single()
        // Charging is not reported as met by the test driver, so the work waits in ENQUEUED.
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
        assertTrue(info.constraints.requiresCharging())
    }

    @Test
    fun pushAmpWorkIsScheduledThenRescheduledThenCancelled() {
        val accountId = "TEST-6153-" + UUID.randomUUID().toString().take(8)
        val config = CleverTapInstanceConfig.createInstance(context, accountId, "TEST").apply {
            isBackgroundSync = true
        }
        // Creating the instance runs Push Amp set-up on the SDK's background thread with the
        // default ping frequency (240 minutes).
        val pushProviders = CleverTapAPI.instanceWithConfig(context, config)!!.coreState!!.pushProviders

        // 1. Scheduled with the default frequency, named after the account.
        val first = waitForWork(accountId) { it.periodicityInfo?.repeatIntervalMillis == minutes(240) }
        assertEquals(WorkInfo.State.ENQUEUED, first.state)
        assertEquals(NetworkType.CONNECTED, first.constraints.requiredNetworkType)
        assertTrue(first.constraints.requiresBatteryNotLow())
        assertEquals(accountId, StorageHelper.getString(context, PF_WORK_ID, ""))

        // 2. A new frequency from the server replaces it (ExistingPeriodicWorkPolicy.UPDATE).
        pushProviders.updatePingFrequencyIfNeeded(context, 60)
        waitForWork(accountId) { it.periodicityInfo?.repeatIntervalMillis == minutes(60) }

        // 3. Frequency 0 cancels it and clears the stored name.
        pushProviders.updatePingFrequencyIfNeeded(context, 0)
        waitForWork(accountId) { it.state == WorkInfo.State.CANCELLED }
        assertEquals("", StorageHelper.getString(context, PF_WORK_ID, ""))
    }

    /** Push Amp scheduling runs on the SDK's background thread, so poll for up to 10 seconds. */
    private fun waitForWork(name: String, matches: (WorkInfo) -> Boolean): WorkInfo {
        val deadline = System.currentTimeMillis() + 10_000
        var last: WorkInfo? = null
        while (System.currentTimeMillis() < deadline) {
            last = WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get().lastOrNull()
            if (last != null && matches(last)) return last
            Thread.sleep(100)
        }
        assertNotNull("No work named $name was scheduled", last)
        throw AssertionError("Work $name did not reach the expected state in time. Last: $last")
    }

    /**
     * The library's test APK targets the library minSdk (23), and CTWorkManager.init() schedules
     * only when both the OS and the app target are above Android 8.0 (API 26). Real apps target
     * 35 or 36, so report that target here. Changing testOptions.targetSdk instead would also
     * change the Robolectric SDK for every unit test in the module.
     */
    private fun contextTargeting(targetSdk: Int): Context {
        val appContext = context.applicationContext
        val info = ApplicationInfo(appContext.applicationInfo).apply { targetSdkVersion = targetSdk }
        val app = object : ContextWrapper(appContext) {
            override fun getApplicationInfo(): ApplicationInfo = info
            override fun getApplicationContext(): Context = this
        }
        return object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = app
            override fun getApplicationInfo(): ApplicationInfo = info
        }
    }

    private fun minutes(value: Long) = TimeUnit.MINUTES.toMillis(value)

    private companion object {
        // Mirror PushProviders' private shared-prefs keys.
        const val PF_WORK_ID = "pfworkid"
        const val PING_FREQUENCY = "pf"
    }
}
