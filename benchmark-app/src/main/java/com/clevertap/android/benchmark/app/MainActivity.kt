package com.clevertap.android.benchmark.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.trace
import com.clevertap.android.benchmark.app.ui.theme.ClevertapandroidsdkTheme
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.CleverTapInstanceConfig

/**
 * Empty activity showing some text used for performing Macro Benchmark tests.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClevertapandroidsdkTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }

        CleverTapAPI.setDebugLevel(VERBOSE)

        /**
         * "bm(benchmark method)" is being passed from macro benchmark tests to tell the Activity which method to benchmark
         */
        when(intent?.extras?.getString("bm") ?: ""){
            "initCT" -> trace("init CT") {
//            val measureNanoTime = measureNanoTime {
//                cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this)
                val config = CleverTapInstanceConfig.createInstance(this, "TEST-R78-ZZK-955Z", "TEST-311-ba2")
                val cleverTapDefaultInstance = CleverTapAPI.instanceWithConfig(this, config)
//            }
//            Logger.v("nano time ct init = $measureNanoTime")
            }

            "initCTD" -> trace("init CTD") {
//            val measureNanoTime = measureNanoTime {
                val cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this)
//            }
//            Logger.v("nano time ct init = $measureNanoTime")
            }
            else -> Log.d("MainActivity","no method specified by benchmark module")
        }



    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClevertapandroidsdkTheme {
        Greeting("Android")
    }
}