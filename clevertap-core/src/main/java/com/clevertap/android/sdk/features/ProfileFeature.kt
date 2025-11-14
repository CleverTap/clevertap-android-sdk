package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.LocationManager
import com.clevertap.android.sdk.ProfileValueHandler
import com.clevertap.android.sdk.login.LoginInfoProvider

/**
 * User profile and login management
 * Handles user identity, login operations, and profile updates
 */
internal class ProfileFeature(
    val loginInfoProvider: LoginInfoProvider,
    val profileValueHandler: ProfileValueHandler,
) {
    lateinit var coreContract: CoreContract
    val locationManager by lazy {
        // todo check if this is the right place for the same
        LocationManager(
            mContext = coreContract.context(),
            mConfig = coreContract.config(),
            mCoreMetaData = coreContract.coreMetaData(),
            analyticsManager = coreContract.analytics()
        )
    }

    // ========== PUBLIC API FACADES ==========
    // These methods provide direct delegation from CleverTapAPI to Profile functionality
    // Signature matches CleverTapAPI public methods for 1:1 mapping

    /**
     * Get the current device location
     */
    fun getLocation(): android.location.Location? {
        return locationManager._getLocation()
    }

    /**
     * Set the user profile location in CleverTap
     */
    fun setLocation(location: android.location.Location?): java.util.concurrent.Future<*>? {
        return locationManager._setLocation(location)
    }

    /**
     * Set location for geofences
     */
    fun setLocationForGeofences(location: android.location.Location?, sdkVersion: Int): java.util.concurrent.Future<*>? {
        coreContract.coreMetaData().setLocationForGeofence(true)
        coreContract.coreMetaData().setGeofenceSDKVersion(sdkVersion)
        return locationManager._setLocation(location)
    }
}
