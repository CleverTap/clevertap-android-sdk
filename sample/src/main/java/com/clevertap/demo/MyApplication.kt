package com.clevertap.demo

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.clevertap.android.hms.HmsConstants
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
import com.clevertap.android.sdk.pushnotification.PushType
import com.clevertap.demo.ui.customtemplates.CopyToClipboardPresenter
import com.clevertap.demo.ui.customtemplates.CustomInterstitialPresenter
import com.clevertap.demo.ui.customtemplates.OpenURLConfirmPresenter
import com.clevertap.demo.ui.main.NotificationUtils
import com.clevertap.demo.ui.customtemplates.createCustomTemplates
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

            private val BAIDU_PUSH_TYPE = PushType(
                "bps",
                "bps_token",
                "com.clevertap.android.bps.BaiduPushProvider",
                "com.baidu.android.pushservice.PushMessageReceiver"
            )
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
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        CleverTapAPI.registerCustomInAppTemplates {
            createCustomTemplates(
                customInterPresenter = CustomInterstitialPresenter(),
                openUrlConfirmPresenter = OpenURLConfirmPresenter(this),
                copyToClipboardPresenter = CopyToClipboardPresenter(clipboardManager)
            )
        }

        CleverTapAPI.setDebugLevel(VERBOSE)
        //CleverTapAPI.changeXiaomiCredentials("your xiaomi app id","your xiaomi app key")
        //CleverTapAPI.enableXiaomiPushOn(XIAOMI_MIUI_DEVICES)
        TemplateRenderer.debugLevel = 3
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

            setInAppNotificationButtonListener { data ->
                Log.i(TAG, "in app notification button clicked, data = $data")
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

            // Different ways of creating default instance
            // Type 1
            val defaultConfig = CleverTapInstanceConfig.getDefaultInstance(this)
            CleverTapAPI.instanceWithConfig(this, defaultConfig)
            // Type 2
            CleverTapAPI.getDefaultInstance(this)!!
        } else {
            val config = CleverTapInstanceConfig.createInstance(
                applicationContext,
                "YOUR CLEVERTAP ACCOUNT ID",
                "YOUR CLEVERTAP TOKEN",
                "YOUR CLEVERTAP REGION",
            ).apply {
                customHandshakeDomain = handshakeDomain

                // enable push types other than FCM
                addPushType(HmsConstants.HPS)
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

    /**
     * Configures strict mode policies for both thread and VM operations to detect potential performance
     * and correctness issues during app development.
     *
     * - For thread policy, it detects all possible thread-related issues such as network operations or
     *   disk I/O on the main thread. It logs the violations for debugging purposes.
     *
     * - For VM policy, it monitors potential memory-related issues such as leaks or misuse of API calls.
     *   Violations are logged, helping developers find and fix memory management problems.
     *
     * This method is typically used during the development phase to catch bugs early. The `penaltyLog()`
     * method ensures that violations are logged without crashing the app, but more aggressive actions
     * like crashing can be enabled by using `penaltyDeath()` if desired.
     */
    private fun setupStrictMode(enable: Boolean = false) {
        if (enable) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    //.penaltyDeath()
                    .build()
            )
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
            val nm = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(payload["notificationId"] as Int)
        }
        if (payload?.containsKey("pt_id") == true && payload["pt_id"] == "pt_product_display") {
            val nm = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
