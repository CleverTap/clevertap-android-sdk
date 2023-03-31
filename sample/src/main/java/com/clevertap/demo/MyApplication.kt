package com.clevertap.demo

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.app.NotificationManager
import android.content.Context
import android.content.Intent

import android.os.Build

import android.os.Bundle
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.clevertap.android.pushtemplates.PushTemplateNotificationHandler
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.*
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.interfaces.NotificationHandler
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.demo.ui.main.NotificationUtils
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener
import org.json.JSONObject
import kotlin.system.measureTimeMillis

class MyApplication : MultiDexApplication(), CTPushNotificationListener, ActivityLifecycleCallbacks,
    InboxMessageButtonListener, InboxMessageListener {

    override fun onCreate() {

        /*StrictMode.setThreadPolicy(
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
        )*/

        CleverTapAPI.setDebugLevel(VERBOSE)
        //CleverTapAPI.changeXiaomiCredentials("your xiaomi app id","your xiaomi app key")
        //CleverTapAPI.enableXiaomiPushOn(XIAOMI_MIUI_DEVICES)
        TemplateRenderer.debugLevel = 3;
        CleverTapAPI.setNotificationHandler(PushTemplateNotificationHandler() as NotificationHandler)

        val measureTimeMillis = measureTimeMillis { ActivityLifecycleCallback.register(this) }
        println("Time taken to execute  ActivityLifecycleCallback.register = $measureTimeMillis milliseconds")

        registerActivityLifecycleCallbacks(this)
        super.onCreate()

        ProviderInstaller.installIfNeededAsync(this, object : ProviderInstallListener {
            override fun onProviderInstalled() {}
            override fun onProviderInstallFailed(i: Int, intent: Intent?) {
                Log.i("ProviderInstaller", "Provider install failed ($i) : SSL Problems may occurs")
            }
        })

        val defaultInstance = CleverTapAPI.getDefaultInstance(this)
        defaultInstance?.syncListener = object : SyncListener {
            override fun profileDataUpdated(updates: JSONObject?) {//no op
            }

            override fun profileDidInitialize(CleverTapID: String?) {
                println(
                    "CleverTap DeviceID from Application class= $CleverTapID"
                )
            }
        }

        defaultInstance?.setInboxMessageButtonListener(this)
        defaultInstance?.setCTInboxMessageListener(this)

        defaultInstance?.ctPushNotificationListener = this

        defaultInstance?.getCleverTapID {
            println(
                "CleverTap DeviceID from Application class= $it"
            )
        }

        /*println(
            "CleverTapAttribution Identifier from Application class= " +
                    "${defaultInstance?.cleverTapAttributionIdentifier}"
        )*/

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            CleverTapAPI.createNotificationChannel(
                this, "BRTesting", "Core",
                "Core notifications", NotificationManager.IMPORTANCE_MAX, true
            )
            CleverTapAPI.createNotificationChannel(
                this, "PTTesting", "Push templates",
                "All push templates", NotificationManager.IMPORTANCE_MAX, true
            )
            CleverTapAPI.createNotificationChannel(
                this, "BlockBRTesting", "Blocked Core",
                "Blocked Core notifications", NotificationManager.IMPORTANCE_NONE, true
            )
        }
    }

    override fun onNotificationClickedPayloadReceived(payload: HashMap<String, Any>?) {

        Log.i("MyApplication", "onNotificationClickedPayloadReceived = $payload")
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
        Log.i("MyApplication", "InboxButtonClick with payload: $payload")
        //dismissAppInbox()
    }

    override fun onInboxItemClicked(message: CTInboxMessage?, contentPageIndex: Int, buttonIndex: Int) {
        Log.i(
            "MyApplication",
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
                        Log.i("MyApplication", "copied text to Clipboard: $copiedText")
                        //dismissAppInbox()
                    }
                    "url" -> {
                        //this type fires the deeplink
                        val firedDeepLinkUrl =
                            buttonObject.optJSONObject("url")?.optJSONObject("android")
                                ?.optString("text")
                        Log.i("MyApplication", "fired deeplink url: $firedDeepLinkUrl")
                        //dismissAppInbox()
                    }
                    "kv" -> {
                        //this type contains the custom key-value pairs
                        val kvPair = buttonObject.optJSONObject("kv")
                        Log.i("MyApplication", "custom key-value pair: $kvPair")
                        //dismissAppInbox()
                    }
                    "rfp" -> {
                        //this type triggers the hard prompt of the notification permission
                        val rfpData = buttonObject.optString("text")
                        Log.i("MyApplication", "notification permission data: $rfpData")
                    }
                    else -> {
                        //do nothing here
                    }
                }
            }
        } else {
            //Item's body is clicked
            Log.i("MyApplication", "type/template of App Inbox item: ${message?.type}")
            //dismissAppInbox()
        }
    }

    private fun dismissAppInbox() {
        val defaultInstance = CleverTapAPI.getDefaultInstance(this)
        defaultInstance?.dismissAppInbox()
    }
}