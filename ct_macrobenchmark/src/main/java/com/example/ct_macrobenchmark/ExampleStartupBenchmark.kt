package com.example.ct_macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.CompilationMode.Full
import androidx.benchmark.macro.CompilationMode.None
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupMode.COLD
import androidx.benchmark.macro.StartupMode.WARM
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
class ExampleStartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.clevertap.demo",
        metrics = listOf(TraceSectionMetric("init CT")),
        compilationMode = Full(),
//        compilationMode = None(),
        iterations = 10,
        startupMode = COLD
    ) {
        pressHome()
        startActivityAndWait()
        //device.wait(Until.hasObject(By.text("Fully Drawn")), 3_000)
    }
}