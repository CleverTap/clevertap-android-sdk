import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Constraints.Builder
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.clevertap.android.sdk.AnalyticsManagerBundler.notificationViewedJson
import com.clevertap.android.sdk.AnalyticsManagerBundler.wzrkBundleToJson
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.work.CTFlushPushImpressionsWork
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*

@RunWith(AndroidJUnit4::class)
class PIFlushWorkInstrumentationTest{
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

        val bundle = Bundle().apply {
            putString("wzrk_acct_id", "TEST-R78-ZZK-955Z")
            putString("nm", "Grab 'em on Myntra's Maxessorize Sale")
            putString("nt", "Ye dil ❤️️ maange more accessories?")
            putString("pr", "")
            putString("wzrk_pivot", "")
            putString("wzrk_sound", "true")
            putString("wzrk_cid", "BRTesting")
            putString("wzrk_clr", "#ed732d")
            putString("wzrk_nms", "Grab 'em on Myntra's Maxessorize Sale")
            putString("wzrk_pid", (10000_00000..99999_99999).random().toString())
            putString("wzrk_rnv", "true")
            putString("wzrk_ttl", "1627731067")
            putString("wzrk_push_amp", "false")
            putString("wzrk_bc", "")
            putString("wzrk_bi", "2")
            putString("wzrk_bp", "https://imgur.com/6DavQwg.jpg")
            putString("wzrk_dl", "")
            putString("wzrk_dt", "FIREBASE")
            putString("wzrk_id", "1627639375_20210730")
            putString("wzrk_pn", "true")
        }

        val myContext = ApplicationProvider.getApplicationContext<Context>()
        val defaultInstance = CleverTapAPI.getDefaultInstance(myContext)

        val config1 = CleverTapInstanceConfig.createInstance(myContext, "88R-R54-5Z6Z", "452-2bb");
        config1.isAnalyticsOnly = true
        val config2 = CleverTapInstanceConfig.createInstance(myContext, "TEST-46W-WWR-R85Z", "TEST-200-064");

        val ctInstance1 = CleverTapAPI.instanceWithConfig(myContext,config1);
        val ctInstance2 = CleverTapAPI.instanceWithConfig(myContext,config2);

        val bundle1 = (bundle.clone() as Bundle).apply {
            putString("wzrk_acct_id", "88R-R54-5Z6Z")
        }
        val bundle2 = (bundle.clone() as Bundle).apply {
            putString("wzrk_acct_id", "TEST-46W-WWR-R85Z")
        }

        listOf(Pair(defaultInstance,bundle),Pair(ctInstance1,bundle1), Pair(ctInstance2,bundle2)).map {
            val event = notificationViewedJson(it.second)
            Pair(it.first,event)
        }.forEach {
            it.first!!.coreState!!.databaseManager.queuePushNotificationViewedEventToDB(myContext, it.second)
        }

        val constraints = Builder()
            .setRequiredNetworkType(CONNECTED)
            .setRequiresCharging(true)
            .build()

        val request = OneTimeWorkRequestBuilder<CTFlushPushImpressionsWork>()
            .setConstraints(constraints)
            .build()


        val workManager = WorkManager.getInstance(myContext)
        val testDriver = WorkManagerTestInitHelper.getTestDriver()!!
        // Enqueue
        workManager.enqueue(request).result.get()
        // Tells the testing framework that all constraints are met.
        testDriver.setAllConstraintsMet(request.id)
        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()
        println("workInfo = $workInfo")
        // Assert
        assertThat(workInfo.state, `is`(WorkInfo.State.SUCCEEDED))
    }
}