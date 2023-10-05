package com.clevertap.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.demo.ui.theme.ClevertapandroidsdkTheme
import com.google.android.gms.instantapps.InstantApps
class InstantDemoActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClevertapandroidsdkTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }

    fun sendEvent() {
        CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.VERBOSE)
        CleverTapAPI.getDefaultInstance(this)?.pushEvent("Instant app")
    }

    fun upgradeApp() {
        val postInstall = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setPackage(BuildConfig.APPLICATION_ID)

        // The request code is passed to startActivityForResult().
        InstantApps.showInstallPrompt(this, postInstall, REQUEST_CODE, null)
    }
}

@Composable
fun Ui(
    sendEvent: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Button(onClick = {
            sendEvent.invoke()
        }) {
            Text(
                text = "Send event"
            )
        }

        Button(onClick = {
        }) {
            Text(
                text = "Upgrade"
            )
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

@Preview(showBackground = true)
@Composable
fun UiPreview() {
    ClevertapandroidsdkTheme {
        Ui({})
    }
}