package com.clevertap.demo.ui.main

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.clevertap.android.geofence.CTGeofenceAPI
import com.clevertap.android.geofence.CTGeofenceSettings
import com.clevertap.android.geofence.interfaces.CTGeofenceEventsListener
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.demo.BuildConfig
import com.clevertap.demo.HomeScreenActivity
import com.clevertap.demo.R
import com.clevertap.demo.ViewModelFactory
import com.clevertap.demo.WebViewActivity
import com.clevertap.demo.action
import com.clevertap.demo.snack
import org.json.JSONObject

private const val TAG = "HomeScreenFragment"
private const val PERMISSIONS_REQUEST_CODE = 34

data class HomeScreenFragmentBinding(
   val expandableListView: ExpandableListView,
   val root: CoordinatorLayout
)

class HomeScreenFragment : Fragment() {

    private val viewModel by viewModels<HomeScreenViewModel> {
        ViewModelFactory((activity as? HomeScreenActivity)?.cleverTapDefaultInstance)
    }

    companion object {
        fun newInstance() = HomeScreenFragment()
    }

    private lateinit var listItemBinding: HomeScreenFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.home_screen_fragment, container, false)
        listItemBinding = HomeScreenFragmentBinding(
            expandableListView = view.findViewById(R.id.expandableListView),
            root = view.findViewById(R.id.home_root)
        )

        listItemBinding.expandableListView.isNestedScrollingEnabled = true

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListAdapter()
        val cleverTapInstance = (activity as? HomeScreenActivity)?.cleverTapDefaultInstance
        val context = activity?.applicationContext!!

        viewModel.clickCommand.observe(viewLifecycleOwner, Observer<String> { commandPosition ->
            when (commandPosition) {
                "6-0" -> startActivity(Intent(activity, WebViewActivity::class.java))
                "7-0" -> { // init Geofence API
                    when {
                        // proceed only if cleverTap instance is not null
                        cleverTapInstance == null -> println("cleverTapInstance is null")
                        !checkPermissions() -> requestPermissions()
                        else -> initCTGeofenceApi(cleverTapInstance)
                    }
                }
                "7-1" -> { // trigger location
                    try {
                        CTGeofenceAPI.getInstance(context).triggerLocation()
                    } catch (e: IllegalStateException) {
                        // geofence not initialized
                        e.printStackTrace()
                        // init geofence
                        initCTGeofenceApi(cleverTapInstance!!)
                    }
                }
                "7-2" -> CTGeofenceAPI.getInstance(context).deactivate() // deactivate geofence
            }
        })
    }

    private fun setupListAdapter() {
        listItemBinding.expandableListView.setAdapter(
            HomeScreenListAdapter(viewModel, HomeScreenModel.listData.keys.toList(), HomeScreenModel.listData)
        )
    }

    private fun initCTGeofenceApi(cleverTapInstance: CleverTapAPI) {
        val context = activity?.applicationContext!!

        CTGeofenceAPI.getInstance(context).apply {
            init(
                CTGeofenceSettings.Builder()
                    .enableBackgroundLocationUpdates(true)
                    .setLogLevel(com.clevertap.android.geofence.Logger.DEBUG)
                    .setLocationAccuracy(CTGeofenceSettings.ACCURACY_HIGH)
                    .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                    .setGeofenceMonitoringCount(99)
                    .setInterval(3600000) // 1 hour
                    .setFastestInterval(1800000) // 30 minutes
                    .setSmallestDisplacement(1000f) // 1 km
                    .setGeofenceNotificationResponsiveness(300000) // 5 minute
                    .build(), cleverTapInstance
            )
            setOnGeofenceApiInitializedListener {
                Toast.makeText(context, "Geofence API initialized", Toast.LENGTH_SHORT).show()
            }
            setCtGeofenceEventsListener(object : CTGeofenceEventsListener {
                override fun onGeofenceEnteredEvent(jsonObject: JSONObject) {
                    Toast.makeText(context, "Geofence Entered", Toast.LENGTH_SHORT).show()
                }

                override fun onGeofenceExitedEvent(jsonObject: JSONObject) {
                    Toast.makeText(context, "Geofence Exited", Toast.LENGTH_SHORT).show()
                }
            })
            setCtLocationUpdatesListener { Toast.makeText(context, "Location updated", Toast.LENGTH_SHORT).show() }
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        val applicationContext = activity?.applicationContext!!
        val fineLocationPermissionState = ContextCompat.checkSelfPermission(applicationContext, ACCESS_FINE_LOCATION)

        val backgroundLocationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ContextCompat.checkSelfPermission(applicationContext, ACCESS_BACKGROUND_LOCATION)
        else PackageManager.PERMISSION_GRANTED

        return fineLocationPermissionState == PackageManager.PERMISSION_GRANTED &&
                backgroundLocationPermissionState == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("InlinedApi")
    private fun requestPermissions() {
        val applicationContext = activity?.applicationContext!!

        val permissionAccessFineLocationApproved = (ActivityCompat.checkSelfPermission(
            applicationContext, ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)

        val backgroundLocationPermissionApproved = (ActivityCompat.checkSelfPermission(
            applicationContext, ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)

        val shouldProvideRationale = permissionAccessFineLocationApproved && backgroundLocationPermissionApproved

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            listItemBinding.root.snack(R.string.permission_rationale) {
                action(R.string.ok) {
                    requestPermissions(
                        arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            requestPermissions(
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> println("user permission interaction was interrupted")
                grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED
                -> initCTGeofenceApi((activity as? HomeScreenActivity)?.cleverTapDefaultInstance!!)
                else -> {
                    listItemBinding.root.snack(R.string.permission_denied_explanation) {
                        action(R.string.settings) {

                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })

                        }
                    }
                }

            }
        }
    }
}