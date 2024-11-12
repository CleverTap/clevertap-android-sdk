package com.clevertap.demo

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.clevertap.android.pushtemplates.PushTemplateNotificationHandler
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.InboxMessageButtonListener
import com.clevertap.android.sdk.InboxMessageListener
import com.clevertap.android.sdk.SyncListener
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.interfaces.NotificationHandler
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.demo.ui.main.NotificationUtils
import com.github.anrwatchdog.ANRWatchDog
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener
import com.google.firebase.analytics.FirebaseAnalytics
import org.json.JSONObject
import kotlin.system.measureTimeMillis

class MyApplication : MultiDexApplication(), CTPushNotificationListener, ActivityLifecycleCallbacks,
    InboxMessageButtonListener, InboxMessageListener {

        companion object {
            private const val TAG = "MyApplication"

            var ctInstance: CleverTapAPI? = null
        }

    override fun onCreate() {
        ANRWatchDog().start()
        setupStrictMode()

        val preOnCreateTime = measureTimeMillis {
            cleverTapPreAppCreated() // setup pre 'super.onCreate()'
        }
        Log.i(TAG, "clevertap setup pre application onCreate took = $preOnCreateTime milliseconds")

        super.onCreate()

        val postOnCreateTime = measureTimeMillis {
            cleverTapPostAppCreated() // setup post 'super.onCreate()'
        }
        Log.i(TAG, "clevertap instance creation took = $postOnCreateTime milliseconds")
    }

    private fun cleverTapPreAppCreated() {
        CleverTapAPI.setDebugLevel(VERBOSE)
        //CleverTapAPI.changeXiaomiCredentials("your xiaomi app id","your xiaomi app key")
        //CleverTapAPI.enableXiaomiPushOn(XIAOMI_MIUI_DEVICES)
        TemplateRenderer.debugLevel = 3;
        CleverTapAPI.setNotificationHandler(PushTemplateNotificationHandler() as NotificationHandler)

        // this is for clevertap to start sending events => app launched => hence done in app create
        val measureTimeMillis = measureTimeMillis { ActivityLifecycleCallback.register(this) }
        Log.i(TAG, "Time taken to execute  ActivityLifecycleCallback.register = $measureTimeMillis milliseconds")

        // this is for the setup for canceling notifications
        registerActivityLifecycleCallbacks(this)
    }

    private fun cleverTapPostAppCreated() {
        ProviderInstaller.installIfNeededAsync(this, object : ProviderInstallListener {
            override fun onProviderInstalled() {}
            override fun onProviderInstallFailed(i: Int, intent: Intent?) {
                Log.i("ProviderInstaller", "Provider install failed ($i) : SSL Problems may occurs")
            }
        })

        ctInstance = buildCtInstance(useDefaultInstance = true)

        // attach necessary/needed listeners
        ctInstance?.apply {
            syncListener = object : SyncListener {
                override fun profileDataUpdated(updates: JSONObject?) {//no op
                }

                override fun profileDidInitialize(CleverTapID: String?) {
                    println(
                        "CleverTap DeviceID from Application class= $CleverTapID"
                    )
                }
            }

            setInboxMessageButtonListener(this@MyApplication)
            setCTInboxMessageListener(this@MyApplication)

            ctPushNotificationListener = this@MyApplication

            getCleverTapID { ctId ->
                println(
                    "CleverTap DeviceID from Application class= $ctId"
                )
                Log.i(TAG, "setting object id to firebase : $ctId")
                FirebaseAnalytics.getInstance(this@MyApplication)
                    .setUserProperty("ct_objectId", ctId)
            }

            // FileVarsData.defineFileVars(cleverTapAPI = this) // uncomment to define file vars before app launch
        }

        createNotificationChannels()
    }

    /**
     * Build Clevertap instance for testing and flags can be manipulated for different variations
     */
    private fun buildCtInstance(
        useDefaultInstance: Boolean = true,
        changeCredentials: Boolean = false,
        handshakeDomain: String? = null
    ): CleverTapAPI {
        val ctInstance = if (useDefaultInstance) {
            //CleverTapAPI.getDefaultInstance(this)!!

            val sp = getSharedPreferences("MigrationPrefs", MODE_PRIVATE)
            val migrationDone = sp.getBoolean("Migration Done", false)

            if (migrationDone.not()) {
                val editor = getSharedPreferences("WizRocket", Context.MODE_PRIVATE).edit()
                editor.clear()
                editor.apply()
                CleverTapAPI.setInstances(null)
                sp.edit().putBoolean("Migration Done", true).apply()
            }
            CleverTapAPI.getDefaultInstance(this, "custom-ct-id")
        } else {
            val config = CleverTapInstanceConfig.createInstance(
                applicationContext,
                "YOUR CLEVERTAP ACCOUNT ID",
                "YOUR CLEVERTAP TOKEN",
                "YOUR CLEVERTAP REGION",
            ).apply {
                handshakeDomain?.let { handshakeDomain ->
                    customHandshakeDomain = handshakeDomain
                }
            }
            CleverTapAPI.instanceWithConfig(this, config)
        }

        if (changeCredentials) {
            CleverTapAPI.changeCredentials(
                "YOUR CLEVERTAP ACCOUNT ID",
                "YOUR CLEVERTAP TOKEN",
                "YOUR PROXY DOMAIN",
                "YOUR SPIKY PROXY DOMAIN",
                "YOUR CUSTOM HANDSHAKE DOMAIN"
            )
        }

        return ctInstance
    }

    private fun createNotificationChannels() {

        listOf(
            Triple("BRTesting", "Core", "Core notifications"),
            Triple("PTTesting", "Push templates", "All push templates"),
            Triple("BlockBRTesting", "Blocked Core", "Blocked Core notifications"),
        ).forEach {
            CleverTapAPI.createNotificationChannel(
                this,
                it.first,
                it.second,
                it.third,
                NotificationManager.IMPORTANCE_MAX,
                true
            )
        }
    }

    private fun setupStrictMode(enable: Boolean = false) {
        if (enable) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    //.penaltyDeath()
                    .build()
            );
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build()
            )
        }
    }

    override fun onNotificationClickedPayloadReceived(payload: HashMap<String, Any>?) {
        Log.i(TAG, "onNotificationClickedPayloadReceived = $payload")
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

        // On Android 12, clear notification on CTA click when Activity is already running in activity backstack
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            NotificationUtils.dismissNotification(activity.intent, applicationContext)
        }
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        val payload = activity.intent?.extras
        if (payload?.containsKey("pt_id") == true && payload["pt_id"] == "pt_rating") {
            val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(payload["notificationId"] as Int)
        }
        if (payload?.containsKey("pt_id") == true && payload["pt_id"] == "pt_product_display") {
            val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(payload["notificationId"] as Int)
        }
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onInboxButtonClick(payload: HashMap<String, String>?) {
        Log.i(TAG, "InboxButtonClick with payload: $payload")
        //dismissAppInbox()
    }

    override fun onInboxItemClicked(message: CTInboxMessage?, contentPageIndex: Int, buttonIndex: Int) {
        Log.i(
            TAG,
            "InboxItemClicked at $contentPageIndex page-index with button-index: $buttonIndex"
        )

        //The contentPageIndex corresponds to the page index of the content, which ranges from 0 to the total number of pages for carousel templates. For non-carousel templates, the value is always 0, as they only have one page of content.
        val messageContentObject = message?.inboxMessageContents?.get(contentPageIndex)

        //The buttonIndex corresponds to the CTA button clicked (0, 1, or 2). A value of -1 indicates the app inbox body/message clicked.
        if (buttonIndex != -1) {
            //button is clicked
            val buttonObject: JSONObject? = messageContentObject?.links?.get(buttonIndex) as JSONObject?
            val buttonType = buttonObject?.optString("type")
            buttonType?.let {
                when (it) {
                    "copy" -> {
                        //this type copies the associated text to the clipboard
                        val copiedText = buttonObject.optJSONObject("copyText")?.optString("text")
                        Log.i(TAG, "copied text to Clipboard: $copiedText")
                        //dismissAppInbox()
                    }
                    "url" -> {
                        //this type fires the deeplink
                        val firedDeepLinkUrl =
                            buttonObject.optJSONObject("url")?.optJSONObject("android")
                                ?.optString("text")
                        Log.i(TAG, "fired deeplink url: $firedDeepLinkUrl")
                        //dismissAppInbox()
                    }
                    "kv" -> {
                        //this type contains the custom key-value pairs
                        val kvPair = buttonObject.optJSONObject("kv")
                        Log.i(TAG, "custom key-value pair: $kvPair")
                        //dismissAppInbox()
                    }
                    "rfp" -> {
                        //this type triggers the hard prompt of the notification permission
                        val rfpData = buttonObject.optString("text")
                        Log.i(TAG, "notification permission data: $rfpData")
                    }
                    else -> {
                        //do nothing here
                    }
                }
            }
        } else {
            //Item's body is clicked
            Log.i(TAG, "type/template of App Inbox item: ${message?.type}")
            //dismissAppInbox()
        }
    }
}