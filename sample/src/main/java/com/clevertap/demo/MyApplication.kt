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
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.SyncListener
import com.clevertap.android.sdk.interfaces.NotificationHandler
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.demo.ui.main.NotificationUtils
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener
import org.json.JSONObject
import kotlin.system.measureTimeMillis

class MyApplication : MultiDexApplication(), CTPushNotificationListener, ActivityLifecycleCallbacks {

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
}