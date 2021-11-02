package com.clevertap.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import com.clevertap.android.sdk.CTFeatureFlagsListener
import com.clevertap.android.sdk.CTInboxListener
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.SyncListener
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.product_config.CTProductConfigListener
import com.clevertap.demo.ui.main.HomeScreenFragment
import org.json.JSONObject
import java.util.ArrayList

private const val TAG = "HomeScreenActivity"

class HomeScreenActivity : AppCompatActivity(), CTInboxListener, DisplayUnitListener, CTProductConfigListener,
    CTFeatureFlagsListener, SyncListener {

    var cleverTapDefaultInstance: CleverTapAPI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, HomeScreenFragment.newInstance())
            }
        }

        initCleverTap()
    }

    private fun initCleverTap() {

        //Set Debug level for CleverTap
        CleverTapAPI.setDebugLevel(VERBOSE)

        //Create CleverTap's default instance
        cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this)

        cleverTapDefaultInstance?.apply {
            syncListener = this@HomeScreenActivity
            enableDeviceNetworkInfoReporting(true)
            //Set the Notification Inbox Listener
            ctNotificationInboxListener = this@HomeScreenActivity
            //Set the Display Unit Listener
            setDisplayUnitListener(this@HomeScreenActivity)
            //Set the Product Config Listener
            setCTProductConfigListener(this@HomeScreenActivity)
            //Set Feature Flags Listener
            setCTFeatureFlagsListener(this@HomeScreenActivity)
            //Initialize the inbox and wait for callbacks on overridden methods
            initializeInbox()
        }

        //With CleverTap Android SDK v3.2.0 you can create additional instances to send data to multiple CleverTap accounts
        //Create config object for an additional instance
        //While using this app, replace the below Account Id and token with your Account Id and token
//        val config = CleverTapInstanceConfig.createInstance(this, "YOUR_ACCOUNT_ID", "YOUR_ACCOUNT_TOKEN")
        // configure your clevertap identifiers as mentioned below
//        config.setIdentityKeys(Constants.TYPE_EMAIL,Constants.TYPE_PHONE, Constants.TYPE_IDENTITY)
        //Use the config object to create a custom instance
//        var cleverTapInstanceTwo = CleverTapAPI.instanceWithConfig(this, config)
    }

    override fun inboxDidInitialize() {
        Log.i(TAG, "inboxDidInitialize() called")
    }

    override fun inboxMessagesDidUpdate() {
        Log.i(TAG, "inboxMessagesDidUpdate() called")
    }

    override fun onDisplayUnitsLoaded(units: ArrayList<CleverTapDisplayUnit>?) {
        Log.i(TAG, "onDisplayUnitsLoaded() called")
    }

    override fun onInit() {
        Log.i(TAG, "onInit() called")
        //Must Call activate if you want to apply the last fetched values on init every time.
        cleverTapDefaultInstance?.productConfig()?.activate()
    }

    override fun onActivated() {
        Log.i(TAG, "onActivated() called")
    }

    override fun onFetched() {
        Log.i(TAG, "onFetched() called")
    }

    override fun featureFlagsUpdated() {
        Log.i(TAG, "featureFlagsUpdated() called")
    }

    override fun profileDataUpdated(updates: JSONObject?) {
        Log.i(TAG, "profileDataUpdated() called")
    }

    override fun profileDidInitialize(CleverTapID: String?) {
        Log.i(TAG, "profileDidInitialize() called")
    }
}