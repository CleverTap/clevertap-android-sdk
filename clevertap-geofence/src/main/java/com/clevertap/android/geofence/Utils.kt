package com.clevertap.android.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.clevertap.android.sdk.CleverTapAPI
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object Utils {

    private var isPlayServicesDependencyAvailable: Boolean? = null

    private var isFusedLocationDependencyAvailable: Boolean? = null

    private var isConcurrentFuturesDependencyAvailable: Boolean? = null

    @JvmStatic
    fun emptyIfNull(str: String?): String {
        return str.orEmpty()
    }

    @JvmStatic
    fun getGeofenceSDKVersion(): Int {
        return BuildConfig.VERSION_CODE
    }

    @JvmStatic
    fun hasBackgroundLocationPermission(context: Context?): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Checks if Application has provided permission
     *
     * @param context    application [Context]
     * @param permission for example, [Manifest.permission.ACCESS_FINE_LOCATION]
     */
    @JvmStatic
    fun hasPermission(context: Context?, permission: String?): Boolean {
        return try {
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context!!, permission!!)
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Creates [com.clevertap.android.sdk.CleverTapAPI] instance if it's null and initializes
     * Geofence SDK, mostly in killed state.
     *
     * **Must be called from background thread**
     *
     * @param context application [Context]
     * @return true if geofence sdk initialized successfully, false otherwise
     */
    @JvmStatic
    @WorkerThread
    fun initCTGeofenceApiIfRequired(context: Context): Boolean {
        val ctGeofenceAPI = CTGeofenceAPI.getInstance(context)

        if (ctGeofenceAPI.cleverTapApi == null) {
            val ctGeofenceSettings = readSettingsFromFile(context)
            if (ctGeofenceSettings == null) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Could not initialize CT instance! Dropping this call"
                )
                return false
            }

            val cleverTapAPI = CleverTapAPI.getGlobalInstance(context, ctGeofenceSettings.id)

            if (cleverTapAPI == null) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Critical issue :: After calling  CleverTapAPI.getGlobalInstance also init is failed! Dropping this call"
                )
                return false
            }

            ctGeofenceAPI.init(ctGeofenceSettings, cleverTapAPI)
        }

        return true
    }

    /**
     * Checks if Google Play services dependency is available.
     *
     * @return `true` if available, otherwise `false`.
     */
    @JvmStatic
    fun isConcurrentFuturesDependencyAvailable(): Boolean {
        if (isConcurrentFuturesDependencyAvailable == null) { //use reflection only once
            // concurrent futures
            isConcurrentFuturesDependencyAvailable = try {
                Class.forName("androidx.concurrent.futures.CallbackToFutureAdapter")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }

        return isConcurrentFuturesDependencyAvailable!!
    }

    /**
     * Checks if Google Play services dependency is available for Fused Location.
     *
     * @return `true` if available, otherwise `false`.
     */
    @JvmStatic
    fun isFusedLocationApiDependencyAvailable(): Boolean {
        if (isFusedLocationDependencyAvailable == null) { //use reflection only once
            if (!isPlayServicesDependencyAvailable()) {
                isFusedLocationDependencyAvailable = false
            } else {
                isFusedLocationDependencyAvailable = isFusedLocationProviderClassAvailable()
            }
        }

        return isFusedLocationDependencyAvailable!!
    }

    /**
     * Checks via reflection whether the Fused Location provider client class is on the classpath.
     */
    private fun isFusedLocationProviderClassAvailable(): Boolean {
        return try {
            Class.forName("com.google.android.gms.location.FusedLocationProviderClient")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Converts [JSONObject] to list of [com.google.android.gms.location.Geofence] Ids
     *
     * @param jsonObject containing geofence list
     * @return list of [com.google.android.gms.location.Geofence] Ids
     */
    @JvmStatic
    fun jsonToGeoFenceList(jsonObject: JSONObject): List<String> {
        val geofenceIdList = ArrayList<String>()
        try {
            val array = jsonObject.getJSONArray("geofences")

            for (i in 0 until array.length()) {
                val `object` = array.getJSONObject(i)
                geofenceIdList.add(`object`.getString(CTGeofenceConstants.KEY_ID))
            }
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Could not convert JSON to GeofenceIdList", e
            )
        }
        return geofenceIdList
    }

    /**
     * Notifies Listener for location update on main thread through [CTLocationUpdatesListener]
     *
     * @param context  application [Context]
     * @param location instance of [Location]
     */
    @JvmStatic
    fun notifyLocationUpdates(context: Context, location: Location?) {
        val ctLocationUpdatesListener = CTGeofenceAPI.getInstance(context)
            .ctLocationUpdatesListener

        if (ctLocationUpdatesListener != null) {
            com.clevertap.android.sdk.Utils.runOnUiThread {
                ctLocationUpdatesListener.onLocationUpdates(location)
            }
        }
    }

    /**
     * Reads [CTGeofenceSettings] from file.
     *
     * **Must be called from background thread**
     *
     * @param context application [Context]
     * @return [CTGeofenceSettings], null if no settings found in file
     */
    @JvmStatic
    @WorkerThread
    fun readSettingsFromFile(context: Context): CTGeofenceSettings? {
        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Reading settings from file..."
        )

        var ctGeofenceSettings: CTGeofenceSettings? = null

        val settingsString = FileUtils.readFromFile(
            context,
            FileUtils.getCachedFullPath(context, CTGeofenceConstants.SETTINGS_FILE_NAME)
        )
        if (settingsString.trim { it <= ' ' } != "") {
            try {
                val jsonObject = JSONObject(settingsString)

                ctGeofenceSettings = CTGeofenceSettings.Builder()
                    .enableBackgroundLocationUpdates(
                        jsonObject.getBoolean(CTGeofenceConstants.KEY_LAST_BG_LOCATION_UPDATES)
                    )
                    .setLocationAccuracy(jsonObject.getInt(CTGeofenceConstants.KEY_LAST_ACCURACY).toByte())
                    .setLocationFetchMode(jsonObject.getInt(CTGeofenceConstants.KEY_LAST_FETCH_MODE).toByte())
                    .setLogLevel(jsonObject.getInt(CTGeofenceConstants.KEY_LAST_LOG_LEVEL))
                    .setGeofenceMonitoringCount(jsonObject.getInt(CTGeofenceConstants.KEY_LAST_GEO_COUNT))
                    .setId(jsonObject.getString(CTGeofenceConstants.KEY_ID))
                    .setInterval(jsonObject.getLong(CTGeofenceConstants.KEY_LAST_INTERVAL))
                    .setFastestInterval(jsonObject.getLong(CTGeofenceConstants.KEY_LAST_FASTEST_INTERVAL))
                    .setSmallestDisplacement(
                        jsonObject.getDouble(CTGeofenceConstants.KEY_LAST_DISPLACEMENT).toFloat()
                    )
                    .setGeofenceNotificationResponsiveness(
                        jsonObject.getInt(CTGeofenceConstants.KEY_LAST_GEO_NOTIFICATION_RESPONSIVENESS)
                    )
                    .build()

                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Read settings successfully from file"
                )
            } catch (e: Exception) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to read geofence settings from file"
                )
            }
        } else {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Settings not found in file..."
            )
        }

        return ctGeofenceSettings
    }

    /**
     * Creates sub array from provided [JSONArray]
     *
     * @param arr       [JSONArray]
     * @param fromIndex fromIndex
     * @param toIndex   toIndex
     * @return sub array exclusive of toIndex
     * @throws IllegalStateException if fromIndex > toIndex
     */
    @JvmStatic
    fun subArray(arr: JSONArray, fromIndex: Int, toIndex: Int): JSONArray {
        require(fromIndex <= toIndex) { "fromIndex($fromIndex) > toIndex($toIndex)" }

        val jsonArray = JSONArray()

        try {
            for (i in fromIndex until toIndex) {
                jsonArray.put(arr.getJSONObject(i))
            }
        } catch (e: JSONException) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to create subArray from JSONArray", e
            )
        }

        return jsonArray
    }

    /**
     * Writes [CTGeofenceSettings] to file.
     *
     * **Must be called from background thread**
     *
     * @param context            application [Context]
     * @param ctGeofenceSettings new [CTGeofenceSettings]
     */
    @JvmStatic
    @WorkerThread
    fun writeSettingsToFile(context: Context, ctGeofenceSettings: CTGeofenceSettings) {
        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Writing new settings to file..."
        )

        val settings = JSONObject()
        try {
            putSettings(settings, context, ctGeofenceSettings)

            val writeJsonToFile = FileUtils.writeJsonToFile(
                context, FileUtils.getCachedDirName(context),
                CTGeofenceConstants.SETTINGS_FILE_NAME, settings
            )

            if (writeJsonToFile) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "New settings successfully written to file"
                )
            } else {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to write new settings to file"
                )
            }
        } catch (e: JSONException) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to write new settings to file while parsing json"
            )
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to write new settings to file"
            )
        }
    }

    /**
     * Copies the persisted fields of [ctGeofenceSettings] and the account id into [settings].
     */
    @Throws(JSONException::class)
    private fun putSettings(settings: JSONObject, context: Context, ctGeofenceSettings: CTGeofenceSettings) {
        settings.put(CTGeofenceConstants.KEY_LAST_ACCURACY, ctGeofenceSettings.locationAccuracy)
        settings.put(CTGeofenceConstants.KEY_LAST_FETCH_MODE, ctGeofenceSettings.locationFetchMode)
        settings.put(
            CTGeofenceConstants.KEY_LAST_BG_LOCATION_UPDATES,
            ctGeofenceSettings.isBackgroundLocationUpdatesEnabled
        )
        settings.put(CTGeofenceConstants.KEY_LAST_LOG_LEVEL, ctGeofenceSettings.logLevel)
        settings.put(CTGeofenceConstants.KEY_LAST_GEO_COUNT, ctGeofenceSettings.geofenceMonitoringCount)
        settings.put(CTGeofenceConstants.KEY_LAST_INTERVAL, ctGeofenceSettings.interval)
        settings.put(CTGeofenceConstants.KEY_LAST_FASTEST_INTERVAL, ctGeofenceSettings.fastestInterval)
        settings.put(
            CTGeofenceConstants.KEY_LAST_DISPLACEMENT,
            ctGeofenceSettings.smallestDisplacement.toDouble()
        )
        settings.put(
            CTGeofenceConstants.KEY_LAST_GEO_NOTIFICATION_RESPONSIVENESS,
            ctGeofenceSettings.geofenceNotificationResponsiveness
        )
        settings.put(CTGeofenceConstants.KEY_ID, CTGeofenceAPI.getInstance(context).accountId)
    }

    /**
     * Checks if Google Play services dependency is available.
     *
     * @return `true` if available, otherwise `false`.
     */
    private fun isPlayServicesDependencyAvailable(): Boolean {
        if (isPlayServicesDependencyAvailable == null) { //use reflection only once
            // Play Services
            isPlayServicesDependencyAvailable = try {
                Class.forName("com.google.android.gms.common.GooglePlayServicesUtil")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }

        return isPlayServicesDependencyAvailable!!
    }
}
