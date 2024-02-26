import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Constraints.Builder
import androidx.work.NetworkType.CONNECTED
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpWorker
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.*
import org.junit.runner.*
import java.util.concurrent.TimeUnit.MINUTES

@RunWith(AndroidJUnit4::class)
class CTPushAmpWorkerInstrumentationTest {
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun testWork(){
        CleverTapAPI.setDebugLevel(VERBOSE)
        val myContext = ApplicationProvider.getApplicationContext<Context>()

        val constraints = Builder()
            .setRequiredNetworkType(CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()

        val request =
            PeriodicWorkRequest.Builder(CTPushAmpWorker::class.java, 15, MINUTES, 5, MINUTES)
                .setConstraints(constraints).build()

        val workManager = WorkManager.getInstance(myContext)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(myContext)!!
        // Enqueue
        workManager.enqueue(request).result.get()
        testDriver.setAllConstraintsMet(request.id)
        testDriver.setPeriodDelayMet(request.id)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        println("workInfo = $workInfo")
        // Assert
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))
    }
}