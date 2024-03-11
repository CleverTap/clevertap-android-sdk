package com.clevertap.benchmark

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import org.junit.*
import org.junit.runner.*

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun log() {
        //CleverTapAPI.setDebugLevel(VERBOSE)
        benchmarkRule.measureRepeated {
            val myContext = ApplicationProvider.getApplicationContext<Context>()
            //CleverTapAPI.setDebugLevel(VERBOSE)
            /*setMetaData("CLEVERTAP_ACCOUNT_ID", java.util.UUID.randomUUID().toString(),myContext);
            CleverTapAPI.getDefaultInstance(myContext)*/
            val config = CleverTapInstanceConfig.createInstance(myContext, java.util.UUID.randomUUID().toString(), java.util.UUID.randomUUID().toString())
            val cleverTapDefaultInstance = CleverTapAPI.instanceWithConfig(myContext, config)
            //cleverTapDefaultInstance.pushEvent("test")
        }
    }

}