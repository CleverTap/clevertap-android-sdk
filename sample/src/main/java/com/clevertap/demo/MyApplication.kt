package com.clevertap.demo

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.SyncListener
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener
import org.json.JSONObject
import java.util.HashMap

class MyApplication : MultiDexApplication(), CTPushNotificationListener {

    override fun onCreate() {

        CleverTapAPI.setDebugLevel(VERBOSE)
        ActivityLifecycleCallback.register(this)
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
        CleverTapAPI.createNotificationChannel(
            this, "BRTesting", "Offers",
            "All Offers", NotificationManager.IMPORTANCE_MAX, true
        )
    }

    override fun onNotificationClickedPayloadReceived(payload: HashMap<String, Any>?) {
        Log.i("MyApplication", "onNotificationClickedPayloadReceived = $payload")
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}