package com.clevertap.demo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.pushnotification.fcm.CTFcmMessageHandler
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

/**
 * QA tool: paste a full Live Update push payload (wrapper + `data`, as in
 * `sample/live-updates-test/QA_PAYLOADS.md`) and send it to the SDK.
 *
 * The payload goes through the same path as a real FCM push: it is turned into a [RemoteMessage]
 * (every value a string, `data` as a JSON string, like FCM delivers it) and handed to
 * [CTFcmMessageHandler.createNotification], exactly as [MyFcmMessageListenerService] does. So the
 * Live Update routing (`wzrk_la`), the `data` surfacing, in-place updates by `wzrk_activityId` and
 * the start/update/end lifecycle all run as in production.
 *
 * Timer/countdown payloads need a real time in `pt_when`, so `{{now}}`, `{{now+600}}` or `{{now-300}}`
 * (seconds from now) are replaced with the current epoch milliseconds before sending. This is a QA
 * convenience of this screen only; a real backend sends the number.
 *
 * A real push carries `wzrk_acct_id`; when the pasted payload has none (or a `<placeholder>`), this
 * app's own account id is filled in so QA does not have to edit every payload.
 *
 * Can also be started from a computer, for example:
 * `adb shell am start -n com.clevertap.demo/.LiveUpdatePayloadActivity --es payload '<json>' --ez send true`
 */
class LiveUpdatePayloadActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_SEND = "send"
        private const val KEY_ACCOUNT_ID = "wzrk_acct_id"
        private const val KEY_CHANNEL_ID = "wzrk_cid"
        private val NOW_TOKEN = Regex("""\{\{now([+-]\d+)?\}\}""")

        /** Replaces each `{{now±seconds}}` with that time in epoch milliseconds. */
        private fun fillTimes(json: String, nowMs: Long = System.currentTimeMillis()): String =
            NOW_TOKEN.replace(json) { m -> (nowMs + (m.groupValues[1].ifEmpty { "0" }.toLong() * 1000)).toString() }

        /**
         * Turns a pasted payload into FCM data: FCM delivers a flat map of strings, so nested
         * objects/arrays (the Live Update `data` object, `wzrk_acts`, …) become JSON strings and
         * numbers/booleans become their text. JSON null is left out.
         */
        private fun toFcmData(json: String): MutableMap<String, String> {
            val obj = JSONObject(json.trim())
            val out = LinkedHashMap<String, String>()
            for (key in obj.keys()) {
                val value = obj.get(key)
                if (value != JSONObject.NULL) out[key] = value.toString()
            }
            return out
        }
    }

    private val fcmHandler = CTFcmMessageHandler()
    private var payload by mutableStateOf("")
    private var status by mutableStateOf("")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh start: a rotation must not send the adb payload a second time.
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Live Update payload (QA)") },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    ) { paddingValues ->
                        PayloadScreen(Modifier.padding(paddingValues))
                    }
                }
            }
        }
    }

    // Started again while open (for example a second adb command): take the new payload too.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val extra = intent.getStringExtra(EXTRA_PAYLOAD) ?: return
        payload = extra
        status = if (intent.getBooleanExtra(EXTRA_SEND, false)) send(extra) else ""
    }

    @Composable
    private fun PayloadScreen(modifier: Modifier) {
        val clipboard = LocalClipboardManager.current

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Paste a full payload (wrapper + data) from QA_PAYLOADS.md and tap Send. It runs " +
                    "like a real FCM push. Same wzrk_activityId = the same card updates. Do not " +
                    "add a wzrk_pid you already sent: the SDK drops it as a duplicate.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 12,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                label = { Text("Payload JSON") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clipboard.getText()?.let { payload = it.text } }) { Text("Paste") }
                OutlinedButton(onClick = { payload = ""; status = "" }) { Text("Clear") }
                Button(onClick = { status = send(payload) }) { Text("Send") }
            }
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodyMedium)
        }
    }

    /** Sends [payload] through the FCM path and returns a short result for the screen. */
    private fun send(payload: String): String {
        val data = try {
            toFcmData(fillTimes(payload))
        } catch (t: Throwable) {
            return "Not sent: the JSON is not valid (${t.message})"
        }
        // Without an account id the SDK finds no CleverTap instance and drops the push.
        val accountId = data[KEY_ACCOUNT_ID]
        if (accountId.isNullOrBlank() || accountId.startsWith("<")) {
            CleverTapAPI.getDefaultInstance(applicationContext)?.accountId?.let { data[KEY_ACCOUNT_ID] = it }
        }
        data[KEY_CHANNEL_ID]?.let { ensureChannel(applicationContext, it) }

        val message = RemoteMessage.Builder("qa@fcm.googleapis.com").setData(data).build()
        return if (fcmHandler.createNotification(applicationContext, message)) {
            "Sent to the SDK (${data.size} keys). Check the notification shade."
        } else {
            "Not sent: the SDK did not treat it as a CleverTap push (is \"wzrk_pn\": \"true\" there?)"
        }
    }

    /** The payload's channel must exist or Android drops the notification; create it like the demos do. */
    private fun ensureChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Live Updates", NotificationManager.IMPORTANCE_HIGH))
        }
    }
}
