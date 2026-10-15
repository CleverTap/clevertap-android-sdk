package com.clevertap.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONException
import org.json.JSONObject

/**
 * Renders a push payload typed straight into the app, so a template can be checked on a device
 * without setting up a campaign on the dashboard.
 *
 * Debug-only, like [PushTestReceiver]: it renders whatever payload it is given, deep links included,
 * which is not something to ship. [HomeScreenFragment] launches it by name and simply leaves the
 * entry out when the class is absent.
 */
class LocalPushActivity : AppCompatActivity() {

    private companion object {
        const val PRESET_DIR = "local_push"
        const val PROMPT = "Select a preset..."
    }

    private lateinit var payloadInput: EditText
    private lateinit var presetSpinner: Spinner

    /** Asset file name per spinner position; the prompt at position 0 has none. */
    private var presetFiles: List<String> = emptyList()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) toast("Notifications are blocked - the push will render but stay hidden")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_push)
        title = "Local Push (JSON)"

        payloadInput = findViewById(R.id.payloadInput)
        presetSpinner = findViewById(R.id.presetSpinner)

        setupPresets()
        findViewById<Button>(R.id.renderButton).setOnClickListener { renderPush() }
        findViewById<Button>(R.id.formatButton).setOnClickListener { formatPayload() }

        requestNotificationPermissionIfNeeded()
    }

    private fun setupPresets() {
        presetFiles = runCatching { assets.list(PRESET_DIR)?.sorted() }.getOrNull().orEmpty()

        val labels = listOf(PROMPT) + presetFiles.map(::labelFor)
        presetSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) loadPreset(presetFiles[position - 1])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    /** "02_zero_bezel.json" reads better in a spinner as "Zero bezel". */
    private fun labelFor(fileName: String): String = fileName
        .removeSuffix(".json")
        .substringAfter('_')
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

    private fun loadPreset(fileName: String) {
        runCatching {
            assets.open("$PRESET_DIR/$fileName").bufferedReader().use { it.readText() }
        }.onSuccess {
            payloadInput.setText(it.trim())
        }.onFailure {
            toast("Could not read $fileName")
        }
    }

    private fun renderPush() {
        val payload = payloadInput.text.toString().trim()
        if (payload.isEmpty()) {
            toast("Paste a payload first")
            return
        }

        val extras = try {
            LocalPushSender.bundleFromJson(payload)
        } catch (e: JSONException) {
            toast("Invalid JSON: ${e.message}")
            return
        }

        LocalPushSender.render(this, extras)
        toast("Rendering...")
    }

    /** Re-indents what is in the box, which also tells you at a glance whether it parses. */
    private fun formatPayload() {
        val payload = payloadInput.text.toString().trim()
        if (payload.isEmpty()) return

        try {
            payloadInput.setText(JSONObject(payload).toString(2))
        } catch (e: JSONException) {
            toast("Invalid JSON: ${e.message}")
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
