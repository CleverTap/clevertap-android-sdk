package com.example.ct_macrobenchmark

import androidx.benchmark.macro.CompilationMode.Full
import androidx.benchmark.macro.CompilationMode.None
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode.COLD
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.*

/**
 * This is an example startup benchmark.
 *
 * It navigates to the device's home screen, and launches the default activity.
 *
 * Before running this benchmark:
 * 1) switch your app's active build variant in the Studio (affects Studio runs only)
 * 2) add `<profileable android:shell="true" />` to your app's manifest, within the `<application>` tag
 *
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance.
 */
@RunWith(AndroidJUnit4::class)
class CTInstanceCreationBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Benchmarking function to measure the initialization of CleverTap instance with configuration
     * under compilation mode 'Full'.
     *
     * This function uses the BenchmarkRule to measure the repeated execution of the initialization process.
     * It benchmarks the initialization process by repeatedly launching the specified package name,
     * measuring the time taken to initialize the CleverTap instance with configuration under compilation mode 'Full'.
     *
     * packageName :  The package name of the Android application to be benchmarked.
     * metrics : A list of metrics to be measured during benchmarking, such as TraceSectionMetric("init CT").
     * compilationMode : The compilation mode for benchmarking. In this case, it's set to Full().
     *                        (To disable compilation, comment out 'compilationMode = Full()' and uncomment 'compilationMode = None()'.)
     * iterations : The number of iterations for the benchmarking process.
     * startupMode : The startup mode for the benchmarking process, such as COLD or WARM.
     */
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun benchmarkInstanceWithConfigCompilationModeFull() = benchmarkRule.measureRepeated(
        packageName = "com.clevertap.android.benchmark.app",
        metrics = listOf(TraceSectionMetric("init CT")),
        compilationMode = Full(),
//        compilationMode = None(),
        iterations = 10,
        startupMode = COLD
    ) {
        //A function to simulate pressing the Home button on the device.
        pressHome()
        //A function to start an activity with specified extras bm(benchmark method) and wait for it to complete.
        startActivityAndWait {
            it.putExtra("bm", "initCT")
        }
//        device.wait(Until.hasObject(By.text("Fully Drawn")), 3_000)
    }

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun benchmarkDefaultInstanceCompilationModeFull() = benchmarkRule.measureRepeated(
        packageName = "com.clevertap.android.benchmark.app",
        metrics = listOf(TraceSectionMetric("init CTD")),
        compilationMode = Full(),
//        compilationMode = None(),
        iterations = 10,
        startupMode = COLD
    ) {
        pressHome()

        startActivityAndWait {
            it.putExtra("bm", "initCTD")
        }
    }

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun benchmarkInstanceWithConfigCompilationModeNone() = benchmarkRule.measureRepeated(
        packageName = "com.clevertap.android.benchmark.app",
        metrics = listOf(TraceSectionMetric("init CT")),
        compilationMode = None(),
        iterations = 10,
        startupMode = COLD
    ) {
        pressHome()
        startActivityAndWait {
            it.putExtra("bm", "initCT")
        }
    }

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun benchmarkDefaultInstanceCompilationModeNone() = benchmarkRule.measureRepeated(
        packageName = "com.clevertap.android.benchmark.app",
        metrics = listOf(TraceSectionMetric("init CTD")),
        compilationMode = None(),
        iterations = 10,
        startupMode = COLD
    ) {
        pressHome()

        startActivityAndWait {
            it.putExtra("bm", "initCTD")
        }
    }
}