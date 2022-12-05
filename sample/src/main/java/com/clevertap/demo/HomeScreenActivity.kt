package com.clevertap.demo


import android.app.NotificationManager
import android.content.Context

import android.content.Intent
import android.os.Build

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import com.clevertap.android.sdk.*
import com.clevertap.android.sdk.CleverTapAPI.LogLevel.VERBOSE
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.product_config.CTProductConfigListener
import com.clevertap.demo.ui.main.HomeScreenFragment

import dev.shreyaspatil.MaterialDialog.MaterialDialog.Builder
import net.khirr.android.privacypolicy.PrivacyPolicyDialog
import net.khirr.android.privacypolicy.PrivacyPolicyDialog.OnClickListener

import com.clevertap.demo.ui.main.NotificationUtils

import org.json.JSONObject
import java.util.HashMap

private const val TAG = "HomeScreenActivity"

class HomeScreenActivity : AppCompatActivity(), CTInboxListener, DisplayUnitListener, CTProductConfigListener,
    CTFeatureFlagsListener, SyncListener, InAppNotificationListener,
    PushPermissionResponseListener,
    InAppNotificationButtonListener {

    var cleverTapDefaultInstance: CleverTapAPI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, HomeScreenFragment.newInstance())
            }
        }

        /*val bundle = Bundle().apply {
            putString("wzrk_acct_id", "TEST-46W-WWR-R85Z")
            putString("nm", "Grab 'em on Myntra's Maxessorize Sale")
            putString("nt", "Ye dil ❤️️ maange more accessories?")
            putString("pr", "")
            putString("wzrk_pivot", "")
            putString("wzrk_sound", "true")
            putString("wzrk_cid", "BRTesting")
            putString("wzrk_clr", "#ed732d")
            putString("wzrk_nms", "Grab 'em on Myntra's Maxessorize Sale")
            putString("wzrk_pid", (10000_00000..99999_99999).random().toString())
            putString("wzrk_rnv", "true")
            putString("wzrk_ttl", "1627731067")
            putString("wzrk_push_amp", "false")
            putString("wzrk_bc", "")
            putString("wzrk_bi", "2")
            putString("wzrk_bp", "https://imgur.com/6DavQwg.jpg")
            putString("wzrk_dl", "")
            putString("wzrk_dt", "FIREBASE")
            putString("wzrk_id", "1627639375_20210730")
            putString("wzrk_pn", "true")
        }

        Thread {
            CleverTapAPI.createNotification(applicationContext, bundle)
        }.start()
        Thread {
            CleverTapAPI.createNotification(applicationContext, bundle)
        }.start()*/
        initCleverTap()

        val isReadPolicy: Boolean
        val email: String?

        val sharedPref = getPreferences(Context.MODE_PRIVATE).apply {
            isReadPolicy = getBoolean("isReadPolicy", false)
            email = getString("email", null)
        }

        if (!isReadPolicy) {
            val dialog = PrivacyPolicyDialog(
                this,
                "https://clevertap.com/terms-service/",
                "https://clevertap.com/privacy-policy/"
            )
            dialog.apply {
                addPoliceLine(resources.getString(R.string.policy_line_1));
                addPoliceLine(resources.getString(R.string.policy_line_2));
                addPoliceLine(resources.getString(R.string.policy_line_3));
                addPoliceLine(resources.getString(R.string.policy_line_4));
                addPoliceLine(resources.getString(R.string.policy_line_5));
                addPoliceLine(resources.getString(R.string.policy_line_6));
                addPoliceLine(resources.getString(R.string.policy_line_7));
                onClickListener = object : OnClickListener {
                    override fun onAccept(isFirstTime: Boolean) {
                        showLocationPermissionPolicyDialog {
                            with(sharedPref!!.edit()) {
                                putBoolean("isReadPolicy", true)
                                apply()
                                EmailDialogFragment().show(supportFragmentManager, "Email")
                            }
                        }
                    }

                    override fun onCancel() {
                        finish()
                    }
                }
                show()
            }
        } else {
            if (email == null) {
                EmailDialogFragment().show(supportFragmentManager, "Email")
            }
        }
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

            inAppNotificationListener = this@HomeScreenActivity

            registerPushPermissionNotificationResponseListener(this@HomeScreenActivity)
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i("Playground", "onNewIntent()")

        /**
         * On Android 12, Raise notification clicked event when Activity is already running in activity backstack
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            cleverTapDefaultInstance?.pushNotificationClickedEvent(intent!!.extras)
        }

        /**
        * On Android 12, clear notification on CTA click when Activity is already running in activity backstack
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            NotificationUtils.dismissNotification(intent, applicationContext)
        }
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

    private fun showLocationPermissionPolicyDialog(success : () -> Unit) {
        val mDialog = Builder(this)
            .setTitle("Location Privacy Policy")
            .setMessage(getString(R.string.location_policy_message))
            .setCancelable(false)
            .setPositiveButton("Accept", R.drawable.thumb_up) { dialogInterface, which ->
                dialogInterface.dismiss()
                success()
            }
            .setNegativeButton(
                "Deny", R.drawable.thumb_down
            ) { dialogInterface, which ->
                dialogInterface.dismiss()
                finish()
            }
            .setAnimation(R.raw.location)
            .build()

        // Show Dialog

        // Show Dialog
        mDialog.show()
    }

    override fun beforeShow(extras: MutableMap<String, Any>?): Boolean {
        Log.i(TAG, "InApp---> beforeShow() called")
        return true
    }

    override fun onShow(ctInAppNotification: CTInAppNotification?) {
        Log.i(TAG, "InApp---> onShow() called")
    }

    override fun onDismissed(
        extras: MutableMap<String, Any>?,
        actionExtras: MutableMap<String, Any>?
    ) {
        Log.i(TAG, "InApp---> onDismissed() called")
    }

    override fun onPushPermissionResponse(accepted: Boolean) {
        Log.i(TAG, "InApp---> response() called  $accepted")
        if(accepted){
            Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()

            //For Android 13+ we need to create notification channel after notification permission is accepted
            CleverTapAPI.createNotificationChannel(
                this, "BRTesting", "Core",
                "Core notifications", NotificationManager.IMPORTANCE_MAX, true
            )

            CleverTapAPI.createNotificationChannel(
                this, "PTTesting", "Push templates",
                "All push templates", NotificationManager.IMPORTANCE_MAX, true
            )
        }else{
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInAppButtonClick(payload: HashMap<String, String>?) {
        Log.i(TAG, "onInAppButtonClick() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        cleverTapDefaultInstance?.unregisterPushPermissionNotificationResponseListener(this)
    }
}